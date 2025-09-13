/*
 * Copyright 2025 sheik
 *
 * Created by sheik on 11-07-2024.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zs.core.coil

import android.content.ContentResolver
import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.Paint
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.core.graphics.applyCanvas
import coil3.ImageLoader
import coil3.Uri
import coil3.asImage
import coil3.decode.DataSource
import coil3.decode.DecodeUtils
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.fetch.ImageFetchResult
import coil3.request.Options
import coil3.request.bitmapConfig
import coil3.size.Precision
import coil3.size.Size
import coil3.size.pxOrElse
import coil3.toAndroidUri
import kotlin.math.roundToInt

private const val TAG = "ThumbnailFetcher"

class VideoThumbnailFetcher(
    private val data: Uri,
    private val options: Options
) : Fetcher {

    /**
     * A [Fetcher.Factory] that creates [ThumbnailFetcher] instances for URIs that point to content
     * using either [ContentResolver.loadThumbnail] on API 10 and below or [MediaStore.Images.Thumbnails.getThumbnail]
     * on older devices.
     */
    class Factory : Fetcher.Factory<Uri> {
        override fun create(
            data: Uri,
            options: Options,
            imageLoader: ImageLoader
        ): Fetcher? {
            // Early exit if the URI is not a content URI
            if (data.scheme != ContentResolver.SCHEME_CONTENT) return null

            val context = options.context
            // Retrieve the MIME type of the content
            val mimeType = context.contentResolver.getType(data.toAndroidUri()) ?: return null

            // Check if the MIME type is supported (image or video)
            if (!mimeType.startsWith("video/")) {
                return null
            }

            // Create and return a ThumbnailFetcher instance
            return VideoThumbnailFetcher(data, options)
        }
    }

    private fun isConfigValid(
        bitmap: Bitmap,
        options: Options
    ): Boolean =
        Build.VERSION.SDK_INT < 26 || bitmap.config != Bitmap.Config.HARDWARE || options.bitmapConfig == Bitmap.Config.HARDWARE

    private fun isSizeValid(
        bitmap: Bitmap,
        options: Options,
        size: Size
    ): Boolean {
        if (options.precision == Precision.INEXACT) return true
        val multiplier = DecodeUtils.computeSizeMultiplier(
            srcWidth = bitmap.width,
            srcHeight = bitmap.height,
            dstWidth = size.width.pxOrElse { bitmap.width },
            dstHeight = size.height.pxOrElse { bitmap.height },
            scale = options.scale
        )
        return multiplier == 1.0
    }

    /** Return [inBitmap] or a copy of [inBitmap] that is valid for the input [options] and [size]. */
    private fun normalize(
        inBitmap: Bitmap,
        size: Size
    ): Bitmap {
        // Fast path: if the input bitmap is valid, return it.
        if (isConfigValid(inBitmap, options) && isSizeValid(inBitmap, options, size)) {
            return inBitmap
        }

        // Slow path: re-render the bitmap with the correct size + config.
        val scale = DecodeUtils.computeSizeMultiplier(
            srcWidth = inBitmap.width,
            srcHeight = inBitmap.height,
            dstWidth = size.width.pxOrElse { inBitmap.width },
            dstHeight = size.height.pxOrElse { inBitmap.height },
            scale = options.scale
        ).toFloat()
        val dstWidth = (scale * inBitmap.width).roundToInt()
        val dstHeight = (scale * inBitmap.height).roundToInt()
        val safeConfig = when {
            Build.VERSION.SDK_INT >= 26 && options.bitmapConfig == Bitmap.Config.HARDWARE -> Bitmap.Config.ARGB_8888
            else -> options.bitmapConfig
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val outBitmap = Bitmap.createBitmap(dstWidth, dstHeight, safeConfig)
        outBitmap.applyCanvas {
            scale(scale, scale)
            drawBitmap(inBitmap, 0f, 0f, paint)
        }
        inBitmap.recycle()

        return outBitmap
    }

    override suspend fun fetch(): FetchResult {
        val resolver = options.context.contentResolver
        val uri = data.toAndroidUri()
        // Fetch the raw bitmap based on Android version
        val rawBitmap = when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                // On Android Q and above, use loadThumbnail with a ThumbnailSize object
                val size = options.size.let {
                    android.util.Size(it.width.pxOrElse { 256 },
                        it.height.pxOrElse { 256 })
                }
                resolver.loadThumbnail(uri, size, null)
            }

            else -> {
                // On older versions, use getThumbnail with appropriate kind based on requested size
                val id = ContentUris.parseId(uri)
                val size =
                    options.size.let { it.width.pxOrElse { 256 } to it.height.pxOrElse { 256 } }
                val kind = when {
                    size.first <= 96 && size.second <= 96 -> MediaStore.Images.Thumbnails.MICRO_KIND
                    size.first <= 512 && size.second <= 384 -> MediaStore.Images.Thumbnails.MINI_KIND
                    else -> MediaStore.Images.Thumbnails.FULL_SCREEN_KIND
                }
                MediaStore.Images.Thumbnails.getThumbnail(resolver, id, kind, null)
            }
        }
        // Ensure the bitmap was successfully decoded
        // https://developer.android.com/guide/topics/media/media-formats#video-formats
        checkNotNull(rawBitmap) { "Failed to decode thumbnail of size ${options.size}." }

        // Extract dimensions and normalize the bitmap
        val srcWidth = rawBitmap.width;
        val srcHeight = rawBitmap.height
        val dstSize = options.size
        val bitmap = normalize(rawBitmap, dstSize)

        // Determine if the image was sampled
        val isSampled = when {
            srcWidth < 0 && srcHeight < 0 -> true // We were unable to determine the original size of the video. Assume it is sampled.

            else -> DecodeUtils.computeSizeMultiplier(
                srcWidth = srcWidth,
                srcHeight = srcHeight,
                dstWidth = bitmap.width,
                dstHeight = bitmap.height,
                scale = options.scale
            ) < 1.0
        }

        // Log debugging information
        Log.d(
            TAG,
            "fetch - DstSize: $dstSize | SrcSize: ${srcWidth}x$srcHeight | isSampled: $isSampled"
        )

        // Return the decoded drawable result
        return ImageFetchResult(
            image = bitmap.asImage(),
            isSampled = isSampled,
            dataSource = DataSource.DISK
        )
    }
}
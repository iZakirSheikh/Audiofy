/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 21-11-2024.
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

package com.zs.core_ui.coil

import android.content.ContentResolver
import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.Bitmap.createBitmap
import android.graphics.Paint
import android.net.Uri
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.provider.MediaStore
import android.provider.MediaStore.Images.Thumbnails.getThumbnail
import android.util.Log
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.drawable.toDrawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.decode.DecodeUtils
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.Size
import coil.size.pxOrElse
import kotlin.math.roundToInt
import android.util.Size as ThumbnailSize

private const val TAG = "VideoThumbnailFetcher"

class VideoThumbnailFetcher(
    private val data: Uri,
    private val options: Options
) : Fetcher {

    /**
     * A [Fetcher.Factory] that creates [VideoThumbnailFetcher] instances for URIs that point to content
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
            val resolver = options.context.contentResolver
            // Retrieve the MIME type of the content
            val mimeType = resolver.getType(data) ?: return null
            // Check if the MIME type is supported (image or video)
            if (!mimeType.startsWith("video/"))
                return null
            // Create and return a ThumbnailFetcher instance
            return VideoThumbnailFetcher(data, options)
        }
    }

    private fun isConfigValid(bitmap: Bitmap, options: Options): Boolean =
        SDK_INT < 26 || bitmap.config != Bitmap.Config.HARDWARE || options.config == Bitmap.Config.HARDWARE

    private fun isSizeValid(bitmap: Bitmap, options: Options, size: Size): Boolean {
        if (options.allowInexactSize) return true
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
    private fun normalize(inBitmap: Bitmap, size: Size): Bitmap {
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
            SDK_INT >= 26 && options.config == Bitmap.Config.HARDWARE -> Bitmap.Config.ARGB_8888
            else -> options.config
        }

        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val outBitmap = createBitmap(dstWidth, dstHeight, safeConfig)
        outBitmap.applyCanvas {
            scale(scale, scale)
            drawBitmap(inBitmap, 0f, 0f, paint)
        }
        inBitmap.recycle()

        return outBitmap
    }

    override suspend fun fetch(): FetchResult? {
        // Fetch the raw bitmap based on Android version
        val resolver = options.context.contentResolver
        val rawBitmap = when {
            SDK_INT >= Build.VERSION_CODES.Q -> {
                // On Android Q and above, use loadThumbnail with a ThumbnailSize object
                val size = options.size.let {
                    ThumbnailSize(it.width.pxOrElse { 256 }, it.height.pxOrElse { 256 })
                }
                resolver.loadThumbnail(data, size, null)
            }

            else -> {
                // On older versions, use getThumbnail with appropriate kind based on requested size
                val id = ContentUris.parseId(data)
                val size =
                    options.size.let { it.width.pxOrElse { 256 } to it.height.pxOrElse { 256 } }
                val kind = when {
                    size.first <= 96 && size.second <= 96 -> MediaStore.Images.Thumbnails.MICRO_KIND
                    size.first <= 512 && size.second <= 384 -> MediaStore.Images.Thumbnails.MINI_KIND
                    else -> MediaStore.Images.Thumbnails.FULL_SCREEN_KIND
                }
                getThumbnail(resolver, id, kind, null)
            }
        }
        // Ensure the bitmap was successfully decoded
        // https://developer.android.com/guide/topics/media/media-formats#video-formats
        checkNotNull(rawBitmap) { "Failed to decode thumbnail of size ${options.size}." }
        // Extract dimensions and normalize the bitmap
        val sWidth = rawBitmap.width;
        val sHeight = rawBitmap.height
        val dstSize = options.size
        val bitmap = normalize(rawBitmap, dstSize)

        val isSampled = when {
            // We were unable to determine the original size of the video. Assume it is sampled.
            sWidth < 0 && sHeight < 0 -> true
            else -> DecodeUtils.computeSizeMultiplier(
                srcWidth = sWidth,
                srcHeight = sHeight,
                dstWidth = bitmap.width,
                dstHeight = bitmap.height,
                scale = options.scale
            ) < 1.0
        }
        // Log debugging information
        Log.d(
            TAG, "fetch - DstSize: $dstSize | SrcSize: ${sWidth}x$sHeight | isSampled: $isSampled"
        )

        // Return the decoded drawable result
        return DrawableResult(
            drawable = bitmap.toDrawable(options.context.resources),
            isSampled = isSampled,
            dataSource = DataSource.DISK
        )
    }
}


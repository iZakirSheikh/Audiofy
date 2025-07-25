/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 22-07-2025.
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

package com.zs.audiofy.common.impl

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.lifecycle.SavedStateHandle
import com.zs.audiofy.R
import com.zs.audiofy.properties.PropertiesViewState
import com.zs.audiofy.properties.RouteProperties
import com.zs.audiofy.properties.get
import java.io.File

private val MediaMetadataRetriever.embeddedBitmap: ImageBitmap?
    get() {
        val array = embeddedPicture ?: return null
        return BitmapFactory.decodeByteArray(array, 0, array.size).asImageBitmap()
    }

private val MediaMetadataRetriever.title get() = extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
private val MediaMetadataRetriever.mimeType get() = extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
private val MediaMetadataRetriever.bitrate get() = extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()
private val MediaMetadataRetriever.duration get() = extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
private val MediaMetadataRetriever.year get() = extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toIntOrNull()
private val MediaMetadataRetriever.diskNumber get() = extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)?.toIntOrNull()
private val MediaMetadataRetriever.trackNumber get() = extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
private val MediaMetadataRetriever.artist get() = extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
private val MediaMetadataRetriever.album get() = extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
private val MediaMetadataRetriever.genre get() = extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
private val MediaMetadataRetriever.composer get() = extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER)
private val MediaMetadataRetriever.author get() = extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)
private val MediaMetadataRetriever.writer get() = extractMetadata(MediaMetadataRetriever.METADATA_KEY_WRITER)
private val MediaMetadataRetriever.sampleRate
    get() = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) extractMetadata(
        MediaMetadataRetriever.METADATA_KEY_SAMPLERATE
    )?.toIntOrNull() else null)
private val MediaMetadataRetriever.bitsPerSample
    get() = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) extractMetadata(
        MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE
    )?.toIntOrNull() else null)

//
class PropertiesViewModel(handle: SavedStateHandle) : KoinViewModel(), PropertiesViewState {

    val source = handle[RouteProperties]!!
    val file = File(source)

    val retriever =
        MediaMetadataRetriever().apply { setDataSource(source) }

    override val artwork: ImageBitmap? = retriever.embeddedBitmap

    override fun onCleared() {
        retriever.release()
        artwork?.asAndroidBitmap()?.recycle()
        super.onCleared()
    }

    val notAvailable = getText(R.string.abbr_not_available)

    override val title: CharSequence = retriever.title ?: notAvailable
    override val mimeType: CharSequence = retriever.mimeType ?: notAvailable
    override val year: CharSequence = "${retriever.year ?: notAvailable}"
    override val diskNumber: CharSequence = "${retriever.diskNumber ?: notAvailable}"
    override val trackNumber: CharSequence = "${retriever.trackNumber ?: notAvailable}"
    override val artist: CharSequence = retriever.artist ?: notAvailable
    override val album: CharSequence = retriever.album ?: notAvailable
    override val genre: CharSequence =  retriever.genre ?: notAvailable
    override val composer: CharSequence = retriever.composer ?: notAvailable
    override val author: CharSequence = retriever.author ?: notAvailable
    override val writer: CharSequence = retriever.writer ?: notAvailable
    override val path: CharSequence = file.path
    override val size: CharSequence = Formatter.formatShortFileSize(context, file.length())
    override val duration: CharSequence = let {
        val value = retriever.duration
        if (value == null) notAvailable else DateUtils.formatElapsedTime(value / 1000L)
    }
    override val bitrate: CharSequence = let {
        val value = retriever.bitrate
        if (value == null) notAvailable else Formatter.formatShortFileSize(context, value / 8) + "/s"
    }
    override val sampleRate: CharSequence= let {
        val value = retriever.sampleRate
        if (value == null) notAvailable else "${value}Hz"
    }

    override val bitsPerSample: CharSequence = let {
        val value = retriever.bitsPerSample
        if (value == null) notAvailable else "$value"
    }
}
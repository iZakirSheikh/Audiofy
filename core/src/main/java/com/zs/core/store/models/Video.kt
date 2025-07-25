/*
 * Copyright 2025 sheik
 *
 * Created by sheik on 09-05-2025.
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

package com.zs.core.store.models

import android.content.ContentUris
import android.database.Cursor
import android.provider.MediaStore
import com.zs.core.store.MediaProvider

/**
 * Represents a video file, extending the base [File] class.
 * It includes metadata specific to video files like duration, width, height, etc.
 *
 * @param id The unique ID of the video file.
 * @param name The name of the video file.
 * @param mimeType The MIME type of the video file, e.g., "video/mp4".
 * @param path The absolute path to the video file.
 * @param dateAdded The timestamp when the file was added, in milliseconds since epoch.
 * @param dateModified The timestamp when the file was last modified, in milliseconds since epoch.
 * @param size The size of the video file in bytes.
 * @param duration The duration of the video in milliseconds.
 * @param width The width of the video in pixels.
 * @param height The height of the video in pixels.
 * @param orientation The orientation of the video (e.g., 0, 90, 180, 270).
 *
 * @see android.provider.MediaStore.Video
 */
class Video(
    @JvmField val id: Long,
    @JvmField val name: String,
    @JvmField val mimeType: String,
    @JvmField val path: String,
    @JvmField val dateAdded: Long,
    @JvmField val dateModified: Long,
    @JvmField val size: Long,
    @JvmField val duration: Long,
    @JvmField val width: Int,
    @JvmField val height: Int,
    @JvmField val orientation: Int
) {

    val contentUri
        get() = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)

    internal constructor(cursor: Cursor) : this(
        id = cursor.getLong(0),
        name = cursor.getString(1) ?: MediaProvider.Companion.UNKNOWN_STRING,
        mimeType = cursor.getString(2),
        path = cursor.getString(3),
        dateAdded = cursor.getLong(4) * 1000,
        dateModified = cursor.getLong(5) * 1000,
        size = cursor.getLong(6),
        duration = cursor.getLong(7),
        width = cursor.getInt(8),
        height = cursor.getInt(9),
        orientation = cursor.getInt(10)
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Video

        if (id != other.id) return false
        if (dateAdded != other.dateAdded) return false
        if (dateModified != other.dateModified) return false
        if (size != other.size) return false
        if (duration != other.duration) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (orientation != other.orientation) return false
        if (name != other.name) return false
        if (mimeType != other.mimeType) return false
        if (path != other.path) return false

        return true
    }


    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + dateAdded.hashCode()
        result = 31 * result + dateModified.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + duration.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + orientation
        result = 31 * result + name.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + path.hashCode()
        return result
    }

    override fun toString(): String {
        return "Video(id=$id, name='$name', mimeType='$mimeType', path='$path', dateAdded=$dateAdded, dateModified=$dateModified, size=$size, duration=$duration, width=$width, height=$height, orientation=$orientation)"
    }
}

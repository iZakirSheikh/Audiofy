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
import android.provider.MediaStore
import com.zs.core.common.PathUtils
import com.zs.core.store.MediaProvider

/**
 * Represents a folder with associated properties.
 *
 * @property artworkID The artwork associated with the folder.
 * @property path The path of the folder.
 * @property count The count of items within the folder.
 * @property size The size of the folder in bytes.
 */
class Folder(
    @JvmField internal var artworkID: Long,
    @JvmField internal var mimeType: String,
    @JvmField var path: String,
    @JvmField var count: Int,
    @JvmField var size: Int,
    @JvmField var lastModified: Long
) {
    val artworkUri
        get() = when {
            artworkID == -1L -> null
            mimeType.startsWith("audio") -> MediaProvider.buildAlbumArtUri(artworkID)
            else -> ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, artworkID)
        }


    val name: String get() = PathUtils.name(path)
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Folder

        if (artworkID != other.artworkID) return false
        if (count != other.count) return false
        if (size != other.size) return false
        if (lastModified != other.lastModified) return false
        if (mimeType != other.mimeType) return false
        if (path != other.path) return false

        return true
    }

    override fun hashCode(): Int {
        var result = artworkID.hashCode()
        result = 31 * result + count
        result = 31 * result + size
        result = 31 * result + lastModified.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + path.hashCode()
        return result
    }

    override fun toString(): String {
        return "Folder(lastModified=$lastModified, size=$size, count=$count, path='$path', mimeType='$mimeType', artworkID=$artworkID)"
    }
}
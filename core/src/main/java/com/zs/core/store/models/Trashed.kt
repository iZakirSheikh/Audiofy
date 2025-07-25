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

import android.database.Cursor
import com.zs.core.store.MediaProvider

/**
 * Represents a Trashed file on the device.
 * @property expires The timestamp (in milliseconds) when the trashed file will be permanently deleted.
 */
data class Trashed(
    @JvmField val id: Long,
    @JvmField val name: String,
    @JvmField val mimeType: String,
    @JvmField val path: String,
    @JvmField val dateAdded: Long,
    @JvmField val dateModified: Long,
    @JvmField val size: Long,
    @JvmField val expires: Long,
) {
    internal constructor(cursor: Cursor) : this(
        id = cursor.getLong(0),
        name = cursor.getString(1) ?: MediaProvider.Companion.UNKNOWN_STRING,
        mimeType = cursor.getString(2),
        path = cursor.getString(3),
        dateAdded = cursor.getLong(4) * 1000,
        dateModified = cursor.getLong(5) * 1000,
        size = cursor.getLong(6),
        expires = cursor.getLong(7) * 1000
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Trashed

        if (id != other.id) return false
        if (dateAdded != other.dateAdded) return false
        if (dateModified != other.dateModified) return false
        if (size != other.size) return false
        if (expires != other.expires) return false
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
        result = 31 * result + expires.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + path.hashCode()
        return result
    }

    override fun toString(): String {
        return "Trashed(id=$id, name='$name', mimeType='$mimeType', path='$path', dateAdded=$dateAdded, dateModified=$dateModified, size=$size, expires=$expires)"
    }


}
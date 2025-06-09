/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 28-09-2024.
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

package com.zs.core.db.playlists

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Representing a playlist.
 *
 * @property name The name of the playlist.
 * @property id The unique ID of the playlist (auto-generated).
 * @property desc The description of the playlist (optional).
 * @property dateCreated The timestamp when the playlist was created.
 * @property dateModified The timestamp when the playlist was last modified.
 * @property count The number of media files in the playlist; defaults to -1(N/A).
 * @property artwork The artwork associated with the playlist. Default is null.
 */
@Entity(tableName = "tbl_playlists")
data class Playlist @Deprecated("This is used by Room internally") internal constructor(
    @JvmField val name: String,
    @JvmField @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "playlist_id") val id: Long = 0,
    @JvmField @ColumnInfo(defaultValue = "") val desc: String = "",
    @JvmField @ColumnInfo(name = "date_created") val dateCreated: Long = System.currentTimeMillis(),
    @JvmField @ColumnInfo(name = "date_modified") val dateModified: Long = dateCreated,
) {
    /**
     * Used by user to construct new playlist.
     */
    @Ignore
    constructor(name: String, desc: String) : this(name, 0, desc)

    /**
     * Creates a new copy of the playlist with updated details and automatically updates dateModified.
     *
     * **Note** - calling copy method is error and this should be called instead.
     */
    fun clone(name: String = this.name, desc: String = this.desc) =
        this.copy(name = name, this.id, desc = desc, this.dateCreated, System.currentTimeMillis())

    @Ignore
    var count: Int = -1
        internal set
    @Ignore
    var artwork: String? = null
        internal set

    /**
     * Representing a member (media file) within a playlist.
     *
     * @property playlistID The ID of the playlist this member belongs to.
     * @property order The playback order of this member within the playlist.
     * @property uri The URI of the media file.
     * @property title The title of the media file.
     * @property subtitle The subtitle of the media file.
     * @property artwork The artwork associated with the media file (optional).
     * @property mimeType The MIME type of the media file.
     */
    @Entity(
        tableName = "tbl_playlist_members",
        foreignKeys = [ForeignKey(
            entity = Playlist::class,
            parentColumns = ["playlist_id"],
            childColumns = ["playlist_id"],
            onDelete = CASCADE
        )],
        indices = [Index(value = ["playlist_id", "uri"], unique = false)]
    )
    data class Track @Deprecated("This is meant to be used by Room internally.") constructor(
        @JvmField @PrimaryKey(autoGenerate = true) @ColumnInfo(name = "track_id") val id: Long = 0,
        @JvmField @ColumnInfo(name = "playlist_id") var playlistID: Long,
        @JvmField @ColumnInfo(name = "play_order") var order: Int,
        @JvmField @ColumnInfo(name = "uri") val uri: String,
        @JvmField val title: String,
        @JvmField val subtitle: String,
        @JvmField @ColumnInfo(name = "artwork_uri") val artwork: String? = null,
        @JvmField @ColumnInfo(name = "mime_type") val mimeType: String? = null,
    ) {
        /**
         * Representing a member (media file) within a playlist.
         */
        constructor(
            title: String,
            subtitle: String,
            uri: String,
            artwork: String? = null,
            mimeType: String? = null
        ) : this(0L, -1L, -1, uri, title, subtitle, artwork, mimeType)
    }
}
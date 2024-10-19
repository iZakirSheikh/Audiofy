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

package com.zs.core.db

import android.content.Context
import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Ignore
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Update
import com.zs.core.db.Playlist.Track
import kotlinx.coroutines.flow.Flow

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
    @ColumnInfo(defaultValue = "") val desc: String = "",
    @JvmField @ColumnInfo(name = "date_created") val dateCreated: Long = System.currentTimeMillis(),
    @JvmField @ColumnInfo(name = "date_modified") val dateModified: Long = dateCreated,
) {
    /**
     * Used by user to construct new playlist.
     */
    @Ignore constructor(name: String, desc: String) : this(name, 0, desc)

    /**
     * Creates a new copy of the playlist with updated details and automatically updates dateModified.
     *
     * **Note** - calling copy method is error and this should be called instead.
     */
    fun clone(name: String = this.name, desc: String = this.desc) =
        this.copy(name = name, this.id, desc = desc, this.dateCreated, System.currentTimeMillis())

    @Ignore var count: Int = -1
        internal set
    @Ignore var artwork: String? = null
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
        primaryKeys = ["playlist_id", "uri"],
        foreignKeys = [ForeignKey(
            entity = Playlist::class,
            parentColumns = ["playlist_id"],
            childColumns = ["playlist_id"],
            onDelete = CASCADE
        )],
        indices = [Index(value = ["playlist_id", "uri"], unique = false)]
    )
    data class Track @Deprecated("This is meant to be used by Room internally.") constructor(
        @JvmField @ColumnInfo(name = "playlist_id") var playlistID: Long,
        @JvmField @ColumnInfo(name = "play_order") var order: Int,
        @JvmField @ColumnInfo(name = "uri") val uri: String,
        @JvmField val title: String,
        @JvmField val subtitle: String,
        @JvmField @ColumnInfo(name = "artwork_uri") val artwork: String? = null,
        @JvmField @ColumnInfo(name = "mime_type") val mimeType: String? = null,
    ){
        /**
         * Representing a member (media file) within a playlist.
         */
        constructor(title: String, subtitle: String, uri: String, artwork: String? = null, mimeType: String? = null) :
                this(-1L, -1, uri, title, subtitle, artwork, mimeType)

    }
}

@Dao
@Deprecated("This is about to be replaced by Playlists")
interface Playlists2 {

    companion object {
        /**
         * A prefix char for private playlists.
         */
        const val PRIVATE_PLAYLIST_PREFIX = '_'

        //language=RoomSql
        internal const val TRIGGER_BEFORE_INSERT =
            "CREATE TRIGGER IF NOT EXISTS trigger_reorder_insert BEFORE INSERT ON tbl_playlist_members " +
                    "BEGIN UPDATE tbl_playlist_members SET play_order = play_order + 1 " +
                    "WHERE new.playlist_id == playlist_id AND play_order >= new.play_order;" +
                    "END;"

        //language=RoomSql
        internal const val TRIGGER_AFTER_DELETE =
            "CREATE TRIGGER IF NOT EXISTS trigger_reorder_delete AFTER DELETE ON tbl_playlist_members " +
                    "BEGIN UPDATE tbl_playlist_members SET play_order = play_order - 1 " +
                    "WHERE old.playlist_id == playlist_id AND old.play_order < play_order;" +
                    "END;"

        operator fun invoke(context: Context) = Realm(context).playlists
    }

    // playlists
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(playlist: Playlist): Long


    @Query("SELECT MAX(play_order) FROM tbl_playlist_members WHERE playlist_id = :playlistId")
    suspend fun lastPlayOrder(playlistId: Long): Int?

    /**
     * Update the playlist with new details.
     * Note: Please Provide [Playlist.dateModified] and [Playlist.dateCreated].
     */
    @Update
    suspend fun update(playlist: Playlist): Int

    @Delete
    suspend fun delete(playlist: Playlist): Int

    @Delete
    suspend fun delete(playlists: List<Playlist>): Int

    /**
     * @return [Playlist] represented by [id]
     */
    @Query("SELECT * FROM tbl_playlists WHERE playlist_id == :id")
    suspend fun get(id: Long): Playlist?

    /**
     * @return [Playlist] represented by [id]
     */
    @Query("SELECT * FROM tbl_playlists WHERE name == :name")
    suspend fun get(name: String): Playlist?

    /**
     * @return All [Flow][Playlist]s matched by the query.
     */
    @Query("SELECT * FROM tbl_playlists WHERE :query IS NULL OR name LIKE '%' || :query || '%'")
    fun observe(query: String? = null): Flow<List<Playlist>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: Track): Long

    // members
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(members: List<Track>): List<Long>

    /**
     * This is not the recommended way to do it.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun update(member: Track): Long

    @Delete
    suspend fun delete(member: Track): Int

    @Delete
    suspend fun delete(members: ArrayList<Track>): Int

    @Query("SELECT * FROM tbl_playlist_members WHERE playlist_id == :playlistId AND uri == :uri")
    suspend fun get(playlistId: Long, uri: String): Track?

    /**
     * Check if the [Playlist.Member] exits in [Playlist]
     */
    @Query("SELECT EXISTS(SELECT 1 FROM tbl_playlist_members WHERE playlist_id == :playlistId AND uri == :uri)")
    suspend fun exists(playlistId: Long, uri: String): Boolean

    /**
     * Checks if [Playlist] [name] exits
     */
    @Query("SELECT EXISTS(SELECT 1 FROM tbl_playlists WHERE name == :name)")
    suspend fun exists(name: String): Boolean

    suspend fun exists(name: String, uri: String): Boolean {
        val id = get(name)?.id ?: 0
        return exists(id, uri)
    }

    /**
     * Delete the [Playlist.Member] from the [Playlist]
     */
    @Query("DELETE FROM tbl_playlist_members WHERE playlist_id == :playlistId AND uri == :uri")
    suspend fun delete(playlistId: Long, uri: String): Int


    /**
     * Delete the [Playlist.Member] where [Playlist.Member.order] > [order]
     */
    @Query("DELETE FROM tbl_playlist_members WHERE playlist_id == :playlistId AND play_order >= :order")
    suspend fun delete(playlistId: Long, order: Long): Int

    /**
     * @return [List][Audio]s of [Playlist] represented by [playlistId]
     */
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM tbl_playlist_members WHERE playlist_id = :playlistId ORDER BY play_order ASC")
    fun observe2(playlistId: Long): Flow<List<Track>>

    /**
     * Observes the [Playlist] spacified by the name.
     */
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM tbl_playlist_members LEFT JOIN tbl_playlists ON tbl_playlist_members.playlist_id == tbl_playlists.playlist_id WHERE tbl_playlists.name == :name ORDER BY tbl_playlist_members.play_order ASC")
    fun observe2(name: String): Flow<List<Track>>

    @Query("SELECT * FROM tbl_playlist_members WHERE playlist_id = :id ORDER BY play_order ASC")
    suspend fun getMembers(id: Long): List<Track>

    suspend fun getMembers(name: String): List<Track> {
        val x = get(name) ?: return emptyList()
        return getMembers(x.id)
    }
}
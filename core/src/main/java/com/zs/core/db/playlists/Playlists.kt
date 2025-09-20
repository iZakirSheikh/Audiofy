/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 19-10-2024.
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

import android.content.Context
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Update
import com.zs.core.db.Realm
import com.zs.core.db.playlists.Playlist.Track
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach

// Two new fields have been added to the Playlist API:
// - count: Represents the number of items in the playlist.
// - artwork: Represents the artwork of the last item in the playlist.

// Several approaches were considered for updating these fields:

// 1. Schema Update with Trigger: Add the fields to the database schema
//    and use a trigger to automatically update them.
//    This seems too expensive and hence dropped.
// 2. Room's @Ignore with Query: Mark the fields with `@Ignore` and
//    use a separate query to fetch and update them. This approach was
//    tested, but Room currently drops ignored fields during updates.
//    Future Room versions might provide a way to update ignored fields.
// 3. Manual Update Methods: Define dedicated methods to update the
//    fields. While this approach requires additional methods, it
//    currently appears to be the most reliable and preferred solution.

// For future reference
// @Query("SELECT p.*, COUNT(m.uri) AS count, (SELECT artwork_uri FROM tbl_playlist_members
// WHERE playlist_id = p.playlist_id ORDER BY play_order DESC LIMIT 1) AS artwork
// FROM tbl_playlists p LEFT JOIN tbl_playlist_members m ON p.playlist_id = m.playlist_id
// WHERE p.playlist_id = :id GROUP BY p.playlist_id")
////////////////////////////////////////////////////////////////////////////////////////////////////

@Dao
abstract class Playlists {

    companion object {
        /**
         * A prefix char for private playlists.
         */
        const val PRIVATE_PLAYLIST_PREFIX = '_'

        /**
         * @return an instance of [Playlists]
         */
        operator fun invoke(context: Context) = Realm(context).playlistsNew


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
    }

    /**
     * @return the cardinality of playlists stored in the table playlist
     */
    @Query("SELECT COUNT(*) FROM tbl_playlists")
    abstract suspend fun count(): Int

    /**
     * @return the number of [Track]s in Playlists [id]
     */
    @Query("SELECT COUNT(*) FROM tbl_playlist_members WHERE playlist_id = :id")
    abstract suspend fun count(id: Long): Int

    /**
     * @return artwork of the last item in the playlist represented by [id]
     */
    @Query("SELECT artwork_uri FROM tbl_playlist_members WHERE playlist_id = :id ORDER BY play_order DESC LIMIT 1")
    @Deprecated("For internal use only")
    internal abstract suspend fun _artwork(id: Long): String?

    @Query("SELECT * FROM tbl_playlists WHERE playlist_id = :id")
    @Deprecated("For internal use only")
    internal abstract suspend fun _get(id: Long): Playlist?

    suspend operator fun get(id: Long): Playlist? = _get(id)?.apply{
        count = count(id)
        artwork = _artwork(id)
    }

    @Query("SELECT * FROM tbl_playlists WHERE name = :name")
    @Deprecated("For internal use only")
    internal abstract suspend fun _get(name: String): Playlist?

    suspend operator fun get(name: String): Playlist? = _get(name)?.apply{
        count = count(id)
        artwork = _artwork(id)
    }

    /**
     * Checks if [Playlist] [name] exits
     */
    @Query("SELECT EXISTS(SELECT 1 FROM tbl_playlists WHERE name == :name)")
    abstract suspend fun exists(name: String): Boolean

    @Query("SELECT * FROM tbl_playlists WHERE (:query IS NULL OR name LIKE '%' || :query || '%') AND name NOT GLOB '_*'")
    @Deprecated("For internal use only")
    internal abstract fun _observe(query: String? = null): Flow<List<Playlist>>

    fun observe(query: String? = null) = _observe(query).onEach { playlists ->
        playlists.onEach { playlist ->
            playlist.count = count(playlist.id)
            playlist.artwork = _artwork(playlist.id)
        }
    }

    @Insert
    abstract suspend fun insert(playlist: Playlist): Long

    @Query("DELETE FROM tbl_playlists WHERE playlist_id == :id")
    abstract suspend fun delete(id: Long): Int

    @Update
    abstract suspend fun update(playlist: Playlist): Int

    /**
     * Observes the [Playlist] spacified by the name.
     */
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM tbl_playlist_members LEFT JOIN tbl_playlists ON tbl_playlist_members.playlist_id == tbl_playlists.playlist_id WHERE tbl_playlists.name == :name AND (:query IS NULL OR title LIKE '%' || :query || '%') ORDER BY tbl_playlist_members.play_order ASC")
    abstract fun observer(name: String, query: String? = null): Flow<List<Track>>

    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM tbl_playlist_members WHERE playlist_id = :id AND (:query IS NULL OR title LIKE '%' || :query || '%') ORDER BY play_order ASC ")
    abstract fun observe(id: Long, query: String? = null): Flow<List<Track>>

    @Query("SELECT * FROM tbl_playlist_members WHERE playlist_id = :id ORDER BY play_order ASC")
    abstract suspend fun getTracks(id: Long): List<Track>

    suspend fun getTracks(name: String): List<Track> {
        val x = get(name) ?: return emptyList()
        return getTracks(x.id)
    }

    @Query("DELETE FROM tbl_playlist_members WHERE playlist_id = :playlistID")
    abstract suspend fun clear(playlistID: Long): Int

    // members
    @Insert
    abstract suspend fun insert(tracks: List<Track>): List<Long>

    /**
     * Delete the [Playlist.Member] where [Playlist.Member.order] > [order]
     */
    @Query("DELETE FROM tbl_playlist_members WHERE playlist_id == :playlistId AND play_order >= :order")
    internal abstract suspend fun _delete(playlistId: Long, order: Int): Int

    @Query("SELECT * FROM tbl_playlist_members WHERE playlist_id == :playlistId AND uri == :uri")
    internal abstract suspend fun _get(playlistId: Long, uri: String): Track?

    /**
     * This is not the recommended way to do it.
     */
    @Update
    abstract suspend fun update(member: Track)

    @Query("SELECT EXISTS(SELECT 1 FROM tbl_playlist_members m INNER JOIN tbl_playlists p ON m.playlist_id == p.playlist_id WHERE p.name = :name AND m.uri = :key)")
    abstract suspend fun contains(name: String, key: String): Boolean

    /** Removes item from playlist*/
    @Query("DELETE FROM tbl_playlist_members WHERE playlist_id = :playlistID AND uri IN (:keys)")
    abstract suspend fun remove(playlistID: Long, vararg keys: String): Int

    @Query("SELECT m.uri FROM tbl_playlist_members m INNER JOIN tbl_playlists p ON m.playlist_id = p.playlist_id WHERE p.name = :playlistName")
    abstract fun observeKeysOf(playlistName: String): Flow<List<String>>

    /**
     * @return the last play order of the playlist represented by [name] or -1 if empty.
     */
    @Query(" SELECT COALESCE(MAX(m.play_order), -1) FROM tbl_playlist_members m INNER JOIN tbl_playlists p ON m.playlist_id = p.playlist_id WHERE p.name = :name")
    abstract suspend fun lastPlayOrder(name: String): Int
}
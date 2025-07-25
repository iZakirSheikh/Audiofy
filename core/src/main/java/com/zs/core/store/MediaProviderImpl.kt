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

package com.zs.core.store

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import com.zs.core.common.PathUtils
import com.zs.core.store.MediaProvider.Companion.ALBUM_PROJECTION
import com.zs.core.store.MediaProvider.Companion.ARTIST_PROJECTION
import com.zs.core.store.MediaProvider.Companion.AUDIO_PROJECTION
import com.zs.core.store.MediaProvider.Companion.COLUMN_DATE_MODIFIED
import com.zs.core.store.MediaProvider.Companion.COLUMN_ID
import com.zs.core.store.MediaProvider.Companion.COLUMN_IS_TRASHED
import com.zs.core.store.MediaProvider.Companion.COLUMN_MEDIA_TYPE
import com.zs.core.store.MediaProvider.Companion.COLUMN_NAME
import com.zs.core.store.MediaProvider.Companion.COLUMN_PATH
import com.zs.core.store.MediaProvider.Companion.EXTERNAL_CONTENT_URI
import com.zs.core.store.MediaProvider.Companion.GENRE_PROJECTION
import com.zs.core.store.MediaProvider.Companion.MEDIA_TYPE_VIDEO
import com.zs.core.store.MediaProvider.Companion.TRASHED_PROJECTION
import com.zs.core.store.MediaProvider.Companion.VIDEO_PROJECTION
import com.zs.core.store.models.Audio
import com.zs.core.store.models.Audio.Album
import com.zs.core.store.models.Audio.Artist
import com.zs.core.store.models.Audio.Genre
import com.zs.core.store.models.Folder
import com.zs.core.store.models.Trashed
import com.zs.core.store.models.Video
import kotlinx.coroutines.flow.Flow
import java.io.File

internal class MediaProviderImpl(context: Context) : MediaProvider {

    private val TAG = "MediaProviderImpl"

    // TODO - Allow for more types like podcasts etc. as well
    private val ONLY_MUSIC_SELECTION = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

    private val resolver = context.contentResolver
    override fun observer(uri: Uri): Flow<Boolean> = resolver.observe(uri)

    /**
     * Counts media items in the MediaStore based on trash state.
     *
     * @param type The filter type for counting:
     *  - 0 = all items (both trashed and non-trashed)
     *  - 1 = only trashed items
     *  - 2 = only non-trashed items
     *
     * @return The number of matching media items.
     */
    private suspend fun count(type: Int): Int {
        // Build selection condition based on the type:
        // - API 29+ supports IS_TRASHED column
        // - Pre-API 29 falls back to basic non-zero ID check
        val selection = when {
            type == 0 || Build.VERSION.SDK_INT < Build.VERSION_CODES.Q -> "$COLUMN_ID != 0"   // Count all media items
            type == 1 -> "$COLUMN_IS_TRASHED = 1"          // Count only trashed items
            else -> "$COLUMN_IS_TRASHED != 1"      // Count only non-trashed items
        }

        // Perform the query with the appropriate selection.
        // Fallback for pre-Android 10 (no IS_TRASHED support)
        return resolver.query2(
            EXTERNAL_CONTENT_URI,
            arrayOf(COLUMN_ID), // Just need IDs to count rows
            selection = selection,
            transform = { it.count } // Return row count
        )
    }

    /**
     * Fetches the content URIs for the given media IDs.
     *
     * This function queries the MediaStore to determine the typeof each media item (image or video)
     * based on the provided IDs and constructs the corresponding content URIs.
     *
     * @param ids The IDs of the media items to fetch URIs for.
     * @return A list of content URIs corresponding to the given IDs.
     */
    suspend fun fetchContentUri(vararg ids: Long): List<Uri> {
        // Create a comma-separated string of IDs for the SQL IN clause.
        val idsString = ids.joinToString(",") { it.toString() }

        // Define the projection to retrieve the ID and media type of each item.
        val projection = arrayOf(COLUMN_ID, COLUMN_MEDIA_TYPE)

        // Define the selection clause to filter items based on the provided IDs.
        val selection = "$COLUMN_ID IN ($idsString)"

        // Query the MediaStore and transform the result into a list of content URIs.
        return resolver.query2(
            EXTERNAL_CONTENT_URI, // The base content URI for media files
            projection, // The columns to retrieve
            selection, // The selection clause to filter results
            transform = { c ->
                List(c.count) { index -> // Iterate over the cursor results
                    c.moveToPosition(index) // Move to the current row
                    val type = c.getInt(1) // Get the media type (image or video)
                    // Construct the appropriate content URI based on the media type.
                    val uri = if (type == MEDIA_TYPE_VIDEO) {
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                    } else {
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    }
                    ContentUris.withAppendedId(uri, c.getLong(0)) // Append the ID to the URI
                }
            }
        )
    }

    override suspend fun delete(vararg id: Long): Int {
        // Create a comma-separated string of IDs for the SQL IN clause.
        val idString = id.joinToString(",") { "$it" }

        // Define the projection to retrieve the file path of each item.
        val projection = arrayOf(COLUMN_PATH)

        // Define the selection clause to filter items based on the provided IDs.
        val selection = "$COLUMN_ID IN ($idString)"

        // Query the MediaStore to get the file paths of the items to be deleted.
        val paths = resolver.query2(
            EXTERNAL_CONTENT_URI,
            projection,
            selection,
            transform = { c ->
                List(c.count) {
                    c.moveToPosition(it);
                    // Get the file path
                    c.getString(0)
                }
            }
        )
        // Attempt to delete the items from the MediaStore.
        var count = resolver.delete(
            EXTERNAL_CONTENT_URI,
            "${MediaStore.MediaColumns._ID} IN ($idString)",
            null
        )

        // Error deleting from MediaStore
        if (count == 0) return -1 // error

        // Iterate over the file paths and attempt to delete them from the file system.
        Log.d(TAG, "delete: $count")
        paths.forEach {
            val file = File(it)
            // Decrement count if file deletion fails
            if (file.exists() && !File(it).delete())
                count--
        }
        Log.d(TAG, "delete: $count")

        // Return the number of successfully deleted items
        return count
    }

    override suspend fun delete(vararg uri: Uri): Int {
        if (uri.isEmpty()) return 0
        val ids = uri.map { ContentUris.parseId(it) }
        return delete(*ids.toLongArray())
    }

    @SuppressLint("NewApi")
    override suspend fun delete(activity: Activity, vararg uri: Uri): Int {
        // Create a delete request for the given URIs.
        val request = MediaStore.createDeleteRequest(resolver, uri.toList())
        if (activity is ComponentActivity) {
            // If the activity is a ComponentActivity, use Activity Result
            // APIs for detailed feedback.
            // Count items before deletion (including trashed)
            val before = count(0)
            // Launch the delete request and get the result.
            val result = activity.launchForResult(
                request.intentSender
            )
            // Handle the result of the deletion request.
            // Deletion was canceled by the user
            if (Activity.RESULT_CANCELED == result.resultCode) return -3
            // Count items after deletion (including trashed)
            val after = count(0)
            // Log for debugging if result is unexpected
            Log.d(TAG, "delete: before: $before, after: $after")
            // Return the number of deleted items
            if (Activity.RESULT_OK == result.resultCode) return before - after
            // Deletion failed for an unknown reason
            return -1
        }
        // If the activity is not a ComponentActivity, initiate the deletion without detailed feedback.
        activity.startIntentSenderForResult(request.intentSender, 100, null, 0, 0, 0)
        // Deletion request initiated, but the exact outcome is unknown
        return -2
    }

    override suspend fun delete(activity: Activity, vararg id: Long): Int {
        val uri = fetchContentUri(*id).toTypedArray()
        return delete(activity, *uri)
    }

    @SuppressLint("NewApi")
    override suspend fun trash(activity: Activity, vararg uri: Uri): Int {
        // Create a delete request for the given URIs.
        val request = MediaStore.createTrashRequest(resolver, uri.toList(), true)
        if (activity is ComponentActivity) {
            // If the activity is a ComponentActivity, use Activity Result
            // APIs for detailed feedback.
            // Count items before deletion (including trashed)
            val before = count(1)
            // Launch the delete request and get the result.
            val result = activity.launchForResult(
                request.intentSender
            )
            // Handle the result of the deletion request.
            // Deletion was canceled by the user
            if (Activity.RESULT_CANCELED == result.resultCode) return -3
            // Count items after deletion (including trashed)
            val after = count(1)
            // Log for debugging if result is unexpected
            Log.d(TAG, "trash: before: $before, after: $after")
            // Return the number of deleted items
            if (Activity.RESULT_OK == result.resultCode) return after - before
            // Deletion failed for an unknown reason
            return -1
        }
        // If the activity is not a ComponentActivity, initiate the deletion without detailed feedback.
        activity.startIntentSenderForResult(request.intentSender, 100, null, 0, 0, 0)
        // Deletion request initiated, but the exact outcome is unknown
        return -2
    }

    override suspend fun trash(activity: Activity, vararg id: Long): Int {
        val uri = fetchContentUri(*id).toTypedArray()
        return trash(activity, *uri)
    }

    @SuppressLint("NewApi")
    override suspend fun restore(activity: Activity, vararg uri: Uri): Int {
        // Create a delete request for the given URIs.
        val request = MediaStore.createTrashRequest(resolver, uri.toList(), false)
        if (activity is ComponentActivity) {
            // If the activity is a ComponentActivity, use Activity Result
            // APIs for detailed feedback.
            // Count items before deletion (including trashed)
            val before = count(1)
            // Launch the delete request and get the result.
            val result = activity.launchForResult(
                request.intentSender
            )
            // Handle the result of the deletion request.
            // Deletion was canceled by the user
            if (Activity.RESULT_CANCELED == result.resultCode) return -3
            // Count items after deletion (including trashed)
            val after = count(1)
            // Log for debugging if result is unexpected
            Log.d(TAG, "restored: before: $before, after: $after")
            // Return the number of deleted items
            if (Activity.RESULT_OK == result.resultCode) return before - after
            // Deletion failed for an unknown reason
            return -1
        }
        // If the activity is not a ComponentActivity, initiate the deletion without detailed feedback.
        activity.startIntentSenderForResult(request.intentSender, 100, null, 0, 0, 0)
        // Deletion request initiated, but the exact outcome is unknown
        return -2
    }

    override suspend fun restore(activity: Activity, vararg id: Long): Int {
        val uri = fetchContentUri(*id).toTypedArray()
        return restore(activity, *uri)
    }

    override suspend fun fetchAudioFiles(
        filter: String?,
        order: String,
        ascending: Boolean,
        parent: String?,
        offset: Int,
        limit: Int
    ): List<Audio> {
        // TODO - Consider allowing users to specify mediaType as a parameter to customize
        //  the query.
        // Compose selection criteria based on user's input and filter settings.
        // On Android 10 and above, remove trashed items from the query to comply with scoped storage restrictions.
        // language = SQL
        val selection = buildString {
            append(ONLY_MUSIC_SELECTION)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                append(" AND $COLUMN_IS_TRASHED != 1")
            // Filter by parent directory if provided.
            // if (parent != null) append(" AND $COLUMN_PATH LIKE ?")
            // Add name filter if provided.
            if (filter != null)
                append(" AND $COLUMN_NAME || ${MediaProvider.COLUMN_AUDIO_ARTIST} || ${MediaProvider.COLUMN_AUDIO_ALBUM} LIKE ?")
        }
        return resolver.query2(
            uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection = AUDIO_PROJECTION,
            ascending = ascending,
            selection = selection,
            args = if (filter != null) arrayOf("%$filter%") else null,
            order = order,
            offset = offset,
            limit = limit,
            transform = { c ->
              val list =   MutableList(c.count) {
                    c.moveToPosition(it)
                    Audio(c)
                }
                if (parent == null)
                    return@query2 list
                // This section filters the results, retaining only files that are direct children
                // of the specified folder. Files residing in subfolders are excluded.
                // TODO: Investigate using a 'LIKE' clause to initially filter non-matching paths,
                //  and then refine further to exclude subfolders.
                list.filter { PathUtils.parent(it.path) == parent }
            },
        )
    }

    override suspend fun fetchAudioFiles(
        vararg ids: Long,
        order: String,
        ascending: Boolean,
        offset: Int,
        limit: Int
    ): List<Audio> {
        if (ids.isEmpty()) return emptyList()
        val idsString = ids.joinToString(",") { "$it" }
        return resolver.query2(
            uri = EXTERNAL_CONTENT_URI,
            projection = AUDIO_PROJECTION,
            ascending = ascending,
            selection = "$COLUMN_ID IN ($idsString)",
            // provide args if available.
            args = null,
            order = order,
            offset = offset,
            limit = limit,
            transform = { c ->
                List(c.count) {
                    c.moveToPosition(it);
                    Audio(c)
                }
            },
        )
    }

    override suspend fun fetchVideoFiles(
        filter: String?,
        order: String,
        ascending: Boolean,
        parent: String?,
        offset: Int,
        limit: Int
    ): List<Video> {
        // Compose selection criteria based on user's input and filter settings.
        // On Android 10 and above, remove trashed items from the query to comply with scoped storage restrictions.
        // language = SQL
        val selection = buildString {
            append("($COLUMN_MEDIA_TYPE = ${MEDIA_TYPE_VIDEO})")
            // Filter out trashed items on Android 10+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                append(" AND $COLUMN_IS_TRASHED != 1")
            if (parent != null)
                append(" AND $COLUMN_PATH LIKE ?")
            if (filter != null)
                append(" AND $COLUMN_NAME LIKE ?")
        }

        // query for files.
        return resolver.query2(
            uri = EXTERNAL_CONTENT_URI,
            projection = VIDEO_PROJECTION,
            ascending = ascending,
            selection = selection,
            // provide args if available.
            args = when {
                filter != null && parent != null -> arrayOf("$parent%", "%$filter%")
                filter == null && parent != null -> arrayOf("$parent%")
                filter != null && parent == null -> arrayOf("%$filter%")
                else -> null // when both are null
            },
            order = order,
            offset = offset,
            limit = limit,
            transform = { c ->
                List(c.count) {
                    c.moveToPosition(it);
                    Video(c)
                }
            },
        )
    }

    override suspend fun fetchVideoFiles(
        vararg ids: Long,
        order: String,
        ascending: Boolean,
        offset: Int,
        limit: Int
    ): List<Video> {
        if (ids.isEmpty()) return emptyList()
        val idsString = ids.joinToString(",") { "$it" }
        return resolver.query2(
            uri = EXTERNAL_CONTENT_URI,
            projection = VIDEO_PROJECTION,
            ascending = ascending,
            selection = "$COLUMN_ID IN ($idsString)",
            // provide args if available.
            args = null,
            order = order,
            offset = offset,
            limit = limit,
            transform = { c ->
                List(c.count) {
                    c.moveToPosition(it);
                    Video(c)
                }
            },
        )
    }




    override suspend fun fetchAudioFolders(
        filter: String?,
        ascending: Boolean,
        offset: Int,
        limit: Int
    ): List<Folder> {
        // The selection to fetch all folders from the MediaStore.
        // FixMe - For Android versions below API 10, consider using GroupBy, Count, etc.
        //         In Android 10 and above, we rely on this current implementation.
        //         Additionally, explore ways to optimize performance for faster results.
        // Compose the selection for folders; exclude trashed items for Android 11 and above.
        //language = SQL
        //TODO - consider making this not music only
        val selection = buildString {
            append("($ONLY_MUSIC_SELECTION)")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                append(" AND $COLUMN_IS_TRASHED != 1")
            if (filter != null)
                append(" AND $COLUMN_NAME LIKE ?")
        }
        // Query MediaStore to fetch audio file information and group them by their parent folder path.
        return resolver.query2(
            MediaProvider.EXTERNAL_AUDIO_URI,
            MediaProvider.AUDIO_FOLDER_PROJECTION,
            selection = selection,
            if (filter != null) arrayOf("%$filter%") else null,
            order = COLUMN_DATE_MODIFIED,
            ascending = ascending
        ) { c ->
            val list = ArrayList<Folder>()
            while (c.moveToNext()) {
                val parent = PathUtils.parent(c.getString(1))
                val id = c.getLong(0)
                val size = c.getInt(2)
                val lastModified = c.getLong(3) * 1000
                val index = list.indexOfFirst { it.path == parent }
                // If this folder is already in the map, update its aggregated data.
                if (index == -1) {
                    list += Folder(id, "audio/*", parent, 1, size, lastModified)
                    continue
                }
                val old = list[index]
                val artwork = if (old.lastModified > lastModified) old.artworkID else id
                old.artworkID = artwork
                old.count = old.count + 1
                old.size = old.size + size
                old.lastModified = maxOf(old.lastModified, lastModified)
                old.mimeType = "audio/*"
            }
            list
        }
    }

    override suspend fun fetchVideoFolders(
        filter: String?,
        ascending: Boolean,
        offset: Int,
        limit: Int
    ): List<Folder> {
        // The selection to fetch all folders from the MediaStore.
        // FixMe - For Android versions below API 10, consider using GroupBy, Count, etc.
        //         In Android 10 and above, we rely on this current implementation.
        //         Additionally, explore ways to optimize performance for faster results.
        // Compose the selection for folders; exclude trashed items for Android 11 and above.
        //language = SQL
        //TODO - consider making this not music only
        val selection = buildString {
            append("($COLUMN_ID != 0)")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                append(" AND $COLUMN_IS_TRASHED != 1")
            if (filter != null)
                append(" AND $COLUMN_PATH LIKE ?")
        }
        // Query MediaStore to fetch audio file information and group them by their parent folder path.
        return resolver.query2(
            MediaProvider.EXTERNAL_VIDEO_URI,
            MediaProvider.VIDEO_FOLDER_PROJECTION,
            selection = selection,
            if (filter != null) arrayOf("%$filter%") else null,
            order = COLUMN_DATE_MODIFIED,
            ascending = ascending
        ) { c ->
            val list = ArrayList<Folder>()
            while (c.moveToNext()) {
                val parent = PathUtils.parent(c.getString(1))
                val id = c.getLong(0)
                val size = c.getInt(2)
                val lastModified = c.getLong(3) * 1000
                val index = list.indexOfFirst { it.path == parent }
                // If this folder is already in the map, update its aggregated data.
                if (index == -1) {
                    list += Folder(id, "video/*", parent, 1, size, lastModified)
                    continue
                }
                val old = list[index]
                val artwork = if (old.lastModified > lastModified) old.artworkID else id
                old.artworkID = artwork
                old.count = old.count + 1
                old.size = old.size + size
                old.lastModified = maxOf(old.lastModified, lastModified)
                old.mimeType = "video/*"
            }
            list
        }
    }

    private suspend inline fun ContentResolver.getBucketAudios(
        selection: String,
        args: Array<String>?,
        order: String = MediaStore.Audio.Media.TITLE,
        ascending: Boolean = true,
        offset: Int = 0,
        limit: Int = Int.MAX_VALUE
    ): List<Audio> {
        return query2(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection = AUDIO_PROJECTION,
            selection = selection,
            args,
            order,
            ascending,
            offset,
            limit,
            transform = { c ->
                List(c.count) {
                    c.moveToPosition(it);
                    Audio(c)
                }
            },
        )
    }

    override suspend fun fetchArtists(
        filter: String?,
        order: String,
        ascending: Boolean,
        offset: Int,
        limit: Int
    ): List<Artist> {
        return resolver.query2(
            MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
            projection = ARTIST_PROJECTION,
            selection = if (filter == null) "${MediaStore.Audio.Artists._ID} != 0" else "${MediaStore.Audio.Artists.ARTIST} LIKE ?",
            if (filter != null) arrayOf("%$filter%") else null,
            order,
            ascending = ascending,
            offset = offset,
            limit = limit,
        ) { c ->
            List(c.count) {
                c.moveToPosition(it)
                Artist(c)
            }
        }
    }

    override suspend fun getArtist(id: Long): Artist {
        return resolver.query2(
            MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
            projection = ARTIST_PROJECTION,
            selection = "${MediaStore.Audio.Artists._ID} == $id",
            limit = 1,
        ) { c ->
            c.moveToPosition(0)
            Artist(c)
        }
    }

    override suspend fun fetchAlbums(
        filter: String?,
        order: String,
        ascending: Boolean,
        offset: Int,
        limit: Int
    ): List<Album> {
        return resolver.query2(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            projection = ALBUM_PROJECTION,
            selection = if (filter == null) "${MediaStore.Audio.Media._ID} != 0" else "${MediaStore.Audio.Albums.ALBUM} LIKE ?",
            if (filter != null) arrayOf("%$filter%") else null,
            order,
            ascending = ascending,
            offset = offset,
            limit = limit,
        ) { c ->
            List(c.count) {
                c.moveToPosition(it)
                Album(c)
            }
                // FixMe: The albums are not distinct.
                // For now apply the function
                .distinctBy { it.title }
        }
    }

    override suspend fun getAlbum(id: Long): Album {
        return resolver.query2(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            projection = ALBUM_PROJECTION,
            selection ="${MediaStore.Audio.Albums.ALBUM_ID} == $id",
            limit = 1,
        ) { c ->
            c.moveToNext()
            Album(c)
        }
    }

    override suspend fun fetchGenres(
        filter: String?,
        order: String,
        ascending: Boolean,
        offset: Int,
        limit: Int
    ): List<Genre> {
        return resolver.query2(
            MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
            projection = GENRE_PROJECTION,
            selection = if (filter == null) "${MediaStore.Audio.Genres._ID} != 0" else "${MediaStore.Audio.Genres.NAME} LIKE ?",
            if (filter != null) arrayOf("%$filter%") else null,
            order,
            ascending = ascending,
            offset = offset,
            limit = limit,
        ) { c ->
            List(c.count) {
                c.moveToPosition(it)
                Genre(c)
            }
        }
    }

    override suspend fun getGenre(id: Long): Genre {
        return resolver.query2(
            MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
            projection = GENRE_PROJECTION,
            selection ="${MediaStore.Audio.Genres._ID} == $id",
            limit = 1,
        ) { c ->
            c.moveToNext()
            Genre(c)
        }
    }

    override suspend fun fetchTrashedFiles(
        offset: Int,
        limit: Int
    ): List<Trashed> {
        // TODO - Add selection for only Audios and Videos
        return resolver.query2(
            EXTERNAL_CONTENT_URI,
            TRASHED_PROJECTION,
            selection = "$COLUMN_IS_TRASHED = 1",
            offset = offset,
            limit = limit,
            order = MediaProvider.COLUMN_DATE_EXPIRES,
            ascending = false,
            transform = { c ->
                List(c.count) { index ->
                    c.moveToPosition(index)
                    Trashed(c)
                }
            }
        )
    }

    override suspend fun fetchAlbumAudios(
        id: Long,
        filter: String?,
        order: String,
        ascending: Boolean,
        offset: Int,
        limit: Int
    ): List<Audio> {
        val like = if (filter != null) " AND ${MediaStore.Audio.Media.TITLE} LIKE ?" else ""
        val selection = "${MediaStore.Audio.Media.ALBUM_ID} == ?" + like
        val args = if (filter != null) arrayOf("$id", "%$filter%") else arrayOf("$id")
        return resolver.getBucketAudios(selection, args, order, ascending, offset, limit)
    }

    override suspend fun fetchArtistAudios(
        id: Long,
        filter: String?,
        order: String,
        ascending: Boolean,
        offset: Int,
        limit: Int
    ): List<Audio> {
        val like = if (filter != null) " AND ${MediaStore.Audio.Media.TITLE} LIKE ?" else ""
        val selection = "${MediaStore.Audio.Media.ARTIST_ID} == ?" + like
        val args = if (filter != null) arrayOf("$id", "%$filter%") else arrayOf("$id")
        return resolver.getBucketAudios(selection, args, order, ascending, offset, limit)
    }

    override suspend fun fetchGenreAudios(
        id: Long,
        filter: String?,
        order: String,
        ascending: Boolean,
        offset: Int,
        limit: Int
    ): List<Audio> {
        // calculate the ids.
        val list = resolver.query2(
            MediaStore.Audio.Genres.Members.getContentUri("external", id),
            arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID),
        ) { c ->
            if (c.count == 0) return emptyList()
            val buffer = StringBuilder()
            while (c.moveToNext()) {
                if (!c.isFirst) buffer.append(",")
                val element = c.getLong(0)
                buffer.append("'$element'")
            }
            buffer.toString()
        }

        val like = if (filter != null) " AND ${MediaStore.Audio.Media.TITLE} LIKE ?" else ""
        return resolver.query2(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            AUDIO_PROJECTION,
            //language=SQL
            ONLY_MUSIC_SELECTION + " AND ${MediaStore.Audio.Media._ID} IN ($list)" + like,
            if (filter != null) arrayOf("%$filter%") else null,
            order,
            ascending,
            offset,
            limit
        ) { c ->
            List(c.count) {
                c.moveToPosition(it)
                Audio(c)
            }
        }
    }


}
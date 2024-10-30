/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 21-09-2024.
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
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import com.zs.core.store.MediaProvider.Companion.COLUMN_DATE_MODIFIED
import com.zs.core.store.MediaProvider.Companion.COLUMN_ID
import com.zs.core.store.MediaProvider.Companion.COLUMN_IS_TRASHED
import com.zs.core.store.MediaProvider.Companion.COLUMN_MEDIA_TYPE
import com.zs.core.store.MediaProvider.Companion.COLUMN_NAME
import com.zs.core.store.MediaProvider.Companion.COLUMN_PATH
import com.zs.core.store.MediaProvider.Companion.COLUMN_SIZE
import com.zs.core.store.MediaProvider.Companion.EXTERNAL_CONTENT_URI
import com.zs.core.store.MediaProvider.Companion.MEDIA_TYPE_AUDIO
import com.zs.core.store.MediaProvider.Companion.MEDIA_TYPE_VIDEO
import com.zs.core.util.PathUtils
import kotlinx.coroutines.flow.Flow
import java.io.File as JavaFile

private const val TAG = "MediaProviderImpl"

/**
 * Counts the number of media items in the MediaStore.
 *
 * @param trashed Whether to include trashed items in the count. Defaults to false.
 * @return The number of media items.
 */
private suspend fun ContentResolver.count(trashed: Boolean = false): Int {
    val noTrashSelection =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) "$COLUMN_IS_TRASHED != 1" else ""
    return query2(
        EXTERNAL_CONTENT_URI,
        arrayOf(COLUMN_ID),
        selection = if (!trashed) noTrashSelection else "",
        transform = { c ->
            c.count
        },
    )
}


private const val ONLY_MUSIC_SELECTION = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

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

internal class MediaProviderImpl(context: Context) : MediaProvider {
    private val resolver = context.contentResolver
    override fun observer(uri: Uri): Flow<Boolean> = resolver.observe(uri)
    override fun register(uri: Uri, onChanged: () -> Unit): ContentObserver =
        resolver.register(uri, onChanged)

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
        paths.forEach {
            // Decrement count if file deletion fails
            if (!JavaFile(it).delete())
                count--
        }

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
            val before = resolver.count(true)
            // Launch the delete request and get the result.
            val result = activity.launchForResult(
                request.intentSender
            )
            // Handle the result of the deletion request.
            // Deletion was canceled by the user
            if (Activity.RESULT_CANCELED == result.resultCode) return -3
            // Count items after deletion (including trashed)
            val after = resolver.count(true)
            // Return the number of deleted items
            if (Activity.RESULT_OK == result.resultCode) return after - before
            // Log for debugging if result is unexpected
            Log.d(TAG, "delete: before: $before, after: $after")
            // Deletion failed for an unknown reason
            return -1
        }
        // If the activity is not a ComponentActivity, initiate the deletion without detailed feedback.
        activity.startIntentSenderForResult(request.intentSender, 100, null, 0, 0, 0)
        // Deletion request initiated, but the exact outcome is unknown
        return -2
    }

    override suspend fun delete(activity: Activity, vararg id: Long): Int {
        val uri = resolver.fetchContentUri(*id).toTypedArray()
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
            val before = resolver.count(false)
            // Launch the delete request and get the result.
            val result = activity.launchForResult(
                request.intentSender
            )
            // Handle the result of the deletion request.
            // Deletion was canceled by the user
            if (Activity.RESULT_CANCELED == result.resultCode) return -3
            // Count items after deletion (including trashed)
            val after = resolver.count(false)
            // Return the number of deleted items
            if (Activity.RESULT_OK == result.resultCode) return before - after
            // Log for debugging if result is unexpected
            Log.d(TAG, "delete: before: $before, after: $after")
            // Deletion failed for an unknown reason
            return -1
        }
        // If the activity is not a ComponentActivity, initiate the deletion without detailed feedback.
        activity.startIntentSenderForResult(request.intentSender, 100, null, 0, 0, 0)
        // Deletion request initiated, but the exact outcome is unknown
        return -2
    }

    override suspend fun trash(activity: Activity, vararg id: Long): Int {
        val uri = resolver.fetchContentUri(*id).toTypedArray()
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
            val before = resolver.count(true)
            // Launch the delete request and get the result.
            val result = activity.launchForResult(
                request.intentSender
            )
            // Handle the result of the deletion request.
            // Deletion was canceled by the user
            if (Activity.RESULT_CANCELED == result.resultCode) return -3
            // Count items after deletion (including trashed)
            val after = resolver.count(true)
            // Return the number of deleted items
            if (Activity.RESULT_OK == result.resultCode) return after - before
            // Log for debugging if result is unexpected
            Log.d(TAG, "delete: before: $before, after: $after")
            // Deletion failed for an unknown reason
            return -1
        }
        // If the activity is not a ComponentActivity, initiate the deletion without detailed feedback.
        activity.startIntentSenderForResult(request.intentSender, 100, null, 0, 0, 0)
        // Deletion request initiated, but the exact outcome is unknown
        return -2
    }

    override suspend fun restore(activity: Activity, vararg id: Long): Int {
        val uri = resolver.fetchContentUri(*id).toTypedArray()
        return restore(activity, *uri)
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

    override suspend fun fetchVideoFiles(
        filter: String?,
        order: String,
        ascending: Boolean,
        parent: String?,
        offset: Int,
        limit: Int
    ): List<Video> {
        // TODO - Consider allowing users to specify mediaType as a parameter to customize
        //  the query.
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

    override suspend fun fetchAlbumAudios(
        name: String,
        filter: String?,
        order: String,
        ascending: Boolean,
        offset: Int,
        limit: Int
    ): List<Audio> {
        val like = if (filter != null) " AND ${MediaStore.Audio.Media.TITLE} LIKE ?" else ""
        val selection = "${MediaStore.Audio.Media.ALBUM} == ?" + like
        val args = if (filter != null) arrayOf(name, "%$filter%") else arrayOf(name)
        return resolver.getBucketAudios(selection, args, order, ascending, offset, limit)
    }

    override suspend fun fetchArtistAudios(
        name: String,
        filter: String?,
        order: String,
        ascending: Boolean,
        offset: Int,
        limit: Int
    ): List<Audio> {
        val like = if (filter != null) " AND ${MediaStore.Audio.Media.TITLE} LIKE ?" else ""
        val selection = "${MediaStore.Audio.Media.ARTIST} == ?" + like
        val args = if (filter != null) arrayOf(name, "%$filter%") else arrayOf(name)
        return resolver.getBucketAudios(selection, args, order, ascending, offset, limit)
    }

    override suspend fun fetchGenreAudios(
        name: String,
        filter: String?,
        order: String,
        ascending: Boolean,
        offset: Int,
        limit: Int
    ): List<Audio> {
        //maybe for api 30 we can use directly the genre name.
        // find the id.
        val id = resolver.query2(
            MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Audio.Genres._ID),
            "${MediaStore.Audio.Genres.NAME} == ?",
            arrayOf(name),
            limit = 1
        ) {
            if (it.count == 0) return emptyList()
            it.moveToPosition(0)
            it.getLong(0)
        }

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

    override suspend fun fetchTrashedFiles(offset: Int, limit: Int): List<Trashed> {
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
            append("(${COLUMN_MEDIA_TYPE} = $MEDIA_TYPE_AUDIO) AND $ONLY_MUSIC_SELECTION")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                append(" AND $COLUMN_IS_TRASHED != 1")
            // Filter by parent directory if provided.
            if (parent != null) append(" AND $COLUMN_PATH LIKE ?")
            // Add name filter if provided.
            if (filter != null)
                append(" AND $COLUMN_NAME LIKE ?")
        }
        return resolver.getBucketAudios(
            selection = selection,
            args = when {
                filter != null && parent != null -> arrayOf("$parent%", "%$filter%")
                filter == null && parent != null -> arrayOf("$parent%")
                filter != null && parent == null -> arrayOf("%$filter%")
                else -> null // when both are null
            },
            order = order,
            offset = offset,
            limit = limit,
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

    override suspend fun fetchFolders(
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
        val selection =
            "(${COLUMN_MEDIA_TYPE} = $MEDIA_TYPE_AUDIO OR $COLUMN_MEDIA_TYPE = ${MEDIA_TYPE_VIDEO})" +
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) " AND $COLUMN_IS_TRASHED != 1" else "" +
                            if (filter != null) " AND $COLUMN_NAME LIKE ?" else ""
        return resolver.query2(
            EXTERNAL_CONTENT_URI,
            arrayOf(COLUMN_ID, COLUMN_PATH, COLUMN_SIZE, COLUMN_DATE_MODIFIED),
            selection = selection,
            if (filter != null) arrayOf("%$filter%") else null,
            order = COLUMN_DATE_MODIFIED,
            ascending = ascending
        ) { c ->
            val list = ArrayList<Folder>()
            while (c.moveToNext()) {
                val path = c.getString(1)
                val parent = PathUtils.parent(path).let {
                    if (PathUtils.name(it).startsWith("img", true)) PathUtils.parent(it)
                    else it
                }
                val id = c.getLong(0)
                val size = c.getInt(2)
                val lastModified = c.getLong(3)
                val index = list.indexOfFirst { it.path == parent }
                if (index == -1) {
                    list += Folder(id, parent, 1, size, lastModified)
                    continue
                }
                val old = list[index]
                val artwork = if (old.lastModified > lastModified) old.artworkID else id
                list[index] = Folder(
                    artwork,
                    parent,
                    old.count + 1,
                    old.size + size,
                    maxOf(old.lastModified, lastModified)
                )
            }
            list
        }
    }

    override suspend fun fetchArtists(
        filter: String?,
        order: String,
        ascending: Boolean,
        offset: Int,
        limit: Int
    ): List<Artist> {
        TODO("Not yet implemented")
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

    override suspend fun fetchGenres(
        filter: String?,
        order: String,
        ascending: Boolean,
        offset: Int,
        limit: Int
    ): List<Genre> {
        TODO("Not yet implemented")
    }
}
/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 20-07-2024.
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
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Intent
import android.content.IntentSender
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.withContext as using

private const val TAG = "resolver-ext"

private const val DUMMY_SELECTION = "${MediaStore.Audio.Media._ID} != 0"

/**
 * Register [ContentObserver] for change in [uri]
 */
internal inline fun ContentResolver.register(
    uri: Uri, crossinline onChanged: () -> Unit
): ContentObserver {
    val observer = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) {
            onChanged()
        }
    }
    registerContentObserver(uri, false, observer)
    return observer
}

/**
 * Register an observer class that gets callbacks when data identified by a given content URI
 * changes.
 */
internal fun ContentResolver.observe(uri: Uri) = callbackFlow {
    val observer = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) {
            trySend(selfChange)
        }
    }
    registerContentObserver(uri, true, observer)
    // trigger first.
    trySend(false)
    awaitClose {
        unregisterContentObserver(observer)
    }
}

/**
 * An advanced version of [ContentResolver.query] with additional features.
 *
 * This function performs a query on the given [uri] using the specified parameters.
 *
 * @param uri The URI to query.
 * @param projection The list of columns to include in the result. Default is `null`, which returns all columns.
 * @param selection The selection criteria. Default is [DUMMY_SELECTION], which retrieves all rows.
 * @param args The selection arguments. Default is `null`.
 * @param order The column name to use for ordering the results. Default is [MediaStore.MediaColumns._ID].
 * @param ascending Specifies the sorting order of the results. Default is `true`, which sorts in ascending order.
 * @param offset The offset index to start retrieving the results. Default is `0`.
 * @param limit The maximum number of results to retrieve. Default is [Int.MAX_VALUE].
 * @return A [Cursor] object representing the query results.
 * @throws NullPointerException if the returned cursor is null.
 * @see ContentResolver.query
 */
@SuppressLint("Recycle")
internal suspend fun ContentResolver.query2(
    uri: Uri,
    projection: Array<String>? = null,
    selection: String = DUMMY_SELECTION,
    args: Array<String>? = null,
    order: String = MediaStore.MediaColumns._ID,
    ascending: Boolean = true,
    offset: Int = 0,
    limit: Int = Int.MAX_VALUE
): Cursor {
    return using(Dispatchers.IO) {
        // Use the modern query approach for devices running Android 10 and above
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Compose the arguments for the query
            val args2 = Bundle().apply {
                // Set the limit and offset for pagination
                putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                putInt(ContentResolver.QUERY_ARG_OFFSET, offset)

                // Set the sort order
                putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(order))
                putInt(
                    ContentResolver.QUERY_ARG_SORT_DIRECTION,
                    if (ascending)
                        ContentResolver.QUERY_SORT_DIRECTION_ASCENDING
                    else
                        ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                )
                // Set the selection arguments and selection string
                if (args != null) putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, args)
                // Add the selection string.
                // TODO: Consider adding support for group by.
                // Currently, using group by on Android 10results in errors,
                // and the argument for group by is only supported on Android 11 and above.
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
                // Include trashed files in the result for devices
                // running Android 11 and above
                // The presence of trashed files will be controlled by the 'isTrashed'
                // column in the result.
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    putInt(MediaStore.QUERY_ARG_MATCH_TRASHED, 1)
                }
            }
            query(uri, projection, args2, null)
        }
        // Fallback to the traditional query approach for devices running older Android versions
        else {
            // Construct the ORDER BY clause with limit and offset
            //language=SQL
            val order2 =
                order + (if (ascending) " ASC" else " DESC") + " LIMIT $limit OFFSET $offset"
            // Perform the query with the traditional approach
            query(uri, projection, selection, args, order2)
        }
    } ?: throw NullPointerException("Can't retrieve cursor for $uri")

}


/**
 * @see query2
 */
internal suspend inline fun <T> ContentResolver.query2(
    uri: Uri,
    projection: Array<String>? = null,
    selection: String = DUMMY_SELECTION,
    args: Array<String>? = null,
    order: String = MediaStore.MediaColumns._ID,
    ascending: Boolean = true,
    offset: Int = 0,
    limit: Int = Int.MAX_VALUE,
    transform: (Cursor) -> T
): T = query2(uri, projection, selection, args, order, ascending, offset, limit).use(transform)

/**
 * Launches an activity for result using the provided [request] [IntentSender].
 *
 * @param request The [IntentSender] to launch.
 * @return An [ActivityResult] wrapped in a [suspendCoroutine].
 */
internal suspend fun ComponentActivity.launchForResult(
    request: IntentSender
): ActivityResult =
    suspendCoroutine { cont ->
        var launcher: ActivityResultLauncher<IntentSenderRequest>? = null
        // Assign result to launcher in such a way tha it allows us to
        // unregister later.
        val contract = ActivityResultContracts.StartIntentSenderForResult()
        val key = UUID.randomUUID().toString()
        launcher = activityResultRegistry.register(key, contract) { it ->
            // unregister launcher
            launcher?.unregister()
            Log.d(TAG, "launchForResult: $it")
            cont.resume(it)
        }
        // Create an IntentSenderRequest object from the IntentSender object
        val intentSenderRequest = IntentSenderRequest.Builder(request).setFlags(
            Intent.FLAG_GRANT_WRITE_URI_PERMISSION,
            0
        ).build()
        // Launch the activity for result using the IntentSenderRequest object
        launcher.launch(intentSenderRequest)
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
internal suspend fun ContentResolver.fetchContentUri(vararg ids: Long): List<Uri> {
    // Create a comma-separated string of IDs for the SQL IN clause.
    val idsString = ids.joinToString(",") { it.toString() }

    // Define the projection to retrieve the ID and media type of each item.
    val projection = arrayOf(MediaProvider.COLUMN_ID, MediaProvider.COLUMN_MEDIA_TYPE)

    // Define the selection clause to filter items based on the provided IDs.
    val selection = "${MediaProvider.COLUMN_ID} IN ($idsString)"

    // Query the MediaStore and transform the result into a list of content URIs.
    return query2(
        MediaProvider.EXTERNAL_CONTENT_URI, // The base content URI for media files
        projection, // The columns to retrieve
        selection, // The selection clause to filter results
        transform = { c ->
            List(c.count) { index -> // Iterate over the cursor results
                c.moveToPosition(index) // Move to the current row
                val type = c.getInt(1) // Get the media type (image or video)
                // Construct the appropriate content URI based on the media type.
                val uri = when (type) {
                    MediaProvider.MEDIA_TYPE_IMAGE -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
                    MediaProvider.MEDIA_TYPE_AUDIO -> MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
                    else -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
                }
                ContentUris.withAppendedId(uri, c.getLong(0)) // Append the ID to the URI
            }
        }
    )
}


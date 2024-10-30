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

import android.annotation.TargetApi
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import kotlinx.coroutines.flow.Flow

private const val TAG = "MediaProvider"

private val sArtworkUri = Uri.parse("content://media/external/audio/albumart")

/**
 * Provides access to media files on the device.
 *
 * This interface defines methods for fetching, searching, and managing media files.
 * Implementations of this interface can be used to interact with different media sources,
 * such as the local filesystem, cloud storage, or external devices.
 */
interface MediaProvider {

    /**
     * Retrieves a list of media files from the provider.
     */
    companion object {
        const val COLUMN_ID = MediaStore.Files.FileColumns._ID
        const val COLUMN_NAME = MediaStore.Files.FileColumns.TITLE
        const val COLUMN_MIME_TYPE = MediaStore.Files.FileColumns.MIME_TYPE
        const val COLUMN_PATH = MediaStore.Files.FileColumns.DATA
        const val COLUMN_DATE_ADDED = MediaStore.Files.FileColumns.DATE_ADDED
        const val COLUMN_DATE_MODIFIED = MediaStore.Files.FileColumns.DATE_MODIFIED
        const val COLUMN_SIZE = MediaStore.Files.FileColumns.SIZE
        const val COLUMN_PARENT = MediaStore.Files.FileColumns.PARENT

        /*Used only internally*/
        internal const val COLUMN_IS_TRASHED = MediaStore.Files.FileColumns.IS_TRASHED
        internal const val COLUMN_DATE_EXPIRES = MediaStore.Files.FileColumns.DATE_EXPIRES

        /*Common media properties*/
        const val COLUMN_MEDIA_DURATION = MediaStore.MediaColumns.DURATION

        /*Media Type and corresponding values*/
        const val COLUMN_MEDIA_TYPE = MediaStore.Files.FileColumns.MEDIA_TYPE
        const val MEDIA_TYPE_VIDEO = MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
        const val MEDIA_TYPE_AUDIO = MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO
        const val MEDIA_TYPE_IMAGE = MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE

        const val UNKNOWN_STRING = MediaStore.UNKNOWN_STRING

        /*Audio file properties*/
        const val COLUMN_AUDIO_NUMBER_OF_TRACKS = MediaStore.Audio.AudioColumns.TRACK
        const val COLUMN_AUDIO_ALBUM = MediaStore.Audio.AudioColumns.TRACK
        const val COLUMN_AUDIO_ARTIST = MediaStore.Audio.AudioColumns.ARTIST
        const val COLUMN_AUDIO_COMPOSER = MediaStore.Audio.AudioColumns.COMPOSER
        const val COLUMN_AUDIO_YEAR = MediaStore.Audio.AudioColumns.YEAR
        const val COLUMN_AUDIO_ALBUM_ID = MediaStore.Audio.AudioColumns.ALBUM_ID
        const val COLUMN_ALBUM_ARTIST = MediaStore.Audio.AudioColumns.ALBUM_ARTIST

        // video properties
        const val COLUMN_HEIGHT = MediaStore.Video.VideoColumns.HEIGHT
        const val COLUMN_WIDTH = MediaStore.Video.VideoColumns.WIDTH
        const val COLUMN_RESOLUTION = MediaStore.Video.VideoColumns.RESOLUTION
        const val COLUMN_ORIENTATION = MediaStore.Video.VideoColumns.ORIENTATION

        internal fun buildAlbumArtUri(id: Long) = ContentUris.withAppendedId(sArtworkUri, id)
        val EXTERNAL_CONTENT_URI = MediaStore.Files.getContentUri("external")
    }

    /**
     * Observes changes in the data identified by the given [uri] and emits the change events as a flow of booleans.
     *
     * This function registers a [ContentObserver] with the specified [uri] and emits a boolean value indicating whether
     * the observed data has changed. The flow will emit `false` immediately upon registration and subsequently emit `true`
     * whenever a change occurs.
     *
     * @param uri The content URI to observe for changes.
     * @return A flow of boolean values indicating whether the observed data has changed.
     * @see ContentResolver.registerContentObserver
     */
    fun observer(uri: Uri): Flow<Boolean>

    /**
     * Registers a [ContentObserver] to receive notifications for changes in the specified [uri].
     *
     * This function registers a [ContentObserver] with the given [uri] and invokes the [onChanged]
     * callback whenever a change occurs.
     *
     * @param uri The URI to monitor for changes.
     * @param onChanged The callback function to be invoked when a change occurs.
     * @return The registered [ContentObserver] instance.
     * @see ContentResolver.registerContentObserver
     */
    fun register(uri: Uri, onChanged: () -> Unit): ContentObserver

    /**
     * Retrieves media files of type [Audio] from the [MediaStore].
     *
     * @param filter The filter string used to match specific media files. Default is `null`, which retrieves all media files.
     * @param order The column name to sort the media files. Default is [File.COLUMN_NAME].
     * @param ascending Specifies the sorting order of the media files. Default is `true`, which sorts in ascending order.
     * @param parent The parent directory of the media files. Default is `null`, which retrieves media files from all directories.
     * @param offset The offset index to start retrieving media files. Default is `0`.
     * @param limit The maximum number of media files to retrieve. Default is [Int.MAX_VALUE].
     * @return A list of [File] objects representing the retrieved media files.
     */
    suspend fun fetchAudioFiles(
        filter: String? = null,
        order: String = COLUMN_NAME,
        ascending: Boolean = true,
        parent: String? = null,
        offset: Int = 0,
        limit: Int = Int.MAX_VALUE
    ): List<Audio>


    /**
     * @see fetchAudioFiles
     */
    suspend fun fetchVideoFiles(
        filter: String? = null,
        order: String = COLUMN_NAME,
        ascending: Boolean = true,
        parent: String? = null,
        offset: Int = 0,
        limit: Int = Int.MAX_VALUE
    ): List<Video>

    /**
     * Retrieves a list of folders based on the provided filter and sorting options.
     *
     * @param filter The filter string used to match specific folders. Default is `null`, which retrieves all folders.
     * @param ascending Specifies the sorting order of the folders. Default is `true`, which sorts in ascending order.
     * @param offset The offset index to start retrieving folders. Default is `0`.
     * @param limit The maximum number of folders to retrieve. Default is [Int.MAX_VALUE].
     * @return A list of [Folder] objects representing the retrieved folders.
     */
    suspend fun fetchFolders(
        filter: String? = null,
        ascending: Boolean = true,
        offset: Int = 0,
        limit: Int = Int.MAX_VALUE
    ): List<Folder>

    /**
     * @see fetchFolders
     */
    suspend fun fetchArtists(
        filter: String? = null,
        order: String = MediaStore.Audio.Media.ARTIST,
        ascending: Boolean = true,
        offset: Int = 0,
        limit: Int = Int.MAX_VALUE
    ): List<Artist>

    /**
     * @see fetchFolders
     */
    suspend fun fetchAlbums(
        filter: String? = null,
        order: String = MediaStore.Audio.Albums.ALBUM,
        ascending: Boolean = true,
        offset: Int = 0,
        limit: Int = Int.MAX_VALUE
    ): List<Album>

    /**
     * @see fetchFolders
     */
    suspend fun fetchGenres(
        filter: String? = null,
        order: String = MediaStore.Audio.Albums.ALBUM,
        ascending: Boolean = true,
        offset: Int = 0,
        limit: Int = Int.MAX_VALUE
    ): List<Genre>

    /**
     * Retrieves a list of trashed files.
     *
     * @param offset The number of files to skip (for pagination).
     * @param limit The maximum number of files to return (for pagination).
     * @return A list of [Trashed] objects representing the trashed files.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun fetchTrashedFiles(
        offset: Int = 0, limit: Int = Int.MAX_VALUE
    ): List<Trashed>

    /**
     * Retrieves a list of [Video] files based on their IDs.
     *
     * @param ids The IDs of the media files to fetch.
     * @param order The column to order the results by (default is [COLUMN_NAME]).
     * @param ascending Whether to sort the results in ascending order (default is true).
     * @param offset The number of files to skip (for pagination).
     * @param limit The maximum number of files to return (for pagination).
     * @return A list of [MediaFile] objects representing the fetched files.
     */
    suspend fun fetchVideoFiles(
        vararg ids: Long,
        order: String = COLUMN_NAME,
        ascending: Boolean = true,
        offset: Int = 0,
        limit: Int = Int.MAX_VALUE
    ): List<Video>

    /**
     * @see fetchVideoFiles
     */
    suspend fun fetchAudioFiles(
        vararg ids: Long,
        order: String = COLUMN_NAME,
        ascending: Boolean = true,
        offset: Int = 0,
        limit: Int = Int.MAX_VALUE
    ): List<Audio>

    /**
     * Permanently deletes content from the device at the specified URIs.
     *
     * This function handles the deletion of content using the provided URIs. If the given
     * activity is a [ComponentActivity], it leverages the Activity Result APIs to report
     * back information about the deletion process, such as the number of items deleted.
     * For other activity types, it performs the deletion directly without providing detailed feedback.
     *
     * @param activity The Activity used to initiate the deletion request. If it is a
     * [ComponentActivity], it will receive a result callback with deletion information.
     * @param uris The URIs of the content to be deleted.
     * @return The number of items successfully deleted.
     *  - `-1` if an error occurred during the deletion process.
     *  - `-2` if the activity is not a [ComponentActivity] and the deletion request has been initiated.
     *     In this case, the user might see a confirmation dialog, but the exact outcome is unknown.
     *- `-3` if the deletion request was canceled by the user.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun delete(activity: Activity, vararg uri: Uri): Int

    /**
     * Deletes the specified URIs from the device's persistent storage permanently.
     *
     *  ***Note hat this fun works only unto android 10.***
     * @param uri The URIs to delete.
     * @return The number of items that were deleted, or -1 if an error occurred.
     * @see delete
     */
    @TargetApi(Build.VERSION_CODES.Q)
    suspend fun delete(vararg uri: Uri): Int

    /**
     * @see delete
     */
    @TargetApi(Build.VERSION_CODES.Q)
    suspend fun delete(vararg id: Long): Int

    /**
     * @see delete
     */
    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun delete(activity: Activity, vararg id: Long): Int

    /**
     * @see delete
     */
    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun trash(activity: Activity, vararg uri: Uri): Int

    /**
     * @see delete
     */
    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun trash(activity: Activity, vararg id: Long): Int

    /**
     * @see delete
     */
    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun restore(activity: Activity, vararg uri: Uri): Int

    /**
     * @see delete
     */
    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun restore(activity: Activity, vararg id: Long): Int

    /**
     * Returns the [Audio]s of the [Album] [name]
     */
    suspend fun fetchAlbumAudios(
        name: String,
        filter: String? = null,
        order: String = MediaStore.Audio.Media.TITLE,
        ascending: Boolean = true,
        offset: Int = 0,
        limit: Int = Int.MAX_VALUE
    ): List<Audio>

    /**
     * @see fetchAlbumAudio
     */
    suspend fun fetchArtistAudios(
        name: String,
        filter: String? = null,
        order: String = MediaStore.Audio.Media.TITLE,
        ascending: Boolean = true,
        offset: Int = 0,
        limit: Int = Int.MAX_VALUE
    ): List<Audio>

    /**
     * @see fetchAlbumAudio
     */
    suspend fun fetchGenreAudios(
        name: String,
        filter: String? = null,
        order: String = MediaStore.Audio.Media.TITLE,
        ascending: Boolean = true,
        offset: Int = 0,
        limit: Int = Int.MAX_VALUE
    ): List<Audio>
}

/**
 * Provides an instance of [MediaProvider].
 *
 * @param context The application context.
 * @return An instance of[MediaProviderImpl].
 */
fun MediaProvider(context: Context): MediaProvider =
    MediaProviderImpl(context)


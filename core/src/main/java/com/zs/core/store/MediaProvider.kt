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

import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.annotation.DeprecatedSinceApi
import androidx.annotation.RequiresApi
import com.zs.core.store.MediaProvider.Companion.COLUMN_NAME
import com.zs.core.store.models.Audio
import com.zs.core.store.models.Audio.Album
import com.zs.core.store.models.Audio.Artist
import com.zs.core.store.models.Audio.Genre
import com.zs.core.store.models.Folder
import com.zs.core.store.models.Trashed
import com.zs.core.store.models.Video
import kotlinx.coroutines.flow.Flow

/**
 * Provides access to media files on the device.
 *
 * This interface defines methods for fetching, searching, and managing media files.
 * Implementations of this interface can be used to interact with android's MediaStore
 */
interface MediaProvider {
    /***/
    companion object {
        const val UNKNOWN_STRING = MediaStore.UNKNOWN_STRING

        const val COLUMN_ID = MediaStore.Files.FileColumns._ID
        const val COLUMN_NAME = MediaStore.Files.FileColumns.TITLE
        const val COLUMN_MIME_TYPE = MediaStore.Files.FileColumns.MIME_TYPE
        const val COLUMN_PATH = MediaStore.Files.FileColumns.DATA
        const val COLUMN_RELATIVE = MediaStore.Files.FileColumns.RELATIVE_PATH
        const val COLUMN_DATE_ADDED = MediaStore.Files.FileColumns.DATE_ADDED
        const val COLUMN_DATE_MODIFIED = MediaStore.Files.FileColumns.DATE_MODIFIED
        const val COLUMN_SIZE = MediaStore.Files.FileColumns.SIZE
        const val COLUMN_PARENT = MediaStore.Files.FileColumns.PARENT
        const val COLUMN_MEDIA_DURATION = MediaStore.MediaColumns.DURATION

        // Audio file properties
        const val COLUMN_AUDIO_NUMBER_OF_TRACKS = MediaStore.Audio.AudioColumns.TRACK
        const val COLUMN_AUDIO_ALBUM = MediaStore.Audio.AudioColumns.ALBUM
        const val COLUMN_AUDIO_ARTIST = MediaStore.Audio.AudioColumns.ARTIST
        const val COLUMN_AUDIO_COMPOSER = MediaStore.Audio.AudioColumns.COMPOSER
        const val COLUMN_AUDIO_YEAR = MediaStore.Audio.AudioColumns.YEAR
        const val COLUMN_AUDIO_ALBUM_ID = MediaStore.Audio.AudioColumns.ALBUM_ID
        const val COLUMN_ALBUM_ARTIST = MediaStore.Audio.AudioColumns.ALBUM_ARTIST

        // Represents different types of audio files
        const val AUDIO_TYPE_ALARM = 0
        const val AUDIO_TYPE_PODCAST = 1
        const val AUDIO_TYPE_RINGTONE = 2
        const val AUDIO_TYPE_MUSIC = 3
        const val AUDIO_TYPE_RECORDING = 4
        const val AUDIO_TYPE_AUDIOBOOK = 5


        // Video file properties
        const val COLUMN_HEIGHT = MediaStore.Video.VideoColumns.HEIGHT
        const val COLUMN_WIDTH = MediaStore.Video.VideoColumns.WIDTH
        const val COLUMN_RESOLUTION = MediaStore.Video.VideoColumns.RESOLUTION
        const val COLUMN_ORIENTATION = MediaStore.Video.VideoColumns.ORIENTATION

        // Other file properties meant to be used internally
        internal const val COLUMN_IS_TRASHED = MediaStore.Files.FileColumns.IS_TRASHED
        internal const val COLUMN_DATE_EXPIRES = MediaStore.Files.FileColumns.DATE_EXPIRES
        internal const val COLUMN_MEDIA_TYPE = MediaStore.Files.FileColumns.MEDIA_TYPE
        internal const val MEDIA_TYPE_VIDEO = MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
        internal const val MEDIA_TYPE_AUDIO = MediaStore.Files.FileColumns.MEDIA_TYPE_AUDIO

        val EXTERNAL_CONTENT_URI = MediaStore.Files.getContentUri("external")
        val EXTERNAL_AUDIO_URI = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
        val EXTERNAL_VIDEO_URI = MediaStore.Video.Media.EXTERNAL_CONTENT_URI

        private val sArtworkUri = Uri.parse("content://media/external/audio/albumart")
        fun buildAlbumArtUri(id: Long) = ContentUris.withAppendedId(sArtworkUri, id)

        /**
         * @return The content URI for [id] provider.
         */
        fun contentUri(id: Long) = ContentUris.withAppendedId(EXTERNAL_CONTENT_URI, id)

        internal val FILE_PROJECTION = arrayOf(
            COLUMN_ID, // 0
            COLUMN_NAME, // 1
            COLUMN_MIME_TYPE, // 2
            COLUMN_PATH, // 3
            COLUMN_DATE_ADDED,  // 4
            COLUMN_DATE_MODIFIED, // 5
            COLUMN_SIZE // 6
        )

        internal val AUDIO_PROJECTION = FILE_PROJECTION + arrayOf(
            COLUMN_MEDIA_DURATION, // 7
            COLUMN_AUDIO_ALBUM, // 8
            COLUMN_AUDIO_ARTIST, // 9
            COLUMN_AUDIO_ALBUM_ID, // 10
            COLUMN_AUDIO_COMPOSER, // 11
            COLUMN_AUDIO_YEAR, // 12
            COLUMN_AUDIO_NUMBER_OF_TRACKS,  //13
            COLUMN_ALBUM_ARTIST // 14
        )

        internal val VIDEO_PROJECTION = FILE_PROJECTION + arrayOf(
            COLUMN_MEDIA_DURATION, // 7
            COLUMN_WIDTH, // 8
            COLUMN_HEIGHT, // 9
            COLUMN_ORIENTATION // 10
        )

        internal val TRASHED_PROJECTION =
            FILE_PROJECTION + COLUMN_DATE_EXPIRES // 7

        internal val ALBUM_PROJECTION = arrayOf(
            MediaStore.Audio.Albums._ID, //0
            MediaStore.Audio.Albums.ALBUM,//1
            MediaStore.Audio.Albums.ARTIST, //2
            MediaStore.Audio.Albums.FIRST_YEAR, //3
            MediaStore.Audio.Albums.LAST_YEAR, // 4,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS, // 5
        )

        internal val ARTIST_PROJECTION = arrayOf(
            MediaStore.Audio.Artists._ID,// 0
            MediaStore.Audio.Artists.ARTIST, // 1,
            MediaStore.Audio.Artists.NUMBER_OF_TRACKS, // 2
            MediaStore.Audio.Artists.NUMBER_OF_ALBUMS, // 3
        )

        internal val GENRE_PROJECTION = arrayOf(
            MediaStore.Audio.Genres._ID, //0
            MediaStore.Audio.Genres.NAME,//1
        )

        internal val AUDIO_FOLDER_PROJECTION =
            arrayOf(
                COLUMN_AUDIO_ALBUM_ID,
                COLUMN_PATH,
                COLUMN_SIZE,
                COLUMN_DATE_MODIFIED
            )

        internal val VIDEO_FOLDER_PROJECTION =
            arrayOf(
                COLUMN_ID,
                COLUMN_PATH,
                COLUMN_SIZE,
                COLUMN_DATE_MODIFIED
            )

        /**
         * Provides an instance of [MediaProvider].
         *
         * @param context the calling context.
         * @return An instance of[MediaProviderImpl].
         */
        operator fun invoke(context: Context): MediaProvider =
            MediaProviderImpl(context.applicationContext)
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
     * Retrieves media files of type [com.zs.core.store.models.Audio] from the [MediaStore].
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
    suspend fun fetchAudioFolders(
        filter: String? = null,
        ascending: Boolean = true,
        offset: Int = 0,
        limit: Int = Int.MAX_VALUE
    ): List<Folder>

    /**@see fetchAudioFolders*/
    suspend fun fetchVideoFolders(
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

    suspend fun getArtist(id: Long): Artist

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

    suspend fun getAlbum(id: Long): Album

    /**
     * @see fetchFolders
     */
    suspend fun fetchGenres(
        filter: String? = null,
        order: String = MediaStore.Audio.Genres.DEFAULT_SORT_ORDER,
        ascending: Boolean = true,
        offset: Int = 0,
        limit: Int = Int.MAX_VALUE
    ): List<Genre>

    suspend fun getGenre(id: Long): Genre

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
     * Deletes the specified URIs from the device's persistent storage permanently.
     *
     *  ***Note hat this fun works only unto android 10.***
     * @param uri The URIs to delete.
     * @return The number of items that were deleted, or -1 if an error occurred.
     * @see delete
     */
    @DeprecatedSinceApi(Build.VERSION_CODES.Q, "Use delete(vararg uri: Uri) instead.")
    suspend fun delete(vararg id: Long): Int

    /** @see delete */
    @DeprecatedSinceApi(Build.VERSION_CODES.Q, "Use delete(vararg id: Long) instead.")
    suspend fun delete(vararg uri: Uri): Int


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
    suspend fun delete(activity: Activity, vararg id: Long): Int

    /**@see delete */
    @RequiresApi(Build.VERSION_CODES.R)
    suspend fun delete(activity: Activity, vararg uri: Uri): Int

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
        id: Long,
        filter: String? = null,
        order: String = MediaProvider.COLUMN_NAME,
        ascending: Boolean = true,
        offset: Int = 0,
        limit: Int = Int.MAX_VALUE
    ): List<Audio>

    /**
     * @see fetchAlbumAudio
     */
    suspend fun fetchArtistAudios(
        id: Long,
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
        id: Long,
        filter: String? = null,
        order: String = MediaStore.Audio.Media.TITLE,
        ascending: Boolean = true,
        offset: Int = 0,
        limit: Int = Int.MAX_VALUE
    ): List<Audio>
}
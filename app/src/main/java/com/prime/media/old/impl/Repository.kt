@file:Suppress("DeprecatedCallableAddReplaceWith")

package com.prime.media.old.impl

import android.app.Activity
import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.annotation.WorkerThread
import com.prime.media.old.common.util.toMember
import com.prime.media.old.core.db.*
import com.prime.media.settings.AppConfig
import com.prime.media.settings.Settings
import com.primex.preferences.Preferences
import com.primex.preferences.value
import com.zs.core.db.Playlist
import com.zs.core.db.Playlist.Track
import com.zs.core.db.Playlists2
import com.zs.core.playback.Playback
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext


private const val TAG = "Repository"

/**
 * Composes the content URI for an audio track based on the provided [id], using the
 * [MediaStore.Audio.Media.EXTERNAL_CONTENT_URI] and appending the track ID to the end of the URI.
 *
 * @param id the unique ID of the audio track
 * @return the content URI for the audio track
 */
private fun toAudioTrackUri(id: Long) =
    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

/**
 * Repository class for managing playlists and related audio files. This class is annotated with
 * `@ActivityRetainedScoped` and is designed to be used in Android app development.
 *
 * @property playlistz An instance of the `Playlists` class that provides access to a local database
 * of playlists.
 * @property resolver An instance of the Android `ContentResolver` class used to access content providers,
 * such as the device's media store, to retrieve audio files.
 *
 * @constructor Creates a new `Repository2` object with the given `playlistz` and `resolver` objects.
 */

class Repository  (
    private val playlistz: Playlists2,
    private val resolver: ContentResolver,
    private val preferences: Preferences
) {
    companion object {
        private const val ALBUM_ART_URI: String = "content://media/external/audio/albumart"

        /**
         * Composes the content URI for an album art image from the provided album [id], using the
         * [MediaStore.Images.Media.EXTERNAL_CONTENT_URI] and appending the album ID to the end of the URI.
         *
         * @param id the unique ID of the album
         * @return the content URI for the album art image
         */
        fun toAlbumArtUri(id: Long): Uri = ContentUris.withAppendedId(Uri.parse(ALBUM_ART_URI), id)
    }

    init {
        // ensure that playlist favourites exists.
        // TODO: Remove this from here. Place this somewhere suitable or just find better alternative.
        GlobalScope.launch {
            if (!playlistz.exists(Playback.PLAYLIST_FAVOURITE)) playlistz.insert(
                Playlist(
                    Playback.PLAYLIST_FAVOURITE,
                    desc = "",
                )
            )
        }
    }

    /**
     * Register an observer class that gets callbacks when data identified by a given content URI
     * changes.
     *
     * @param uri The content URI for which to observe changes.
     *
     * Usage example:
     *
     * ```
     * contentResolver.observe(uri).collect {
     *     // handle changes here
     * }
     * ```
     *
     * @return A [Flow] that emits `true` when the content identified by the given URI changes, and
     * `false` otherwise.
     */
    // FixMe: Move the logic of black list to MediaStore.
    fun observe(uri: Uri) = combine(
        flow = resolver.observe(uri),
        flow2 = preferences[Settings.BLACKLISTED_FILES],
    ) { self, _ ->
        self
    }


    /**
     * Returns the most recent album IDs from the user's device as a flow of Long integers.
     * This function is based on the `MediaStore.Audio.Media.EXTERNAL_CONTENT_URI` content provider,
     * which is used to access audio files on the device.
     *
     * ```
     * // Example usage:
     * viewModelScope.launch {
     * recent(10).collect { albumId ->
     * Log.d(TAG, "Recent album ID: $albumId")
     *   }
     * }
     * ```
     *
     * @param limit The maximum number of album IDs to retrieve.
     * @return A flow of Long integers representing the most recent album IDs.
     * @throws SecurityException if the app doesn't have permission to access the content provider.
     */
    fun recent(limit: Int) =
        observe(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI).map {
            resolver.query2(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                arrayOf(MediaStore.Audio.Media.ALBUM_ID),
                order = MediaStore.Audio.Media.DATE_MODIFIED,
                limit = limit,
                ascending = false,
            ) { c -> Array(c.count) { c.moveToPosition(it); c.getLong(0) } } ?: emptyArray()
        }

    /**
     * A flow that emits a list of favorite song URIs whenever the favorite playlist changes.
     * The favorite playlist is a user-defined playlist that can contain any number of songs.
     *
     * `Usage Example`
     * ```
     * val viewModel = ViewModelProvider(this).get(MyViewModel::class.java)
     * viewModel.favourite.onEach { favoriteUris ->
     *     Log.d(TAG, "Favorite URIs: $favoriteUris")
     * }.launchIn(viewModelScope)
     * ```
     * @return A flow of string lists representing the favorite song URIs.
     */
    val favourite: Flow<List<String>>
        get() = playlistz
            .observe2(name = Playback.PLAYLIST_FAVOURITE)
            .map { it.map { it.uri } }

    /**
     * A flow that emits a list of playlists excluding the special ones like [PLAYLIST_RECENT] etc.
     * Playlists can be user-defined or system-defined, and can contain any number of songs.
     *
     * `Usage Example`
     * ```
     * val viewModel = ViewModelProvider(this).get(MyViewModel::class.java)
     * viewModel.playlists.onEach { playlists ->
     *     Log.d(TAG, "All Playlists: $playlists")
     * }.launchIn(viewModelScope)
     * ```
     *
     * @return A flow of lists representing the playlists.
     */
    val playlists =
        playlistz.observe().map { playlists ->
            // drop private playlists.
            //playlists.dropWhile { it.name.indexOf(Playlists.PRIVATE_PLAYLIST_PREFIX) == 0 }
            playlists.filter { it.name.indexOf(Playlists2.PRIVATE_PLAYLIST_PREFIX) != 0 }
        }

    /**
     * Filters the given list of audios by excluding the ones that match the blacklisted paths or
     * are shorter than the minimum track length.
     *
     * @param values The list of audios to be filtered.
     * @return The list of audios that satisfy the filtering criteria.
     */
    private suspend fun filter(values: List<Audio>): List<Audio> {
        val excludePaths = preferences.value(Settings.BLACKLISTED_FILES)

        // A helper function that checks if a given path is blacklisted or not.
        fun isExcluded(path: String): Boolean {
            // If the excludePaths list is null, return false.
            if (excludePaths == null) return false
            // Otherwise, loop through the excludePaths list and check if the path matches or starts
            // with any of them.
            for (p in excludePaths) {
                if (p == path || path.startsWith("$p/"))
                    return true
            }
            // If none of them match, return false.
            return false
        }

        val limit = AppConfig.minTrackLengthSecs
        // Filter out the audios that are blacklisted or shorter than the limit.
        return values.filter { audio ->
            // Convert the duration from milliseconds to seconds and compare it with the limit.
            (audio.duration / 1000) > limit && !isExcluded(audio.data)
        }
    }

    /**
     * Returns a list of all local audios.
     * Audios can include any type of audio file, such as music files, ringtones, and alarm sounds.
     *
     * `Usage Example`
     * ```
     * val viewModel = ViewModelProvider(this).get(MyViewModel::class.java)
     * viewModel.getAllAudios().forEach { audio ->
     *     Log.d(TAG, "Audio: ${audio.title}")
     * }
     * ```
     *
     * @param query An optional search query to filter the audios.
     * @param order The order in which to sort the audios. Defaults to sorting by title.
     * @param ascending A Boolean indicating whether to sort the audios in ascending order. Defaults to true.
     * @param offset The number of audios to skip before returning results. Defaults to 0.
     * @param limit The maximum number of audios to return. Defaults to returning all audios.
     *
     * @return A list of audios matching the specified query, order, and limit.
     *
     * @throws SecurityException if the app doesn't have permission to access the audio content provider.
     */
    suspend fun getAudios(
        query: String? = null,
        order: String = MediaStore.Audio.Media.TITLE,
        ascending: Boolean = true,
        offset: Int = 0,
        limit: Int = Int.MAX_VALUE
    ) = filter(resolver.getAudios(query, order, ascending, offset = offset, limit = limit))

    /**
     * Returns a list of audios that are contained within the specified folder.
     *
     * `Usage Example`
     * ```
     * val viewModel = ViewModelProvider(this).get(MyViewModel::class.java)
     * viewModel.getAudiosFromFolder("/storage/emulated/0/Music/MyPlaylist", query = "love").forEach { audio ->
     *     Log.d(TAG, "Audio: ${audio.title}")
     * }
     * ```
     *
     * @param path The path to the folder to retrieve audios from.
     * @param query An optional search query to filter the audios.
     * @param order The order in which to sort the audios. Defaults to sorting by title.
     * @param ascending A Boolean indicating whether to sort the audios in ascending order. Defaults to true.
     *
     * @return A list of audios contained within the specified folder and matching the specified query and order.
     *
     * @throws SecurityException if the app doesn't have permission to access the audio content provider.
     */
    suspend fun getAudiosOfFolder(
        path: String,
        query: String? = null,
        order: String = MediaStore.Audio.Media.TITLE,
        ascending: Boolean = true,
    ) = filter(resolver.getFolder(path, query, order, ascending))


    /**
     * Returns a list of audios that belong to the specified genre.
     *
     * `Usage Example`
     * ```
     * val viewModel = ViewModelProvider(this).get(MyViewModel::class.java)
     * viewModel.getAudiosFromGenre("Pop", query = "love").forEach { audio ->
     *     Log.d(TAG, "Audio: ${audio.title}")
     * }
     * ```
     *
     * @param name The name of the genre to retrieve audios from.
     * @param query An optional search query to filter the audios.
     * @param order The order in which to sort the audios. Defaults to sorting by title.
     * @param ascending A Boolean indicating whether to sort the audios in ascending order. Defaults to true.
     *
     * @return A list of audios that belong to the specified genre and match the specified query and order.
     *
     * @throws SecurityException if the app doesn't have permission to access the audio content provider.
     */
    suspend fun getAudiosOfGenre(
        name: String,
        query: String? = null,
        order: String = MediaStore.Audio.Media.TITLE,
        ascending: Boolean = true,
    ) = filter(resolver.getGenre(name, query, order, ascending))

    /**
     * Returns the [Audio]s from the artist represented by the given [name].
     *
     * @param name the name of the artist to search for.
     * @param query optional search query to filter the results further.
     * @param order the column to sort the results by. Defaults to [MediaStore.Audio.Media.TITLE].
     * @param ascending the sort order, defaults to true for ascending order.
     *
     * @return a list of [Audio]s by the artist represented by the [name].
     *
     * @throws SecurityException if the app doesn't have permission to access the media store.
     *
     * Usage Example
     * ```
     * val viewModel = ViewModelProvider(this).get(MyViewModel::class.java)
     * viewModel.getAudiosOfArtist("Pink Floyd").forEach { audio ->
     *     Log.d(TAG, "Audio by Pink Floyd: ${audio.title}")
     * }
     * ```
     */
    suspend fun getAudiosOfArtist(
        name: String,
        query: String? = null,
        order: String = MediaStore.Audio.Media.TITLE,
        ascending: Boolean = true,
    ) = filter(resolver.getArtist(name, query, order, ascending))


    /**
     * Returns the [Audio]s from the album represented by the given [title].
     *
     * @param title The title of the album to retrieve the [Audio]s from.
     * @param query Optional search query to filter the [Audio]s returned.
     * @param order The field to order the results by (default: MediaStore.Audio.Media.TITLE).
     * @param ascending Whether to sort the results in ascending or descending order (default: true).
     *
     * Usage Example:
     * ```
     * val audioList: List<Audio> = album("My Album", null, MediaStore.Audio.Media.TITLE, true)
     * ```
     *
     * @return A list of `Audio` objects from the album represented by the [title].
     *
     * @throws SecurityException If the app does not have permission to access the audio files.
     * @throws IllegalStateException If the [MediaResolver] is not initialized.
     */
    suspend fun getAudiosOfAlbum(
        title: String,
        query: String? = null,
        order: String = MediaStore.Audio.Media.TITLE,
        ascending: Boolean = true,
    ) = filter(resolver.getAlbum(title, query, order, ascending))


    /**
     * Returns an observable [Flow] of [Playlist] [Playlist.Member]s represented by the unique
     * [name] of a playlist.
     *
     * @param name The unique name of the playlist to retrieve the members from.
     * @return An observable [Flow] of [Playlist] [Playlist.Member]s represented by the unique
     * [name] of a playlist.
     *
     * Usage Example:
     * ```
     * viewModelScope.launch {
     *     playlistRepository.getPlaylistMembers("My Playlist")
     *         .collect { member ->
     *             // Do something with the playlist member
     *         }
     * }
     * ```
     * @throws Exception if the playlist does not exist or if there is an issue retrieving its members.
     */
    suspend fun getPlaylistMembers(name: String) =
        playlistz.getMembers(name)

    /**
     * Returns a list of [Folder]s that match the given [filter], ordered in either ascending or
     * descending order based on the [ascending] parameter.
     *
     * @param filter A string to filter the folders by, or null if no filtering is desired (default: null).
     * @param ascending A boolean indicating whether to sort the folders in ascending (true)
     * or descending (false) order (default: true).
     *
     * Usage Example:
     * ```
     * val folders: List<Folder> = getFolders("My Music", false)
     * ```
     *
     * @return A list of [Folder]s that match the given [filter], ordered in either ascending or
     * descending order based on the [ascending] parameter.
     *
     * @throws SecurityException If the app does not have permission to access the audio files.
     * @throws IllegalStateException If the [MediaResolver] is not initialized.
     */
    suspend fun getFolders(
        filter: String? = null,
        ascending: Boolean = true,
    ) = resolver.getFolders(filter, ascending).let { folders ->
        val excludePaths = preferences.value(Settings.BLACKLISTED_FILES)
        fun isExcluded(path: String): Boolean {
            // If the excludePaths list is null, return false.
            if (excludePaths == null) return false
            // Otherwise, loop through the excludePaths list and check if the path matches or starts
            // with any of them.
            for (p in excludePaths) {
                if (p == path || path.startsWith("$path/"))
                    return true
            }
            // If none of them match, return false.
            return false
        }
        folders.filter { !isExcluded(it.path) }
    }

    /**
     * Returns a list of [Artist]s that match the given [filter], ordered by the specified [order]
     * parameter and in either ascending or descending order based on the [ascending] parameter.
     *
     * @param filter A string to filter the artists by, or null if no filtering is desired
     * (default: null).
     * @param order The field to order the results by (default: MediaStore.Audio.Media.ARTIST).
     * @param ascending A boolean indicating whether to sort the artists in ascending (true) or
     * descending (false) order (default: true).
     *
     * Usage Example:
     * ```
     * val artists: List<Artist> = getArtists("My favorite artists",
     * MediaStore.Audio.Media.ARTIST, false)
     * ```
     *
     * @return A list of [Artist]s that match the given [filter], ordered by the specified [order]
     * parameter and in either ascending or descending order based on the [ascending] parameter.
     *
     * @throws SecurityException If the app does not have permission to access the audio files.
     * @throws IllegalStateException If the [MediaResolver] is not initialized.
     */
    suspend fun getArtists(
        filter: String? = null,
        order: String = MediaStore.Audio.Media.ARTIST,
        ascending: Boolean = true,
    ) = resolver.getArtists(filter, order, ascending)

    /**
     * Returns a list of [Album]s that match the given [filter], ordered by the specified [order]
     * parameter and in either ascending or descending order based on the [ascending] parameter.
     *
     * @param filter A string to filter the albums by, or null if no filtering is desired
     * (default: null).
     * @param order The field to order the results by (default: MediaStore.Audio.Albums.ALBUM).
     * @param ascending A boolean indicating whether to sort the albums in ascending (true) or
     * descending (false) order (default: true).
     *
     * Usage Example:
     * ```
     * val albums: List<Album> =
     *          getAlbums("My favorite albums", MediaStore.Audio.Albums.ALBUM, false)
     * ```
     *
     * @return A list of [Album]s that match the given [filter], ordered by the specified
     * [order] parameter and in either ascending or descending order based on the [ascending] parameter.
     *
     * @throws SecurityException If the app does not have permission to access the audio files.
     * @throws IllegalStateException If the [MediaResolver] is not initialized.
     */
    suspend fun getAlbums(
        filter: String? = null,
        order: String = MediaStore.Audio.Albums.ALBUM,
        ascending: Boolean = true,
    ) = resolver.getAlbums(filter, order, ascending)

    /**
     * Returns a list of [Genre]s that match the given [filter], ordered by the specified [order]
     * parameter and in either ascending or descending order based on the [ascending] parameter.
     *
     * @param filter A string to filter the genres by, or null if no filtering is desired
     * (default: null).
     * @param order The field to order the results by (default: MediaStore.Audio.Genres.NAME).
     * @param ascending A boolean indicating whether to sort the genres in ascending (true) or
     * descending (false) order (default: true).
     *
     * Usage Example:
     * ```
     * val genres: List<Genre> = getGenres("Rock", MediaStore.Audio.Genres.NAME, true)
     * ```
     *
     * @return A list of [Genre]s that match the given [filter], ordered by the specified
     * [order] parameter and in either ascending or descending order based on the [ascending]
     * parameter.
     *
     * @throws SecurityException If the app does not have permission to access the audio files.
     * @throws IllegalStateException If the [MediaResolver] is not initialized.
     */
    suspend fun getGenres(
        filter: String? = null,
        order: String = MediaStore.Audio.Genres.NAME,
        ascending: Boolean = true,
    ) = resolver.getGenres(filter, order, ascending)

    /**
     * Returns a [Flow] of [Playlist.Member]s that belong to the playlist with the specified [id].
     *
     * @param id The unique ID of the playlist to retrieve [Playlist.Member]s for.
     *
     * Usage Example:
     * ```
     * playlist(5).collect { members ->
     *     // do something with the playlist members
     * }
     * ```
     *
     * @return A [Flow] of [Playlist.Member]s that belong to the playlist with the specified [id].
     */
    fun playlist(id: Long): Flow<List<Track>> =
        playlistz.observe2(id)

    /**
     * Returns a [Flow] of [Playlist.Member]s that belong to the playlist with the specified [name].
     *
     * @param name The name of the playlist to retrieve [Playlist.Member]s for.
     *
     * Usage Example:
     * ```
     * playlist("My Playlist").collect { members ->
     *     // do something with the playlist members
     * }
     * ```
     *
     * @return A [Flow] of [Playlist.Member]s that belong to the playlist with the specified [name].
     */
    fun playlist(name: String): Flow<List<Track>> =
        playlistz.observe2(name)

    /**
     * Returns the [Playlist] with the specified [name].
     *
     * @param name The name of the playlist to retrieve.
     *
     * Usage Example:
     * ```
     * val myPlaylist: Playlist = getPlaylist("My Playlist")
     * ```
     *
     * @return The [Playlist] with the specified [name].
     */
    suspend fun getPlaylist(name: String) = playlistz.get(name)

    /**
     * Returns the [Audio] with the specified [id], or null if no [Audio] exists with that [id].
     *
     * @param id The ID of the [Audio] to retrieve.
     *
     * Usage Example:
     * ```
     * val myAudio: Audio? = findAudio(123)
     * ```
     *
     * @return The [Audio] with the specified [id], or null if no [Audio] exists with that [id].
     *
     * @throws SecurityException If the app does not have permission to access the audio files.
     * @throws IllegalStateException If the [MediaResolver] is not initialized.
     */
    suspend fun findAudio(id: Long) = resolver.findAudio(id)


    /**
     * Checks whether a [Playlist] with the specified [playlistName] exists.
     *
     * @param playlistName The name of the playlist to check for existence.
     *
     * Usage Example:
     * ```
     * val exists: Boolean = exists("My Playlist")
     * ```
     *
     * @return `true` if a [Playlist] with the specified [playlistName] exists, `false` otherwise.
     *
     * @throws SecurityException If the app does not have permission to access the audio files.
     */
    suspend fun exists(playlistName: String): Boolean = playlistz.get(playlistName) != null

    /**
     * Inserts a new playlist into the database. If a playlist with the same name already exists, returns -1L.
     *
     * @param playlist The [Playlist] to be created.
     * @return The ID of the newly created playlist, or -1L if a playlist with the same name already exists.
     *
     * Usage Example:
     * ```
     * val newPlaylist = Playlist("My Playlist", dateCreated = System.currentTimeMillis())
     * val playlistId = runBlocking { create(newPlaylist) }
     * if (playlistId > 0) {
     *     println("Playlist created with ID $playlistId.")
     * } else {
     *     println("Playlist with name '${newPlaylist.name}' already exists.")
     * }
     * ```
     *
     * @author Zakir Sheikh
     * @since 1.0.0
     */
    suspend fun create(playlist: Playlist): Long {
        // check if early on if it might exist or not
        if (exists(playlist.name)) return -1L
        return playlistz.insert(playlist)
    }

    /**
     * Deletes the specified `playlist` from the database and returns a boolean value indicating
     * whether the deletion was successful.
     *
     * @param playlist the playlist to be deleted.
     * @return `true` if the deletion was successful, `false` otherwise.
     *
     * Usage Example:
     * ```
     * val playlist = Playlist(123)
     * val deleted = delete(playlist)
     * if (deleted) {
     *     // Do something if the playlist was deleted successfully.
     * } else {
     *     // Show an error message if the deletion failed.
     * }
     * ```
     *
     * @since 1.0.0
     * @author Zakir Sheikh
     */
    suspend fun delete(playlist: Playlist): Boolean = playlistz.delete(playlist) == 1

    /**
     * Updates the specified `value` in the database and returns a boolean value indicating whether the update was successful.
     *
     * @param value the playlist with updated information to be stored in the database.
     * @return `true` if the update was successful, `false` otherwise.
     *
     * Usage Example:
     * ```
     * val playlist = Playlist(123, "New Playlist Name")
     * val updated = update(playlist)
     * if (updated) {
     *     // Do something if the playlist was updated successfully.
     * } else {
     *     // Show an error message if the update failed.
     * }
     * ```
     *
     * @since 1.0.0
     * @author Zakir Sheikh
     */
    suspend fun update(value: Playlist): Boolean = playlistz.update(value) == 1

    /**
     * Function that checks whether a member identified by [uri] exists in it.
     *
     * Usage Example:
     * ```
     * val `my id` = 123
     * val `my uri` = "http://example.com"
     * val exists = exists(`my id`, `my uri`)
     * ```
     *
     * @param id The ID of the playlist to check.
     * @param uri The URI of the song within the playlist to check.
     * @return `true` if the track exists, `false` otherwise.
     *
     * @since 1.0.0
     * @author Zakir Sheikh
     */
    suspend fun exists(id: Long, uri: String) = playlistz.exists(id, uri)

    /**
     * Function that checks if the member identified by [uri] exists in [name]
     * @see exists
     */
    suspend fun exists(name: String, uri: String) = playlistz.exists(name, uri)

    /**
     * Utility function that deletes the specified member from the playlist.
     *
     * Usage example:
     * ```
     * val member = Playlist.Member("http://example.com", "Song Title", "Artist Name")
     * val deleted = delete(member)
     *
     * if (deleted) {
     *     println("The member was successfully deleted.")
     * } else {
     *     println("Failed to delete the member.")
     * }
     * ```
     *
     * @param value The playlist member to delete.
     * @return `true` if the member was deleted successfully, `false` otherwise.
     *
     * @since 1.0.0
     * @author Zakir Sheikh
     */
    suspend fun delete(value: Track) = playlistz.delete(value) == 1

    /**
     * Utility function that checks if a song identified by [uri] is a favourite.
     *
     * Usage example:
     * ```
     * val songUri = "http://example.com"
     * val isFav = isFavourite(songUri)
     * if (isFav) {
     *     println("This song is a favourite.")
     * } else {
     *     println("This song is not a favourite.")
     * }
     * ```
     *
     * @param uri The URI of the song to check.
     * @return `true` if the song is a favourite, `false` otherwise.
     * @throws SomeException if the operation fails for some reason.
     * @since 1.0.0
     * @Author [Author name]
     */
    suspend fun isFavourite(uri: String) = exists(name = Playback.PLAYLIST_FAVOURITE, uri)

    @Deprecated("use method with uri.")
    suspend fun isFavourite(id: Long) = isFavourite(toAudioTrackUri(id).toString())

    /**
     * Creates playlist if not exist otherwise returns the -1L
     */
    @Deprecated("Use create(Playlist)")
    suspend fun createPlaylist(
        name: String, desc: String = ""
    ): Long = create(Playlist(name = name, desc = desc))

    @Deprecated("Use delete(Playlist)")
    suspend fun deletePlaylist(playlist: Playlist): Boolean = delete(playlist)

    @Deprecated("Use update(Playlist)")
    suspend fun updatePlaylist(value: Playlist): Boolean = update(value)

    @Deprecated("use above one.")
    suspend fun addToPlaylist(
        audioID: Long, name: String
    ): Boolean {
        val audio = findAudio(audioID) ?: return false
        //TODO: Update dateModified.
        val older = playlistz.lastPlayOrder(audioID) ?: 0
        val id = playlistz.get(name)?.id ?: return false
        val member = audio.toMember(id, older + 1)
        return upsert(member)
    }

    /**
     * @return [Audio] specified by [id] or null
     */
    @WorkerThread
    @Deprecated("use directly findAudio")
    fun getAudioById(id: Long) = runBlocking { findAudio(id) }

    /**
     * Remove from playlist if first [Playlist] found and then if successfully removed.
     */
    @Deprecated("audioID might not work in future.")
    suspend fun removeFromPlaylist(name: String, audioID: Long): Boolean {
        val playlistsDb = playlistz
        val playlist = playlistsDb.get(name) ?: return false
        val key = toAudioTrackUri(audioID).toString()
        val count = playlistsDb.delete(playlist.id, key).also {
            if (it == 1) playlistsDb.update(playlist.clone())
        }
        return count == 1
    }

    @Deprecated("audioID might not work in future.")
    suspend fun toggleFav(audioID: Long): Boolean {
        val favourite = isFavourite(audioID)
        val op = if (favourite) removeFromPlaylist(Playback.PLAYLIST_FAVOURITE, audioID)
        else addToPlaylist(audioID, Playback.PLAYLIST_FAVOURITE)
        return !favourite && op
    }

    /**
     * Removes a member from a playlist with the given ID.
     *
     * @param id the ID of the playlist to remove the member from
     * @param uri the URI of the member to remove from the playlist
     *
     * @return true if the member was successfully removed from the playlist, false otherwise
     */
    suspend fun removeFromPlaylist(id: Long, uri: String): Boolean {
        return playlistz.delete(id, uri) == 1
    }

    /**
     * Removes a member from the playlist with the given name.
     *
     * @param name the name of the playlist to remove the member from
     * @param uri the URI of the member to remove from the playlist
     *
     * @return true if the member was successfully removed from the playlist, false otherwise
     */
    suspend fun removeFromPlaylist(name: String, uri: String): Boolean {
        val playlist = playlistz.get(name) ?: return false
        return playlistz.delete(playlist.id, uri) == 1
    }

    /**
     * Retrieves a [Member] from a playlist by its [id] and [uri].
     * @param id the ID of the playlist to retrieve the member from
     * @param uri the URI of the member to retrieve
     * @return the [Member] object if it exists in the playlist, null otherwise
     */
    suspend fun getPlaylistMember(id: Long, uri: String): Track? {
        return playlistz.get(id, uri)
    }

    /**
     * @see getPlaylistMember
     */
    suspend fun getPlaylistMember(name: String, uri: String): Track? {
        val playlist = playlistz.get(name) ?: return null
        return playlistz.get(playlist.id, uri)
    }

    /**
     * Returns the last play order value for the playlist with the specified [id].
     * @param id the ID of the playlist to retrieve the last play order value for
     * @return the last play order value for the playlist if it exists, null otherwise
     */
    suspend fun getLastPlayOrder(id: Long): Int? {
        return playlistz.lastPlayOrder(id)
    }

    /**
     * @see getLastPlayOrder
     */
    suspend fun getLastPlayOrder(name: String): Int? {
        val playlist = playlistz.get(name) ?: return null
        return playlistz.lastPlayOrder(playlist.id)
    }

    /**
     * Inserts a new member into a playlist. If the member already exists in the playlist, updates
     * the member's information.
     *
     * @param value The [Playlist.Member] to be inserted or updated in the playlist.
     * @return `true` if the member was inserted or updated successfully, `false` otherwise.
     *
     * Usage example:
     * ```
     * val member =
     *      Playlist.Member(playlistID = 1, uri = "spotify:track:4uLU6hMCjMI75M1A2tKUQC", order = 0)
     * if (insert(member)) {
     *     println("Member added to playlist.")
     * } else {
     *     println("Failed to add member to playlist.")
     * }
     * ```
     *
     * @since 1.0.0
     * @author Zakir Sheikh
     */
    @Deprecated("Use simple insert")
    suspend fun upsert(value: Track): Boolean {
        // if the item is already in playlist return false;
        // because we don't support same uri's in single playlist
        if (exists(value.playlistID, value.uri))
            return update(value)
        else
            return insert(value)
    }

    /**
     * Inserts a new member into the playlist.
     *
     * @param value The member to insert into the playlist.
     *
     * @return `true` if the insertion was successful, `false` if the member is already in the playlist.
     *
     * @see exists
     */
    suspend fun insert(value: Track): Boolean {
        val playlistsDb = playlistz
        // if the item is already in playlist return false;
        // because we don't support same uri's in single playlist
        if (exists(value.playlistID, value.uri))
            return false
        val order = playlistsDb.lastPlayOrder(value.playlistID) ?: -1
        // ensure that order is coerced in limit.
        val member =
            if (value.order < 0 || value.order > order + 1)
                value.copy(order = value.order.coerceIn(0, order + 1))
            else
                value
        val success = playlistsDb.insert(member = member) != -1L
        if (success) {
            // update the modified time of the playlist.
            // here this should not be null
            // but we should play safe
            val old = playlistsDb.get(value.playlistID) ?: return true
            update(old.clone())
        }
        return success
    }

    /**
     * Updates the given member in the playlist.
     *
     * @param value The member to update in the playlist.
     *
     * @return Returns `true` if the update was successful, `false` if the member is not in the playlist.
     *
     * @fixme What happens if the user has changed/updated the URI of the item?
     * @see exists
     */
    suspend fun update(value: Track): Boolean {
        val playlistsDb = playlistz
        // if the item is not in playlist return false
        // FixMe: what happens if the user have changed/updated the uri of the item.
        if (!exists(value.playlistID, value.uri))
            return false
        val order = playlistsDb.lastPlayOrder(value.playlistID) ?: -1
        // ensure that order is coerced in limit.
        val member =
            if (value.order < 0 || value.order > order + 1)
                value.copy(order = value.order.coerceIn(0, order + 1))
            else
                value
        val success = playlistsDb.update(member = member) != -1L
        if (success) {
            // update the modified time of the playlist.
            // here this should not be null
            // but we should play safe
            val old = playlistsDb.get(value.playlistID) ?: return true
            update(old.clone())
        }
        return success
    }

    /**
     * @see com.prime.media.core.db.delete
     */
    suspend fun delete(activity: Activity, vararg uri: Uri, trash: Boolean = true): Int {
        return withContext(Dispatchers.IO) {
            val result = runCatching {
                // if less than R simply delete the items.
                if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.R)
                    return@runCatching resolver.delete(*uri)
                if (activity is ComponentActivity)
                    return@runCatching if (trash) resolver.trash(
                        activity,
                        *uri
                    ) else resolver.delete(activity, *uri)
                return@runCatching if (trash) resolver.trash(activity, *uri) else resolver.trash(
                    activity,
                    *uri
                )
            }
            // return -1 if failure.
            result.getOrElse { -1 }
        }
    }
}
package com.prime.media.directory.store

import android.content.Context
import android.net.Uri
import androidx.navigation.NavHostController
import com.prime.media.core.compose.directory.Directory
import com.prime.media.core.compose.directory.GroupBy
import com.prime.media.core.compose.directory.ViewType
import com.prime.media.core.db.Album
import com.prime.media.core.db.Artist
import com.prime.media.core.db.Audio
import com.prime.media.core.db.Folder
import com.prime.media.core.db.Genre
import com.prime.media.core.db.Playlist
import com.prime.media.impl.DirectoryViewModel
import kotlinx.coroutines.flow.Flow

interface Albums : Directory<Album> {
    companion object {
        private const val HOST = "_local_audio_albums"

        val route = DirectoryViewModel.compose(HOST)
        fun direction(
            query: String = DirectoryViewModel.NULL_STRING,
            order: GroupBy = GroupBy.Name,
            ascending: Boolean = true,
            viewType: ViewType = ViewType.List
        ) = DirectoryViewModel.compose(
            HOST,
            DirectoryViewModel.NULL_STRING,
            query,
            order,
            ascending,
            viewType
        )
    }
}

interface Artists : Directory<Artist> {
    companion object {
        private const val HOST = "_local_audio_artists"

        val route = DirectoryViewModel.compose(HOST)
        fun direction(
            query: String = DirectoryViewModel.NULL_STRING,
            order: GroupBy = GroupBy.Name,
            ascending: Boolean = true,
            viewType: ViewType = ViewType.List
        ) = DirectoryViewModel.compose(
            HOST,
            DirectoryViewModel.NULL_STRING,
            query,
            order,
            ascending,
            viewType
        )
    }
}

interface Audios : Directory<Audio> {
    /**
     * Loads the items into the [Playback] service and starts playing.
     *
     * The algorithm works as follows:
     * 1. If nothing is selected, all items based on the current filter are obtained. If shuffle is
     * enabled, the index is set to a random item.
     * 2. If the "focused" item is not empty, the index is set to that item. Otherwise, the index is
     * set to the first item in the list.
     * 3. If items are selected, only those items are consumed. If shuffle is enabled, the index is
     * set to a random item from the selected items. Otherwise, the index is set to the first selected item in the list.
     *
     * @param shuffle if true, the items are played in random order
     */
    fun play(shuffle: Boolean)

    /**
     * Adds the selected or focused item(s) to the specified playlist.
     *
     * The algorithm works as follows:
     * 1. Determine whether the action was called on a selected item or a focused one.
     * 2. Retrieve the keys/ids of the selected or focused item(s).
     * 3. Clear the selection.
     * 4. Retrieve the specified playlist from the repository.
     * 5. If the playlist doesn't exist, display an error message and return.
     * 6. Retrieve the last play order of the playlist from the repository, if it exists.
     * 7. Map the keys/ids to audio items, with the corresponding playlist ID and play order.
     * 8. Insert or update the audio items in the repository.
     * 9. Display a success or warning message, depending on the number of items added to the playlist.
     *
     * @param name The name of the playlist to add the item(s) to.
     */
    fun addToPlaylist(name: String)
    fun toggleFav()
    fun playNext()
    fun addToQueue()
    fun delete()
    fun selectAll()

    /**
     * Shares the selected or focused item(s).
     *
     * The algorithm works as follows:
     * 1. If an item is focused, add its ID to the list.
     * 2. else if there are selected items, add their IDs to the list.
     * 2. Consume the selected items.
     * 3. Retrieve the metadata for each ID in the list.
     * 4. If the list is empty, do nothing.
     * 5. Share the metadata with the specified context.
     *
     * @param context the context to share the metadata with.
     */
    fun share(context: Context)
    fun toArtist(controller: NavHostController)
    fun toAlbum(controller: NavHostController)

    companion object {

        const val GET_EVERY = "_every"
        const val GET_FROM_FOLDER = "_folder"
        const val GET_FROM_ARTIST = "_artist"
        const val GET_FROM_GENRE = "_genre"
        const val GET_FROM_ALBUM = "_album"

        private const val HOST = "_local_audios"

        const val PARAM_TYPE = "_param_type"

        val route = DirectoryViewModel.compose("$HOST/{${PARAM_TYPE}}")
        fun direction(
            of: String,
            key: String = DirectoryViewModel.NULL_STRING,
            query: String = DirectoryViewModel.NULL_STRING,
            order: GroupBy = GroupBy.Name,
            ascending: Boolean = true,
            viewType: ViewType = ViewType.List
        ) = DirectoryViewModel.compose(
            "$HOST/$of",
            Uri.encode(key),
            query,
            order,
            ascending,
            viewType
        )
    }

    val favourites: Flow<List<String>>
    val playlists: Flow<List<Playlist>>
}

interface Folders : Directory<Folder> {
    companion object {
        private const val HOST = "_local_folders"

        val route = DirectoryViewModel.compose(HOST)
        fun direction(
            query: String = DirectoryViewModel.NULL_STRING,
            order: GroupBy = GroupBy.Name,
            ascending: Boolean = true,
            viewType: ViewType = ViewType.List
        ) = DirectoryViewModel.compose(
            HOST,
            DirectoryViewModel.NULL_STRING,
            query,
            order,
            ascending,
            viewType
        )
    }
}

interface Genres : Directory<Genre> {
    companion object {
        private const val HOST = "_local_genres"

        val route = DirectoryViewModel.compose(HOST)
        fun direction(
            query: String = DirectoryViewModel.NULL_STRING,
            order: GroupBy = GroupBy.Name,
            ascending: Boolean = true,
            viewType: ViewType = ViewType.List
        ) = DirectoryViewModel.compose(
            HOST,
            DirectoryViewModel.NULL_STRING,
            query,
            order,
            ascending,
            viewType
        )
    }
}
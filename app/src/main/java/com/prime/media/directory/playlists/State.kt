package com.prime.media.directory.playlists

import android.net.Uri
import com.prime.media.core.compose.directory.Directory
import com.prime.media.core.compose.directory.GroupBy
import com.prime.media.core.compose.directory.ViewType
import com.prime.media.core.db.Playlist
import com.prime.media.impl.DirectoryViewModel
import kotlinx.coroutines.flow.Flow

interface Members : Directory<Playlist.Member> {
    /**
     * @see AudiosViewModel.play
     */
    fun play(shuffle: Boolean)

    /**
     * Deletes the selected or focused item(s) from the playlist.
     * If no item is selected, shows a toast message and returns.
     * If an item is focused, deletes that item.
     * If multiple items are selected, deletes all selected items.
     * Shows a toast message indicating the number of items deleted.
     */
    fun delete()
    fun playNext()
    fun addToQueue()

    /**
     * @see AudiosViewModel.addToPlaylist
     */
    fun addToPlaylist(name: String)

    companion object {
        private const val HOST = "_local_playlist_members"

        val route = DirectoryViewModel.compose(HOST)
        fun direction(
            key: String,
            query: String = DirectoryViewModel.NULL_STRING,
            order: GroupBy = GroupBy.Name,
            ascending: Boolean = true,
            viewType: ViewType = ViewType.List
        ) = DirectoryViewModel.compose(HOST, Uri.encode(key), query, order, ascending, viewType)
    }

    val playlists: Flow<List<Playlist>>
    fun selectAll()
}

interface Playlists : Directory<Playlist> {
    fun createPlaylist(name: String)
    fun delete()
    fun rename(name: String)

    companion object {
        private const val HOST = "_local_playlists"

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
package com.prime.media.impl.playlists

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Message
import androidx.compose.material.icons.outlined.MoreTime
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.prime.media.core.compose.channel.Channel
import com.prime.media.core.compose.directory.Action
import com.prime.media.core.compose.directory.GroupBy
import com.prime.media.core.db.Playlist
import com.prime.media.core.playback.Playback
import com.prime.media.core.playback.Remote
import com.prime.media.core.util.addDistinct
import com.prime.media.playlists.Members
import com.prime.media.impl.DirectoryViewModel
import com.prime.media.impl.Mapped
import com.prime.media.impl.MetaData
import com.prime.media.impl.Repository
import com.prime.media.impl.toMediaItem
import com.primex.core.Amber
import com.primex.core.MetroGreen
import com.primex.core.Rose
import com.primex.core.Text
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.ArrayList
import javax.inject.Inject
import kotlin.random.Random

private const val TAG = "AlbumsViewModel"

private val Playlist.Member.firstTitleChar
    inline get() = title.uppercase()[0].toString()

@HiltViewModel
class MembersViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val repository: Repository,
    private val channel: Channel,
    private val remote: Remote,
) : DirectoryViewModel<Playlist.Member>(handle), Members {

    private val title = when (key) {
        Playback.PLAYLIST_FAVOURITE -> "Favourites"
        Playback.PLAYLIST_RECENT -> "History"
        else -> key
    }

    init {
        // emit the name to meta
        //TODO: Add other fields in future versions.
        meta = MetaData(
            Text(title)
        )
    }


    override val playlists = repository.playlists

    override val actions: List<Action> =
        mutableStateListOf(Action.PlaylistAdd, Action.PlayNext, Action.AddToQueue, Action.Delete)
    override val orders: List<GroupBy> = listOf(GroupBy.None, GroupBy.Name)
    override val mActions: List<Action?> = listOf(null, Action.Play, Action.Shuffle)

    override val data: Flow<Mapped<Playlist.Member>> =
        filter.flatMapLatest { (order, query, ascending) ->
            repository
                .playlist(key)
                .map { data ->
                    val filtered =
                        if (query == null)
                            data
                        else
                            data.filter { it.title.contains(query, true) }
                    val src = if (ascending) filtered else filtered.reversed()

                    // Don't know if this is correct place to emit changes to Meta.
                    val latest = src.maxByOrNull { it.order }
                    meta = meta?.copy(
                        artwork = latest?.artwork.toString(),
                        cardinality = src.size
                    )

                    when (order) {
                        GroupBy.None -> mapOf(Text("") to src)
                        GroupBy.Name -> src.groupBy { Text(it.firstTitleChar) }
                        else -> error("$order invalid")
                    }
                }
        }.catch {
            // any exception.
            channel.show(
                "Some unknown error occured!.",
                "Error",
                leading = Icons.Outlined.Error,
                accent = Color.Rose,
                duration = Channel.Duration.Indefinite
            )
        }

    override fun toggleViewType() {
        // we only currently support single viewType. Maybe in future might support more.
        viewModelScope.launch {
            channel.show("Toggle not implemented yet.", "ViewType")
        }
    }

    /**
     * @see AudiosViewModel.play
     */
    override fun play(shuffle: Boolean) {
        viewModelScope.launch {
            // because the order is necessary to play intented item first.
            val src = data.firstOrNull()?.values?.flatten() ?: return@launch
            val list =
                src.let {
                    // return same list if selected is empty else return the only selected items from the list.
                    val arr = ArrayList(selected)
                    // consume selected.
                    clear()
                    if (arr.isEmpty())
                        it
                    else
                        arr.mapNotNull { id -> it.find { it.uri == id } }
                }
            // don't do anything
            if (list.isEmpty()) return@launch
            val focused = focused
            // check which is focused
            val index = when {
                // pick random
                shuffle -> Random.nextInt(0, list.size)
                // find focused
                focused.isNotBlank() -> list.indexOfFirst { it.uri == focused }
                    .let { if (it == -1) 0 else it }

                else -> 0
            }
            remote.onRequestPlay(shuffle, index, list.map { it.toMediaItem })
            channel.show(title = "Playing", message = "Playing tracks enjoy.")
        }
    }

    /**
     * Deletes the selected or focused item(s) from the playlist.
     * If no item is selected, shows a toast message and returns.
     * If an item is focused, deletes that item.
     * If multiple items are selected, deletes all selected items.
     * Shows a toast message indicating the number of items deleted.
     */
    override fun delete() {
        viewModelScope.launch {
            val list = when {
                focused.isNotBlank() -> listOf(focused)
                selected.isNotEmpty() -> ArrayList(selected)
                else -> {
                    channel.show("No item selected.", "Message")
                    return@launch
                }
            }
            // consume selected
            clear()

            var count = 0
            list.forEach {
                val deleted = repository.removeFromPlaylist(key, it)
                if (deleted)
                    count++
            }
            if (count < list.size)
                channel.show(
                    "Deleted $count items from $title",
                    "Delete",
                    leading = Icons.Outlined.Error,
                    accent = Color.Rose,
                )
        }
    }


    override fun playNext() {
        viewModelScope.launch {
            channel.show(
                title = "Coming soon.",
                message = "Requires more polishing. Please wait!",
                leading = Icons.Outlined.MoreTime
            )
        }
    }


    override fun addToQueue() {
        viewModelScope.launch {
            channel.show(
                title = "Coming soon.",
                message = "Requires more polishing. Please wait!",
                leading = Icons.Outlined.MoreTime
            )
        }
    }

    /**
     * Overrides the `select` method of the parent class to add or remove the given item key from the
     * list of selected items. Also adds or removes the `SelectAll` action from the list of available
     * actions depending on the number of selected items.
     *
     * The algorithm of this function is as follows:
     *
     * 1. Call the `select` method of the parent class with the given item key to add or remove it
     *    from the list of selected items.
     *
     * 2. Get the mutable list of actions by casting the `actions` property to a `SnapshotStateList`.
     *
     * 3. Depending on the number of selected items, add or remove the `SelectAll` action from the list
     *    of available actions:
     *    - If no items are selected, remove the `SelectAll` action if it exists.
     *    - If one item is selected, add the `SelectAll` action if it doesn't exist.
     *
     * @param key The key of the item to select or deselect.
     */
    override fun select(key: String) {
        super.select(key)
        // add actions if selected.size == 1
        val mutable = actions as SnapshotStateList
        when {
            selected.isEmpty() -> mutable.remove(Action.SelectAll)
            selected.size == 1 -> mutable.addDistinct(Action.SelectAll)
        }
    }

    /**
     * @see AudiosViewModel.addToPlaylist
     */
    override fun addToPlaylist(name: String) {
        // focus or selected.
        viewModelScope.launch {
            if (key == name) {
                channel.show(
                    "The tracks are already in the playlist",
                    "Message",
                    leading = Icons.Outlined.Message
                )
                return@launch
            }

            // The algo goes like this.
            // This fun is called on selected item or focused one.
            // so obtain the keys/ids
            val list = when {
                focused.isNotBlank() -> listOf(focused)
                selected.isNotEmpty() -> kotlin.collections.ArrayList(selected)
                else -> {
                    channel.show("No item selected.", "Message")
                    return@launch
                }
            }

            // consume selected
            clear()

            val playlist = repository.getPlaylist(name)
            if (playlist == null) {
                channel.show(
                    "It seems the playlist doesn't exist.",
                    "Error",
                    leading = Icons.Outlined.Error
                )
                return@launch
            }

            var order = repository.getLastPlayOrder(playlist.id) ?: -1

            // you can;t just add to playlist using the keys.
            val audios = list.mapNotNull {
                repository.getPlaylistMember(playlist.id, it)?.copy(order = order++)
            }

            var count = 0
            audios.forEach {
                val success = repository.upsert(it)
                if (success)
                    count++
            }

            if (count < list.size)
                channel.show(
                    "Added only $count items to $name",
                    "Warning",
                    leading = Icons.Outlined.Warning,
                    accent = Color.Amber,
                )
            else
                channel.show(
                    "Added $count items to $name",
                    "Success",
                    leading = Icons.Outlined.CheckCircle,
                    accent = Color.MetroGreen,
                )
        }
    }

    override fun selectAll() {}
}
package com.prime.media.impl.playlists

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.prime.media.core.compose.channel.Channel
import com.prime.media.core.compose.directory.Action
import com.prime.media.core.compose.directory.GroupBy
import com.prime.media.core.db.Playlist
import com.prime.media.core.playback.Remote
import com.prime.media.playlists.Playlists
import com.prime.media.impl.DirectoryViewModel
import com.prime.media.impl.Mapped
import com.prime.media.impl.MetaData
import com.prime.media.impl.Repository
import com.primex.core.Rose
import com.primex.core.Text
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

private const val TAG = "PlaylistsViewModel"

private val Playlist.firstTitleChar
    inline get() = name.uppercase(Locale.ROOT)[0].toString()
private val VALID_NAME_REGEX = Regex("^[a-zA-Z0-9]+$")

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val repository: Repository,
    private val channel: Channel,
    private val remote: Remote,
) : DirectoryViewModel<Playlist>(handle), Playlists {

    init {
        // emit the name to meta
        //TODO: Add other fields in future versions.
        meta = MetaData(Text("Playlists"))
    }

    override fun toggleViewType() {
        // we only currently support single viewType. Maybe in future might support more.
        viewModelScope.launch {
            channel.show("Toggle not implemented yet.", "ViewType")
        }
    }

    override fun createPlaylist(name: String) {
        viewModelScope.launch {
            if (name.isBlank() || !VALID_NAME_REGEX.matches(name)) {
                channel.show(
                    message = "The provided name is an invalid",
                    title = "Error",
                    leading = Icons.Outlined.ErrorOutline,
                    accent = Color.Rose
                )
                return@launch
            }
            val exists = repository.exists(name)
            if (exists) {
                channel.show(
                    message = "The playlist with name $name already exists.",
                    title = "Error",
                    leading = Icons.Outlined.ErrorOutline,
                    accent = Color.Rose
                )
                return@launch
            }
            val res = repository.create(Playlist(name))
        }
    }

    override fun delete() {
        viewModelScope.launch {
            val item = selected.firstOrNull() ?: return@launch
            // consume
            clear()
            val playlist = repository.getPlaylist(item) ?: return@launch
            val success = repository.delete(playlist)
            if (!success)
                channel.show(
                    "An error occured while deleting ${playlist.name}",
                    "Error",
                    leading = Icons.Outlined.ErrorOutline,
                    accent = Color.Rose
                )
        }
    }

    override fun rename(name: String) {
        viewModelScope.launch {
            if (name.isBlank() || !VALID_NAME_REGEX.matches(name)) {
                channel.show(
                    message = "The provided name is an invalid",
                    title = "Error",
                    leading = Icons.Outlined.ErrorOutline,
                    accent = Color.Rose
                )
                return@launch
            }
            val exists = repository.exists(name)
            if (exists) {
                channel.show(
                    message = "The playlist with name $name already exists.",
                    title = "Error",
                    leading = Icons.Outlined.ErrorOutline,
                    accent = Color.Rose
                )
                return@launch
            }
            val value = selected.first().let { repository.getPlaylist(it) } ?: return@launch
            // consume
            clear()
            val update = value.copy(
                name = name, dateModified = System.currentTimeMillis()
            )
            when (repository.update(update)) {
                true -> channel.show(message = "The name of the playlist has been update to $name")
                else -> channel.show(message = "An error occurred while update the name of the playlist to $name")
            }
        }
    }

    override val actions: List<Action> = listOf(Action.Delete, Action.Edit)
    override val orders: List<GroupBy> = listOf(GroupBy.None, GroupBy.Name)
    override val mActions: List<Action?> = listOf(Action.PlaylistAdd)

    override val data: Flow<Mapped<Playlist>> =
        filter.combine(repository.playlists) { f, d ->
            val (order, query, ascending) = f
            val filtered = if (query == null) d else d.filter { it.name.contains(query, true) }
            val src = if (ascending) filtered else filtered.reversed()
            when (order) {
                GroupBy.None -> mapOf(Text("") to src)
                GroupBy.Name -> src.groupBy { Text(it.firstTitleChar) }
                else -> error("$order invalid")
            }
        }
}
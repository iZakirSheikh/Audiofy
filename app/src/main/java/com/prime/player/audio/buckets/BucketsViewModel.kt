package com.prime.player.audio.buckets

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prime.player.audio.Type
import com.prime.player.audio.buckets.args.BucketsRouteArgsFactory
import com.prime.player.common.compose.SnackDataChannel
import com.prime.player.common.compose.send
import com.prime.player.core.Playlist
import com.prime.player.core.Repository
import com.prime.player.core.name
import com.primex.core.Result
import com.primex.core.Text
import com.primex.core.buildResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

typealias BucketResult = Map<String, List<Any>>

@HiltViewModel
class BucketsViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val repository: Repository
) : ViewModel() {

    val type =
        BucketsRouteArgsFactory
            .fromSavedStateHandle(handle)
            .type.let { Type.valueOf(it) }

    val isTypePlaylists: Boolean = type == Type.PLAYLISTS

    private suspend inline fun exits(name: String, channel: SnackDataChannel?, elze: () -> Unit) {
        val doesExist = name.isBlank() || repository.exists(name)
        if (doesExist)
            channel?.send(message = "The playlist with name: $name already exits.")
        else
            elze()
    }


    fun createPlaylist(
        name: String,
        channel: SnackDataChannel? = null
    ) {
        viewModelScope.launch {
            exits(name = name, channel = channel) {
                channel?.send(message = "Creating playlist - $name")
                repository.createPlaylist(name)
            }
        }
    }

    fun delete(
        playlist: Playlist,
        channel: SnackDataChannel? = null
    ) {
        viewModelScope.launch {
            channel?.send(message = "Deleting playlist - ${playlist.name}")
            repository.deletePlaylist(playlist)
        }
    }

    fun rename(
        value: Playlist,
        name: String,
        channel: SnackDataChannel? = null
    ) {
        viewModelScope.launch {
            exits(name, channel = channel) {
                val update = value.copy(
                    name = name,
                    dateModified = System.currentTimeMillis()
                )
                val success = repository.updatePlaylist(update)
                when (success) {
                    true -> channel?.send(message = "The name of the playlist has been update to $name")
                    else -> channel?.send(message = "An error occurred while update the name of the playlist to $name")
                }
            }
        }
    }

    private val observables =
        when (type) {
            Type.PLAYLISTS ->
                repository
                    .playlists
                    .map { it.groupBy { playlist -> playlist.name.firstChar } }

            Type.FOLDERS ->
                repository
                    .folders
                    .map { it.groupBy { folder -> folder.name.firstChar } }

            Type.ARTISTS ->
                repository
                    .artists
                    .map { it.groupBy { artist -> artist.name.firstChar } }

            Type.ALBUMS ->
                repository
                    .albums
                    .map { it.groupBy { album -> album.title.firstChar } }

            Type.GENRES ->
                repository
                    .genres
                    .map { it.groupBy { genre -> genre.name.firstChar } }
            //
            else -> error("No such Group type: $type")
        }

    @OptIn(FlowPreview::class)
    val result: Result<BucketResult> =
        buildResult(emptyMap()) {
            observables
                .debounce(300)
                .distinctUntilChanged()
                .map { it.toSortedMap() }
                .onEach {
                    emit(it)
                    // change state to empty.
                    if (it.isEmpty()) emit(Result.State.Empty)
                }
                .catch { emit(Result.State.Error(Text("An Unknown error occurred."))) }
                .launchIn(viewModelScope)
        }
}

private inline val String.firstChar
    get() = "${uppercase()[0]}"
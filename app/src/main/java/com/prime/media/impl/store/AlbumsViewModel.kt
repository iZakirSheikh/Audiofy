package com.prime.media.impl.store

import android.provider.MediaStore
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.prime.media.core.compose.channel.Channel
import com.prime.media.core.compose.directory.Action
import com.prime.media.core.compose.directory.GroupBy
import com.prime.media.core.db.Album
import com.prime.media.core.playback.Remote
import com.prime.media.store.Albums
import com.prime.media.impl.DirectoryViewModel
import com.prime.media.impl.Mapped
import com.prime.media.impl.MetaData
import com.prime.media.impl.Repository
import com.primex.core.Rose
import com.primex.core.Text
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject


private const val TAG = "AlbumsViewModel"
private val Album.firstTitleChar
    inline get() = title.uppercase(Locale.ROOT)[0].toString()

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val repository: Repository,
    private val channel: Channel,
    private val remote: Remote,
) : DirectoryViewModel<Album>(handle), Albums {

    init {
        // emit the name to meta
        //TODO: Add other fields in future versions.
        meta = MetaData(Text("Albums"))
    }

    override fun toggleViewType() {
        // we only currently support single viewType. Maybe in future might support more.
        viewModelScope.launch {
            channel.show("Toggle not implemented yet.", "ViewType")
        }
    }

    override val mActions: List<Action?> = emptyList()
    override val actions: List<Action> = emptyList()
    override val orders: List<GroupBy> = listOf(GroupBy.None, GroupBy.Name, GroupBy.Artist)
    private val GroupBy.toMediaOrder
        get() = when (this) {
            GroupBy.None -> MediaStore.Audio.Albums.DEFAULT_SORT_ORDER
            GroupBy.Name -> MediaStore.Audio.Albums.ALBUM
            GroupBy.Artist -> MediaStore.Audio.Albums.ARTIST
            else -> error("Invalid order: $this ")
        }

    override val data: Flow<Mapped<Album>> =
        repository.observe(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI)
            .combine(filter) { f1, f2 -> f2 }.map {
                val (order, query, ascending) = it
                val list = repository.getAlbums(query, order.toMediaOrder, ascending)
                when (order) {
                    GroupBy.None -> mapOf(Text("") to list)
                    GroupBy.Name -> list.groupBy { album -> Text(album.firstTitleChar) }
                    GroupBy.Artist -> list.groupBy { album -> Text(album.artist) }
                    else -> error("$order invalid")
                }
            }
            .catch {
                // any exception.
                channel.show(
                    "Some unknown error occured!.",
                    "Error",
                    leading = Icons.Outlined.Error,
                    accent = Color.Rose,
                    duration = Channel.Duration.Indefinite
                )
            }
}
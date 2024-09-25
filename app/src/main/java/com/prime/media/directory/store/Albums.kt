@file:OptIn(ExperimentalTextApi::class)

package com.prime.media.directory.store

import android.provider.MediaStore
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.prime.media.R
import com.prime.media.core.ContentElevation
import com.prime.media.core.ContentPadding
import com.prime.media.core.compose.Artwork
import com.prime.media.core.compose.Channel
import com.prime.media.core.compose.LocalNavController
import com.prime.media.core.db.Album
import com.prime.media.core.db.uri
import com.prime.media.core.playback.Remote
import com.prime.media.directory.Action
import com.prime.media.directory.Directory
import com.prime.media.directory.DirectoryViewModel
import com.prime.media.directory.GroupBy
import com.prime.media.directory.Mapped
import com.prime.media.directory.MetaData
import com.prime.media.directory.ViewType
import com.prime.media.impl.Repository
import com.primex.core.Rose
import com.primex.core.Text
import com.primex.core.textResource
import com.primex.material2.Label
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

private const val TAG = "AlbumsViewModel"

private val Album.firstTitleChar
    inline get() = title.uppercase(Locale.ROOT)[0].toString()

typealias Albums = AlbumsViewModel.Companion

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val repository: Repository,
    private val toaster: Channel,
    private val remote: Remote,
) : DirectoryViewModel<Album>(handle) {

    companion object {
        private const val HOST = "_local_audio_albums"

        val route = compose(HOST)
        fun direction(
            query: String = NULL_STRING,
            order: GroupBy = GroupBy.Name,
            ascending: Boolean = true,
            viewType: ViewType = ViewType.List
        ) = compose(HOST, NULL_STRING, query, order, ascending, viewType)
    }

    init {
        // emit the name to meta
        //TODO: Add other fields in future versions.
        meta = MetaData(Text(R.string.albums))
    }

    override fun toggleViewType() {
        // we only currently support single viewType. Maybe in future might support more.
        viewModelScope.launch {
            toaster.show("Toggle not implemented yet.", "ViewType")
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
                toaster.show(
                    R.string.msg_unknown_error,
                    leading = Icons.Outlined.Error,
                    accent = Color.Rose,
                    duration = Channel.Duration.Indefinite
                )
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())
}

private val TILE_WIDTH = 80.dp
private val GridItemPadding =
    PaddingValues(vertical = 6.dp, horizontal = 10.dp)

@Composable
fun Album(
    value: Album,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            // clip the ripple
            .clip(com.zs.core_ui.AppTheme.shapes.medium)
            .then(modifier)
            // add padding after size.
            .padding(GridItemPadding)
            // add preferred size with min/max width
            .then(Modifier.width(TILE_WIDTH))
            // wrap the height of the content
            .wrapContentHeight(),
    ) {

        Surface(
            shape = com.zs.core_ui.AppTheme.shapes.compact,
            elevation = ContentElevation.medium,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.65f),
            content = {
                Artwork(
                    data = value.uri,
                    fallback = painterResource(id = R.drawable.default_art)
                )
            },
        )

        // title
        Label(
            text = value.title,
            maxLines = 2,
            modifier = Modifier.padding(top = ContentPadding.medium),
            style = com.zs.core_ui.AppTheme.typography.caption,
        )

        // Subtitle
        Label(
            text = textResource(R.string.albums_scr_year_d, value.firstYear),
            style = com.zs.core_ui.AppTheme.typography.caption
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Albums(viewModel: AlbumsViewModel) {
    val navigator = LocalNavController.current
    Directory(
        viewModel = viewModel,
        cells = GridCells.Adaptive(TILE_WIDTH + (4.dp * 2)),
        onAction = { /*TODO: Currently we don't support more actions.*/ },
        key = { it.id },
        contentPadding = PaddingValues(horizontal = ContentPadding.normal),
    ) {
        Album(
            value = it,
            modifier = Modifier
                .clickable {
                    val direction = Audios.direction(Audios.GET_FROM_ALBUM, it.title)
                    navigator.navigate(direction)
                }
            // .animateItemPlacement()
        )
    }
}
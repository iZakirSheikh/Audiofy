package com.prime.media.directory.local

import android.provider.MediaStore
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.prime.media.*
import com.prime.media.common.ContentElevation
import com.prime.media.common.ContentPadding
import com.prime.media.common.LocalNavController
import com.prime.media.core.Repository
import com.prime.media.core.compose.ToastHostState
import com.prime.media.core.compose.show
import com.prime.media.core.db.Artist
import com.prime.media.core.playback.Remote
import com.prime.media.directory.*
import com.primex.core.Text
import com.primex.ui.Label
import com.primex.ui.Neumorphic
import com.primex.ui.Rose
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import com.prime.media.R
private const val TAG = "AlbumsViewModel"


typealias Artists = ArtistsViewModel.Companion


private val Artist.firstTitleChar
    inline get() = name.uppercase(Locale.ROOT)[0].toString()

@HiltViewModel
class ArtistsViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val repository: Repository,
    private val toaster: ToastHostState,
    private val remote: Remote,
) : DirectoryViewModel<Artist>(handle) {

    companion object {
        private const val HOST = "_local_audio_artists"

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
        meta = MetaData(Text("Artists"))
    }

    override fun toggleViewType() {
        // we only currently support single viewType. Maybe in future might support more.
        viewModelScope.launch {
            toaster.show("Toggle not implemented yet.", "ViewType")
        }
    }

    private val GroupBy.toMediaOrder
        get() = when (this) {
            GroupBy.None -> MediaStore.Audio.Artists.DEFAULT_SORT_ORDER
            GroupBy.Name -> MediaStore.Audio.Artists.ARTIST
            else -> error("Invalid order: $this ")
        }

    override val actions: List<Action> = emptyList()
    override val orders: List<GroupBy> = listOf(GroupBy.None, GroupBy.Name)
    override val mActions: List<Action?> = emptyList()

    override val data: Flow<Mapped<Artist>> =
        repository.observe(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI)
            .combine(filter) { f1, f2 -> f2 }.map {
                val (order, query, ascending) = it
                val list = repository.getArtists(query, order.toMediaOrder, ascending)
                when (order) {
                    GroupBy.None -> mapOf(Text("") to list)
                    GroupBy.Name -> list.groupBy { artist -> Text(artist.firstTitleChar) }
                    GroupBy.Artist -> list.groupBy { artist -> Text(artist.name) }
                    else -> error("$order invalid")
                }
            }
            .catch {
                // any exception.
                toaster.show(
                    "Some unknown error occured!.",
                    "Error",
                    leading = Icons.Outlined.Error,
                    accent = Color.Rose,
                    duration = ToastHostState.Duration.Indefinite
                )
            }
}


private val TILE_WIDTH = 80.dp
private val GridItemPadding =
    PaddingValues(vertical = 6.dp, horizontal = 10.dp)

@Composable
fun Artist(
    value: Artist,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            // clip the ripple
            .clip(Theme.shapes.medium)
            .then(modifier)
            // add padding after size.
            .padding(GridItemPadding)
            // add preferred size with min/max width
            .then(Modifier.width(TILE_WIDTH))
            // wrap the height of the content
            .wrapContentHeight(),
    ) {

        Neumorphic(
            shape = CircleShape,
            modifier = Modifier
                .padding(top = 6.dp)
                .sizeIn(maxWidth = 66.dp)
                .aspectRatio(1.0f),
            elevation = ContentElevation.low,
            lightShadowColor = Theme.colors.lightShadowColor,
            darkShadowColor = Theme.colors.darkShadowColor,

            content = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_artist),
                    contentDescription = null,
                    modifier = Modifier
                        .requiredSize(40.dp)
                        .wrapContentSize(Alignment.Center)
                )
            }
        )

        // title
        Label(
            text = value.name,
            maxLines = 2,
            modifier = Modifier.padding(top = ContentPadding.medium),
            style = Theme.typography.caption,
        )

        // Subtitle
        Label(
            text = "${value.tracks} Tracks",
            style = Theme.typography.caption2
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Artists(viewModel: ArtistsViewModel) {
    val navigator = LocalNavController.current
    Directory(
        viewModel = viewModel,
        cells = GridCells.Adaptive(TILE_WIDTH + (4.dp * 2)),
        onAction = {},
        key = { it.id },
        contentPadding = PaddingValues(horizontal = ContentPadding.normal),
    ) {
        Artist(
            value = it,
            modifier = Modifier
                .clickable {
                    val direction = Audios.direction(Audios.GET_FROM_ARTIST, it.name)
                    navigator.navigate(direction)
                }
                .animateItemPlacement()
        )
    }
}
package com.prime.media.directory.store

import android.provider.MediaStore
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Error
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.prime.media.*
import com.prime.media.impl.Repository
import com.prime.media.core.ContentPadding
import com.prime.media.core.compose.LocalNavController
import com.prime.media.core.compose.Channel
import com.prime.media.caption2
import com.prime.media.core.db.Genre
import com.prime.media.core.playback.Remote
import com.prime.media.directory.*
import com.primex.core.Rose
import com.primex.core.Text
import com.primex.material2.Label
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

private const val TAG = "AlbumsViewModel"


typealias Genres = GenresViewModel.Companion

private val Genre.firstTitleChar
    inline get() = name.uppercase(Locale.ROOT)[0].toString()

@HiltViewModel
class GenresViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val repository: Repository,
    private val toaster: Channel,
    private val remote: Remote,
) : DirectoryViewModel<Genre>(handle) {

    companion object {
        private const val HOST = "_local_genres"

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
        meta = MetaData(Text("Genres"))
    }

    override fun toggleViewType() {
        // we only currently support single viewType. Maybe in future might support more.
        viewModelScope.launch {
            toaster.show("Toggle not implemented yet.", "ViewType")
        }
    }

    private val GroupBy.toMediaOrder
        get() = when (this) {
            GroupBy.None -> MediaStore.Audio.Genres.DEFAULT_SORT_ORDER
            GroupBy.Name -> MediaStore.Audio.Genres.NAME
            else -> error("Invalid order: $this ")
        }

    override val actions: List<Action> = emptyList()
    override val orders: List<GroupBy> = listOf(GroupBy.None, GroupBy.Name)
    override val mActions: List<Action?> = emptyList()


    override val data: Flow<Any> =
        repository.observe(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI)
            .combine(filter) { f1, f2 -> f2 }.map {
                val (order, query, ascending) = it
                val list = repository.getGenres(query, order.toMediaOrder, ascending)
                when (order) {
                    GroupBy.None -> mapOf(Text("") to list)
                    GroupBy.Name -> list.groupBy { genre -> Text(genre.firstTitleChar) }
                    GroupBy.Artist -> list.groupBy { genre -> Text(genre.name) }
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
                    duration = Channel.Duration.Indefinite
                )
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())
}


private val TILE_WIDTH = 80.dp
private val GridItemPadding =
    PaddingValues(vertical = 6.dp, horizontal = 10.dp)

@Composable
fun Genre(
    value: Genre,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            // clip the ripple
            .clip(Material.shapes.medium)
            .then(modifier)
            // add padding after size.
            .padding(GridItemPadding)
            // add preferred size with min/max width
            .then(Modifier.width(TILE_WIDTH))
            // wrap the height of the content
            .wrapContentHeight(),
    ) {

        Surface(
            color = Color.Transparent,
            border = BorderStroke(3.dp, Material.colors.onBackground),
            shape = CircleShape,

            modifier = Modifier
                .sizeIn(maxWidth = 70.dp)
                .aspectRatio(1.0f),

            content = {
                Label(
                    text = "${value.name[0].uppercaseChar()}",
                    fontWeight = FontWeight.Bold,
                    style = Material.typography.h4,
                    modifier = Modifier.wrapContentSize(Alignment.Center)
                )
            }
        )

        // title
        Label(
            text = value.name,
            maxLines = 2,
            modifier = Modifier.padding(top = ContentPadding.medium),
            style = Material.typography.caption,
        )

        // Subtitle
        Label(
            text = "${value.cardinality} Tracks",
            style = Material.typography.caption2
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Genres(viewModel: GenresViewModel) {
    val navigator = LocalNavController.current
    Directory(
        viewModel = viewModel,
        cells = GridCells.Adaptive(TILE_WIDTH + (4.dp * 2)),
        onAction = {},
        key = { it.id },
        contentPadding = PaddingValues(horizontal = ContentPadding.normal),
    ) {
        Genre(
            value = it,
            modifier = Modifier
                .clickable {
                    val direction = Audios.direction(Audios.GET_FROM_GENRE, it.name)
                    navigator.navigate(direction)
                }
            //    .animateItemPlacement()
        )
    }
}
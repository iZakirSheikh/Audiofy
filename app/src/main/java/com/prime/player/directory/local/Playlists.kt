package com.prime.player.directory.local

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.prime.player.*
import com.prime.player.common.ContentElevation
import com.prime.player.common.ContentPadding
import com.prime.player.common.LocalNavController
import com.prime.player.core.Repository
import com.prime.player.core.compose.ToastHostState
import com.prime.player.core.compose.show
import com.prime.player.core.db.Playlist
import com.prime.player.core.playback.Remote
import com.prime.player.directory.*
import com.primex.core.Text
import com.primex.core.rememberState
import com.primex.ui.Label
import com.primex.ui.Neumorphic
import com.primex.ui.TextInputDialog
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

private const val TAG = "AlbumsViewModel"

typealias Playlists = PlaylistsViewModel.Companion

private val Playlist.firstTitleChar
    inline get() = name.uppercase(Locale.ROOT)[0].toString()

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val repository: Repository,
    private val toaster: ToastHostState,
    private val remote: Remote,
) : DirectoryViewModel<Playlist>(handle) {

    companion object {
        private const val HOST = "_local_playlists"

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
        meta = MetaData(Text("Playlists"))
    }
    
    override fun toggleViewType() {
        // we only currently support single viewType. Maybe in future might support more.
        viewModelScope.launch {
            toaster.show("Toggle not implemented yet.", "ViewType")
        }
    }

    fun createPlaylist(name: String) {}

    fun delete() {}

    fun rename(name: String) {}

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

private val TILE_WIDTH = 80.dp
private val GridItemPadding =
    PaddingValues(vertical = 6.dp, horizontal = 10.dp)

private val PlaylistShape = RoundedCornerShape(20)
private val PlaylistIcon = Icons.Outlined.PlaylistPlay

@Composable
fun Playlist(
    value: Playlist,
    modifier: Modifier = Modifier,
    checked: Boolean = false,
) {
    Column(
        modifier = Modifier
            // clip the ripple
            .clip(Theme.shapes.small2)
            .then(modifier)
            .then(
                if (checked)
                    Modifier.border(
                        BorderStroke(2.dp, LocalContentColor.current),
                        Theme.shapes.small2
                    )
                else
                    Modifier
            )
            // add padding after size.
            .padding(GridItemPadding)
            // add preferred size with min/max width
            .then(Modifier.width(TILE_WIDTH))
            // wrap the height of the content
            .wrapContentHeight()
            .then(
                if (checked)
                    Modifier.scale(0.8f)
                else
                    Modifier
            ),
    ) {
        Neumorphic(
            shape = PlaylistShape,
            modifier = Modifier
                .sizeIn(maxWidth = 70.dp)
                .aspectRatio(1.0f),
            elevation = ContentElevation.low,
            lightShadowColor = Theme.colors.lightShadowColor,
            darkShadowColor = Theme.colors.darkShadowColor,

            content = {
                Icon(
                    imageVector = PlaylistIcon,
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
    }
}

@Composable
private inline fun EditDialog(
    expanded: Boolean,
    title: String,
    placeholder: String,
    noinline onDismissRequest: (name: String?) -> Unit
) {
    if (expanded)
        TextInputDialog(
            title = title,
            onDismissRequest = onDismissRequest,
            label = placeholder,
            vectorIcon = Icons.TwoTone.Edit,
            textFieldShape = RoundedCornerShape(20),
            topBarContentColor = Theme.colors.onBackground,
            topBarBackgroundColor = Theme.colors.overlay
        )
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Playlists(viewModel: PlaylistsViewModel) {
    val navigator = LocalNavController.current
    var confirm by rememberState<Action?>(initial = null)

    EditDialog(
        expanded = confirm == Action.PlaylistAdd,
        title = "Create Playlist",
        placeholder = "Enter Playlist name",
        onDismissRequest = { name ->
            when (name) {
                // simply do-no and  dismiss
                null -> {}
                // other wise if name might exist.
                // else create the playlist.
                else -> viewModel.createPlaylist(name)
            }
            confirm = null
        }
    )

    EditDialog(
        expanded = confirm == Action.Edit,
        title = "Rename",
        placeholder = "Enter new name",
        onDismissRequest = { new ->
            when (new) {
                // just dismiss
                null -> {}
                // try to rename to new name.
                else -> viewModel.rename(new)
            }
            confirm = null
        }
    )

    // observe the selected items
    val selected = viewModel.selected
    Directory(
        viewModel = viewModel,
        cells = GridCells.Adaptive(TILE_WIDTH + (4.dp * 2)),
        onAction = { confirm = it },
        key = { it.id },
        contentPadding = PaddingValues(horizontal = ContentPadding.normal),
    ) {
        val checked by remember {
            derivedStateOf { selected.contains(it.name) }
        }
        Playlist(
            value = it,
            checked = checked,
            modifier = Modifier
                .combinedClickable(
                    onClick = {
                        if (selected.isEmpty()) {
                            val direction = Members.direction(it.name)
                            navigator.navigate(direction)
                        }
                    },
                    onLongClick = {
                        viewModel.clear()
                        viewModel.select(it.name)
                    }
                )
                .animateItemPlacement()
        )
    }
}
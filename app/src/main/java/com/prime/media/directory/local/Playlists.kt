package com.prime.media.directory.local

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
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
import com.prime.media.core.db.Playlist
import com.prime.media.core.playback.Remote
import com.prime.media.directory.*
import com.primex.core.Text
import com.primex.core.rememberState
import com.primex.ui.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

private const val TAG = "AlbumsViewModel"

typealias Playlists = PlaylistsViewModel.Companion

private val Playlist.firstTitleChar
    inline get() = name.uppercase(Locale.ROOT)[0].toString()

private val VALID_NAME_REGEX = Regex("^[a-zA-Z0-9]+$")

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

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            if (name.isBlank() || !VALID_NAME_REGEX.matches(name)) {
                toaster.show(
                    message = "The provided name is an invalid",
                    title = "Error",
                    leading = Icons.Outlined.ErrorOutline,
                    accent = Color.Rose
                )
                return@launch
            }
            val exists = repository.exists(name)
            if (exists) {
                toaster.show(
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

    fun delete() {
        viewModelScope.launch {
            val item = selected.firstOrNull() ?: return@launch
            // consume
            clear()
            val playlist = repository.getPlaylist(item) ?: return@launch
            val success = repository.delete(playlist)
            if (!success)
                toaster.show(
                    "An error occured while deleting ${playlist.name}",
                    "Error",
                    leading = Icons.Outlined.ErrorOutline,
                    accent = Color.Rose
                )
        }
    }

    fun rename(name: String) {
        viewModelScope.launch {
            if (name.isBlank() || !VALID_NAME_REGEX.matches(name)) {
                toaster.show(
                    message = "The provided name is an invalid",
                    title = "Error",
                    leading = Icons.Outlined.ErrorOutline,
                    accent = Color.Rose
                )
                return@launch
            }
            val exists = repository.exists(name)
            if (exists) {
                toaster.show(
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
                true -> toaster.show(message = "The name of the playlist has been update to $name")
                else -> toaster.show(message = "An error occurred while update the name of the playlist to $name")
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
                    Modifier.scale(0.65f)
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

    if (confirm == Action.Delete)
        AlertDialog(
            title = "Delete",
            message = "You are about to delete playlist. This can't be undone. \nAre you sure?",
            vectorIcon = Icons.Outlined.DeleteForever,
            onDismissRequest = { action ->
                when (action) {
                    true -> viewModel.delete()
                    else -> {}
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
                        // only move forward if nothing is focused
                        if (selected.isEmpty()) {
                            val direction = Members.direction(it.name)
                            navigator.navigate(direction)
                        } else {
                            // clear others since we need only 1 item to have focus.
                            viewModel.clear()
                            viewModel.select(it.name)
                        }
                    },
                    onLongClick = {
                        // clear others since we need only 1 item to have focus.
                        viewModel.clear()
                        viewModel.select(it.name)
                    },
                    enabled = !checked
                )
                .animateItemPlacement()
        )
    }
}
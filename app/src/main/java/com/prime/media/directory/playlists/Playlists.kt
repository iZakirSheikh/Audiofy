package com.prime.media.directory.playlists

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.zs.core_ui.ContentElevation
import com.zs.core_ui.ContentPadding
import com.prime.media.common.LocalNavController
import com.prime.media.common.preference
import com.primex.core.thenIf
import com.prime.media.core.db.Playlist
import com.prime.media.core.playback.Remote
import com.prime.media.directory.Action
import com.prime.media.directory.Directory
import com.prime.media.directory.DirectoryViewModel
import com.prime.media.directory.GroupBy
import com.prime.media.directory.Mapped
import com.prime.media.directory.MetaData
import com.prime.media.directory.ViewType
import com.prime.media.impl.Repository
import com.prime.media.settings.Settings
import com.primex.core.Rose
import com.primex.core.Text
import com.primex.material2.Label
import com.primex.material2.dialog.AlertDialog
import com.primex.material2.dialog.TextInputDialog
import com.primex.material2.neumorphic.Neumorphic
import com.zs.core_ui.AppTheme
import com.zs.core_ui.toast.ToastHostState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Locale
import javax.inject.Inject

private const val TAG = "AlbumsViewModel"

typealias Playlists = PlaylistsViewModel.Companion

private val Playlist.firstTitleChar
    inline get() = name.uppercase(Locale.ROOT)[0].toString()

private val VALID_NAME_REGEX = Regex("^[\\p{L}\\p{N}]+$")

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
            toaster.showToast("ViewType\nToggle not implemented yet.")
        }
    }

    fun createPlaylist(name: String) {
        viewModelScope.launch {
            if (name.isBlank() || !VALID_NAME_REGEX.matches(name)) {
                toaster.showToast(
                    message = "Error\nThe provided name is an invalid",
                    icon = Icons.Outlined.ErrorOutline,
                    accent = Color.Rose
                )
                return@launch
            }
            val exists = repository.exists(name)
            if (exists) {
                toaster.showToast(
                    message = "Error\nThe playlist with name $name already exists.",
                    icon = Icons.Outlined.ErrorOutline,
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
                toaster.showToast(
                    "Error\nAn error occured while deleting ${playlist.name}",
                    icon = Icons.Outlined.ErrorOutline,
                    accent = Color.Rose
                )
        }
    }

    fun rename(name: String) {
        viewModelScope.launch {
            if (name.isBlank() || !VALID_NAME_REGEX.matches(name)) {
                toaster.showToast(
                    message = "Error\nThe provided name is an invalid",
                    icon = Icons.Outlined.ErrorOutline,
                    accent = Color.Rose
                )
                return@launch
            }
            val exists = repository.exists(name)
            if (exists) {
                toaster.showToast(
                    message = "Error\nThe playlist with name $name already exists.",
                    icon = Icons.Outlined.ErrorOutline,
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
                true -> toaster.showToast(message = "The name of the playlist has been update to $name")
                else -> toaster.showToast(message = "An error occurred while update the name of the playlist to $name")
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
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())
}

private val TILE_WIDTH = 100.dp

private val PlaylistShape = RoundedCornerShape(20)
private val PlaylistIcon = Icons.AutoMirrored.Outlined.PlaylistPlay

@Composable
fun Playlist(
    value: Playlist,
    modifier: Modifier = Modifier,
    checked: Boolean = false,
) {
    Column(
        modifier = Modifier
            // clip the ripple
            .clip(AppTheme.shapes.compact)
            .then(modifier)
            .thenIf(
                checked,
            ){
                border(
                    BorderStroke(2.dp, LocalContentColor.current),
                    AppTheme.shapes.compact
                ).scale(0.85f)
            },
        verticalArrangement = Arrangement.spacedBy(ContentPadding.medium)
    ) {
        Neumorphic(
            shape = PlaylistShape,
            modifier = Modifier
                .scale(0.85f)
                .weight(1f)
                .aspectRatio(1.0f, true)
                .align(Alignment.CenterHorizontally),
            elevation = ContentElevation.low,
            lightShadowColor = AppTheme.colors.lightShadowColor,
            darkShadowColor = AppTheme.colors.darkShadowColor,
            content = {
                Icon(
                    imageVector = PlaylistIcon,
                    contentDescription = null,
                    modifier = Modifier
                        .wrapContentSize(Alignment.Center)
                        .requiredSize(32.dp)
                )
            }
        )

        // title
        Label(
            text = value.name,
            maxLines = 2,
            style = AppTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
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
            topBarContentColor = AppTheme.colors.onBackground,
            topBarBackgroundColor = AppTheme.colors.background(1.dp)
        )
}

private val GridItemsArrangement = Arrangement.spacedBy(6.dp)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Playlists(viewModel: PlaylistsViewModel) {
    val navigator = LocalNavController.current
    var confirm by remember { mutableStateOf<Action?>(null) }

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
    val multiplier by preference(Settings.GRID_ITEM_SIZE_MULTIPLIER)
    Directory(
        viewModel = viewModel,
        cells = GridCells.Adaptive(TILE_WIDTH * multiplier),
        onAction = { confirm = it },
        key = { it.id },
        horizontalArrangement = GridItemsArrangement,
        verticalArrangement = GridItemsArrangement,
        contentPadding = PaddingValues(horizontal = ContentPadding.normal),
    ) {
        val checked by remember {
            derivedStateOf { selected.contains(it.name) }
        }
        Playlist(
            value = it,
            checked = checked,
            modifier = Modifier
                .aspectRatio(1.0f)
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
            //  .animateItemPlacement()
        )
    }
}
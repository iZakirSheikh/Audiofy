package com.prime.media.playlists

import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.prime.media.*
import com.prime.media.core.ContentElevation
import com.prime.media.core.ContentPadding
import com.prime.media.core.compose.LocalNavController
import com.prime.media.core.compose.darkShadowColor
import com.prime.media.core.compose.directory.Action
import com.prime.media.core.compose.directory.Directory
import com.prime.media.core.compose.lightShadowColor
import com.prime.media.core.compose.overlay
import com.prime.media.core.compose.small2

import com.prime.media.core.db.Playlist
import com.primex.core.rememberState
import com.primex.material2.*
import com.primex.material2.dialog.AlertDialog
import com.primex.material2.dialog.TextInputDialog
import com.primex.material2.neumorphic.Neumorphic

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
fun Playlists(state: Playlists) {
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
                else -> state.createPlaylist(name)
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
                else -> state.rename(new)
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
                    true -> state.delete()
                    else -> {}
                }
                confirm = null
            }
        )

    // observe the selected items
    val selected = state.selected
    Directory(
        viewModel = state,
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
                            state.clear()
                            state.select(it.name)
                        }
                    },
                    onLongClick = {
                        // clear others since we need only 1 item to have focus.
                        state.clear()
                        state.select(it.name)
                    },
                    enabled = !checked
                )
                .animateItemPlacement()
        )
    }
}
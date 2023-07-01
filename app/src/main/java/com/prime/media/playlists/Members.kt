package com.prime.media.playlists

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prime.media.R
import com.prime.media.Theme
import com.prime.media.core.*
import com.prime.media.core.compose.*
import com.prime.media.core.compose.directory.Action
import com.prime.media.core.compose.directory.Directory

import com.prime.media.core.db.Playlist.Member
import com.prime.media.dialogs.Playlists
import com.primex.core.*
import com.primex.material2.*

private val ARTWORK_SIZE = 56.dp
private val MEMBER_ICON_SHAPE = RoundedCornerShape(30)

@OptIn(ExperimentalMaterialApi::class)
@Composable
@NonRestartableComposable
private fun Member(
    value: Member,
    actions: List<Action>,
    modifier: Modifier = Modifier,
    focused: Boolean = false,
    checked: Boolean = false,
    onAction: (Action) -> Unit
) {
    ListTile(
        selected = checked,
        centreVertically = false,
        modifier = modifier,
        overlineText = {
            Label(
                style = Theme.typography.caption,
                text = value.subtitle,
                color = LocalContentColor.current.copy(ContentAlpha.medium),
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Label(
                text = value.title,
                style = Theme.typography.body1,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
        },
        leading = {
            Image(
                data = value.artwork,
                fallback = painterResource(id = R.drawable.default_art),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .border(2.dp, Color.White, shape = MEMBER_ICON_SHAPE)
                    .shadow(ContentElevation.medium, shape = MEMBER_ICON_SHAPE)
                    .size(ARTWORK_SIZE),
            )
        },
        trailing = {
            IconButton(
                contentDescription = null,
                imageVector = Icons.Default.DragIndicator,
                onClick = { /*TODO: Add drag logic in future,*/ },
            )
        },

        bottom = composable(focused) {
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(start = ARTWORK_SIZE, end = ContentPadding.normal)
            ) {
                actions.forEach {
                    val color = ChipDefaults.outlinedChipColors(backgroundColor = Color.Transparent)
                    Chip(
                        onClick = { onAction(it) },
                        colors = color,
                        border =
                        BorderStroke(
                            1.dp,
                            Theme.colors.primary.copy(ChipDefaults.OutlinedBorderOpacity)
                        ),
                        modifier = Modifier.padding(ContentPadding.small)
                    ) {
                        Label(
                            text = it.title.get,
                            modifier = Modifier.padding(end = ContentPadding.small),
                            style = Theme.typography.caption
                        )
                        Icon(
                            imageVector = it.icon,
                            contentDescription = "",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Members(state: Members) {
    val selected = state.selected
    // The confirm is a stata variable
    // that holds the value of current confirmation action
    var confirm by rememberState<Action?>(initial = null)
    // show confirm dialog for playlist Add.
    //irrespective of it is called on single or multiple items.
    if (confirm == Action.PlaylistAdd) {
        Playlists(
            value = state.playlists.collectAsState(initial = emptyList()).value,
            expanded = true,
            onPlaylistClick = {
                if (it != null) {
                    state.addToPlaylist(it.name)
                }
                confirm = null
            }
        )
    }

    if (confirm == Action.Delete)
        com.primex.material2.dialog.AlertDialog(
            title = "Delete",
            message = "You are about to remove items. This can't be undone. \nAre you sure?",
            vectorIcon = Icons.Outlined.DeleteForever,
            onDismissRequest = { action ->
                when (action) {
                    true -> state.delete()
                    else -> {}
                }
                confirm = null
            }
        )


    // perform action. show dialogs maybe.
    val onPerformAction = { action: Action ->
        when (action) {
            // show dialog
            Action.PlaylistAdd, Action.Properties -> confirm = action
            Action.AddToQueue -> state.addToQueue()
            Action.PlayNext -> state.playNext()
            Action.Delete -> confirm = action
            Action.SelectAll -> state.selectAll()
            Action.Shuffle -> state.play(true)
            Action.Play -> state.play(false)
            else -> error("Action: $action not supported.")
        }
    }
    // The actual content of the directory
    Directory(
        viewModel = state,
        cells = GridCells.Fixed(1),
        onAction = onPerformAction,
        key = { it.uri },
    ) { member ->
        // emit checked for each item.
        val checked by remember {
            derivedStateOf {
                selected.contains(member.uri)
            }
        }
        // Check if the current member has the focus.
        // if true make it show more options.
        val focused = state.focused == member.uri
        Member(
            value = member,
            onAction = onPerformAction,
            checked = checked,
            focused = focused,
            actions = state.actions,
            modifier = Modifier
                .animateContentSize()
                .animateItemPlacement()
                .combinedClickable(
                    onClick = {
                        when {
                            selected.isNotEmpty() -> state.select(member.uri)
                            // change focused to current.
                            !focused -> state.focused = member.uri
                            // cause the playlist to start playing from current track.
                            else -> state.play(false)
                        }
                    },
                    onLongClick = {
                        state.select(member.uri)
                    }
                )
                .padding(horizontal = ContentPadding.normal)
        )
    }
}
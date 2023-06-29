package com.prime.media.directory.store

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prime.media.R
import com.prime.media.Theme
import com.prime.media.core.ContentElevation
import com.prime.media.core.ContentPadding
import com.prime.media.core.compose.LocalNavController
import com.prime.media.core.compose.composable
import com.prime.media.core.compose.Image
import com.prime.media.core.compose.directory.Action
import com.prime.media.core.compose.directory.Directory

import com.prime.media.core.db.Audio
import com.prime.media.directory.dialogs.Playlists
import com.prime.media.directory.dialogs.Properties
import com.prime.media.impl.albumUri
import com.prime.media.impl.key
import com.primex.core.*
import com.primex.material2.*

private val ARTWORK_SIZE = 48.dp

@OptIn(ExperimentalMaterialApi::class)
@Composable
@NonRestartableComposable
private fun Audio(
    value: Audio,
    actions: List<Action>,
    modifier: Modifier = Modifier,
    focused: Boolean = false,
    checked: Boolean = false,
    favourite: Boolean = false,
    onAction: (Action) -> Unit
) {
    ListTile(
        selected = checked,
        centreVertically = false,
        modifier = modifier,

        overlineText = {
            Label(
                text = value.name,
                style = Theme.typography.body1,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
        },
        text = {
            Label(
                style = Theme.typography.caption,
                text = value.album,
                modifier = Modifier.padding(top = ContentPadding.small),
                color = LocalContentColor.current.copy(ContentAlpha.disabled),
                fontWeight = FontWeight.SemiBold,
            )
        },
        secondaryText = {
            Label(
                text = value.artist,
                fontWeight = FontWeight.SemiBold,
                style = Theme.typography.caption
            )
        },
        leading = {
            Image(
                data = value.albumUri,
                fallback = painterResource(id = R.drawable.default_art),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .border(2.dp, Color.White, shape = CircleShape)
                    .shadow(ContentElevation.high, shape = CircleShape)
                    .size(ARTWORK_SIZE)
                    .wrapContentSize(Alignment.TopCenter)
                    .requiredSize(70.dp),
            )
        },
        trailing = {
            IconButton(
                contentDescription = null,
                imageVector = if (favourite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                onClick = { onAction(Action.Make) },
                // TODO: Currently we don't know how to grant this focus
                // Hence we have disabled it. if not in focus.
                enabled = focused
            )
        },
        bottom = composable(focused) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(
                        start = ARTWORK_SIZE,
                        end = ContentPadding.normal
                    )
            ) {
                actions.forEach {
                    val color = ChipDefaults.outlinedChipColors(backgroundColor = Color.Transparent)
                    Chip(
                        onClick = { onAction(it) },
                        colors = color,
                        border =
                        BorderStroke(
                            1.dp, Theme.colors.primary.copy(ChipDefaults.OutlinedBorderOpacity)
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
fun Audios(state: Audios) {
    val favourites by state.favourites.collectAsState(initial = emptyList())
    val selected = state.selected
    var confirm by rememberState<Action?>(initial = null)

    // show conform irrespective of it is called on single or multiple items.
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

    // handle the action logic.
    val navigator = LocalNavController.current
    val context = LocalContext.current
    val onPerformAction = { action: Action ->
        when (action) {
            // show dialog
            Action.PlaylistAdd, Action.Properties -> confirm = action
            Action.Make -> state.toggleFav()
            Action.Share -> state.share(context)
            Action.AddToQueue -> state.addToQueue()
            Action.PlayNext -> state.playNext()
            Action.Delete -> state.delete()
            Action.SelectAll -> state.selectAll()
            Action.GoToAlbum -> state.toAlbum(navigator)
            Action.GoToArtist -> state.toArtist(navigator)
            Action.Shuffle -> state.play(true)
            Action.Play -> state.play(false)
            else -> error("Action: $action not supported.")
        }
    }

    // extend the Directory.
    Directory(
        viewModel = state,
        cells = GridCells.Fixed(1),
        onAction = onPerformAction,
        key = { it.id },
    ) { audio ->
        // emit checked for each item.
        val checked by remember {
            derivedStateOf {
                selected.contains("${audio.id}")
            }
        }
        val favourite by remember {
            derivedStateOf {
                favourites.contains(audio.key)
            }
        }
        val focused = state.focused == "${audio.id}"

        // if is focused and action is properties
        // show the dialog.
        with(audio) {
            Properties(expanded = confirm == com.prime.media.core.compose.directory.Action.Properties && focused) {
                confirm = null
            }
        }

        // actual content
        Audio(
            value = audio,
            actions = state.actions,
            favourite = favourite,
            checked = checked,
            focused = focused,
            // TODO: need to update focus state on interaction.
            onAction = onPerformAction,
            modifier = Modifier
                .animateContentSize()
                .animateItemPlacement()
                .combinedClickable(
                    onClick = {
                        when {
                            selected.isNotEmpty() -> state.select("${audio.id}")
                            // change focused to current.
                            !focused -> state.focused = "${audio.id}"
                            // cause the playlist to start playing from current track.
                            else -> state.play(false)
                        }
                    },
                    onLongClick = {
                        state.select("${audio.id}")
                    }
                )
                .padding(horizontal = ContentPadding.medium)
        )
    }
}
/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 13-05-2025.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.zs.audiofy.audios

import android.app.Activity
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zs.audiofy.R
import com.zs.audiofy.common.Action
import com.zs.audiofy.common.EDIT
import com.zs.audiofy.common.GO_TO_ALBUM
import com.zs.audiofy.common.INFO
import com.zs.audiofy.common.PLAYLIST_ADD
import com.zs.audiofy.common.compose.ContentPadding
import com.zs.audiofy.common.compose.LocalNavController
import com.zs.audiofy.common.compose.LocalSystemFacade
import com.zs.audiofy.common.compose.LottieAnimatedButton
import com.zs.audiofy.common.compose.LottieAnimatedIcon
import com.zs.audiofy.common.compose.OverflowMenu
import com.zs.audiofy.common.compose.directory.Files
import com.zs.audiofy.editor.RouteEditor
import com.zs.audiofy.playlists.Playlists
import com.zs.audiofy.properties.RouteProperties
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.BaseListItem
import com.zs.compose.theme.LocalContentColor
import com.zs.compose.theme.minimumInteractiveComponentSize
import com.zs.compose.theme.text.Label
import com.zs.core.store.MediaProvider
import com.zs.core.store.models.Audio
import dev.chrisbanes.haze.rememberHazeState
import androidx.compose.foundation.combinedClickable as clickable

private val ArtworkGraphicsModifier = Modifier.graphicsLayer() {
    scaleX = 0.85f; scaleY = 0.85f; this.shadowElevation = 4.dp.toPx();
    shape = CircleShape
    clip = true
}
private val AudioItemPadding = PaddingValues(horizontal = ContentPadding.large)

@Composable
private fun Audio(
    value: Audio,
    actions: @Composable (() -> Unit),
    modifier: Modifier = Modifier
) {
    // TODO - use list item; once alignment is added to it.
    BaseListItem(
        subheading = { Label(text = value.artist) },
        overline = { Label(text = value.album) },
        heading = {
            Label(
                text = value.name,
                maxLines = 2,
                style = AppTheme.typography.body2,
                fontWeight = FontWeight.Bold
            )
        },
        leading = {
            AsyncImage(
                model = MediaProvider.buildAlbumArtUri(value.albumId),
                contentScale = ContentScale.Crop,
                contentDescription = null,
                modifier = Modifier
                    .border(2.dp, LocalContentColor.current, shape = CircleShape)
                    .then(ArtworkGraphicsModifier)
                    .background(AppTheme.colors.background(1.dp))
                    .size(50.dp),
            )
        },
        trailing = actions,
        centerAlign = true,
        padding = AudioItemPadding,
        modifier = modifier
    )
}

/**
 * Represents the state of the audios screen.
 */
@Composable
fun Audios(viewState: AudiosViewState) {
    val selected = viewState.selected
    val favourites by viewState.favourites.collectAsState(emptySet())
    val facade = LocalSystemFacade.current
    val navController = LocalNavController.current
    val surface = rememberHazeState()

    var showPlaylists by remember { mutableStateOf(false) }

    var focused: Audio? by remember { mutableStateOf(null) }
    Playlists (
        showPlaylists,
        viewState.playlists,
        onSelect = { playlist ->
            if (playlist != null) {
                viewState.addToPlaylist(playlist.id, focused)
                focused = null
            }
            showPlaylists = false
        }
    )

    Files(
        viewState,
        surface = surface,
        onTapAction = {
            when(it){
                Action.PLAYLIST_ADD -> showPlaylists = true
                else -> viewState.onPerformAction(it, facade as Activity)
            }
        },
        key = Audio::id,
        itemContent = { audio ->
            Audio(
                value = audio,
                modifier = Modifier
                    .animateItem()
                    .clickable(
                        onClick = {
                            if (viewState.isInSelectionMode)
                                return@clickable viewState.select(audio.id)
                            viewState.play(audio)
                        },
                        onLongClick = { viewState.select(audio.id) }
                    ),
                // actions
                actions = {
                    // show checkbox
                    if (viewState.isInSelectionMode)
                        return@Audio LottieAnimatedIcon(
                            R.raw.lt_checkbox,
                            animationSpec = AppTheme.motionScheme.slowSpatialSpec(),
                            atEnd = audio.id in selected, // if fav
                            contentDescription = null,
                            progressRange = 0.05f..0.30f,
                            scale = 1.6f,
                            tint = AppTheme.colors.accent,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .padding(end = ContentPadding.small)
                        )

                    // show actions
                    Row {
                        // Heart
                        LottieAnimatedButton(
                            R.raw.lt_twitter_heart_filled_unfilled,
                            onClick = { viewState.toggleLiked(audio) },
                            animationSpec = tween(800),
                            atEnd = audio.uri.toString() in favourites, // if fav
                            contentDescription = null,
                            progressRange = 0.13f..1.0f,
                            scale = 3.5f,
                            tint = AppTheme.colors.accent,
                        )
                        // More
                        val onPerformAction = { action: Action ->
                            when (action) {
                                Action.GO_TO_ALBUM -> {
                                    val route =
                                        RouteAudios(RouteAudios.SOURCE_ALBUM, "${audio.albumId}")
                                    navController.navigate(route)
                                }

                                Action.INFO -> navController.navigate(RouteProperties(audio.path))
                                Action.EDIT -> navController.navigate(RouteEditor(audio.path))
                                Action.PLAYLIST_ADD -> {
                                    showPlaylists = true; focused = audio
                                }

                                else -> viewState.onPerformAction(action, facade as Activity, audio)
                            }
                        }

                        OverflowMenu(
                            collapsed = 0,
                            items = viewState.actions,
                            expanded = 5,
                            onItemClicked = onPerformAction
                        )
                    }
                }
            )
        }
    )
}
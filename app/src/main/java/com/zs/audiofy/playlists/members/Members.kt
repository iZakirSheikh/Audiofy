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

package com.zs.audiofy.playlists.members

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zs.audiofy.R
import com.zs.audiofy.common.compose.ContentPadding
import com.zs.audiofy.common.compose.LocalNavController
import com.zs.audiofy.common.compose.LottieAnimatedButton
import com.zs.audiofy.common.compose.LottieAnimatedIcon
import com.zs.audiofy.common.compose.OverflowMenu
import com.zs.audiofy.common.compose.directory.Files
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.BaseListItem
import com.zs.compose.theme.minimumInteractiveComponentSize
import com.zs.compose.theme.text.Label
import com.zs.core.db.playlists.Playlist.Track
import androidx.compose.foundation.combinedClickable as clickable

private val MEMBER_ICON_SHAPE = RoundedCornerShape(30)
private val TrackItemPadding = PaddingValues(horizontal = ContentPadding.large, vertical = ContentPadding.small)

private val CommonTrackStyle = Modifier
    .border(2.dp, Color.White, shape = MEMBER_ICON_SHAPE)
    .shadow(elevation = 8.dp, shape = MEMBER_ICON_SHAPE)
    .size(56.dp)

@Composable
private fun Track(
    value: Track,
    actions: @Composable (() -> Unit),
    modifier: Modifier = Modifier
) {
    // TODO - use list item; once alignment is added to it.
    BaseListItem(
        overline = { Label(text = value.subtitle) },
        heading = {
            Label(
                text = value.title,
                maxLines = 2,
                style = AppTheme.typography.body2,
                fontWeight = FontWeight.Bold
            )
        },
        leading = {
            AsyncImage(
                model = value.artwork,
                contentScale = ContentScale.Crop,
                contentDescription = null,
                modifier = CommonTrackStyle then Modifier.background(AppTheme.colors.background(1.dp)),
            )
        },
        trailing = actions,
        centerAlign = true,
        padding = TrackItemPadding,
        modifier = modifier
    )
}


/**
 * Represents the state of the members screen.
 */
@Composable
fun Members(viewState: MembersViewState) {
    val selected = viewState.selected
    val favourites by viewState.favourites.collectAsState(emptySet())
    val navController = LocalNavController.current
    Files(
        viewState,
        onTapAction = { viewState.onPerformAction(it) },
        key = Track::id,
        itemContent = { audio ->
            Track(
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
                        return@Track LottieAnimatedIcon(
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
                        if (viewState.showFavButton)
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

                        OverflowMenu(
                            collapsed = 0,
                            items = viewState.actions,
                            expanded = 5,
                            onItemClicked = {
                                viewState.onPerformAction(it, audio)
                            }
                        )
                    }
                }
            )
        }
    )
}
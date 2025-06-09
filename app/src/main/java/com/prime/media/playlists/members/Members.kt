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

package com.prime.media.playlists.members

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.prime.media.R
import com.prime.media.common.compose.ContentPadding
import com.prime.media.common.compose.LottieAnimatedIcon
import com.prime.media.common.compose.OverflowMenu
import com.prime.media.common.compose.directory.Files
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.BaseListItem
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.LocalContentColor
import com.zs.compose.theme.minimumInteractiveComponentSize
import com.zs.compose.theme.text.Label
import com.zs.core.db.playlists.Playlist.Track

private val MEMBER_ICON_SHAPE = RoundedCornerShape(30)
private val TrackItemPadding = PaddingValues(horizontal = ContentPadding.large, vertical = ContentPadding.medium)

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

@Composable
fun Members(viewState: MembersViewState) {
    val selected = viewState.selected
    Files(
        viewState,
        onTapAction = {},
        key = Track::id,
        itemContent = { value ->
            Track(
                value = value,
                modifier = Modifier
                    .animateItem()
                    .combinedClickable(
                        onClick = {
                            if (viewState.isInSelectionMode) viewState.select(value.id) else viewState.play()
                        },
                        onLongClick = { viewState.select(value.id) }
                    ),
                actions = {
                    // show checkbox
                    if (viewState.isInSelectionMode)
                        return@Track LottieAnimatedIcon(
                            R.raw.lt_checkbox,
                            animationSpec = AppTheme.motionScheme.slowSpatialSpec(),
                            atEnd = value.id in selected, // if fav
                            contentDescription = null,
                            progressRange = 0.05f..0.30f,
                            scale = 1.6f,
                            tint = Color.Unspecified,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .padding(end = ContentPadding.medium)
                        )
                    // else show overflow menu.
                    Row {
                        // Favourite icon
                        IconButton(onClick = { /*Action fav*/ }) {
                            LottieAnimatedIcon(
                                R.raw.lt_twitter_heart_filled_unfilled,
                                //duration = 800,
                                atEnd = false, // if fav
                                contentDescription = null,
                                progressRange = 0.13f..0.95f,
                                scale = 3.5f,
                                tint = Color.Unspecified
                            )
                        }
                        // Menu.
                        OverflowMenu(
                            collapsed = 0,
                            items = viewState.actions,
                            onItemClicked = {},
                            expanded = 3
                        )
                    }
                }
            )
        }
    )
}
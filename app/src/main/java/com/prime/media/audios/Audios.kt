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

package com.prime.media.audios

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
import com.zs.core.store.MediaProvider
import com.zs.core.store.models.Audio
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
    Files(
        viewState,
        onTapAction = {},
        key = Audio::id,
        itemContent = { value ->
            Audio(
                value = value,
                modifier = Modifier
                    .animateItem()
                    .clickable(
                        onClick = {
                            if (viewState.isInSelectionMode) viewState.select(value.id) else viewState.play()
                        },
                        onLongClick = { viewState.select(value.id) }
                    ),
                actions = {
                    // show checkbox
                    if (viewState.isInSelectionMode)
                        return@Audio LottieAnimatedIcon(
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
                            expanded = -6
                        )
                    }
                }
            )
        }
    )
}
/*
 *  Copyright (c) 2025 Zakir Sheikh
 *
 *  Created by Zakir Sheikh on $today.date.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.zs.audiofy.console

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.twotone.RemoveCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.rememberAsyncImagePainter
import com.zs.audiofy.R
import com.zs.audiofy.common.compose.LottieAnimatedButton
import com.zs.audiofy.common.compose.collectAsState
import com.zs.audiofy.common.compose.emit
import com.zs.audiofy.common.compose.fadingEdge2
import com.zs.audiofy.common.compose.lottie
import com.zs.audiofy.common.compose.lottieAnimationPainter
import com.zs.audiofy.common.compose.shine
import com.zs.compose.foundation.Background
import com.zs.compose.foundation.ImageBrush
import com.zs.compose.foundation.foreground
import com.zs.compose.foundation.stickyHeader
import com.zs.compose.foundation.textResource
import com.zs.compose.foundation.thenIf
import com.zs.compose.foundation.visualEffect
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.ContentAlpha
import com.zs.compose.theme.Icon
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.ListItem
import com.zs.compose.theme.LocalContentColor
import com.zs.compose.theme.adaptive.Scaffold
import com.zs.compose.theme.adaptive.content
import com.zs.compose.theme.appbar.TopAppBar
import com.zs.compose.theme.drawHorizontalDivider
import com.zs.compose.theme.minimumInteractiveComponentSize
import com.zs.compose.theme.text.Header
import com.zs.compose.theme.text.Label
import com.zs.compose.theme.text.TonalHeader
import com.zs.core.playback.MediaFile
import com.zs.core.playback.NowPlaying
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.WindowInsetsSides as WIS
import com.zs.audiofy.common.compose.ContentPadding as CP

private val ThumbnailModifier = Modifier.size(110.dp, 64.dp)

private val NonePlaying = NowPlaying(null, null)
private val NoiseTexture = Modifier.visualEffect(ImageBrush.NoiseBrush, 0.2f, overlay = false)
private val ScrimModifier = Modifier.foreground(Color.Black.copy(0.35f))


@Composable
private fun MediaFile(
    value: MediaFile,
    modifier: Modifier = Modifier,
    playing: Boolean = false,
    actions: @Composable () -> Unit,
) {
    ListItem(
        trailing = actions,
        modifier = modifier.thenIf(playing) {
            NoiseTexture then Modifier.background(AppTheme.colors.accent.copy(ContentAlpha.indication))
        },
        heading = {
            Label(
                text = value.title ?: stringResource(R.string.abbr_not_available),
                maxLines = 2,
                style = AppTheme.typography.title3,
            )

        },
        subheading = {
            Label(
                value.subtitle ?: stringResource(R.string.abbr_not_available),
                style = AppTheme.typography.label3
            )
        },
        leading = {
            // Thumbnail
            Box(
                Modifier
                    .shadow(2.dp, AppTheme.shapes.medium)
                    .background(AppTheme.colors.background(1.dp)) then ThumbnailModifier,
                contentAlignment = Alignment.Center,
                content = {
                    Image(
                        rememberAsyncImagePainter(value.artworkUri),
                        contentDescription = value.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .thenIf(playing) { ScrimModifier }
                    )

                    if (!playing)
                        return@Box
                    Icon(
                        painter = lottieAnimationPainter(R.raw.playback_indicator),
                        contentDescription = null,
                        modifier = Modifier.lottie(),
                        tint = Color.White
                    )
                }
            )
        },
    )
}

/**
 * Represents the layout of the queue.
 */
@Composable
fun Queue(viewState: QueueViewState, shape: Shape, insets: WindowInsets) {
    val state by viewState.state.collectAsState(default = NonePlaying)
    val density = LocalDensity.current
    //
    Scaffold(
        // TODO- Remove thenIf when bug in TwoPane is fixed.
        modifier = Modifier
            .thenIf(insets.getTop(density) == 0) { padding(top = CP.medium) }
            //.clip(shape)
            .shadow(6.dp, shape)
            .border(AppTheme.colors.shine,shape)
        ,
        topBar = {
            TopAppBar(
                navigationIcon = {
                    Icon(
                        Icons.Default.QueueMusic,
                        contentDescription = null,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    )
                },
                windowInsets = insets.only(WIS.Top + WIS.End),
                background = Background(Color.Transparent),
                title = {
                    Label(
                        textResource(R.string.scr_queue_title),
                        maxLines = 2,
                        fontWeight = FontWeight.Light,
                    )
                },
                actions = {
                    // Clear all
                    IconButton(
                        icon = Icons.Outlined.ClearAll,
                        contentDescription = null,
                        onClick = { viewState.clear() },
                    )

                    // Shuffle
                    val accent = AppTheme.colors.accent
                    val onColor = LocalContentColor.current
                    LottieAnimatedButton(
                        id = R.raw.lt_shuffle_on_off,
                        onClick = { viewState.shuffle(!state.shuffle) },
                        atEnd = state.shuffle,
                        progressRange = 0f..0.8f,
                        scale = 1.5f,
                        contentDescription = null,
                        tint = if (state.shuffle
                        ) accent else onColor.copy(ContentAlpha.disabled),
                    )

                    val dispacher = LocalOnBackPressedDispatcherOwner.current
                    IconButton(
                        icon = Icons.Default.Close,
                        contentDescription = null,
                        onClick = { dispacher?.onBackPressedDispatcher?.onBackPressed() },
                    )
                },
                modifier = Modifier.drawHorizontalDivider(LocalContentColor.current.copy(
                    ContentAlpha.divider), thickness = 1.dp)
            )
        },
        content = {
            val items by viewState.queue.collectAsState(null)
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                contentPadding = insets.only(WIS.Bottom + WIS.End)
                    .asPaddingValues(density),
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.content),
                content = {
                    val data = emit(true, items) ?: return@LazyColumn
                    for (index in 0 until data.size) {
                        val file = data[index]
                        val playing = file.mediaUri == state.data

                        // Now Playing Header
                        if (playing)
                            item("header", "now_playing_header") {
                                Header(
                                    textResource(R.string.now_playing),
                                    modifier = Modifier
                                        .animateItem()
                                        .padding(
                                            top = CP.normal,
                                            start = CP.normal,
                                            bottom = CP.small
                                        ),
                                    style = AppTheme.typography.label2,
                                    color = AppTheme.colors.accent
                                )
                            }

                        // List item
                        item(file.mediaUri, "media_file") {
                            MediaFile(
                                file,
                                Modifier
                                    .clickable(enabled = !playing) {
                                        viewState.skipTo(file.mediaUri!!)
                                    }
                                    .animateItem()
                                    .thenIf(playing) {
                                        padding(bottom = CP.normal)
                                    },
                                playing = playing,
                                actions = {
                                    IconButton(
                                        onClick = { viewState.remove(file.mediaUri!!) },
                                        icon = Icons.TwoTone.RemoveCircle,
                                        contentDescription = null
                                    )
                                }
                            )
                        }

                        // Upnext header
                        val prev = if (index > 0) data[index] else null
                        val isUpNext = (index > 0 && index != data.size -1) && prev?.mediaUri == state.data
                        if (isUpNext)
                            stickyHeader(listState, "up_next", "upnext") {
                                TonalHeader(
                                    stringResource(R.string.up_next),
                                    modifier = Modifier
                                        .animateItem()
                                        .padding(start = CP.normal)
                                )
                            }
                    }
                }
            )

            // On First launch, navigate to the current playing item.
            LaunchedEffect(key1 = Unit) {
                delay(300)
                val item = state.data ?: return@LaunchedEffect
                val index = items?.indexOfFirst { it.mediaUri == item } ?: -1
                if (index != -1)
                    listState.scrollToItem(index)
            }
        },
        containerColor = if (AppTheme.colors.isLight) AppTheme.colors.background else AppTheme.colors.background(1.dp)
    )
}
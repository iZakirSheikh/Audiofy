/*
 * Copyright (c)  2026 Zakir Sheikh
 *
 * Created by sheik on 26 of Jan 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Last Modified by sheik on 26 of Jan 2026
 *
 */

package com.prime.media.console

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Scaffold
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Queue
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.prime.media.R
import com.prime.media.common.collectAsState
import com.prime.media.common.emit
import com.prime.media.common.lottie
import com.prime.media.common.shine
import com.primex.core.ImageBrush
import com.primex.core.drawHorizontalDivider
import com.primex.core.foreground
import com.primex.core.textResource
import com.primex.core.thenIf
import com.primex.core.visualEffect
import com.primex.material2.Label
import com.primex.material2.ListTile
import com.zs.core.playback.MediaFile
import com.zs.core.playback.NowPlaying2
import com.zs.core_ui.AppTheme
import com.zs.core_ui.Divider
import com.zs.core_ui.Header
import com.zs.core_ui.Indication
import com.zs.core_ui.LottieAnimatedButton
import com.zs.core_ui.TonalIconButton
import com.zs.core_ui.lottieAnimationPainter
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.WindowInsetsSides as WIS
import com.zs.core_ui.ContentPadding as CP

private val ThumbnailModifier = Modifier.size(110.dp, 64.dp)

private val NonePlaying = NowPlaying2(null, null)
private val NoiseTexture = Modifier.visualEffect(ImageBrush.NoiseBrush, 0.2f, overlay = false)
private val ScrimModifier = Modifier.foreground(Color.Black.copy(0.35f))

@Composable
private fun MediaFile(
    value: MediaFile,
    modifier: Modifier = Modifier,
    playing: Boolean = false,
    actions: @Composable () -> Unit,
) {
    val progress by animateFloatAsState(
        if (playing) 1f else 0f,
        tween(500, 200)
    )
    val colors = AppTheme.colors
    ListTile(
        trailing = actions,
        centerAlign = false,
        modifier = modifier.thenIf(playing) {
            NoiseTexture then Modifier.background(AppTheme.colors.accent.copy(progress * ContentAlpha.Indication))
        },
        onColor = lerp(colors.onBackground, colors.accent, progress),
        headline = {
            val typography = AppTheme.typography
            Label(
                text = value.title ?: stringResource(R.string.abbr_not_available),
                maxLines = 2,
                fontWeight = androidx.compose.ui.text.font.lerp(FontWeight.Normal, FontWeight.Bold, progress),
                style = typography.titleSmall,
            )
        },
        subtitle = {
            Label(
                value.subtitle ?: stringResource(R.string.abbr_not_available),
                style = AppTheme.typography.caption2
            )
        },
        leading = {
            // Thumbnail
            Box(
                Modifier
                    .shadow(2.dp, AppTheme.shapes.compact)
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
    Scaffold (
        modifier = Modifier
            //.clip(shape)
            .shadow(6.dp, shape)
            .border(AppTheme.colors.shine,shape),
        topBar = {
            TopAppBar(
                navigationIcon = {
                    Icon(
                        Icons.Outlined.Queue,
                        contentDescription = null,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    )
                },
                windowInsets = insets.only(WIS.Top + WIS.End),
                backgroundColor =  Color.Transparent,
                modifier = Modifier.drawHorizontalDivider(LocalContentColor.current.copy(
                    ContentAlpha.Divider), thickness = 1.dp),
                elevation = 0.dp,
                title = {
                    Label(
                        textResource(R.string.playing_queue),
                        style = AppTheme.typography.bodyMedium
                    )
                },
                actions = {
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

                    // Clear all
                    val scale = Modifier.scale(0.8f).padding(end = CP.small)
                    TonalIconButton(
                        icon = Icons.Outlined.ClearAll,
                        contentDescription = null,
                        onClick = { viewState.clear() },
                        modifier = scale
                    )

                    val dispacher = LocalOnBackPressedDispatcherOwner.current
                    TonalIconButton (
                        icon = Icons.Outlined.Close,
                        contentDescription = null,
                        onClick = { dispacher?.onBackPressedDispatcher?.onBackPressed() },
                        modifier = scale
                    )
                },
            )
        },
        content = { pd ->
            val items by viewState.queue.collectAsState(null)
            val listState = rememberLazyListState()
            LazyColumn(
                state = listState,
                contentPadding = insets.only(WIS.Bottom + WIS.End)
                    .asPaddingValues(density),
                modifier = Modifier.padding(pd),
                content = {
                    val data = emit(true, items) ?: return@LazyColumn
                    for (index in 0 until data.size) {
                        val file = data[index]
                        val playing = file.mediaUri == state.data

                        // Now Playing Header
                        if (playing)
                            item("now_playing_header", "header") {
                                Header(
                                    textResource(R.string.now_playing),
                                    modifier = Modifier
                                        .animateItem()
                                        .padding(
                                            top = CP.large,
                                            start = CP.large,
                                            bottom = CP.small
                                        ),
                                    style = AppTheme.typography.caption,
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
                                    com.primex.material2.IconButton(
                                        onClick = { viewState.remove(file.mediaUri!!) },
                                        imageVector = Icons.Outlined.RemoveCircleOutline,
                                        contentDescription = null
                                    )
                                }
                            )
                        }

                        // Upnext header
                        val prev = if (index > 0) data[index] else null
                        val isUpNext = (index > 0 && index != data.size -1) && prev?.mediaUri == state.data
                        if (isUpNext)
                            item("up_next", "header") {
                                Header(
                                    stringResource(R.string.up_next),
                                    modifier = Modifier
                                        .animateItem()
                                        .padding(start = CP.normal, top = CP.large)
                                )
                            }
                    }
                }
            )

            // On First launch, navigate to the current playing item.
            LaunchedEffect (key1 = Unit) {
                delay(300)
                val item = state.data ?: return@LaunchedEffect
                val index = items?.indexOfFirst { it.mediaUri == item } ?: -1
                if (index != -1)
                    listState.scrollToItem(index)
            }
        }
    )
}
/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 17-01-2024.
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

package com.prime.media.old.console

import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Surface
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Queue
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.MediaItem
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.prime.media.R
import com.zs.core_ui.ContentPadding
import com.prime.media.old.common.Artwork
import com.prime.media.old.common.LottieAnimButton
import com.prime.media.old.common.Placeholder
import com.prime.media.old.core.playback.artworkUri
import com.prime.media.old.core.playback.mediaUri
import com.prime.media.old.core.playback.subtitle
import com.prime.media.old.core.playback.title
import com.primex.material2.Dialog
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.ListTile
import com.zs.core_ui.AppTheme

private const val TAG = "PlayingQueue"

/**
 * The Shape of the dialog track artwork
 */
private val ArtworkShape = RoundedCornerShape(25)

@Composable
private fun Track(
    value: MediaItem,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false,
    onRemoveRequest: () -> Unit
) {
    ListTile(
        centerAlign = true,
        modifier = modifier,
        color = Color.Transparent,
        overline = { Label(text = value.subtitle.toString()) },
        // Leading icon is the artwork
        leading = {
            Artwork(
                value.artworkUri,
                modifier = Modifier
                    .shadow(ContentPadding.small, clip = true, shape = ArtworkShape)
                    .border(2.dp, color = Color.White, ArtworkShape)
                    .size(50.dp)
            )
        },
        // The title of this track
        headline = {
            Label(
                text = value.title.toString(),
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                style = AppTheme.typography.bodyMedium
            )
        },
        // The playBars animation or drag indicator;
        // depends weather the item is checked or not.
        trailing = {
            Crossfade(
                targetState = isPlaying,
                content = { isPlaying ->
                    when (isPlaying) {
                        // show drag indicator
                        false -> IconButton(
                            onClick = onRemoveRequest,
                            imageVector = Icons.Outlined.RemoveCircleOutline,
                            contentDescription = null
                        )
                        // show playingBars animation.
                        else -> {
                            val composition by rememberLottieComposition(
                                spec = LottieCompositionSpec.RawRes(R.raw.playback_indicator)
                            )
                            // show playing bars.
                            LottieAnimation(
                                composition = composition,
                                iterations = Int.MAX_VALUE,
                                dynamicProperties = rememberLottieDynamicProperties(
                                    rememberLottieDynamicProperty(
                                        property = LottieProperty.COLOR,
                                        AppTheme.colors.accent.toArgb(),
                                        "**"
                                    )
                                ),
                                modifier = Modifier
                                    .requiredSize(24.dp)
                                    .offset(x = -ContentPadding.medium)
                            )
                        }
                    }
                }
            )
        }
    )
}

@Composable
@NonRestartableComposable
private fun TopAppBar(
    state: PlayingQueue,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TopAppBar(
        title = {
            Label(
                text = stringResource(R.string.playing_queue),
                style = AppTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold
            )
        },
        backgroundColor = AppTheme.colors.background(5.dp),
        contentColor = AppTheme.colors.onBackground,
        elevation = 0.dp,
        modifier = modifier,
        // Just for representational purposes.
        navigationIcon = {
            Icon(
                imageVector = Icons.Outlined.Queue,
                contentDescription = null,
                modifier = Modifier.padding(start = ContentPadding.normal)
            )
        },
        // Actions like close, shuffle.
        actions = {

            val ctx = LocalContext.current
            IconButton(
                onClick = { state.clear(ctx) },
                imageVector = Icons.Outlined.ClearAll,
                contentDescription = null,
            )

            val shuffle = state.shuffle
            LottieAnimButton(
                id = R.raw.lt_shuffle_on_off,
                onClick = { state.toggleShuffle() },
                atEnd = !shuffle,
                progressRange = 0f..0.8f,
                scale = 1.5f
            )

            IconButton(
                onClick = onDismissRequest,
                imageVector = Icons.Outlined.Close,
                contentDescription = null
            )
        }
    )
}

context(LazyListScope)
@Suppress("FunctionName", "NOTHING_TO_INLINE")
@OptIn(ExperimentalFoundationApi::class)
private inline fun ListHeader(
    @StringRes res: Int,
    key: String
) {
    item(key = key, contentType = "header") {
        Label(
            text = stringResource(res),
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = ContentPadding.normal,
                    end = ContentPadding.normal,
                    top = ContentPadding.large,
                    bottom = ContentPadding.medium
                )
                .animateItem()
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Content(
    resolver: PlayingQueue,
    modifier: Modifier = Modifier
) {
    val data by resolver.queue.collectAsState(initial = null)
    // compose the state of the passed list.
    // list can be loading = 0 | empty = 1 or loaded = 2
    val listState by remember {
        derivedStateOf {
            when {
                data == null -> 0// loading
                data?.isEmpty() ?: false -> 1// empty
                else -> 2 // show list.
            }
        }
    }
    // construct the lazyList
    val lazyListState = rememberLazyListState()
    val current = resolver.current
    val context = LocalContext.current
    val list: LazyListScope.() -> Unit = list@{
        val list = data ?: return@list
        list.forEach { item ->
            // if this item is the same as the loaded item.
            val isLoaded = item.mediaUri == current?.mediaUri
            if (isLoaded) // emit header
                ListHeader(R.string.now_playing, "now_playing_key")
            // The Track
            // Maybe in future add the possibility of moving media items.
            // The Playback service is ready but there needs the support for
            // moving content in android.
            item(key = item.mediaUri!!, contentType = "list_track") {
                Track(
                    value = item,
                    modifier = Modifier
                        .offset(y = -ContentPadding.medium)
                        .clickable { resolver.playTrack(item.mediaUri!!) }
                        .animateItem()
                        .padding(horizontal = ContentPadding.small),
                    isPlaying = isLoaded,
                    onRemoveRequest = { resolver.remove(context, item.mediaUri!!) }
                )
            }
            // 2nd header.
            if (isLoaded && !resolver.isLast) // emit header
                ListHeader(R.string.up_next, "up_next_key")
        }
    }
    // Show different screen; according to listState.
    Crossfade(targetState = listState, modifier = modifier, label = TAG + "_dialog_state") {
        when (it) {
            // loading
            0 -> Placeholder(title = "Loading", iconResId = R.raw.lt_loading_bubbles)
            // empty; current it will never be the case.
            1 -> Placeholder(title = "Oops empty!!", iconResId = R.raw.lt_empty_box)
            // loaded; show the list.
            else -> {
                LazyColumn(
                    content = list,
                    state = lazyListState,
                    modifier = Modifier.padding(bottom = ContentPadding.small)
                )
                // On First launch, navigate to the current playing item.
                LaunchedEffect(key1 = Unit) {
                    val item = current ?: return@LaunchedEffect
                    val index = data?.indexOfFirst { it.mediaUri == item.mediaUri } ?: -1
                    if (index != -1)
                        lazyListState.scrollToItem(index)
                }
            }
        }
    }
}


/**
 * ## Composable for displaying the playing queue dialog
 *
 * Displays the current playing queue within a dialog-like layout.
 *
 * ### Key points
 *
 * - **Requires wrapping:** Must be used within a `Surface` composable to handle
 *   elevation and shadow effects appropriately.
 *
 * ### Parameters
 *
 * - **state: PlayingQueue**
 *   - The current state of the playing queue, providing essential data for rendering.
 * - **onDismissRequest: () -> Unit**
 *   - A callback function invoked when the dialog should be dismissed.
 * - **modifier: Modifier = Modifier**
 *   - Optional modifier for customizing layout, styling, and behavior.
 *
 * ### Usage
 *
 * ```kotlin
 * Surface {
 *     PlayingQueue(
 *         state = playingQueueState,
 *         onDismissRequest = { /* Handle dismissal logic */ }
 *     )
 * }
 * ```
 */
@Composable
@NonRestartableComposable
fun PlayingQueue(
    state: PlayingQueue,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        TopAppBar(state = state, onDismissRequest = onDismissRequest)
        Content(resolver = state)
    }
}


/**
 * Hosts a customizable dialog that displays the current playing queue.
 *
 * This composable function simplifies managing the dialog's state and interactions,
 * allowing you to focus on the content and user experience.
 *
 * @param state The state of the dialog.
 * @param expanded A boolean flag indicating whether the dialog should be
 *                  displayed or not.
 * @param onDismissRequest A callback function invoked when the user requests
 *                         to dismiss the dialog.
 *
 * @see [PlayingQueue]
 */
@Composable
inline fun PlayingQueue(
    state: PlayingQueue,
    expanded: Boolean,
    noinline onDismissRequest: () -> Unit
) {
    Dialog(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        content = {
            Surface(
                color = AppTheme.colors.background(1.dp),
                shape = AppTheme.shapes.compact,
                content = { PlayingQueue(state = state, onDismissRequest = onDismissRequest) },
                modifier = Modifier
                    .sizeIn(maxWidth = 500.dp, maxHeight = 700.dp)
                    .padding(ContentPadding.xLarge)
                    .animateContentSize(),
            )
        }
    )
}
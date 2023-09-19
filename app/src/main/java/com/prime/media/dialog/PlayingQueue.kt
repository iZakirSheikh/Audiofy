@file:Suppress("CrossfadeLabel")

package com.prime.media.dialog

import androidx.annotation.StringRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Queue
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.prime.media.Material
import com.prime.media.R
import com.prime.media.core.ContentPadding
import com.prime.media.core.compose.Image
import com.prime.media.core.compose.LottieAnimButton
import com.prime.media.core.compose.Placeholder
import com.prime.media.core.playback.artworkUri
import com.prime.media.core.playback.mediaUri
import com.prime.media.core.playback.subtitle
import com.prime.media.core.playback.title
import com.prime.media.small2
import com.prime.media.surfaceColorAtElevation
import com.primex.material2.Dialog
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.ListTile

private const val TAG = "PlayingQueue"

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
            Image(
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
                style = Material.typography.body2
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
        title = { Label(text = stringResource(R.string.playing_queue), style =  Material.typography.body2, fontWeight = FontWeight.Bold) },
        backgroundColor = Material.colors.surfaceColorAtElevation(1.dp),
        contentColor = Material.colors.onSurface,
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
                onClick = { state.clear(ctx)},
                imageVector = Icons.Outlined.ClearAll,
                contentDescription = null,
            )

            val shuffle = state.shuffle
            LottieAnimButton(
                id = R.raw.lt_shuffle_on_off,
                onClick = { state.toggleShuffle()},
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
                .animateItemPlacement()
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
                        .animateItemPlacement()
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
    Crossfade(targetState = listState, modifier = modifier) {
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

// TODO: Maybe Consider moving away from using Dialog; just provide layout.
@Composable
@NonRestartableComposable
fun PlayingQueue(
    state: PlayingQueue,
    expanded: Boolean,
    onDismissRequest: () -> Unit
) {
    Dialog(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        content = {
            Scaffold(
                topBar = { TopAppBar(state = state, onDismissRequest = onDismissRequest) },
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.60f) // different when width > height
                    .clip(Material.shapes.small2),
                // content.
                content = {
                    Content(
                        resolver = state,
                        modifier = Modifier.padding(it)
                    )
                },
                backgroundColor = Material.colors.surface,
            )
        }
    )
}

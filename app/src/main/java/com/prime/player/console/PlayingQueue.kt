@file:Suppress("NOTHING_TO_INLINE")

package com.prime.player.console

import androidx.compose.animation.*
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.Queue
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import coil.compose.AsyncImage
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.prime.player.*
import com.prime.player.R
import com.prime.player.common.*
import com.prime.player.tracks.Header
import com.primex.core.verticalFadingEdge
import com.primex.ui.*
import com.primex.ui.dialog.BottomSheetDialog

private const val TAG = "PlayingQueue"

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun Track(
    value: MediaItem,
    modifier: Modifier = Modifier,
    isPlaying: Boolean = false
) {
    val meta = value.mediaMetadata
    ListTile(
        centreVertically = true,
        modifier = modifier,

        // the leading image is rect in case isPlaying else circle.
        leading = {
            // change between rectangle and circle.
            val radius by animateIntAsState(
                targetValue = if (isPlaying) 20 else 40,
                animationSpec = tween(750)
            )
            AsyncImage(
                meta.artworkUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .shadow(5.dp, clip = true, shape = RoundedCornerShape(radius))
                    .border(2.dp, color = Color.White, RoundedCornerShape(radius))
                    .then(if (isPlaying) Modifier.aspectRatio(1.5f) else Modifier.size(56.dp))
                    .animateContentSize(tween(750, delayMillis = 100))
            )
        },
        text = {
            Label(
                text = meta.title.toString(),
               // style = Material.typography.body1,
                fontWeight = FontWeight.Bold,
                maxLines = 2
            )
        },
        secondaryText = {
            Label(
                text = meta.subtitle.toString(),
                style = Material.typography.caption2
            )
        },

        trailing = {
            // show drag indicator
            if (!isPlaying) {
                IconButton(
                    onClick = { /*TODO*/ },
                    imageVector = Icons.Outlined.DragIndicator,
                    contentDescription = null
                )
            }
            // show playing bars.
            else {
                val composition by rememberLottieComposition(
                    spec = LottieCompositionSpec.RawRes(R.raw.playback_indicator)
                )
                LottieAnimation(
                    composition = composition,
                    iterations = Int.MAX_VALUE,
                    modifier = Modifier
                        .requiredSize(24.dp)
                        .offset(x = -ContentPadding.normal)
                )
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Layout(
    resolver: ConsoleViewModel,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = Material.colors.outline
    Column(modifier = modifier) {

        // top bar mimicking youtube app.
        TopAppBar(
            title = { Label(text = "Playing Queue") },
            backgroundColor = Material.colors.surface,
            contentColor = Material.colors.onSurface,
            elevation = 0.dp,
            navigationIcon = {
                Icon(
                    imageVector = Icons.Outlined.Queue,
                    contentDescription = null,
                    modifier = Modifier.padding(start = ContentPadding.normal)
                )
            },
            actions = {

                val shuffle by resolver.shuffle
                IconButton(
                    onClick = { resolver.toggleShuffle() },
                    painter = painterResource(id = R.drawable.ic_shuffle),
                    contentDescription = null,
                    tint = Material.colors.onSurface.copy(if (shuffle) ContentAlpha.high else ContentAlpha.disabled),
                )


                IconButton(
                    onClick = onDismissRequest,
                    imageVector = Icons.Outlined.Close,
                    contentDescription = null
                )
            },

            // draw the capsule
            modifier = Modifier.drawWithContent {
                drawContent()
                // drawRoundRect(color, size = Size())
                drawRoundRect(
                    cornerRadius = CornerRadius(12f, 12f),
                    color = color,
                    topLeft = Offset(size.width / 2 - 12.dp.toPx(), 8.dp.toPx()),
                    size = Size(25.dp.toPx(), 4.dp.toPx())
                )
            }
        )

        Divider()

        val list by resolver.queue.collectAsState(initial = emptyList())
        LazyColumn(contentPadding = PaddingValues(vertical = ContentPadding.medium)) {
            list.forEachIndexed { index, item ->

                // current playing track
                if (index == 0) {
                    item(key = item.mediaId) {
                        Track(
                            value = item,
                            modifier = Modifier.animateItemPlacement(),
                            isPlaying = true
                        )
                    }
                    return@forEachIndexed
                }


                if (index == 1)
                    item(key = "up_next_header") {
                        Header(
                            text = "Up Next",
                            modifier = Modifier
                                .padding(horizontal = ContentPadding.normal)
                                .padding(top = ContentPadding.normal)
                                .animateItemPlacement(),
                            fontWeight = FontWeight.Bold,
                            color = LocalContentColor.current
                        )
                    }

                // other tracks.
                item(key = item.mediaId) {
                    Track(
                        value = item,
                        modifier = Modifier
                            .offset(y = -ContentPadding.medium)
                            .clickable { resolver.playTrack(item.mediaId.toLong()) }
                            .animateItemPlacement(),
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalComposeApi::class)
@Composable
fun ConsoleViewModel.PlayingQueue(
    expanded: Boolean,
    onDismissRequest: () -> Unit
) {
    BottomSheetDialog(expanded = expanded, onDismissRequest = onDismissRequest) {
        Surface(shape = RoundedCornerShape(topStartPercent = 5, topEndPercent = 5)) {
            Layout(
                resolver = this, onDismissRequest
            )
        }
    }
}
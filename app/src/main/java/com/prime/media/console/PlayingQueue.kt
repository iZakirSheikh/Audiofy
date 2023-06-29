@file:Suppress("NOTHING_TO_INLINE")

package com.prime.media.console

import androidx.compose.animation.*
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Queue
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.prime.media.*
import com.prime.media.R
import com.prime.media.core.ContentPadding
import com.prime.media.core.compose.caption2
import com.prime.media.core.compose.outline
import com.prime.media.impl.key
import com.primex.material2.*

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
                targetValue = if (isPlaying) 15 else 40,
                animationSpec = tween(750)
            )
            com.prime.media.core.compose.Image(
                meta.artworkUri,
                modifier = Modifier
                    .padding(end = ContentPadding.medium)
                    .shadow(5.dp, clip = true, shape = RoundedCornerShape(radius))
                    .border(2.dp, color = Color.White, RoundedCornerShape(radius))
                    .then(if (isPlaying) Modifier.aspectRatio(1.5f) else Modifier.size(53.dp))
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
                style = Theme.typography.caption2
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
    resolver: Console,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val color = Theme.colors.outline
    Column(modifier = modifier) {

        // top bar mimicking youtube app.
        TopAppBar(
            title = { Label(text = "Playing Queue") },
            backgroundColor = Theme.colors.surface,
            contentColor = Theme.colors.onSurface,
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
                    tint = Theme.colors.onSurface.copy(if (shuffle) ContentAlpha.high else ContentAlpha.disabled),
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
                    item(key = item.key) {
                        Track(
                            value = item,
                            modifier = Modifier.animateItemPlacement().padding(horizontal = ContentPadding.normal),
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
                                .padding(ContentPadding.normal)
                                .animateItemPlacement(),
                            fontWeight = FontWeight.Bold,
                            color = LocalContentColor.current
                        )
                    }

                // other tracks.
                // FixMe: use key instead of mediaId
                item(key = item.key) {
                    Track(
                        value = item,
                        modifier = Modifier
                            .offset(y = -ContentPadding.medium)
                            .clickable { resolver.playTrack(item.requestMetadata.mediaUri!!) }
                            .animateItemPlacement()
                            .padding(horizontal = ContentPadding.normal),
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalComposeApi::class, ExperimentalComposeUiApi::class)
@Composable
fun Console.PlayingQueue(
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
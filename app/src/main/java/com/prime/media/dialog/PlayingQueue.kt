@file:Suppress("NOTHING_TO_INLINE")

package com.prime.media.dialog

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Surface
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DragIndicator
import androidx.compose.material.icons.outlined.Queue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ExperimentalComposeApi
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.prime.media.Material
import com.prime.media.R
import com.prime.media.core.ContentPadding
import com.prime.media.caption2
import com.prime.media.outline
import com.prime.media.core.util.key
import com.primex.material2.BottomSheetDialog
import com.primex.material2.Header
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.ListTile

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
                    .then(if (isPlaying) Modifier.height(60.dp).aspectRatio(1.5f) else Modifier.size(53.dp))
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
                        .offset(x = -ContentPadding.medium)
                )
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Layout(
    resolver: PlayingQueue,
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

                val shuffle = resolver.shuffle
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
                    item(key = item.key) {
                        Track(
                            value = item,
                            modifier = Modifier
                                .animateItemPlacement()
                                .padding(horizontal = ContentPadding.normal),
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
@NonRestartableComposable
fun PlayingQueue(
    state: PlayingQueue,
    expanded: Boolean,
    onDismissRequest: () -> Unit
) {
    BottomSheetDialog(expanded = expanded, onDismissRequest = onDismissRequest) {
        Surface(shape = RoundedCornerShape(topStartPercent = 5, topEndPercent = 5)) {
            Layout(
                resolver = state, onDismissRequest
            )
        }
    }
}
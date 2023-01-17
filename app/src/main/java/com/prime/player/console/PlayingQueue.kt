@file:Suppress("NOTHING_TO_INLINE")

package com.prime.player.console

import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Divider
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition
import com.prime.player.Material
import com.prime.player.R
import com.prime.player.common.*
import com.prime.player.overlay
import com.primex.core.verticalFadingEdge
import com.primex.ui.Label
import com.primex.ui.ListTile
import com.primex.ui.PrimeDialog


@Composable
private fun Track(
    value: MediaItem,
    playing: Boolean = false,
    modifier: Modifier = Modifier,
) {
    ListTile(
        overlineText = null,
        text = {
            Label(
                text = value.mediaMetadata.title.toString(),
                fontWeight = FontWeight.SemiBold
            )
        },
        secondaryText = {
            Label(
                text = value.mediaMetadata.subtitle.toString(),
                // fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth(0.75f)
            )
        },
        modifier = modifier,
        leading = {
            Surface(
                modifier = Modifier.requiredSize(48.dp),
                shape = CircleShape,
                border = BorderStroke(2.dp, Color.White),
                elevation = ContentElevation.medium,
                content = {
                    Image(data = value.mediaMetadata.artworkUri)
                    if (playing) {
                        val composition by rememberLottieComposition(
                            spec = LottieCompositionSpec.RawRes(
                                R.raw.playback_indicator,
                            )
                        )
                        LottieAnimation(
                            composition = composition,
                            iterations = Int.MAX_VALUE,
                            modifier = Modifier.requiredSize(18.dp)
                        )
                    }
                }
            )
        },
    )
}

@Composable
fun ConsoleViewModel.PlayingQueue(
    expanded: Boolean,
    onDismissRequest: () -> Unit
) {
    if (expanded) {
        val list by queue.collectAsState(initial = emptyList())
        val current by current
        val state = rememberLazyListState()
        val playing by playing
        val secondary = Material.colors.secondary


        PrimeDialog(
            title = "Playing Queue",
            onDismissRequest = onDismissRequest,
            vectorIcon = Icons.Outlined.PlaylistPlay,
            button2 = stringResource(id = R.string.dismiss) to onDismissRequest,
            topBarBackgroundColor = Material.colors.overlay,
            topBarContentColor = Material.colors.onSurface,
        ) {
            LazyColumn(
                state = state,
                modifier = Modifier
                    .heightIn(min = 350.dp, max = 350.dp)
                    .verticalFadingEdge(color = Material.colors.surface, state = state),
                contentPadding = PaddingValues(vertical = ContentPadding.medium)
            ) {

                items(
                    list,
                    key = { it.mediaId },
                    contentType = { "Audio_file" }
                ) { audio ->

                    Crossfade(
                        targetState = audio.mediaId== current?.mediaId,
                        modifier = Modifier
                            .wrapContentSize()
                            .animateContentSize(animationSpec = tween(Anim.LongDurationMills))
                    ) { show ->
                        if (show)
                            Column {
                                Label(
                                    text = "Now Playing",
                                    modifier = Modifier.padding(
                                        top = ContentPadding.normal,
                                        start = ContentPadding.normal,
                                        bottom = ContentPadding.medium
                                    ),
                                    fontWeight = FontWeight.SemiBold,
                                    color = secondary
                                )
                                Divider(
                                    modifier = Modifier.padding(horizontal = ContentPadding.normal),
                                    color = secondary.copy(0.12f)
                                )
                            }
                    }

                    Track(
                        value = audio,
                        playing = current?.mediaId == audio.mediaId && playing,
                        modifier = Modifier.clickable(enabled = audio.mediaId != current?.mediaId) {
                            playTrack(audio.mediaId.toLong())
                        }
                    )

                    Crossfade(
                        targetState = audio.mediaId == current?.mediaId,
                        modifier = Modifier
                            .wrapContentSize()
                            .animateContentSize(animationSpec = tween(Anim.LongDurationMills))
                    ) { show ->
                        if (show)
                            Column {
                                Label(
                                    text = "Up Next",
                                    modifier = Modifier.padding(
                                        top = ContentPadding.large,
                                        start = ContentPadding.normal,
                                        bottom = ContentPadding.medium
                                    ),
                                    fontWeight = FontWeight.SemiBold,
                                    color = secondary
                                )
                                Divider(
                                    modifier = Modifier.padding(horizontal = ContentPadding.normal),
                                    color = secondary.copy(0.12f)
                                )
                            }
                    }

                }
            }
        }

        /*LaunchedEffect(key1 = Unit) {
            current?.let {
                state.scrollToItem(list.indexOf(it))
            }
        }*/
    }
}

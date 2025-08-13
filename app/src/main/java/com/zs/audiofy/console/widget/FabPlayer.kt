@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.zs.audiofy.console.widget

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zs.audiofy.R
import com.zs.audiofy.common.compose.ContentPadding
import com.zs.audiofy.common.compose.lottie
import com.zs.audiofy.common.compose.lottieAnimationPainter
import com.zs.audiofy.common.compose.shine
import com.zs.audiofy.console.Console
import com.zs.compose.foundation.SignalWhite
import com.zs.compose.foundation.foreground
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.Icon
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.sharedElement
import com.zs.core.playback.NowPlaying


/** Represents the compact [Widget] of the console. */
@Composable
fun FabPlayer(
    state: NowPlaying,
    onAction: (code: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = AppTheme.shapes.large
    Box(
        modifier = modifier
            .sharedElement(Console.ID_BACKGROUND)
            .border(AppTheme.colors.shine, shape)
            .shadow(8.dp, shape)
            .background(AppTheme.colors.background(1.dp)) then Widget.FabRequiredSize,
        content = {
            // Artwork
            AsyncImage(
                state.artwork,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .sharedElement(Console.ID_ARTWORK)
                    .matchParentSize()
                    .clip(shape)
                    .foreground(Color.Black.copy(0.5f))
            )

            // Playing bars.
            Icon(
                painter = lottieAnimationPainter(
                    R.raw.playback_indicator,
                    isPlaying = state.playing
                ),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = ContentPadding.medium)
                    .sharedElement(Console.ID_PLAYING_INDICATOR)
                    .lottie()
                    .align(Alignment.CenterStart),
                tint = Color.SignalWhite
            )

            // Play Toggle
            IconButton(
                onClick = { onAction(Widget.ACTION_PLAY_TOGGLE) },
                modifier = Modifier
                    .sharedElement(Console.ID_BTN_PLAY_PAUSE)
                    .align(Alignment.CenterEnd),
                content = {
                    Icon(
                        painter = lottieAnimationPainter(
                            id = R.raw.lt_play_pause,
                            atEnd = state.playing,
                            progressRange = 0.0f..0.29f,
                            animationSpec = tween(easing = LinearEasing)
                        ),
                        contentDescription = null,
                        tint = Color.SignalWhite,
                        modifier = Modifier.lottie(1.5f)
                    )
                },
            )
        }
    )
}
/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 05-09-2024.
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

@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.zs.audiofy.console.widget.styles

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zs.audiofy.R
import com.zs.audiofy.common.compose.ContentPadding
import com.zs.audiofy.common.compose.lottie
import com.zs.audiofy.common.compose.lottieAnimationPainter
import com.zs.audiofy.common.compose.marque
import com.zs.audiofy.common.compose.shine
import com.zs.audiofy.console.RouteConsole
import com.zs.audiofy.console.widget.Widget
import com.zs.compose.foundation.ImageBrush
import com.zs.compose.foundation.SignalWhite
import com.zs.compose.foundation.UmbraGrey
import com.zs.compose.foundation.blend
import com.zs.compose.foundation.visualEffect
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.BaseListItem
import com.zs.compose.theme.Colors
import com.zs.compose.theme.FloatingActionButton
import com.zs.compose.theme.Icon
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.sharedBounds
import com.zs.compose.theme.sharedElement
import com.zs.compose.theme.text.Label
import com.zs.core.playback.NowPlaying


private val Colors.bg: Brush
    @Composable
    get() {
        val accent = accent.copy(0.85f)
        return Brush.horizontalGradient(
            listOf(
                accent,
                accent.blend(Color.SignalWhite, 0.5f),
                accent.blend(Color.SignalWhite, 0.7f),
                accent.blend(Color.SignalWhite, 0.5f),
                accent.blend(Color.SignalWhite, 0.2f)
            )
        )
    }

private val DefaultArtworkSize = 70.dp
private val DefaultArtworkShape = RoundedCornerShape(
    30, 6, 30, 6
)
private val WidgetShape = DefaultArtworkShape

/**
 * A Modifier that creates a "double vision" effect by drawing the content twice with a slight offset.
 *
 * @return A Modifier that applies the double vision effect.
 */
private fun Modifier.offsetDoubleVision() =
    drawWithContent {
        drawIntoCanvas { canvas ->
            canvas.save()
            canvas.translate(5.dp.toPx(), -5.dp.toPx())
            //drawRect(Color.Black.copy(0.5f))
            drawContent()
            canvas.restore()
        }
        drawContent()
    }

@Composable
fun GradientGroves(
    state: NowPlaying,
    onRequest: (code: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val contentColor = Color.UmbraGrey
    BaseListItem(
        modifier = modifier
            .sharedBounds(RouteConsole.ID_BACKGROUND)
            .visualEffect(ImageBrush.NoiseBrush, 0.4f, overlay = true)
            .border(colors.shine, WidgetShape)
            .background(Color.White, WidgetShape)
            .background(colors.bg, WidgetShape),
        contentColor = contentColor,
        // Title
        heading = {
            Box(
                modifier = Modifier
                    .sharedElement(RouteConsole.ID_TITLE)
                    .clipToBounds(),
                content = {
                    Label(
                        state.title ?: stringResource(R.string.unknown),
                        modifier = Modifier.marque(Int.MAX_VALUE),
                        style = AppTheme.typography.headline3,
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        // Subtitle
        overline = {
            Label(
                state.subtitle ?: stringResource(R.string.unknown),
                style = AppTheme.typography.label3,
            )
        },
        // AlbumArt
        trailing = {
            AsyncImage(
                model = state.artwork,
                contentScale = ContentScale.Crop,
                contentDescription = null,
                modifier = Modifier
                    .size(DefaultArtworkSize)
                    .sharedElement(RouteConsole.ID_ARTWORK)
                    .offsetDoubleVision()
                    .border(1.dp, Color.SignalWhite, DefaultArtworkShape)
                    .shadow(8.dp, DefaultArtworkShape)
                    .background(colors.accent)
            )
        },
        // controls
        subheading = {
            Row(
                horizontalArrangement = ContentPadding.SmallArrangement,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = ContentPadding.medium)
                    .fillMaxWidth(),
                content = {
                    val bgModifier = Modifier
                        .scale(0.88f)
                        .border(colors.shine, CircleShape)
                        .background(AppTheme.colors.accent.copy(0.3f), CircleShape)
                    // SeekBackward
                    IconButton(
                        onClick = { onRequest(Widget.REQUEST_SKIP_TO_PREVIOUS) },
                        icon = Icons.Outlined.KeyboardDoubleArrowLeft,
                        contentDescription = null,
                        modifier = bgModifier
                    )

                    // Play/Pause
                    FloatingActionButton(
                        backgroundColor = colors.accent.blend(Color.SignalWhite, 0.2f),
                        contentColor = contentColor,
                        shape = RoundedCornerShape(28),
                        modifier = Modifier.sharedElement(RouteConsole.ID_BTN_PLAY_PAUSE),
                        onClick = { onRequest(Widget.REQUEST_PLAY_TOGGLE) },
                        content = {
                            Icon(
                                painter = lottieAnimationPainter(
                                    id = R.raw.lt_play_pause,
                                    atEnd = state.playing,
                                    progressRange = 0.0f..0.29f,
                                    animationSpec = tween(easing = LinearEasing)
                                ),
                                modifier = Modifier.lottie(1.5f),
                                contentDescription = null
                            )
                        },
                    )

                    // SeekNext
                    IconButton(
                        onClick = { onRequest(Widget.REQUEST_SKIP_TO_NEXT) },
                        icon = Icons.Outlined.KeyboardDoubleArrowRight,
                        contentDescription = null,
                        modifier = bgModifier
                    )

                    // Open Console
                    IconButton(
                        icon = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = null,
                        onClick = { onRequest(Widget.REQUEST_OPEN_CONSOLE) },
                        modifier = bgModifier,
                    )
                }
            )
        }
    )
}
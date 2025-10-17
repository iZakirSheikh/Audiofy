/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 26-02-2025.
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
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zs.audiofy.R
import com.zs.audiofy.common.compose.ContentPadding
import com.zs.audiofy.common.compose.LottieAnimatedButton
import com.zs.audiofy.common.compose.marque
import com.zs.audiofy.common.shapes.CompactDisk
import com.zs.audiofy.console.RouteConsole
import com.zs.audiofy.console.widget.Widget
import com.zs.compose.foundation.SignalWhite
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.BaseListItem
import com.zs.compose.theme.ContentAlpha
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.LocalContentColor
import com.zs.compose.theme.sharedBounds
import com.zs.compose.theme.sharedElement
import com.zs.compose.theme.text.Label
import com.zs.core.playback.NowPlaying

private val DefaultArtworkSize = 84.dp
private val DefaultArtworkShape = CompactDisk
private val Shape = RoundedCornerShape(25, 8, 25, 8)

private val PlayButtonShape = RoundedCornerShape(28)
private val WidgetContentPadding = PaddingValues(8.dp, 6.dp)
private val TitleDrawStyle = Stroke(width = 2.8f, join = StrokeJoin.Round)


@Composable
fun DiskDynamo(
    state: NowPlaying,
    onRequest: (code: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = AppTheme.colors
    val contentColor = colors.onBackground
    BaseListItem(
        contentColor = contentColor,
        centerAlign = true,
        padding = WidgetContentPadding,
        modifier = modifier
            .sharedBounds(RouteConsole.ID_BACKGROUND)
            .border(0.5.dp, colors.background(30.dp), Shape)
            .shadow(12.dp, Shape)
            .background(colors.background(1.dp)),

        // AlbumArt
        leading = {
            val infiniteTransition = rememberInfiniteTransition()
            val degrees by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 3000, easing = LinearEasing),
                )
            )
            AsyncImage(
                model = state.artwork,
                contentScale = ContentScale.Crop,
                contentDescription = null,
                modifier = Modifier
                    .size(DefaultArtworkSize)
                    .sharedElement(RouteConsole.ID_ARTWORK)
                    .graphicsLayer() {
                        rotationZ = if (!state.playing) 0f else degrees
                        scaleX = 1.1f
                        scaleY = 1.1f
                        this.shape = DefaultArtworkShape
                        shadowElevation = 12.dp.toPx()
                        clip = true
                    }
                    .border(1.dp, Color.SignalWhite, DefaultArtworkShape)
            )
        },

        // Title
        overline = {
            Box(
                modifier = Modifier
                    .sharedElement(RouteConsole.ID_TITLE)
                    .clipToBounds(),
                content = {
                    Label(
                        state.title ?: stringResource(R.string.unknown),
                        modifier = Modifier.marque(Int.MAX_VALUE),
                        style = AppTheme.typography.headline3.copy(
                            drawStyle = TitleDrawStyle,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                }
            )
        },

        // Subtitle
        heading = {
            Label(
                state.subtitle ?: stringResource(R.string.unknown),
                style = AppTheme.typography.label3,
                color = LocalContentColor.current.copy(ContentAlpha.medium),
                modifier = Modifier.sharedElement(RouteConsole.ID_SUBTITLE),
            )
        },

        // Controls
        subheading = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = ContentPadding.medium)
                    .fillMaxWidth(),
                content = {
                    val IconModifier = Modifier
                        .scale(0.75f)
                        .background(colors.background(10.dp), CircleShape)
                    // SeekBackward
                    IconButton(
                        onClick = { onRequest(Widget.REQUEST_SKIP_TO_PREVIOUS) },
                        icon = Icons.Outlined.KeyboardDoubleArrowLeft,
                        contentDescription = null,
                        modifier = IconModifier
                    )

                    // Play/Pause
                    LottieAnimatedButton(
                        id = R.raw.lt_play_pause3,
                        atEnd = state.playing,
                        scale = 2f,
                        progressRange = 0.0f..0.48f,
                        animationSpec = tween(easing = LinearEasing),
                        onClick = { onRequest(Widget.REQUEST_PLAY_TOGGLE) },
                        contentDescription = null,
                        tint = contentColor
                    )

                    // SeekForward
                    IconButton(
                        onClick = { onRequest(Widget.REQUEST_SKIP_TO_NEXT) },
                        icon = Icons.Outlined.KeyboardDoubleArrowRight,
                        contentDescription = null,
                        modifier = IconModifier
                    )

                    //
                    Spacer(Modifier.weight(1f))

                    // launch console
                    IconButton(
                        icon = Icons.AutoMirrored.Outlined.OpenInNew,
                        contentDescription = null,
                        //   tint = accent
                        onClick = { onRequest(Widget.REQUEST_OPEN_CONSOLE) },
                        modifier = IconModifier,
                    )
                }
            )
        }
    )
}
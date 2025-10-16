@file:OptIn(ExperimentalSharedTransitionApi::class)

/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 24-02-2025.
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


package com.zs.audiofy.console.widget.styles

import android.text.format.DateUtils
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowRight
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zs.audiofy.R
import com.zs.audiofy.common.compose.LottieAnimatedButton
import com.zs.audiofy.common.compose.background
import com.zs.audiofy.common.compose.chronometer
import com.zs.audiofy.common.compose.lottie
import com.zs.audiofy.common.compose.lottieAnimationPainter
import com.zs.audiofy.common.compose.marque
import com.zs.audiofy.common.compose.shine
import com.zs.audiofy.common.shapes.RoundedStarShape
import com.zs.audiofy.console.RouteConsole
import com.zs.audiofy.console.widget.Widget
import com.zs.audiofy.settings.AppConfig
import com.zs.compose.foundation.UmbraGrey
import com.zs.compose.foundation.textResource
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.BaseListItem
import com.zs.compose.theme.ContentAlpha
import com.zs.compose.theme.Icon
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.LocalContentColor
import com.zs.compose.theme.Slider
import com.zs.compose.theme.SliderDefaults
import com.zs.compose.theme.TonalIconButton
import com.zs.compose.theme.sharedBounds
import com.zs.compose.theme.sharedElement
import com.zs.compose.theme.text.Label
import com.zs.core.playback.NowPlaying
import com.zs.core.playback.Remote
import com.zs.audiofy.common.compose.ContentPadding as CP

private const val TAG = "RotatingGradient"

private val Shape = RoundedCornerShape(12)
private val ArtworkSize = 100.dp
private val ArtworkShape = /*RoundedPolygonShape(5, 0.3f)*/RoundedStarShape(15, 0.03)
private val TitleDrawStyle = Stroke(width = 2.5f, join = StrokeJoin.Round)

// Example changing dynamic gradient
//dynamicProperties = rememberLottieDynamicProperties(
//                        rememberLottieDynamicProperty(
//                            LottieProperty.GRADIENT_COLOR,
//                            arrayOf(
//                                accent.toArgb(),
//                                accent.hsl(lightness = accent.luminance() *  0.85f).toArgb(),
//                                accent.hsl(lightness = accent.luminance() *  0.80f).toArgb(),
//                            ),
//                            "**"
//                        )
//                    )


@Composable
fun RotatingColorGradient(
    state: NowPlaying,
    onRequest: (code: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val contentColor = Color.UmbraGrey
    BaseListItem(
        contentColor = contentColor,
        spacing = CP.small,
        modifier = modifier
            .sharedBounds(RouteConsole.ID_BACKGROUND)
            .shadow(8.dp, Shape)
            .border(AppTheme.colors.shine, Shape)
            .background(
                lottieAnimationPainter(R.raw.bg_rotating_color_gradient),
                contentScale = ContentScale.Crop
            ),
        padding = PaddingValues(horizontal = CP.medium, vertical = CP.small),
        // subtitle
        overline = {
            Label(
                state.subtitle ?: textResource(R.string.unknown),
                style = AppTheme.typography.label3,
                color = contentColor.copy(ContentAlpha.medium),
                modifier = Modifier
                    .padding(top = CP.normal)
                    .sharedElement(RouteConsole.ID_SUBTITLE),
            )
        },

        // Title
        heading = {
            Box(
                modifier = Modifier
                    .sharedElement(RouteConsole.ID_TITLE)
                    .clipToBounds(),
                content = {
                    Label(
                        state.title ?: textResource(R.string.unknown),
                        style = AppTheme.typography.headline3.copy(
                            drawStyle = TitleDrawStyle,
                            fontWeight = FontWeight.Medium,
                        ),
                        modifier = Modifier.marque(Int.MAX_VALUE),
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        },
        // Artwork
        leading = {
            AsyncImage(
                model = state.artwork,
                modifier = Modifier
                    .sharedElement(RouteConsole.ID_ARTWORK)
                    .clip(ArtworkShape)
                    .border(0.5.dp, LocalContentColor.current, ArtworkShape)
                    .background(AppTheme.colors.background(1.dp))
                    .size(ArtworkSize),
                contentScale = ContentScale.Crop,
                contentDescription = null
            )
        },
        // Expand to Fill
        trailing = {
            // Expand to fill
            TonalIconButton (
                icon = Icons.AutoMirrored.Outlined.OpenInNew,
                contentDescription = null,
                onClick = { onRequest(Widget.REQUEST_OPEN_CONSOLE) },
                modifier = Widget.SmallIconBtn then Modifier.offset(x = 7.dp),
            )
        },

        // Controls
        subheading = {
            Row(
                horizontalArrangement = CP.xSmallArrangement,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().offset(x = -CP.medium),
                content = {
                    // Skip to Prev
                    IconButton(
                        onClick = { onRequest(Widget.REQUEST_SKIP_TO_PREVIOUS) },
                        icon = Icons.Outlined.KeyboardDoubleArrowLeft,
                        contentDescription = null,
                        tint = contentColor
                    )

                    // Play/Pause
                    LottieAnimatedButton(
                        id = R.raw.lt_play_pause_circle_bordered,
                        atEnd = state.playing,
                        scale = 7f,
                        progressRange = 0.0f..0.7f,
                        animationSpec = tween(easing = LinearEasing),
                        onClick = { onRequest(Widget.REQUEST_PLAY_TOGGLE) },
                        contentDescription = null,
                        tint = contentColor
                    )

                    // Skip to Next
                    IconButton(
                        onClick = { onRequest(Widget.REQUEST_SKIP_TO_NEXT) },
                        icon = Icons.Outlined.KeyboardDoubleArrowRight,
                        contentDescription = null,
                        tint = contentColor
                    )
                }
            )
        },
        // Progress
        footer = {
            Row(
                horizontalArrangement = CP.SmallArrangement,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                content = {
                    // Playing bars.
                    Icon(
                        painter = lottieAnimationPainter(
                            R.raw.playback_indicator,
                            isPlaying = state.playing
                        ),
                        contentDescription = null,
                        modifier = Modifier
                            .sharedElement(RouteConsole.ID_PLAYING_INDICATOR)
                            .lottie(),
                        tint = contentColor
                    )

                    val chronometer = state.chronometer
                    val position = chronometer.elapsed
                    // Position
                    Label(
                        when (position) {
                            Long.MIN_VALUE-> stringResource(R.string.abbr_not_available)
                            else -> DateUtils.formatElapsedTime((position / 1000 ))
                        },
                        style = AppTheme.typography.label3,
                        color = contentColor.copy(ContentAlpha.medium),
                        fontWeight = FontWeight.Bold
                    )

                    // Slider
                    Slider(
                        chronometer.progress(state.duration),
                        onValueChange = {
                            val mills = (it * state.duration).toLong()
                            chronometer.raw = mills
                        },
                        onValueChangeFinished = {
                            val progress = chronometer.elapsed / state.duration.toFloat()
                            onRequest(progress)
                        },
                        enabled = state.duration > 0,
                        modifier = Modifier
                            .sharedElement(RouteConsole.ID_SEEK_BAR)
                            .weight(1f),
                        colors = SliderDefaults.colors(
                            thumbColor = contentColor,
                            disabledThumbColor = contentColor,
                            activeTrackColor = contentColor
                        )
                    )

                    // Duration
                    val duration = state.duration
                    Label(
                        when  {
                            duration == Remote.TIME_UNSET -> stringResource(R.string.abbr_not_available)
                            else -> DateUtils.formatElapsedTime(duration / 1000)
                        },
                        style = AppTheme.typography.label3,
                        color = contentColor.copy(ContentAlpha.medium),
                        fontWeight = FontWeight.Bold
                    )

                    when {
                        AppConfig.inAppWidgetLongPressOpenConfig -> LottieAnimatedButton(
                            R.raw.lt_twitter_heart_filled_unfilled,
                            onClick = { onRequest(Widget.REQUEST_LIKED) },
                            animationSpec = tween(800),
                            atEnd = state.favourite, // if fav
                            contentDescription = null,
                            progressRange = 0.13f..1.0f,
                            scale = 3.5f,
                            tint = AppTheme.colors.accent,
                            modifier = Modifier.layoutId(RouteConsole.ID_BTN_LIKED) then Widget.SmallIconBtn
                        )
                        else ->  IconButton(
                            icon = Icons.Outlined.Tune,
                            contentDescription = null,
                            onClick = { onRequest(Widget.REQUEST_SHOW_CONFIG) },
                            modifier = Widget.SmallIconBtn
                        )
                    }
                }
            )
        }
    )
}
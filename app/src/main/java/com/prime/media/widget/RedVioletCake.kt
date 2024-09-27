/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 28-08-2024.
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

package com.prime.media.widget

import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.SliderDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowRight
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.prime.media.R
import com.zs.core_ui.Anim
import com.zs.core_ui.ContentPadding
import com.zs.core_ui.MediumDurationMills
import com.prime.media.common.Artwork
import com.prime.media.common.LottieAnimButton
import com.prime.media.common.LottieAnimation
import com.primex.core.thenIf
import com.prime.media.core.playback.artworkUri
import com.prime.media.core.playback.mediaUri
import com.prime.media.core.playback.subtitle
import com.prime.media.core.playback.title
import com.primex.core.SignalWhite
import com.primex.core.foreground
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.ListTile
import com.zs.core_ui.AppTheme
import com.zs.core_ui.sharedBounds
import com.zs.core_ui.sharedElement
import ir.mahozad.multiplatform.wavyslider.material.WavySlider
import kotlin.math.roundToLong

private val WidgetShape = RoundedCornerShape(14)

private val com.zs.core_ui.Colors.widgetBackground
    @Composable
    inline get() = background(2.dp)
private val com.zs.core_ui.Colors.veil
    @Composable
    inline get() = Brush.horizontalGradient(
        0.0f to widgetBackground,
        0.4f to widgetBackground.copy(0.85f),
        0.96f to Color.Transparent,
        //startX = -30f
    )

/**
 * Represents a widget inspired from the media notification of android 11.
 */
@Composable
fun RedVelvetCake(
    item: MediaItem,
    modifier: Modifier = Modifier,
    playing: Boolean = false,
    duration: Long = C.TIME_UNSET,
    progress: Float = 0.0f,
    onSeek: (progress: Float) -> Unit = {},
    onAction: (action: String) -> Unit = {},
) {
    val colors = AppTheme.colors
    val background = colors.widgetBackground
    Box(
        modifier = modifier
            .shadow(Glance.ELEVATION, WidgetShape)
            .thenIf(
                !colors.isLight){border(0.5.dp, colors.accent.copy(0.12f), WidgetShape)}
            .background(background, WidgetShape)
            .heightIn(max = 150.dp)
            .fillMaxWidth(),
        content = {

            // The artwork situated in end of the component
            Artwork(
                data = item.artworkUri,
                modifier = Modifier
                    .thenIf(
                        item.mediaUri != Uri.EMPTY){sharedElement(Glance.SHARED_ARTWORK_ID)}
                    .align(Alignment.TopEnd)
                    .aspectRatio(1.0f, matchHeightConstraintsFirst = true)
                    .foreground(colors.veil)
                    .foreground(Color.Black.copy(0.24f))
                    .clip(WidgetShape),
            )

            // actual content
            val accent = colors.accent
            ListTile(
                color = Color.Transparent,
                onColor = AppTheme.colors.onBackground,
                modifier = Modifier.thenIf(
                    item.mediaUri != Uri.EMPTY){sharedBounds(
                    Glance.SHARED_BACKGROUND_ID,
                    exit = fadeOut() + scaleOut(),
                    enter = fadeIn() + scaleIn(),
                    zIndexInOverlay = 1f
                )},
                headline = {
                    Label(
                        item.subtitle.toString(),
                        color = LocalContentColor.current.copy(ContentAlpha.medium),
                        style = AppTheme.typography.caption,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    )
                },
                overline = {
                    Label(
                        item.title.toString(),
                        style = AppTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth(0.85f),
                        fontWeight = FontWeight.Bold,
                    )
                },
                trailing = {
                    // show playing bars.
                    LottieAnimation(
                        id = R.raw.playback_indicator,
                        iterations = Int.MAX_VALUE,
                        dynamicProperties = rememberLottieDynamicProperties(
                            rememberLottieDynamicProperty(
                                property = LottieProperty.COLOR,
                                accent.toArgb(),
                                "**"
                            )
                        ),
                        modifier = Modifier
                            .thenIf(
                                item.mediaUri != Uri.EMPTY){sharedBounds(Glance.SHARED_PLAYING_BARS_ID)}
                            .requiredSize(24.dp),
                        isPlaying = playing,
                    )
                },
                subtitle = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(ContentPadding.small),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        content = {
                            val color = LocalContentColor.current.copy(ContentAlpha.medium)
                            // SeekBackward
                            IconButton(
                                onClick = { onAction(Glance.ACTION_PREV_TRACK) },
                                imageVector = Icons.Outlined.KeyboardDoubleArrowLeft,
                                contentDescription = null,
                                tint = color
                            )

                            val properties = rememberLottieDynamicProperties(
                                rememberLottieDynamicProperty(
                                    property = LottieProperty.STROKE_COLOR,
                                    accent.toArgb(),
                                    "**"
                                )
                            )
                            // Play Toggle
                            LottieAnimButton(
                                id = R.raw.lt_play_pause,
                                atEnd = !playing,
                                scale = 1.8f,
                                progressRange = 0.0f..0.29f,
                                duration = Anim.MediumDurationMills,
                                easing = LinearEasing,
                                onClick = { onAction(Glance.ACTION_PLAY) },
                                dynamicProperties = properties
                            )

                            // SeekNext
                            IconButton(
                                onClick = { onAction(Glance.ACTION_NEXT_TRACK) },
                                imageVector = Icons.Outlined.KeyboardDoubleArrowRight,
                                contentDescription = null,
                                tint = color
                            )
                        }
                    )
                },
                footer = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        content = {
                            // Expand to fill
                            IconButton(
                                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                //   tint = accent
                                onClick = { onAction(Glance.ACTION_LAUCH_CONSOLE) },
                            )

                            // played duration
                            Label(
                                when (duration) {
                                    C.TIME_UNSET -> stringResource(R.string.abbr_not_available)
                                    else -> DateUtils.formatElapsedTime((duration / 1000 * progress).roundToLong())
                                },
                                style = AppTheme.typography.caption,
                                color = LocalContentColor.current.copy(ContentAlpha.medium)
                            )

                            // slider
                            WavySlider(
                                value = progress.fastCoerceIn(0f, 1f),
                                onValueChange = onSeek,
                                modifier = Modifier.weight(1f),
                                // idp because 0 dp is not supported.
                                waveLength = if (!playing) 0.dp else 20.dp,
                                waveHeight = if (!playing) 0.dp else 7.dp,
                                incremental = true,
                                colors = SliderDefaults.colors(
                                    activeTrackColor = accent,
                                    thumbColor = Color.Transparent
                                ),
                            )

                            // total duration
                            Label(
                                when (duration) {
                                    C.TIME_UNSET -> stringResource(R.string.abbr_not_available)
                                    else -> DateUtils.formatElapsedTime((duration / 1000))
                                },
                                style = AppTheme.typography.caption,
                                color = LocalContentColor.current
                            )
                            // control centre
                            IconButton(
                                imageVector = Icons.Outlined.Tune,
                                onClick = { onAction(Glance.ACTION_LAUNCH_CONTROL_PANEL) },
                                tint = Color.SignalWhite
                            )
                        }
                    )
                },
            )
        }
    )
}
/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 01-03-2025.
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

import android.text.format.DateUtils
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.prime.media.R
import com.prime.media.common.chronometer
import com.prime.media.old.common.Artwork
import com.prime.media.old.common.LocalNavController
import com.prime.media.old.common.LottieAnimButton
import com.prime.media.old.common.marque
import com.prime.media.old.console.Console
import com.prime.media.personalize.RoutePersonalize
import com.primex.core.thenIf
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.ListTile
import com.zs.core.playback.NowPlaying
import com.zs.core_ui.Anim
import com.zs.core_ui.AppTheme
import com.zs.core_ui.MediumDurationMills
import com.zs.core_ui.shape.SkewedRoundedRectangleShape
import com.zs.core_ui.sharedBounds
import com.zs.core_ui.sharedElement
import ir.mahozad.multiplatform.wavyslider.material.WavySlider
import kotlin.math.roundToLong

private val WidgetShape = SkewedRoundedRectangleShape(15.dp, 0.15f)
private val ArtworkShape = SkewedRoundedRectangleShape(15.dp)
private val ArtworkSize = 84.dp
private val TitleDrawStyle =
    Stroke(width = 3.0f /*pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)*/)


@Composable
fun SkewedDynamic(
    state: NowPlaying,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    showcase: Boolean = false
) {
    val colors = AppTheme.colors
    val navController = LocalNavController.current

    // Content
    ListTile(
        onColor = colors.onBackground,
        modifier = modifier
            .thenIf(!showcase) {
                sharedBounds(
                    Glance.SHARED_BACKGROUND_ID,
                    exit = fadeOut() + scaleOut(),
                    enter = fadeIn() + scaleIn(),
                )
            }
            .shadow(16.dp, WidgetShape)
            .border(0.5.dp, colors.accent.copy(if (colors.isLight) 0.24f else 0.12f), WidgetShape)
            .background(AppTheme.colors.background(1.dp)),
        // subtitle
        headline = {
            Label(
                state.subtitle ?: stringResource(R.string.unknown),
                style = AppTheme.typography.caption,
                color = LocalContentColor.current.copy(ContentAlpha.medium)
            )
        },

        // title
        overline = {
            Label(
                state.title ?: stringResource(R.string.unknown),
                style = AppTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.marque(Int.MAX_VALUE)
            )
        },
        // AlbumArt
        leading = {
            Artwork(
                data = state.artwork,
                modifier = Modifier
                    .size(ArtworkSize)
                    .thenIf(!showcase) { sharedElement(Glance.SHARED_ARTWORK_ID) }
                    .clip(ArtworkShape)
                    .border(1.dp, colors.onBackground, ArtworkShape)
            )
        },

        // control centre
        trailing = {
            Column {
                // Expand to fill
                IconButton(
                    imageVector = Icons.Outlined.Tune,
                    onClick = { navController.navigate(RoutePersonalize()); onDismissRequest() },
                    modifier = Modifier.offset(10.dp, -10.dp)
                )

                IconButton(
                    imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                    onClick = { navController.navigate(Console.route); onDismissRequest() },
                    modifier = Modifier.offset(10.dp, -4.dp)
                )
            }
        },

        // play controls
        subtitle = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                content = {
                    val color = LocalContentColor.current
                    val ctx = LocalContext.current
                    // SeekBackward
                    IconButton(
                        onClick = { NowPlaying.trySend(ctx, NowPlaying.ACTION_PREVIOUS) },
                        imageVector = Icons.Outlined.KeyboardDoubleArrowLeft,
                        contentDescription = null,
                        tint = color
                    )

                    val properties = rememberLottieDynamicProperties(
                        rememberLottieDynamicProperty(
                            property = LottieProperty.COLOR,
                            color.toArgb(),
                            "**"
                        ),
                        rememberLottieDynamicProperty(
                            property = LottieProperty.STROKE_COLOR,
                            color.toArgb(),
                            "**"
                        )
                    )
                    // Play Toggle
                    LottieAnimButton(
                        id = R.raw.lt_play_pause5,
                        atEnd = !state.playing,
                        scale = 2.5f,
                        progressRange = 0.0f..0.45f,
                        duration = Anim.MediumDurationMills,
                        easing = LinearEasing,
                        onClick = { NowPlaying.trySend(ctx, NowPlaying.ACTION_TOGGLE_PLAY) },
                        dynamicProperties = properties
                    )

                    // SeekNext
                    IconButton(
                        onClick = { NowPlaying.trySend(ctx, NowPlaying.ACTION_NEXT) },
                        imageVector = Icons.Outlined.KeyboardDoubleArrowRight,
                        contentDescription = null,
                        tint = color
                    )
                }
            )
        },

        // progress
        footer = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .thenIf(!showcase) { sharedElement(Glance.SHARD_TIME_BAR) },
                content = {
                    val chronometer = state.chronometer
                    val position = chronometer.value
                    val contentColor = colors.onBackground
                    // Position
                    Label(
                        when (position) {
                            -1L -> stringResource(R.string.abbr_not_available)
                            else -> DateUtils.formatElapsedTime((chronometer.value / 1000))
                        },
                        style = AppTheme.typography.headlineMedium.copy(
                            drawStyle = TitleDrawStyle,
                            fontWeight = FontWeight.Medium,
                        ),
                        modifier = Modifier.animateContentSize()
                    )

                    // TimeBar
                    val newProgress =
                        if (chronometer.value != -1L) chronometer.value / state.duration.toFloat() else 0f
                    val ctx = LocalContext.current
                    WavySlider(
                        newProgress,
                        onValueChange = {
                            if (position == -1L) return@WavySlider
                            chronometer.value = ((it * state.duration).roundToLong())
                            NowPlaying.trySend(ctx, NowPlaying.ACTION_SEEK_TO) {
                                putExtra(NowPlaying.EXTRA_SEEK_PCT, it)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        // idp because 0 dp is not supported.
                        waveLength = if (!state.playing) 0.dp else 20.dp,
                        waveHeight = if (!state.playing) 0.dp else 7.dp,
                        incremental = true,
                        colors = SliderDefaults.colors(
                            activeTrackColor = contentColor,
                            thumbColor = Color.Transparent
                        )
                    )

                    // Duration
                    Label(
                        when {
                            position == -1L -> stringResource(R.string.abbr_not_available)
                            else -> DateUtils.formatElapsedTime(((state.duration) / 1000))
                        },
                        style = AppTheme.typography.caption,
                        color = contentColor.copy(ContentAlpha.medium),
                        fontWeight = FontWeight.Bold
                    )
                }
            )
        }
    )
}
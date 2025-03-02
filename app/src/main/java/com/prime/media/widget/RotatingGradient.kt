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

@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.prime.media.widget

import android.text.format.DateUtils
import android.util.Log
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.SliderDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
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
import com.primex.core.UmbraGrey
import com.primex.core.textResource
import com.primex.core.thenIf
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.ListTile
import com.zs.core.playback.NowPlaying
import com.zs.core_ui.Anim
import com.zs.core_ui.AppTheme
import com.zs.core_ui.ContentPadding
import com.zs.core_ui.MediumDurationMills
import com.zs.core_ui.background
import com.zs.core_ui.lottieAnimationPainter
import com.zs.core_ui.shape.RoundedPolygonShape
import com.zs.core_ui.shape.RoundedStarShape
import com.zs.core_ui.sharedBounds
import com.zs.core_ui.sharedElement
import ir.mahozad.multiplatform.wavyslider.material.WavySlider
import kotlin.math.roundToLong

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
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    showcase: Boolean = false
) {
    val navController = LocalNavController.current
    val contentColor = Color.UmbraGrey
    val ctx = LocalContext.current
    // Content
    ListTile(
        onColor = contentColor,
        spacing = ContentPadding.small,
        modifier = modifier
            .thenIf(!showcase) {
                sharedBounds(
                    Glance.SHARED_BACKGROUND_ID,
                    exit = scaleOut(),
                    enter = scaleIn()
                )
            }
            .shadow(Glance.ELEVATION, Shape)
            .border(1.dp, Color.Gray.copy(0.24f), Shape)
            .background(
                lottieAnimationPainter(
                    R.raw.bg_rotating_color_gradient,

                    ),
                contentScale = ContentScale.Crop
            ),
        overline = {
            Label(
                state.subtitle ?: textResource(R.string.unknown),
                style = AppTheme.typography.caption,
                color = LocalContentColor.current.copy(ContentAlpha.medium),
                modifier = Modifier.thenIf(!showcase) { sharedElement(Glance.SHARED_SUBTITLE) },
            )
        },
        headline = {
            Label(
                state.title ?: textResource(R.string.unknown),
                modifier = Modifier
                    .thenIf(!showcase) { sharedElement(Glance.SHARED_TITLE) }
                    .marque(Int.MAX_VALUE),
                style = AppTheme.typography.headlineSmall.copy(
                    drawStyle = TitleDrawStyle,
                    fontWeight = FontWeight.Medium,
                )
            )
        },
        leading = {
            Artwork(
                data = state.artwork,
                modifier = Modifier
                    .size(ArtworkSize)
                    .thenIf(!showcase) { sharedElement(Glance.SHARED_ARTWORK_ID) }
                    .clip(ArtworkShape)
                .border(0.5.dp, contentColor, ArtworkShape)
            )
        },
        trailing = {
            // Expand to fill
            IconButton(
                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                //   tint = accent
                onClick = { navController.navigate(Console.route); onDismissRequest() },
                modifier = Modifier
                    .scale(0.9f)
                    .offset(x = 14.dp),
            )
        },
        subtitle = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(ContentPadding.small),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .thenIf(!showcase) { sharedElement(Glance.SHARED_CONTROLS) },
                content = {
                    val color = contentColor.copy(ContentAlpha.medium)
                    // Skip to Prev
                    IconButton(
                        onClick = { NowPlaying.trySend(ctx, NowPlaying.ACTION_PREVIOUS) },
                        imageVector = Icons.Outlined.KeyboardDoubleArrowLeft,
                        contentDescription = null,
                        tint = color
                    )

                    // Play Toggle
                    LottieAnimButton(
                        id = R.raw.lt_play_pause_circle_bordered,
                        atEnd = !state.playing,
                        scale = 7f,
                        progressRange = 0.0f..0.7f,
                        duration = Anim.MediumDurationMills,
                        easing = LinearEasing,
                        onClick = { NowPlaying.trySend(ctx, NowPlaying.ACTION_TOGGLE_PLAY) },
                        dynamicProperties = rememberLottieDynamicProperties(
                            rememberLottieDynamicProperty(
                                property = LottieProperty.STROKE_COLOR,
                                contentColor.toArgb(),
                                "**"
                            )
                        )
                    )

                    // Skip to Next
                    IconButton(
                        onClick = { NowPlaying.trySend(ctx, NowPlaying.ACTION_NEXT) },
                        imageVector = Icons.Outlined.KeyboardDoubleArrowRight,
                        contentDescription = null,
                        tint = color
                    )
                }
            )
        },
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
                    // Position
                    Label(
                        when (position) {
                            -1L-> stringResource(R.string.abbr_not_available)
                            else -> DateUtils.formatElapsedTime((chronometer.value / 1000 ))
                        },
                        style = AppTheme.typography.caption,
                        color = contentColor.copy(ContentAlpha.medium),
                        fontWeight = FontWeight.Bold
                    )

                    // TimeBar
                    val newProgress = if (chronometer.value != -1L) chronometer.value / state.duration.toFloat() else 0f
                    WavySlider(
                        newProgress,
                        onValueChange = {
                            if (position == -1L) return@WavySlider
                            Log.d(TAG, "Iphone: $it")
                            chronometer.value = ((it * state.duration).roundToLong())
                            NowPlaying.trySend(ctx, NowPlaying.ACTION_SEEK_TO){
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
                        when  {
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
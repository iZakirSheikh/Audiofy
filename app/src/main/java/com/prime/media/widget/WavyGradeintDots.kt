/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 25-02-2025.
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

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.FloatingActionButtonDefaults
import androidx.compose.material.LocalContentColor
import androidx.compose.material.SliderDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.prime.media.old.common.LottieAnimation
import com.prime.media.old.common.marque
import com.prime.media.old.console.Console
import com.primex.core.SignalWhite
import com.primex.core.textResource
import com.primex.core.thenIf
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.ListTile
import com.zs.core.playback.NowPlaying
import com.zs.core_ui.Anim
import com.zs.core_ui.AppTheme
import com.zs.core_ui.MediumDurationMills
import com.zs.core_ui.background
import com.zs.core_ui.lottieAnimationPainter
import com.zs.core_ui.shape.RoundedPolygonShape
import com.zs.core_ui.shape.RoundedStarShape
import com.zs.core_ui.sharedBounds
import com.zs.core_ui.sharedElement
import ir.mahozad.multiplatform.wavyslider.material.WavySlider
import kotlin.math.roundToLong

private const val TAG = "WavyGradeintDots"

private val Shape = RoundedCornerShape(12)
private val ArtworkSize = 84.dp
private val ArtworkShape = RoundedPolygonShape(5, 0.3f)
private val TitleDrawStyle = Stroke(width = 3.2f, join = StrokeJoin.Round)

private val IconModifier = Modifier
    .scale(0.84f)
    .background(Color.SignalWhite.copy(0.3f), CircleShape)

@Composable
fun WavyGradientDots(
    state: NowPlaying,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    showcase: Boolean = false
) {
    val navController = LocalNavController.current
    val contentColor = Color.SignalWhite
    val ctx = LocalContext.current

    //
    ListTile(
        onColor = contentColor,
        spacing = 0.dp,
        centerAlign = true,
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
                lottieAnimationPainter(R.raw.bg_gradeint_dots),
                contentScale = ContentScale.Crop,
                overlay = Color.Black.copy(0.3f)
            ),
        // subtitle
        headline = {
            Label(
                state.subtitle ?: stringResource(R.string.unknown),
                color = LocalContentColor.current.copy(ContentAlpha.medium),
                style = AppTheme.typography.caption,
                modifier = Modifier.fillMaxWidth(0.85f)
            )
        },
        // title
        subtitle = {
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
                //.border(2.dp, Color.Gray.copy(0.24f), ArtworkShape)
            )
        },
        trailing = {
            val radius by animateIntAsState(if (state.playing) 28 else 100)
            FloatingActionButton(
                onClick = { NowPlaying.trySend(ctx, NowPlaying.ACTION_TOGGLE_PLAY) },
                shape = RoundedCornerShape(radius),
                backgroundColor = contentColor.copy(ContentAlpha.disabled),
                contentColor = contentColor,
                modifier = Modifier.scale(0.9f),
                elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
            ) {
                val properties = rememberLottieDynamicProperties(
                    rememberLottieDynamicProperty(
                        property = LottieProperty.STROKE_COLOR,
                        LocalContentColor.current.toArgb(),
                        "**"
                    )
                )

                LottieAnimation(
                    id = R.raw.lt_play_pause,
                    atEnd = !state.playing,
                    scale = 1.5f,
                    progressRange = 0.0f..0.29f,
                    duration = Anim.MediumDurationMills,
                    easing = LinearEasing,
                    dynamicProperties = properties
                )
            }
        },
        // timebar
        footer = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                content = {
                    // Expand to fill
                    val color = LocalContentColor.current.copy(ContentAlpha.medium)
                    val chronometer = state.chronometer
                    val position = chronometer.value
                    val ctx = LocalContext.current
                    // SeekBackward
                    IconButton(
                        onClick = { NowPlaying.trySend(ctx, NowPlaying.ACTION_PREVIOUS) },
                        imageVector = Icons.Outlined.KeyboardDoubleArrowLeft,
                        contentDescription = null,
                        tint = color,
                        modifier = IconModifier
                    )

                    // slider
                    val progress =
                        if (chronometer.value != -1L) chronometer.value / state.duration.toFloat() else 0f
                    WavySlider(
                        value = progress,
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
                            activeTrackColor = LocalContentColor.current,
                            thumbColor = Color.Transparent
                        ),
                    )


                    // SeekNext
                    IconButton(
                        onClick = { NowPlaying.trySend(ctx, NowPlaying.ACTION_NEXT) },
                        imageVector = Icons.Outlined.KeyboardDoubleArrowRight,
                        contentDescription = null,
                        tint = color,
                        modifier = IconModifier
                    )

                    // Expand to fill
                    IconButton(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        onClick = { navController.navigate(Console.route); onDismissRequest() },
                        modifier = IconModifier
                    )
                }
            )
        }
    )
}
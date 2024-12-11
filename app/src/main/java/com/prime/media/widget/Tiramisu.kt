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

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
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
import com.prime.media.old.common.LottieAnimation
import com.prime.media.old.console.Console
import com.prime.media.personalize.RoutePersonalize
import com.primex.core.SignalWhite
import com.primex.core.UmbraGrey
import com.primex.core.blend
import com.primex.core.foreground
import com.primex.core.thenIf
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.ListTile
import com.zs.core.playback.NowPlaying
import com.zs.core_ui.Anim
import com.zs.core_ui.AppTheme
import com.zs.core_ui.Colors
import com.zs.core_ui.MediumDurationMills
import com.zs.core_ui.sharedBounds
import com.zs.core_ui.sharedElement
import ir.mahozad.multiplatform.wavyslider.material.WavySlider
import kotlin.math.roundToLong

private val TiramisuShape = RoundedCornerShape(14)
private inline val Colors.ring
    @Composable
    get() =
        Brush.horizontalGradient(listOf(accent.copy(0.5f), Color.Transparent, accent.copy(0.5f)))
private inline val Colors.contentColor
    @Composable get() =
        if (accent.luminance() > 0.6f) Color.UmbraGrey else Color.SignalWhite

/**
 * Represents a widget inspired from the media notification of android 13.
 */
@Composable
fun Tiramisu(
    state: NowPlaying,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    showcase: Boolean = false
) {
    val colors = AppTheme.colors
    Box(
        modifier = modifier
            .shadow(Glance.ELEVATION, TiramisuShape)
            .background(AppTheme.colors.background)
            .heightIn(max = 160.dp)
            .fillMaxWidth(),
        content = {
            // The artwork as the background of the widget
            Artwork(
                data = state.artwork,
                modifier = Modifier
                    .thenIf(!showcase) {
                        sharedElement(
                            Glance.SHARED_ARTWORK_ID,
                            zIndexInOverlay = 0f
                        )
                    }
                    .clip(TiramisuShape)
                    .foreground(colors.ring)
                    .foreground(Color.Black.copy(0.26f))
                    .matchParentSize(),
            )
            val onColor = Color.SignalWhite
            val navController = LocalNavController.current
            ListTile(
                color = Color.Transparent,
                onColor = onColor,
                modifier = Modifier.thenIf(!showcase) {
                    sharedBounds(
                        Glance.SHARED_BACKGROUND_ID,
                        exit = fadeOut() + scaleOut(),
                        enter = fadeIn() + scaleIn(),
                        zIndexInOverlay = 1f
                    )
                },
                centerAlign = true,
                // title
                subtitle = {
                    Label(
                        state.title ?: stringResource(R.string.unknown),
                        color = LocalContentColor.current.copy(ContentAlpha.medium),
                        style = AppTheme.typography.caption,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    )
                },
                // subtitle
                headline = {
                    Label(
                        state.title ?: stringResource(R.string.unknown),
                        style = AppTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth(0.85f),
                        fontWeight = FontWeight.Bold,
                    )
                },
                // control centre
                overline = {
                    IconButton(
                        imageVector = Icons.Outlined.Tune,
                        onClick = { navController.navigate(RoutePersonalize()); onDismissRequest() },
                        modifier = Modifier.offset(-12.dp, -16.dp)
                    )
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
                                tint = color
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
                                tint = color
                            )

                            // Expand to fill
                            IconButton(
                                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                //   tint = accent
                                onClick = { navController.navigate(Console.route); onDismissRequest() },
                            )
                        }
                    )
                },
                // play button
                trailing = {
                    val ctx = LocalContext.current
                    FloatingActionButton(
                        onClick = { NowPlaying.trySend(ctx, NowPlaying.ACTION_TOGGLE_PLAY) },
                        shape = RoundedCornerShape(28),
                        backgroundColor = Color.SignalWhite.blend(colors.accent, 0.2f),
                        contentColor = Color.UmbraGrey,
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
                }
            )
        }
    )
}


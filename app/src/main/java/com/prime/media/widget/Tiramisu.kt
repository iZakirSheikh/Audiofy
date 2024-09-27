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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.prime.media.R
import com.prime.media.common.Artwork
import com.prime.media.common.LottieAnimation
import com.prime.media.core.playback.artworkUri
import com.prime.media.core.playback.mediaUri
import com.prime.media.core.playback.title
import com.primex.core.SignalWhite
import com.primex.core.UmbraGrey
import com.primex.core.blend
import com.primex.core.foreground
import com.primex.core.thenIf
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.ListTile
import com.zs.core_ui.Anim
import com.zs.core_ui.AppTheme
import com.zs.core_ui.MediumDurationMills
import com.zs.core_ui.sharedBounds
import com.zs.core_ui.sharedElement
import ir.mahozad.multiplatform.wavyslider.material.WavySlider

private val TiramisuShape = RoundedCornerShape(14)
private inline val com.zs.core_ui.Colors.ring
    @Composable
    get() =
        Brush.horizontalGradient(listOf(accent.copy(0.5f), Color.Transparent,  accent.copy(0.5f)))
private inline val com.zs.core_ui.Colors.contentColor @Composable get() =
    if (accent.luminance() > 0.6f) Color.UmbraGrey else Color.SignalWhite

/**
 * Represents a widget inspired from the media notification of android 13.
 */
@Composable
fun Tiramisu(
    item: MediaItem,
    modifier: Modifier = Modifier,
    playing: Boolean = false,
    duration: Long = C.TIME_UNSET,
    progress: Float = 0.0f,
    onSeek: (progress: Float) -> Unit = {},
    onAction: (action: String) -> Unit = {},
) {
    val colors =  AppTheme.colors
    Box(
        modifier = modifier
            .shadow(Glance.ELEVATION, TiramisuShape)
            .background(AppTheme.colors.background)
            .heightIn(max = 160.dp)
            .fillMaxWidth(),
        content = {
            // The artwork as the background of the widget
            Artwork(
                data = item.artworkUri,
                modifier = Modifier.thenIf(item.mediaUri != Uri.EMPTY){sharedElement(Glance.SHARED_ARTWORK_ID, zIndexInOverlay = 0f)}
                    .clip(TiramisuShape)
                    .foreground(colors.ring)
                    .foreground(Color.Black.copy(0.26f))
                    .matchParentSize(),
            )
            val onColor = Color.SignalWhite
            ListTile(
                color = Color.Transparent,
                onColor = onColor,
                modifier = Modifier.thenIf(item.mediaUri != Uri.EMPTY){sharedBounds(
                    Glance.SHARED_BACKGROUND_ID,
                    exit = fadeOut() + scaleOut(),
                    enter = fadeIn() + scaleIn(),
                    zIndexInOverlay = 1f
                )},
                centerAlign = true,
                // title
                subtitle = {
                    Label(
                        item.title.toString(),
                        color = LocalContentColor.current.copy(ContentAlpha.medium),
                        style = AppTheme.typography.caption,
                        modifier = Modifier.fillMaxWidth(0.85f)
                    )
                },
                // subtitle
                headline = {
                    Label(
                        item.title.toString(),
                        style = AppTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth(0.85f),
                        fontWeight = FontWeight.Bold,
                    )
                },
                // control centre
                overline = {
                    IconButton(
                        imageVector = Icons.Outlined.Tune,
                        onClick = { onAction(Glance.ACTION_LAUNCH_CONTROL_PANEL) },
                        modifier = Modifier.offset(-12.dp, -16.dp)
                    )
                },

                footer = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                        content = {
                            // Expand to fill
                            val color = LocalContentColor.current.copy(ContentAlpha.medium)
                            // SeekBackward
                            IconButton(
                                onClick = { onAction(Glance.ACTION_PREV_TRACK) },
                                imageVector = Icons.Outlined.KeyboardDoubleArrowLeft,
                                contentDescription = null,
                                tint = color
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
                                    activeTrackColor = LocalContentColor.current,
                                    thumbColor = Color.Transparent
                                ),
                            )


                            // SeekNext
                            IconButton(
                                onClick = { onAction(Glance.ACTION_NEXT_TRACK) },
                                imageVector = Icons.Outlined.KeyboardDoubleArrowRight,
                                contentDescription = null,
                                tint = color
                            )

                            // Expand to fill
                            IconButton(
                                imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                                //   tint = accent
                                onClick = { onAction(Glance.ACTION_LAUCH_CONSOLE) },
                            )
                        }
                    )
                },
                trailing = {
                    FloatingActionButton(
                        onClick = { onAction(Glance.ACTION_PLAY) },
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
                            atEnd = !playing,
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


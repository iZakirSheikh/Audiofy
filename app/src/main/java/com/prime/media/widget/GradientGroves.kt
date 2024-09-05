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

package com.prime.media.widget

import android.graphics.DrawFilter
import android.net.Uri
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Colors
import androidx.compose.material.ContentAlpha
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowRight
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.prime.media.Material
import com.prime.media.R
import com.prime.media.backgroundColorAtElevation
import com.prime.media.core.Anim
import com.prime.media.core.ContentElevation
import com.prime.media.core.ContentPadding
import com.prime.media.core.MediumDurationMills
import com.prime.media.core.compose.Artwork
import com.prime.media.core.compose.LottieAnimButton
import com.prime.media.core.compose.LottieAnimation
import com.prime.media.core.compose.marque
import com.prime.media.core.compose.sharedBounds
import com.prime.media.core.compose.sharedElement
import com.prime.media.core.compose.thenIf
import com.prime.media.core.playback.artworkUri
import com.prime.media.core.playback.mediaUri
import com.prime.media.core.playback.subtitle
import com.prime.media.core.playback.title
import com.primex.core.ImageBrush
import com.primex.core.MetroGreen2
import com.primex.core.Rose
import com.primex.core.SignalWhite
import com.primex.core.SkyBlue
import com.primex.core.UmbraGrey
import com.primex.core.blend
import com.primex.core.shadow.shadow
import com.primex.core.visualEffect
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.ListTile

private val WidgetShape = RoundedCornerShape(16.dp)
private val Colors.bg: Brush
    get() {
        val accent = primary.copy(0.85f)
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
    20, 4, 20, 4
)

/**
 * A Modifier that creates a "double vision" effect by drawing the content twice with a slight offset.
 *
 * @return A Modifier that applies the double vision effect.
 */
private fun Modifier.offsetDoubleVision() =
    drawWithCache {
        onDrawWithContent {
            drawIntoCanvas { canvas ->
                canvas.save()
                canvas.translate(5.dp.toPx(), -5.dp.toPx())
                //drawRect(Color.Black.copy(0.5f))
                drawContent()
                canvas.restore()
            }
            drawContent()
        }
    }

@Composable
fun GradientGroves(
    item: MediaItem,
    modifier: Modifier = Modifier,
    playing: Boolean = false,
    duration: Long = C.TIME_UNSET,
    progress: Float = 0.0f,
    onSeek: (progress: Float) -> Unit = {},
    onAction: (action: String) -> Unit = {},
) {
    val colors = Material.colors
    ListTile(
        modifier = modifier
            .thenIf(item.mediaUri != Uri.EMPTY, Glance.SharedBoundsModifier)
            .visualEffect(ImageBrush.NoiseBrush, 0.4f, overlay = true)
            .background(Color.White, WidgetShape)
            .background(colors.bg, WidgetShape),
        color = Color.Transparent,
        headline = {
            Label(
                item.title.toString(),
                modifier = Modifier.marque(Int.MAX_VALUE),
                style = Material.typography.h5,
                fontWeight = FontWeight.Bold
            )
        },
        overline = {
            Label(
                item.subtitle.toString(),
                style = Material.typography.caption,
                color = LocalContentColor.current
            )
        },
        trailing = {
            Artwork(
                data = item.artworkUri,
                modifier = Modifier
                    .size(DefaultArtworkSize)
                    .thenIf(item.mediaUri != Uri.EMPTY) { sharedElement(Glance.SHARED_ARTWORK_ID) }
                    .offsetDoubleVision()
                    .shadow(ContentElevation.medium, DefaultArtworkShape),
            )
        },
        subtitle = {
            Row(
                horizontalArrangement = Arrangement.spacedBy(ContentPadding.medium),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(top = ContentPadding.medium)
                    .fillMaxWidth(),
                content = {
                    val bgModifier = Modifier
                        .scale(0.88f)
                        .background(Material.colors.primary.copy(0.3f), CircleShape)
                    // SeekBackward
                    IconButton(
                        onClick = { onAction(Glance.ACTION_PREV_TRACK) },
                        imageVector = Icons.Outlined.KeyboardDoubleArrowLeft,
                        contentDescription = null,
                        modifier = bgModifier
                    )

                    FloatingActionButton(
                        backgroundColor = colors.primary.blend(Color.SignalWhite, 0.2f),
                        contentColor = LocalContentColor.current,
                        shape = RoundedCornerShape(28),
                        onClick = { onAction(Glance.ACTION_PLAY) },) {
                        val properties = rememberLottieDynamicProperties(
                            rememberLottieDynamicProperty(
                                property = LottieProperty.STROKE_COLOR,
                                LocalContentColor.current.toArgb(),
                                "**"
                            )
                        )
                        // Play Toggle
                        LottieAnimation(
                            id = R.raw.lt_play_pause,
                            atEnd = !playing,
                            scale = 1.65f,
                            progressRange = 0.0f..0.29f,
                            duration = Anim.MediumDurationMills,
                            easing = LinearEasing,
                            dynamicProperties = properties,
                        )
                    }

                    // SeekNext
                    IconButton(
                        onClick = { onAction(Glance.ACTION_NEXT_TRACK) },
                        imageVector = Icons.Outlined.KeyboardDoubleArrowRight,
                        contentDescription = null,
                        modifier = bgModifier
                    )

                    //
                    IconButton(
                        imageVector = Icons.Outlined.OpenInNew,
                        onClick = { onAction(Glance.ACTION_LAUCH_CONSOLE) },
                        modifier = bgModifier,
                    )
                }
            )
        }
    )
}
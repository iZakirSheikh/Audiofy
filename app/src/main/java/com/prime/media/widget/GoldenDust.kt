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

import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.EaseInOutBounce
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.FloatingActionButtonDefaults
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowRight
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
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
import com.prime.media.Material
import com.prime.media.R
import com.prime.media.core.Anim
import com.prime.media.core.ContentElevation
import com.prime.media.core.ContentPadding
import com.prime.media.core.MediumDurationMills
import com.prime.media.core.compose.Artwork
import com.prime.media.core.compose.LottieAnimation
import com.prime.media.core.compose.marque
import com.prime.media.core.compose.shape.RoundedPolygonShape
import com.prime.media.core.compose.sharedBounds
import com.prime.media.core.compose.sharedElement
import com.prime.media.core.compose.shimmer.shimmer
import com.prime.media.core.compose.thenIf
import com.prime.media.core.playback.artworkUri
import com.prime.media.core.playback.mediaUri
import com.prime.media.core.playback.subtitle
import com.prime.media.core.playback.title
import com.primex.core.ImageBrush
import com.primex.core.visualEffect
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.ListTile
import ir.mahozad.multiplatform.wavyslider.material.WavySlider
import kotlin.math.roundToLong

private val WidgetShape = RoundedCornerShape(16.dp)
private val ArtworkShape = RoundedPolygonShape(6, 0.3f)
private val DefaultArtworkSize = 78.dp
private val ShimmerAnimSpec =
    infiniteRepeatable<Float>(tween(5000, 2500, easing = EaseInOutBounce))

private val Accent = Color(0xCBFF9405)
private val onAccent = Color(0xFFFED68C)

private val bgColor = Color(0xFF1F1B11)
private val background = Brush.linearGradient(
    0.0f to Accent.copy(0.5f),
    0.55f to bgColor,
    start = Offset.Infinite,
    end = Offset.Zero
)
private val IconModifier = Modifier
    .scale(0.84f)
    .background(onAccent.copy(0.3f), CircleShape)

private val PlayButtonShape = RoundedCornerShape(28)

@Composable
fun GoldenDust(
    item: MediaItem,
    modifier: Modifier = Modifier,
    playing: Boolean = false,
    duration: Long = C.TIME_UNSET,
    progress: Float = 0.0f,
    onSeek: (progress: Float) -> Unit = {},
    onAction: (action: String) -> Unit = {},
) {
    val isPreview = item.mediaUri == Uri.EMPTY
    ListTile(
        modifier = modifier
            .thenIf(!isPreview, Glance.SharedBoundsModifier)
            .visualEffect(ImageBrush.NoiseBrush, 0.4f, overlay = true)
            .shimmer(Accent, 400.dp, BlendMode.Overlay, ShimmerAnimSpec)
            .border(1.dp, onAccent, WidgetShape)
            .background(bgColor, WidgetShape)
            .background(background, WidgetShape),
        color = Color.Transparent,
        onColor = onAccent,
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
                    .thenIf(!isPreview) { sharedElement(Glance.SHARED_ARTWORK_ID) }
                    .scale(1.15f)
                    .shadow(ContentElevation.medium, ArtworkShape),
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
                    // SeekBackward
                    IconButton(
                        onClick = { onAction(Glance.ACTION_PREV_TRACK) },
                        imageVector = Icons.Outlined.KeyboardDoubleArrowLeft,
                        contentDescription = null,
                        modifier = IconModifier
                    )

                    FloatingActionButton(
                        backgroundColor = onAccent.copy(0.3f),
                        contentColor = LocalContentColor.current,
                        shape = PlayButtonShape,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                        onClick = { onAction(Glance.ACTION_PLAY) },
                    ) {
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
                        modifier = IconModifier
                    )

                    //
                    Spacer(Modifier.weight(1f))

                    // control centre
                    IconButton(
                        imageVector = Icons.Outlined.Tune,
                        onClick = { onAction(Glance.ACTION_LAUNCH_CONTROL_PANEL) },
                        modifier = IconModifier
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
                    //
                    // show playing bars.
                    LottieAnimation(
                        id = R.raw.playback_indicator,
                        iterations = Int.MAX_VALUE,
                        dynamicProperties = rememberLottieDynamicProperties(
                            rememberLottieDynamicProperty(
                                property = LottieProperty.COLOR,
                                LocalContentColor.current.toArgb(),
                                "**"
                            )
                        ),
                        modifier = Modifier
                            .thenIf(
                                item.mediaUri != Uri.EMPTY, Modifier
                                    .sharedBounds(Glance.SHARED_PLAYING_BARS_ID)
                            )
                            .requiredSize(24.dp),
                        isPlaying = playing,
                    )

                    // played duration
                    Label(
                        when (duration) {
                            C.TIME_UNSET -> stringResource(R.string.not_available_abbv)
                            else -> DateUtils.formatElapsedTime((duration / 1000 * progress).roundToLong())
                        },
                        style = Material.typography.caption,
                    )

                    // slider
                    Slider(
                        value = progress.fastCoerceIn(0f, 1f),
                        onValueChange = onSeek,
                        modifier = Modifier.weight(1f),
                        colors = SliderDefaults.colors(
                            activeTrackColor = Accent,
                            thumbColor = Color.Transparent
                        ),
                    )

                    // total duration
                    Label(
                        when (duration) {
                            C.TIME_UNSET -> stringResource(R.string.not_available_abbv)
                            else -> DateUtils.formatElapsedTime((duration / 1000))
                        },
                        style = Material.typography.caption,
                        color = LocalContentColor.current,
                    )

                    // Expand to fill
                    IconButton(
                        imageVector = Icons.Outlined.OpenInNew,
                        //   tint = accent
                        onClick = { onAction(Glance.ACTION_LAUCH_CONSOLE) },
                        modifier = IconModifier
                    )
                }
            )
        }
    )
}
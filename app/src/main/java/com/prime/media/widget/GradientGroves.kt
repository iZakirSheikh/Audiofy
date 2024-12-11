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
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.prime.media.R
import com.zs.core_ui.Anim
import com.zs.core_ui.ContentElevation
import com.zs.core_ui.ContentPadding
import com.zs.core_ui.MediumDurationMills
import com.prime.media.old.common.Artwork
import com.prime.media.old.common.LocalNavController
import com.prime.media.old.common.LottieAnimation
import com.prime.media.old.common.marque
import com.prime.media.old.console.Console
import com.primex.core.thenIf
import com.prime.media.old.core.playback.artworkUri
import com.prime.media.old.core.playback.mediaUri
import com.prime.media.old.core.playback.subtitle
import com.prime.media.old.core.playback.title
import com.primex.core.ImageBrush
import com.primex.core.SignalWhite
import com.primex.core.UmbraGrey
import com.primex.core.blend
import com.primex.core.stringResource
import com.primex.core.visualEffect
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.ListTile
import com.zs.core.playback.NowPlaying
import com.zs.core_ui.AppTheme
import com.zs.core_ui.Colors
import com.zs.core_ui.sharedElement

private val WidgetShape = RoundedCornerShape(16.dp)
private val Colors.bg: Brush
    @Composable
    get() {
        val accent = accent.copy(0.85f)
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
    state: NowPlaying,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    showcase: Boolean = false
) {
    val colors = AppTheme.colors
    ListTile(
        modifier = modifier
            .thenIf(!showcase) { Glance.SharedBoundsModifier }
            .visualEffect(ImageBrush.NoiseBrush, 0.4f, overlay = true)
            .background(Color.White, WidgetShape)
            .background(colors.bg, WidgetShape),
        color = Color.Transparent,
        onColor = Color.UmbraGrey,
        headline = {
            Label(
                state.title ?: androidx.compose.ui.res.stringResource(R.string.unknown),
                modifier = Modifier.marque(Int.MAX_VALUE),
                style = AppTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        },
        overline = {
            Label(
                state.subtitle ?: androidx.compose.ui.res.stringResource(R.string.unknown),
                style = AppTheme.typography.caption,
                color = LocalContentColor.current
            )
        },
        trailing = {
            Artwork(
                data = state.artwork,
                modifier = Modifier
                    .size(DefaultArtworkSize)
                    .thenIf(!showcase) { sharedElement(Glance.SHARED_ARTWORK_ID) }
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
                        .background(AppTheme.colors.accent.copy(0.3f), CircleShape)
                    // SeekBackward
                    val ctx = LocalContext.current
                    IconButton(
                        onClick = { NowPlaying.trySend(ctx, NowPlaying.ACTION_PREVIOUS) },
                        imageVector = Icons.Outlined.KeyboardDoubleArrowLeft,
                        contentDescription = null,
                        modifier = bgModifier
                    )

                    FloatingActionButton(
                        backgroundColor = colors.accent.blend(Color.SignalWhite, 0.2f),
                        contentColor = LocalContentColor.current,
                        shape = RoundedCornerShape(28),
                        onClick = { NowPlaying.trySend(ctx, NowPlaying.ACTION_TOGGLE_PLAY) },) {
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
                            atEnd = !state.playing,
                            scale = 1.65f,
                            progressRange = 0.0f..0.29f,
                            duration = Anim.MediumDurationMills,
                            easing = LinearEasing,
                            dynamicProperties = properties,
                        )
                    }

                    // SeekNext
                    IconButton(
                        onClick = { NowPlaying.trySend(ctx, NowPlaying.ACTION_NEXT) },
                        imageVector = Icons.Outlined.KeyboardDoubleArrowRight,
                        contentDescription = null,
                        modifier = bgModifier
                    )

                    //
                    val navController = LocalNavController.current
                    IconButton(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        onClick = { navController.navigate(Console.route); onDismissRequest() },
                        modifier = bgModifier,
                    )

                }
            )
        }
    )
}
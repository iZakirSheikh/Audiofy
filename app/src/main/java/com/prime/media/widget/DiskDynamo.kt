/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 26-02-2025.
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
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.FloatingActionButtonDefaults
import androidx.compose.material.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowRight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.prime.media.R
import com.prime.media.old.common.Artwork
import com.prime.media.old.common.LocalNavController
import com.prime.media.old.common.LottieAnimation
import com.prime.media.old.common.marque
import com.prime.media.old.console.Console
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
import com.zs.core_ui.shape.CompactDisk
import com.zs.core_ui.sharedBounds
import com.zs.core_ui.sharedElement

private val DefaultArtworkSize = 84.dp
private val DefaultArtworkShape = CompactDisk
private val Shape = RoundedCornerShape(50, 8, 25, 50)

private val PlayButtonShape = RoundedCornerShape(28)
private val WidgetContentPadding = PaddingValues(8.dp, 6.dp)

@Composable
fun DiskDynamo(
    state: NowPlaying,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    showcase: Boolean = false
) {
    val colors = AppTheme.colors
    val contentColor = colors.onBackground
    ListTile(
        onColor = contentColor,
        centerAlign = true,
        padding = WidgetContentPadding,
        modifier = modifier
            .thenIf(!showcase) {
                sharedBounds(
                    Glance.SHARED_BACKGROUND_ID,
                    exit = fadeOut() + scaleOut(),
                    enter = fadeIn() + scaleIn()
                )
            }
            .shadow(Glance.ELEVATION, Shape)
            .border(1.dp, Color.DarkGray.copy(0.12f), Shape)
            .background(colors.background(2.dp)),
        leading = {
            val infiniteTransition = rememberInfiniteTransition()
            val degrees by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(durationMillis = 2000, easing = LinearEasing),
                )
            )
            Artwork(
                data = state.artwork,
                modifier = Modifier
                    .size(DefaultArtworkSize)
                    .thenIf(!showcase) { sharedElement(Glance.SHARED_ARTWORK_ID) }
                    .graphicsLayer {
                        rotationZ = if (!state.playing) 0f else degrees
                        scaleX = 1.1f
                        scaleY = 1.1f
                        this.shape = DefaultArtworkShape
                        shadowElevation = 12.dp.toPx()
                        clip = true
                    }
                    .border(0.5.dp, contentColor, DefaultArtworkShape),
            )
        },
        overline = {
            Label(
                state.title ?: textResource(R.string.unknown),
                modifier = Modifier
                    .thenIf(!showcase) { sharedElement(Glance.SHARED_TITLE) }
                    .marque(Int.MAX_VALUE),
                style = AppTheme.typography.titleLarge
            )
        },
        headline = {
            Label(
                state.subtitle ?: textResource(R.string.unknown),
                style = AppTheme.typography.caption,
                color = LocalContentColor.current.copy(ContentAlpha.medium),
                modifier = Modifier.thenIf(!showcase) { sharedElement(Glance.SHARED_SUBTITLE) },
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
                    val IconModifier = Modifier
                        .scale(0.84f)
                        .background(contentColor.copy(0.3f), CircleShape)

                    val ctx = LocalContext.current
                    val navController = LocalNavController.current
                    // SeekBackward
                    IconButton(
                        onClick = { NowPlaying.trySend(ctx, NowPlaying.ACTION_PREVIOUS) },
                        imageVector = Icons.Outlined.KeyboardDoubleArrowLeft,
                        contentDescription = null,
                        modifier = IconModifier
                    )

                    FloatingActionButton(
                        backgroundColor = contentColor.copy(0.3f),
                        contentColor = LocalContentColor.current,
                        shape = PlayButtonShape,
                        elevation = FloatingActionButtonDefaults.elevation(0.dp, 0.dp, 0.dp, 0.dp),
                        onClick = { NowPlaying.trySend(ctx, NowPlaying.ACTION_TOGGLE_PLAY) },
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
                        modifier = IconModifier
                    )

                    //
                    Spacer(Modifier.weight(1f))

                    // control centre
                    IconButton(
                        imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
                        //   tint = accent
                        onClick = { navController.navigate(Console.route); onDismissRequest() },
                        modifier = IconModifier,
                    )
                }
            )
        }
    )
}
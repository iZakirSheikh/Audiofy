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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.times
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
import com.zs.core_ui.sharedBounds
import com.zs.core_ui.sharedElement

private val DefaultArtworkSize = DpSize(84.dp, 1.5 * 84.dp)
private val DefaultArtworkShape = CircleShape
private val Shape = RoundedCornerShape(14)

@Composable
fun ElongatedBeat(
    state: NowPlaying,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    showcase: Boolean = false
) {
    val accent = AppTheme.colors.accent
    val contentColor = AppTheme.colors.onAccent
    val navController = LocalNavController.current

    // Real Content
    ListTile(
        onColor = AppTheme.colors.onAccent,
        modifier = modifier
            .thenIf(
                !showcase
            ) {
                sharedBounds(
                    Glance.SHARED_BACKGROUND_ID,
                    exit = fadeOut() + scaleOut(),
                    enter = fadeIn() + scaleIn()
                )
            }
            .shadow(Glance.ELEVATION, Shape)
            .border(1.dp, Color.Gray.copy(0.24f), Shape)
            .background(accent),
        overline = {
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
                    .thenIf(!showcase) { sharedBounds(Glance.SHARED_PLAYING_BARS_ID) }
                    .requiredSize(24.dp),
                isPlaying = state.playing,
            )
        },
        headline = {
            Column {
                Label(
                    state.title ?: textResource(R.string.unknown),
                    modifier = Modifier
                        .thenIf(!showcase) { sharedElement(Glance.SHARED_TITLE) }
                        .marque(Int.MAX_VALUE),
                    style = AppTheme.typography.titleLarge
                )
                Label(
                    state.subtitle ?: textResource(R.string.unknown),
                    style = AppTheme.typography.caption,
                    color = LocalContentColor.current.copy(ContentAlpha.medium),
                    modifier = Modifier.thenIf(!showcase) { sharedElement(Glance.SHARED_SUBTITLE) },
                )
            }
        },
        trailing = {
            Artwork(
                data = state.artwork,
                modifier = Modifier
                    .requiredSize(DefaultArtworkSize)
                    .thenIf(!showcase) { sharedElement(Glance.SHARED_ARTWORK_ID) }
                    .clip(DefaultArtworkShape),
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
                        .background(AppTheme.colors.onAccent.copy(0.3f), CircleShape)
                    // SeekBackward
                    val ctx = LocalContext.current
                    IconButton(
                        onClick = { NowPlaying.trySend(ctx, NowPlaying.ACTION_PREVIOUS) },
                        imageVector = Icons.Outlined.KeyboardDoubleArrowLeft,
                        contentDescription = null,
                        modifier = bgModifier
                    )

                    FloatingActionButton(
                        backgroundColor = AppTheme.colors.onAccent.copy(0.3f),
                        contentColor = LocalContentColor.current,
                        shape = RoundedCornerShape(28),
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
                        modifier = bgModifier
                    )

                    //
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
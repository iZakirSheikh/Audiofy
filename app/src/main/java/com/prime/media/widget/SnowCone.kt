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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentColor
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.prime.media.R
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
import com.zs.core_ui.sharedBounds
import com.zs.core_ui.sharedElement

private val SnowConeShape = RoundedCornerShape(14)
private val DefaultArtworkShape = RoundedCornerShape(20)
private val DefaultArtworkSize = 84.dp


/**
 * A mini-player inspired by android 12 notification
 */
@Composable
fun SnowCone(
    state: NowPlaying,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    showcase: Boolean = false
) {
    val colors = AppTheme.colors
    val navController = LocalNavController.current
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
            .heightIn(max = 120.dp)
            .shadow(16.dp, SnowConeShape)
            .thenIf(
                !colors.isLight
            ) { border(0.5.dp, colors.accent.copy(0.12f), SnowConeShape) }
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
                    .size(DefaultArtworkSize)
                    .thenIf(!showcase) {
                        sharedElement(Glance.SHARED_ARTWORK_ID)
                            .clip(DefaultArtworkShape)
                    }
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
                            property = LottieProperty.STROKE_COLOR,
                            color.toArgb(),
                            "**"
                        )
                    )
                    // Play Toggle
                    LottieAnimButton(
                        id = R.raw.lt_play_pause,
                        atEnd = !state.playing,
                        scale = 1.8f,
                        progressRange = 0.0f..0.29f,
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
    )
}
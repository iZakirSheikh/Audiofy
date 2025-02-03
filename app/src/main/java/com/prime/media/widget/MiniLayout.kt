/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 18-12-2024.
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
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.prime.media.R
import com.prime.media.old.common.Artwork
import com.primex.core.SignalWhite
import com.primex.core.foreground
import com.zs.core.playback.NowPlaying
import com.zs.core_ui.AppTheme
import com.zs.core_ui.sharedBounds
import com.zs.core_ui.sharedElement

private const val TAG = "MiniLayout"

@Composable
fun MiniLayout(
    state: NowPlaying,
    modifier: Modifier = Modifier
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            // .clip(CircleShape)
            .sharedBounds(
                Glance.SHARED_BACKGROUND_ID,
                exit = fadeOut() + scaleOut(),
                enter = fadeIn() + scaleIn(),
                zIndexInOverlay = 0.21f
            )
            .shadow(Glance.ELEVATION, CircleShape)
            .background(AppTheme.colors.background(1.dp))
            .requiredSize(Glance.MIN_SIZE),
        content = {
            // The artwork of the current media item
            Artwork(
                data = state.artwork,
                modifier = Modifier
                    .border(1.dp, Color.White.copy(0.12f), CircleShape)
                    .aspectRatio(1.0f)
                    .sharedElement(Glance.SHARED_ARTWORK_ID, zIndexInOverlay = 0.22f)
                    .foreground(Color.Black.copy(0.24f), CircleShape)
                    .clip(CircleShape),
            )

            val properties = rememberLottieDynamicProperties(
                rememberLottieDynamicProperty(
                    property = LottieProperty.COLOR,
                    Color.SignalWhite.toArgb(),
                    "**"
                )
            )
            // show playing bars.
            com.prime.media.old.common.LottieAnimation(
                id = R.raw.playback_indicator,
                iterations = Int.MAX_VALUE,
                dynamicProperties = properties,
                modifier = Modifier
                    .sharedBounds(Glance.SHARED_PLAYING_BARS_ID, zIndexInOverlay = 0.23f)
                    .requiredSize(24.dp),
                isPlaying = state.playing,
            )
        }
    )
}
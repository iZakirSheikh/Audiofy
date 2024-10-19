/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 20-10-2024.
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

package com.prime.media.playlists

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.primex.core.foreground
import com.primex.core.thenIf
import com.primex.material2.Label
import com.zs.core.db.Playlist
import com.zs.core_ui.AppTheme
import coil.compose.rememberAsyncImagePainter as coilImagePainter
import com.zs.core_ui.ContentPadding as CP

private val ForegroundBrush = Brush.horizontalGradient(
    listOf(Color.Black, Color.Transparent),
    startX = Float.POSITIVE_INFINITY,
    endX = 0f
)
private val ArtworkShape = RoundedCornerShape(6.dp)
/**
 * Represents an item of [Playlists] screen
 */
@Composable
fun PlaylistItem(
    value: Playlist,
    focused: Boolean = false,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(CP.medium),
        content = {
            // Top section containing the artwork and count
            // and showcases a scaling animation if focused
            val artworkShape = ArtworkShape
            Box(
                Modifier
                    .thenIf(focused) {
                        val infiniteTransition = rememberInfiniteTransition(label = value.name)
                        val animation by infiniteTransition.animateFloat(
                            initialValue = 0.0f,
                            targetValue = 1.0f,
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 750, easing = LinearEasing),
                                repeatMode = RepeatMode.Reverse
                            ), label = value.name
                        )
                        graphicsLayer {
                            val scale = com.primex.core.lerp(1.0f, 0.8f, animation)
                            scaleX = scale
                            scaleY = scale
                        }.border(2.dp, Color.White, artworkShape)
                    }
                    .shadow(0.dp, artworkShape, true)
                    .aspectRatio(1.76f),
                content = {
                    Image(
                        coilImagePainter(value.artwork),
                        contentDescription = null,
                        modifier = Modifier
                            .foreground(ForegroundBrush)
                            .matchParentSize(),
                        contentScale = ContentScale.Crop
                    )

                    Icon(
                        Icons.Default.PlaylistPlay,
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(CP.medium),
                        tint = Color.White
                    )

                    Label(
                        "${value.count}",
                        style = AppTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(bottom = CP.medium, end = CP.normal)
                    )
                }
            )
            // Bottom section containing the playlist name
            Label(
                value.name,
                maxLines = 2,
                style = AppTheme.typography.caption,
                modifier = Modifier.padding(horizontal = CP.normal)
            )
        }
    )
}
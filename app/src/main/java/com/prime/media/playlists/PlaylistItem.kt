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

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.twotone.CheckCircle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImagePainter
import com.prime.media.R
import com.primex.core.SignalWhite
import com.primex.core.foreground
import com.primex.core.thenIf
import com.primex.material2.Label
import com.zs.core.db.Playlist
import com.zs.core_ui.AppTheme
import coil.compose.rememberAsyncImagePainter as ImagePainter
import coil.request.ImageRequest.Builder as ImageRequest
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
            // TopSection: Contains the artwork, play icon, track count, and focus indicator.
            Box(
                modifier = Modifier
                    .clip(ArtworkShape) // Clips the box to the desired artwork shape.
                  //  .background(AppTheme.colors.background(1.dp)) // Applies a background color.
                    .aspectRatio(1.76f), // Sets the aspect ratio of the artwork.
                content = {
                    // Artwork: Displays the playlist artwork.
                    val painter = ImagePainter(
                        ImageRequest(LocalContext.current).data(value.artwork)
                            .error(R.drawable.default_art) // Sets a default image if artwork loading fails.
                            .build()
                    )
                    val isError =
                        painter.state is AsyncImagePainter.State.Error // Checks if artwork loading failed.
                    val onColor =
                        if (isError) AppTheme.colors.onBackground else Color.SignalWhite // Sets the appropriate color based on artwork loading status.
                    Image(
                        painter,
                        contentDescription = null,
                        modifier = Modifier
                            .thenIf(!isError) { foreground(ForegroundBrush) } // Applies a foreground brush if artwork loading is successful.
                            .matchParentSize(), // Makes the image fill the parent's size.
                        contentScale = ContentScale.Crop // Crops the image to fit the container.
                    )

                    // Play Icon: Displays a play icon on top of the artwork.
                    Icon(
                        Icons.Default.PlaylistPlay,
                        contentDescription = null,
                        modifier = Modifier
                            .align(Alignment.TopEnd) // Aligns the icon to the top end.
                            .padding(CP.medium), // Adds padding around the icon.
                        tint = onColor // Sets the icon tint color.
                    )

                    // Track Count Label: Displays the number of tracks in the playlist.
                    Label(
                        "${value.count}",
                        style = AppTheme.typography.titleLarge,
                        color = onColor,
                        modifier = Modifier
                            .align(Alignment.BottomEnd) // Aligns the label to the bottom end.
                            .padding(
                                bottom = CP.medium,
                                end = CP.normal
                            ) // Adds padding around the label.
                    )

                    // Focus Indicator: Displays a checkmark icon when the item is focused.
                    if (!focused) return@Box // If not focused, skip displaying the focus indicator.
                    Spacer(
                        modifier = Modifier
                            .background(onColor.copy(0.35f)) // Applies a semi-transparent background.
                            .matchParentSize() // Makes the spacer fill the parent's size.
                    )

                    Icon(
                        Icons.TwoTone.CheckCircle,
                        contentDescription = "",
                        Modifier
                            .align(Alignment.Center) // Aligns the icon to the center.
                            .size(45.dp), // Sets the icon size.
                        tint = onColor // Sets the icon tint color.
                    )
                }
            )
            // Bottom (Label): Displays the playlist name.
            Label(
                value.name,
                maxLines = 2, // Limits the number of lines for the playlist name.
                style = AppTheme.typography.caption,
                modifier = Modifier.padding(horizontal = CP.normal) // Adds horizontal padding.
            )
        }
    )
}


/*
 * Copyright (c)  2025 Zakir Sheikh
 *
 * Created by sheik on 12 of Dec 2025
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.prime.media.library


import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.SaverScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.prime.media.R
import com.prime.media.common.emit
import com.prime.media.common.fadingEdge2
import com.primex.core.ImageBrush
import com.primex.core.SignalWhite
import com.primex.core.foreground
import com.primex.core.visualEffect
import com.primex.material2.Label
import com.zs.core.store.Audio
import com.zs.core.store.MediaProvider
import com.zs.core_ui.Anim
import com.zs.core_ui.AppTheme
import com.zs.core_ui.ContentElevation
import com.zs.core_ui.ContentPadding
import com.zs.core_ui.WallpaperAccentColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.runtime.saveable.rememberSaveable as savable

private val ColorSaver = object : androidx.compose.runtime.saveable.Saver<Color, Int> {
    override fun restore(value: Int): Color? {
        return Color(value)
    }

    override fun SaverScope.save(value: Color): Int? {
        return value.toArgb()
    }
}

private val Shape = RoundedCornerShape(15)

/**
 * Composable function to create a clickable newly added item with image, label, and play icon.
 *
 * @param label: The CharSequence representing the item's label.
 * @param onClick: The action to perform when the item is clicked.
 * @param modifier: Optional modifier to apply to the item's layout.
 * @param imageUri: Optional Uri for the item's image.
 * @param alignment: The alignment of the image within the item (default: Center).
 */
@Composable
private fun NewlyAddedItem(
    label: CharSequence,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    imageUri: Uri? = null,
) {
    Box(
        modifier = modifier
            .shadow(ContentElevation.low, Shape) // Light shadow
            .clickable(onClick = onClick) // Enable clicking
            .size(224.dp, 132.dp), // Set minimum size
        contentAlignment = Alignment.Center, // Center content within the box
        content = {
            val primary = AppTheme.colors.accent
            var savable by savable(
                imageUri?.toString(),
                stateSaver = ColorSaver,
                init = { mutableStateOf(Color.Unspecified) })
            val accent by animateColorAsState(
                savable.takeOrElse { primary },
                label = "accent-color"
            )
            val context = LocalContext.current
            val scope = rememberCoroutineScope()
            // Load image using Coil
            Image(
                contentDescription = null,
                modifier = Modifier
                    .visualEffect(ImageBrush.NoiseBrush, 0.3f, true)
                    .foreground(
                        Brush.horizontalGradient(
                            0.0f to accent.copy(0.8f),
                            0.3f to accent.copy(0.4f),
                            1.0f to Color.Transparent,
                        )
                    ) // Apply transparent-to-primary gradient
                    .foreground(Color.Black.copy(0.4f))
                    .background(AppTheme.colors.background(1.dp))
                    .matchParentSize(), // Fill available space,
                contentScale = ContentScale.Crop,
                alignment = Alignment.Center,
                painter = rememberAsyncImagePainter(
                    fallback = painterResource(id = R.drawable.default_art),
                    model = ImageRequest
                        .Builder(context).apply {
                            data(imageUri)
                            allowHardware(false)
                            crossfade(Anim.DefaultDurationMillis)
                        }
                        .build(),
                    onSuccess = {
                        if (savable != Color.Unspecified) return@rememberAsyncImagePainter
                        scope.launch(Dispatchers.IO) {
                            val image = it.result.drawable.toBitmap()
                            val value = WallpaperAccentColor(image, false, primary)
                            savable = Color(value)
                        }
                    }
                ),
            )

            // Play icon aligned to the right with padding and size
            Icon(
                imageVector = Icons.Rounded.PlayArrow,
                contentDescription = null, // Provide content description for accessibility
                modifier = Modifier
                    .align(Alignment.CenterStart) // Align to the right
                    .padding(horizontal = ContentPadding.large) // Add horizontal padding
                    .size(64.dp), // Set icon size
                tint = Color.SignalWhite.copy(0.6f) // Use contrasting color
            )

            // Label aligned to the left with padding and styling
            Label(
                text = label,
                modifier = Modifier
                    .padding(horizontal = ContentPadding.medium) // Add horizontal padding
                    .fillMaxWidth(0.5f) // Take up half the available width
                    .align(Alignment.CenterEnd), // Align to the left
                style = AppTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2, // Allow at most 2 lines for label
                color = Color.SignalWhite, // Use contrasting text color
            )
        }
    )
}

private val LargeListItemArrangement = Arrangement.spacedBy(16.dp)

@Composable
context(_: RouteLibrary)
fun Latest(
    viewState: LibraryViewState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    // Collect newly added items from the Library state
    val audios by viewState.newlyAdded.collectAsState()
    // Display the list with loading, empty, and content states
    LazyRow (
        horizontalArrangement = LargeListItemArrangement,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.fadingEdge2(20.dp, false),
        contentPadding = contentPadding,
        content = {
            // Ensure first item is visible by adding a spacer at the front
            item(contentType = "library_list_spacer") {
                Spacer(modifier = Modifier)
            }

            val data = emit(false, audios) ?: return@LazyRow
            items(data, key = Audio::id) { item ->
                // Create newly added item with parallax-adjusted image alignment
                NewlyAddedItem(
                    label = item.name,
                    onClick = { viewState.onClickRecentAddedFile(item.id) },
                    imageUri = MediaProvider.buildAlbumArtUri(item.albumId),
                    modifier = Modifier.animateItem(),
                )
            }
        }
    )
}
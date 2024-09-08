/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 07-07-2024.
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

package com.prime.media.library

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.PlayCircle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.palette.graphics.Palette
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import com.prime.media.Material
import com.prime.media.R
import com.prime.media.core.Anim
import com.prime.media.core.ContentElevation
import com.prime.media.core.ContentPadding
import com.prime.media.core.compose.Artwork
import com.prime.media.core.db.albumUri
import com.prime.media.small2
import com.primex.core.ImageBrush
import com.primex.core.blend
import com.primex.core.foreground
import com.primex.core.visualEffect
import com.primex.material2.Label
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

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
    alignment: Alignment = Alignment.Center,
) {
    Box(
        modifier = modifier
            .shadow(ContentElevation.low, Material.shapes.small2) // Light shadow
            .clickable(onClick = onClick) // Enable clicking
            .size(224.dp, 132.dp), // Set minimum size
        contentAlignment = Alignment.Center // Center content within the box
    ) {

        var accent by remember { mutableStateOf(Color.Unspecified) }
        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        val primary = MaterialTheme.colors.primary
        Image(
            contentDescription = null,
            modifier = Modifier
                .visualEffect(ImageBrush.NoiseBrush, 0.3f, true)
                .foreground(
                    Brush.horizontalGradient(
                        listOf(
                            accent.takeOrElse { primary },
                            accent.takeOrElse { primary }.copy(0.5f),
                            Color.Transparent,
                        )
                    )
                ) // Apply transparent-to-primary gradient
                .foreground(Color.Black.copy(0.2f))
                .background(Material.colors.surface)
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
                    scope.launch(Dispatchers.IO) {
                        val image = it.result.drawable.toBitmap()
                        val value =
                            Palette.from(image).generate().getDominantColor(primary.toArgb())
                        accent = Color(value)
                    }
                }
            ),
        )

        // Label aligned to the left with padding and styling
        Label(
            text = label,
            modifier = Modifier
                .padding(horizontal = ContentPadding.normal) // Add horizontal padding
                .fillMaxWidth(0.5f) // Take up half the available width
                .align(Alignment.CenterStart), // Align to the left
            style = Material.typography.body1,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2, // Allow at most 2 lines for label
            color = Material.colors.onPrimary, // Use contrasting text color
        )

        // Play icon aligned to the right with padding and size
        Icon(
            imageVector = Icons.TwoTone.PlayCircle,
            contentDescription = null, // Provide content description for accessibility
            modifier = Modifier
                .align(Alignment.CenterEnd) // Align to the right
                .padding(horizontal = ContentPadding.large) // Add horizontal padding
                .size(40.dp, 40.dp), // Set icon size
            tint = Material.colors.onPrimary // Use contrasting color
        )
    }
}

private val LargeListItemArrangement = Arrangement.spacedBy(16.dp)

/**
 * A Composable function that displays a list of newly added items.
 *
 * @param state The state of the library.
 * @param modifier The modifier to be applied to the list.
 * @param contentPadding The padding to be applied to the list.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NewlyAddedList(
    state: Library,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp)
) {
    // Collect newly added items from the Library state
    val audios by state.newlyAdded.collectAsState()
    // Display the list with loading, empty, and content states
    StatefulLazyList(
        items = audios,
        key = { it.id },
        modifier = modifier,
        horizontalArrangement = LargeListItemArrangement,
        contentPadding = contentPadding
    ) { item ->
        // Create newly added item with parallax-adjusted image alignment
        NewlyAddedItem(
            label = item.name,
            onClick = { state.onClickRecentAddedFile(item.id) },
            imageUri = item.albumUri,
            modifier = Modifier.animateItem(),
        )
    }
}


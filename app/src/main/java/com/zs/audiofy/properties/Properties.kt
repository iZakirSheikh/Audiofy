/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 21-07-2025.
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

package com.zs.audiofy.properties

import android.view.Gravity
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.ParagraphStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogWindowProvider
import com.zs.audiofy.R
import com.zs.audiofy.common.compose.ContentPadding
import com.zs.audiofy.common.compose.LocalNavController
import com.zs.audiofy.common.compose.fadingEdge2
import com.zs.audiofy.common.compose.shine
import com.zs.compose.foundation.ImageBrush
import com.zs.compose.foundation.fullLineSpan
import com.zs.compose.foundation.textResource
import com.zs.compose.foundation.visualEffect
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.Button
import com.zs.compose.theme.ContentAlpha
import com.zs.compose.theme.LocalContentColor
import com.zs.compose.theme.LocalWindowSize
import com.zs.compose.theme.WindowSize.Category
import com.zs.compose.theme.adaptive.HorizontalTwoPaneStrategy
import com.zs.compose.theme.adaptive.TwoPane
import com.zs.compose.theme.adaptive.VerticalTwoPaneStrategy
import com.zs.compose.theme.text.Text

private const val TAG = "Properties"

private val TITLE_STYLE = ParagraphStyle(lineHeight = 20.sp)

/**
 * Represents a single property in the properties dialog.
 */
private inline fun LazyGridScope.Property(
    @StringRes title: Int,
    value: CharSequence,
    noinline span: (LazyGridItemSpanScope.() -> GridItemSpan)? = null,
) {
    item(key = title, contentType = "property", span = span) {
        val name = textResource(id = title)
        val color = LocalContentColor.current
        Text(
            style = AppTheme.typography.label3,
            color = color.copy(ContentAlpha.medium),
            modifier = Modifier.padding(ContentPadding.normal, ContentPadding.small),
            text = buildAnnotatedString {
                // Title
                withStyle(TITLE_STYLE) {
                    append(name)
                }
                // Desc
                withStyle(SpanStyle(fontSize = 16.sp, color = color)) {
                    append(value)
                }
            }
        )
    }
}


private val DETAILS_CONTENT_PADDING = PaddingValues(vertical = ContentPadding.normal)

@Composable
private fun Details(viewState: PropertiesViewState, modifier: Modifier = Modifier) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.SpaceAround,
        modifier = modifier.fadingEdge2(24.dp),
        contentPadding = DETAILS_CONTENT_PADDING,
        content = {
            // Title
            Property(
                title = R.string.title,
                value = viewState.title,
                span = fullLineSpan
            )

            // Path
            Property(
                title = R.string.file_path,
                value = viewState.path,
                span = fullLineSpan
            )

            // Size
            Property(
                title = R.string.size,
                value = viewState.size,
            )

            // Format
            Property(
                title = R.string.format,
                value = viewState.mimeType
            )

            // Bitrate
            Property(
                title = R.string.bitrate, value = viewState.bitrate
            )

            // SampleRate
            Property(
                title = R.string.sampling_rate, value = viewState.sampleRate
            )

            // BitsPerSample
            Property(
                title = R.string.bits_per_sample, value = viewState.bitsPerSample
            )

            // Duration
            Property(
                title = R.string.duration, value = viewState.duration
            )

            // Year
            Property(
                title = R.string.year,
                value = viewState.year
            )


            // Disk Number
            Property(
                title = R.string.disk_number,
                value = viewState.diskNumber
            )

            // Track Number
            Property(
                title = R.string.track_number,
                value = viewState.trackNumber
            )

            // Artist
            Property(
                title = R.string.artist,
                value = viewState.artist
            )
            // Album
            Property(
                title = R.string.album,
                value = viewState.album
            )

            // Genre
            Property(
                title = R.string.genre,
                value = viewState.genre
            )

            // Composer
            Property(
                title = R.string.composer,
                value = viewState.composer
            )
            // Author
            Property(
                title = R.string.author,
                value = viewState.author
            )
            // Writer
            Property(
                title = R.string.writer,
                value = viewState.writer
            )
        }
    )
}

@Composable
private fun Artwork(bitmap: ImageBitmap?, modifier: Modifier = Modifier) {
    // Artwork
    if (bitmap != null)
        Image(
            bitmap = bitmap,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .visualEffect(ImageBrush.NoiseBrush, overlay = true, alpha = 0.3f)
        )
    else
        Image(
            painterResource(R.drawable.ic_error_image_placeholder),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .background(AppTheme.colors.background(1.dp))
        )
}

@Composable
fun Properties(viewState: PropertiesViewState) {
    val clazz = LocalWindowSize.current
    val compact = clazz.width < Category.Medium
    val view = LocalView.current
    SideEffect {
        val window = (view.parent as DialogWindowProvider).window
        window.setGravity(if (compact) Gravity.BOTTOM else Gravity.CENTER)
    }
    //
    val shape = if (compact) AppTheme.shapes.xLarge else AppTheme.shapes.large
    // TODO: The Box composable is used here as a workaround.
    // It's currently employed to position the dismiss button as a Floating Action Button (FAB).
    // This approach is necessary because the FAB within the TwoPane component isn't behaving as expected.
    // Once the TwoPane's FAB becomes customizable, this logic will be refactored and moved back into the TwoPane.
    Box() {
        // content
        TwoPane(
            spacing = ContentPadding.normal,
            strategy = if (compact) VerticalTwoPaneStrategy(0.3f) else HorizontalTwoPaneStrategy(0.35f),
            modifier = Modifier
                .requiredSizeIn(
                    maxWidth = (clazz.value.width * 0.9f).coerceAtMost(550.dp),
                    maxHeight = (clazz.value.height * if (compact) 0.7f else 0.8f)
                )
                .border(AppTheme.colors.shine, shape)
                .clip(shape),
            primary = { Artwork(viewState.artwork, modifier = Modifier.fillMaxSize()) },
            secondary = { Details(viewState, Modifier.fillMaxSize()) }
        )

        // Fab - Dismiss Button
        val navController = LocalNavController.current
        Button(
            textResource(R.string.dismiss),
            onClick = navController::navigateUp,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        )
    }
}
/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 19-01-2025.
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

package com.zs.widget

import android.net.Uri
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.appwidget.ImageProvider
import androidx.glance.appwidget.appWidgetBackground
import androidx.glance.appwidget.components.CircleIconButton
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ContentScale
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.layout.wrapContentHeight
import androidx.glance.layout.wrapContentSize
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.zs.core.playback.NowPlaying
import androidx.glance.GlanceModifier as Modifier

private const val TAG = "Universal"

private val ROUNDNESS = 12.dp

private val PAUSE = ImageProvider(R.drawable.media3_notification_pause)
private val PLAY = ImageProvider(R.drawable.media3_notification_play)
private val CONFIG = ImageProvider(R.drawable.ic_config)
private val SKIP_TO_NEXT = ImageProvider(R.drawable.ic_skip_to_next)
private val SKIP_TO_PREV = ImageProvider(R.drawable.ic_skip_to_prev)

private val ArtworkBg = Color.Gray.copy(0.7f)

@Composable
internal fun Universal(state: NowPlaying, type: ViewType) {
    // Define a common top_modifier
    val modifier = Modifier
        // Rounded corners (cornerRadius) are not directly supported in Android API levels below 12.
        // We're using a background drawable with rounded corners as a workaround for backward compatibility.
        .background(
            ImageProvider(R.drawable.rect_rounded_cornors_12dp),
            colorFilter = ColorFilter.tint(GlanceTheme.colors.surface)
        )
       // cornerRadius(ROUNDNESS)
            //.background(GlanceTheme.colors.surface)
            .padding(8.dp)
            .appWidgetBackground()
            .launchApp()
            .fillMaxSize()
    // Load appropriate composable based on the ViewType
    when (type) {
        ViewType.COMPACT -> Compact(state, modifier)
        ViewType.SQUARE -> Square(state, modifier)
        ViewType.NORMAL -> Normal(state, modifier)
    }
}

@Composable
private fun Compact(
    state: NowPlaying,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    // Define the Layout of compact.
    Row(
        modifier.then(Modifier.wrapContentHeight()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Define first the image.
        Image(
            ImageProvider(state.artwork ?: Uri.EMPTY),
            state.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.background(ArtworkBg).cornerRadius(ROUNDNESS)
                .size(50.dp),
        )

        // Column (Chronometer | Title)
        Column(
            modifier = Modifier.padding(horizontal = 4.dp).wrapContentSize().defaultWeight(),
            content = {
                Chronometer(
                    remember(state.title) { SystemClock.elapsedRealtime() - state.position },
                    started = state.playing,
                    textSize = 17.sp,
                    textColor = GlanceTheme.colors.onBackground.getColor(ctx),
                    bold = true
                )
                Text(
                    state.title ?: "N/A",
                    maxLines = 1,
                    style = TextStyle(color = GlanceTheme.colors.onBackground, fontSize = 11.sp),
                    modifier = Modifier
                )
            }
        )

        // Button (Play/Pause)
        CircleIconButton(
            if (state.playing) PAUSE else PLAY,
            "",
            onClick = { NowPlaying.trySend(ctx, NowPlaying.ACTION_TOGGLE_PLAY) },
            backgroundColor = null,
            contentColor = GlanceTheme.colors.primary,
            modifier = Modifier.size(40.dp)
        )
    }
}

@Composable
private fun Square(
    state: NowPlaying,
    modifier: Modifier = Modifier
) {
    // Square
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val ctx = LocalContext.current
        // Artwork
        Box(
            modifier = Modifier
                .cornerRadius(ROUNDNESS)
                .fillMaxSize()
                .defaultWeight()
                .background(GlanceTheme.colors.surfaceVariant),
            content = {
                Image(
                    ImageProvider(state.artwork ?: Uri.EMPTY),
                    state.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.background(ArtworkBg).cornerRadius(ROUNDNESS).fillMaxSize(),
                )

                val color = Color.White
                Chronometer(
                    remember(state.title) { SystemClock.elapsedRealtime() - state.position },
                    started = state.playing,
                    textSize = 22.sp,
                    textColor = color,
                    bold = true,
                    modifier = Modifier.padding(start = 6.dp).cornerRadius(6.dp)
                        .background(color.copy(0.45f))
                )
            }
        )

        // Controls
        Row(
            modifier = Modifier,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.CenterVertically,
            content = {
                // Skip to previous
                CircleIconButton(
                    SKIP_TO_PREV,
                    "",
                    onClick = { NowPlaying.trySend(ctx, NowPlaying.ACTION_PREVIOUS) },
                    backgroundColor = null
                )

                // Play/Pause
                CircleIconButton(
                    if (state.playing) PAUSE else PLAY,
                    "",
                    onClick = { NowPlaying.trySend(ctx, NowPlaying.ACTION_TOGGLE_PLAY) },
                    contentColor = GlanceTheme.colors.primary,
                    modifier = Modifier.size(40.dp)
                )

                // Skip to Next
                CircleIconButton(
                    ImageProvider(R.drawable.ic_skip_to_next),
                    "",
                    onClick = { NowPlaying.trySend(ctx, NowPlaying.ACTION_NEXT) },
                    backgroundColor = null
                )
            }
        )
    }
}

@Composable
private fun Normal(
    state: NowPlaying,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.wrapContentHeight().width(320.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalAlignment = Alignment.Top
    ) {
        // Artwork
        Image(
            ImageProvider(state.artwork ?: Uri.EMPTY),
            state.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.background(ArtworkBg).cornerRadius(ROUNDNESS).size(68.dp),
        )

        // Column (Overline|Title|Subtitle)
        Column(
            modifier = Modifier.padding(horizontal = 8.dp).defaultWeight(),
            horizontalAlignment = Alignment.Start,
            verticalAlignment = Alignment.Top,
            content = {
                // Title
                Text(
                    state.title ?: "",
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.onBackground,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    modifier = Modifier
                )

                // Subtitle
                val ctx = LocalContext.current
                Text(
                    state.subtitle ?: "",
                    maxLines = 1,
                    style = TextStyle(
                        color = GlanceTheme.colors.onBackground.copy(ctx, 0.6f),
                        fontSize = 11.sp,
                    ),
                    modifier = Modifier
                )

                // Controls
                Row(
                    modifier = Modifier,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalAlignment = Alignment.CenterVertically,

                    content = {
                        // Skip to previous
                        CircleIconButton(
                            ImageProvider(R.drawable.ic_skip_to_prev),
                            "",
                            onClick = { NowPlaying.trySend(ctx, NowPlaying.ACTION_PREVIOUS) },
                            backgroundColor = null
                        )

                        // Play/Pause
                        CircleIconButton(
                            if (state.playing) PAUSE else PLAY,
                            "",
                            onClick = { NowPlaying.trySend(ctx, NowPlaying.ACTION_TOGGLE_PLAY) },
                            contentColor = GlanceTheme.colors.primary,
                            modifier = Modifier.size(40.dp)
                        )

                        // Skip to Next
                        CircleIconButton(
                            ImageProvider(R.drawable.ic_skip_to_next),
                            "",
                            onClick = { NowPlaying.trySend(ctx, NowPlaying.ACTION_NEXT) },
                            backgroundColor = null
                        )
                    }
                )

            }
        )

        val ctx = LocalContext.current
        CircleIconButton(
            ImageProvider(R.drawable.ic_config),
            "",
            onClick = {
                Toast.makeText(ctx, "Action not available!", Toast.LENGTH_SHORT).show()
            },
            backgroundColor = null,
            contentColor = GlanceTheme.colors.onBackground
        )
    }
}
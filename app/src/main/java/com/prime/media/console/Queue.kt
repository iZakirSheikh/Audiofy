/*
 * Copyright (c)  2026 Zakir Sheikh
 *
 * Created by sheik on 26 of Jan 2026
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
 *
 * Last Modified by sheik on 26 of Jan 2026
 *
 */

package com.prime.media.console

import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Scaffold
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Queue
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.rememberAsyncImagePainter
import com.prime.media.R
import com.prime.media.common.emit
import com.prime.media.common.lottie
import com.prime.media.common.shine
import com.primex.core.ImageBrush
import com.primex.core.drawHorizontalDivider
import com.primex.core.foreground
import com.primex.core.textResource
import com.primex.core.thenIf
import com.primex.core.visualEffect
import com.primex.material2.Label
import com.primex.material2.ListTile
import com.zs.core.playback.MediaFile
import com.zs.core.playback.NowPlaying
import com.zs.core.playback.NowPlaying2
import com.zs.core_ui.AppTheme
import com.zs.core_ui.ContentPadding as CP
import com.zs.core_ui.Divider
import com.zs.core_ui.Header
import com.zs.core_ui.Indication
import com.zs.core_ui.adaptive.contentInsets
import com.zs.core_ui.lottieAnimationPainter
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.WindowInsetsSides as WIS

private val ThumbnailModifier = Modifier.size(110.dp, 64.dp)

private val NonePlaying = NowPlaying2(null, null)
private val NoiseTexture = Modifier.visualEffect(ImageBrush.NoiseBrush, 0.2f, overlay = false)
private val ScrimModifier = Modifier.foreground(Color.Black.copy(0.35f))

@Composable
private fun MediaFile(
    value: MediaFile,
    modifier: Modifier = Modifier,
    playing: Boolean = false,
    actions: @Composable () -> Unit,
) {
    val progress by animateFloatAsState(
        if (playing) 1f else 0f,
        tween(500, 200)
    )
    val colors = AppTheme.colors
    ListTile(
        trailing = actions,
        centerAlign = false,
        modifier = modifier.thenIf(playing) {
            NoiseTexture then Modifier.background(AppTheme.colors.accent.copy(progress * ContentAlpha.Indication))
        },
        onColor = lerp(colors.onBackground, colors.accent, progress),
        headline = {
            val typography = AppTheme.typography
            Label(
                text = value.title ?: stringResource(R.string.abbr_not_available),
                maxLines = 2,
                fontWeight = androidx.compose.ui.text.font.lerp(FontWeight.Normal, FontWeight.Bold, progress),
                style = typography.titleSmall,
            )
        },
        subtitle = {
            Label(
                value.subtitle ?: stringResource(R.string.abbr_not_available),
                style = AppTheme.typography.caption2
            )
        },
        leading = {
            // Thumbnail
            Box(
                Modifier
                    .shadow(2.dp, AppTheme.shapes.medium)
                    .background(AppTheme.colors.background(1.dp)) then ThumbnailModifier,
                contentAlignment = Alignment.Center,
                content = {
                    Image(
                        rememberAsyncImagePainter(value.artworkUri),
                        contentDescription = value.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .matchParentSize()
                            .thenIf(playing) { ScrimModifier }
                    )

                    if (!playing)
                        return@Box
                    Icon(
                        painter = lottieAnimationPainter(R.raw.playback_indicator),
                        contentDescription = null,
                        modifier = Modifier.lottie(),
                        tint = Color.White
                    )
                }
            )
        },
    )
}

/**
 * Represents the layout of the queue.
 */
@Composable
fun Queue(viewState: QueueViewState, shape: Shape, insets: WindowInsets) {

}
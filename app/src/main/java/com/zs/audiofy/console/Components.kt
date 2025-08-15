/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 07-08-2025.
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

package com.zs.audiofy.console

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zs.audiofy.R
import com.zs.audiofy.common.compose.lottie
import com.zs.audiofy.common.compose.lottieAnimationPainter
import com.zs.compose.foundation.ImageBrush
import com.zs.compose.foundation.thenIf
import com.zs.compose.foundation.visualEffect
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.ButtonDefaults
import com.zs.compose.theme.ContentAlpha
import com.zs.compose.theme.Icon
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.LocalContentColor
import com.zs.compose.theme.Slider
import com.zs.compose.theme.SliderDefaults
import com.zs.compose.theme.Surface

/** @return A [DpRect] containing the left, top, right, and bottom insets in density-independent pixels (dp). */
val WindowInsets.asDpRect: DpRect
    @Composable
    @ReadOnlyComposable
    get() {
        val ld =
            LocalLayoutDirection.current  // Get current layout direction for correct inset handling
        val density = LocalDensity.current    // Get current screen density for conversion to dp
        with(density) {
            // Convert raw insets to dp values, considering layout direction
            return DpRect(
                left = getLeft(density, ld).toDp(),
                right = getRight(this, ld).toDp(),
                top = getTop(this).toDp(),
                bottom = getBottom(this).toDp()
            )
        }
    }


/**
 * Composes a artwork representation for PLayer Console view
 */
@Composable
inline fun Artwork(
    model: Any?,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    border: Dp = 1.dp,
    shadow: Dp = 0.dp,
) {
    key(model) {
        AsyncImage(
            model = model,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = modifier
                .visualEffect(ImageBrush.NoiseBrush, 0.5f, true)
                .thenIf(border > 0.dp) { border(1.dp, Color.White, shape) }
                .shadow(shadow, shape, clip = shape != RectangleShape)
                .background(AppTheme.colors.background(1.dp)),
        )
    }
}

@Composable
private fun OutlinedPlayButton(
    onClick: () -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier.size(60.dp),
        shape = AppTheme.shapes.large,
        color = Color.Transparent,
        border = BorderStroke(
            1.dp,
            AppTheme.colors.onBackground.copy(if (!AppTheme.colors.isLight) ContentAlpha.indication else ContentAlpha.medium)
        ),
        contentColor = LocalContentColor.current,
        content = {
            Icon(
                painter = lottieAnimationPainter(
                    id = R.raw.lt_play_pause,
                    atEnd = isPlaying,
                    progressRange = 0.0f..0.29f,
                    animationSpec = tween(easing = LinearEasing)
                ),
                modifier = Modifier.lottie(1.5f),
                contentDescription = null
            )
        }
    )
}

@Composable
private fun SimplePlayButton(
    onClick: () -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            painter = lottieAnimationPainter(
                id = R.raw.lt_play_pause,
                atEnd = isPlaying,
                progressRange = 0.0f..0.29f,
                animationSpec = tween(easing = LinearEasing)
            ),
            modifier = Modifier.lottie(1.5f),
            contentDescription = null
        )
    }
}

/**
 * Composes a simple button representation for Player Console view.
 * @param style Button style from [Console]  e.g., [Console.PLAY_BTN_STYLE_SIMPLE]
 */
@Composable
@NonRestartableComposable
fun PlayButton(
    onClick: () -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    style: Int = Console.STYLE_PLAY_BUTTON_SIMPLE,
) {
   when(style){
       Console.STYLE_PLAY_OUTLINED -> OutlinedPlayButton(onClick, isPlaying, modifier)
       Console.STYLE_PLAY_BUTTON_SIMPLE -> SimplePlayButton(onClick, isPlaying, modifier)
       else -> error("Invalid style $style")
   }
}

/**
 * Represents the Slider for Console's PlayerView.
 */
@Composable
fun TimeBar(
    progress: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    // FIXME: This is a temporary workaround.
    //  Problem:
    //  The Slider composable uses BoxWithConstraints internally. When used within a ConstraintLayout
    //  with width Dimension.fillToConstraints, it behaves unexpectedly. This workaround addresses the issue.
    //  Remove this workaround once the underlying issue is resolved.
    var width by remember { mutableIntStateOf(0) }
    Box(modifier.onSizeChanged() {
        width = it.width
    }) {
        Slider(
            progress,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            modifier = Modifier.width(with(LocalDensity.current) { width.toDp() }),
            enabled = enabled,
            colors = SliderDefaults.colors(
                disabledThumbColor = AppTheme.colors.accent,
                disabledActiveTrackColor = AppTheme.colors.accent
            )
        )
    }
}

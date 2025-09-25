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

import android.app.Activity
import android.content.pm.ActivityInfo
import android.media.audiofx.AudioEffect
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
import com.zs.audiofy.BuildConfig
import com.zs.audiofy.R
import com.zs.audiofy.common.SystemFacade
import com.zs.audiofy.common.compose.lottie
import com.zs.audiofy.common.compose.lottieAnimationPainter
import com.zs.compose.foundation.ImageBrush
import com.zs.compose.foundation.thenIf
import com.zs.compose.foundation.visualEffect
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.ContentAlpha
import com.zs.compose.theme.Icon
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.LinearProgressIndicator
import com.zs.compose.theme.LocalContentColor
import com.zs.compose.theme.Slider
import com.zs.compose.theme.SliderDefaults
import com.zs.compose.theme.Surface

val WindowInsets.toDpRect: DpRect
    @Composable
    inline get() {
        val ldr = LocalLayoutDirection.current
        val density = LocalDensity.current
        val insets = this
        return remember(insets, ldr.ordinal, density.density) {
            with(density) {
                // Convert raw insets to dp values, considering layout direction
                DpRect(
                    left = insets.getLeft(density, ldr).toDp(),
                    right = insets.getRight(this, ldr).toDp(),
                    top = insets.getTop(this).toDp(),
                    bottom = insets.getBottom(this).toDp()
                )
            }
        }
    }

/**
 * Toggles the screen rotation lock for the current activity.
 *
 * If the current screen orientation is unspecified ([ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED]),
 * this function sets the requested screen orientation to [ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE],
 * effectively locking the screen to landscape mode. Otherwise, it resets the screen orientation to
 * [ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED], allowing the system to determine the orientation based
 * on the device sensor.
 *
 * @return `true` if the screen orientation is locked to landscape after the toggle,
 *         `false` otherwise.
 *
 * @see ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
 * @see ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
 */
fun Activity.toggleRotationLock(): Boolean {
    // Determine the new screen orientation based on the current state.
    val rotation =
        if (requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED)
            ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        else
            ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

    // Set the requested screen orientation to the calculated value.
    requestedOrientation = rotation

    // Return the weather orientation is locked or not.
    return rotation == ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
}

/**
 * Returns `true` if the screen orientation is locked, `false` otherwise.
 *
 * @see ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
 */
inline val Activity.isOrientationLocked
    get() = requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

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
                .shadow(shadow, shape, clip = shape != RectangleShape)
                .thenIf(border > 0.dp) { border(border, Color.White, shape) }
                .visualEffect(ImageBrush.NoiseBrush, 0.5f, true)
                .background(AppTheme.colors.background(1.dp)),
        )
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
    accent: Color = AppTheme.colors.accent
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
        when{
            progress >= 0 -> Slider(
                progress,
                onValueChange = onValueChange,
                onValueChangeFinished = onValueChangeFinished,
                modifier = Modifier.width(with(LocalDensity.current) { width.toDp() }),
                enabled = enabled,
                colors = SliderDefaults.colors(
                    thumbColor = accent,
                    activeTrackColor = accent,
                    disabledThumbColor = accent,
                    disabledActiveTrackColor = accent
                )
            )
            else -> LinearProgressIndicator(
                color = accent,
                backgroundColor = accent.copy(ContentAlpha.indication)
            )
        }
    }
}

@Composable
private fun OutlinedPlayButton(
    onClick: () -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Surface(
        onClick = onClick,
        enabled = enabled,
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
    enabled: Boolean = true,
) {
    IconButton(onClick = onClick, modifier = modifier, enabled = enabled) {
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
    enabled: Boolean = true,
    simple: Boolean = false,
) {
    when {
        !simple -> OutlinedPlayButton(onClick, isPlaying, modifier,enabled)
        else -> SimplePlayButton(onClick, isPlaying, modifier, enabled)
    }
}

fun SystemFacade.launchEqualizer(id: Int) {
    if (id == AudioEffect.ERROR_BAD_VALUE)
        return showToast(R.string.msg_unknown_error)
    val intent = android.content.Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
        putExtra(AudioEffect.EXTRA_PACKAGE_NAME, BuildConfig.APPLICATION_ID)
        putExtra(AudioEffect.EXTRA_AUDIO_SESSION, id)
        putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
    }
    val res = runCatching { launch(intent) }
    if (!res.isFailure)
        return
    showToast(message = R.string.msg_3rd_party_equalizer_not_found)
}

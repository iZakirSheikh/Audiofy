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

@file:OptIn(ExperimentalMaterialApi::class)

package com.prime.media.console

import android.app.Activity
import android.content.pm.ActivityInfo
import android.media.audiofx.AudioEffect
import android.net.Uri
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.prime.media.BuildConfig
import com.prime.media.R
import com.prime.media.common.SystemFacade
import com.prime.media.common.lottie
import com.primex.core.ImageBrush
import com.primex.core.thenIf
import com.primex.core.visualEffect
import com.primex.material2.Label
import com.primex.material2.Text
import com.zs.core_ui.AppTheme
import com.zs.core_ui.Indication
import com.zs.core_ui.lottieAnimationPainter
import com.prime.media.console.RouteConsole as RC

context(_:RC)
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
context(_: RC)
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
context(_: RC)
inline val Activity.isOrientationLocked
    get() = requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

/**
 * Composes a artwork representation for PLayer Console view
 */
@Composable
context(_: RC)
fun Artwork(
    uri: Uri?,
    modifier: Modifier = Modifier,
    shape: Shape = RectangleShape,
    border: Dp = 1.dp,
    shadow: Dp = 0.dp,
) {
    AsyncImage(
        model = uri,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier
            .shadow(shadow, shape, clip = shape != RectangleShape)
            .thenIf(border > 0.dp) { border(border, Color.White, shape) }
            .visualEffect(ImageBrush.NoiseBrush, 0.5f, true)
            .background(AppTheme.colors.background(1.dp)),
    )
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
        shape = AppTheme.shapes.small,
        color = Color.Transparent,
        border = BorderStroke(
            1.dp,
            AppTheme.colors.onBackground.copy(if (!AppTheme.colors.isLight) ContentAlpha.Indication else ContentAlpha.medium)
        ),
        contentColor = LocalContentColor.current,
        content = {
            Icon(
                painter = lottieAnimationPainter(
                    id = R.raw.lt_play_pause,
                    atEnd = isPlaying,
                    progressRange = 0.0f..0.29f,
                    easing = LinearEasing
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
                easing = LinearEasing
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
context(_:RC)
fun PlayButton(
    onClick: () -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: Int = RC.PLAY_BTN_STYLE_SIMPLE
) {
    when(style) {
        RC.PLAY_BTN_STYLE_OUTLINED -> OutlinedPlayButton(onClick, isPlaying, modifier, enabled)
        else -> SimplePlayButton(onClick, isPlaying, modifier, enabled)
    }
}

context(_: RC)
fun SystemFacade.launchEqualizer(id: Int) {
    if (id == AudioEffect.ERROR_BAD_VALUE)
        return showToast(R.string.msg_unknown_error)
    val intent =
        android.content.Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
            putExtra(AudioEffect.EXTRA_PACKAGE_NAME, BuildConfig.APPLICATION_ID)
            putExtra(AudioEffect.EXTRA_AUDIO_SESSION, id)
            putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
        }
    val res = runCatching { launch(intent) }
    if (!res.isFailure)
        return
    showToast(message = R.string.msg_3rd_party_equalizer_not_found)
}


@Composable
@NonRestartableComposable
context(_: RC)
fun Background(
    artwork: Uri?= null,
    style: Int = RC.BG_STYLE_AUTO,
    modifier: Modifier = Modifier
) {
    Crossfade(
        targetState = style,
        modifier = modifier,
        content = { value ->
            when (value) {
                RC.BG_STYLE_AUTO_ACRYLIC -> /*Acrylic(artwork, Modifier.fillMaxSize())*/ {}
                else -> Spacer(
                    Modifier
                        .background(Color.Black)
                        .fillMaxSize()
                )
            }
        }
    )
}

@Composable
context(_: RC)
fun ExtraInfo(
    provider: () -> CharSequence,
    color: Color = LocalContentColor.current,
    modifier: Modifier = Modifier
) {
    Label(
        style = AppTheme.typography.caption2,
        color = color,
        modifier = modifier,
        text = provider()
    )
}


private val CUE_TEXT_SHADOW = Shadow(offset = Offset(5f, 5f), blurRadius = 8.0f)
@Composable
context(_: RC)
fun Cue(
    provider: () -> CharSequence,
    modifier: Modifier = Modifier,
    color: Color = LocalContentColor.current
) {
    Text(
        text = provider(),
        color = color,
        modifier = modifier,
        style = AppTheme.typography.bodyLarge.copy(
            shadow = CUE_TEXT_SHADOW,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        ),
        textAlign = TextAlign.Center
    )
}
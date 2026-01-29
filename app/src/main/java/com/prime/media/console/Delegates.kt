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
import android.util.Log
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Slider
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.LongState
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
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
import com.zs.core.playback.VideoProvider
import com.zs.core_ui.AppTheme
import com.zs.core_ui.Indication
import com.zs.core_ui.lottieAnimationPainter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import com.prime.media.console.RouteConsole as RC

private const val TAG = "Delegates"

context(_: RC)
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
        shape = RoundedCornerShape(25),
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
                    atEnd = !isPlaying,
                    progressRange = 0.0f..0.29f,
                    easing = LinearEasing,
                    duration = 300
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
                atEnd = !isPlaying,
                progressRange = 0.0f..0.29f,
                easing = LinearEasing,
                duration = 300
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
context(_: RC)
fun PlayButton(
    onClick: () -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    style: Int = RC.PLAY_BTN_STYLE_SIMPLE
) {
    when (style) {
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
    artwork: Uri? = null,
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


/**
 * A Composable that hosts a [SurfaceView] and attaches it to the given [VideoProvider].
 *
 * This Composable ensures that the [SurfaceView] is correctly bound to the player's video output,
 * while also managing reattachment if a different [VideoProvider] is passed on recomposition.
 *
 * ### Key behaviors:
 * - Creates and manages a [SurfaceView] inside Compose using [AndroidView].
 * - Keeps the screen awake during playback if [keepScreenOn] is `true`.
 * - Handles safe switching: detaches the [SurfaceView] from an old [VideoProvider] before attaching it to a new one.
 * - Automatically clears the video surface when the [SurfaceView] is released (e.g., disposed).
 *
 * @param provider The [VideoProvider] representing the player instance that will receive the video surface.
 * @param modifier [Modifier] for layout adjustments (e.g., size, padding).
 * @param keepScreenOn Whether to keep the device screen awake while this surface is active.
 */
@Composable
fun VideoSurface(
    provider: VideoProvider,
    modifier: Modifier = Modifier,
    typeSurfaceView: Boolean = true,
    keepScreenOn: Boolean = false
) {
    var view by remember { mutableStateOf<View?>(null) }

    AndroidView(
        modifier = modifier,
        // Factory: creates a new SurfaceView when this Composable first enters the composition.
        factory = { if (typeSurfaceView) SurfaceView(it) else TextureView(it) },
        onReset = {},
        update = {
            Log.d(TAG, "VideoSurface: updating")
            // Prevents the screen from turning off during video playback.
            it.keepScreenOn = keepScreenOn
            view = it
        },
    )

    view?.let { view ->
        LaunchedEffect(view, provider.value) {
            val attached = VideoProvider(view.tag)

            if (provider.isEmpty) {
                // Handle null provider safely
                withContext(Dispatchers.Main) {
                    if (attached.canSetVideoSurface)
                        attached.clearVideoSurfaceView(view)
                    view.tag = null
                }
                return@LaunchedEffect
            }

            if (attached.value != provider.value) {
                if (attached.canSetVideoSurface)
                    attached.clearVideoSurfaceView(view)

                if (provider.canSetVideoSurface) {
                    provider.setVideoSurfaceView(view)
                    view.tag = provider.value
                }
            }
        }
    }
}

@Composable
fun TimeBar(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = AppTheme.colors.accent,
    enabled: Boolean = true,
    style: Int = RC.TIME_BAR_STYLE_REGULAR
) {
    when {
        value.isNaN() -> LinearProgressIndicator(
            modifier = modifier,
            color = accent,
            strokeCap = StrokeCap.Round,
        )

        style == RC.TIME_BAR_STYLE_REGULAR -> Slider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            modifier = modifier,
            enabled = enabled,
            colors = SliderDefaults.colors(activeTrackColor = accent, thumbColor = accent)
        )

        style == RC.TIME_BAR_STYLE_WAVY -> ir.mahozad.multiplatform.wavyslider.material.WavySlider(
            value = value,
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            modifier = modifier,
            // idp because 0 dp is not supported.
            waveLength = 20.dp,
            waveHeight = 7.dp,
            incremental = true,
            colors = SliderDefaults.colors(activeTrackColor = accent, thumbColor = accent)
        )
    }

}

/**
 * A simple countdown timer Composable.
 *
 * This Composable takes an initial duration in milliseconds and provides a [LongState]
 * that represents the remaining time. The timer counts down every second.
 *
 * The timer will stop when the remaining time reaches zero or less.
 * If the initial `mills` value changes, the timer will restart with the new duration.
 *
 * @param mills The initial duration for the timer in milliseconds.
 * @return A [LongState] holding the current remaining time in milliseconds.
 *         This state will be updated every second as the timer counts down.
 */
@Composable
inline fun timer(mills: Long): LongState {
    // Remember the state of the timer. Initialize with the provided `mills`.
    val state = remember { mutableLongStateOf(mills) }
    // Launch a side-effect that depends on `mills`.
    // If `mills` changes, the existing coroutine is cancelled and a new one starts.
    LaunchedEffect(mills) {
        // Loop as long as there is time remaining.
        while (state.longValue > 0) {
            delay(1000) // Wait for 1 second.
            state.longValue -= 1000 // Decrement the remaining time by 1000 milliseconds.
        }
    }
    return state // Return the state object that holds the current time.
}
/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 19-01-2024.
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

@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterialApi::class)

package com.prime.media.old.console

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.media.AudioManager
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Build
import android.text.format.DateUtils.formatElapsedTime
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.IntDef
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.LocalContentColor
import androidx.compose.material.SliderDefaults
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ClosedCaption
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.FitScreen
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowLeft
import androidx.compose.material.icons.outlined.KeyboardDoubleArrowRight
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Queue
import androidx.compose.material.icons.outlined.ScreenLockLandscape
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Speaker
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.coerceIn
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.isSpecified
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.takeOrElse
import androidx.compose.ui.unit.times
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.common.Player
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.google.accompanist.adaptive.HorizontalTwoPaneStrategy
import com.google.accompanist.adaptive.TwoPane
import com.google.accompanist.adaptive.TwoPaneStrategy
import com.google.accompanist.adaptive.VerticalTwoPaneStrategy
import com.google.accompanist.adaptive.calculateDisplayFeatures
import com.prime.media.BuildConfig
import com.prime.media.R
import com.prime.media.common.LocalSystemFacade
import com.prime.media.common.SystemFacade
import com.prime.media.common.brightness
import com.prime.media.common.preference
import com.prime.media.common.volume
import com.prime.media.old.common.AnimatedIconButton
import com.prime.media.old.common.Artwork
import com.prime.media.old.common.LocalNavController
import com.prime.media.old.common.LottieAnimButton
import com.prime.media.old.common.LottieAnimation
import com.prime.media.old.common.PlayerView
import com.prime.media.old.common.marque
import com.prime.media.old.core.playback.artworkUri
import com.prime.media.old.core.playback.subtitle
import com.prime.media.old.core.playback.title
import com.prime.media.old.effects.AudioFx
import com.prime.media.personalize.RoutePersonalize
import com.prime.media.settings.DancingScriptFontFamily
import com.prime.media.settings.Settings
import com.primex.core.ImageBrush
import com.primex.core.OrientRed
import com.primex.core.SignalWhite
import com.primex.core.findActivity
import com.primex.core.foreground
import com.primex.core.plus
import com.primex.core.thenIf
import com.primex.core.visualEffect
import com.primex.material2.Divider
import com.primex.material2.DropDownMenuItem
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.OutlinedButton2
import com.primex.material2.menu.DropDownMenu2
import com.primex.material2.neumorphic.NeumorphicButton
import com.primex.material2.neumorphic.NeumorphicButtonDefaults
import com.zs.core_ui.Anim
import com.zs.core_ui.AppTheme
import com.zs.core_ui.Colors
import com.zs.core_ui.ContentElevation
import com.zs.core_ui.ContentPadding
import com.zs.core_ui.Indication
import com.zs.core_ui.LocalWindowSize
import com.zs.core_ui.MediumDurationMills
import com.zs.core_ui.Range
import com.zs.core_ui.WindowSize
import com.zs.core_ui.coil.RsBlurTransformation
import com.zs.core_ui.lottieAnimationPainter
import com.zs.core_ui.sharedElement
import com.zs.core_ui.toast.Toast
import ir.mahozad.multiplatform.wavyslider.material.WavySlider
import kotlin.math.roundToInt

private const val TAG = "ConsoleView"

private fun SystemFacade.launchEqualizer(id: Int) {
    if (id == AudioEffect.ERROR_BAD_VALUE)
        return showToast(R.string.msg_unknown_error)
    val intent = Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
        putExtra(AudioEffect.EXTRA_PACKAGE_NAME, BuildConfig.APPLICATION_ID)
        putExtra(AudioEffect.EXTRA_AUDIO_SESSION, id)
        putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
    }
    val res = runCatching { launch(intent) }
    if (!res.isFailure)
        return
    showToast(
        message = R.string.msg_3rd_party_equalizer_not_found,
        accent = Color.OrientRed,
        priority = Toast.PRIORITY_LOW
    )
}

/**
 * Returns true if the system bars are required to be light-themed, false otherwise.
 * @see WindowInsetsControllerCompat.isAppearanceLightStatusBars
 */
private inline val Colors.isAppearanceLightSystemBars
    @Composable inline get() = isLight

/** Default background style. */
private const val DEFAULT_BACKGROUND = 0

/** Background style featuring artwork. */
private const val BACKGROUND_ARTWORK = 1

/** Background style with a gradient. */
private const val BACKGROUND_GRADIENT = 2

/** Background style black color when video*/
private const val BACKGROUND_VIDEO_SURFACE = 3

/**
 * Annotation to indicate valid values for Background styles.
 *
 * Supported values:
 *  - [DEFAULT_BACKGROUND]
 *  - [BACKGROUND_ARTWORK]
 *  - [BACKGROUND_GRADIENT]
 */
@IntDef(DEFAULT_BACKGROUND, BACKGROUND_ARTWORK, BACKGROUND_GRADIENT, BACKGROUND_VIDEO_SURFACE)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
private annotation class Background

/** Simple seekbar style. */
private const val SEEKBAR_STYLE_SIMPLE = 0

/** Wavy seekbar style. */
private const val SEEKBAR_STYLE_WAVY = 1

/**
 * Annotation to indicate valid values for Seekbar styles.
 *
 * Supported values:
 *  - [SEEKBAR_STYLE_SIMPLE]
 *  - [SEEKBAR_STYLE_WAVY]
 */
@IntDef(SEEKBAR_STYLE_SIMPLE, SEEKBAR_STYLE_WAVY)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
private annotation class Seekbar

/** Simple play button style. */
private const val PLAY_BUTTON_STYLE_SIMPLE = 0

/** Neumorphic play button style. */
private const val PLAY_BUTTON_STYLE_NEUMORPHIC = 1

private const val PLAY_BUTTON_STYLE_ROUNDED = 2

/**
 * Annotation to restrict valid values for play button styles.
 *
 * Supported values:
 *  - [PLAY_BUTTON_STYLE_SIMPLE]
 *  - [PLAY_BUTTON_STYLE_NEUMORPHIC]
 */
@IntDef(PLAY_BUTTON_STYLE_SIMPLE, PLAY_BUTTON_STYLE_NEUMORPHIC, PLAY_BUTTON_STYLE_ROUNDED)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
private annotation class PlayButton

/**
 * Constants representing different types of requests that a component can make to its parent.
 */
private const val REQUEST_SHOW_PLAYING_QUEUE = 1

/** Request to show the properties dialog. */
private const val REQUEST_SHOW_PROPERTIES = 2

/** Request to handle a back press event. */
private const val REQUEST_HANDLE_BACK_PRESS = 3


/** Request to show light */
private const val REQUEST_REQUIRES_LIGHT_SYSTEM_BARS = 4

/** @see */
private const val REQUEST_REQUIRES_DARK_SYSTEM_BARS = 5

/**
 * Request to toggle the [Console] visibility between [Console.VISIBILITY_LOCKED] and [Console.VISIBILITY_VISIBLE]
 */
private const val REQUEST_TOOGLE_LOCK = 6

/**
 * @see REQUEST_TOOGLE_LOCK
 */
private const val REQUEST_TOGGLE_VISIBILITY = 7


/**
 * Toggles [toggleRotationLock] between locked and unlocked.
 */
private const val REQUEST_TOGGLE_ROTATION_LOCK = 8

/**
 * Annotation to restrict valid values for request types.
 *
 * This annotation also serves as a signal to the parent component that handles
 * different actions based on the request type:
 *  - Showing/hiding dialogs (toggling visibility if the same request is passed again)
 *  - Handling back button presses
 *
 * Supported values:
 *  - [REQUEST_HANDLE_BACK_PRESS]
 *  - [REQUEST_SHOW_PLAYING_QUEUE]
 *  - [REQUEST_SHOW_PROPERTIES]
 *  - [REQUEST_REQUIRES_LIGHT_SYSTEM_BARS]
 *  - [REQUEST_REQUIRES_DARK_SYSTEM_BARS]
 */
@IntDef(
    REQUEST_HANDLE_BACK_PRESS,
    REQUEST_SHOW_PLAYING_QUEUE,
    REQUEST_SHOW_PROPERTIES,
    REQUEST_REQUIRES_LIGHT_SYSTEM_BARS,
    REQUEST_REQUIRES_DARK_SYSTEM_BARS,
    REQUEST_TOOGLE_LOCK,
    REQUEST_TOGGLE_VISIBILITY,
    REQUEST_TOGGLE_ROTATION_LOCK
)
@Target(AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.PROPERTY, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
private annotation class Request

/**
 * Extension property for [WindowInsets] that provides a [DpRect] representation of the insets,
 * ensuring layout compatibility across different screen densities and layout directions.
 *
 * @return A [DpRect] containing the left, top, right, and bottom insets in density-independent pixels (dp).
 */
private val WindowInsets.asDpRect: DpRect
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
 * Checks if this [DpSize] is large enough to contain another [DpSize].
 *
 * This extension function is useful for determining whether a given size can
 * accommodate another size within its bounds. For example, it can be used to
 * check if a layout can fit a child view or if an image can be displayed
 * within a certain area.
 *
 * @param other The other [DpSize] to check against.
 * @return `true` if this [DpSize] is equal to or larger than the other [DpSize]
 *         in both width and height, meaning it can fully contain the other size.
 *         Returns `false` otherwise.
 */
private fun DpSize.contains(other: DpSize): Boolean {
    // Ensure that both the width and height of this DpSize are greater than
    // or equal to the corresponding dimensions of the other DpSize to indicate
    // that it can fully contain the other size within its bounds.
    return this.width >= other.width && this.height >= other.height
}

/**
 * Extensions for the [Console] class, providing convenient access to media metadata.
 */
private inline val Console.title: String?
    get() = current?.title?.toString()

/**
 * Retrieves the subtitle of the currently playing media item, if available.
 */
private inline val Console.subtitle: String?
    get() = current?.subtitle?.toString()

/**
 * Retrieves the artwork URI of the currently playing media item, providing
 * convenient access for UI elements and data management.
 */
private inline val Console.artworkUri: Uri?
    get() = current?.artworkUri

/**
 * Utility Fun that toggles [Console.resizeMode]
 */
@Deprecated("just remove this and replace with onsite cycling.")
private fun Console.cycleResizeMode() {
    resizeMode =
        if (resizeMode == Console.RESIZE_MODE_FILL) Console.RESIZE_MORE_FIT else Console.RESIZE_MODE_FILL
}

/**
 * Shows or hides the system bars, such as the status bar and the navigation bar.
 * @param enable A boolean value that indicates whether to show or hide the system bars.
 * If true, the system bars are hidden. If false, the system bars are shown.
 */
// Explore implications of toggling immersive mode when the user has hidden system bars.
// TODO: Assess potential outcomes and address any issues that may arise.
private fun WindowInsetsControllerCompat.immersiveMode(enable: Boolean) =
    if (enable) hide(WindowInsetsCompat.Type.systemBars()) else show(WindowInsetsCompat.Type.systemBars())

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
private fun Activity.toggleRotationLock(): Boolean {
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
private inline val Activity.isOrientationLocked
    get() = requestedOrientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED

/**
 * Ensures that the console is always visible. If the current visibility is set to
 * [Console.VISIBILITY_LOCKED] the visibility remains unchanged.
 *
 * Sets the console visibility to [Console.VISIBILITY_ALWAYS] if the given parameter is true and the current visibility is not [Console.VISIBILITY_LOCKED] or [Console.VISIBILITY_ALWAYS].
 * If the given parameter is false and the current visibility is [Console.VISIBILITY_ALWAYS], it sets the visibility to [Console.VISIBILITY_VISIBLE].
 * This function has no effect if the console is a video console, as it is always visible in that case.
 * @param enabled A boolean value indicating whether to ensure the console is always visible or not.
 */
private fun Console.ensureAlwaysVisible(enabled: Boolean) {
    visibility = when {
        visibility == Console.VISIBILITY_LOCKED -> return
        !isVideo -> return // because in this case it will always be visible.
        visibility == Console.VISIBILITY_ALWAYS && !enabled -> Console.VISIBILITY_VISIBLE
        else -> Console.VISIBILITY_ALWAYS
    }
}

/**
 * Composable function representing a SeekBar.
 *
 * @param value The current value of the SeekBar between 0 and 1, [Float.NaN] indicates waiting.
 * @param onValueChange Callback triggered when the SeekBar value changes.
 * @param modifier Optional Modifier for additional styling.
 * @param color The color of the SeekBar, default is the primary color from AppTheme.
 * @param style The style of the SeekBar, either [SEEKBAR_STYLE_SIMPLE] or [SEEKBAR_STYLE_WAVY].
 */
@Composable
@NonRestartableComposable
private fun SeekBar(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = AppTheme.colors.accent,
    @Seekbar style: Int = SEEKBAR_STYLE_SIMPLE,
) {
    // if the value is Float.NaN; show a non ending progress bar.
    when (value.isNaN()) {
        true -> LinearProgressIndicator(
            modifier = modifier,
            color = accent,
            strokeCap = StrokeCap.Round,
        )

        else -> WavySlider(
            value = value,
            onValueChange = onValueChange,
            modifier = modifier,
            // idp because 0 dp is not supported.
            waveLength = if (style == SEEKBAR_STYLE_SIMPLE) 0.dp else 20.dp,
            waveHeight = if (style == SEEKBAR_STYLE_SIMPLE) 0.dp else 7.dp,
            incremental = true,
            colors = SliderDefaults.colors(activeTrackColor = accent, thumbColor = accent)
        )
    }
}

private val RoundedCornerShape_24 = RoundedCornerShape(24)

/**
 * Composable function representing a PlayButton with different styles.
 *
 * @param onClick Callback triggered when the PlayButton is clicked.
 * @param isPlaying Whether the media is currently playing.
 * @param modifier Optional Modifier for additional styling.
 * @param style The style of the PlayButton, either [PLAY_BUTTON_STYLE_SIMPLE], [PLAY_BUTTON_STYLE_NEUMORPHIC],
 * or [PLAY_BUTTON_STYLE_FAB].
 */
// TODO: Ensure each play button has a distinct style of surface only.
@Composable
@NonRestartableComposable
private fun PlayButton(
    onClick: () -> Unit,
    isPlaying: Boolean,
    modifier: Modifier = Modifier,
    @PlayButton style: Int = PLAY_BUTTON_STYLE_NEUMORPHIC,
) {
    when (style) {
        // Use the simple version of the play button.
        // The tint in this case is derived from the LocalContentColor.
        PLAY_BUTTON_STYLE_SIMPLE ->
            IconButton(
                modifier = modifier.scale(1.25f),
                onClick = onClick,
                painter = lottieAnimationPainter(
                    R.raw.lt_play_pause,
                    progressRange = 0.0f..0.29f,
                    atEnd = !isPlaying,
                    duration = Anim.MediumDurationMills,
                    easing = LinearEasing,
                    dynamicProperties = rememberLottieDynamicProperties(
                        rememberLottieDynamicProperty(
                            property = LottieProperty.STROKE_COLOR,
                            Color.SignalWhite.toArgb(),
                            "**"
                        )
                    )
                )
            )

        PLAY_BUTTON_STYLE_NEUMORPHIC -> NeumorphicButton(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape_24,
            colors = NeumorphicButtonDefaults.neumorphicButtonColors(
                lightShadowColor = AppTheme.colors.lightShadowColor,
                darkShadowColor = AppTheme.colors.darkShadowColor
            ),
            border = if (!AppTheme.colors.isLight)
                BorderStroke(1.dp, AppTheme.colors.onBackground.copy(0.06f))
            else null,
            content = {
                val accent = AppTheme.colors.accent
                LottieAnimation(
                    id = R.raw.lt_play_pause,
                    atEnd = !isPlaying,
                    scale = 1.5f,
                    progressRange = 0.0f..0.29f,
                    duration = Anim.MediumDurationMills,
                    easing = LinearEasing,
                    dynamicProperties = rememberLottieDynamicProperties(
                        rememberLottieDynamicProperty(
                            property = LottieProperty.STROKE_COLOR,
                            accent.toArgb(),
                            "**"
                        )
                    )
                )
            }
        )

        PLAY_BUTTON_STYLE_ROUNDED -> Surface(
            onClick = onClick,
            modifier = modifier,
            shape = RoundedCornerShape(25),
            color = Color.Transparent,
            border = BorderStroke(
                1.dp,
                AppTheme.colors.onBackground.copy(if (!AppTheme.colors.isLight) ContentAlpha.Indication else ContentAlpha.medium)
            ),
            content = {
                LottieAnimation(
                    id = R.raw.lt_play_pause,
                    atEnd = !isPlaying,
                    scale = 0.8f,
                    progressRange = 0.0f..0.29f,
                    duration = Anim.MediumDurationMills,
                    easing = LinearEasing,
                    dynamicProperties = rememberLottieDynamicProperties(
                        rememberLottieDynamicProperty(
                            property = LottieProperty.STROKE_COLOR,
                            AppTheme.colors.onBackground.toArgb(),
                            "**"
                        )
                    )
                )
            }
        )
        // handle others
        else -> TODO("$style Not Implemented Yet!")
    }
}

/**
 * No-op pointer input [Modifier] that discards pointer events.
 */
private val NoOpPointerInput = Modifier.pointerInput(Unit) {}

/**
 * Defines a row of controls, consisting of 5 buttons with a play button at the center.
 * The color of icons is determined by [LocalContentColor].
 *
 * @param state The current state of the console.
 * @param style The style of the play button, default is [PLAY_BUTTON_STYLE_NEUMORPHIC].
 */
@Composable
private inline fun Controls(
    state: Console,
    modifier: Modifier = Modifier,
    @PlayButton style: Int = PLAY_BUTTON_STYLE_NEUMORPHIC
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.then(NoOpPointerInput)
) {
    // Shuffle | Option 5
    val shuffle = state.shuffle
    val facade = LocalSystemFacade.current
    LottieAnimButton(
        id = R.raw.lt_shuffle_on_off,
        onClick = { state.toggleShuffle(); facade.initiateReviewFlow(); },
        atEnd = !shuffle,
        progressRange = 0f..0.8f,
        scale = 1.5f
    )

    var enabled = !state.isFirst
    val onColor = LocalContentColor.current
    IconButton(
        onClick = { state.skipToPrev(); facade.initiateReviewFlow() },
        painter = rememberVectorPainter(image = Icons.Outlined.KeyboardDoubleArrowLeft),
        contentDescription = null,
        enabled = enabled,
        tint = onColor.copy(if (enabled) ContentAlpha.high else ContentAlpha.medium)
    )

    // play_button
    PlayButton(
        onClick = { state.togglePlay(); facade.initiateReviewFlow() },
        isPlaying = state.isPlaying,
        modifier = Modifier
            .padding(horizontal = ContentPadding.medium)
            .size(60.dp),
        style = style
    )

    // Skip to Next
    enabled = !state.isLast
    IconButton(
        onClick = { state.skipToNext(); facade.initiateReviewFlow() },
        painter = rememberVectorPainter(image = Icons.Outlined.KeyboardDoubleArrowRight),
        contentDescription = null,
        enabled = enabled,
        tint = onColor.copy(if (enabled) ContentAlpha.high else ContentAlpha.medium)
    )

    // CycleRepeatMode | Option 6
    val mode = state.repeatMode
    AnimatedIconButton(
        id = R.drawable.avd_repeat_more_one_all,
        onClick = { state.cycleRepeatMode(); facade.initiateReviewFlow(); },
        atEnd = mode == Player.REPEAT_MODE_ALL,
        tint = onColor.copy(if (mode == Player.REPEAT_MODE_OFF) ContentAlpha.disabled else ContentAlpha.high)
    )
}

/**
 * Composable function displaying a progress bar along with two icon buttons.
 *
 * @param style The style of the progress bar, default is [SEEKBAR_STYLE_WAVY].
 * @param accent The accent color for the progress bar, default is the primary color from AppTheme.
 * @param state The current state of the console.
 */
@Composable
private fun TimeBar(
    state: Console,
    modifier: Modifier = Modifier,
    onRequest: (request: @Request Int) -> Boolean,
    @Seekbar style: Int = SEEKBAR_STYLE_WAVY,
    accent: Color = AppTheme.colors.accent,
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.then(NoOpPointerInput)
) {
    // resize_mode
    val resizeMode = state.resizeMode
    IconButton(
        imageVector = if (resizeMode == Console.RESIZE_MODE_FILL) Icons.Outlined.Fullscreen else Icons.Outlined.FitScreen,
        onClick = state::cycleResizeMode,
        enabled = state.isVideo
    )

    SeekBar(
        value = if (state.isSeekable) state.progress else Float.NaN,
        style = style,
        onValueChange = { state.progress = it },
        accent = accent,
        modifier = Modifier.weight(1f)
    )

    val context = LocalContext.current
    IconButton(
        imageVector = when {
            LocalInspectionMode.current || context.findActivity().isOrientationLocked -> Icons.Outlined.ScreenLockLandscape
            else -> Icons.Outlined.ScreenRotation
        },
        onClick = { onRequest(REQUEST_TOGGLE_ROTATION_LOCK) }
    )
}


/**
 * Composable function representing additional options available in a popup menu.
 * The 'More' menu includes the icon and the necessary logic for displaying different menus.
 *
 * @param state The current state of the console.
 * @param modifier Optional Modifier for additional styling.
 */
@Composable
private fun More(
    state: Console,
    onRequest: (request: @Request Int) -> Boolean,
    modifier: Modifier = Modifier
) {
    // If the 'More' is compact, it includes options as well.
    // Represents the state of all menus in this composable:
    // - 0: Off state.
    // - 1: Main menu
    // - 2: Audio menu
    // - 3: Subtitle menu
    var expanded by remember { mutableIntStateOf(0) }
    // Main Icon
    IconButton(
        onClick = { expanded = 1; state.ensureAlwaysVisible(true) },
        modifier = modifier,
        content = {
            val onColor = LocalContentColor.current
            // The icon of this item.
            Icon(
                imageVector = Icons.Outlined.MoreHoriz,
                contentDescription = null,
                tint = onColor
            )

            // Audio Menu
            val colors = AppTheme.colors
            DropDownMenu2(
                expanded = expanded == 2,
                onDismissRequest = { expanded = 1 },
                shape = AppTheme.shapes.compact,
                elevation = if (colors.isLight) 4.dp else 2.dp,
                border = if (colors.isLight) null else BorderStroke(
                    1.dp,
                    colors.onBackground.copy(ContentAlpha.Divider)
                ),

                content = {
                    DropDownMenuItem(
                        title = "Auto",
                        onClick = { state.currAudioTrack = null; expanded = 1 }
                    )
                    // Others
                    state.audios.forEach { track ->
                        DropDownMenuItem(
                            title = track.name,
                            onClick = { state.currAudioTrack = track; expanded = 1 })
                    }
                }
            )

            // Subtitle Menu
            DropDownMenu2(
                expanded = expanded == 3,
                onDismissRequest = { expanded = 1; },
                shape = AppTheme.shapes.compact,
                elevation = if (colors.isLight) 4.dp else 2.dp,
                border = if (colors.isLight) null else BorderStroke(
                    1.dp,
                    colors.onBackground.copy(ContentAlpha.Divider)
                ),
                // TODO - Add one option for enabling/adding custom subtitle track.
                content = {
                    DropDownMenuItem(
                        title = "Off",
                        onClick = { state.currSubtitleTrack = null; expanded = 1 }
                    )
                    // Others
                    state.subtiles.forEach { track ->
                        DropDownMenuItem(
                            title = track.name,
                            onClick = { state.currSubtitleTrack = track; expanded = 1 })
                    }
                }
            )

            // Main Menu
            DropDownMenu2(
                expanded = expanded == 1,
                onDismissRequest = { expanded = 0; state.ensureAlwaysVisible(false) },
                shape = AppTheme.shapes.compact,
                content = {
                    // A top row; showing common options
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        content = {
                            // Equalizer
                            val controller = LocalNavController.current
                            // FixMe: State is not required here. implement to get value without state.
                            val useBuiltIn by preference(key = Settings.USE_IN_BUILT_AUDIO_FX)
                            val facade = LocalSystemFacade.current
                            IconButton(
                                imageVector = Icons.Outlined.Tune,
                                onClick = {
                                    if (useBuiltIn)
                                        controller.navigate(AudioFx.route)
                                    else
                                        facade.launchEqualizer(state.audioSessionId)
                                    expanded = 0
                                }
                            )

                            // Control Centre
                            IconButton(
                                imageVector = Icons.Outlined.ColorLens,
                                onClick = { controller.navigate(RoutePersonalize()); expanded = 0 }
                            )
                        }
                    )
                    //  A divider between Row and Other items.
                    Divider()
                    // Audio
                    val isVideo = state.isVideo
                    DropDownMenuItem(
                        title = "Audio",
                        subtitle = state.currAudioTrack?.name ?: "Auto",
                        onClick = { expanded = 2 },
                        icon = rememberVectorPainter(image = Icons.Outlined.Speaker),
                        enabled = isVideo
                    )

                    // Subtitle
                    DropDownMenuItem(
                        title = "Subtitle",
                        subtitle = state.currSubtitleTrack?.name ?: "Off",
                        onClick = { expanded = 3 },
                        icon = rememberVectorPainter(Icons.Outlined.ClosedCaption),
                        enabled = isVideo
                    )

                    // Lock
                    /*DropDownMenuItem(
                        title = "Lock",
                        subtitle = "Lock/Hide controller",
                        icon = rememberVectorPainter(Icons.Outlined.Lock),
                        enabled = isVideo,
                        onClick = {
                            // handle lock toggle request
                            onRequest(REQUEST_TOOGLE_LOCK)
                            expanded = 0
                        }
                    )*/

                    // Share
                    // Properties
                }
            )
        }
    )
}

/**
 * Composable function representing a row of options, containing 5 buttons.
 * The tint of icons is derived from [LocalContentColor].
 *
 * @param state The current state of the console.
 */
@Composable
private fun Options(
    state: Console,
    onRequest: (request: @Request Int) -> Boolean,
    modifier: Modifier = Modifier,
) = Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier.then(NoOpPointerInput)
) {
    // Control the state of components using a single state variable.
    // TODO: Investigate if using a single state for controlling multiple components causes side effects.
    // The value '0' represents all components turned off.
    // The value '1' represents the queue component.
    // The value '2' represents the speed controller component.
    // The value '3' represents the sleep timer component.

    // Represents the current component that is in expanded state.
    var expanded by remember { mutableIntStateOf(0) }
    // show playing queue
    PlayingQueue(
        state = state,
        expanded = expanded == 1,
        // restore the visibility back to normal.
        onDismissRequest = { expanded = 0; state.ensureAlwaysVisible(false) }
    )

    // Lock Button
    if (state.isVideo)
        IconButton(
            imageVector = Icons.Outlined.Lock,
            onClick = { onRequest(REQUEST_TOOGLE_LOCK) },
        )

    // Speed Controller.
    PlaybackSpeed(
        expanded = expanded == 2,
        value = state.playbackSpeed,
        onValueChange = {
            if (it != -1f)
            // If the value is -1f, it means a dismiss request
                state.playbackSpeed = it
            state.ensureAlwaysVisible(false)
            expanded = 0
        }
    )

    // Sleep Timer.
    SleepTimer(
        expanded = expanded == 3,
        onValueChange = {
            if (it != -2L)
                state.sleepAfterMills = it
            expanded = 0
            state.ensureAlwaysVisible(false)
        }
    )

    // Queue
    IconButton(
        painter = rememberVectorPainter(image = Icons.Outlined.Queue),
        tint = LocalContentColor.current,
        onClick = {
            // check if parent might handle the request of showing the dialog.
            if (onRequest(REQUEST_SHOW_PLAYING_QUEUE))
                return@IconButton
            expanded = 1; state.ensureAlwaysVisible(true)
        },
    )

    // Speed Controller.
    IconButton(
        onClick = { expanded = 2; state.ensureAlwaysVisible(true) },
        painter = rememberVectorPainter(image = Icons.Outlined.Speed),
        tint = LocalContentColor.current
    )

    // SleepAfter.
    IconButton(
        onClick = { expanded = 3; state.ensureAlwaysVisible(true) },
        content = {
            val mills = state.sleepAfterMills
            Crossfade(targetState = mills != -1L, label = "SleepAfter CrossFade") { show ->
                when (show) {
                    true -> Label(
                        text = formatElapsedTime(mills / 1000L),
                        style = AppTheme.typography.caption,
                        fontWeight = FontWeight.Bold,
                        color = AppTheme.colors.accent
                    )

                    else -> Icon(
                        imageVector = Icons.Outlined.Timer,
                        contentDescription = null,
                        tint = LocalContentColor.current
                    )
                }
            }
        },
    )

    // Favourite
    val favourite = state.favourite
    val facade = LocalSystemFacade.current
    LottieAnimButton(
        id = R.raw.lt_twitter_heart_filled_unfilled,
        onClick = { state.toggleFav(); facade.initiateReviewFlow() },
        scale = 3.5f,
        progressRange = 0.13f..0.95f,
        duration = 800,
        atEnd = !favourite
    )

    // More
    More(state = state, onRequest)
}


/**
 * Composable function that renders the background of the console, providing a customizable background and content area.
 *
 * This function handles different background styles, and animations for smooth transitions.
 *
 * @param style The desired background style. Use one of the values from the [Background] annotation.
 * @param modifier [Modifier] to apply to the background element.
 */
@Composable
private fun Background(
    @Background style: Int,
    state: Console,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    when (style) {
        // Use black color for video background and animate changes smoothly
        BACKGROUND_VIDEO_SURFACE -> {
            // Animate color changes for visual transitions
            val color by animateColorAsState(
                targetValue = if (style == BACKGROUND_VIDEO_SURFACE) Color.Black else AppTheme.colors.background,
                label = "Background color Change."
            )
            // Create the background with the determined color
            Spacer(modifier = modifier.background(color))
        }

        DEFAULT_BACKGROUND if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) -> {
            val transformation = remember {
                RsBlurTransformation(ctx, 25f, 2.1f)
            }
            AsyncImage(
                model = ImageRequest.Builder(ctx)
                    .data(state.artworkUri)
                    .transformations(transformation).build(),
                contentDescription = null,
                modifier = modifier
                    .foreground(AppTheme.colors.background.copy(if (AppTheme.colors.isLight) 0.85f else 0.92f))
                    .visualEffect(ImageBrush.NoiseBrush, overlay = true),
                contentScale = ContentScale.Crop
            )
        }

        DEFAULT_BACKGROUND -> {
            AsyncImage(
                model = state.artworkUri,
                contentDescription = null,
                modifier = modifier
                    .blur(95.dp)
                    .foreground(AppTheme.colors.background.copy(if (AppTheme.colors.isLight) 0.85f else 0.92f))
                    .visualEffect(ImageBrush.NoiseBrush, overlay = true),
                contentScale = ContentScale.Crop
            )
        }

        else -> TODO("Not Implemented yet background style: $style")
    }
}


@Composable
private fun Message(
    message: CharSequence?,
    modifier: Modifier = Modifier
) {
    // Early return when message is empty
    if (message == null) return
    Label(
        text = message,
        modifier = modifier
            .shadow(ContentElevation.medium, AppTheme.shapes.compact, true)
            .background(Color.Black)
            .padding(horizontal = ContentPadding.normal, vertical = ContentPadding.medium)
    )
}

/**
 * Composable function that renders the main content of the console, managing layout, media
 * playback, and user interactions.
 *
 * @param state The current state of the console, providing access to playback information, UI
 *              configuration, and content metadata.
 * @param constraints A [Constraints] that defines the layout constraints for the content,
 *                    ensuring proper positioning and adaptability.
 * @param onRequest A callback function that handles requests from the main content, such as showing
 *                  dialogs or handling back button presses.
 * @param modifier [Modifier] to apply to the main content element.
 *
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun MainContent(
    state: Console,
    constraints: Constraints,
    onRequest: (request: @Request Int) -> Boolean,
    modifier: Modifier = Modifier
) = ConstraintLayout(
    constraintSet = constraints.value,
    modifier = modifier,
    //TODO - Setting this true causes crash; Surely in future.
    animateChanges = false
) {
    // Get the current navigation controller
    val navController = LocalNavController.current
    // FixMe: The PlayerView, rendering on a separate SurfaceView, interferes with animations.
    // This workaround flags the removal of PlayerView beforehand to prevent animation interference.
    // A mutable state variable that indicates whether to remove the PlayerView or not
    var removePlayerView by remember { mutableStateOf(false) }
    val context = LocalContext.current
    // Define the navigation action when the back button is pressed
    val onNavigateBack: () -> Unit = onNavigateBack@{
        // Early return if the request was handled by the parent
        if (onRequest(REQUEST_HANDLE_BACK_PRESS))
            return@onNavigateBack
        // Check if the activity has orientation lock enabled - unlock it.
        if (context.findActivity().isOrientationLocked)
            context.findActivity().toggleRotationLock()
        else {
            // Flag to remove the PlayerView and navigate up in the navigation controller
            removePlayerView = true
            navController.navigateUp()
        }
    }
    // Set up BackHandler with the defined onNavigateBack action
    BackHandler(onBack = onNavigateBack)
    // Window Style
    // Each of these depend on background
    // The background color of the window
    val background: Int // The accent color of the window.
    val accent: Color   // The content color of the window.
    val contentColor: Color // The content color over the background.
    val isAppearanceLightSystemBars: Boolean // Whether the status bar and navigation bar should use light colors or not
    // Check if the console is playing a video or not
    val isVideo = state.isVideo
    when (isVideo) {
        true -> {
            background = BACKGROUND_VIDEO_SURFACE
            accent = Color.SignalWhite
            contentColor = Color.SignalWhite
            isAppearanceLightSystemBars = false
        }

        else -> {
            background = DEFAULT_BACKGROUND
            accent = AppTheme.colors.accent
            contentColor = AppTheme.colors.onBackground
            isAppearanceLightSystemBars = AppTheme.colors.isAppearanceLightSystemBars
        }
    }
    // Change the appearance of System bars
    // Here I have decided the status bar will change only colors for this window.
    SideEffect {
        // Maybe use
        onRequest(if (isAppearanceLightSystemBars) REQUEST_REQUIRES_LIGHT_SYSTEM_BARS else REQUEST_REQUIRES_DARK_SYSTEM_BARS)
    }

    // Why use CompositionLocal?
    // This is because the children of this layout's components like Options etc. depend on this
    CompositionLocalProvider(LocalContentColor provides contentColor) {
        // The Background of this component.
        Background(
            style = background,
            state = state,
            modifier = Modifier.layoutId(Constraints.ID_BACKGROUND)
        )

        // VideoSurface
        val isInInspectionMode = LocalInspectionMode.current
        val facade = LocalSystemFacade.current
        if (isVideo && !removePlayerView && !isInInspectionMode)
            PlayerView(
                player = state.player,
                resizeMode = state.resizeMode,
                modifier = Modifier
                    .layoutId(Constraints.ID_VIDEO_SURFACE)
                    // TODO - Find Proper Place to store this logic.
                    // TODO - Add support for other gestures like seek
                    .pointerInput("tap_gesture") {
                        var lastTapTime = -1L
                        var tapCount = 1 // Track double tap timing and count
                        detectTapGestures(
                            // Reset onTap
                            onTap = {
                                tapCount = 1; lastTapTime = -1L; onRequest(
                                REQUEST_TOGGLE_VISIBILITY
                            )
                            },
                            onLongPress = {
                                if (state.visibility == Console.VISIBILITY_LOCKED) onRequest(
                                    REQUEST_TOOGLE_LOCK
                                )
                            },
                            onDoubleTap = { offset ->
                                val visibility = state.visibility
                                val isLocked = visibility == Console.VISIBILITY_LOCKED
                                if (isLocked) {
                                    // Show message and return on lock
                                    onRequest(REQUEST_TOGGLE_VISIBILITY)
                                    return@detectTapGestures
                                }
                                // Ensure controller is hidden while tapping.
                                val visible = state.visibility == Console.VISIBILITY_VISIBLE
                                if (visible)
                                    state.visibility = Console.VISIBILITY_HIDDEN
                                val current = System.currentTimeMillis()
                                // Check if it is a continuous fast tap.
                                val isFastTap =
                                    current - lastTapTime < 600 // 600ms double tap window
                                if (isFastTap) tapCount++ else tapCount = 1
                                lastTapTime = current
                                val (width, _) = size
                                val (x, _) = offset // Extract tap position
                                // Maybe check if the device is in ltr/rtl
                                val isLeftTap = x < width / 2 // Determine tap side

                                // Calculate seek increment based on side
                                val increment = if (isLeftTap) -10_000L else +10_000L
                                Log.d(
                                    TAG,
                                    "onDoubleTap: width: $width, x: $x, isLeft: $isLeftTap, multiplier: $tapCount, visible: $visible"
                                )
                                state.seek(increment) // Perform seek
                                // Update message with multiplied seek time
                                state.message = "${if (isLeftTap) "-" else "+"}${tapCount * 10}s"
                            }
                        )
                    }
                    .pointerInput("seek_controls") {
                        val manager = facade.getDeviceService<AudioManager>(Context.AUDIO_SERVICE)
                        // These are used to keep track of the brightness/volume to make change more
                        // clean.
                        var volume = manager.volume
                        var brightness = facade.brightness
                        detectVerticalDragGestures(
                            onVerticalDrag = { change, dragAmount ->
                                if (state.visibility == Console.VISIBILITY_LOCKED) {
                                    // Show message and return on lock
                                    onRequest(REQUEST_TOGGLE_VISIBILITY)
                                    return@detectVerticalDragGestures
                                }
                                if (state.visibility != Console.VISIBILITY_HIDDEN)
                                    onRequest(REQUEST_TOGGLE_VISIBILITY)
                                val (width, _) = size
                                // Get the position of the gesture
                                val positionX = change.position.x
                                // Calculate the change in volume or brightness based on the drag amount.
                                // The dragAmount is in pixels, so we convert it to a normalized value
                                // The scaling factor
                                // (-0.001f) was derived empirically.
                                val real = (dragAmount / 1.dp.toPx()) * -0.001f // scale factor

                                // Check if the drag gesture is on the left side of the screen.
                                if (positionX < width / 2) {
                                    // Brightness Control
                                    // -------------------
                                    val new = (brightness + real)
                                    // Adjust brightness.  If the user drags downwards and the brightness is
                                    // already at its minimum (0f), allow it to go to -1f (automatic).
                                    brightness =
                                        if (new < 0f && real < 0f) -1f else new.coerceIn(0f, 1f)
                                    facade.brightness =
                                        brightness           // Set the system brightness.
                                    // Update the UI message to display the current brightness level.
                                    if (brightness == -1f)
                                        state.message = "Ⓐ Automatic"
                                    else
                                        state.message = """🔆 ${(brightness * 100).roundToInt()}%"""
                                } else {
                                    //  Volume Control
                                    //  ----------------
                                    // Calculate the new volume.
                                    volume = (volume + real).coerceIn(
                                        0f,
                                        1f
                                    ) // Keep volume within 0-1 range.
                                    manager.volume =
                                        volume                   // Set the system volume.
                                    // Update the UI message to display the current volume percentage.
                                    state.message = """🔊 ${(volume * 100).roundToInt()}%"""
                                }
                                // Mark the gesture as consumed, so other gestures
                                // don't also respond to it.
                                change.consume()
                            }
                        )
                    }
            )

        // Scrim; when current item is a video
        if (isVideo)
            Spacer(
                modifier = Modifier
                    .background(Color.Black.copy(0.30f))
                    .layoutId(Constraints.ID_SCRIM)
            )

        // Signature
        Text(
            text = stringResource(id = R.string.app_name),
            fontFamily = FontFamily.DancingScriptFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 70.sp,
            modifier = Modifier.layoutId(Constraints.ID_SIGNATURE),
            color = /*contentColor*/ Color.Transparent,
            maxLines = 1
        )

        // Close Button
        OutlinedButton2(
            onClick = onNavigateBack,
            modifier = Modifier
                .scale(0.8f)
                .layoutId(Constraints.ID_CLOSE_BTN),
            colors = ButtonDefaults.outlinedButtonColors(
                backgroundColor = Color.Transparent,
                accent
            ),
            contentPadding = PaddingValues(vertical = 16.dp),
            shape = RoundedCornerShape_24,
            content = {
                Icon(
                    imageVector = Icons.Outlined.Close,
                    contentDescription = "Collapse"
                )
            },
        )

        // Artwork
        // TODO - Support different shapes, animation, effects, etc.
        val bordered by preference(Settings.ARTWORK_BORDERED)
        val elevated by preference(Settings.ARTWORK_ELEVATED)
        val shapeKey by preference(Settings.ARTWORK_SHAPE_KEY)
        val artworkShape = Settings.mapKeyToShape(shapeKey)
        Artwork(
            data = state.artworkUri,
            modifier = Modifier
                .thenIf(!state.isVideo) { sharedElement(Constraints.ID_ARTWORK) }
                .layoutId(Constraints.ID_ARTWORK)
                .visualEffect(ImageBrush.NoiseBrush, 0.5f, true)
                .thenIf(bordered) { border(1.dp, contentColor, artworkShape) }
                .shadow(if (elevated) ContentElevation.medium else 0.dp, artworkShape, clip = true)
                .background(AppTheme.colors.background(1.dp)),
        )

        // Timer
        Label(
            text = state.position(LocalContentColor.current.copy(ContentAlpha.disabled)),
            modifier = Modifier.layoutId(Constraints.ID_POSITION),
            style = AppTheme.typography.caption,
            fontWeight = FontWeight.Bold
        )

        // Subtitle
        Label(
            text = state.subtitle ?: stringResource(id = R.string.unknown),
            style = AppTheme.typography.caption,
            modifier = Modifier
                .sharedElement(Constraints.ID_SUBTITLE)
                .layoutId(Constraints.ID_SUBTITLE),
            color = contentColor
        )

        // Title
        Label(
            text = state.title ?: stringResource(id = R.string.unknown),
            fontSize = constraints.titleTextSize,// Maybe Animate
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .sharedElement(Constraints.ID_TITLE)
                .marque(Int.MAX_VALUE)
                .layoutId(Constraints.ID_TITLE),
            color = contentColor
        )

        // ProgressRow
        TimeBar(
            state = state,
            accent = accent,
            style = when {
                !state.playing || background == BACKGROUND_VIDEO_SURFACE -> SEEKBAR_STYLE_SIMPLE
                else -> SEEKBAR_STYLE_WAVY
            },
            onRequest = onRequest,
            modifier = Modifier
                .sharedElement(Constraints.ID_TIME_BAR)
                .layoutId(Constraints.ID_TIME_BAR)
        )

        // Controls
        Controls(
            state = state,
            style = if (background == BACKGROUND_VIDEO_SURFACE) PLAY_BUTTON_STYLE_SIMPLE else PLAY_BUTTON_STYLE_ROUNDED,
            modifier = Modifier
                .sharedElement(Constraints.ID_CONTROLS)
                .layoutId(Constraints.ID_CONTROLS)
        )

        // Options
        Options(
            state = state,
            modifier = Modifier.layoutId(Constraints.ID_OPTIONS),
            onRequest = onRequest
        )

        // Message
        Message(
            message = state.message,
            modifier = Modifier.layoutId(Constraints.ID_MESSAGE)
        )
    }
}

/**
 * Checks if the shape of the window resembles a mobile phone in portrait mode.
 * @property isMobilePortrait a boolean value that indicates if the window is portrait or not
 */
private val WindowSize.isMobilePortrait
    get() = widthRange == Range.Compact && widthRange < heightRange

/**
 * Constructs a two-pane strategy based on the window size and the gap width.
 * A two-pane strategy is a layout that divides the screen into two panes: a content pane and a details pane.
 * The content pane shows a list of items, and the details pane shows the details of the selected item.
 * The orientation and size of the two panes depend on the window size and the gap width.
 *
 * @param window the size of the window in which the two-pane strategy is applied.
 * @param gapWidth the width of the gap between the two panes.
 * @param restrinct an optional parameter that restricts the size of the details pane to this value if specified;
 * if 0.dp, it hides the details pane altogether;
 * if unspecified, it uses the default values based on the window size and orientation.
 * @return a [TwoPaneStrategy] object that represents the layout of the two panes.
 */
private fun TwoPaneStrategy(
    window: WindowSize,
    gapWidth: Dp,
    restrinct: Dp = Dp.Unspecified
): TwoPaneStrategy {
    // Check if the window is in mobile portrait mode.
    // A mobile portrait mode is one where width is compact and height is greater than width.
    val (width, height) = window.value
    return when (window.isMobilePortrait) {
        true -> {
            // In portrait mode, the two panes are stacked vertically.
            // The content pane height is 45% of the window height by default, but it is
            // constrained to be between 270.dp and 350.dp
            // If restrinct is specified, it overrides the default value.
            val splitFromTop = restrinct.takeOrElse { (height * 0.45f).coerceIn(270.dp, 400.dp) }
            VerticalTwoPaneStrategy(splitFromTop, !restrinct.isSpecified, gapWidth)
        }

        else -> {
            // In landscape mode, the two panes are side by side.
            // The content pane width is 60% of the window width by default, but it is constrained
            // to be between 320.dp and 500.dp
            // If restrinct is specified, it overrides the default value.
            val slitFromStart =
                restrinct.takeOrElse { width - (0.40f * width).coerceIn(320.dp, 500.dp) }
            HorizontalTwoPaneStrategy(slitFromStart, !restrinct.isSpecified, gapWidth)
        }
    }
}

/**
 * The minimum window size necessary to display the details section of the UI.
 *
 * This value ensures that the details have sufficient space to be presented
 * legibly and effectively. If the current window size falls below this threshold,
 * the details willn't be shown as part of ui to avoid visual clutter or potential usability issues.
 *
 * @property width The minimum required width in density-independent pixels (600.dp).
 * @property height The minimum required height in density-independent pixels (550.dp).
 */
private val minimumWindowSizeForDetails = DpSize(600.dp, 550.dp)

/**
 * The raius of the shapes uped in Two Panes.
 */
private const val TWO_PANE_RADIUS_PCT = 7

/**
 * Constant indicating that no details pane should be shown.
 */
private const val DETAILS_OF_NONE = -1

@SuppressLint("UnusedBoxWithConstraintsScope")
@Composable
fun Console(state: Console) {
    // Get the current window size to adapt the UI accordingly
    val windowSize by rememberUpdatedState(newValue = LocalWindowSize.current)
    // Track which component's details are currently being displayed
    var detailsOf by remember { mutableIntStateOf(DETAILS_OF_NONE) }
    Log.d(TAG, "windowSize: ${windowSize.value}")
    val view = LocalView.current
    val isInInspectionMode = LocalInspectionMode.current
    val controller = if (!isInInspectionMode)
        WindowCompat.getInsetsController((view.context as Activity).window, view)
    else
        null
    // Declare a function to handle incoming requests from the UI,
    // such as showing or hiding the details pane.
    val onRequest = onRequest@{ request: Int ->
        // Log the incoming request value for debugging purposes
        Log.d(TAG, "onRequest code: $request")

        // Check for a common toggle scenario, where the user wants to show or hide the currently
        // displayed details pane.
        if (request == detailsOf) {
            detailsOf = DETAILS_OF_NONE
            return@onRequest true
        }

        // Check if the window size is sufficient for showing details as a second pane.
        val isDetailsRequest =
            request == REQUEST_SHOW_PROPERTIES || request == REQUEST_SHOW_PLAYING_QUEUE
        val size = windowSize.value
        if (isDetailsRequest && minimumWindowSizeForDetails.contains(size)) {
            Log.d(TAG, "Window is too small to display details: ${windowSize.value}")
            return@onRequest false
        }

        // Handle specific request types:
        when (request) {
            REQUEST_HANDLE_BACK_PRESS -> {
                // Consume request if locked.
                if (state.visibility == Console.VISIBILITY_LOCKED) {
                    state.message = "\uD83D\uDD12 Long click to unlock."
                    return@onRequest true
                }
                // Back press handling: check the current value of detailsOf
                // If no details are showing, do nothing and return false to indicate that the
                // request has not been handled by this function
                if (detailsOf == DETAILS_OF_NONE) false
                // Otherwise, hide the details and return true to indicate that the request has
                // been handled by this function
                else {
                    detailsOf = DETAILS_OF_NONE
                    true
                }
            }

            // Toggle Show/Hide Playng Queue
            REQUEST_SHOW_PLAYING_QUEUE -> {
                // Show the playing queue in the details pane
                detailsOf = request
                return@onRequest true
            }
            // Handle requests for light or dark system bars:
            REQUEST_REQUIRES_LIGHT_SYSTEM_BARS, REQUEST_REQUIRES_DARK_SYSTEM_BARS -> {
                val isAppearanceLightStatusBars = request != REQUEST_REQUIRES_DARK_SYSTEM_BARS
                controller?.isAppearanceLightStatusBars = isAppearanceLightStatusBars
                controller?.isAppearanceLightNavigationBars = isAppearanceLightStatusBars
                return@onRequest true
            }

            REQUEST_TOOGLE_LOCK -> {
                val isLocked = state.visibility == Console.VISIBILITY_LOCKED
                state.message = if (isLocked) "\uD83D\uDD13 Unlocked" else "\uD83D\uDD12 Locked"
                state.visibility =
                    if (isLocked) Console.VISIBILITY_VISIBLE else Console.VISIBILITY_LOCKED
                // return consumed
                true
            }

            REQUEST_TOGGLE_VISIBILITY -> {
                val visibility = state.visibility
                val isLocked = visibility == Console.VISIBILITY_LOCKED
                if (isLocked) {
                    state.message = "\uD83D\uDD12 Long click to Unlock"
                    return@onRequest true
                }
                // Don't entertain if it is exclusively set to always visible.
                if (visibility == Console.VISIBILITY_ALWAYS)
                    return@onRequest false
                val visible = visibility == Console.VISIBILITY_VISIBLE
                // Toggle Visibility
                if (visible)
                    state.visibility = Console.VISIBILITY_HIDDEN
                else
                    state.visibility = Console.VISIBILITY_VISIBLE
                // Return cosnumed.
                true
            }

            REQUEST_TOGGLE_ROTATION_LOCK -> {
                val activity = view.context.findActivity()
                state.message =
                    if (activity.isOrientationLocked) "\uD83D\uDD13 Rotation" else "\uD83D\uDD12 Rotation"
                activity.toggleRotationLock()
            }

            else -> error("Unsupported request: $request")  // Throw an error for unsupported requests
        }
    }
    //
    val isInTwoPaneMode = detailsOf != DETAILS_OF_NONE
    val radius by animateIntAsState(
        targetValue = if (isInTwoPaneMode) TWO_PANE_RADIUS_PCT else 0,
        label = ""
    )
    // The main content of the UI,
    // which can be moved around based on details being shown
    val content = remember {
        movableContentOf {
            // Determine whether to show the controller based on the visibility and the isVideo values
            // Always show the controller if not video
            val showController =
                state.visibility == Console.VISIBILITY_VISIBLE || state.visibility == Console.VISIBILITY_ALWAYS
            // Get the system bars insets as a DpRect
            val insets = (if (windowSize.isMobilePortrait && detailsOf != DETAILS_OF_NONE)
                WindowInsets.statusBars
            else WindowInsets.systemBars)
                .asDpRect
            // Why use BoxWithConstraints
            // Because the window doesn't always depend on the size i provided; since the size
            // can also depend on the split of the window.
            BoxWithConstraints {
                val newWindowSize = WindowSize(DpSize(maxWidth, maxHeight))
                val isVideo = state.isVideo
                // Calculate the constraints for the content based on the new window size, the insets,
                // the isVideo value, and the showController value
                val constraints = remember(isVideo, newWindowSize, showController, insets) {
                    calculateConstraintSet(newWindowSize, insets, isVideo, !showController)
                }
                // Set immersive mode based on the visibility state.
                SideEffect { controller?.immersiveMode(/*!showController*/ isVideo) }
                // Display the main content with the given state, constraints, onRequest function, and
                // modifier
                MainContent(
                    state = state,
                    constraints = constraints,
                    onRequest = onRequest,
                    modifier = Modifier
                        .clip(RoundedCornerShape(radius))
                        .fillMaxSize()
                        .animateContentSize()
                )
            }
        }
    }
    // Call to pause the screen when the user intends to leave the screen, and the current
    // content is a video.
    val owner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    // Determine the default appearance of light system bars based on user preferences and
    // material theme.
    val isAppearanceLightSystemBars = AppTheme.colors.isAppearanceLightSystemBars
    // Use DisposableEffect to observe the lifecycle events of the current owner (typically the
    // current composable).
    val facade = LocalSystemFacade.current
    DisposableEffect(key1 = owner) {
        // Define a LifecycleEventObserver to pause playback when the screen is paused.
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE && state.isVideo) {
                // Pause the playback when the screen is paused.
                state.isPlaying = false
            }
        }
        // Add the observer to the owner's lifecycle.
        owner.lifecycle.addObserver(observer)
        // Define cleanup logic when the effect is disposed of.
        onDispose {
            // Remove the observer from the owner's lifecycle.
            owner.lifecycle.removeObserver(observer)
            // Restore the default color appearance of system bars based on the theme.
            controller?.immersiveMode(false)
            facade.brightness = -1f // restore back to automatic.
            controller?.isAppearanceLightStatusBars = isAppearanceLightSystemBars
            controller?.isAppearanceLightNavigationBars = isAppearanceLightSystemBars
        }
    }

    // FixMe - This causes a little glitch.
    SideEffect {
        // Remove details if windowSize Changed and it doesn't respect minimumWindowSizeForDetails
        // constraint.
        if (isInTwoPaneMode && minimumWindowSizeForDetails.contains(windowSize.value))
            detailsOf = DETAILS_OF_NONE
    }

    // Use TwoPane component to show the content in two panes
    val context = LocalContext.current
    TwoPane(
        // The first pane is the content, which can be moved around based on details being shown
        first = content,
        strategy = if (!isInTwoPaneMode) VerticalTwoPaneStrategy(1f) else TwoPaneStrategy(
            windowSize,
            10.dp
        ),
        displayFeatures = if (isInInspectionMode || !isInTwoPaneMode) emptyList() else calculateDisplayFeatures(
            activity = context.findActivity()
        ),
        modifier = Modifier.sharedElement(Constraints.ID_BACKGROUND),
        // The second pane is the details pane, which can show different content based on the
        // detailsOf value
        // Use the horizontal or vertical two pane strategy based on the orientation and the new window size
        second = {
            if (detailsOf == DETAILS_OF_NONE)
                return@TwoPane
            // Get the system bars insets as a padding values
            val padding = if (windowSize.isMobilePortrait)
                WindowInsets.navigationBars.asPaddingValues() + PaddingValues(horizontal = ContentPadding.normal)
            else
                WindowInsets.systemBars.asPaddingValues()
            // Use a Surface to display the playing queue
            Surface(
                // Apply the padding to the modifier and adjust the horizontal padding based on the orientation
                modifier = Modifier.padding(padding),
                // Use the ContentShape as the shape of the surface
                shape = RoundedCornerShape(radius),
                // Use the overlay color or the background color based on the lightness of
                // the material colors
                color = if (AppTheme.colors.isLight)
                    AppTheme.colors.background(1.dp)
                else
                    AppTheme.colors.background(0.1.dp),
                // Use the onSurface color as the content color
                contentColor = AppTheme.colors.onBackground,
                // Use the outline color as the border stroke or null based on the lightness
                // of the material colors
                border = if (AppTheme.colors.isLight) BorderStroke(
                    0.2.dp,
                    AppTheme.colors.accent
                ) else null,
                // Display the playing queue with the given state, onDismissRequest function,
                // and modifier
                content = {
                    // Check the detailsOf value to determine what to show in the details pane
                    if (detailsOf == REQUEST_SHOW_PLAYING_QUEUE)
                        PlayingQueue(
                            state = state,
                            onDismissRequest = { detailsOf = DETAILS_OF_NONE },
                            modifier = Modifier.fillMaxSize()
                        )
                }
            )
        }
    )
}


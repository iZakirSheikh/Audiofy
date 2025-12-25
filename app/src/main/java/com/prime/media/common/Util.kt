/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 10-10-2024.
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

@file:Suppress("NOTHING_TO_INLINE")

package com.prime.media.common

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.media3.common.C
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.prime.media.BuildConfig
import com.prime.media.R
import com.prime.media.common.Registry.provider
import com.prime.media.common.menu.Action
import com.primex.core.withSpanStyle
import com.zs.core.paymaster.Paymaster
import com.zs.core.paymaster.ProductInfo
import com.zs.core.playback.NowPlaying
import com.zs.core.playback.PlaybackController
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Represents an app with its title, image URL, and Play Store URL.
 *
 * @property first The title of theapp.
 * @property second The URL of the app's image.
 * @property third The URL of the app's Play Store page.
 */
typealias App = Triple<String, String, String>

/**
 * Creates a split install request for the module with [name]
 */
inline fun SplitInstallRequest(name: String) =
    SplitInstallRequest.newBuilder().addModule(name).build()

/**
 * Checks if the product represents a dynamic feature.
 */
val ProductInfo.isDynamicFeature
    inline get() = this.id == BuildConfig.IAP_CODEX

/**
 * Checks if a dynamic module with the given name is installed.
 *
 * @param id The name of the dynamic module.
 * @return True if the module is installed, false otherwise.
 */
fun SplitInstallManager.isInstalled(id: String): Boolean =
    installedModules.contains(id)

/**
 * Returns the name of the dynamic feature module associated with this product.
 *
 * This function maps product IDs to dynamic feature module names.
 * It throws an [IllegalArgumentException] if the product is not associated
 * with a dynamic feature module.
 *
 * @return The name of the dynamic feature module.
 * @throws IllegalArgumentException if the product is not a dynamic feature module.
 * @see isDynamicFeature
 */
val ProductInfo.dynamicModuleName
    inline get() = when (id) {
        BuildConfig.IAP_CODEX -> "codex"
        else -> error("$id is not a dynamic module.")
    }

/**
 * Creates a SplitInstallRequest for the dynamic feature associated with the product.
 */
val ProductInfo.dynamicFeatureRequest
    inline get() = SplitInstallRequest(dynamicModuleName)

/**
 * Utility function for [Flow] that processes each item in the emitted collections.
 *
 * This function iterates through each collection emitted by the Flow and applies the provided
 * `action` to each individual item within those collections.
 *
 * It essentially flattens the emitted collections of items into a stream of individual items
 * and then applies the `action` to each of them sequentially.
 *
 * @param T The type of the items in the collections emitted by the Flow.
 * @param action A suspending function that is executed for each item in the collections.
 *
 * @return A Flow that emits the original collections after the `action` has been applied
 *         to each item within them.
 */
inline fun <T> Flow<Iterable<T>>.onEachItem(crossinline action: suspend (T) -> Unit) = onEach {
    for (item in it)
        action(item)
}

/**
 * @see com.prime.media.old.core.playback.MediaItem
 */
fun MediaFile(context: Context, uri: Uri, mimeType: String?) =
    com.prime.media.old.core.playback.MediaItem(context, uri, mimeType)

/**
 * Returns a formatted [AnnotatedString] representation of the product description.
 *
 * This property formats the product information by displaying the title in bold
 * followed by the description on a new line. It uses an [AnnotatedString] for
 * richer text representation.
 *
 * @return An [AnnotatedString] containing the formatted product description.
 */
val ProductInfo.richDesc
    get() = buildAnnotatedString {
        withSpanStyle(fontWeight = FontWeight.Bold) { appendLine(title.ellipsize(22)) }
        withSpanStyle(Color.Gray) { append(description) }
    }

private const val ELLIPSIS_NORMAL = "\u2026"; // HORIZONTAL ELLIPSIS (…)

/**
 * Ellipsizes this CharSequence, adding a horizontal ellipsis (…) if it is longer than [after] characters.
 *
 * @param after The maximum length of the CharSequence before it is ellipsized.
 * @return The ellipsized CharSequence.
 */
fun CharSequence.ellipsize(after: Int): CharSequence =
    if (this.length > after) this.substring(0, after) + ELLIPSIS_NORMAL else this

/**
 * @return - Indicates if the widget is free in the Play Console.
 * Some widgets are listed for providing localized names and descriptions,
 * but their price cannot be set to 0.
 *
 * **Note**: Developer need register any product for freemium
 */
val ProductInfo.isFreemium: Boolean
    get() {
        return when (id) {
            BuildConfig.IAP_PLATFORM_WIDGET_IPHONE, BuildConfig.IAP_COLOR_CROFT_GRADIENT_GROVES,
            BuildConfig.IAP_ARTWORK_SHAPE_HEART, BuildConfig.IAP_ARTWORK_SHAPE_ROUNDED_RECT,
            BuildConfig.IAP_ARTWORK_SHAPE_CIRCLE, BuildConfig.IAP_ARTWORK_SHAPE_CUT_CORNORED_RECT -> true

            else -> false
        }
    }

/**
 * Controls whether this item should be showcased for purchase.
 * Sometimes the group is not fully prepared and needs further improvements.
 * This variable can be used to control whether to showcase this item for purchase.
 */
val ProductInfo.isPurchasable: Boolean
    get() {
        return when (id) {
            //BuildConfig.IAP_COLOR_CROFT_WIDGET_BUNDLE -> false
            else -> true
        }
    }

val ProductInfo.action
    @StringRes
    get() = when (id) {
        BuildConfig.IAP_BUY_ME_COFFEE -> R.string.sponsor
        else -> R.string.unlock
    }


private val fullLineSpan: (LazyGridItemSpanScope.() -> GridItemSpan) = { GridItemSpan(maxLineSpan) }
val LazyGridScope.fullLineSpan
    get() = com.prime.media.common.fullLineSpan

/**
 * Represents a sorting order and associated grouping or ordering action.
 *
 * @property first Specifies whether the sorting is ascending or descending.
 * @property second Specifies the action to group by or order by.
 */
typealias Filter = Pair<Boolean, Action>

/**
 * Represents a mapping from a string key to a list of items of type T.
 *
 * @param T The type of items in the list.
 */
typealias Mapped<T> = Map<CharSequence, List<T>>

/**
 * Observes the current [NowPlaying] as state.
 */
@SuppressLint("ProduceStateDoesNotAssignValue")
@Composable
fun PlaybackController.Companion.collectNowPlayingAsState(): State<NowPlaying> {
    val ctx = LocalContext.current
    return produceState(NowPlaying.EMPTY, this) {
        observe(ctx).collect { this.value = it }
    }
}

private const val TAG = "Util"

/**
 * Provides a [Chronometer] that tracks the current playback position.
 *
 * The chronometer's value is `-1L` if the position is not available (e.g., during initial load,
 * if the duration is unknown, or if playback is complete). Otherwise, the value represents the
 * current playback position in milliseconds, updated to reflect the elapsed time during playback.
 */
val NowPlaying.chronometer: Chronometer
    @Composable
    get() {
        // Create and remember a Chronometer instance.
        val chronometer = remember { Chronometer(-1L) }

        // Launch an effect that updates the chronometer based on the NowPlaying state.
        LaunchedEffect(this) {
            // If duration or current position is not set, or if playback is complete, set chronometer to -1L and return.
            if (duration == C.TIME_UNSET || duration == 0L || position == duration || position == C.TIME_UNSET)
                return@LaunchedEffect

            var current = position
            // Set the initial value of the chronometer to the current position.
            chronometer.value = current
            // If not playing, return without starting the chronometer updates.
            if (!playing) return@LaunchedEffect
            // since we might have moved from old to new time
            // FIXME - consider the case if the user have set variable speed during the time.
            val elapsed = System.currentTimeMillis() - timeStamp
            current += (elapsed * speed).roundToLong()
            // Launch a coroutine to update the chronometer periodically.
            launch {
                // Continue updating the chronometer until the current position reaches the duration.
                while (current < duration) {
                    Log.d(
                        TAG,
                        "duration: $duration | value: ${chronometer.value} | position: $current "
                    )
                    // Delay for 1 second.
                    delay(1000)
                    // Update the current position based on the playback speed.
                    current += (1000 * speed).roundToLong()
                    chronometer.value = current
                }
            }
        }
        return chronometer
    }

val TextFieldState.raw get() = text.trim().toString().ifEmpty { null }

/**
 * Represents the volume of the music stream, providing a normalized view (0.0 to 1.0)
 * over the system's integer volume range.
 *
 * The volume is represented as a float between 0.0 and 1.0, where 0.0 is muted and 1.0
 * is the maximum volume.  This property maps between the normalized float
 * representation and the underlying integer volume levels of the Android system's
 * `AudioManager.STREAM_MUSIC` stream.
 *
 * **Important Considerations:**
 *
 * -   **Integer Mapping:** The Android system manages volume using integer levels.
 * This property converts the 0.0-1.0 float range to the nearest integer
 * volume level when setting the volume. This means that very small
 * adjustments in the float value might not result in a change in the actual
 * system volume.
 * -   **Granularity:** The number of discrete volume levels is determined by
 * `getStreamMaxVolume(AudioManager.STREAM_MUSIC)`.  A larger maximum volume
 * means finer-grained control.
 * -   **Clamping:** When setting the volume, the provided float value is clamped
 * to the range 0.0 to 1.0 to ensure it's within valid bounds.
 *
 * @property volume Gets or sets the normalized volume of the music stream (0.0 to 1.0).
 * -   **Get:** Retrieves the current music stream volume and normalizes it to a
 * float between 0.0 and 1.0 by dividing by the maximum stream volume.
 * -   **Set:** Sets the music stream volume.  The provided float value is
 * first clamped to the 0.0-1.0 range, then converted to the corresponding
 * integer volume level.
 */
var AudioManager.volume: Float
    get() {
        // Get the current volume of the music stream.
        val current = getStreamVolume(AudioManager.STREAM_MUSIC)
        // Get the maximum volume for the music stream.
        val max = getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        // Normalize the current volume to a float between 0.0 and 1.0.
        // Handle the case where maxVolume is 0 to avoid division by zero.
        return if (max > 0) current.toFloat() / max.toFloat() else 0f
    }
    set(value) {
        // Get the maximum volume for the music stream.
        val max = getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        // Convert the normalized volume (0.0-1.0) to an integer volume level.
        // Clamp the input value to the valid range of 0.0 to 1.0.
        val target = (value.coerceIn(0f, 1f) * max).roundToInt()
        // Log the volume change for debugging purposes.
        Log.d(TAG, "Setting stream volume to index: $target (raw value: $value, max: $max)")
        // Set the volume of the music stream.
        // The third parameter (0) is a flag that specifies whether to show a volume UI.
        setStreamVolume(AudioManager.STREAM_MUSIC, target, 0)
    }

/**
 *  The current screen brightness of this activity's window.
 *
 *  This value ranges from 0.0f to 1.0f, where 0.0f is the darkest and 1.0f is the brightest.
 *  A value of -1.0f indicates that the system's default brightness is being used.
 *
 *  Note: Setting this value directly modifies the window's layout attributes.
 *  Changes may not be immediately reflected if the system is managing the brightness
 *  automatically (e.g., under automatic brightness control). In such cases, the actual
 *  brightness may be clamped or overridden by the system.
 *
 *  @see android.view.WindowManager.LayoutParams.screenBrightness
 */
var SystemFacade.brightness: Float
    get() = (this as? Activity)?.window?.attributes?.screenBrightness ?: 1f
    set(value) {
        val window = (this as? Activity)?.window ?: return
        val attr = window.attributes
        // -1f means use system brightness.
        if (value == -1f)
            attr.screenBrightness = value
        else
        //  Confine value between 0 and 1.
            attr.screenBrightness = value.coerceIn(0f, 1f)
        window.attributes = attr
    }

/**
 * Creates a [FontFamily] from the given Google Font name.
 *
 * @param name The name of theGoogle Font to use.
 * @return A [FontFamily] object
 */
@Stable
fun FontFamily(name: String): FontFamily {
    // Create a GoogleFont object from the given name.
    val font = GoogleFont(name)
    // Create a FontFamily object with four different font weights.
    return FontFamily(
        Font(fontProvider = Registry.provider, googleFont = font, weight = FontWeight.Light),
        Font(fontProvider = Registry.provider, googleFont = font, weight = FontWeight.Medium),
        Font(fontProvider = Registry.provider, googleFont = font, weight = FontWeight.Normal),
        Font(fontProvider = Registry.provider, googleFont = font, weight = FontWeight.Bold),
    )
}

val FontFamily.Companion.DancingScriptFontFamily get() = Registry.DancingScriptFontFamily
/*
 *  Copyright (c) 2025 Zakir Sheikh
 *
 *  Created by Zakir Sheikh on $today.date.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.zs.audiofy.settings

import android.os.Build
import android.util.Log
import com.zs.audiofy.settings.AppConfig.KEYS_DELIMITER
import com.zs.audiofy.settings.AppConfig.RECORD_DELIMITER
import com.zs.audiofy.settings.AppConfig.isBackgroundBlurEnabled
import com.zs.audiofy.settings.AppConfig.isLoadThumbnailFromCache
import com.zs.audiofy.settings.AppConfig.update

/**
 * Singleton object for managing application-wide configuration settings.
 *
 * `AppConfig` provides a centralized repository for settings that can be dynamically
 * modified during runtime. Some changes may necessitate an application restart.
 *
 * Initialization occurs at application startup via the [update] method.
 * Configuration values can be modified through the application's settings interface.
 *
 * This object handles default values and configurations not directly managed
 * by Jetpack Compose, ensuring a clear separation of concerns for global settings.
 *
 * @author Zakir Sheikh
 * @since 1.0.0
 */
object AppConfig {

    private const val TAG = "AppConfig"

    /** Determines if media thumbnails should be loaded from the cache. */
    var isLoadThumbnailFromCache: Boolean = false

    /** Get/Set strategy enable background blur. */
    var isBackgroundBlurEnabled: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    /** Determines if the trash can feature for deleted media is enabled. */
    var isTrashCanEnabled: Boolean = true

    /** Specifies the font scaling factor for the application. -1f indicates system default. */
    var fontScale: Float = -1f

    /** Minimum duration in seconds for a track to be considered valid or displayed. */
    var minTrackLengthSecs: Int = 30

    /** Indicates whether to use the system's built-in audio effects. */
    var inAppAudioEffectsEnabled: Boolean = false

    /** Multiplier for adjusting the size of items in grid layouts. */
    var gridItemSizeMultiplier: Float = 1.0f

    /**
     * Determines FAB player long press action.
     * `true`: launch [com.zs.audiofy.console.Console].
     * `false`: launch in-app widget. Defaults to `true`.
     */
    var fabLongPressLaunchConsole = true

    /**
     * Toggles between SurfaceView and TextureView for video rendering.
     * `true` enables SurfaceView, `false` enables TextureView. Defaults to `true`.
     */
    var isSurfaceViewVideoRenderingPreferred = true


    // Delimiters
    private const val KEYS_DELIMITER = '\u001E'
    private const val RECORD_DELIMITER = ':'

    // Keys
    private const val KEY_LOAD_THUMB_FROM_CACHE = "param1"
    private const val KEY_BACKGROUND_BLUR_ENABLED = "param2"
    private const val KEY_TRASH_CAN_ENABLED = "param3"
    private const val KEY_FONT_SCALE = "param4"
    private const val KEY_MIN_TRACK_LENGTH_SECS = "param5"
    private const val KEY_USE_SYSTEM_AUDIO_FX = "param6"
    private const val KEY_GRID_ITEM_SIZE_MULTIPLIER = "param7"
    private const val KEY_FAB_LONG_PRESS_LAUNCH_CONSOLE = "param8"
    private const val KEY_SURFACE_VIEW_VIDEO_RENDERING_PREFERRED = "param9"


    /**
     * Appends a single key-value record to the given [StringBuilder].
     *
     * This operator function facilitates the construction of a serialized string
     * representing application configuration. It appends the provided `key` and `value`
     * to the `StringBuilder`, separated by [RECORD_DELIMITER] and followed by
     * [KEYS_DELIMITER].
     *
     * Example usage: `stringBuilder["myKey"] = "myValue"` results in `myKey:myValue\u001E` being appended.
     *
     * @param T The type of the value. Must be a simple type (String, Int, Boolean, Float, Long).
     * @param key The key for the configuration setting.
     * @param value The value of the configuration setting.
     * @throws IllegalArgumentException if the provided value type `T` is not supported.
     */
    @Throws(IllegalArgumentException::class)
    private inline operator fun <reified T> StringBuilder.set(key: String, value: T) {
        when (T::class) {
            // append key, separator, value, and record delimiter in a single call chain
            String::class, Int::class, Boolean::class, Float::class, Long::class -> {
                append(key)
                append(RECORD_DELIMITER)
                append(value)
                append(KEYS_DELIMITER)
            }
            else -> throw IllegalArgumentException("Unsupported type: ${T::class}")
        }
    }

    /**
     * Serializes the current configuration into a string format.
     *
     * This method converts the application's current settings, such as
     * [isLoadThumbnailFromCache] and [isBackgroundBlurEnabled], into a
     * single string. Each setting is represented as a key-value pair,
     * delimited by [RECORD_DELIMITER], and multiple settings are separated
     * by [KEYS_DELIMITER].
     *
     * The resulting string can be used for persisting the configuration or
     * transmitting it. It can be later parsed by the [update] method to
     * restore the configuration.
     *
     * @return A string representation of the current application configuration.
     *         Example: "param1:true␞param2:false␞"
     */
    fun stringify(): String {
        val records = StringBuilder()
        records[KEY_LOAD_THUMB_FROM_CACHE] = isLoadThumbnailFromCache
        records[KEY_BACKGROUND_BLUR_ENABLED] = isBackgroundBlurEnabled
        records[KEY_TRASH_CAN_ENABLED] = isTrashCanEnabled
        records[KEY_FONT_SCALE] = fontScale
        records[KEY_MIN_TRACK_LENGTH_SECS] = minTrackLengthSecs
        records[KEY_USE_SYSTEM_AUDIO_FX] = inAppAudioEffectsEnabled
        records[KEY_GRID_ITEM_SIZE_MULTIPLIER] = gridItemSizeMultiplier
        records[KEY_FAB_LONG_PRESS_LAUNCH_CONSOLE] = fabLongPressLaunchConsole
        records[KEY_SURFACE_VIEW_VIDEO_RENDERING_PREFERRED] = isSurfaceViewVideoRenderingPreferred
        Log.i(TAG, "stringify: $records")
        return records.toString()
    }

    /**
     * Updates the application configuration from a string representation.
     *
     * This function parses the input string, which is expected to be a series of
     * key-value pairs delimited by [KEYS_DELIMITER]. Each record within the string
     * should be in the format "key[RECORD_DELIMITER]value".
     *
     * The function iterates through each record, extracts the key and value,
     * and updates the corresponding configuration property.
     *
     * Example of `from` string:
     * `param1:true[KEYS_DELIMITER]param2:false[KEYS_DELIMITER]`
     *
     * @param from The string containing the configuration data to parse.
     *             It should be a string generated by the [stringify] method or a string
     *             following the same format.
     */
    fun update(from: String){
        Log.i(TAG, "update: $from")
        val records = from.split(KEYS_DELIMITER).filter { it.isNotEmpty() }
        for (record in records) {
            Log.i(TAG, "record: $record")
            val sepIndex = record.indexOf(RECORD_DELIMITER)
            if (sepIndex <= 0) continue

            val key = record.substring(0, sepIndex)
            val value = record.substring(sepIndex + 1)
            when (key) {
                KEY_LOAD_THUMB_FROM_CACHE -> isLoadThumbnailFromCache = value.toBoolean()
                KEY_BACKGROUND_BLUR_ENABLED -> isBackgroundBlurEnabled = value.toBoolean()
                KEY_TRASH_CAN_ENABLED -> isTrashCanEnabled = value.toBoolean()
                KEY_FONT_SCALE -> fontScale = value.toFloat()
                KEY_MIN_TRACK_LENGTH_SECS -> minTrackLengthSecs = value.toInt()
                KEY_USE_SYSTEM_AUDIO_FX -> inAppAudioEffectsEnabled = value.toBoolean()
                KEY_GRID_ITEM_SIZE_MULTIPLIER -> gridItemSizeMultiplier = value.toFloat()
                KEY_FAB_LONG_PRESS_LAUNCH_CONSOLE -> fabLongPressLaunchConsole = value.toBoolean()
                KEY_SURFACE_VIEW_VIDEO_RENDERING_PREFERRED -> isSurfaceViewVideoRenderingPreferred = value.toBoolean()
            }
        }
    }
}
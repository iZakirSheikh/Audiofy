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

package com.zs.audiofy.common.impl

import android.media.audiofx.Equalizer
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.zs.audiofy.effects.AudioFxViewState
import com.zs.core.playback.Remote
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import com.zs.audiofy.effects.RouteAudioFx as AudioFx

private const val TAG = "AudioFxViewModel"

/**
 * Gets an array of band levels for the equalizer.
 * Each element in the array corresponds to the band level at a specific band index.
 * The values are represented as floating-point numbers.
 */
private val Equalizer.bandLevels
    get() = Array(numberOfBands.toInt()) {
        getBandLevel(it.toShort()).toFloat()
    }

/**
 * Returns all the presets supported by the equalizer, including custom presets.
 * If a preset doesn't exist at a particular index, it returns "Custom."
 */
private val Equalizer.presets
    get() = Array(numberOfPresets + 1) {
        if (it == 0) "Custom" else getPresetName((it - 1).toShort())
    }

/**
 * Gets the status of the equalizer, which can be one of the following values:
 * - [AudioFx.EFFECT_STATUS_NO_CONTROL]: The equalizer does not have control over the audio.
 * - [AudioFx.EFFECT_STATUS_ENABLED]: The equalizer is enabled and actively affecting the audio.
 * - [AudioFx.EFFECT_STATUS_DISABLED]: The equalizer is disabled.
 */
private val Equalizer.status
    get() = when {
        !hasControl() -> AudioFx.EFFECT_STATUS_NO_CONTROL
        enabled -> AudioFx.EFFECT_STATUS_ENABLED
        else -> AudioFx.EFFECT_STATUS_DISABLED
    }

/**
 * Constant representing a custom preset in the equalizer.
 */
private const val PRESET_CUSTOM = 0

/**
 * Retries the operation to retrieve an Equalizer from the remote source with increasing delays.
 *
 * @param priority The priority of the operation.
 * @return An [Equalizer] object if the operation is successful, or null if it fails after maximum retries.
 */
private suspend fun Remote.getEqualizerOrRetry(priority: Int): Equalizer? {
    // Initialize the number of retry attempts.
    var tries = 0
    // Initialize the result variable to null.
    var result: Equalizer? = null
    // Use a while loop to retry the operation until the maximum number of tries (3) is reached.
    while (tries != 3) {
        // Increment the number of tries.
        tries++

        // Attempt to retrieve the equalizer from the remote source using runCatching.
        result = runCatching { getEqualizer(priority) }.getOrNull()

        // Check if the result is null, indicating a failure.
        if (result == null) {
            // If the result is null, delay for a progressively longer time before the next retry.
            delay(tries * 100L)
        } else {
            // If the result is not null (success), exit the loop.
            break
        }
    }
    return result
}

class AudioFxViewModel(val remote: Remote): KoinViewModel(), AudioFxViewState {
    private var equalizer: Equalizer? = null

    // init the variables
    // By default the effect is in not ready state.
    override var stateOfEqualizer: Int by mutableIntStateOf(AudioFx.EFFECT_STATUS_NOT_READY)
    private var _eqCurrentPreset: Int by mutableIntStateOf(0)

    override var eqNumberOfBands: Int = 0
    override val eqBandLevels = mutableStateListOf<Float>()
    override var eqBandLevelRange: ClosedFloatingPointRange<Float> = 0.0f..1.0f
    override var eqPresets: Array<String> = emptyArray()

    // Note: It is assumed that this function will only be called when the state of the equalizer
    // is not supported. Therefore, you can safely use the double bang (!!) operator here.
    override fun getBandFreqRange(band: Int): ClosedFloatingPointRange<Float> =
        equalizer!!.getBandFreqRange(band.toShort()).let { it[0].toFloat()..it[1].toFloat() }

    override fun getBandCenterFreq(band: Int): Int =
        equalizer!!.getCenterFreq(band.toShort())

    override fun setBandLevel(band: Int, level: Float) {
        // Set the band level in the equalizer.
        equalizer!!.setBandLevel(band.toShort(), level.roundToInt().toShort())

        // If the current preset is not "Custom," change it to "Custom."
        // This action will automatically update the band levels.
        if (eqCurrentPreset != PRESET_CUSTOM)
            eqCurrentPreset = PRESET_CUSTOM
        // If the current preset is already "Custom," update the band level manually.
        else
            eqBandLevels[band] = level
    }


    override var isEqualizerEnabled: Boolean
        get() = stateOfEqualizer == AudioFx.EFFECT_STATUS_ENABLED
        set(value) {
            // Maybe show Toast here.
            // TODO: Handle case when not supported.
            if (
                equalizer == null ||
                stateOfEqualizer == AudioFx.EFFECT_STATUS_NOT_READY ||
                stateOfEqualizer == AudioFx.EFFECT_STATUS_NOT_READY
            )
                return

            stateOfEqualizer =
                if (value) AudioFx.EFFECT_STATUS_ENABLED else AudioFx.EFFECT_STATUS_DISABLED
            equalizer!!.enabled = value
        }

    override var eqCurrentPreset: Int
        get() = _eqCurrentPreset
        set(value) {
            _eqCurrentPreset = value

            // Only set the preset when it's within a valid range.
            // The default presets of the equalizer represent values between 1 and the number of presets + 1.
            if (value in 1 until equalizer!!.numberOfPresets + 1)
                equalizer!!.usePreset((value - 1).toShort())

            // Retrieve the band levels from the equalizer.
            val levels = equalizer!!.bandLevels

            // Update the bandLevels list with the levels for the current preset.
            repeat(levels.size) { index ->
                eqBandLevels[index] = equalizer!!.getBandLevel(index.toShort()).toFloat()
            }
        }

    override fun onCleared() {
        // when discarded
        equalizer?.release()
        super.onCleared()
    }

    override fun apply() {
        viewModelScope.launch {
            // ignore if not supported.
            // Maybe show Toast for this state.
            if (stateOfEqualizer == AudioFx.EFFECT_STATUS_NOT_READY || stateOfEqualizer == AudioFx.EFFECT_STATUS_NOT_SUPPORTED)
                return@launch
            // equalizer will be release by calling this
            remote.setEqualizer(equalizer)
            showPlatformToast("Equalizer: settings updated and applied")
        }
    }

    //initializer the variables.
    init {
        viewModelScope.launch {
            val result = remote.getEqualizerOrRetry(1)
            // Check if there was a failure when retrieving the equalizer.
            // Optionally, you might consider showing a toast to inform the user of the error.
            // Show a toast message or log an error.
                ?: return@launch
            // The retrieval was successful, get the equalizer instance
            // init the variables
            equalizer = result
            _eqCurrentPreset = result.currentPreset + 1
            eqNumberOfBands = result.numberOfBands.toInt()
            eqBandLevels.addAll(result.bandLevels)
            eqBandLevelRange = result.bandLevelRange.let { it[0].toFloat()..it[1].toFloat() }
            eqPresets = result.presets
            stateOfEqualizer = result.status
        }
    }
}
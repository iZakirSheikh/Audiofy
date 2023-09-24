package com.prime.media.impl

import android.content.ContentResolver
import android.content.res.Resources
import android.media.audiofx.Equalizer
import android.media.audiofx.PresetReverb
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prime.media.core.compose.Channel
import com.prime.media.core.playback.Remote
import com.prime.media.editor.TagEditor
import com.prime.media.effects.AudioFx
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import kotlin.math.abs
import kotlin.math.roundToInt

private const val TAG = "AudioFxViewModel"

private val Equalizer.bandLevels
    get() = List(numberOfBands.toInt()) {
        getBandLevel(it.toShort()).toFloat()
    }

// TODO: Handle the case for not supported.
private val Equalizer.status
    get() = when {
        !hasControl() -> AudioFx.EFFECT_STATUS_NO_CONTROL
        enabled -> AudioFx.EFFECT_STATUS_ENABLED
        else -> AudioFx.EFFECT_STATUS_DISABLED
    }

@HiltViewModel
class AudioFxViewModel @Inject constructor(
    private val remote: Remote
) : ViewModel(), AudioFx {

    // TODO: Find proper way to get this property.
    private val equalizer = runBlocking { remote.getEqualizer(2) }

    private var _eqStatus by mutableIntStateOf(equalizer.status)
    var _eqCurrentPreset: Int by mutableIntStateOf(equalizer.currentPreset.toInt())

    override var isEqualizerEnabled: Boolean
        get() = _eqStatus == AudioFx.EFFECT_STATUS_ENABLED
        set(value) {
            //TODO: Handle case when not supported.
            _eqStatus = if (value) AudioFx.EFFECT_STATUS_ENABLED else AudioFx.EFFECT_STATUS_DISABLED
            equalizer.enabled = value
        }
    override val eqPresets: List<String> =
        List(equalizer.numberOfPresets.toInt()) {
            equalizer.getPresetName(it.toShort())
        }
    override val eqNumberOfBands: Int = equalizer.numberOfBands.toInt()
    override val eqNumPresets: Int = equalizer.numberOfPresets.toInt()
    override var eqCurrentPreset: Int
        get() = _eqCurrentPreset
        set(value) {
            _eqCurrentPreset = value
            // only set preset when in range
            if (value in 0 until equalizer.numberOfPresets)
                equalizer.usePreset(value.toShort())
            val levels = equalizer.bandLevels
            levels.forEachIndexed { index, value ->
                eqBandLevels[index] = equalizer.getBandLevel(index.toShort()).toFloat()
            }
        }

    override val eqBandLevels =
        mutableStateListOf<Float>().also {
            it.addAll(equalizer.bandLevels)
        }
    override val eqBandLevelRange: ClosedFloatingPointRange<Float> =
        equalizer.bandLevelRange.let { it[0].toFloat()..it[1].toFloat() }

    override fun getBandFreqRange(band: Int): ClosedFloatingPointRange<Float> =
        equalizer.getBandFreqRange(band.toShort()).let { it[0].toFloat()..it[1].toFloat() }

    override fun getBandCenterFreq(band: Int): Int =
        equalizer.getCenterFreq(band.toShort())

    override fun setBandLevel(band: Int, level: Float) {
        eqCurrentPreset = -1
        equalizer.setBandLevel(band.toShort(), level.roundToInt().toShort())

        /*repeat(eqNumberOfBands) { b ->
            val value = level * if (b == band) 1.0f else 0.75f / abs(band - b) // distance.
            equalizer.setBandLevel(band.toShort(), value.roundToInt().toShort())
            eqBandLevels[b] = value
        }*/
    }

    override fun apply() {
        viewModelScope.launch {
            // equalizer will be release by calling this
            remote.setEqualizer(equalizer)
        }
    }

    override fun onCleared() {
        equalizer.release()
        super.onCleared()
    }
}
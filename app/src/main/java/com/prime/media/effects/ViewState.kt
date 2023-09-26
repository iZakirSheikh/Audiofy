package com.prime.media.effects

import androidx.compose.runtime.Stable

/**
 * Interface for controlling the state and settings of the AudioFx screen, specifically for Equalizer settings in Android.
 *
 * This interface allows you to manage the state and settings related to the Equalizer in the AudioFx screen.
 * It provides methods for enabling/disabling, selecting presets, and adjusting band levels.
 *
 * @see AudioFx.Companion.direction for usage in [LocalNalController].
 * @see AudioFx.Companion.route for the route to access AudioFx settings.
 */
@Stable
interface AudioFx {

    companion object {
        /** Route to access AudioFx settings. */
        const val route = "audio_fx"

        /**
         * Get the direction to the AudioFx settings.
         *
         * @return The route to AudioFx settings.
         */
        fun direction() = route

        /**
         * Represents that the audio effect is not supported on the device.
         * This means that the device or platform does not provide the capability
         * to use the specific audio effect.
         */
        const val EFFECT_STATUS_NOT_SUPPORTED = -1

        /**
         * Represents that the audio effect is disabled.
         * The effect is available on the device but is currently turned off.
         */
        const val EFFECT_STATUS_DISABLED = 0

        /**
         * Represents that the audio effect is enabled and actively affecting the audio.
         * The effect is currently active and applied to the audio stream.
         */
        const val EFFECT_STATUS_ENABLED = 1

        /**
         * Represents that the audio effect does not have control over the audio.
         * This state typically indicates that the effect cannot modify the audio stream
         * due to various reasons, such as lack of permissions or conflicts with other effects.
         */
        const val EFFECT_STATUS_NO_CONTROL = 2

        /**
         * Represents that the audio effect is not ready for use.
         * This state indicates that the effect is not yet prepared to process audio data.
         * It may require initialization or configuration before it can be enabled.
         */
        const val EFFECT_STATUS_NOT_READY = 3
    }

    /**
     * Represents the state of the equalizer, which can be one of the following values:
     * - [EFFECT_STATUS_NOT_SUPPORTED]: The equalizer is not supported on the device.
     * - [EFFECT_STATUS_DISABLED]: The equalizer is currently disabled.
     * - [EFFECT_STATUS_ENABLED]: The equalizer is currently enabled and actively affecting the audio.
     * - [EFFECT_STATUS_NO_CONTROL]: The equalizer does not have control over the audio.
     * - [EFFECT_STATUS_NOT_READY]: The equalizer is not ready for use and requires initialization or configuration.
     *
     * You can enable or disable the equalizer using the [isEqualizerEnabled] property.
     */
    val stateOfEqualizer: Int

    /**
     * Property for enabling or disabling the Equalizer.
     */
    var isEqualizerEnabled: Boolean

    /**
     * List of supported Equalizer presets.
     */
    val eqPresets: Array<String>

    /**
     * Get the number of supported bands in the Equalizer.
     */
    val eqNumberOfBands: Int

    /**
     * Get the number of available Equalizer presets.
     * This may be greater than the actual default presets of equalizer.
     */
    val eqNumPresets: Int
        get() = eqPresets.size

    /**
     * Get/Sets the currently selected Equalizer preset.
     * If no preset is selected, it will return -1, representing the custom preset.
     *
     *  Note: The index of the preset to apply. It must be -1 for a custom preset or any value from
     *  0 to the number of presets.
     *
     */
    var eqCurrentPreset: Int

    /**
     * List of band levels for the Equalizer.
     * The size of this list corresponds to the number of bands in the Equalizer.
     */
    val eqBandLevels: List<Float>

    /**
     * Range of band level values for the Equalizer.
     *
     * This property represents the valid range of band level values for the Equalizer.
     * The range is specified as a [ClosedFloatingPointRange].
     *
     * @see android.media.audiofx.Equalizer.getBandLevelRange
     */
    val eqBandLevelRange: ClosedFloatingPointRange<Float>

    /**
     * Get the frequency range for a specific Equalizer band.
     *
     * @param band The index of the Equalizer band.
     * @return The frequency range for the specified band as a [ClosedFloatingPointRange].
     * @see android.media.audiofx.Equalizer.getBandFreqRange
     */
    fun getBandFreqRange(band: Int): ClosedFloatingPointRange<Float>

    /**
     * Set the level of a specific Equalizer band.
     *
     * @param band The index of the Equalizer band.
     * @param level The desired level to set for the band.
     */
    fun setBandLevel(band: Int, level: Float)

    /**
     * Gets the center frequency of a specific equalizer band.
     *
     * This function retrieves the center frequency of the equalizer band identified by the [band] parameter.
     *
     * @param band The index of the equalizer band for which to retrieve the center frequency.
     * @return The center frequency of the specified equalizer band in Hertz (Hz).
     * @see android.media.audiofx.Equalizer.getCenterFreq
     */
    fun getBandCenterFreq(band: Int): Int

    /**
     * Applies the configured audio effects to the [Playback] service.
     *
     * This function triggers the transfer of the configured audio effects to the [Playback] service for saving and
     * applying them to the current audio playback.
     */
    fun apply()
}
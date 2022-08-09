package com.prime.player.settings

import com.primex.preferences.*

private const val TAG = "PrefKeys"

object GlobalKeys {
    /**
     * Retrieves/Sets The [NightMode] Strategy
     */
    val NIGHT_MODE = stringPreferenceKey(
        "${TAG}_night_mode",
        NightMode.NO,
        object : StringSaver<NightMode> {
            override fun save(value: NightMode): String = value.name

            override fun restore(value: String): NightMode = NightMode.valueOf(value)
        }
    )

    val FONT_FAMILY = stringPreferenceKey(
        TAG + "_font_family",
        FontFamily.PROVIDED,
        object : StringSaver<FontFamily> {
            override fun save(value: FontFamily): String = value.name

            override fun restore(value: String): FontFamily = FontFamily.valueOf(value)
        }
    )


    val FORCE_COLORIZE =
        booleanPreferenceKey(TAG + "_force_colorize", false)

    val COLOR_STATUS_BAR =
        booleanPreferenceKey(TAG + "_color_status_bar", false)

    val HIDE_STATUS_BAR =
        booleanPreferenceKey(TAG + "_hide_status_bar", false)

    val SHOW_MINI_PROGRESS_BAR =
        booleanPreferenceKey(TAG + "_show_mini_progress_bar", false)

    val FONT_SCALE = floatPreferenceKey(
        TAG + "_font_scale",
        defaultValue = 1.0f
    )
}
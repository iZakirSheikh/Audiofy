package com.prime.player.preferences

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.prime.player.extended.Orange
import com.prime.player.extended.SkyBlue
import com.prime.player.extended.blend
import kotlinx.coroutines.flow.map


enum class NightMode {
    /**
     * Night mode which uses always uses a light mode, enabling {@code notnight} qualified
     * resources regardless of the time.
     *
     * @see #setLocalNightMode(int)
     */
    YES,

    /**
     * Night mode which uses always uses a dark mode, enabling {@code night} qualified
     * resources regardless of the time.
     *
     * @see #setLocalNightMode(int)
     */
    NO,

    /**
     * Mode which uses the system's night mode setting to determine if it is night or not.
     *
     * @see #setLocalNightMode(int)
     */
    FOLLOW_SYSTEM,

    /**
     * Night mode which uses a dark mode when the system's 'Battery Saver' feature is enabled,
     * otherwise it uses a 'light mode'. This mode can help the device to decrease power usage,
     * depending on the display technology in the device.
     *
     * <em>Please note: this mode should only be used when running on devices which do not
     * provide a similar device-wide setting.</em>
     *
     * @see #setLocalNightMode(int)
     */
    AUTO_BATTER,

    /**
     * Night mode which switches between dark and light mode depending on the time of day
     * (dark at night, light in the day).
     *
     * The calculation used to determine whether it is night or not makes use of the location
     * APIs (if this app has the necessary permissions). This allows us to generate accurate
     * sunrise and sunset times. If this app does not have permission to access the location APIs
     * then we use hardcoded times which will be less accurate.
     */
    AUTO_TIME
}

enum class Font {
    // The default typography of app
    SYSTEM_DEFAULT,

    //
    PROVIDED,

    //
    SAN_SERIF,

    //
    SARIF,

    //
    CURSIVE
}

private const val TAG = "PreferencesExt"

const val KEY_PREF_THEME_STATE = TAG + "_theme_state"
const val KEY_PREF_PRIMARY_COLOR = TAG + "_primary_color"
const val KEY_PREF_SECONDARY_COLOR = TAG + "_secondary_color"
const val KEY_PREF_FONT_FAMILY = TAG + "_font_family"
const val KEY_PREF_USE_ACCENT_THOROUGHLY = TAG + "_use_accent_throughly"
const val KEY_PREF_PAINT_STATUS_BAR = TAG + "_paint_status_bar"
const val KEY_PREF_HIDE_STATUS_BAR = TAG + "_hide_status_bar"
const val KEY_PREF_NEW_INSTALL = TAG + "_polluted"
const val SHOW_MINI_PROGRESS_BAR = TAG + "_mini_progress"
const val RECENT_SIZE = TAG + "_recent_size"
const val SHOW_VISUALIZER = TAG + "_show_visualizer"


private val defaultPrimaryColor = Color.Orange
private val defaultPrimaryVariant = defaultPrimaryColor.blend(Color.Black, 0.2f)

private val defaultSecondaryColor = Color.SkyBlue
private val defaultSecondaryVariant = defaultSecondaryColor.blend(Color.Black, 0.2f)

suspend fun Preferences.setDefaultNightMode(mode: NightMode) {
    setString(KEY_PREF_THEME_STATE, mode.name)
}

suspend fun Preferences.setDefaultFont(font: Font) {
    setString(KEY_PREF_FONT_FAMILY, font.name)
}

fun Preferences.getDefaultFont() =
    getString(KEY_PREF_FONT_FAMILY, Font.PROVIDED.name).map { value ->
        Font.valueOf(value)
    }

fun Preferences.getDefaultNightMode() =
    getString(KEY_PREF_THEME_STATE, NightMode.NO.name).map { state ->
        NightMode.valueOf(state)
    }

suspend fun Preferences.setPrimaryColor(color: Color) {
    setInt(KEY_PREF_PRIMARY_COLOR, color.toArgb())
}

fun Preferences.getPrimaryColor() =
    getInt(KEY_PREF_PRIMARY_COLOR, defaultPrimaryColor.toArgb()).map { argb ->
        Color(argb)
    }

suspend fun Preferences.setSecondaryColor(color: Color) {
    setInt(KEY_PREF_SECONDARY_COLOR, color.toArgb())
}

fun Preferences.getSecondaryColor() =
    getInt(KEY_PREF_SECONDARY_COLOR, defaultSecondaryColor.toArgb()).map { argb ->
        Color(argb)
    }

suspend fun Preferences.setHideStatusBar(hide: Boolean) {
    setBoolean(KEY_PREF_HIDE_STATUS_BAR, hide)
}

fun Preferences.isNewInstall() = getBoolean(KEY_PREF_NEW_INSTALL, true)

suspend fun Preferences.flipNewInstall() {
    setBoolean(KEY_PREF_NEW_INSTALL, false)
}

fun Preferences.hideStatusBar() = getBoolean(KEY_PREF_HIDE_STATUS_BAR, false)

suspend fun Preferences.colorStatusBar(should: Boolean) {
    setBoolean(KEY_PREF_PAINT_STATUS_BAR, should)
}

fun Preferences.requiresColoringStatusBar() = getBoolean(
    KEY_PREF_PAINT_STATUS_BAR, false
)

suspend fun Preferences.useAccentThoroughly(should: Boolean) {
    setBoolean(KEY_PREF_USE_ACCENT_THOROUGHLY, should)
}

fun Preferences.requiresAccentThoroughly() = getBoolean(
    KEY_PREF_USE_ACCENT_THOROUGHLY, false
)

suspend fun Preferences.showProgressInMiniPlayer(should: Boolean) {
    setBoolean(SHOW_MINI_PROGRESS_BAR, should)
}

fun Preferences.requiresProgressBarInMiniPlayer() = getBoolean(
    SHOW_MINI_PROGRESS_BAR, false
)

suspend fun Preferences.setRecentSize(size: Int) {
    setInt(RECENT_SIZE, size)
}

fun Preferences.recentSize() = getInt(
    RECENT_SIZE, -1
)

fun Preferences.showVisualizer() = getBoolean(
    SHOW_VISUALIZER, false
)

suspend fun Preferences.setShowVisualizer(should: Boolean) {
    setBoolean(SHOW_VISUALIZER, should)
}





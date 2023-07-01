package com.prime.media.settings

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.prime.media.core.NightMode
import com.primex.core.Text
import com.primex.preferences.Key
import com.primex.preferences.StringSaver
import com.primex.preferences.booleanPreferenceKey
import com.primex.preferences.floatPreferenceKey
import com.primex.preferences.intPreferenceKey
import com.primex.preferences.longPreferenceKey
import com.primex.preferences.stringPreferenceKey
import java.util.concurrent.TimeUnit

private const val PREFIX = "Audiofy"
private const val TAG = "Settings"

interface Settings {

    companion object {
        const val route = "settings"

        /**
         * peek Height of [BottomSheetScaffold], also height of [MiniPlayer]
         */
        val MINI_PLAYER_HEIGHT = 68.dp

        private val defaultMinTrackLimit = TimeUnit.MINUTES.toMillis(1)

        /**
         * Retrieves/Sets The [NightMode] Strategy
         */
        val NIGHT_MODE =
            stringPreferenceKey(
                "${TAG}_night_mode",
                NightMode.FOLLOW_SYSTEM,
                object : StringSaver<NightMode> {
                    override fun save(value: NightMode): String = value.name
                    override fun restore(value: String): NightMode = NightMode.valueOf(value)
                }
            )

        val FORCE_COLORIZE = booleanPreferenceKey(TAG + "_force_colorize", false)
        val COLOR_STATUS_BAR = booleanPreferenceKey(TAG + "_color_status_bar", false)
        val HIDE_STATUS_BAR = booleanPreferenceKey(TAG + "_hide_status_bar", false)
        val FONT_SCALE = floatPreferenceKey(TAG + "_font_scale", defaultValue = 1.0f)



        /**
         * The length/duration of the track in mills considered above which to include
         */
        val EXCLUDE_TRACK_DURATION =
            longPreferenceKey(TAG + "_min_duration_limit_of_track", defaultMinTrackLimit)
        val MAX_RECENT_PLAYLIST_SIZE =
            intPreferenceKey(TAG + "_max_recent_size", defaultValue = 20)

    }

    val darkUiMode: Preference<NightMode>
    val colorStatusBar: Preference<Boolean>
    val hideStatusBar: Preference<Boolean>
    val forceAccent: Preference<Boolean>
    val fontScale: Preference<Float>
    fun <S, O> set(key: Key<S, O>, value: O)
}

@Immutable
data class Preference<out P>(
    val value: P,
    @JvmField val title: Text,
    val vector: ImageVector? = null,
    @JvmField val summery: Text? = null,
)
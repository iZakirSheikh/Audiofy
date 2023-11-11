package com.prime.media.settings

import android.content.Context
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.dp
import com.prime.media.R
import com.prime.media.core.NightMode
import com.prime.media.core.Route
import com.prime.media.core.playback.Playback
import com.primex.core.Text
import com.primex.preferences.Key
import com.primex.preferences.StringSaver
import com.primex.preferences.booleanPreferenceKey
import com.primex.preferences.intPreferenceKey
import com.primex.preferences.stringPreferenceKey
import com.primex.preferences.stringSetPreferenceKey

private const val TAG = "Settings"

@Stable
data class Preference<out P>(
    @JvmField val value: P,
    @JvmField val title: Text,
    @JvmField val vector: ImageVector? = null,
    @JvmField val summery: Text? = null,
)

@OptIn(ExperimentalTextApi::class)
private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

@Stable
private fun FontFamily(name: String) = FontFamily(
    Font(
        fontProvider = provider,
        googleFont = GoogleFont(name),
        weight = FontWeight.Light
    ),
    Font(
        fontProvider = provider,
        googleFont = GoogleFont(name),
        weight = FontWeight.Medium
    ),
    Font(
        fontProvider = provider,
        googleFont = GoogleFont(name),
        weight = FontWeight.Normal
    ),
    Font(
        fontProvider = provider,
        googleFont = GoogleFont(name),
        weight = FontWeight.Bold
    ),
)

@Stable
interface Settings : Blacklist {
    companion object : Route {
        override val route = "settings"
        override val title: Text get() = Text("Settings")
        override val icon: ImageVector get() = Icons.Outlined.Settings

        private const val PREFIX = "Audiofy"

        /**
         * peek Height of [BottomSheetScaffold], also height of [MiniPlayer]
         */
        val MINI_PLAYER_HEIGHT = 68.dp

        val DefaultFontFamily = FontFamily("Roboto")
        val DancingScriptFontFamily = FontFamily("Dancing Script")

        /**
         * Retrieves/Sets The [NightMode] Strategy
         */
        val NIGHT_MODE =
            stringPreferenceKey(
                "${PREFIX}_night_mode",
                NightMode.FOLLOW_SYSTEM,
                object : StringSaver<NightMode> {
                    override fun save(value: NightMode): String = value.name
                    override fun restore(value: String): NightMode = NightMode.valueOf(value)
                }
            )
        val FORCE_COLORIZE = booleanPreferenceKey(PREFIX + "_force_colorize", false)
        val COLOR_STATUS_BAR = booleanPreferenceKey(PREFIX + "_color_status_bar", false)
        val HIDE_STATUS_BAR = booleanPreferenceKey(PREFIX + "_hide_status_bar", false)

        /**
         * The length/duration of the track in mills considered above which to include
         */
        val MIN_TRACK_LENGTH_SECS =
            intPreferenceKey(PREFIX + "_track_duration_", 30)
        val RECENT_PLAYLIST_LIMIT = Playback.PREF_KEY_RECENT_PLAYLIST_LIMIT

        /**
         * The method to use for fetching artwork. default uses legacy (i.e.) MediaStore.
         */
        val USE_LEGACY_ARTWORK_METHOD = booleanPreferenceKey(PREFIX + "_artwork_from_ms", false)
        val TRASH_CAN_ENABLED =
            booleanPreferenceKey(PREFIX + "_trash_can_enabled", defaultValue = true)

        /**
         * The set of files/ folders that have been excluded from media scanning.
         */
        val BLACKLISTED_FILES = stringSetPreferenceKey(PREFIX + "_blacklisted_files")
        val GAPLESS_PLAYBACK = booleanPreferenceKey(PREFIX + "_gap_less_playback")
        val CROSS_FADE_DURATION_SECS = intPreferenceKey(PREFIX + "_cross_fade_tracks_durations")

        /**
         * Determines whether playback should automatically stop when the associated task is removed.
         * If set to true, the playback will be closed when the task is removed.
         * If set to false, the playback will continue even if the task is removed.
         */
        val CLOSE_WHEN_TASK_REMOVED = Playback.PREF_KEY_CLOSE_WHEN_REMOVED

        /**
         * Indicates whether to use the built-in audio effects or third-party audio effects.
         * If set to true, the application will use the built-in audio effects.
         * If set to false, third-party audio effects may be used, if available.
         */
        val USE_IN_BUILT_AUDIO_FX = booleanPreferenceKey(PREFIX + "_use_in_built_audio_fx", true)
    }

    val darkUiMode: Preference<NightMode>
    val colorStatusBar: Preference<Boolean>
    val hideStatusBar: Preference<Boolean>
    val forceAccent: Preference<Boolean>
    val minTrackLength: Preference<Int>
    val recentPlaylistLimit: Preference<Int>
    val fetchArtworkFromMS: Preference<Boolean>
    val enableTrashCan: Preference<Boolean>
    val excludedFiles: Preference<Set<String>?>
    val gaplessPlayback: Preference<Boolean>
    val crossfadeTime: Preference<Int>
    val closePlaybackWhenTaskRemoved: Preference<Boolean>
    val useInbuiltAudioFx: Preference<Boolean>

    fun <S, O> set(key: Key<S, O>, value: O)
}

@Stable
interface Blacklist {
    val values: Set<String>?
    fun unblock(path: String, context: Context)
}
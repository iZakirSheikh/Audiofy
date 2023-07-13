package com.prime.media.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.dp
import com.prime.media.R
import com.prime.media.core.NightMode
import com.prime.media.core.Route
import com.primex.core.Text
import com.primex.preferences.Key
import com.primex.preferences.StringSaver
import com.primex.preferences.booleanPreferenceKey
import com.primex.preferences.intPreferenceKey
import com.primex.preferences.longPreferenceKey
import com.primex.preferences.stringPreferenceKey
import java.util.concurrent.TimeUnit

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
interface Settings {

    companion object : Route {

        override val route = "settings"
        override val title: Text get() = Text("Settings")
        override val icon: ImageVector get() = Icons.Outlined.Settings

        private const val PREFIX = "Audiofy"

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
        val EXCLUDE_TRACK_DURATION =
            longPreferenceKey(PREFIX + "_min_duration_limit_of_track", defaultMinTrackLimit)
        val MAX_RECENT_PLAYLIST_SIZE =
            intPreferenceKey(PREFIX + "_max_recent_size", defaultValue = 20)

        val LatoFontFamily = FontFamily(
            Font(fontProvider = provider, googleFont = GoogleFont("Lato"))
        )
    }

    val darkUiMode: Preference<NightMode>
    val colorStatusBar: Preference<Boolean>
    val hideStatusBar: Preference<Boolean>
    val forceAccent: Preference<Boolean>

    fun <S, O> set(key: Key<S, O>, value: O)
}


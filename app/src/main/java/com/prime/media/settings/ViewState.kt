package com.prime.media.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.dp
import com.prime.media.BuildConfig
import com.prime.media.R
import com.prime.media.core.NightMode
import com.prime.media.core.playback.Playback
import com.primex.core.Text
import com.primex.preferences.IntSaver
import com.primex.preferences.Key
import com.primex.preferences.LongSaver
import com.primex.preferences.StringSaver
import com.primex.preferences.booleanPreferenceKey
import com.primex.preferences.floatPreferenceKey
import com.primex.preferences.intPreferenceKey
import com.primex.preferences.longPreferenceKey
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

private val ColorSaver =   object : LongSaver<Color> {
    override fun restore(value: Long): Color = Color(value)
    override fun save(value: Color): Long = value.value.toLong()
}

/**
 * Represents the available strategies for extracting a source color accent to construct a theme.
 */
enum class ColorizationStrategy {
    Manual, Default, Wallpaper, Artwork
}

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
    companion object {
        val route = "settings"

        val FeedbackIntent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:helpline.prime.zs@gmail.com")
            putExtra(Intent.EXTRA_SUBJECT, "Feedback/Suggestion for Audiofy")
        }
        val PrivacyPolicyIntent = Intent(Intent.ACTION_VIEW).apply {
            data =
                Uri.parse("https://docs.google.com/document/d/1AWStMw3oPY8H2dmdLgZu_kRFN-A8L6PDShVuY8BAhCw/edit?usp=sharing")
        }
        val GitHubIssuesPage = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://github.com/iZakirSheikh/Audiofy/issues")
        }
        val TelegramIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://t.me/audiofy_support")
        }
        val GithubIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://github.com/iZakirSheikh/Audiofy")
        }
        val JoinBetaIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse("https://play.google.com/apps/testing/com.prime.player/join")
        }

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
                NightMode.YES,
                object : StringSaver<NightMode> {
                    override fun save(value: NightMode): String = value.name
                    override fun restore(value: String): NightMode = NightMode.valueOf(value)
                }
            )
        val TRANSLUCENT_SYSTEM_BARS = booleanPreferenceKey(PREFIX + "_force_colorize", true)
        val IMMERSIVE_VIEW = booleanPreferenceKey(PREFIX + "_hide_status_bar", false)

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
            booleanPreferenceKey(PREFIX + "_trash_can_enabled", defaultValue = false)

        /**
         * The set of files/ folders that have been excluded from media scanning.
         */
        val BLACKLISTED_FILES = stringSetPreferenceKey(PREFIX + "_blacklisted_files")
        val GAP_LESS_PLAYBACK = booleanPreferenceKey(PREFIX + "_gap_less_playback")
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

        /**
         * Preference key for the grid item size multiplier (0.6 - 2.0f).
         * Adjust to make grid items smaller or larger.
         */
        val GRID_ITEM_SIZE_MULTIPLIER =
            floatPreferenceKey(PREFIX + "_grid_item_size_multiplier", defaultValue = 1.0f)

        /**
         * Preference key for font scale (0.8f - 2.0f).
         * Adjust to control font size relative to system default (-1f).
         */
        val FONT_SCALE = floatPreferenceKey(PREFIX + "_font_scale", -1f)


        val COLORIZATION_STRATEGY = intPreferenceKey(
            "${PREFIX}_colorization_strategy",
            ColorizationStrategy.Default,
            object : IntSaver<ColorizationStrategy> {
                override fun restore(value: Int): ColorizationStrategy {
                    return ColorizationStrategy.entries[value]
                }

                override fun save(value: ColorizationStrategy): Int {
                    return value.ordinal
                }
            }
        )

        /**
         * Preference key for controlling the content displayed in the console's signature field.
         *
         * Possible values:
         * - `""`: Displays the default signature(the app's name).
         * - `null` (empty string): Displays nothing in the signature field.
         * - Any other string value: Displays the specified custom signature.
         */
        val SIGNATURE = stringPreferenceKey("${PREFIX}_signature")

        /**
         * Preference key for controlling the border width of artwork.
         *
         * Possible values:
         * - `null`:  Artwork is displayed without a border.
         * - Non-null integer: Represents the border width in dp (density-independent pixels).
         */
        val ARTWORK_BORDER_WIDTH = intPreferenceKey("${PREFIX}_artwork_border_width")

        /**
         * Preference key for storing the user's selected accent color for light theme.
         *
         * The color is stored as a Long value representing the ARGB color integer.
         * A value of [Color.Unspecified] indicates that no accent color has been explicitly set.
         */
        val COLOR_ACCENT_LIGHT = longPreferenceKey(
            "${PREFIX}_color_accent_light",
            Color.Unspecified,
            ColorSaver
        )

        /**
         * @see COLOR_ACCENT_LIGHT
         */
        val COLOR_ACCENT_DARK = longPreferenceKey(
            "${PREFIX}_color_accent_dark",
            Color.Unspecified,
            ColorSaver
        )

        /**
         * Stores the ID of the currently selected glance widget version.
         * This ID corresponds to an available In-App Purchase (IAP) product
         * Defaults to the iPhone-inspired widget ([BuildConfig.IAP_WIDGET_PLATFORM_IPHONE]).
         */
        val GLANCE = stringPreferenceKey(
            "${PREFIX}_glance",
            BuildConfig.IAP_PLATFORM_WIDGET_IPHONE
        )
    }

    val darkUiMode: Preference<NightMode>
    val translucentSystemBars: Preference<Boolean>
    val immersiveView: Preference<Boolean>
    val minTrackLength: Preference<Int>
    val recentPlaylistLimit: Preference<Int>
    val fetchArtworkFromMS: Preference<Boolean>
    val enableTrashCan: Preference<Boolean>
    val excludedFiles: Preference<Set<String>?>
    val gaplessPlayback: Preference<Boolean>
    val crossfadeTime: Preference<Int>
    val closePlaybackWhenTaskRemoved: Preference<Boolean>
    val useInbuiltAudioFx: Preference<Boolean>
    val fontScale: Preference<Float>
    val gridItemSizeMultiplier: Preference<Float>
    val colorizationStrategy: Preference<ColorizationStrategy>

    fun <S, O> set(key: Key<S, O>, value: O)
}

@Stable
interface Blacklist {
    val values: Set<String>?
    fun unblock(path: String, context: Context)
}
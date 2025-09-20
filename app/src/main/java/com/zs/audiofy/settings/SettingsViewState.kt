package com.zs.audiofy.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import com.zs.audiofy.R
import com.zs.audiofy.common.ColorizationStrategy
import com.zs.audiofy.common.IAP_ARTWORK_SHAPE_ROUNDED_RECT
import com.zs.audiofy.common.IAP_PLATFORM_WIDGET_IPHONE
import com.zs.audiofy.common.NightMode
import com.zs.audiofy.common.SystemFacade
import com.zs.audiofy.settings.Settings.GitHubIssuesPage
import com.zs.audiofy.settings.Settings.GithubIntent
import com.zs.audiofy.settings.Settings.PrivacyPolicyIntent
import com.zs.audiofy.settings.Settings.TelegramIntent
import com.zs.core.billing.Paymaster
import com.zs.core.common.Intent
import com.zs.preferences.IntSaver
import com.zs.preferences.Key
import com.zs.preferences.StringSaver
import com.zs.preferences.booleanPreferenceKey
import com.zs.preferences.intPreferenceKey
import com.zs.preferences.stringPreferenceKey

private const val TAG = "Settings"



private val provider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

/**
 * Creates a [FontFamily] from the given Google Font name.
 *
 * @param name The name of theGoogle Font to use.
 * @return A [FontFamily] object
 */
@Stable
private fun FontFamily(name: String): FontFamily {
    // Create a GoogleFont object from the given name.
    val font = GoogleFont(name)
    // Create a FontFamily object with four different font weights.
    return FontFamily(
        Font(
            fontProvider = provider,
            googleFont = font,
            weight = FontWeight.Light
        ),
        Font(
            fontProvider = provider,
            googleFont = font,
            weight = FontWeight.Medium
        ),
        Font(
            fontProvider = provider,
            googleFont = font,
            weight = FontWeight.Normal
        ),
        Font(
            fontProvider = provider,
            googleFont = font,
            weight = FontWeight.Bold
        ),
    )
}

private val OutfitFontFamily = FontFamily("Outfit")
val FontFamily.Companion.OutfitFontFamily get() = com.zs.audiofy.settings.OutfitFontFamily
private val RobotoFontFamily = FontFamily("Roboto")
val FontFamily.Companion.RobotoFontFamily get() = com.zs.audiofy.settings.RobotoFontFamily
val DancingScriptFontFamily = FontFamily("Dancing Script")
val FontFamily.Companion.DancingScriptFontFamily get() = com.zs.audiofy.settings.DancingScriptFontFamily

/**
 * ##### Settings
 *
 * This object contains various preference keys and their default values used throughout the app.
 *
 * @property FeedbackIntent Intent to send feedback via email.
 * @property PrivacyPolicyIntent Intent to view the privacy policy document.
 * @property GitHubIssuesPage Intent to view the GitHub issues page.
 * @property TelegramIntent Intent to open the Telegram support channel.
 * @property GithubIntent Intent to view the GitHub repository.
 * @property MIN_TRACK_LENGTH_SECS The length/duration of the track in mills considered above which to include
 * @property USE_LEGACY_ARTWORK_METHOD The method to use for fetching artwork. default uses legacy (i.e.) MediaStore.
 * @property TRASH_CAN_ENABLED Preference key for enabling trash can feature.
 * @property BLACKLISTED_FILES The set of files/ folders that have been excluded from media scanning.
 *
 * @property USE_IN_BUILT_AUDIO_FX Indicates whether to use the built-in audio effects or third-party audio effects.
 *
 * * If set to true, the application will use the built-in audio effects.
 * * If set to false, third-party audio effects may be used, if available.
 * @property GRID_ITEM_SIZE_MULTIPLIER Preference key for the grid item size multiplier (0.6 - 2.0f).
 * Adjust to make grid items smaller or larger.
 *
 */
object Settings {
    //
    const val PREFIX_MARKET_URL = "market://details?id="
    const val PREFIX_MARKET_FALLBACK = "http://play.google.com/store/apps/details?id="
    const val PKG_MARKET_ID = "com.android.vending"

    // Default Accents
    val LightAccentColor = Color(0xFF514700)
    val DarkAccentColor = /*Color(0xFFD8A25E)*/ Color(0xFF7d6c1e)

    //
    val ColorSaver = object : IntSaver<Color> {
        override fun restore(value: Int): Color = Color(value)
        override fun save(value: Color): Int = value.toArgb()
    }

    // Intents
    val PrivacyPolicyIntent = Intent(Intent.ACTION_VIEW) {
        data =
            Uri.parse("https://docs.google.com/document/d/1AWStMw3oPY8H2dmdLgZu_kRFN-A8L6PDShVuY8BAhCw/edit?usp=sharing")
    }
    val GitHubIssuesPage = Intent(Intent.ACTION_VIEW) {
        data = Uri.parse("https://github.com/iZakirSheikh/Audiofy/issues")
    }
    val TelegramIntent = Intent(Intent.ACTION_VIEW) {
        data = Uri.parse("https://t.me/audiofy_support")
    }
    val GithubIntent = Intent(Intent.ACTION_VIEW) {
        data = Uri.parse("https://github.com/iZakirSheikh/Audiofy")
    }
    val JoinBetaIntent = Intent(Intent.ACTION_VIEW) {
        data = Uri.parse("https://play.google.com/apps/testing/com.prime.player/join")
    }
    val ShareAppIntent = Intent(Intent.ACTION_SEND) {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "Hey, check out this cool app: [app link here]")
    }
    val TranslateIntent = Intent(Intent.ACTION_VIEW) {
        data = Uri.parse("https://crowdin.com/project/audiofy")
    }

    // Keys
    private const val PREFIX = "Audiofy"
    val NIGHT_MODE =
        stringPreferenceKey(
            "${PREFIX}_night_mode",
            NightMode.YES,
            object : StringSaver<NightMode> {
                override fun save(value: NightMode): String = value.name
                override fun restore(value: String): NightMode = NightMode.valueOf(value)
            }
        )

    // For Android versions below 10 (API level 29), this is true by default, meaning
    // system bars are translucent and cannot be toggled.
    //
    // In Android 15 (API level 34), this preference is deprecated and no longer functional,
    // as system bar translucency is managed by the system and cannot be customized.
    //
    // For intermediate versions (between Android 10 and Android 15), this setting is false
    // by default but can be toggled to enable or disable translucent system bars based
    // on user preferences.
    val TRANSPARENT_SYSTEM_BARS =
        booleanPreferenceKey(
            PREFIX + "_force_colorize",
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
        )

    //val CLOSE_WHEN_TASK_REMOVED = Playback.PREF_KEY_CLOSE_WHEN_REMOVED
    val COLORIZATION_STRATEGY = intPreferenceKey(
        "${PREFIX}_colorization_strategy",
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) ColorizationStrategy.Wallpaper else ColorizationStrategy.Default,
        object : IntSaver<ColorizationStrategy> {
            override fun restore(value: Int): ColorizationStrategy {
                return ColorizationStrategy.entries[value]
            }

            override fun save(value: ColorizationStrategy): Int {
                return value.ordinal
            }
        }
    )

    val COLOR_ACCENT_LIGHT =
        intPreferenceKey("${PREFIX}_color_accent_light", LightAccentColor, ColorSaver)
    val COLOR_ACCENT_DARK =
        intPreferenceKey("${PREFIX}_color_accent_dark", DarkAccentColor, ColorSaver)
    val GLANCE =
        stringPreferenceKey("${PREFIX}_glance", Paymaster.IAP_PLATFORM_WIDGET_IPHONE)
    val KEY_LAUNCH_COUNTER =
        intPreferenceKey("Audiofy_launch_counter", 0)
    val USE_ACCENT_IN_NAV_BAR = booleanPreferenceKey("use_accent_in_nav_bar", false)
    val KEY_APP_CONFIG = stringPreferenceKey("${PREFIX}_app_config")


    /** @see mapKeyToShape */
    val ARTWORK_SHAPE_KEY =
        stringPreferenceKey(
            "${PREFIX}_artwork_shape_key",
            Paymaster.IAP_ARTWORK_SHAPE_ROUNDED_RECT,
        )

    val DefaultFontFamily = FontFamily.Default
}

/**
 * Represents the state of the Settings screen.
 */
@Stable
interface SettingsViewState {

    val save: Boolean

    var trashCanEnabled: Boolean
    var preferCachedThumbnails: Boolean
    var enabledBackgroundBlur: Boolean
    var fontScale: Float
    var minTrackLengthSecs: Int
    var shouldUseSystemAudioEffects: Boolean
    var gridItemSizeMultiplier: Float

    /**
     * Commits [AppConfig] to memory.
     */
    fun commit(facade: SystemFacade)

    fun discard()

    /** Sets the value of the given [key] to [value]. */
    fun <S, O> set(key: Key<S, O>, value: O)
}
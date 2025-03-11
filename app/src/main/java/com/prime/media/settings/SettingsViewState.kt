package com.prime.media.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.dp
import com.prime.media.BuildConfig
import com.prime.media.R
import com.prime.media.common.Route
import com.prime.media.settings.Settings.BLACKLISTED_FILES
import com.prime.media.settings.Settings.FeedbackIntent
import com.prime.media.settings.Settings.GRID_ITEM_SIZE_MULTIPLIER
import com.prime.media.settings.Settings.GitHubIssuesPage
import com.prime.media.settings.Settings.GithubIntent
import com.prime.media.settings.Settings.MIN_TRACK_LENGTH_SECS
import com.prime.media.settings.Settings.PrivacyPolicyIntent
import com.prime.media.settings.Settings.TRASH_CAN_ENABLED
import com.prime.media.settings.Settings.TelegramIntent
import com.prime.media.settings.Settings.USE_IN_BUILT_AUDIO_FX
import com.prime.media.settings.Settings.USE_LEGACY_ARTWORK_METHOD
import com.primex.core.shapes.SquircleShape
import com.primex.preferences.IntSaver
import com.primex.preferences.Key
import com.primex.preferences.StringSaver
import com.primex.preferences.booleanPreferenceKey
import com.primex.preferences.floatPreferenceKey
import com.primex.preferences.intPreferenceKey
import com.primex.preferences.stringPreferenceKey
import com.primex.preferences.stringSetPreferenceKey
import com.zs.core_ui.NightMode
import com.zs.core_ui.shape.CompactDisk
import com.zs.core_ui.shape.HeartShape
import com.zs.core_ui.shape.NotchedCornerShape
import com.zs.core_ui.shape.RoundedPolygonShape
import com.zs.core_ui.shape.RoundedStarShape
import com.zs.core_ui.shape.SkewedRoundedRectangleShape

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
val FontFamily.Companion.OutfitFontFamily get() = com.prime.media.settings.OutfitFontFamily
private val RobotoFontFamily = FontFamily("Roboto")
val FontFamily.Companion.RobotoFontFamily get() = com.prime.media.settings.RobotoFontFamily
val DancingScriptFontFamily = FontFamily("Dancing Script")
val FontFamily.Companion.DancingScriptFontFamily get() = com.prime.media.settings.DancingScriptFontFamily

object RouteSettings : Route

/**
 * Represents the available strategies for extracting a source color accent to construct a theme.
 */
enum class ColorizationStrategy {
    Manual, Default, Wallpaper, Artwork
}

private val ColorSaver = object : IntSaver<Color> {
    override fun restore(value: Int): Color = Color(value)
    override fun save(value: Color): Int = value.toArgb()
}

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
    val ShareAppIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "Hey, check out this cool app: [app link here]")
    }
    val TranslateIntent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("https://crowdin.com/project/audiofy")
    }

    private const val PREFIX = "Audiofy"

    private val ArtworkShapeRoundedRect = RoundedCornerShape(15)
    private val ArtworkShapeHeart = HeartShape
    private val ArtworkShapeDisk = CompactDisk
    private val ArtworkShapeCircle = CircleShape
    private val ArtworkShapeSkewedRect = SkewedRoundedRectangleShape(15.dp, 0.15f)
    private val ArtworkShapeLeaf = RoundedCornerShape(20, 4, 20, 4)
    private val ArtworkShapePentagon = RoundedPolygonShape(5, 0.3f)
    private val ArtworkShapeWavyCircle = RoundedStarShape(15, 0.03)
    private val ArtworkShapeCutCorneredRect = CutCornerShape(15)
    private val ArtworkShapeScopedRect = NotchedCornerShape(30.dp)
    private val ArtworkShapeSquircle = SquircleShape(0.7f)

    val LightAccentColor = Color(0xFF514700)
    val DarkAccentColor = Color(0xFFD8A25E)

    val NIGHT_MODE =
        stringPreferenceKey(
            "${PREFIX}_night_mode",
            NightMode.YES,
            object : StringSaver<NightMode> {
                override fun save(value: NightMode): String = value.name
                override fun restore(value: String): NightMode = NightMode.valueOf(value)
            }
        )

    fun mapKeyToShape(key: String): Shape =
        when(key){
            BuildConfig.IAP_ARTWORK_SHAPE_ROUNDED_RECT -> ArtworkShapeRoundedRect
            BuildConfig.IAP_ARTWORK_SHAPE_HEART -> ArtworkShapeHeart
            BuildConfig.IAP_ARTWORK_SHAPE_DISK -> ArtworkShapeDisk
            BuildConfig.IAP_ARTWORK_SHAPE_CIRCLE -> ArtworkShapeCircle
            BuildConfig.IAP_ARTWORK_SHAPE_SKEWED_RECT -> ArtworkShapeSkewedRect
            BuildConfig.IAP_ARTWORK_SHAPE_LEAF -> ArtworkShapeLeaf
            BuildConfig.IAP_ARTWORK_SHAPE_PENTAGON -> ArtworkShapePentagon
            BuildConfig.IAP_ARTWORK_SHAPE_WAVY_CIRCLE-> ArtworkShapeWavyCircle
            BuildConfig.IAP_ARTWORK_SHAPE_CUT_CORNORED_RECT -> ArtworkShapeCutCorneredRect
            BuildConfig.IAP_ARTWORK_SHAPE_SCOPED_RECT -> ArtworkShapeScopedRect
            BuildConfig.IAP_ARTWORK_SHAPE_SQUIRCLE -> ArtworkShapeSquircle
            else -> error("$key is not among shapes mentioned in Settings.")
        }


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
    val IMMERSIVE_VIEW =
        booleanPreferenceKey(PREFIX + "_hide_status_bar", false)
    val MIN_TRACK_LENGTH_SECS =
        intPreferenceKey(PREFIX + "_track_duration_", 30)
    val USE_LEGACY_ARTWORK_METHOD =
        booleanPreferenceKey(PREFIX + "_artwork_from_ms", false)
    val TRASH_CAN_ENABLED =
        booleanPreferenceKey(PREFIX + "_trash_can_enabled", defaultValue = false)
    val BLACKLISTED_FILES =
        stringSetPreferenceKey(PREFIX + "_blacklisted_files")
    val GAP_LESS_PLAYBACK =
        booleanPreferenceKey(PREFIX + "_gap_less_playback")
    val CROSS_FADE_DURATION_SECS =
        intPreferenceKey(PREFIX + "_cross_fade_tracks_durations")

    //val CLOSE_WHEN_TASK_REMOVED = Playback.PREF_KEY_CLOSE_WHEN_REMOVED
    val USE_IN_BUILT_AUDIO_FX =
        booleanPreferenceKey(PREFIX + "_use_in_built_audio_fx", true)
    val GRID_ITEM_SIZE_MULTIPLIER =
        floatPreferenceKey(PREFIX + "_grid_item_size_multiplier", defaultValue = 1.0f)
    val FONT_SCALE =
        floatPreferenceKey(PREFIX + "_font_scale", -1f)
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

    val SIGNATURE =
        stringPreferenceKey("${PREFIX}_signature")
    val ARTWORK_BORDERED =
        booleanPreferenceKey("${PREFIX}_artwork_bordered", false)

    val ARTWORK_ELEVATED = booleanPreferenceKey("${PREFIX}_artwork_elevated", true)
    val COLOR_ACCENT_LIGHT =
        intPreferenceKey("${PREFIX}_color_accent_light", LightAccentColor, ColorSaver)
    val COLOR_ACCENT_DARK =
        intPreferenceKey("${PREFIX}_color_accent_dark", DarkAccentColor, ColorSaver)
    val GLANCE =
        stringPreferenceKey("${PREFIX}_glance", BuildConfig.IAP_PLATFORM_WIDGET_IPHONE)
    val KEY_LAUNCH_COUNTER =
        intPreferenceKey("Audiofy_launch_counter")
    val USE_ACCENT_IN_NAV_BAR = booleanPreferenceKey("use_accent_in_nav_bar", false)

    /** @see mapKeyToShape */
    val ARTWORK_SHAPE_KEY =
        stringPreferenceKey(
            "${PREFIX}_artwork_shape_key",
            BuildConfig.IAP_ARTWORK_SHAPE_ROUNDED_RECT,
        )

    const val GOOGLE_STORE = "market://details?id=" + BuildConfig.APPLICATION_ID
    const val FALLBACK_GOOGLE_STORE =
        "http://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID
    const val PKG_GOOGLE_PLAY_STORE = "com.android.vending"

    val DefaultFontFamily = FontFamily.Default
}

@Stable
interface SettingsViewState {
    fun <S, O> set(key: Key<S, O>, value: O)
}





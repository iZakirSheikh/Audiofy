/*  Copyright (c) 2025 Zakir Sheikh
*
*  Created by Zakir Sheikh on $today.date.
*
*  Licensed under the Apache License, Version 2.0 (the "License");
*  you may not use this file except in compliance with the License.
*  You may obtain a copy of the License at
*
*   https://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS,
*  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
*   See the License for the specific language governing permissions and
*   limitations under the License.
*/

package com.prime.media.settings

import android.annotation.SuppressLint
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
import com.prime.media.common.SystemFacade
import com.prime.media.settings.Settings.BLACKLISTED_FILES
import com.prime.media.settings.Settings.FeedbackIntent
import com.prime.media.settings.Settings.GitHubIssuesPage
import com.prime.media.settings.Settings.GithubIntent
import com.prime.media.settings.Settings.PrivacyPolicyIntent
import com.prime.media.settings.Settings.TelegramIntent
import com.prime.media.settings.Settings.mapKeyToShape
import com.primex.core.Amber
import com.primex.core.BlackOlive
import com.primex.core.BlueLilac
import com.primex.core.ClaretViolet
import com.primex.core.MetroGreen
import com.primex.core.MetroGreen2
import com.primex.core.OliveYellow
import com.primex.core.RedViolet
import com.primex.core.SepiaBrown
import com.primex.core.SkyBlue
import com.primex.core.shapes.SquircleShape
import com.primex.preferences.IntSaver
import com.primex.preferences.Key
import com.primex.preferences.StringSaver
import com.primex.preferences.booleanPreferenceKey
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

/**
 * Represents the available strategies for extracting a source color accent to construct a theme.
 */
enum class ColorizationStrategy {
    Manual, Default, Wallpaper, Artwork
}

// Defines the color saver that serializes the color
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
    const val GOOGLE_STORE = "market://details?id=" + BuildConfig.APPLICATION_ID
    const val FALLBACK_GOOGLE_STORE =
        "http://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID
    const val PKG_GOOGLE_PLAY_STORE = "com.android.vending"

    val DefaultFontFamily = FontFamily.Default

    // Some common intents
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

    val LightAccentColor = Color.ClaretViolet
    val DarkAccentColor = Color.OliveYellow

    val NIGHT_MODE =
        stringPreferenceKey(
            "${PREFIX}_night_mode",
            NightMode.YES,
            object : StringSaver<NightMode> {
                override fun save(value: NightMode): String = value.name
                override fun restore(value: String): NightMode = NightMode.valueOf(value)
            }
        )

    /**
     *
     */
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
    val BLACKLISTED_FILES =
        stringSetPreferenceKey(PREFIX + "_blacklisted_files")
    val GAP_LESS_PLAYBACK =
        booleanPreferenceKey(PREFIX + "_gap_less_playback")
    val CROSS_FADE_DURATION_SECS =
        intPreferenceKey(PREFIX + "_cross_fade_tracks_durations")

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
    val USE_ACCENT_IN_NAV_BAR = booleanPreferenceKey("use_accent_in_nav_bar", true)

    /** @see mapKeyToShape */
    val ARTWORK_SHAPE_KEY =
        stringPreferenceKey(
            "${PREFIX}_artwork_shape_key",
            BuildConfig.IAP_ARTWORK_SHAPE_ROUNDED_RECT,
        )

    val KEY_APP_CONFIG = stringPreferenceKey("${PREFIX}_app_config")

    /**
     * List of permissions required to run the app.
     *
     * This list is constructed based on the device's Android version to ensure
     * compatibility with scoped storage and legacy storage access.
     */
    @SuppressLint("BuildListAdds")
    val REQUIRED_PERMISSIONS = buildList {
        // For Android Tiramisu (33) and above, use media permissions for scoped storage
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            this += android.Manifest.permission.ACCESS_MEDIA_LOCATION
            this += android.Manifest.permission.READ_MEDIA_VIDEO
            this += android.Manifest.permission.READ_MEDIA_AUDIO
        }
        // For Android Upside Down Cake (34) and above, add permission for user-selected visual media
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            this += android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        // For Android versions below Tiramisu 10(29), request WRITE_EXTERNAL_STORAGE for
        // legacy storage access
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q)
            this += android.Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            this += android.Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }
}

@Stable
interface SettingsViewState {

    val save: Boolean

    var trashCanEnabled: Boolean
    var preferCachedThumbnails: Boolean
    var enabledBackgroundBlur: Boolean
    var fontScale: Float
    var minTrackLengthSecs: Int
    var inAppAudioEffectsEnabled: Boolean
    var gridItemSizeMultiplier: Float
    var fabLongPressLaunchConsole: Boolean
    var isFileGroupingEnabled: Boolean

    /**
     * Commits [AppConfig] to memory.
     */
    fun commit(facade: SystemFacade)

    fun discard()

    /** Sets the value of the given [key] to [value]. */
    fun <S, O> set(key: Key<S, O>, value: O)
}
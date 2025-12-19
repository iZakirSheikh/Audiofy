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

package com.prime.media.common

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.googlefonts.GoogleFont
import androidx.compose.ui.unit.dp
import com.prime.media.BuildConfig
import com.prime.media.R
import com.prime.media.common.Registry.ARTWORK_BORDERED
import com.prime.media.common.Registry.ARTWORK_ELEVATED
import com.prime.media.common.Registry.ARTWORK_SHAPE_KEY
import com.prime.media.common.Registry.BLACKLISTED_FILES
import com.prime.media.common.Registry.COLORIZATION_STRATEGY
import com.prime.media.common.Registry.COLOR_ACCENT_DARK
import com.prime.media.common.Registry.COLOR_ACCENT_LIGHT
import com.prime.media.common.Registry.CROSS_FADE_DURATION_SECS
import com.prime.media.common.Registry.ColorSaver
import com.prime.media.common.Registry.DancingScriptFontFamily
import com.prime.media.common.Registry.DarkAccentColor
import com.prime.media.common.Registry.DefaultFontFamily
import com.prime.media.common.Registry.FALLBACK_GOOGLE_STORE
import com.prime.media.common.Registry.FeedbackIntent
import com.prime.media.common.Registry.GAP_LESS_PLAYBACK
import com.prime.media.common.Registry.GLANCE
import com.prime.media.common.Registry.GOOGLE_STORE
import com.prime.media.common.Registry.GitHubIssuesPage
import com.prime.media.common.Registry.GithubIntent
import com.prime.media.common.Registry.JoinBetaIntent
import com.prime.media.common.Registry.KEY_APP_CONFIG
import com.prime.media.common.Registry.KEY_LAUNCH_COUNTER
import com.prime.media.common.Registry.LightAccentColor
import com.prime.media.common.Registry.NIGHT_MODE
import com.prime.media.common.Registry.OutfitFontFamily
import com.prime.media.common.Registry.PKG_GOOGLE_PLAY_STORE
import com.prime.media.common.Registry.PrivacyPolicyIntent
import com.prime.media.common.Registry.RobotoFontFamily
import com.prime.media.common.Registry.ShareAppIntent
import com.prime.media.common.Registry.TRANSPARENT_SYSTEM_BARS
import com.prime.media.common.Registry.TelegramIntent
import com.prime.media.common.Registry.TranslateIntent
import com.prime.media.common.Registry.USE_ACCENT_IN_NAV_BAR
import com.prime.media.common.Registry.mapKeyToShape
import com.prime.media.common.Registry.provider
import com.primex.core.ClaretViolet
import com.primex.core.OliveYellow
import com.primex.core.shapes.SquircleShape
import com.primex.preferences.IntSaver
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
import kotlin.Triple

/**
 * ##### Registry
 *
 * Centralized object that holds constants, reusable intents, font providers,
 * artwork shapes, accent colors, and preference keys used throughout the Audiofy app.
 *
 * ---
 * ### Play Store
 * @property GOOGLE_STORE            Play Store deep link for the app.
 * @property FALLBACK_GOOGLE_STORE   Fallback HTTP Play Store link for the app.
 * @property PKG_GOOGLE_PLAY_STORE   Package name of Google Play Store.
 *
 * ---
 * ### Fonts
 * @property provider                 Google Fonts provider configuration.
 * @property OutfitFontFamily         Font family for "Outfit".
 * @property RobotoFontFamily         Font family for "Roboto".
 * @property DancingScriptFontFamily  Font family for "Dancing Script".
 * @property DefaultFontFamily        Default system font family.
 *
 * ---
 * ### Persistence
 * @property ColorSaver Saver for persisting and restoring [Color] values in preferences.
 *
 * ---
 * ### Intents
 * @property FeedbackIntent       Intent to send feedback via email.
 * @property PrivacyPolicyIntent  Intent to view the privacy policy document.
 * @property GitHubIssuesPage     Intent to view the GitHub issues page.
 * @property TelegramIntent       Intent to open the Telegram support channel.
 * @property GithubIntent         Intent to view the GitHub repository.
 * @property JoinBetaIntent       Intent to join the Play Store beta program.
 * @property ShareAppIntent       Intent to share the app link.
 * @property TranslateIntent      Intent to open the Crowdin translation project.
 *
 * ---
 * ### Accent Colors
 * @property LightAccentColor Default light accent color.
 * @property DarkAccentColor  Default dark accent color.
 *
 * ---
 * ### Preferences
 * @property NIGHT_MODE                 Preference key for night mode setting.
 * @property TRANSPARENT_SYSTEM_BARS    Preference key for system bar translucency behavior.
 * @property BLACKLISTED_FILES          Preference key for excluded files/folders from media scanning.
 * @property GAP_LESS_PLAYBACK          Preference key for enabling gapless playback.
 * @property CROSS_FADE_DURATION_SECS   Preference key for crossfade duration between tracks.
 * @property COLORIZATION_STRATEGY      Preference key for artwork colorization strategy.
 * @property ARTWORK_BORDERED           Preference key for enabling artwork borders.
 * @property ARTWORK_ELEVATED           Preference key for enabling artwork elevation.
 * @property COLOR_ACCENT_LIGHT         Preference key for light accent color.
 * @property COLOR_ACCENT_DARK          Preference key for dark accent color.
 * @property GLANCE                     Preference key for widget platform style.
 * @property KEY_LAUNCH_COUNTER         Preference key for tracking app launches.
 * @property USE_ACCENT_IN_NAV_BAR      Preference key for accent color usage in navigation bar.
 * @property ARTWORK_SHAPE_KEY          Preference key for selected artwork shape.
 * @property KEY_APP_CONFIG             Preference key for app configuration.
 *
 * ---
 * ### Featured Apps
 * @property featuredApps A curated list of featured apps published by the developer.
 * Each entry is represented as a [Triple] containing:
 * 1. **App name** – the human‑readable title of the app.
 * 2. **Icon URL** – a direct link to the app’s promotional image or icon.
 * 3. **Package name** – the unique identifier used on the Play Store.
 *
 * This list can be used to showcase other apps inside the UI,
 * for cross‑promotion, or to provide quick navigation links.
 *
 * Example usage:
 * ```
 * featuredApps.forEach { (name, iconUrl, packageName) ->
 *     // Display app card with name, icon, and launch intent
 * }
 * ```
 *
 * ---
 * ### Functions
 * // Add function documentation here
 */
object Registry {
    // --- Store links and package identifiers ---
    const val GOOGLE_STORE = "market://details?id=" + BuildConfig.APPLICATION_ID
    const val FALLBACK_GOOGLE_STORE =
        "http://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID
    const val PKG_GOOGLE_PLAY_STORE = "com.android.vending"

    // --- Font provider and families ---
    val provider = GoogleFont.Provider(
        providerAuthority = "com.google.android.gms.fonts",
        providerPackage = "com.google.android.gms",
        certificates = R.array.com_google_android_gms_fonts_certs
    )

    val OutfitFontFamily = FontFamily("Outfit")
    val RobotoFontFamily = FontFamily("Roboto")
    val DancingScriptFontFamily = FontFamily("Dancing Script")
    val DefaultFontFamily = FontFamily.Default

    // --- Color saver utility ---
    // Used to persist and restore Color values in preferences.
    val ColorSaver = object : IntSaver<Color> {
        override fun restore(value: Int): Color = Color(value)
        override fun save(value: Color): Int = value.toArgb()
    }

    // --- Common intents ---
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

    // --- Featured apps ---
    val featuredApps = listOf(
        // 🎵 Media player app
        Triple(
            "One Player", // App name
            "https://lh3.googleusercontent.com/FSYnTtWc0vdQPgLq2bMv9IMyKy45EKR4AI0Y-90hCuNhfzwfy1SEXAoPuZhijl9JoMIqKK1ipa8ZzJUdFsqeY8k", // Icon URL
            "com.googol.android.apps.oneplayer" // Package name
        ),

        // 🔢 Utility app for conversions
        Triple(
            "Unit Converter",
            "https://play-lh.googleusercontent.com/TtUj94noX7g5B6Vs84A2PpVSCreYWVye5mHz32mSMHXCojT0xxDRtXBwXbc1q42AaA=s256-rw",
            "com.prime.toolz2"
        ),

        // 🧮 Advanced calculator
        Triple(
            "Scientific Calculator",
            "https://play-lh.googleusercontent.com/ZK1RCWbqO5faf4Z1diQM6HtoaGbmM5dYudYY5yXXP1yZawHrElerat7ix0slYzAxHZRq=s256-rw",
            "com.prime.calculator.paid"
        ),

        // 🖼️ Gallery app for photos & videos
        Triple(
            "Gallery - Photos & Videos",
            "https://play-lh.googleusercontent.com/HlADK_i_qZoBn_4GNdjgCDt3Ah-h1ZbL_jUy1j_kDUo9Hvoq3AiUPI_ZxZXY95ftl7hu=w240-h480-rw",
            "com.googol.android.apps.photos"
        )
    )
    // --- Artwork shapes ---
    // Shapes used for artwork customization in UI
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

    // --- Accent colors ---
    val LightAccentColor = Color(0xFF904A42)
    val DarkAccentColor = Color.OliveYellow

    /**
     * Maps a preference key to its corresponding artwork shape.
     *
     * @param key The shape key stored in preferences.
     * @return The corresponding [Shape] instance.
     * @throws IllegalArgumentException if the key does not match any known shape.
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

    // --- Preference keys ---
    private const val PREFIX = "Audiofy"
    val NIGHT_MODE = stringPreferenceKey(
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
            this += Manifest.permission.ACCESS_MEDIA_LOCATION
            this += Manifest.permission.READ_MEDIA_VIDEO
            this += Manifest.permission.READ_MEDIA_AUDIO
        }
        // For Android Upside Down Cake (34) and above, add permission for user-selected visual media
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE)
            this += Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
        // For Android versions below Tiramisu 10(29), request WRITE_EXTERNAL_STORAGE for
        // legacy storage access
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.Q)
            this += Manifest.permission.WRITE_EXTERNAL_STORAGE
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
            this += Manifest.permission.READ_EXTERNAL_STORAGE
        }
    }


    /**
     * The list of in-App Purchasable that need to be showcased for purchases.
     */
    val FEATURED_IAPs = arrayOf(
        BuildConfig.IAP_NO_ADS,
        BuildConfig.IAP_TAG_EDITOR_PRO,
        BuildConfig.IAP_BUY_ME_COFFEE,
        BuildConfig.IAP_CODEX,
        BuildConfig.IAP_WIDGETS_PLATFORM,
        BuildConfig.IAP_COLOR_CROFT_WIDGET_BUNDLE,
    )
}
package com.prime.media

import android.app.Application
import android.os.Build
import androidx.compose.ui.unit.dp
import com.prime.media.core.NightMode
import com.primex.preferences.*
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

private const val TAG = "Audiofy"

@HiltAndroidApp
class Audiofy : Application() {

    companion object {
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
         * The counter counts the number of times this app was launched.
         */
        val KEY_LAUNCH_COUNTER =
            intPreferenceKey(TAG + "_launch_counter")

        /**
         * The length/duration of the track in mills considered above which to include
         */
        val EXCLUDE_TRACK_DURATION =
            longPreferenceKey(TAG + "_min_duration_limit_of_track", defaultMinTrackLimit)
        val MAX_RECENT_PLAYLIST_SIZE =
            intPreferenceKey(TAG + "_max_recent_size", defaultValue = 20)

        /**
         * peek Height of [BottomSheetScaffold], also height of [MiniPlayer]
         */
        val MINI_PLAYER_HEIGHT = 68.dp

        /**
         * The link to PlayStore Market.
         */
        const val GOOGLE_STORE = "market://details?id=" + BuildConfig.APPLICATION_ID

        /**
         * If PlayStore is not available in Users Phone. This will be used to redirect to the
         * WebPage of the app.
         */
        const val FALLBACK_GOOGLE_STORE =
            "http://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID

        /**
         * Package name for the Google Play Store. Value can be verified here:
         * https://developers.google.com/android/reference/com/google/android/gms/common/GooglePlayServicesUtil.html#GOOGLE_PLAY_STORE_PACKAGE
         */
        const val PKG_GOOGLE_PLAY_STORE = "com.android.vending"

        val STORAGE_PERMISSION =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                android.Manifest.permission.READ_MEDIA_AUDIO
            else
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    }
}


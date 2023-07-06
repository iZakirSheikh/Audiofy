package com.prime.media

import android.app.Application
import android.os.Build
import com.primex.preferences.intPreferenceKey
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit

private const val TAG = "Audiofy"

@HiltAndroidApp
class Audiofy : Application() {

    companion object {
        private val defaultMinTrackLimit = TimeUnit.MINUTES.toMillis(1)

        /**
         * The counter counts the number of times this app was launched.
         */
        val KEY_LAUNCH_COUNTER =
            intPreferenceKey(TAG + "_launch_counter")

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


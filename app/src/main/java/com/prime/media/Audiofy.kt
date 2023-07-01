package com.prime.media

import android.app.Application
import com.google.firebase.FirebaseApp
import com.primex.preferences.intPreferenceKey
import dagger.hilt.android.HiltAndroidApp

private const val TAG = "Audiofy"

@HiltAndroidApp
class Audiofy : Application() {

    companion object {
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

        /**
         * The counter counts the number of times this app was launched.
         */
        val KEY_LAUNCH_COUNTER =
            intPreferenceKey(TAG + "_launch_counter")
    }

    override fun onCreate() {
        super.onCreate()
        // initialize firebase
        FirebaseApp.initializeApp(this)
    }
}


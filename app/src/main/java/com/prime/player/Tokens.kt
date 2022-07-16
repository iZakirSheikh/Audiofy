package com.prime.player

import androidx.compose.ui.unit.dp
import androidx.core.app.ShareCompat

object Tokens {
    /**
     * This object contains the constants of the [Audio] Player.
     */
    object Audio {
        /**
         * The name of the playlist contains the favourites.
         */
        const val PLAYLIST_FAVOURITES = "_favourites"

        /**
         * peek Height of [BottomSheetScaffold], also height of [MiniPlayer]
         */
        val MINI_PLAYER_HEIGHT = 68.dp
    }

    /**
     * The link to PlayStore Market.
     */
    const val GOOGLE_STORE = "market://details?id=" + BuildConfig.APPLICATION_ID

    @Deprecated(
        "Not Required. As currently I am distributing app through the PlayStore only.",
        level = DeprecationLevel.WARNING
    )
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
}
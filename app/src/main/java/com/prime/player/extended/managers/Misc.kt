package com.prime.player.extended.managers

import android.content.Intent
import android.net.Uri
import com.prime.player.BuildConfig

enum class Market {
    GOOGLE,

    SAMSUNG,

    AMAZON
}


object MarketPackage {
    /**
     * Package name for the Amazon App Store.
     */
    const val AMAZON_APP_STORE = "com.amazon.venezia"

    /**
     * Package name for the Google Play Store. Value can be verified here:
     * https://developers.google.com/android/reference/com/google/android/gms/common/GooglePlayServicesUtil.html#GOOGLE_PLAY_STORE_PACKAGE
     */
    const val GOOGLE_PLAY_STORE = "com.android.vending"

    const val SAMSUNG_APP_STORE = "com.sec.android.app.samsungapps"

    fun map(pkg: String): Market? {
        return when (pkg) {
            AMAZON_APP_STORE -> Market.AMAZON
            GOOGLE_PLAY_STORE -> Market.GOOGLE
            SAMSUNG_APP_STORE -> Market.SAMSUNG
            else -> null
        }
    }

    /**
     * All packages as list
     */
    val packages: List<String>
        get() = listOf(
            AMAZON_APP_STORE,
            GOOGLE_PLAY_STORE,
            SAMSUNG_APP_STORE
        )

    /**
     * All [Market]s as list
     */
    val markets: List<Market>
        get() = listOf(
            Market.AMAZON,
            Market.GOOGLE,
            Market.SAMSUNG
        )


    fun map(market: Market): String {
        return when (market) {
            Market.GOOGLE -> GOOGLE_PLAY_STORE
            Market.SAMSUNG -> SAMSUNG_APP_STORE
            Market.AMAZON -> AMAZON_APP_STORE
        }
    }
}

val Market.title
    get() = when (this) {
        Market.GOOGLE -> "Google Play Store"
        Market.SAMSUNG -> "Samsung App Store"
        Market.AMAZON -> "Amazon App Store"
    }

/**
 * Returns the [Intent] for this market only.
 */
val Market.intent: Intent
    get() {
        val pkg = MarketPackage.map(this)
        val uri = Uri.parse(MarketLink.compose(market = this@intent))
        return Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage(pkg)
            addFlags(
                Intent.FLAG_ACTIVITY_NO_HISTORY or
                        Intent.FLAG_ACTIVITY_NEW_DOCUMENT or
                        Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            )
        }
    }

object MarketLink {
    /*URI: samsungapps://AppRating/<App Package Name>*/
    private const val SAMSUNG_STORE = "samsungapps://AppRating/"
    private const val GOOGLE_STORE = "market://details?id="
    private const val AMAZON_STORE = "amzn://apps/android?p="

    private const val FALLBACK_SAMSUNG_STORE = "https://apps.samsung.com/appquery/AppRating.as?appId="
    private const val FALLBACK_GOOGLE_STORE = "http://play.google.com/store/apps/details?id="
    private const val FALLBACK_AMAZON_STORE = "http://www.amazon.com/gp/mas/dl/android?p="

    /**
     * Composes the Market url of corresponding [Package]
     */
    fun compose(market: Market): String {
        return when (market) {
            Market.GOOGLE -> GOOGLE_STORE
            Market.SAMSUNG -> SAMSUNG_STORE
            Market.AMAZON -> AMAZON_STORE
        } + BuildConfig.APPLICATION_ID
    }

    fun fallbackUrl(market: Market): String {
        return when (market) {
            Market.GOOGLE -> FALLBACK_GOOGLE_STORE
            Market.SAMSUNG -> FALLBACK_SAMSUNG_STORE
            Market.AMAZON -> FALLBACK_AMAZON_STORE
        } + BuildConfig.APPLICATION_ID
    }
}
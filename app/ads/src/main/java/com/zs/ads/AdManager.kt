package com.zs.ads

import android.app.Activity
import android.content.Context
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo
import com.ironsource.mediationsdk.impressionData.ImpressionData
import com.ironsource.mediationsdk.impressionData.ImpressionDataListener
import com.ironsource.mediationsdk.sdk.LevelPlayBannerListener
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


/**
 * Manages the display and lifecycle of interstitial and rewarded ads within the application,
 * ensuring a minimum delay between ad presentations.*
 * Currently, only the [iListener] is available for tracking ad impressions.
 *
 * **Future Enhancements:**
 * * Action callbacks to streamline ad integration.
 * * Dynamic module support for optional uninstallation in paid app versions.
 */
interface AdManager {

    companion object {
        /**
         * Initial delay (2 minutes) before the first interstitial ad is shown after app launch.
         */
        val INTERSTITIAL_AD_INITIAL_DELAY = 2.minutes

        /**
         * Standard delay (10 minutes) between interstitial ads.
         */
        val INTERSTITIAL_AD_STANDARD_DELAY = 10.minutes

        /**
         * Minimum delay (45 seconds) enforced between two interstitial ads when a forced display is requested.
         */
        val INTERSTITIAL_AD_FORCED_DELAY = 45.seconds

        /**
         * Initializes the third-party ad SDK.
         *
         * @param context The application context.
         * @param id The unique application ID for the ad SDK.
         */
        fun initialize(context: Context, id: String) {
            IronSource.init(context, id)
        }
    }

    /**
     * Callback invoked when an ad impression is successfully recorded.
     *
     * For rewarded video and interstitial ads, this callback is triggered when the ad is opened.
     * For banner ads, the impression is reported upon successful load.
     *
     * @param data An [AdImpression] object containing information about the recorded impression.
     */
    var iListener: ((data: AdImpression) -> Unit)?

    /**
     * Pauses the ad manager when the hosting activity is paused.
     *
     * @param context The activity that is being paused.
     */
    fun onPause(context: Activity)

    /**
     * Resumes the ad manager when the hosting activity is resumed.
     *
     * @param context The activity that is being resumed.
     */
    fun onResume(context: Activity)

    /**
     * Shows an interstitial ad.
     *
     * @param force If true, attempts to show an ad even if the standard delay hasn't elapsed.
     *              The actual display may still be subject to the minimum forced delay.
     */
    fun show(force: Boolean)

    /**
     * Shows an interstitial ad with an optional delay.
     *
     * @param delay The time to wait before showing the ad. Defaults to [INTERSTITIAL_AD_STANDARD_DELAY].
     */
    fun show(delay: Duration = INTERSTITIAL_AD_STANDARD_DELAY)
}

/**
 * Creates an instance of [AdManager] with customizable delays.
 *
 * This utility function simplifies the creation of an AdManager with specific delay configurations.
 *
 * @param iDelay The initial delay before the first interstitial ad is shown.
 *               Defaults to [AdManager.INTERSTITIAL_AD_INITIAL_DELAY].
 * @param delay The standard delay between interstitial ads.
 *              Defaults to [AdManager.INTERSTITIAL_AD_STANDARD_DELAY].
 * @param forceDelay The minimum delay between interstitial ads when forced display is requested.
 *                   Defaults to [AdManager.INTERSTITIAL_AD_FORCED_DELAY].
 *
 * @return A configured instance of [AdManager].
 */
fun AdManager(
    iDelay: Duration = AdManager.INTERSTITIAL_AD_INITIAL_DELAY,
    delay: Duration = AdManager.INTERSTITIAL_AD_STANDARD_DELAY,
    forceDelay: Duration = AdManager.INTERSTITIAL_AD_FORCED_DELAY
): AdManager = AdManagerImpl(iDelay, delay, forceDelay)
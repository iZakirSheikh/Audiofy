/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 29-07-2024.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zs.ads

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.FrameLayout
import com.ironsource.mediationsdk.IronSource
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds


interface AdManager {

    companion object {
        /**
         * Triggered when an ad is successfully loaded.
         *
         *   **Note:** For banner ads, this event signifies both the successful load and the recording
         *   of an ad impression. For other ad formats like interstitial and rewarded ads, it simply
         *   indicates that the ad  is ready to be displayed.
         */
        const val AD_EVENT_LOADED = "ad_event_loaded"

        /**
         * Triggered when an ad fails to load. For rewarded videos, this typically means that an ad
         * is simply not available.
         */
        const val AD_EVENT_FAILED_TO_LOAD = "ad_event_failed_to_load"

        /**
         * Triggered when an ad impression is recorded. For banner ads this event will be triggered
         * instead of [AD_EVENT_LOADED].
         */
        const val AD_EVENT_IMPRESSION = "ad_event_impression"

        /**
         * Triggered when an ad is clicked by the user. In the case of rewarded videos, this event
         * might not be supported by all ad networks.
         */
        const val AD_EVENT_CLICKED = "ad_event_clicked"

        /**
         * Triggered when an ad is presented as a full-screen overlay to the user.
         */
        const val AD_EVENT_PRESENTED = "ad_event_presented"

        /**
         * Invoked when the user is about to return to the app after interacting with an ad. The
         * presented full-screen ad view is about to be closed, and your activity will regain its focus.
         */
        const val AD_EVENT_CLOSED = "ad_event_closed"

        /**
         * Triggered when the application loses focus, typically when the user clicks on an ad.
         */
        const val AD_EVENT_APPLICATION_LOST_FOCUS = "ad_event_application_lost_focus"

        /**
         * Triggered when the user completes watching a rewarded video and
         *      * should be rewarded. The placement parameter will include the reward data. When
         *      using server-to-server callbacks, you may ignore this event and wait for the ironSource server callback instead.
         */
        const val AD_EVENT_REWARDED = "ad_event_rewarded"

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
            IronSource.init(context, id,)
        }
    }

    /**
     * Registers an [AdEventListener] to receive ad events.
     */
    var listener: AdEventListener?

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

    /**
     * Shows a rewarded ad.
     *
     * The `onAdEvent` callback in the registered [AdEventListener] will be invoked with the
     * [AD_EVENT_REWARDED] event when the user completes watching the ad and is eligible for a reward.
     */
    fun showRewardedAd()

    /**
     * Provides the banner layout managed by the ad manager.
     *
     * @return The [FrameLayout] that contains the banner adview.
     */
    fun banner(context: Context): View

    /**
     * Loads an ad of the specified [size] in the banner.
     */
    fun load(size: AdSize)

    /**
     * Releases the resources associated with the ad manager.
     *
     * Call this method when you no longer need to display ads to clean up resources and prevent
     * potential leaks.
     */
    fun release()

    /**
     *  Checks weather rewarded video ad is loaded/avaailable.
     */
    val isRewardedVideoAvailable: Boolean
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
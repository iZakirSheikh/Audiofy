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
import android.content.ContextWrapper
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.IronSourceBannerLayout
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.model.Placement
import com.ironsource.mediationsdk.sdk.LevelPlayBannerListener
import com.ironsource.mediationsdk.sdk.LevelPlayInterstitialListener
import com.ironsource.mediationsdk.sdk.LevelPlayRewardedVideoListener
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo as IsAdInfo

private const val TAG = "AdManagerImpl"

private val INITIAL_RETRY_DELAY_MILLIS =
    TimeUnit.SECONDS.toMillis(1)

private val MAX_RETRY_BACKOFF_MILLIS =
    TimeUnit.MINUTES.toMillis(1)

private val IsAdInfo?.asAdInfo
    get() = if (this == null) null else AdData.AdInfo(this)
private val IronSourceError?.asAdError
    get() = if (this == null) null else AdData.AdError(this)
private val Placement?.asReward
    get() = if (this == null) null else Reward(this)

private val BANNER_RETRY_DELAY_MILLS =
    TimeUnit.SECONDS.toMillis(15)

private tailrec fun Context.findActivity(): Activity? = this as? Activity
    ?: (this as? ContextWrapper)?.baseContext?.findActivity()

internal class AdManagerImpl(
    private val iDelay: Duration,
    private val delay: Duration,
    private val forceDelay: Duration
) : AdManager {

    override var listener: AdEventListener? = null

    // how long before the data source tries to reload the interstitial/rewarded ad.
    private var delayRetryMills = INITIAL_RETRY_DELAY_MILLIS
    private lateinit var _cachedBannerLayout: IronSourceBannerLayout
    private val adManagerScope: CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onPause(context: Activity) = IronSource.onPause(context)
    override fun onResume(context: Activity) = IronSource.onResume(context)
    override fun showRewardedAd() = IronSource.showRewardedVideo()
    override val isRewardedVideoAvailable: Boolean get() = IronSource.isRewardedVideoAvailable()



    // Add observers.
    init {
        IronSource.setLevelPlayInterstitialListener(
            object : LevelPlayInterstitialListener {
                override fun onAdReady(p0: AdInfo?) {
                    // Invoked when the interstitial ad was loaded successfully.
                    // AdInfo parameter includes information about the loaded ad
                    Log.d(TAG, "onAdReady: $p0")
                    listener?.onAdEvent(AdManager.AD_EVENT_LOADED, p0.asAdInfo)
                }

                override fun onAdLoadFailed(p0: IronSourceError?) {
                    Log.d(TAG, "onAdLoadFailed: $p0")
                    listener?.onAdEvent(AdManager.AD_EVENT_FAILED_TO_LOAD, p0.asAdError)
                    // Retry loading the ad after a delay, increasing the delay exponentially
                    // until it reaches a maximum value
                    adManagerScope.launch {
                        delay(delayRetryMills)
                        delayRetryMills = (delayRetryMills * 2).coerceAtMost(MAX_RETRY_BACKOFF_MILLIS)
                        IronSource.loadInterstitial()
                    }
                }

                override fun onAdOpened(p0: AdInfo?) {
                    // Invoked when the Interstitial Ad Unit has opened, and user left the application screen.
                    // This is the impression indication.
                    Log.d(TAG, "onAdOpened: $p0")
                    listener?.onAdEvent(AdManager.AD_EVENT_PRESENTED, p0.asAdInfo)
                    timeMillsWhenAdShowed = System.currentTimeMillis()
                }

                override fun onAdShowSucceeded(p0: AdInfo?) {
                    Log.d(TAG, "onAdShowSucceeded: $p0")
                    // Invoked before the interstitial ad was opened, and before the
                    // InterstitialOnAdOpenedEvent is reported.
                    // This callback is not supported by all networks, and we recommend using it only if
                    // it's supported by all networks you included in your build.
                }

                override fun onAdShowFailed(p0: IronSourceError?, p1: AdInfo?) {
                    Log.d(TAG, "onAdShowFailed: $p0, $p1")
                    // Invoked when the ad failed to show
                }

                override fun onAdClicked(p0: AdInfo?) {
                    Log.d(TAG, "onAdClicked: $p0")
                    listener?.onAdEvent(AdManager.AD_EVENT_CLICKED, p0.asAdInfo)
                }

                override fun onAdClosed(p0: AdInfo?) {
                    Log.d(TAG, "onAdClosed: $p0")
                    listener?.onAdEvent(AdManager.AD_EVENT_CLOSED, p0.asAdInfo)
                    // Start a new load - this makes sure ads are present when called for show.
                    IronSource.loadInterstitial()
                }
            }
        )
        // add impression listener
        IronSource.addImpressionDataListener {data ->
            val wrapped = if (data == null) null else AdData.AdImpression(data)
            listener?.onAdImpression(wrapped)
        }
        // Add Listener for interstitial ads.
        IronSource.setLevelPlayRewardedVideoListener(
            object : LevelPlayRewardedVideoListener {
                override fun onAdOpened(p0: AdInfo?) {
                    // The Rewarded Video ad view has opened. Your activity will loose focus
                    Log.d(TAG, "onAdOpened: $p0")
                    listener?.onAdEvent(AdManager.AD_EVENT_PRESENTED, p0.asAdInfo)
                }

                override fun onAdShowFailed(p0: IronSourceError?, p1: AdInfo?) {
                    // The rewarded video ad was failed to show
                    Log.d(TAG, "onAdShowFailed: $p0, $p1")
                }

                override fun onAdClicked(p0: Placement?, p1: AdInfo?) {
                    // Invoked when the video ad was clicked.
                    // This callback is not supported by all networks, and we recommend using it
                    // only if it's supported by all networks you included in your build
                    Log.d(TAG, "onAdClicked: $p0, $p1")
                    listener?.onAdEvent(AdManager.AD_EVENT_CLICKED, p1.asAdInfo)
                }

                override fun onAdRewarded(p0: Placement?, p1: AdInfo?) {
                    // The user completed to watch the video, and should be rewarded.
                    // The placement parameter will include the reward data.
                    // When using server-to-server callbacks, you may ignore this event and wait for the ironSource server callback
                    Log.d(TAG, "onAdRewarded: $p0, $p1")
                    listener?.onAdRewarded(p0.asReward, p1.asAdInfo)
                }

                override fun onAdClosed(p0: AdInfo?) {
                    // The Rewarded Video ad view is about to be closed. Your activity will regain its focus
                    Log.d(TAG, "onAdClosed: $p0")
                    listener?.onAdEvent(AdManager.AD_EVENT_CLOSED, p0.asAdInfo)
                    // Start a new load - this makes sure ads are present when called for show.
                    IronSource.loadRewardedVideo()
                }

                override fun onAdAvailable(p0: AdInfo?) {
                    // Indicates that there's an available ad.
                    // The adInfo object includes information about the ad that was loaded successfully
                    // Use this callback instead of onRewardedVideoAvailabilityChanged(true)
                    Log.d(TAG, "onAdAvailable: $p0")
                    listener?.onAdEvent(AdManager.AD_EVENT_LOADED, p0.asAdInfo)
                }

                override fun onAdUnavailable() {
                    // Indicates that no ads are available to be displayed
                    // Use this callback instead of onRewardedVideoAvailabilityChanged(false)
                    Log.d(TAG, "onAdUnavailable: ")
                    listener?.onAdEvent(AdManager.AD_EVENT_FAILED_TO_LOAD, null)
                    // Retry loading the ad after a delay, increasing the delay exponentially
                    // until it reaches a maximum value
                    adManagerScope.launch {
                        delay(delayRetryMills)
                        delayRetryMills = (delayRetryMills * 2).coerceAtMost(MAX_RETRY_BACKOFF_MILLIS)
                        IronSource.loadRewardedVideo()
                    }
                }
            }
        )
    }

    // Trigger initial load requests
    init {
        IronSource.loadInterstitial()
        IronSource.loadRewardedVideo()
    }

    override fun release() {
        if (::_cachedBannerLayout.isInitialized)
            IronSource.destroyBanner(_cachedBannerLayout)
        adManagerScope.cancel()
    }

    override fun banner(context: Context): View {
        // Check if banner layout is already cached
        // Return cached banner if available
        if (::_cachedBannerLayout.isInitialized)
            return _cachedBannerLayout
        // Find the activity associated with the context
        val activity = context.findActivity() ?: error("No activity associated with view.")
        // Get the standard banner ad size
        val banner = AdSize.BANNER.value
        // Create and configure the IronSource banner ad view
        _cachedBannerLayout = IronSource.createBanner(activity, banner).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
            )
            // Initially hide the ad view until it's loaded
            visibility = View.GONE
            // Set the LevelPlay banner listener to handle ad events
            levelPlayBannerListener = object : LevelPlayBannerListener {
                override fun onAdLoaded(p0: AdInfo?) {
                    Log.d(TAG, "onAdLoaded: $p0")
                    // Notify listener about the ad loaded event
                    listener?.onAdEvent(AdManager.AD_EVENT_LOADED, p0.asAdInfo)
                    // Create AdData for impression event (if ad info is available)
                    val data = if (p0 == null) null else AdData.AdImpression(p0)
                    // Notify listener about the ad impression event
                    // for banners this represents both the successful load and the recording of an ad
                    // impression
                    listener?.onAdImpression(data)
                    // Show the banner ad view since it's now loaded
                    _cachedBannerLayout.visibility = View.VISIBLE
                }

                override fun onAdLoadFailed(p0: IronSourceError?) {
                    Log.d(TAG, "onAdLoadFailed: $p0")
                    // Hide the empty banner.
                    _cachedBannerLayout.visibility = View.GONE
                    // Attempt to load the banner again
                    adManagerScope.launch {
                        // Retry loading the banner after a delay
                        delay(BANNER_RETRY_DELAY_MILLS)
                        IronSource.loadBanner(_cachedBannerLayout)
                    }
                }

                override fun onAdClicked(p0: AdInfo?) {
                    Log.d(TAG, "onAdClicked: $p0")
                    listener?.onAdEvent(AdManager.AD_EVENT_CLICKED, p0.asAdInfo)
                }

                override fun onAdLeftApplication(p0: AdInfo?) {
                    Log.d(TAG, "onAdLeftApplication: $p0")
                    listener?.onAdEvent(AdManager.AD_EVENT_APPLICATION_LOST_FOCUS, p0.asAdInfo)
                }

                override fun onAdScreenPresented(p0: AdInfo?) {
                    Log.d(TAG, "onAdScreenPresented: $p0")
                    listener?.onAdEvent(AdManager.AD_EVENT_PRESENTED, p0.asAdInfo)
                }

                override fun onAdScreenDismissed(p0: AdInfo?) {
                    Log.d(TAG, "onAdScreenDismissed: $p0")
                    listener?.onAdEvent(AdManager.AD_EVENT_CLOSED, p0.asAdInfo)
                }
            }
            // Initiate the ad loading process
            IronSource.loadBanner(this)
        }
        // Return the newly created and cached banner layout
        return _cachedBannerLayout
    }

    override fun load(size: AdSize) {
        if (!::_cachedBannerLayout.isInitialized)
            return
        // update the banner size
        _cachedBannerLayout.setBannerSize(size.value)
        IronSource.loadBanner(_cachedBannerLayout)
    }

    /**
     * Shows the interesting ad if it is ready otherwise just calls the load method.
     */
    fun show() {
        Log.d(TAG, "show: isInterstitialReady: ${IronSource.isInterstitialReady()}")
        if (!IronSource.isInterstitialReady()) {
            IronSource.loadInterstitial()
            return
        }
        IronSource.showInterstitial()
    }


    /**
     * The time this object was created; since this a global variable and hence this represents the
     * time of app first loaded.
     */
    val timeMillsWhenCreated = System.currentTimeMillis()

    /**
     * The time in *mills* when interstitial ad was showed.
     * *null* value represents, the ad was never showed.
     */
    var timeMillsWhenAdShowed: Long? = null

    override fun show(force: Boolean) {
        // Calculate time elapsed since last ad show (or object creation if no ads shown yet)
        //  1. Get current time in milliseconds: System.currentTimeMillis()
        //  2. Get time of last ad show (or object creation if none): timeMillsWhenAdShowed ?: timeMillsWhenCreated
        //  3. Subtract time of last ad show (or creation) from current time to get elapsed time in milliseconds
        val elapsed = (System.currentTimeMillis() - (timeMillsWhenAdShowed
            ?: timeMillsWhenCreated)).milliseconds
        // Determine if ad should be shown based on force flag and elapsed time
        val show = when {
            // If forced, check against minimum forced delay
            force -> elapsed > forceDelay
            // If first ad, check against initial delay
            timeMillsWhenAdShowed == null -> elapsed > iDelay
            // Otherwise, check against standard delay
            else -> elapsed > delay
        }
        Log.d(TAG, "show: Force: $force, Elapsed: $elapsed,  show:$show")
        // Show the ad if conditions are met, otherwise do nothing
        if (show) show()
    }
    override fun show(delay: Duration) {
        // Calculate the time elapsed since the last ad was shown
        // (or object creation if no ads shown yet)
        val elapsed = (System.currentTimeMillis() - (timeMillsWhenAdShowed
            ?: timeMillsWhenCreated)).milliseconds
        // Check if the elapsed time meets or exceeds the specified delay
        if (elapsed >= delay) {
            show() // Show the ad since the delay requirement is met
        }
        // If the delay hasn't been met, the ad won't be shown (implicitly handled)
    }


    override fun setConsent(value: Boolean) {
        IronSource.setConsent(value)
    }

    override fun setMetaData(meta: String, value: String) {
        IronSource.setMetaData(meta, value)
    }
}
/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 06-07-2024.
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
import android.util.Log
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo
import com.ironsource.mediationsdk.impressionData.ImpressionData
import com.ironsource.mediationsdk.impressionData.ImpressionDataListener
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.LevelPlayInterstitialListener
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

private const val TAG = "AdManagerImpl"

internal class AdManagerImpl(
    private val iDelay: Duration,
    private val delay: Duration,
    private val forceDelay: Duration
) : AdManager, LevelPlayInterstitialListener {
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

    init {
        // Set listener and load first Ad.
        IronSource.setLevelPlayInterstitialListener(this)
        IronSource.loadInterstitial()
    }

    val onImpressionData = object : ImpressionDataListener {
        override fun onImpressionSuccess(p0: ImpressionData?) {
            if (p0 == null) return
            iListener?.invoke(AdImpression(p0))
        }
    }

    override var iListener: ((data: AdImpression) -> Unit)? = null
        set(value) {
            field = value
            if (value == null)
                IronSource.removeImpressionDataListener(onImpressionData)
            else
                IronSource.addImpressionDataListener(onImpressionData)
        }

    override fun onPause(context: Activity) {
        IronSource.onPause(context)
    }

    override fun onResume(context: Activity) {
        IronSource.onResume(context)
    }

    override fun onAdReady(p0: AdInfo?) {
        Log.d(TAG, "onAdReady: Info: $p0")
    }

    override fun onAdLoadFailed(p0: IronSourceError?) {
        Log.d(TAG, "onAdLoadFailed: Error: $p0")
        // start a new load.
        IronSource.loadInterstitial()
    }

    override fun onAdOpened(p0: AdInfo?) {
        Log.d(TAG, "onAdOpened: Info: $p0")
    }

    override fun onAdShowSucceeded(p0: AdInfo?) {
        Log.d(TAG, "onAdShowSucceeded: $p0")
        // update the shown time
        timeMillsWhenAdShowed = System.currentTimeMillis()
    }

    override fun onAdShowFailed(p0: IronSourceError?, p1: AdInfo?) {
        Log.d(TAG, "onAdShowFailed: Info: $p1, Error $p0")
    }

    override fun onAdClicked(p0: AdInfo?) {
        Log.d(TAG, "onAdClicked: Info $p0")
    }

    override fun onAdClosed(p0: AdInfo?) {
        // Start a new load - this makes sure ads are present when called for show.
        IronSource.loadInterstitial()
        Log.d(TAG, "onAdClosed: Info $p0")
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
}
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
import android.content.Context
import android.content.ContextWrapper
import android.util.Log
import android.widget.FrameLayout
import androidx.core.view.get
import androidx.core.view.postDelayed
import com.ironsource.mediationsdk.IronSource
import com.ironsource.mediationsdk.IronSourceBannerLayout
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.sdk.LevelPlayBannerListener
import kotlin.time.Duration

private const val TAG = "AdView"

private tailrec fun Context.getActivity(): Activity? = this as? Activity
    ?: (this as? ContextWrapper)?.baseContext?.getActivity()

class AdView : FrameLayout {
    /**
     * The underlying IronSourceBannerLayout used to display the ad.
     */
    private val adLayout get() = get(0) as IronSourceBannerLayout

    /**
     * The size of the banner ad. Defaults to [AdSize.BANNER].*/
    var size: AdSize = AdSize.BANNER
        set(value) {
            field = value
            adLayout.setBannerSize(value.value)
        }

    /**
     * Creates a new AdView instance.
     *
     * @param context The context in which the view is created.
     * @throws IllegalStateException If no activity is associated with the context.
     */
    constructor(context: Context) : super(context) {
        val activity = context.getActivity() ?: error("No activity associated with view.")
        // Create the IronSource banner layout and add it to the view
        val layout = IronSource.createBanner(activity, size.value)
        addView(layout)
    }

    private val _listener by lazy {
        object : LevelPlayBannerListener {
            override fun onAdLoaded(p0: AdInfo?) {
                Log.d(TAG, "onAdLoaded: Info: $p0")
                listener?.onAdLoaded(if (p0 != null) AdInfo(p0) else null)
            }

            override fun onAdLoadFailed(p0: IronSourceError?) {
                Log.d(TAG, "onAdLoadFailed: Error: $p0")
                listener?.onAdFailedToLoad(if (p0 != null) AdError(p0) else null)
            }

            override fun onAdClicked(p0: AdInfo?) {
                Log.d(TAG, "onAdClicked: Info: $p0")
                listener?.onAdClicked(if (p0 != null) AdInfo(p0) else null)
            }

            override fun onAdLeftApplication(p0: AdInfo?) {
                Log.d(TAG, "onAdLeftApplication: Info: $p0")
            }

            override fun onAdScreenPresented(p0: AdInfo?) {
                listener?.onAdScreenPresented(if (p0 != null) AdInfo(p0) else null)
                Log.d(TAG, "onAdScreenPresented: Info: $p0")
            }

            override fun onAdScreenDismissed(p0: AdInfo?) {
                Log.d(TAG, "onAdScreenDismissed: Info: $p0")
                listener?.onAdClosed(if (p0 != null) AdInfo(p0) else null)
            }
        }
    }

    var listener: AdListener? = null
        set(value) {
            field = value
            adLayout.levelPlayBannerListener = if (value == null) null else _listener
        }

    private fun loadAd(placement: String?){
        if (adLayout.isDestroyed)
            return
        IronSource.loadBanner(adLayout, placement)
    }

    /**
     * Loads a banner ad with an optional placement ID and delay.
     *
     * @param placement The ad placement ID. If null, the default placement is used.
     * @param delay The duration to wait before loading the ad. Defaults to no delay.
     */
    fun loadAd(placement: String? = null, delay: Duration = Duration.ZERO) {
        if (adLayout.isDestroyed)
            return
        if (delay == Duration.ZERO)
        // Load the banner ad immediately
            loadAd(placement)
        else
        // Load the banner ad after the specified delay
            postDelayed(delay.inWholeMilliseconds) {
                loadAd(placement)
            }
    }

    /**
     * Releases the resources associated with the banner ad.
     */
    fun release() = IronSource.destroyBanner(adLayout)
}
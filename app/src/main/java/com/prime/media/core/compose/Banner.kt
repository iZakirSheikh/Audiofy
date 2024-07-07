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

package com.prime.media.core.compose

import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.ktx.Firebase
import com.zs.ads.AdError
import com.zs.ads.AdInfo
import com.zs.ads.AdListener
import com.zs.ads.AdSize
import com.zs.ads.AdView
import kotlin.time.Duration.Companion.seconds

private const val TAG = "Banner"


private val DELAY = 3.seconds

/**
 * A Composable function that displays a banner ad using the Ad SDK.
 *
 * @param modifier Modifiers to beapplied to the AdView.
 * @param size The desired size of the banner ad. Defaults to [AdSize.SMART].
 * @param key An optional key to identify the ad placement.
 */
@Suppress("UNRESOLVED_REFERENCE")
@Composable
fun Banner(
    modifier: Modifier = Modifier,
    size: AdSize = AdSize.SMART,
    key: String? = null,
) {
    AndroidView(
        factory = { context ->
            // Create an AdView instance and configure its layout parameters
            AdView(context).apply {
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
                )
                // Initially hide the ad view
                visibility = View.GONE

                // Set up the AdListener to handle ad events
                val fAnalytics = Firebase.analytics
                listener = object : AdListener {
                    override fun onAdLoaded(info: AdInfo?) {
                        Log.d(TAG, "onAdLoaded: $info")
                        visibility = View.VISIBLE // Show the ad view when loaded
                        // Log ad impression event to Firebase Analytics
                        fAnalytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION){
                            param(FirebaseAnalytics.Param.AD_PLATFORM, "IronSource")
                            param(FirebaseAnalytics.Param.AD_UNIT_NAME, info?.country ?: "")
                            param(FirebaseAnalytics.Param.AD_FORMAT, info?.format ?: "")
                            param(FirebaseAnalytics.Param.AD_SOURCE, info?.network ?: "")
                            param(FirebaseAnalytics.Param.VALUE, info?.revenue ?: 0.0)
                            // All IronSource revenue is sent in USD
                            param(FirebaseAnalytics.Param.CURRENCY, "USD")
                        }
                    }

                    override fun onAdFailedToLoad(error: AdError?) {
                        Log.d(TAG, "onAdFailedToLoad: $error")
                        visibility = View.GONE // Hide the ad view if loading failed
                        loadAd(key) //Retry loading the ad with the provided key
                    }
                }
            }
        },
        modifier = modifier, // Apply modifiers to the AndroidView
        update = { view ->
            view.size = size // Update the ad size
            Log.d(TAG, "onUpdate Banner : $key $size ")
            // Load the ad with a 1-second delay to avoid potential issues
            view.loadAd(key, DELAY)
        },
        onRelease = { layout ->
            Log.d(TAG, "onRelease Banner : $key $size ")
            layout.listener = null // Clear the listener to prevent leaks
            layout.release() // Release resources associated with the ad
        }
    )
}
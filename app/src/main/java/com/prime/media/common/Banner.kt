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

package com.prime.media.common

import android.util.Log
import android.view.View
import android.widget.FrameLayout
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.prime.media.MainActivity
import com.zs.ads.AdSize

private const val TAG = "Banner"

/**
 * A Composable function that displays a banner ad using the Ad SDK.
 * It uses an [AndroidView] to display an[AdView] within the Composable hierarchy.
 * The AdView is managed by a [LocalSystemFacade] to ensure proper handling
 * across different composables.
 *
 * @param modifier Modifiers to be applied to the AdView.
 * @param size The desired size of the banner ad. Defaults to [AdSize.SMART].
 * @param key An optional key to identify the ad placement. This is typically
 *             used to differentiate between different ad units in your app.
 */
@Composable
fun Banner(
    modifier: Modifier = Modifier,
    size: AdSize = AdSize.SMART,
    key: String? = null,
) {
    val activity = LocalSystemFacade.current as? MainActivity
    if (activity == null) {
        Log.i(TAG, "Loading Banner Ad is not supported in preview mode.")
        return Spacer(modifier)
    }
    val view = activity.bannerAd
    val id = remember(View::generateViewId)
    // Check if the current AdView is either null or has a different ID
    // than the one generated for this composable. This indicates that
    // either it's the first time the Banner composable is being used or
    // the previous AdView is still attached and needs to be detached first.
    if (view == null || view.id != View.NO_ID && view.id != id) {
        Log.d(
            TAG,
            "Banner: $view with id: $id ; either it is still attached; wait for it to get detached."
        )
        return Spacer(modifier)
    }

    // Attach the AdView to the Composable hierarchy
    AndroidView(
        factory = {
            Log.d(TAG, "Banner: attaching to AndroidView")
            view.id = id // Assign the generated ID to the AdView
            view.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            view
        },
        modifier = modifier.animateContentSize(), // Apply modifiers to the AndroidView
        update = {
            activity.loadBannerAd(size)
            Log.d(TAG, "onUpdate Banner : $key $size ")
        },
        onRelease = {
            Log.d(TAG, "onRelease Banner : $key $size ")
            view.id = View.NO_ID  // Reset the AdView's ID to indicate it's no longer attached
            activity.bannerAd = null // Clear the reference to the AdView in the facade
        }
    )
}
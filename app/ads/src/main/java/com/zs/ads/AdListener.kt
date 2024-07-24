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

import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo as ISAdInfo

/**
 * Value class wrapper around [ISAdInfo] representing information about an ad impression.
 *
 * @property format The format displayed (Rewarded Video/Interstitial/Banner).
 * @property network The ad network name that served the ad.
 * @property name The ad network instance name as defined on the platform. For bidding
 *                          sources, the instance name will be ‘Bidding’.
 * @property id Identifier per network, this includes the ad network’s instanceID/placement/zone/etc.
 * @property country Country code ISO 3166-1 format.
 * @property revenue The revenue generated for the impression (USD). The revenue value is either
 *                   estimated or exact, according to the precision (see precision field description).
 */
@JvmInline
value class AdInfo internal constructor(private val value: ISAdInfo) {
    val format: String get() = value.adUnit
    val network: String get() = value.adNetwork
    val id: String get() = value.instanceId
    val country: String get() = value.country
    val revenue: Double get() = value.revenue
    val name: String get() = value.instanceName
}

/**
 * Value wrapper class representing an ad error.
 *
 * @property message The error message.
 * @property code The error code.
 */
@JvmInline
value class AdError internal constructor(private val value: IronSourceError) {
    val message get() = value.errorMessage
    val code get() = value.errorCode
}


/**
 * A listener for ads.
 */
interface AdListener {

    /**
     * Invoked each time an ad is loaded, either on refresh or manual load.
     *
     * @param info Information about the loaded ad, or `null` if unavailable.
     */
    fun onAdLoaded(info: AdInfo?) {}

    /**
     * Invoked when the ad loading process has failed.
     * This callback is sent for both manual load and refreshed ad failures.
     *
     * @param error An object containing information about why the ad failed to load.
     */
    fun onAdFailedToLoad(error: AdError?) {}

    /**
     * Invoked when the end user clicks on the ad.
     *
     * @param info Information about the clicked ad, or `null` if unavailable.*/
    fun onAdClicked(info: AdInfo?) {}

    /**
     * Called when the user is about to return to the app after interacting with an ad.
     *
     * @param info Information about the ad that was interacted with, or `null` if unavailable.
     */
    fun onAdClosed(info: AdInfo?) {}

    /**
     * Notifies the presentation of a full-screen ad following user interaction.
     *
     * @param info Information about the presented ad, or `null` if unavailable.
     */
    fun onAdScreenPresented(info: AdInfo?) {}
}

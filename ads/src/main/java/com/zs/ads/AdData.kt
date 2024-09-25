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

import com.ironsource.mediationsdk.impressionData.ImpressionData
import com.ironsource.mediationsdk.logger.IronSourceError
import com.ironsource.mediationsdk.adunit.adapter.utility.AdInfo as ISAdInfo

/**
 * A sealed interface representing different types of ad data,
 * such as ad metadata, errors, and impression details.
 */
sealed interface AdData {

    /**
     * Represents ad information.
     *
     * @property format The format displayed (Rewarded Video/Interstitial/Banner).
     * @property network The ad network name that served the ad.
     * @property name The ad network instance name as defined on the platform. For bidding
     *                          sources, the instance name will be ‘Bidding’.
     * @property id Identifier per network, this includes the ad network’s instanceID/placement/zone/etc.
     * @property country Country code ISO 3166-1 format.
     * @property revenue The revenue generated for the impression (USD). The revenue value is either
     *                   estimated or exact, according to the precision (see precision field description).
     *
     * **Note:** If the `AdInfo` data is not available, the string properties will return an empty
     * string, and the numeric properties will return 0.
     */
    @JvmInline
    value class AdInfo(internal val value: ISAdInfo) : AdData {
        val format: String get() = value.adUnit
        val network: String get() = value.adNetwork
        val id: String get() = value.instanceId
        val country: String get() = value.country
        val revenue: Double get() = value.revenue
        val name: String get() = value.instanceName
    }

    /**
     * Represents an error that occurred during ad loading.
     *
     * @property message The error message.
     * @property code The error code.
     */
    @JvmInline
    value class AdError(internal val value: IronSourceError) : AdData {
        val message get() = value.errorMessage
        val code get() = value.errorCode
    }

    /**
     * Represents a successful ad impression.
     *
     * @property data The underlying [ImpressionData] or [ISAdInfo] instance. For banner ads,
     * it's [ISAdInfo] as the impression is reported on load success.
     *
     * @property format The ad unit displayed (Rewarded Video/Interstitial/Banner)
     * @property network The ad network name that served the ad
     * @property id Identifier per network, this includes the ad network’s instanceID/placement/zone/etc.
     * @property country Country code ISO 3166-1 format.
     * @property revenue    The revenue generated for the impression (USD). The revenue value is either
     *                      estimated or exact, according to the precision (see precision field description)
     *  @property name The ad network instance name as defined on the platform. For bidding sources,
     *                  the instance name will be ‘Bidding’
     *
     *  Note - For banners, the impression is reported on load success.
     *  @see [https://firebase.google.com/docs/analytics/measure-ad-revenue]
     */
    @JvmInline
    value class AdImpression(internal val data: Any) : AdData {
        init {
            require(data is ImpressionData || data is ISAdInfo) {
                "$data must be ImpressionData or ISAdInfo"
            }
        }

        val format: String get() = if (data is ImpressionData) data.adUnit else (data as ISAdInfo).adUnit
        val network: String get() = if (data is ImpressionData) data.adNetwork else (data as ISAdInfo).adNetwork
        val id: String get() = if (data is ImpressionData) data.instanceId else (data as ISAdInfo).instanceId
        val country: String get() = if (data is ImpressionData) data.country else (data as ISAdInfo).country
        val revenue: Double get() = if (data is ImpressionData) data.revenue else (data as ISAdInfo).revenue
        val name: String get() = if (data is ImpressionData) data.instanceName else (data as ISAdInfo).instanceName
    }
}
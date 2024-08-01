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


/**
 * An interface for listening to ad events.
 *
 * Implement this interface to receive notifications about various ad events,
 * such asad loading, impressions, clicks, and errors.
 */
interface AdEventListener {

    /**
     * Called when an ad event occurs.
     *
     * @param event The name of the ad event that occurred.
     * @param data An optional [AdData] object containing additional information about the event,
     * such as ad metadata or error details.
     */
    fun onAdEvent(event: String, data: AdData?)

    /**
     * Called when an ad impression event occurs. This can be triggered when:
     * - A banner ad is successfully loaded.
     * - or the impression listener records impression
     *
     * @param value An [AdData.AdImpression] object containing details about the ad impression,
     * or null if no ad impression data is available.
     */
    fun onAdImpression(value: AdData.AdImpression?)

    /**
     * Called when a rewarded ad is successfully completed and the user earns a reward.
     *
     * @param reward A [Reward] object representing the reward earned, or null if no reward is provided.
     * @param info An [AdData.AdInfo] object containing additional information about the ad event.
     */
    fun onAdRewarded(reward: Reward?, info: AdData.AdInfo?)
}
/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 02-07-2024.
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

import com.ironsource.mediationsdk.ISBannerSize

/**
 * Represents the size of an ad banner.
 ** @property size The underlying [ISBannerSize] object.
 */
@JvmInline
value class AdSize private constructor(internal val value: ISBannerSize) {

    /**
     * Creates a custom ad size.
     *
     * @param type The type of the custom ad size (default is "CUSTOM").
     * @param width The width of the ad in pixels.
     * @param height The height of the ad in pixels.
     */
    constructor(width: Int, height: Int, type: String = "CUSTOM") : this(
        ISBannerSize(
            type,
            width,
            height
        )
    )

    companion object {
        /**
         * Standard banner size (320x50).
         */
        val BANNER = AdSize(ISBannerSize.BANNER)

        /**
         * Large banner size (320x100).*/
        val LARGE_BANNER = AdSize(ISBannerSize.LARGE)

        /**
         * Medium rectangle size (300x250).
         */
        val MEDIUM_RECTANGLE = AdSize(ISBannerSize.RECTANGLE)

        /**
         * Smart banner size that adjusts to the device's width.
         */
        val SMART = AdSize(ISBannerSize.SMART)

        /**
         * Leaderboard size (728x90).
         */
        val LEADERBOARD = AdSize(728, 90, "LEADERBOARD",)

        /**
         * Full banner size (468x60).
         */
        val FULL_BANNER = AdSize(468, 60, "FULL_BANNER",)
    }
}
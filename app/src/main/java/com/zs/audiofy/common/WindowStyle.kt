/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 23-09-2024.
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

@file:OptIn(ExperimentalStdlibApi::class)

package com.zs.audiofy.common

import androidx.compose.runtime.Stable

// Note to myself
/*
 * Why use flags?
 *
 * Efficiency - Flags are a lightweight way to represent multiple boolean/ int values within a
 *              single integer. This reduces memory usage and can improve performance compared to
 *              storing each value individually.
 * Flexibility - Flags can be easily combined using bitwise operations (like OR and AND),
 *               allowing for flexible representation of different state combinations.
 * Extensibility - The system can be easily extended by adding more flags in the future without
 *                 requiring significant code changes.
 * Readability - Using well-named flags can improve code readability by clearly indicating
 *              the purpose of each bit.
 * Conciseness - Flags allow for concise representation of complex states, making code easier
 *                to understand and maintain.
 */

/**
 * A value class that encapsulates window style flags for various UI elements.
 *
 * @property value The integer representation of the window style flags, defaulting to [FLAG_STYLE_AUTO].
 * @property flagNavBarVisibility Current App Nav Bar Visibility state as a normalized 3-bit code: AUTO, VISIBLE, HIDDEN
 * @property flagSystemBarBackground Current System Bars Background state as a normalized 3-bit code: AUTO, TRANSPARENT, TRANSLUCENT
 * @property flagSystemBarAppearance Current System Bars Appearance state as a normalized 3-bit code:, AUTO, LIGHT, DARK
 * @property flagSystemBarVisibility Current System Bars Visibility state as a normalized 3-bit code:, AUTO, VISIBLE, HIDDEN
 * @property flagNavBarPosition Current App Nav Bar Orientation state as a normalized 2-bit code: NONE, SIDE, BOTTOM
 * @property flagFabVisibility Current FAB Visibility state as a normalized 3-bit code:, AUTO, VISIBLE, HIDDEN
 */
@JvmInline
@Stable
value class WindowStyle(val value: Int = FLAG_STYLE_AUTO) {
    //
    companion object {
        // 3-bit field masks using bit positions:
        private const val NAV_BAR_VISIBILITY_MASK = 0b111 shl 0    // Bits 0–2  (3 bits)
        private const val SYSTEM_BARS_BG_MASK = 0b111 shl 3    // Bits 3–5  (3 bits)
        private const val SYSTEM_BARS_APPEARANCE_MASK = 0b111 shl 6    // Bits 6–8  (3 bits)
        private const val SYSTEM_BARS_VISIBILITY_MASK = 0b111 shl 9    // Bits 9–11 (3 bits)
        private const val NAV_BAR_POSITION_MASK = 0b11 shl 12   // Bits 12–13 (2 bits)
        private const val FAB_VISIBILITY_MASK = 0b111 shl 14   // Bits 14–16 (3 bits)

        // Field 1: App Nav Bar Visibility (bits 0–2)
        const val FLAG_APP_NAV_BAR_VISIBILITY_AUTO = 0b100 shl 0  // AUTO  = bit 2
        const val FLAG_APP_NAV_BAR_VISIBLE = 0b010 shl 0  // VISIBLE = bit 1
        const val FLAG_APP_NAV_BAR_HIDDEN = 0b001 shl 0  // HIDDEN  = bit 0

        // Field 2: System Bars Background (bits 3–5)
        const val FLAG_SYSTEM_BARS_BG_AUTO = 0b100 shl 3  // AUTO       = bit 5
        const val FLAG_SYSTEM_BARS_BG_TRANSPARENT = 0b001 shl 3  // TRANSPARENT = bit 3
        const val FLAG_SYSTEM_BARS_BG_TRANSLUCENT = 0b010 shl 3  // TRANSLUCENT = bit 4

        // Field 3: System Bars Appearance (bits 6–8)
        const val FLAG_SYSTEM_BARS_APPEARANCE_AUTO = 0b100 shl 6  // AUTO  = bit 8
        const val FLAG_SYSTEM_BARS_APPEARANCE_LIGHT = 0b001 shl 6  // LIGHT = bit 6
        const val FLAG_SYSTEM_BARS_APPEARANCE_DARK = 0b010 shl 6  // DARK  = bit 7

        // Field 4: System Bars Visibility (bits 9–11)
        const val FLAG_SYSTEM_BARS_VISIBILITY_AUTO = 0b100 shl 9  // AUTO    = bit 11
        const val FLAG_SYSTEM_BARS_VISIBLE = 0b010 shl 9  // VISIBLE = bit 10
        const val FLAG_SYSTEM_BARS_HIDDEN = 0b001 shl 9  // HIDDEN  = bit 9

        // Field 5: App Nav Bar Position (bits 12–13)
        const val FLAG_NAV_BAR_POSITION_NONE = 0b00 shl 12 // NONE       = bits 12–13 cleared
        const val FLAG_NAV_BAR_POSITION_SIDE = 0b01 shl 12 // SIDE       = bit 12
        const val FLAG_NAV_BAR_POSITION_BOTTOM = 0b10 shl 12 // BOTTOM     = bit 13

        // Field 6: FAB Visibility (bits 14–16)
        const val FLAG_FAB_VISIBILITY_AUTO = 0b100 shl 14  // AUTO    = bit 16
        const val FLAG_FAB_VISIBLE = 0b010 shl 14  // VISIBLE = bit 15
        const val FLAG_FAB_HIDDEN = 0b001 shl 14  // HIDDEN  = bit 14

        // Combined “all‐AUTO” default style (include FAB if desired)
        const val FLAG_STYLE_AUTO =
            FLAG_SYSTEM_BARS_APPEARANCE_AUTO or
                    FLAG_SYSTEM_BARS_BG_AUTO or
                    FLAG_SYSTEM_BARS_VISIBILITY_AUTO or
                    FLAG_APP_NAV_BAR_VISIBILITY_AUTO or
                    FLAG_NAV_BAR_POSITION_NONE or
                    FLAG_FAB_VISIBILITY_AUTO

        private fun maskOf(flag: Int): Int =
            when (flag) {
                FLAG_APP_NAV_BAR_VISIBILITY_AUTO, FLAG_APP_NAV_BAR_VISIBLE, FLAG_APP_NAV_BAR_HIDDEN -> NAV_BAR_VISIBILITY_MASK
                FLAG_SYSTEM_BARS_BG_AUTO, FLAG_SYSTEM_BARS_BG_TRANSPARENT, FLAG_SYSTEM_BARS_BG_TRANSLUCENT -> SYSTEM_BARS_BG_MASK
                FLAG_SYSTEM_BARS_APPEARANCE_AUTO, FLAG_SYSTEM_BARS_APPEARANCE_LIGHT, FLAG_SYSTEM_BARS_APPEARANCE_DARK -> SYSTEM_BARS_APPEARANCE_MASK
                FLAG_SYSTEM_BARS_VISIBILITY_AUTO, FLAG_SYSTEM_BARS_VISIBLE, FLAG_SYSTEM_BARS_HIDDEN -> SYSTEM_BARS_VISIBILITY_MASK
                FLAG_NAV_BAR_POSITION_NONE, FLAG_NAV_BAR_POSITION_SIDE, FLAG_NAV_BAR_POSITION_BOTTOM -> NAV_BAR_POSITION_MASK
                FLAG_FAB_VISIBILITY_AUTO, FLAG_FAB_VISIBLE, FLAG_FAB_HIDDEN -> FAB_VISIBILITY_MASK
                else -> throw IllegalArgumentException("Unknown flag: $flag")
            }
    }

    /**
     * Adds a flag to the current window style, ensuring only relevant bits are set.
     *
     * @param flag The flag to be added.
     * @throws IllegalArgumentException If the flag is not recognized.
     * @return A new [WindowStyle] instance with the added flag.
     */
    operator fun plus(flag: Int): WindowStyle {
        val mask = maskOf(flag)
        return WindowStyle((value and mask.inv()) or (flag and mask))
    }

    /**
     * Removes a flag from the current window style, setting the relevant bits to automatic.
     *
     * @param flag The flag to be removed.
     * @throws IllegalArgumentException If the flag is not recognized.
     * @return A new [WindowStyle] instance with the removed flag.
     */
    operator fun minus(flag: Int): WindowStyle {
        val mask = maskOf(flag)
        return WindowStyle(value and mask.inv())
    }

    override fun toString(): String {
        return "WindowStyle(value=$value)"
    }

    val flagNavBarVisibility: Int get() = (value and NAV_BAR_VISIBILITY_MASK)
    val flagSystemBarBackground: Int get() = (value and SYSTEM_BARS_BG_MASK)
    val flagSystemBarAppearance: Int get() = (value and SYSTEM_BARS_APPEARANCE_MASK)
    val flagSystemBarVisibility: Int get() = (value and SYSTEM_BARS_VISIBILITY_MASK)
    val flagNavBarPosition: Int get() = (value and NAV_BAR_POSITION_MASK)
    val flagFabVisibility: Int get() = (value and FAB_VISIBILITY_MASK)
}
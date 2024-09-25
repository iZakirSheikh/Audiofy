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

package com.zs.core_ui

import com.zs.core_ui.WindowStyle.Companion.FLAG_STYLE_AUTO

// Shift and mask constants for 2-bit fields
private const val SYSTEM_BARS_VISIBILITY_MASK = 0b11000000
private const val SYSTEM_BARS_APPEARANCE_MASK = 0b00110000
private const val SYSTEM_BARS_BG_MASK = 0b00001100
private const val APP_NAV_BAR_MASK = 0b00000011

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
 * A value class that encapsulates window style flags for system bars visibility, appearance,
 * background, and app navigation bar visibility.
 *
 * @property flags The integer representation of the window style flags, defaulting to [FLAG_STYLE_AUTO].
 */
@JvmInline
value class WindowStyle(private val flags: Int = FLAG_STYLE_AUTO) {
    /**
     * Companion object holding constants for window style flags, used to configure system bars
     * visibility, appearance, background, and app navigation bar visibility.
     *
     * These flags are represented as bitfields occupying specific bits in an integer value.
     *
     * @property FLAG_STYLE_AUTO Automatically applies the default window style.
     *
     * @property FLAG_SYSTEM_BARS_VISIBILITY_AUTO Automatically applies the default system bars visibility setting.
     * @property FLAG_SYSTEM_BARS_VISIBLE Makes the system bars (status and navigation) visible. Occupies bit 6.
     * @property FLAG_SYSTEM_BARS_HIDDEN Hides the system bars (status and navigation). Occupies bit 7.
     *
     * @property FLAG_SYSTEM_BARS_APPEARANCE_AUTO Automatically applies the default system bars appearance setting.
     * @property FLAG_SYSTEM_BARS_APPEARANCE_LIGHT Sets the system bars' appearance to light mode. Occupies bit 4.
     * @property FLAG_SYSTEM_BARS_APPEARANCE_DARK Sets the system bars' appearance to dark mode. Occupies bit 5.
     *
     * @property FLAG_SYSTEM_BARS_BG_AUTO Automatically applies the default system bars background setting.
     * @property FLAG_SYSTEM_BARS_BG_TRANSPARENT Makes the system bars' background transparent. Occupies bit 2.
     * @property FLAG_SYSTEM_BARS_BG_TRANSLUCENT Makes the system bars' background translucent. Occupies bit 3.
     *
     * @property FLAG_APP_NAV_BAR_AUTO Automatically applies the default app navigation bar visibility setting.
     * @property FLAG_APP_NAV_BAR_VISIBLE Makes the app navigation bar visible. Occupies bit 0.
     * @property FLAG_APP_NAV_BAR_HIDDEN Hides the app navigation bar. Occupies bit 1.
     */
    companion object {
        val FLAG_STYLE_AUTO = 0b00000000

        // Flag 1: System Bars Visibility (Bits 6-7)
        val FLAG_SYSTEM_BARS_VISIBILITY_AUTO = FLAG_STYLE_AUTO
        val FLAG_SYSTEM_BARS_VISIBLE = 0b01000000 // Bits 6
        val FLAG_SYSTEM_BARS_HIDDEN = 0b10000000 // Bit 7

        // Flag 2: System Bars Appearance (Bits 4-5)
        val FLAG_SYSTEM_BARS_APPEARANCE_AUTO = FLAG_STYLE_AUTO
        val FLAG_SYSTEM_BARS_APPEARANCE_LIGHT = 0b00010000 // Bit 4
        val FLAG_SYSTEM_BARS_APPEARANCE_DARK = 0b00100000 // Bit 5

        // Flag 3: System Bars Background (Bits 2-3)
        val FLAG_SYSTEM_BARS_BG_AUTO = FLAG_STYLE_AUTO
        val FLAG_SYSTEM_BARS_BG_TRANSPARENT = 0b00000100 // Bit 2
        val FLAG_SYSTEM_BARS_BG_TRANSLUCENT = 0b00001000 // Bit 3

        // Flag 4: App Nav Bar (Bits 0-1)
        val FLAG_APP_NAV_BAR_AUTO = FLAG_STYLE_AUTO
        val FLAG_APP_NAV_BAR_VISIBLE = 0b00000001 // Bit 0
        val FLAG_APP_NAV_BAR_HIDDEN = 0b00000010 // Bit 1
    }

    /**
     * Retrieves the System Bars Visibility flag (Bits 6-7)
     */
    val flagSystemBarVisibility: Int
        get() = (flags and SYSTEM_BARS_VISIBILITY_MASK)

    /**
     * Retrieves the System Bars Appearance flag (Bits 4-5)
     */
    val flagSystemBarAppearance: Int
        get() = (flags and SYSTEM_BARS_APPEARANCE_MASK)

    /**
     * Retrieves the System Bars Background flag (Bits 2-3)
     */
    val flagSystemBarBackground: Int
        get() = (flags and SYSTEM_BARS_BG_MASK)

    /**
     * Retrieves the App Nav Bar flag (Bits 0-1)
     */
    val flagAppNavBar: Int
        get() = (flags and APP_NAV_BAR_MASK)

    /**
     * Performs a bitwise OR operation between the current [WindowStyle] and the provided flag.
     *
     * This operation combines the current flags with the specified flag using the bitwise OR operation.
     *
     * @param flag The integer flag to be combined with the current [WindowStyle].
     * @return A new [WindowStyle] instance with the combined flags.
     */
    infix fun or(flag: Int): WindowStyle{
        return WindowStyle(flags or flag)
    }
}



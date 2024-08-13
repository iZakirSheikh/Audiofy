/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 12-08-2024.
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

import android.app.WallpaperColors
import android.graphics.Bitmap
import android.os.Build
import androidx.annotation.ColorInt
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette

/**
 * Extracts a color from a bitmap to be used as a wallpaper accent color.
 *
 * On Android O MR1 and above, it uses [WallpaperColors] to extract the primary color.
 * On older versions, it uses [Palette] to extract a vibrant color based on the provided theme.
 * If no suitable color is found in the palette, the provided default color is returned.
 *
 * @param bitmap The bitmap to extract the color from. Must not be null or empty.
 * @param isDark Whether the current theme is dark.
 * @param default The default color to use if no suitable color is found.
 *
 * @return The extracted color as an ARGB integer.
 */
@Stable
@ColorInt
fun WallpaperAccentColor(
    bitmap: Bitmap?,
    isDark: Boolean,
    default: Color
): Int {
    if (bitmap == null || bitmap.isRecycled || bitmap.width == 0 || bitmap.height == 0) {
        return default.toArgb()
    }

    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
        WallpaperColors.fromBitmap(bitmap).primaryColor.toArgb()
    } else {
        // Generate a color palette from the bitmap
        val palette = Palette.from(bitmap).generate()
        val argb = default.toArgb() // Obtain the ARGB value of the default color for potential use
        // Pick a vibrant color based on the current theme, or use the default if none is found
        if (isDark) palette.getLightVibrantColor(argb) else palette.getDarkVibrantColor(argb)
    }
}



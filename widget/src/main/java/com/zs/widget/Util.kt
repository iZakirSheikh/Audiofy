/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 20-01-2025.
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

package com.zs.widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.GlanceModifier as Modifier
import androidx.glance.unit.ColorProvider
import java.nio.file.WatchEvent

private const val TAG = "Util"

/**
 * Creates a new `ColorProvider` with a modified alpha value.
 *
 * @param context The context needed to resolve the color.
 * @param alpha The new alpha value (0f-1f); -1f means use the original alpha.
 * @return A new `ColorProvider` or the original if alpha is -1f.
 */
@SuppressLint("RestrictedApi")
fun ColorProvider.copy(context: Context, alpha: Float = -1f) = if (alpha == -1f) this else
    ColorProvider(getColor(context).copy(alpha))

@Composable
internal fun Modifier.launchApp(): Modifier {
    val context = LocalContext.current
    return clickable {
        Log.d(TAG, "Universal: ${context.packageName}")
        context.startActivity(context.packageManager.getLaunchIntentForPackage(context.packageName))
    }
}
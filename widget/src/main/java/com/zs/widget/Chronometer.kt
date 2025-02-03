/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 16-01-2025.
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

import android.os.Build
import android.util.TypedValue
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.takeOrElse
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType.Companion.Sp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceTheme
import androidx.glance.LocalContext
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.GlanceModifier as Modifier

/**
 * A composable function that displays a chronometer using Android's RemoteViews.
 *
 * This function leverages a custom layout (R.layout.chronometer) containing a Chronometer view
 * and updates it based on the provided parameters.
 *
 * @param base The base time in milliseconds for the chronometer.
 *             This is the time that the chronometer starts counting from.
 *             If it's a countdown, it represents the target time.
 * @param format The format string to use for the chronometer display.
 *               Defaults to "%tH%tM:%tS" (HH:MM:SS). See [java.util.Formatter]
 *               for valid format specifiers.
 * @param started A boolean indicating whether the chronometer should be started or not.
 *                Default is true. If false, the chronometer will be paused.
 * @param isCountDown A boolean indicating whether the chronometer should count down or not.
 *                   Defaults to false (counts up).
 *                   This parameter only takes effect on API level 24 and higher.
 * @param modifier Modifier to apply to the underlying AndroidRemoteViews Composable.
 */
@Composable
internal fun Chronometer(
    base: Long,
    format: String = "%tH%tM:%tS",
    started: Boolean = true,
    isCountDown: Boolean = false,
    textColor: Color = Color.Unspecified,
    bold: Boolean = false,
    textSize: TextUnit = 12.sp,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val id = if (bold) R.layout.chronometer_bold else R.layout.chronometer
    AndroidRemoteViews(
        modifier = modifier,
        remoteViews = RemoteViews(ctx.packageName, id).apply {
            setChronometer(R.id.chronometer, base, format, started)
            setTextColor(
                R.id.chronometer,
                textColor.takeOrElse { GlanceTheme.colors.onBackground.getColor(ctx) }.toArgb()
            )
            setTextViewTextSize(R.id.chronometer, TypedValue.COMPLEX_UNIT_SP, textSize.value)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                setChronometerCountDown(R.id.chronometer, isCountDown)
        }
    )
}
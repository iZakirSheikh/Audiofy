/*
 * Copyright (c)  2026 Zakir Sheikh
 *
 * Created by sheik on 26 of Jan 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Last Modified by sheik on 26 of Jan 2026
 *
 */

package com.prime.media.console

import android.view.Gravity
import androidx.annotation.FloatRange
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import kotlin.math.roundToInt

private val CustomWidthProperties = DialogProperties(usePlatformDefaultWidth = false)
private val SPEED_RANGE = 0.25f..8.0f
private const val REQUEST_DISMISS = -1.0f

private val PLAYBACK_SPEED_PRESETS =
    floatArrayOf(0.9f, 0.95f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f)

/**
 * Represents the playback speed dialog.
 * @param onRequestChange Called when the playback speed is changed. -1; dismiss the dialog.
 */
@Composable
fun PlaybackSpeed(
    expanded: Boolean,
    @FloatRange(0.25, 8.0) value: Float,
    onRequestChange: (value: Float) -> Unit
) {

}

private val TIMER_RANGE = 10f..120f
private const val TIMER_INCREMENT = 10f

/**
 * Represents the sleep timer dialog.
 */
@Composable
fun SleepTimer(
    expanded: Boolean,
    onRequestChange: (mills: Long) -> Unit
) {}


/**
 * A dialog that allows the user to configure media tracks (audio and video).
 *
 * @param viewState The current state of the console view.
 * @param onDismissRequest A callback that is invoked when the dialog is dismissed.
 */
@Composable
fun MediaConfigDialog(
    viewState: ConsoleViewState,
    onDismissRequest: () -> Unit
) {}
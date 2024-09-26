/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 12-07-2024.
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

package com.zs.core_ui.toast

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalAccessibilityManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * A class that manages the display of [Toast] messages, ensuring only one Toast is shown at a time.
 */
@Stable
class ToastHostState {
    private val mutex = Mutex()
    var current by mutableStateOf<Toast?>(null)
        private set

    /**
     * Displays a [Toast] message with the given parameters.
     *
     * This function suspends until the Toast is eitherdismissed or its action is performed.
     * Only one Toast can be shown at a time, so subsequent calls to this function will
     * replace the currently displayed Toast.
     *
     * @param message The text message to be displayed in the Toast.
     * @param action Optional label for an action button to be shown in the Toast.
     * @param icon Optional leading icon to be displayed in the Toast.
     * @param accent The accent color to be used for this Toast.
     * @param duration The duration for which the Toast should be displayed.
     * @return A[Result] code indicating whether the Toast was dismissed or its action was performed.
     */
    suspend fun showToast(
        message: CharSequence,
        action: CharSequence? = null,
        icon: ImageVector? = null,
        accent: Color = Color.Unspecified,
        @Duration duration: Int = if (action == null) Toast.DURATION_SHORT else Toast.DURATION_INDEFINITE
    ): @Result Int {
        mutex.withLock {
            try {
                return suspendCancellableCoroutine { continuation ->
                    current = Data(icon, message, duration, action, accent, continuation)
                }
            } finally {
                current = null
            }
        }
    }
}

@Composable
fun ToastHost(
    state: ToastHostState,
    modifier: Modifier = Modifier
) {
    val currentToastData = state.current
    val accessibilityManager = LocalAccessibilityManager.current
    LaunchedEffect(currentToastData) {
        if (currentToastData != null) {
            val duration = currentToastData.toMillis(
                currentToastData.action != null, accessibilityManager
            )
            delay(duration)
            currentToastData.dismiss()
        }
    }
    FadeInFadeOutWithScale(
        current = state.current, modifier = modifier, content = {
            Toast(it)
        }
    )
}
/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 14-10-2024.
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

package com.prime.media.impl

import android.app.Application
import android.content.res.Resources
import android.text.format.Formatter
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.res.ResourcesCompat
import com.primex.core.getText2
import com.primex.preferences.Preferences
import com.zs.core_ui.toast.Duration
import com.zs.core_ui.toast.Result
import com.zs.core_ui.toast.Toast
import com.zs.core_ui.toast.ToastHostState
import org.koin.androidx.scope.ScopeViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.component.inject
import com.zs.core_ui.showPlatformToast as showAndroidToast

private const val TAG = "KoinViewModel"

@OptIn(KoinExperimentalAPI::class)
abstract class KoinViewModel: ScopeViewModel() {
    private val resources: Resources by inject()
    private val toastHostState: ToastHostState by inject()
    val preferences: Preferences by inject()
    private val context: Application by inject()

    fun showPlatformToast(
        @StringRes message: Int,
        @Duration duration: Int = Toast.DURATION_SHORT
    ) = context.showAndroidToast(message, duration)

    fun showPlatformToast(
        message: String,
        @Duration duration: Int = Toast.DURATION_SHORT
    )  = context.showAndroidToast(message, duration)

    suspend fun showToast(
        message: CharSequence,
        action: CharSequence? = null,
        icon: ImageVector? = null,
        accent: Color = Color.Unspecified,
        @Duration duration: Int = if (action == null) Toast.DURATION_SHORT else Toast.DURATION_INDEFINITE
    ): @Result Int = toastHostState.showToast(message, action, icon, accent, duration)

    suspend fun showToast(
        @StringRes message: Int,
        @StringRes action: Int = ResourcesCompat.ID_NULL,
        icon: ImageVector? = null,
        accent: Color = Color.Unspecified,
        @Duration duration: Int = if (action == ResourcesCompat.ID_NULL) Toast.DURATION_SHORT else Toast.DURATION_INDEFINITE
    ): @Result Int = showToast(
        message = resources.getText2(message),
        action = if (action == ResourcesCompat.ID_NULL) null else resources.getText2(action),
        icon = icon,
        accent = accent,
        duration = duration
    )

    fun getText(@StringRes id: Int): CharSequence = resources.getText2(id)
    fun getText(@StringRes id: Int, vararg args: Any) = resources.getText2(id, *args)
    fun formatFileSize(sizeBytes: Long): String = Formatter.formatFileSize(context, sizeBytes)

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared: ${this::class.simpleName}")
    }
}
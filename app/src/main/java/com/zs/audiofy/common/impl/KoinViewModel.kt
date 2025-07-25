/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 10-05-2025.
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

package com.zs.audiofy.common.impl

import android.app.Application
import android.content.res.Resources
import android.text.format.Formatter
import android.util.Log
import android.widget.Toast
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.viewModelScope
import com.zs.audiofy.R
import com.zs.compose.foundation.OrientRed
import com.zs.compose.foundation.getText2
import com.zs.compose.theme.snackbar.SnackbarDuration
import com.zs.compose.theme.snackbar.SnackbarHostState
import com.zs.compose.theme.snackbar.SnackbarResult
import com.zs.core.common.showPlatformToast
import com.zs.core.telemetry.Analytics
import com.zs.preferences.Preferences
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.launch
import org.koin.androidx.scope.ScopeViewModel
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.component.inject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

@OptIn(KoinExperimentalAPI::class)
abstract class KoinViewModel : ScopeViewModel() {
    private val TAG = "KoinViewModel"

    private val resources: Resources by inject()
    private val toastHostState: SnackbarHostState by inject()
    val preferences: Preferences by inject()
    val analytics: Analytics by inject()
    val context: Application by inject()

    fun ImageVector(@DrawableRes id: Int): ImageVector =
        ImageVector.vectorResource(context.theme, res = resources, id)

    fun showPlatformToast(
        @StringRes message: Int,
        length: Int = Toast.LENGTH_SHORT,
    ) = context.showPlatformToast(message, length)

    fun showPlatformToast(
        message: String,
        length: Int = Toast.LENGTH_SHORT,
    ) = context.showPlatformToast(message, length)

    suspend fun showSnackbar(
        message: CharSequence,
        action: CharSequence? = null,
        icon: ImageVector? = null,
        accent: Color = Color.Unspecified,
        duration: SnackbarDuration = if (action == null) SnackbarDuration.Short else SnackbarDuration.Long,
    ): SnackbarResult = toastHostState.showSnackbar(message, action, icon, accent, duration)

    suspend fun showSnackbar(
        @StringRes message: Int,
        @StringRes action: Int = ResourcesCompat.ID_NULL,
        icon: ImageVector? = null,
        accent: Color = Color.Unspecified,
        duration: SnackbarDuration = if (action == ResourcesCompat.ID_NULL) SnackbarDuration.Short else SnackbarDuration.Long,
    ): SnackbarResult = showSnackbar(
        message = resources.getText2(message),
        action = if (action == ResourcesCompat.ID_NULL) null else resources.getText2(action),
        icon = icon,
        accent = accent,
        duration = duration
    )

    fun getText(@StringRes id: Int): CharSequence = resources.getText2(id)
    fun getText(@StringRes id: Int, vararg args: Any) = resources.getText2(id, *args)
    fun formatFileSize(sizeBytes: Long): String = Formatter.formatFileSize(context, sizeBytes)

    /**
     * Reports an error message to the user.
     */
    suspend fun report(message: CharSequence) = showSnackbar(
        message = buildAnnotatedString {
            appendLine(getText(R.string.error))
            withStyle(SpanStyle(color = Color.Gray)) {
                append(message)
            }
        },
        action = getText(R.string.report),
        icon = Icons.Outlined.ErrorOutline,
        accent = Color.OrientRed,
        duration = SnackbarDuration.Indefinite
    )

    /**
     * Launches a new coroutine within the ViewModel scope, handling potential exceptions.
     *
     * This function wraps the [viewModelScope.launch] function to provide a convenient way
     * to launch coroutines with error handling. If any exceptions occur during the execution
     * of the coroutine, a toast message with the error details will be displayed to the user.
     *
     * @param context The [CoroutineContext] to use for the coroutine.
     *                Defaults to [EmptyCoroutineContext].
     * @param start The [CoroutineStart] mode to use for the coroutine.
     *              Defaults to [CoroutineStart.DEFAULT].
     * @param block The suspend function to execute within the coroutine.
     */
    inline fun runCatching(
        context: CoroutineContext = EmptyCoroutineContext,
        start: CoroutineStart = CoroutineStart.DEFAULT,
        crossinline block: suspend CoroutineScope.() -> Unit
    ) {
        viewModelScope.launch(context, start) {
            try { block() }
            catch (e: Exception) {
                // Handle any exceptions that occurred during the coroutine execution.
                // Display an error message to the user, providing context and error details.
                val report = report(e.message ?: getText(R.string.msg_unknown_error))
                if (report == SnackbarResult.ActionPerformed)
                    analytics.record(e)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        Log.d(TAG, "onCleared: ${this::class.simpleName}")
    }
}
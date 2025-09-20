/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 03-06-2025.
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

import android.app.Activity
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.outlined.NearbyError
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.Color
import com.zs.audiofy.R
import com.zs.audiofy.common.Action
import com.zs.audiofy.common.INFO
import com.zs.audiofy.common.ShareFilesIntent
import com.zs.audiofy.common.compose.FilterDefaults
import com.zs.audiofy.common.raw
import com.zs.audiofy.settings.AppConfig
import com.zs.audiofy.settings.Settings
import com.zs.compose.foundation.Rose
import com.zs.compose.foundation.runCatching
import com.zs.compose.theme.snackbar.SnackbarDuration
import com.zs.compose.theme.snackbar.SnackbarResult
import com.zs.core.common.debounceAfterFirst
import com.zs.core.db.playlists.Playlists
import com.zs.core.playback.Remote
import com.zs.core.store.MediaProvider
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.onEach

abstract class StoreViewModel<T>(
    private val provider: MediaProvider, remote: Remote, playlists: Playlists
) : FilesViewModel<T>(remote, playlists) {

    private val TAG = "StoreViewModel"

    // Some common orders
    val ORDER_BY_NONE = FilterDefaults.ORDER_NONE
    val ORDER_BY_TITLE = FilterDefaults.ORDER_BY_TITLE
    val ORDER_BY_DATE_MODIFIED = FilterDefaults.ORDER_BY_DATE_MODIFIED
    val ORDER_BY_LENGTH get() = Action(R.string.length, id = "filter_by_length")

    // common actions
    val ACTION_DELETE = Action(R.string.delete, Icons.Default.DeleteOutline)
    val ACTION_SHARE = Action(R.string.share, Icons.Outlined.Share)
    val ACTION_INFO = Action.INFO

    abstract suspend fun refresh(query: String?, ascending: Boolean, order: Action)

    abstract val contentUri: Uri

    val flow by lazy {
        combine(
            flow = provider.observer(contentUri),
            flow2 = snapshotFlow(query::raw),
            flow3 = snapshotFlow(::filter),
            transform = { _, query, filter -> Triple(query, filter.first, filter.second) }
        ).debounceAfterFirst(300)
            .onEach() { (query, ascending, order) -> refresh(query, ascending, order) }
            .catch { exception ->
                Log.d(TAG, "provider: ${exception.stackTraceToString()}")
                val action = report(exception.message ?: getText(R.string.msg_unknown_error))
            }
    }

    /** Deletes file[s] represented by id[s]. */
    private suspend fun delete(resolver: Activity, vararg id: Long) {
        val result = runCatching(TAG) {
            // Get the selected items for deletion
            val consumed = id
            // For Android R and above, use the provider's delete function directly
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                return@runCatching provider.delete(resolver, *consumed)
            // For versions below Android R, show a confirmation toast
            // If the user performs the action, proceed with deletion
            // Otherwise, return -3 to indicate user cancellation
            val action = showSnackbar(
                message = R.string.msg_deletion_confirm,
                action = R.string.delete,
                icon = Icons.Outlined.NearbyError,
                accent = Color.Rose,
                duration = SnackbarDuration.Indefinite
            )
            // Delete the selected items
            // else return -3 to indicate user cancellation
            if (action == SnackbarResult.ActionPerformed)
                return@runCatching provider.delete(*consumed)
            // else return user cancelled
            -3
        }
        // Display a message based on the result of the deletion operation.
        if (result == null || result == 0 || result == -1)
            showPlatformToast(R.string.msg_files_delete_unknown_error)// General error
    }

    @Suppress("NewApi")
    private suspend fun trash(resolver: Activity, vararg id: Long) {
        // Ensure this is called on Android 10 or higher (API level 29).
        val result = runCatching(TAG) {
            // consume selected
            val selected = id
            provider.trash(resolver, *selected)
        }
        // General error
        if (result == null || result == 0 || result == -1)
            showPlatformToast(R.string.msg_files_trash_unknown_error)
    }

    /** Deletes or Trashes file(s) represented by id(s).*/
    fun remove(resolver: Activity, vararg id: Long) {
        runCatching {
            val isTrashEnabled = AppConfig.isTrashCanEnabled
            if (isTrashEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                trash(resolver = resolver, *id)
            else {
                Log.d(TAG, "remove: ${id.size}")
                delete(resolver = resolver, *id)
            }
        }
    }

    @Suppress("NewApi")
    private suspend fun restore(resolver: Activity, vararg id: Long) {
        val selected = id
        val result = runCatching(TAG) {
            provider.restore(resolver, *selected)
        }
        // Display a message based on the result of the deletion operation.
        // General error
        if (result == null || result == 0 || result == -1)
            showPlatformToast(R.string.msg_files_restore_unknown_error)
    }

    fun share(resolver: Activity, vararg ids: Long) {
        runCatching {
            val result = runCatching {
                resolver.startActivity(ShareFilesIntent(*ids))
            }
            // If sharing fails, display an error message.
            if (result.isFailure)
                error(getText(R.string.msg_error_sharing_files))
        }
    }
}
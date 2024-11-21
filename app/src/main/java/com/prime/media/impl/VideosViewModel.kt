/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 16-11-2024.
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

@file:OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)

package com.prime.media.impl

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.provider.MediaStore
import android.text.format.DateUtils
import android.util.Log
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.NearbyError
import androidx.compose.material.icons.outlined.Share
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.viewModelScope
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.prime.media.R
import com.prime.media.common.Filter
import com.prime.media.common.Mapped
import com.prime.media.common.menu.Action
import com.prime.media.common.menu.Action.Companion.invoke
import com.prime.media.local.videos.VideosViewState
import com.prime.media.local.videos.VideosViewState.Companion.ACTION_DELETE
import com.prime.media.local.videos.VideosViewState.Companion.ACTION_SHARE
import com.primex.core.Rose
import com.primex.core.findActivity
import com.primex.core.runCatching
import com.zs.core.playback.MediaFile
import com.zs.core.playback.PlaybackController
import com.zs.core.store.MediaProvider
import com.zs.core.store.Video
import com.zs.core.util.PathUtils
import com.zs.core_ui.toast.Toast
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import java.util.Locale

private const val TAG = "VideosViewModel"

private val Video.firstTitleChar
    inline get() = name.uppercase(Locale.ROOT)[0].toString()

private val ORDER_BY_TITLE = Action(R.string.title, id = MediaStore.Video.Media.TITLE)
private val ORDER_BY_NONE = Action(R.string.none, id = MediaStore.Video.DEFAULT_SORT_ORDER)
private val ORDER_BY_FOLDER = Action(R.string.folder, id = MediaStore.Video.Media.DATA)
private val ORDER_BY_DATE_MODIFIED =
    Action(R.string.date_modified, id = MediaStore.Video.Media.DATE_MODIFIED)
private val ORDER_BY_DURATION = Action(R.string.duration, id = MediaStore.Video.Media.DURATION)


private inline val TextFieldState.raw get() = text.trim().toString().ifEmpty { null }

class VideosViewModel(
    private val provider: MediaProvider,
    private val controller: PlaybackController
) : KoinViewModel(), VideosViewState {

    override val orders: List<Action> =
        listOf(ORDER_BY_NONE, ORDER_BY_TITLE, ORDER_BY_FOLDER, ORDER_BY_DATE_MODIFIED)
    override val query: TextFieldState = TextFieldState()
    override var order: Filter by mutableStateOf(true to ORDER_BY_FOLDER)

    override val actions: List<Action> = listOf(ACTION_DELETE, ACTION_SHARE)

    override fun delete(activity: Activity, video: Video) {
        viewModelScope.launch {
            val result = runCatching(TAG) {
                // For Android R and above, use the provider's delete function directly
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
                    return@runCatching provider.trash(activity, video.id)
                // For versions below Android R, show a confirmation toast
                // If the user performs the action, proceed with deletion
                // Otherwise, return -3 to indicate user cancellation
                val action = showToast(
                    message = R.string.msg_files_deletion_confirm,
                    action = R.string.delete,
                    icon = Icons.Outlined.NearbyError,
                    accent = androidx.compose.ui.graphics.Color.Rose,
                    priority = Toast.PRIORITY_HIGH
                )
                // Delete the selected items
                // else return -3 to indicate user cancellation
                if (action == Toast.ACTION_PERFORMED)
                    return@runCatching provider.delete(video.id)
                // else return user cancelled
                -3
            }
            // Display a message based on the result of the deletion operation.
            if (result == null || result == 0 || result == -1)
                showToast(R.string.msg_files_delete_unknown_error)// General error
        }
    }

    override fun share(activity: Activity, file: Video) {
        viewModelScope.launch {
            // Create an intent to share the selected items
            val intent = Intent().apply {
                // Map selected IDs to content URIs.
                // TODO - Construct custom content uri.
                val uri = arrayListOf(file.contentUri)
                // Set the action to send multiple items.
                action = Intent.ACTION_SEND_MULTIPLE
                // Add the URIs as extras.
                putParcelableArrayListExtra(
                    Intent.EXTRA_STREAM,
                    uri
                )
                // Grant read permission to the receiving app.
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // Set the MIME type to allow sharing of various file types.
                type = file.mimeType
                // Specify supported MIME types.
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf(file.mimeType))
            }
            // Start the sharing activity with a chooser.
            try {
                activity.startActivity(Intent.createChooser(intent, "Sharing Files"))
            } catch (e: Exception) {
                // Handle exceptions and display an error message.
                showToast(R.string.msg_error_sharing_files)
                Log.d(TAG, "share: ${e.message}")
            }
        }
    }

    override fun filter(ascending: Boolean, order: Action) {
        if (order == this.order.second && this.order.first == ascending)
            return
        this.order = ascending to order
    }

    override val data: StateFlow<Mapped<Video>?> = combine(
        snapshotFlow(query::raw).onStart { emit(null) }.drop(1).debounce(300L),
        snapshotFlow(::order),
        transform = { query, filter -> Triple(query, filter.first, filter.second) }
    ).flatMapLatest { (query, filter, order) ->
        provider.observer(MediaStore.Video.Media.EXTERNAL_CONTENT_URI).map {
            val videos = provider.fetchVideoFiles(query, order.id, filter)
            val result = when (order) {
                ORDER_BY_TITLE -> videos.groupBy { it.firstTitleChar }
                ORDER_BY_FOLDER -> videos.groupBy { with(PathUtils) { name(parent(it.path)) } }
                ORDER_BY_DATE_MODIFIED -> videos.groupBy {
                    DateUtils.getRelativeTimeSpanString(
                        it.dateModified,
                        System.currentTimeMillis(),
                        DateUtils.HOUR_IN_MILLIS,
                    )
                }

                else -> videos.groupBy { "" }
            }
            // This should be safe as String is subtype of CharSequence.
            result as Mapped<Video>
        }
    }
        // catch any errors.
        .catch {
            val report = report(it.message ?: getText(R.string.msg_unknown_error))
            if (report == Toast.ACTION_PERFORMED)
                Firebase.crashlytics.recordException(it)
        }
        // make sure the flow is released after sometime.
        .stateIn(viewModelScope, WhileSubscribed(), null)

    override fun play(video: Video) {
        viewModelScope.launch() {
            val count = controller.setMediaFiles(
                listOf(
                    MediaFile(
                        video.contentUri,
                        video.name,
                        getText(R.string.unknown).toString(),
                        video.contentUri,
                        video.mimeType
                    )
                )
            )
            if (count == 0)
                showPlatformToast(R.string.msg_unknown_error)
            controller.play()
        }
    }
}

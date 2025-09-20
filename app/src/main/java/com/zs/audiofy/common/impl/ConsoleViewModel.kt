package com.zs.audiofy.common.impl

import android.app.Activity
import android.net.Uri
import android.os.Build
import android.text.format.DateUtils
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import com.zs.audiofy.R
import com.zs.audiofy.console.ConsoleViewState
import com.zs.audiofy.console.RouteConsole
import com.zs.audiofy.settings.AppConfig
import com.zs.audiofy.settings.Settings
import com.zs.compose.foundation.OrientRed
import com.zs.compose.theme.snackbar.SnackbarResult
import com.zs.core.playback.MediaFile
import com.zs.core.playback.NowPlaying
import com.zs.core.playback.Remote
import com.zs.core.playback.VideoProvider
import com.zs.core.store.MediaProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ConsoleViewModel(
    val remote: Remote,
    val dataProvider: MediaProvider
) : KoinViewModel(), ConsoleViewState {

    override val state: StateFlow<NowPlaying?> = remote.state
    override var provider: VideoProvider by mutableStateOf(VideoProvider(null))
    override var visibility: Int by mutableIntStateOf(RouteConsole.VISIBILITY_VISIBLE)
    override var message: CharSequence? by mutableStateOf(null)
    override val queue: Flow<List<MediaFile>?> = remote.queue


    init {
        viewModelScope.launch {
            provider = remote.getViewProvider()
        }
    }

    override fun skipToNext() {
        runCatching {
            remote.skipToNext()
        }
    }

    override fun skipToPrev() {
        runCatching {
            remote.skipToPrevious()
        }
    }

    override fun togglePlay() {
        runCatching {
            remote.togglePlay()
        }
    }

    override fun seekTo(pct: Float) {
        runCatching {
            remote.seekTo(pct)
        }
    }

    override fun sleepAt(mills: Long) {
        viewModelScope.launch {
            if (!remote.isPlaying()) {
                return@launch showPlatformToast(R.string.msg_sleep_timer_playback_inactive)
            }

            remote.setSleepTimer(mills)
            if (mills == Remote.TIME_UNSET) {
                showPlatformToast(R.string.msg_sleep_timer_turned_off)
            } else {
                val endTime = System.currentTimeMillis() + mills
                val text = DateUtils.getRelativeTimeSpanString(
                    endTime,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS,
                    DateUtils.FORMAT_ABBREV_RELATIVE
                )
                val fMessage =
                    context.getString(R.string.msg_sleep_timer_set_playback_stops_s, text)
                showPlatformToast(fMessage)
            }
        }
    }

    override fun shuffle(enable: Boolean) {
        runCatching {
            remote.shuffle(enable)
            showPlatformToast(if (enable) R.string.msg_shuffle_on else R.string.msg_shuffle_off)
        }
    }

    override fun cycleRepeatMode() {
        runCatching {
            val new = remote.cycleRepeatMode()
            val msg = when (new) {
                Remote.REPEAT_MODE_OFF -> R.string.msg_repeat_mode_off
                Remote.REPEAT_MODE_ONE -> R.string.msg_repeat_mode_one
                else -> R.string.msg_repeat_mode_all
            }
            showPlatformToast(msg)
        }
    }

    override fun clear() {
        viewModelScope.launch {
            val permission = showSnackbar(
                R.string.msg_clear_queue_confirmation,
                R.string.clear,
                icon = Icons.Outlined.ClearAll,
                accent = Color.OrientRed
            )
            if (permission == SnackbarResult.ActionPerformed)
                remote.clear()
        }
    }

    var autohideJob: Job? = null
    override fun emit(newVisibility: Int, delayed: Boolean) {
        autohideJob?.cancel()
        if (!delayed) {
            this@ConsoleViewModel.visibility = newVisibility
            return
        }
        autohideJob = viewModelScope.launch {
            delay(RouteConsole.VISIBILITY_AUTO_HIDE_DELAY)
            this@ConsoleViewModel.visibility = newVisibility
        }
    }

    override fun toggleLike(uri: Uri?) {
        viewModelScope.launch { remote.toggleLike() }
    }

    override fun remove(key: Uri) {
        viewModelScope.launch {
            remote.remove(key)
        }
    }

    override fun delete(key: Uri, resolver: Activity) {
        // Execute the deletion logic within a try-catch block to handle potential errors.
        runCatching {
            // Determine the deletion action based on Android version and trash can settings.
            val code = when {
                // If running on Android R or newer and trash can is enabled, move to trash.
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && AppConfig.isTrashCanEnabled ->
                    dataProvider.trash(resolver, key)
                // If running on Android R or newer and trash can is disabled, delete permanently.
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> dataProvider.delete(resolver, key)
                // For older Android versions, show a confirmation dialog before deleting.
                else -> {
                    val concent = showSnackbar(
                        R.string.msg_deletion_confirm,
                        R.string.delete,
                        icon = Icons.Outlined.Delete,
                        accent = Color.OrientRed
                    )
                    if (concent == SnackbarResult.ActionPerformed)
                        dataProvider.delete(key)
                    else
                        -3 // If user cancels
                }
            }
            // Check the result code from the dataProvider operation. A negative code (except -3) indicates an error.
            if (code < 0 && code != -3)
                throw Exception(context.getString(R.string.msg_files_delete_unknown_error))
            // If deletion was successful (or file was moved to trash), remove the item from the remote queue.
            if (code != -3)
                remote.remove(key)
        }
    }

    override fun skipTo(key: Uri) {
        viewModelScope.launch {
            remote.skipTo(key)
        }
    }

    override var playbackSpeed: Float
        get() = runBlocking { remote.getPlaybackSpeed() }
        set(value) {
            viewModelScope.launch {
                val updated = remote.setPlaybackSpeed(value)
                if (updated)
                    showPlatformToast(
                        getText(
                            R.string.msg_playback_speed_updated_f,
                            value
                        ).toString()
                    )
                else
                    showPlatformToast(R.string.msg_error_playback_speed)
            }
        }
}
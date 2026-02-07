/*
 * Copyright (c)  2026 Zakir Sheikh
 *
 * Created by sheik on 21 of Jan 2026
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
 * Last Modified by sheik on 21 of Jan 2026
 *
 */

package com.prime.media.impl

import android.app.Activity
import android.net.Uri
import android.text.format.DateUtils
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.prime.media.R
import com.prime.media.console.ConsoleViewState
import com.prime.media.console.RouteConsole
import com.primex.core.OrientRed
import com.zs.core.playback.MediaFile
import com.zs.core.playback.NowPlaying2
import com.zs.core.playback.PlaybackController
import com.zs.core.playback.PlaybackController.TrackInfo
import com.zs.core.playback.VideoProvider
import com.zs.core_ui.toast.Toast
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

class ConsoleViewModel2(val controller: PlaybackController) : KoinViewModel(), ConsoleViewState {
    override val state: StateFlow<NowPlaying2?> = controller.state
    override var visibility: Int by mutableIntStateOf(RouteConsole.VISIBLE_ALWAYS)
    override val queue: Flow<List<MediaFile>?> = controller.queue

    override var playbackSpeed: Float
        get() = runBlocking { controller.getPlaybackSpeed() }
        set(value) {
            viewModelScope.launch {
                val updated = controller.setPlaybackSpeed(value)
                if (!updated)
                    showPlatformToast(R.string.msg_unknown_error)
            }
        }

    override fun getVideoProvider(): Player {
        return runBlocking { controller.getVideoView() }
    }

    override fun skipToNext() {
        tryLaunch {
            controller.skipToNext()
        }
    }

    override fun skipToPrev() {
        tryLaunch {
            controller.skipToPrevious()
        }
    }

    override fun togglePlay() {
        tryLaunch {
            controller.togglePlay()
        }
    }

    override fun seekTo(pct: Float) {
        tryLaunch {
            controller.seekTo(pct)
        }
    }

    override fun sleepAt(mills: Long) {
       tryLaunch {
           if (!controller.isPlaying()) {
               return@tryLaunch showPlatformToast("Timer only available when media is playing.")
           }

           if (mills == PlaybackController.SLEEP_TIME_UNSET) {
               controller.setSleepAtMs(PlaybackController.SLEEP_TIME_UNSET)
               showPlatformToast("Cleared sleep timer!")
           } else {
               val endTime = System.currentTimeMillis() + mills
               controller.setSleepAtMs(endTime)
               val text = DateUtils.getRelativeTimeSpanString(
                   endTime,
                   System.currentTimeMillis(),
                   DateUtils.MINUTE_IN_MILLIS,
                   DateUtils.FORMAT_ABBREV_RELATIVE
               )
               val fMessage = String.format("Timer set — playback stops %1s", text)
                   //context.getString(Res.string.msg_sleep_timer_set_playback_stops_s, text)
               showPlatformToast(fMessage)
           }
       }
    }

    override fun shuffle(enable: Boolean) {
        tryLaunch {
            controller.shuffle(enable)
           // showPlatformToast(if (enable) R.string.shuffle else R.string.shuffle)
        }
    }

    override fun cycleRepeatMode() {
        tryLaunch {
            val new = controller.cycleRepeatMode()
            /*val msg = when (new) {
                PlaybackController.REPEAT_MODE_OFF -> R.string.msg_repeat_mode_off
                PlaybackController.REPEAT_MODE_ONE -> R.string.msg_repeat_mode_one
                else -> R.string.msg_repeat_mode_all
            }
            showPlatformToast(msg)*/
        }
    }

    override fun clear() {
        viewModelScope.launch {
            val permission = showToast(
                R.string.msg_clearing_playing_queue,
                R.string.remove,
                icon = Icons.Outlined.RemoveCircleOutline,
                accent = Color.OrientRed
            )
            if (permission == Toast.ACTION_PERFORMED)
                controller.clear()
        }
    }

    var autohideJob: Job? = null
    override fun emit(newVisibility: Int, delayed: Boolean) {
        autohideJob?.cancel()
        if (!delayed) {
            this@ConsoleViewModel2.visibility = newVisibility
            return
        }
        autohideJob = viewModelScope.launch {
            delay(RouteConsole.VISIBILITY_AUTO_HIDE_DELAY)
            this@ConsoleViewModel2.visibility = newVisibility
        }
    }

    override fun toggleLike(uri: Uri?) {
        tryLaunch {
            controller.toggleLike(uri)
        }
    }

    override fun remove(key: Uri) {
        viewModelScope.launch {
            controller.remove(key)
        }
    }

    override fun delete(key: Uri, resolver: Activity) {
        TODO("Not yet implemented")
    }

    override fun skipTo(key: Uri) {
        viewModelScope.launch {
            controller.skipTo(key)
        }
    }

    override fun seekBy(mills: Long) {
        tryLaunch {
            controller.seekBy(mills)
        }
    }


    override suspend fun getAvailableTracks(type: Int): List<TrackInfo> = controller.getAvailableTracks(type)

    override suspend fun getCheckedTrack(type: Int): TrackInfo? = controller.getSelectedTrackFor(type)

    override fun setCheckedTrack(type: Int, info: TrackInfo?) {
        tryLaunch {
            controller.setCheckedTrack(info, type)
        }
    }

    override fun pause() {
        tryLaunch {
            controller.pause()
        }
    }
}
package com.zs.audiofy.common.impl

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.zs.audiofy.console.ConsoleViewState
import com.zs.audiofy.console.RouteConsole
import com.zs.core.playback.MediaFile
import com.zs.core.playback.NowPlaying
import com.zs.core.playback.Remote
import com.zs.core.playback.VideoProvider
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ConsoleViewModel(val remote: Remote) : KoinViewModel(), ConsoleViewState {
    override val state: StateFlow<NowPlaying?> = remote.state
    override var provider: VideoProvider by mutableStateOf(VideoProvider(null))

    override var visibility: Int by mutableIntStateOf(RouteConsole.VISIBILITY_VISIBLE)
    override var message: CharSequence? by mutableStateOf(null)

    override val queue: Flow<List<MediaFile>>
        get() = TODO("Not yet implemented")

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

    override fun shuffle(enable: Boolean) {
        runCatching {
            remote.shuffle(enable)
            showPlatformToast("Shuffled")
        }
    }

    override fun cycleRepeatMode() {
        runCatching {
            val new = remote.cycleRepeatMode()
            val msg = when (new) {
                Remote.REPEAT_MODE_OFF -> "Repeat mode off"
                Remote.REPEAT_MODE_ONE -> "Repeat mode one"
                else -> "Repeat mode all"
            }
            showPlatformToast(msg)
        }
    }

    override fun toggleLike() {
        viewModelScope.launch { remote.toggleLike() }
    }
}
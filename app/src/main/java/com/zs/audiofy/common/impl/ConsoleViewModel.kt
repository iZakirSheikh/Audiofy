package com.zs.audiofy.common.impl

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.zs.audiofy.console.ConsoleViewState
import com.zs.core.db.playlists.Playlists
import com.zs.core.playback.NowPlaying
import com.zs.core.playback.Remote
import com.zs.core.playback.VideoProvider
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class ConsoleViewModel(val remote: Remote, playlists: Playlists): KoinViewModel(), ConsoleViewState {
    override val state: StateFlow<NowPlaying?> = remote.state

    override val isLiked: Boolean get() = false
    override var provider: VideoProvider by mutableStateOf(VideoProvider(null))

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
            val msg = when(new){
                Remote.REPEAT_MODE_OFF -> "Repeat mode off"
                Remote.REPEAT_MODE_ONE -> "Repeat mode one"
                else  -> "Repeat mode all"
            }
            showPlatformToast(msg)
        }
    }

    override fun addToLiked(uri: Uri) {
        runCatching {

        }
    }
}
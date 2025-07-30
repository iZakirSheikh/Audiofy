package com.zs.audiofy.common.impl

import com.zs.audiofy.console.ConsoleViewState
import com.zs.core.playback.NowPlaying
import com.zs.core.playback.Remote
import kotlinx.coroutines.flow.Flow


class ConsoleViewModel(val remote: Remote): KoinViewModel(), ConsoleViewState {
    override val state: Flow<NowPlaying?> = remote.state
}
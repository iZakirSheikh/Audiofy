package com.prime.player.audio.library

import android.graphics.Bitmap
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prime.player.common.asComposeState
import com.prime.player.core.Playback
import com.prime.player.core.Remote
import com.prime.player.core.Repository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

private const val TAG = "LibraryViewModel"

private const val CAROUSAL_DELAY_MILLS = 10_000L // 10 seconds

/**
 * Stop observing as soon as timeout.
 */
private val TimeOutPolicy = SharingStarted.Lazily

@HiltViewModel
class LibraryViewModel @Inject constructor(
    repository: Repository
) : ViewModel() {

    val reel = mutableStateOf<Pair<Long, Bitmap>?>(
        null
    )

    /**
     * The recently played tracks.
     */
    val recent =
        // replace with actual recent playlist
        repository.playlist(
            repository.getPlaylist(Playback.PLAYLIST_RECENT)?.id ?: 0
        )


    val carousel =
        repository
            .carousel
            .transform { list ->
                if (list.isEmpty()) {
                    emit(null)
                    return@transform
                }
                var current = 0
                while (true) {
                    if (current >= list.size)
                        current = 0
                    emit(list[current])
                    current++
                    kotlinx.coroutines.delay(CAROUSAL_DELAY_MILLS)
                }
            }
            .stateIn(viewModelScope, TimeOutPolicy, null)
}
package com.prime.media.impl

import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prime.media.core.compose.Channel
import com.prime.media.core.playback.Playback
import com.prime.media.core.playback.Remote
import com.prime.media.library.Library
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import javax.inject.Inject

private const val TAG = "LibraryViewModel"

private const val CAROUSAL_DELAY_MILLS = 10_000L // 10 seconds

/**
 * Stop observing as soon as timeout.
 */
private val TimeOutPolicy = SharingStarted.Lazily

private const val SHOW_CASE_MAX_ITEMS = 20

@HiltViewModel
class LibraryViewModel @Inject constructor(
    repository: Repository,
    remote: Remote,
    channel: Channel
) : ViewModel(), Library {

    override val recent = repository.playlist(Playback.PLAYLIST_RECENT)
    override val carousel = repository
        .recent(SHOW_CASE_MAX_ITEMS)
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

    override val newlyAdded = repository
        .observe(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
        .map {
            repository.getAudios(
                order = MediaStore.Audio.Media.DATE_MODIFIED,
                ascending = false,
                offset = 0,
                limit = SHOW_CASE_MAX_ITEMS
            )
        }
}
package com.prime.media.impl

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.Error
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import com.prime.media.R
import com.prime.media.core.compose.Channel
import com.prime.media.core.db.toMediaItem
import com.prime.media.core.db.uri
import com.prime.media.core.playback.MediaItem
import com.prime.media.core.playback.Playback
import com.prime.media.core.playback.Remote
import com.prime.media.core.util.toMediaItem
import com.prime.media.library.Library
import com.primex.core.DahliaYellow
import com.primex.core.MetroGreen
import com.primex.core.Rose
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
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
    private val remote: Remote,
    private val channel: Channel
) : ViewModel(), Library {

    override val recent = repository.playlist(Playback.PLAYLIST_RECENT)
        .stateIn(viewModelScope, TimeOutPolicy, null)

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
        .stateIn(viewModelScope, TimeOutPolicy, null)

    override fun onClickRecentFile(uri: String) {
        viewModelScope.launch {
            // Check if the media item is already in the playlist.
            val isAlreadyInPlaylist = remote.seekTo(Uri.parse(uri))
            // If the media item is already in the playlist, just play it.
            if (isAlreadyInPlaylist) {
                remote.play(true)
                return@launch
            }
            // Get the history from the recent list, if it exists.
            val items = recent.firstOrNull()
            val item = items?.find { it.uri == uri }
            // If the media item is not found in the recent list, show an error message.
            if (item == null) {
                channel.show(R.string.msg_unknown_error, leading = Icons.Outlined.Error, accent = Color.Rose)
                return@launch
            }
            // Add the media item to the playlist.
            remote.add(item.toMediaItem, index = remote.nextIndex)
            // seek to the item
            remote.seekTo(Uri.parse(uri))
            // play it
            remote.play(true)
        }
    }

    override fun onClickRecentAddedFile(id: Long) {
        viewModelScope.launch {
            val items = newlyAdded.firstOrNull()
            val item = items?.find { it.id == id }
            if (item == null) {
                channel.show(
                    R.string.msg_unknown_error,
                    R.string.error,
                    leading = Icons.Outlined.Error,
                    accent = Color.Rose
                )
                return@launch
            }
            val isAlreadyInPlaylist = remote.seekTo(item.uri)
            // If the media item is already in the playlist, just play it.
            if (isAlreadyInPlaylist) {
                remote.play(true)
                return@launch
            }
            // Add the media item to the playlist.
            remote.add(item.toMediaItem, index = remote.nextIndex)
            // seek to the item
            remote.seekTo(item.uri)
            // play it
            remote.play(true)
        }
    }

    override fun onRequestPlayVideo(uri: Uri, context: Context) {
        viewModelScope.launch {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            val item = MediaItem(context, uri)
            remote.set(item)
            remote.play(true)
        }
    }
}
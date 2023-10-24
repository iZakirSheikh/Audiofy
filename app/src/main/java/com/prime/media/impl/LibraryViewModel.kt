package com.prime.media.impl

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ClearAll
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.Message
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

    override fun onClickRecentFile(uri: String) {
        viewModelScope.launch {
            val isAlreadyInPlaylist = remote.seekTo(Uri.parse(uri))
            // since it is already in the playlist seek to it and return; it will start playing
            // maybe call play
            if (isAlreadyInPlaylist)
                return@launch
            // Since the item is definitely not in the queue, present a message to the user to inquire
            // about their preference. If the user agrees, the playlist will be reset, and the recent
            // item will be added to the playlist, initiating playback from this item's index.
            // If the user decides otherwise, the item will be added to the queue following the current queue order.
            val res = channel.show(
                R.string.msg_library_recent_click,
                action = R.string.reset,
                leading = Icons.Outlined.Message,
                accent = Color.MetroGreen
            )
            val files = recent.firstOrNull()
            val file = files?.find { it.uri == uri }
            if (files == null || file == null) {
                channel.show(
                    R.string.msg_unknown_error,
                    leading = Icons.Outlined.Error,
                    accent = Color.Rose
                )
                return@launch
            }
            if (res != Channel.Result.ActionPerformed) {
                remote.add(file.toMediaItem, index = remote.nextIndex)
                remote.seekTo(Uri.parse(uri))
                return@launch
            }
            remote.clear()
            val index = files.indexOf(file)
            remote.set(files.map { it.toMediaItem })
            remote.seekTo(index.coerceAtLeast(0), C.TIME_UNSET)
            remote.play(true)
        }
    }

    override fun onClickRecentAddedFile(id: Long) {
        viewModelScope.launch {
            // null case should not happen; bacese that measns some weired error.
            val files = newlyAdded.firstOrNull()
            val item = files?.find { it.id == id }
            if (files == null || item == null) {
                channel.show(
                    R.string.msg_unknown_error,
                    R.string.error,
                    leading = Icons.Outlined.Error,
                    accent = Color.Rose
                )
                return@launch
            }
            val isAlreadyInPlaylist = remote.seekTo(item.uri)
            // since it is already in the playlist seek to it and return; it will start playing
            // maybe call play
            if (isAlreadyInPlaylist)
                return@launch
            val res = channel.show(
                R.string.msg_library_recently_added_click,
                action = R.string.reset,
                leading = Icons.Outlined.ClearAll,
                accent = Color.DahliaYellow
            )
            // just return
            if (res != Channel.Result.ActionPerformed)
                return@launch
            val index = files.indexOf(item)
            remote.set(files.map { it.toMediaItem })
            remote.seekTo(index.coerceAtLeast(0), C.TIME_UNSET)
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
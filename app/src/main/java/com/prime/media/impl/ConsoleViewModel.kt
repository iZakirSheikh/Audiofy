package com.prime.media.impl

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.prime.media.R
import com.prime.media.console.Console
import com.prime.media.core.compose.Channel
import com.prime.media.core.db.Playlist
import com.prime.media.core.playback.Playback
import com.prime.media.core.playback.Playback.Companion.UNINITIALIZED_SLEEP_TIME_MILLIS
import com.prime.media.core.playback.Remote
import com.prime.media.core.util.MainHandler
import com.primex.core.OrientRed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ConsoleViewModel"

/**
 * The token to add and remove progress
 */
private val PROGRESS_TOKEN = MainHandler.token

/**
 * Factory function that creates a `Member` object with the given `MediaItem` object and some additional metadata.
 *
 * @param from The `MediaItem` object to create the `Member` from.
 * @param playlistId The ID of the playlist to which this `Member` belongs.
 * @param order The order of this `Member` within the playlist.
 * @return A new `Member` object with the given parameters.
 */
fun Member(from: MediaItem, playlistId: Long, order: Int) =
    Playlist.Member(
        playlistId,
        from.mediaId,
        order,
        from.requestMetadata.mediaUri!!.toString(),
        from.mediaMetadata.title.toString(),
        from.mediaMetadata.subtitle.toString(),
        from.mediaMetadata.artworkUri?.toString()
    )

@HiltViewModel
class ConsoleViewModel @Inject constructor(
    private val remote: Remote,
    private val repository: Repository,
    private val toaster: Channel
) : ViewModel(), Console {

    override var playing: Boolean by mutableStateOf(false)
    override var repeatMode: Int by mutableIntStateOf(Player.REPEAT_MODE_OFF)
    override var current: MediaItem? by mutableStateOf(null)
    override var favourite: Boolean by mutableStateOf(false)
    override var playbackSpeed: Float get() =  remote.playbackSpeed
        set(value) { remote.playbackSpeed = value }
    override var position: Long by mutableLongStateOf(0)

    override val isLast: Boolean get() = remote.next == null
    override var shuffle: Boolean by mutableStateOf(false)
    override val isFirst: Boolean get() = !remote.hasPreviousTrack
    override val duration: Long get() = remote.duration
    override val audioSessionId: Int get() = remote.audioSessionId
    override var sleepAfterMills by mutableLongStateOf(UNINITIALIZED_SLEEP_TIME_MILLIS)


    override val queue: Flow<List<MediaItem>> = remote.queue

    override fun togglePlay() = remote.togglePlay()
    override fun skipToNext() = remote.skipToNext()
    override fun skipToPrev() = remote.skipToPrev()
    override fun playTrackAt(position: Int) = remote.playTrackAt(position)
    override fun playTrack(uri: Uri) = remote.playTrack(uri)

    override fun remove(context: Context, key: Uri) {
        viewModelScope.launch {
            val removed = remote.remove(key)
            val msg = if (removed) R.string.msg_track_will_be_removed else R.string.msg_track_remove_error
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun cycleRepeatMode() {
        viewModelScope.launch {
            remote.cycleRepeatMode()
        }
    }

    override fun clear(context: Context) {
        viewModelScope.launch {
            Toast.makeText(context, R.string.msg_clearing_playing_queue, Toast.LENGTH_SHORT).show()
            remote.clear()
        }
    }

    override fun seekTo(mills: Long) {
        viewModelScope.launch {
            // update the state.
            if (duration == C.TIME_UNSET)
                return@launch
            val upto = mills.coerceIn(0, duration)
            position = upto
            remote.seekTo(upto)
        }
    }

    override fun setSleepAfter(mills: Long) {
        viewModelScope.launch {
            // if not playing don't change.
            if (!playing)
                return@launch
            remote.setSleepTimeAt(mills)
            sleepAfterMills = if (mills == UNINITIALIZED_SLEEP_TIME_MILLIS) mills else mills - System.currentTimeMillis()
        }
    }

    override fun toggleFav() {
        viewModelScope.launch {
            val item = current ?: return@launch
            // the playlist is created already.
            val uri = item.requestMetadata.mediaUri.toString()
            val playlist = repository.getPlaylist(Playback.PLAYLIST_FAVOURITE) ?: return@launch
            var favourite = repository.isFavourite(uri)
            val res = if (favourite)
                repository.removeFromPlaylist(playlist.id, uri)
            else
                repository.insert(
                    Member(item, playlist.id, (repository.getLastPlayOrder(playlist.id) ?: 0) + 1)
                )
            // update the favourite
            this@ConsoleViewModel.favourite = !favourite && res
            if (!res)
                toaster.show(
                    message = R.string.msg_error_fav_playlist_update,
                    leading = R.drawable.ic_heart,
                    accent = Color.OrientRed
                )
        }
    }

    override fun toggleShuffle() {
        viewModelScope.launch {
            val newValue = !remote.shuffle
            remote.shuffle = newValue
            shuffle = newValue
        }
    }

    // listener to progress changes.
    private val onProgressUpdate = {
        position = remote.position
        viewModelScope.launch {
            val scheduled = remote.getSleepTimeAt()
            sleepAfterMills = if (scheduled == UNINITIALIZED_SLEEP_TIME_MILLIS) scheduled else scheduled - System.currentTimeMillis()
        }
        Unit
    }

    private fun onPlayerEvent(event: Int) {
        when (event) {
            // when state of playing.
            // FixMe: This event is triggered more often e.g., when track is changed.
            // may be add some more check.
            Player.EVENT_PLAY_WHEN_READY_CHANGED -> {
                playing = remote.playWhenReady
                sleepAfterMills = UNINITIALIZED_SLEEP_TIME_MILLIS
                if (remote.playWhenReady)
                    MainHandler.repeat(PROGRESS_TOKEN, 1000, call = onProgressUpdate)
                else
                    MainHandler.remove(PROGRESS_TOKEN)
            }
            // update the shuffle mode.
            Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED ->
                shuffle = remote.shuffle

            // and repeat mode.
            Player.EVENT_REPEAT_MODE_CHANGED ->
                repeatMode = remote.repeatMode

            Player.EVENT_MEDIA_ITEM_TRANSITION -> {
                viewModelScope.launch {
                    val mediaItem = remote.current
                    //FixMe: Remove dependency on ID.
                    val uri = mediaItem?.requestMetadata?.mediaUri?.toString() ?: ""
                    favourite = repository.isFavourite(uri)
                    // update the current media id.
                    current = mediaItem
                }
            }
        }
    }

    // Observe the flow for changes.
    init {
        viewModelScope.launch {
            remote.events
               // .onStart { emit(null) }
                .collect {
                    if (it == null) {
                        // init with events.
                        sleepAfterMills = remote.getSleepTimeAt()
                        position = remote.position
                        onPlayerEvent(event = Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED)
                        onPlayerEvent(event = Player.EVENT_REPEAT_MODE_CHANGED)
                        onPlayerEvent(Player.EVENT_MEDIA_ITEM_TRANSITION)
                        onPlayerEvent(Player.EVENT_PLAY_WHEN_READY_CHANGED)
                        return@collect
                    }
                    // emit the event.
                    repeat(it.size()) { index ->
                        onPlayerEvent(it.get(index))
                    }
                }
        }
    }

    override fun onCleared() {
        MainHandler.remove(PROGRESS_TOKEN)
        super.onCleared()

    }
}
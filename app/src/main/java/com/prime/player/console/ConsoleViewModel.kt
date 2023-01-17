package com.prime.player.console

import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.prime.player.R
import com.prime.player.common.ToastHostState
import com.prime.player.common.show
import com.prime.player.core.*
import com.prime.player.core.db.Audio
import com.prime.player.core.db.Playlist
import com.prime.player.core.playback.Remote
import com.primex.core.Text
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject


private inline fun <T> stateOf(value: T): State<T> = mutableStateOf(value)

private const val TAG = "HomeViewModel"

/**
 * The token to add and remove progress
 */
private val PROGRESS_TOKEN = MainHandler.token

private inline val Remote.progress get() = (position / duration.toFloat())

@HiltViewModel
class ConsoleViewModel @Inject constructor(
    private val remote: Remote,
    private val repository: Repository,
) : ViewModel() {

    /**
     * This channel must be set from composable.
     */
    var messenger: ToastHostState? = null
    val playing = stateOf(remote.isPlaying)
    val repeatMode = stateOf(remote.repeatMode)
    val progress = stateOf(0f)
    val sleepAfter = stateOf<Long?>(null)
    val current = stateOf<MediaItem?>(null)
    val shuffle = stateOf(false)
    val artwork = stateOf<Uri?>(null)
    val favourite = stateOf(false)

    val playlists = repository.playlists
    val queue = remote.queue

    // getters.
    val hasNextTrack: Boolean get() = remote.next != null
    val hasPreviousTrack: Boolean get() = remote.hasPreviousTrack
    val playbackSpeed get() = remote.playbackSpeed
    val duration get() = remote.duration
    val audioSessionId get() = remote.audioSessionId

    // listener to progress changes.
    private val onProgressUpdate = {
        (progress as MutableState).value = remote.progress
    }

    private fun onPlayerEvent(event: Int) {
        when (event) {
            // when state of playing.
            // FixMe: This event is triggered more often e.g., when track is changed.
            // may be add some more check.
            Player.EVENT_IS_PLAYING_CHANGED -> {
                (playing as MutableState).value = remote.isPlaying
                if (remote.isPlaying)
                    MainHandler.repeat(PROGRESS_TOKEN, 1000, call = onProgressUpdate)
                else
                    MainHandler.remove(PROGRESS_TOKEN)
            }
            // update the shuffle mode.
            Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED ->
                (shuffle as MutableState).value = remote.shuffle

            // and repeat mode.
            Player.EVENT_REPEAT_MODE_CHANGED ->
                (repeatMode as MutableState).value = remote.repeatMode

            Player.EVENT_MEDIA_ITEM_TRANSITION -> {
                viewModelScope.launch {
                    val mediaItem = remote.current
                    //FixMe: Remove dependency on ID.
                    val id = mediaItem?.mediaId?.toLong() ?: -1
                    (favourite as MutableState).value = repository.isFavourite(id)
                    // update the current media id.
                    (current as MutableState).value = mediaItem
                    (this@ConsoleViewModel.artwork as MutableState).value =
                        mediaItem?.mediaMetadata?.artworkUri
                }
            }
        }
    }

    // Observe the flow for changes.
    init {
        viewModelScope.launch {
            remote.events
                .collect {
                    if (it == null) {
                        // init with events.
                        onPlayerEvent(event = Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED)
                        onPlayerEvent(event = Player.EVENT_REPEAT_MODE_CHANGED)
                        onPlayerEvent(Player.EVENT_IS_PLAYING_CHANGED)
                        onPlayerEvent(Player.EVENT_MEDIA_ITEM_TRANSITION)
                        return@collect
                    }
                    // emit the event.
                    repeat(it.size()) { index ->
                        onPlayerEvent(it.get(index))
                    }
                }
        }
    }

    fun togglePlay() = remote.togglePlay()

    fun skipToNext() = remote.skipToNext()

    fun skipToPrev() = remote.skipToPrev()

    fun cycleRepeatMode() {
        viewModelScope.launch {
            remote.cycleRepeatMode()
            val newMode = remote.repeatMode
            // (repeatMode as MutableState).value = newMode

            messenger?.show(
                message = when (newMode) {
                    Player.REPEAT_MODE_OFF -> "Repeat mode none."
                    Player.REPEAT_MODE_ALL -> "Repeat mode all."
                    else -> "Repeat mode one."
                }
            )
        }
    }


    fun seekTo(mills: Long) {
        viewModelScope.launch {
            // update the state.
            (progress as MutableState).value = mills / duration.toFloat()
            remote.seekTo(mills)
        }
    }

    /**
     * Seek pct of [Remote.duration]
     */
    fun seekTo(pct: Float) = seekTo(
        (pct * remote.duration).toLong()
    )

    fun setSleepAfter(minutes: Int) {
        viewModelScope.launch {
            // currently not available.
            messenger?.show(
                message = "Temporarily disabled!!"
            )
        }
    }

    fun addToPlaylist(playlist: Playlist, value: Audio) {
        viewModelScope.launch {
            repository.addToPlaylist(
                value.id,
                playlist.name
            )
        }
    }


    fun toggleFav() {
        viewModelScope.launch {
            // service?.to
            //service?.toggleFav()
            val id = current.value?.mediaId?.toLong() ?: return@launch
            val favourite = repository.toggleFav(id)
            (this@ConsoleViewModel.favourite as MutableState).value = favourite
            messenger?.show(
                message = when (favourite) {
                    true -> Text(R.string.msg_fav_added)
                    else -> Text(R.string.msg_fav_removed)
                }
            )
        }
    }

    fun toggleShuffle() {
        viewModelScope.launch {
            remote.toggleShuffle()
            val newValue = remote.shuffle
            (shuffle as MutableState).value = newValue
            messenger?.show(
                message = if (newValue) "Shuffle enabled." else "Shuffle disabled."
            )
        }
    }

    fun playTrackAt(position: Int) = remote.playTrackAt(position)

    @Deprecated("write new one independent of id.")
    fun playTrack(id: Long) = remote.playTrack(id)


    @Deprecated("find alternate.")
    fun addToPlaylist(id: Playlist) {
        val audioId = current.value?.mediaId?.toLong() ?: return
        viewModelScope.launch {
            repository.addToPlaylist(
                audioId,
                id.name
            )
        }
    }

    fun remove(key: Uri) {
        viewModelScope.launch {
            remote.remove(key)
        }
    }

    fun replay10() =
        seekTo((remote.position - (10 * 1000)).coerceAtLeast(0L))

    fun forward30() =
        seekTo((remote.position + (30 * 1000)).coerceAtMost(remote.duration))

    fun setPlaybackSpeed(value: Float) {
        remote.playbackSpeed = value
    }
}
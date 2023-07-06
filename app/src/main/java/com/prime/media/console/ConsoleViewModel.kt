package com.prime.media.console

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreTime
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.prime.media.R
import com.prime.media.core.compose.Channel
import com.prime.media.core.db.Playlist
import com.prime.media.core.playback.Playback
import com.prime.media.core.playback.Remote
import com.prime.media.core.util.MainHandler
import com.prime.media.core.util.Member
import com.prime.media.impl.Repository
import com.prime.media.core.util.key
import com.primex.core.Amber
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
    private val toaster: Channel
) : ViewModel() {
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
                    val uri = mediaItem?.requestMetadata?.mediaUri?.toString() ?: ""
                    (favourite as MutableState).value = repository.isFavourite(uri)
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

            toaster.show(
                title = "Repeat Mode",
                message = when (newMode) {
                    Player.REPEAT_MODE_OFF -> "Repeat mode none."
                    Player.REPEAT_MODE_ALL -> "Repeat mode all."
                    else -> "Repeat mode one."
                },
                leading = R.drawable.ic_repeat
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
            toaster.show(
                message = "Temporarily disabled!!",
                title = "Working on it.",
                accent = Color.Amber,
                leading = Icons.Outlined.MoreTime
            )
        }
    }

    fun toggleFav() {
        viewModelScope.launch {
            val item = current.value ?: return@launch
            // the playlist is created already.
            val playlist = repository.getPlaylist(Playback.PLAYLIST_FAVOURITE) ?: return@launch
            val favourite = repository.isFavourite(item.key)
            val res = if (favourite)
                repository.removeFromPlaylist(playlist.id, item.key)
            else
                repository.insert(
                    Member(
                        item,
                        playlist.id,
                        (repository.getLastPlayOrder(playlist.id) ?: 0) + 1
                    )
                )
            // update teh favourite
            (this@ConsoleViewModel.favourite as MutableState).value = !favourite && res
            toaster.show(
                message = when {
                    !res -> Text("An error occured while adding/removing the item to favourite playlist")
                    !favourite -> Text(R.string.msg_fav_added)
                    else -> Text(R.string.msg_fav_removed)
                },
                title = Text("Favourites"),
                leading = R.drawable.ic_heart
            )
        }
    }

    fun toggleShuffle() {
        viewModelScope.launch {
            val newValue = remote.shuffle
            remote.shuffle = newValue
            (shuffle as MutableState).value = newValue
            toaster.show(
                message = if (newValue) "Shuffle enabled." else "Shuffle disabled.",
                title = "Shuffle",
                leading = R.drawable.ic_shuffle,
            )
        }
    }

    fun playTrackAt(position: Int) = remote.playTrackAt(position)

    @Deprecated("write new one independent of id.")
    fun playTrack(id: Long) = remote.playTrack(id)

    fun playTrack(uri: Uri) = remote.playTrack(uri)

    @Deprecated("find alternate.")
    fun addToPlaylist(id: Playlist) {
        val item = current.value ?: return
        viewModelScope.launch {
            /*TODO: Add necessary logic*/
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
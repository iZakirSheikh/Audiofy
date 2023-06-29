package com.prime.media.impl

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
import com.prime.media.console.Console
import com.prime.media.core.compose.channel.Channel
import com.prime.media.core.db.Playlist
import com.prime.media.core.playback.Playback
import com.prime.media.core.playback.Remote
import com.prime.media.core.util.MainHandler
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
    private val channel: Channel
) : ViewModel(), Console {
    override val playing = stateOf(remote.isPlaying)
    override val repeatMode = stateOf(remote.repeatMode)
    override val progress = stateOf(0f)
    override val sleepAfter = stateOf<Long?>(null)
    override val current = stateOf<MediaItem?>(null)
    override val shuffle = stateOf(false)
    override val artwork = stateOf<Uri?>(null)
    override val favourite = stateOf(false)

    override val playlists = repository.playlists
    override val queue = remote.queue

    // getters.
    override val hasNextTrack: Boolean get() = remote.next != null
    override val hasPreviousTrack: Boolean get() = remote.hasPreviousTrack
    override val playbackSpeed get() = remote.playbackSpeed
    override val duration get() = remote.duration
    override val audioSessionId get() = remote.audioSessionId

    // listener to progress changes.
    override val onProgressUpdate = {
        (progress as MutableState).value = remote.progress
    }

    override fun onPlayerEvent(event: Int) {
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

    override fun togglePlay() = remote.togglePlay()

    override fun skipToNext() = remote.skipToNext()

    override fun skipToPrev() = remote.skipToPrev()

    override fun cycleRepeatMode() {
        viewModelScope.launch {
            remote.cycleRepeatMode()
            val newMode = remote.repeatMode
            // (repeatMode as MutableState).value = newMode

            channel.show(
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

    override fun seekTo(mills: Long) {
        viewModelScope.launch {
            // update the state.
            (progress as MutableState).value = mills / duration.toFloat()
            remote.seekTo(mills)
        }
    }

    /**
     * Seek pct of [Remote.duration]
     */
    override fun seekTo(pct: Float) = seekTo(
        (pct * remote.duration).toLong()
    )

    override fun setSleepAfter(minutes: Int) {
        viewModelScope.launch {
            // currently not available.
            channel.show(
                message = "Temporarily disabled!!",
                title = "Working on it.",
                accent = Color.Amber,
                leading = Icons.Outlined.MoreTime
            )
        }
    }

    override fun toggleFav() {
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
            channel.show(
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

    override fun toggleShuffle() {
        viewModelScope.launch {
            val newValue = remote.shuffle
            remote.shuffle = newValue
            (shuffle as MutableState).value = newValue
            channel.show(
                message = if (newValue) "Shuffle enabled." else "Shuffle disabled.",
                title = "Shuffle",
                leading = R.drawable.ic_shuffle,
            )
        }
    }

    override fun playTrackAt(position: Int) = remote.playTrackAt(position)

    @Deprecated("write new one independent of id.")
    override fun playTrack(id: Long) = remote.playTrack(id)

    override fun playTrack(uri: Uri) = remote.playTrack(uri)

    @Deprecated("find alternate.")
    override fun addToPlaylist(id: Playlist) {
        val item = current.value ?: return
        viewModelScope.launch {
            /*TODO: Add necessary logic*/
        }
    }

    override fun remove(key: Uri) {
        viewModelScope.launch {
            remote.remove(key)
        }
    }

    override fun replay10() =
        seekTo((remote.position - (10 * 1000)).coerceAtLeast(0L))

    override fun forward30() =
        seekTo((remote.position + (30 * 1000)).coerceAtMost(remote.duration))

    override fun setPlaybackSpeed(value: Float) {
        remote.playbackSpeed = value
    }
}
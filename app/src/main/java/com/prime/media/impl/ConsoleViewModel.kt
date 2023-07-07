package com.prime.media.impl

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.MoreTime
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.prime.media.R
import com.prime.media.console.Console
import com.prime.media.core.compose.Channel
import com.prime.media.core.playback.Playback
import com.prime.media.core.playback.Remote
import com.prime.media.core.util.MainHandler
import com.prime.media.core.util.Member
import com.prime.media.core.util.key
import com.primex.core.Amber
import com.primex.core.Text
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "ConsoleViewModel"

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
) : ViewModel(), Console {

    override var playing: Boolean by mutableStateOf(false)
    override var repeatMode: Int by mutableIntStateOf(Player.REPEAT_MODE_OFF)
    override var progress: Float by mutableFloatStateOf(0.0f)
    override var current: MediaItem? by mutableStateOf(null)
    override var favourite: Boolean by mutableStateOf(false)
    override val isLast: Boolean get() = remote.next == null
    override var shuffle: Boolean by mutableStateOf(false)
    override val isFirst: Boolean get() = !remote.hasPreviousTrack
    override val duration: Long get() = remote.duration
    override val audioSessionId: Int get() = remote.audioSessionId
    override var playbackSpeed: Float = remote.playbackSpeed

    override val queue: Flow<List<MediaItem>> = remote.queue

    override fun togglePlay() = remote.togglePlay()

    override fun skipToNext() = remote.skipToNext()

    override fun skipToPrev() = remote.skipToPrev()

    override fun cycleRepeatMode(){
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

    override fun seekTo(mills: Long) {
        viewModelScope.launch {
            // update the state.
            progress = mills / duration.toFloat()
            remote.seekTo(mills)
        }
    }

    override fun setSleepAfter(minutes: Int) {
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

    override fun toggleFav() {
        viewModelScope.launch {
            val item = current ?: return@launch
            // the playlist is created already.
            val playlist = repository.getPlaylist(Playback.PLAYLIST_FAVOURITE) ?: return@launch
            var favourite = repository.isFavourite(item.key)
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
            favourite = !favourite && res
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

    override fun replay10() {
        seekTo((remote.position - (10 * 1000)).coerceAtLeast(0L))
    }

    override fun forward30() {
        seekTo((remote.position - (10 * 1000)).coerceAtLeast(0L))
    }

    override fun playTrackAt(position: Int) = remote.playTrackAt(position)

    override fun playTrack(uri: Uri)  = remote.playTrack(uri)

    override fun remove(key: Uri) {
        viewModelScope.launch {
            remote.remove(key)
        }
    }

    override fun toggleShuffle() {
        viewModelScope.launch {
            val newValue = remote.shuffle
            remote.shuffle = newValue
            shuffle = newValue
            toaster.show(
                message = if (newValue) "Shuffle enabled." else "Shuffle disabled.",
                title = "Shuffle",
                leading = R.drawable.ic_shuffle,
            )
        }
    }

    // listener to progress changes.
    private val onProgressUpdate = {
        progress = remote.progress
    }

    private fun onPlayerEvent(event: Int) {
        when (event) {
            // when state of playing.
            // FixMe: This event is triggered more often e.g., when track is changed.
            // may be add some more check.
            Player.EVENT_IS_PLAYING_CHANGED -> {
                playing = remote.isPlaying
                if (remote.isPlaying)
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
}
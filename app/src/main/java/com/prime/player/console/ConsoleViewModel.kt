package com.prime.player.console

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.Player
import com.prime.player.Audiofy
import com.prime.player.R
import com.prime.player.common.ToastHostState
import com.prime.player.common.show
import com.prime.player.core.MainHandler
import com.prime.player.core.Repository
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


    private val onProgressUpdate = {
        (progress as MutableState).value = remote.progress
    }


    /**
     * This channel must be set from composable.
     */
    var messenger: ToastHostState? = null
    val playing = stateOf(remote.isPlaying)
    val repeatMode = stateOf(remote.repeatMode)
    val progress = stateOf(remote.progress)
    val sleepAfter = stateOf<Long?>(null)
    val current = stateOf<Audio?>(null)
    val next: State<Audio?> = stateOf<Audio?>(null)
    val shuffle = stateOf(false)
    val playlistName = stateOf("Unknown")
    val artwork = stateOf(Audiofy.DEFAULT_ALBUM_ART)
    val favourite = stateOf(false)

    val playlists = repository.playlists
    val queue = remote.queue

    init {

        // observe events of the player.
        viewModelScope.launch {
            remote.events.collect {
                    if (it == null) {
                        // init with events.
                        onPlayerEvent(value = Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED)
                        onPlayerEvent(value = Player.EVENT_REPEAT_MODE_CHANGED)
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

    private suspend fun onPlayerEvent(value: Int) {
        when (value) {
            // when playing state is changed.
            // update progress only when isPlaying is changed.
            Player.EVENT_IS_PLAYING_CHANGED -> {
                (playing as MutableState).value = remote.isPlaying
                if (remote.isPlaying) MainHandler.repeat(
                    PROGRESS_TOKEN,
                    1000,
                    call = onProgressUpdate
                )
                else MainHandler.remove(PROGRESS_TOKEN)
            }
            // when media is changed.
            // update favourite, current next etc.
            Player.EVENT_MEDIA_ITEM_TRANSITION -> {
                val id = remote.current?.mediaId?.toLong() ?: -1
                val item = repository.getAudioById(id)
                (favourite as MutableState).value = repository.isFavourite(id)
                (current as MutableState).value = item
                val next = remote.next
                val nextId = next?.mediaId?.toLong() ?: -1
                (this@ConsoleViewModel.next as MutableState).value = repository.getAudioById(nextId)
                (this@ConsoleViewModel.artwork as MutableState).value = remote.artwork()
            }

            // update next
            // update shuffle mode.
            Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED -> {
                (shuffle as MutableState).value = remote.shuffle
                val next = remote.next
                val nextId = next?.mediaId?.toLong() ?: -1
                (this@ConsoleViewModel.next as MutableState).value = repository.getAudioById(nextId)
            }

            // update next
            // aldo the repeat mode.
            Player.EVENT_REPEAT_MODE_CHANGED -> {
                (repeatMode as MutableState).value = remote.repeatMode
                val next = remote.next
                val nextId = next?.mediaId?.toLong() ?: -1
                (this@ConsoleViewModel.next as MutableState).value = repository.getAudioById(nextId)
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

    fun seekTo(duration: Float) {
        val value = (duration * remote.duration)
        (progress as MutableState).value = duration
        remote.seekTo(value.toLong())
    }

    fun setSleepAfter(minutes: Int) {
        viewModelScope.launch {
            messenger?.show(
                message = "Temporarily disabled!!"
            )
        }
    }

    fun addToPlaylist(playlist: Playlist, value: Audio) {
        viewModelScope.launch {
            repository.addToPlaylist(
                value.id, playlist.name
            )
        }
    }

    fun toggleFav() {
        viewModelScope.launch {
            // service?.to
            //service?.toggleFav()
            val id = current.value?.id ?: return@launch
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

    fun playTrack(id: Long) = remote.playTrack(id)

    fun addToPlaylist(id: Playlist) {
        val audioId = current.value?.id ?: return
        viewModelScope.launch {
            repository.addToPlaylist(
                audioId, id.name
            )
        }
    }
}
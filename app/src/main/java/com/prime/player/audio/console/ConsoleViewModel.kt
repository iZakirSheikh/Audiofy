package com.prime.player.audio.console

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.SnapshotMutationPolicy
import androidx.compose.runtime.State
import androidx.compose.runtime.structuralEqualityPolicy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import com.prime.player.Audiofy
import com.prime.player.R
import com.prime.player.common.compose.ToastHostState
import com.prime.player.common.compose.show
import com.prime.player.core.*
import com.primex.core.Text
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject


private inline fun <T> mutableStateOf(
    value: T,
    policy: SnapshotMutationPolicy<T> = structuralEqualityPolicy()
): State<T> = androidx.compose.runtime.mutableStateOf(value, policy)

private const val TAG = "HomeViewModel"

@HiltViewModel
class ConsoleViewModel @Inject constructor(
    private val remote: Remote,
    private val repository: Repository,
) : ViewModel() {

    private val progressJob: Job? = null

    /**
     * This channel must be set from composable.
     */
    var messenger: ToastHostState? = null
    val playing = mutableStateOf(remote.isPLaying)
    val repeatMode = mutableStateOf(remote.repeatMode)
    val progress = mutableStateOf(remote.position.toFloat())
    val sleepAfter = mutableStateOf<Long?>(null)
    val current = mutableStateOf<Audio?>(null)
    val next: State<Audio?> = mutableStateOf<Audio?>(null)
    val shuffle = mutableStateOf(false)
    val playlistName = mutableStateOf("Unknown")
    val artwork = mutableStateOf(Audiofy.DEFAULT_ALBUM_ART)
    val playlists = repository.playlists
    val queue =
        remote.observe(Playback.ROOT_QUEUE)

    val favourite = mutableStateOf(false)

    private val listener =
        object : Player.Listener {

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                viewModelScope.launch {
                    val id = mediaItem?.mediaId?.toLong() ?: -1
                    val item = repository.getAudioById(id)
                    (favourite as MutableState).value = repository.isFavourite(id)
                    (current as MutableState).value = item
                    val next = remote.nextMediaItem
                    val nextId = next?.mediaId?.toLong() ?: -1
                    (this@ConsoleViewModel.next as MutableState).value =
                        repository.getAudioById(nextId)
                    (this@ConsoleViewModel.artwork as MutableState).value =
                        remote.artwork() ?: Audiofy.DEFAULT_ALBUM_ART
                }
            }

            override fun onIsPlayingChanged(isPlaying: Boolean) {
                (playing as MutableState).value = isPlaying
            }

            override fun onPlayerError(error: PlaybackException) {
                super.onPlayerError(error)
            }

            override fun onRepeatModeChanged(rm: Int) {
                (repeatMode as MutableState).value = rm
                viewModelScope.launch {
                    val next = remote.nextMediaItem
                    val nextId = next?.mediaId?.toLong() ?: -1
                    (this@ConsoleViewModel.next as MutableState).value =
                        repository.getAudioById(nextId)
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                (shuffle as MutableState).value = shuffleModeEnabled
                viewModelScope.launch {
                    val next = remote.nextMediaItem
                    val nextId = next?.mediaId?.toLong() ?: -1
                    (this@ConsoleViewModel.next as MutableState).value =
                        repository.getAudioById(nextId)
                }
            }
        }


    init {
        viewModelScope.launch {
            remote.await()
            remote.add(listener)
            listener.onMediaItemTransition(remote.current, Player.MEDIA_ITEM_TRANSITION_REASON_AUTO)
            listener.onIsPlayingChanged(remote.isPLaying)
            listener.onRepeatModeChanged(remote.repeatMode)
            listener.onShuffleModeEnabledChanged(remote.shuffle)

            // FixMe: infinite loop
            while (true) {
                delay(1000)
                (progress as MutableState).value = remote.position.toFloat()
            }
        }
    }

    override fun onCleared() {
        viewModelScope.launch { remote.remove(listener) }
        super.onCleared()
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
        (progress as MutableState).value = duration
        remote.seekTo(duration.toLong())
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
                value.id,
                playlist.name
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
                audioId,
                id.name
            )
        }
    }
}

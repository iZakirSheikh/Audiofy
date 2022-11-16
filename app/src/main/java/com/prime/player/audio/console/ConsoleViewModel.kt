package com.prime.player.audio.console

import android.annotation.SuppressLint
import android.app.Application
import android.content.ComponentName
import android.content.Context.BIND_ADJUST_WITH_ACTIVITY
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.*
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prime.player.Audiofy
import com.prime.player.R
import com.prime.player.common.Util
import com.prime.player.common.compose.Snack
import com.prime.player.common.compose.SnackDataChannel
import com.prime.player.common.formatAsDuration
import com.prime.player.common.getAlbumArt
import com.prime.player.core.Audio
import com.prime.player.core.Playlist
import com.prime.player.core.Repository
import com.prime.player.core.playback.PlaybackService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject


private const val TAG = "HomeViewModel"

@HiltViewModel
class ConsoleViewModel @Inject constructor(
    private val context: Application,
    private val repository: Repository,
) : ViewModel() {

    /**
     * This channel must be set from composable.
     */
    var messenger: SnackDataChannel? = null

    private val mPlaybackListener =
        object : PlaybackService.EventListener {
            override fun onPlayingStateChanged(isPlaying: Boolean) {
                (playing as MutableState).value = isPlaying
                (playlistName as MutableState).value = service?.title ?: ""
            }

            override fun onTrackChanged(newTrack: Audio, isFavourite: Boolean) {
                (current as MutableState).value = newTrack
                (favourite as MutableState).value = isFavourite
                viewModelScope.launch(Dispatchers.IO) {
                    val of = current.value ?: return@launch
                    val bitmap = getArtwork(of)
                    withContext(Dispatchers.Main) {
                        (artwork as MutableState).value = bitmap
                    }
                }
            }

            override fun onProgress(mills: Long) {
                (progress as MutableState).value = mills.toFloat()
                val value = service ?: return
                (sleepAfter as MutableState).value =
                    if (value.sleepAfter != -1L) value.sleepAfter - System.currentTimeMillis() else null
            }

            override fun onError(what: Int, extra: Int) {
                //TODO: "Handle this"
            }

            override fun setNextTrack(nextTrack: Audio) {
                (next as MutableState).value = nextTrack
            }

            override fun setFavourite(favourite: Boolean) {
                (this@ConsoleViewModel.favourite as MutableState).value = favourite
            }
        }

    private suspend fun getArtwork(of: Audio): Bitmap {
        return context.getAlbumArt(Audiofy.toAlbumArtUri(of.albumId))?.toBitmap()
            ?: Audiofy.DEFAULT_ALBUM_ART
    }

    /**
     * FixMe: Fix Service leak issue.
     */
    @SuppressLint("StaticFieldLeak")
    private var service: PlaybackService? = null
        set(value) {
            field = value
            if (value != null) {
                value.registerListener(mPlaybackListener)
                if (value.isInitialized) {
                    (playing as MutableState).value = value.isPlaying
                    (repeatMode as MutableState).value = value.repeatMode
                    (progress as MutableState).value = value.bookmark.toFloat()
                    (current as MutableState).value = value.currentTrack
                    (favourite as MutableState).value = value.isFavourite
                    (next as MutableState).value = value.nextTrack
                    (shuffle as MutableState).value = value.isShuffleEnabled
                    (playlistName as MutableState).value = value.title
                    (sleepAfter as MutableState).value =
                        if (value.sleepAfter != -1L) value.sleepAfter - System.currentTimeMillis() else null

                    viewModelScope.launch(Dispatchers.IO) {
                        val of = current.value ?: return@launch
                        val bitmap = getArtwork(of)
                        withContext(Dispatchers.Main) {
                            (artwork as MutableState).value = bitmap
                        }
                    }
                }
            }
        }

    val connected: State<Boolean> =
        mutableStateOf(false)

    /**
     * Requires to connect with the [PlaybackService]
     */
    private val connection =
        object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                service = (binder as PlaybackService.PlaybackBinder).service
                (connected as MutableState).value = true
            }

            override fun onServiceDisconnected(p0: ComponentName?) {
                service = null
                // in order to hide the player
                (connected as MutableState).value = false
            }
        }

    /**
     * Must be called periodically to check whether service is connected or not.
     */
    fun connect() {
        // start service if it is disconnected.
        if (service == null) {
            try {
                val serviceIntent = Intent(context, PlaybackService::class.java)
                //start normal service
                context.startService(serviceIntent)
                context.bindService(serviceIntent, connection, BIND_ADJUST_WITH_ACTIVITY)
            } catch (e: IllegalStateException) {
                Log.i(TAG, "connect: ${e.message}")
                Toast.makeText(
                    context,
                    "Error occurred in starting playback service.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * **Playing**
     *
     * is currently [Media Player] in playing mode
     */
    val playing: State<Boolean> =
        mutableStateOf(false)

    fun togglePlay() {
        viewModelScope.launch {
            // reset sleep after if playing and paused.
            service?.togglePlay()
            delay(200)
            //hide sleep timer
            if (!playing.value)
                (sleepAfter as MutableState).value = null
        }
    }

    fun skipToNext() {
        service?.playNextTrack(false)
    }

    fun skipToPrev() {
        service?.back(false)
    }

    /**
     * @see PlayerState.repeatMode
     */
    val repeatMode: State<Int> =
        mutableStateOf(PlaybackService.REPEAT_MODE_NONE)

    fun cycleRepeatMode() {
        viewModelScope.launch {
            service?.cycleRepeatMode()
            val newMode = service?.repeatMode ?: PlaybackService.REPEAT_MODE_NONE
            (repeatMode as MutableState).value = newMode

            val message = Snack(
                message = when (newMode) {
                    PlaybackService.REPEAT_MODE_NONE -> "Repeat mode none."
                    PlaybackService.REPEAT_MODE_ALL -> "Repeat mode all."
                    else -> "Repeat mode one."
                }
            )

            messenger?.send(
                message
            )
        }
    }

    /**
     * *Track Progress*
     *
     *  Represents the progress of the current track
     */
    val progress: State<Float> =
        mutableStateOf(0f)

    fun seekTo(duration: Float) {
        (progress as MutableState).value = duration
        service?.seekTo(duration.toInt())
    }

    /**
     *  **Sleep Timer**
     *
     *  Represents when playback will automatically stop.
     *  Default value -1 means unset.
     */
    val sleepAfter: State<Long?> =
        mutableStateOf(null)

    fun setSleepAfter(minutes: Int) {
        viewModelScope.launch {
            val message = Snack(
                message = when (minutes) {
                    in 1..180 -> {
                        val mills = TimeUnit.MINUTES.toMillis(minutes.toLong())
                        service?.setSleepTimer(mills)
                        "Audiofy will sleep after ${Util.formatAsDuration(mills)}."
                    }
                    -1 -> {
                        service?.setSleepTimer(-1L)
                        "Clearing sleep timer."
                    }
                    else -> "Invalid sleep timer value"
                }
            )
            messenger?.send(message)
        }
    }

    /**
     * The Current track playing
     */
    val current: State<Audio?> =
        mutableStateOf<Audio?>(null)

    fun addToPlaylist(playlist: Playlist, value: Audio) {
        viewModelScope.launch {
            repository.addToPlaylist(
                value.id,
                playlist.name
            )
        }
    }

    /**
     * The track after current
     */
    val next: State<Audio?> =
        mutableStateOf<Audio?>(null)

    /**
     * **Favourite**
     *
     * Is [currTrack] in favourite list
     */
    val favourite: State<Boolean> =
        mutableStateOf(false)

    fun toggleFav() {
        viewModelScope.launch {
            // service?.to
            service?.toggleFav()
            val favourite = service?.isFavourite
            val message = Snack(
                message = when (favourite) {
                    true -> context.getString(R.string.msg_fav_added)
                    else -> context.getString(R.string.msg_fav_removed)
                }
            )
            messenger?.send(message)
        }
    }

    /**
     *  **Shuffle**
     *
     *  Represents weather shuffle is [Boolean] **on** or **off**.
     *
     *  Changes to this will result in re-calculation of queue.
     */
    val shuffle: State<Boolean> =
        mutableStateOf(false)

    fun toggleShuffle() {
        viewModelScope.launch {
            service?.toggleShuffle()
            val newValue = service?.isShuffleEnabled ?: false
            (shuffle as MutableState).value = newValue
            val message = Snack(
                message = if (newValue) "Shuffle enabled." else "Shuffle disabled."
            )
            messenger?.send(message)
        }
    }


    /**
     * **Playlist Name**
     *
     * The name of [Playlist] currently playing
     *
     * *Default Value empty [String]*
     */
    val playlistName: State<String> =
        mutableStateOf("")

    /**
     * The [current] track artwork
     */
    val artwork: State<Bitmap> =
        mutableStateOf(
            Audiofy.DEFAULT_ALBUM_ART
        )

    fun playTrackAt(position: Int) {
        service?.playTrackAt(position)
    }

    /**
     * Returns the current playing queue
     */
    val playingQueue
        get() = service?.playingQueue


    fun addToPlaylist(id: Playlist) {
        val audioId = current.value?.id ?: return
        viewModelScope.launch {
            repository.addToPlaylist(
                audioId,
                id.name
            )
        }
    }

    val playlists = repository.playlists
}
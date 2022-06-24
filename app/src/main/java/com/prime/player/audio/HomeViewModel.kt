package com.prime.player.audio

import android.app.Application
import android.content.ComponentName
import android.content.Context.BIND_ADJUST_WITH_ACTIVITY
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.annotation.FloatRange
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import com.prime.player.App
import com.prime.player.R
import com.prime.player.core.models.Audio
import com.prime.player.core.playback.PlaybackService
import com.prime.player.extended.Messenger
import com.prime.player.extended.send
import com.prime.player.utils.getAlbumArt
import com.prime.player.utils.toDuration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject


private const val TAG = "HomeViewModel"

@HiltViewModel
class HomeViewModel @Inject constructor(private val context: Application) : ViewModel() {
    /**
     * The [Playback Service].
     *
     *  Captures the instance of playback service.
     *
     *  ***null*** value represents the service is not running.
     */
    private var service: PlaybackService? = null

    val connected: State<Boolean> = mutableStateOf(false)

    /**
     * **Playing**
     *
     * is currently [Media Player] in playing mode
     */
    val playing: State<Boolean> = mutableStateOf(false)

    /**
     * **OpenFraction**
     *
     * The progress of the opening and closing of [androidx.compose.material.BottomSheetState].
     *
     *  Represents a value between `0` and `1`
     */
    val expanded = mutableStateOf(false)

    /**
     * @see PlayerState.repeatMode
     */
    val repeatMode: State<Int> = mutableStateOf(PlaybackService.REPEAT_MODE_NONE)

    /**
     * *Track Progress*
     *
     *  Represents the progress of the current track
     */
    val progress: State<Long> = mutableStateOf(0L)

    /**
     *  *Sleep Timer*
     *
     *  Represents when playback will automatically stop.
     *  Default value -1 means unset.
     */
    val sleepAfter: State<Long> = mutableStateOf(-1)

    /**
     * The Current track playing
     */
    val current: State<Audio?> = mutableStateOf<Audio?>(null)

    /**
     * The track after current
     */
    val next: State<Audio?> = mutableStateOf<Audio?>(null)

    /**
     * **Favourite**
     *
     * Is [currTrack] in favourite list
     */
    val favourite: State<Boolean> = mutableStateOf(false)

    /**
     * ***Error Msg***
     *
     *  The Recent Error that occurred
     */
    val error: State<String> = mutableStateOf("")

    /**
     *  **Shuffle**
     *
     *  Represents weather shuffle is [Boolean] **on** or **off**.
     *
     *  Changes to this will result in re-calculation of queue.
     */
    val shuffle: State<Boolean> = mutableStateOf(false)

    /**
     * **Playlist Name**
     *
     * The name of [Playlist] currently playing
     *
     * *Default Value empty [String]*
     */
    val playlistName: State<String> = mutableStateOf("")

    /**
     * The [current] track artwork
     */
    val artwork: State<Bitmap> = mutableStateOf(
        App.DEFUALT_ALBUM_ART
    )

    /**
     * The dominant color as suggested by
     */
    val dominant: State<Color> = mutableStateOf(Color.Unspecified)

    /**
     * The session ID of the current playing audio file
     */
    val audioSessionID: State<Int> = mutableStateOf(-1)

    fun cycleRepeatMode(messenger: Messenger) {
        viewModelScope.launch {
            service?.cycleRepeatMode()
            val newMode = service?.repeatMode ?: PlaybackService.REPEAT_MODE_NONE
            (repeatMode as MutableState).value = newMode
            messenger.send(
                message = when (newMode) {
                    PlaybackService.REPEAT_MODE_NONE -> "Repeat mode none."
                    PlaybackService.REPEAT_MODE_ALL -> "Repeat mode all."
                    else -> "Repeat mode one."
                }
            )
        }
    }

    fun skipToNext() {
        service?.playNextTrack(false)
    }

    fun skipToPrev() {
        service?.back(false)
    }

    fun togglePlay() {
        viewModelScope.launch {
            // reset sleep after if playing and paused.
            service?.togglePlay()
            delay(200)
            //hide sleep timer
            if (!playing.value)
                (sleepAfter as MutableState).value = -1L
        }
    }

    fun toggleFav(messenger: Messenger) {
        viewModelScope.launch {
            // service?.to
            service?.toggleFav()
            val favourite = service?.isFavourite
            messenger.send(
                message = when (favourite) {
                    true -> context.getString(R.string.msg_fav_added)
                    else -> context.getString(R.string.msg_fav_removed)
                }
            )
        }
    }


    fun toggleShuffle(messenger: Messenger) {
        viewModelScope.launch {
            service?.toggleShuffle()
            val newValue = service?.isShuffleEnabled ?: false
            (shuffle as MutableState).value = newValue
            messenger.send(
                message = if (newValue) "Shuffle enabled." else "Shuffle disabled."
            )
        }
    }

    fun setSleepAfter(minutes: Int, messenger: Messenger) {
        viewModelScope.launch {
            messenger.send(
                when (minutes) {
                    in 1..180 -> {
                        val mills = TimeUnit.MINUTES.toMillis(minutes.toLong())
                        service?.setSleepTimer(mills)
                        "Rhythm will sleep after ${toDuration(context, mills)}."
                    }
                    -1 -> {
                        service?.setSleepTimer(-1L)
                        "Clearing sleep timer."
                    }
                    else -> "Invalid sleep timer value"
                }
            )
        }
    }

    fun seekTo(@FloatRange(from = 0.0, to = 1.0) pct: Float) {
        val total = current.value?.duration?.times(pct) ?: 0
        service?.seekTo(total.toInt())
    }

    fun getPlayingQueue(): List<Audio> {
        return service?.playingQueue ?: emptyList()
    }

    fun playTrackAt(position: Int) {
        service?.playTrackAt(position)
    }

    /**
     * Requires to connect with the [PlaybackService]
     */
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            (connected as MutableState).value = true
            (binder as PlaybackService.PlaybackBinder).service.also { value ->
                // init service variable
                service = value
                //
                value.registerListener(object : PlaybackService.EventListener {
                    override fun onPlayingStateChanged(isPlaying: Boolean) {
                        (playing as MutableState).value = isPlaying
                        (playlistName as MutableState).value = value.title
                    }

                    override fun onTrackChanged(newTrack: Audio, isFavourite: Boolean) {
                        setNewTrack(newTrack, isFavourite)
                    }

                    override fun onProgress(mills: Long) {
                        (progress as MutableState).value = mills
                        (sleepAfter as MutableState).value =
                            if (value.sleepAfter != -1L) value.sleepAfter - System.currentTimeMillis() else -1
                    }

                    override fun onError(what: Int, extra: Int) {
                        (error as MutableState).value = "Handle this"
                    }

                    override fun setNextTrack(nextTrack: Audio) {
                        (next as MutableState).value = nextTrack
                    }

                    override fun setFavourite(favourite: Boolean) {
                        (this@HomeViewModel.favourite as MutableState).value = favourite
                    }
                })

                // refresh variable after initial connection.
                if (value.isInitialized) {
                    // refresh state of view Model
                    (playing as MutableState).value = value.isPlaying
                    (audioSessionID as MutableState).value = value.audioSessionId
                    (repeatMode as MutableState).value = value.repeatMode
                    (progress as MutableState).value = value.bookmark.toLong()
                    (sleepAfter as MutableState).value = value.sleepAfter
                    // calculate new info
                    setNewTrack(value.currentTrack, value.isFavourite)
                    (next as MutableState).value = value.nextTrack
                    (shuffle as MutableState).value = value.isShuffleEnabled
                    (playlistName as MutableState).value = value.title
                }
            }
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            (connected as MutableState).value = false
            service = null
            // in order to hide the player
        }
    }

    private fun setNewTrack(newTrack: Audio, favourite: Boolean) {
        (current as MutableState).value = newTrack
        (audioSessionID as MutableState).value = service?.audioSessionId ?: -1
        (this.favourite as MutableState).value = favourite
        viewModelScope.launch(Dispatchers.IO) {
            val bitmap = context.getAlbumArt(current.value?.album?.id ?: -1)
                ?: App.DEFUALT_ALBUM_ART
            withContext(Dispatchers.Main) {
                (artwork as MutableState).value = bitmap
            }
            val palette =
                Palette.Builder(bitmap) // Disable any bitmap resizing in Palette. We've already loaded an appropriately
                    // sized bitmap through Coil
                    .resizeBitmapArea(0)
                    // Clear any built-in filters. We want the unfiltered dominant color
                    .clearFilters()
                    // We reduce the maximum color count down to 8
                    .maximumColorCount(8)
                    .generate()
            val swatch = palette.vibrantSwatch ?: palette.mutedSwatch
            ?: palette.darkVibrantSwatch

            withContext(Dispatchers.Main) {
                (dominant as MutableState).value =
                    swatch?.let { Color(it.rgb) } ?: Color.Unspecified
            }
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
}
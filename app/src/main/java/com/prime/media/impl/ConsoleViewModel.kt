package com.prime.media.impl

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import coil.imageLoader
import coil.request.ImageRequest
import com.prime.media.R
import com.prime.media.console.Console
import com.prime.media.core.db.Playlist
import com.prime.media.core.playback.Playback
import com.prime.media.core.playback.Playback.Companion.UNINITIALIZED_SLEEP_TIME_MILLIS
import com.prime.media.core.playback.Remote
import com.prime.media.core.playback.artworkUri
import com.prime.media.core.playback.mediaUri
import com.primex.core.OrientRed
import com.primex.core.withSpanStyle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.math.roundToLong

private const val TAG = "ConsoleViewModel"

/**
 * Factory function that creates a `Member` object with the given `MediaItem` object and some additional metadata.
 *
 * @param from The `MediaItem` object to create the `Member` from.
 * @param playlistId The ID of the playlist to which this `Member` belongs.
 * @param order The order of this `Member` within the playlist.
 * @return A new `Member` object with the given parameters.
 */
private fun Member(from: MediaItem, playlistId: Long, order: Int) =
    Playlist.Member(
        playlistId,
        from.mediaId,
        order,
        from.requestMetadata.mediaUri!!.toString(),
        from.mediaMetadata.title.toString(),
        from.mediaMetadata.subtitle.toString(),
        from.mediaMetadata.artworkUri?.toString()
    )

/**
 * @see Console.neighbours
 */
private val Remote.neighbours
    get() = when {
        // If there is no previous track and no next track, return 0, indicating that the item has no neighbors.
        !hasPreviousTrack && next == null -> 0
        // If there is a previous track but no next track, return -1, signifying only a left neighbor is available.
        hasPreviousTrack && next == null -> -1
        // If there is a next track but no previous track, return 1, indicating only a right neighbor is available.
        !hasPreviousTrack && next != null -> 1
        // If both previous and next tracks are available, return 2, showing that both neighbors are present.
        else -> 2
    }

/**
 * Calculates and returns the playback progress as a floating-point value between 0.0 and 1.0.
 *
 * The playback progress is computed by dividing the current position by the total duration
 * of the media. If the media is not seekable, or if the duration or position is unavailable,
 * it returns -1.0 to indicate an unknown progress value.
 *
 * @return A floating-point value representing the playback progress, or -1.0 if the progress
 * cannot be determined.
 */
val Remote.progress
    get() =
        if (!isCurrentMediaItemSeekable || duration == C.TIME_UNSET || position == C.TIME_UNSET) -1f else position / duration.toFloat()

/**
 * A short hand method to execute a suspended call.
 */
context(ViewModel)
private inline fun suspended(
    context: CoroutineContext = EmptyCoroutineContext,
    start: CoroutineStart = CoroutineStart.DEFAULT,
    noinline block: suspend CoroutineScope.() -> Unit
) = viewModelScope.launch(context, start, block)

@HiltViewModel
class ConsoleViewModel @Inject constructor(
    private val remote: Remote,
    private val repository: Repository,
    private val delegate: SystemDelegate
) : ViewModel(), Console, SystemDelegate by delegate {

    private var _progress by mutableFloatStateOf(remote.progress)
    private var _favourite by mutableStateOf(false)
    private var _sleepAfterMills by mutableLongStateOf(UNINITIALIZED_SLEEP_TIME_MILLIS)
    private var _playbackSpeed by mutableFloatStateOf(remote.playbackSpeed)
    private var _isPlaying by mutableStateOf(remote.isPlaying)
    private var _repeatMode by mutableIntStateOf(remote.repeatMode)
    private var _shuffle by mutableStateOf(remote.shuffle)

    // simple properties
    override var artwork: ImageBitmap? by mutableStateOf(null)
    override val audioSessionId get() = remote.audioSessionId
    override var neighbours by mutableIntStateOf(remote.neighbours)
    override val queue: Flow<List<MediaItem>> = remote.queue
    override var current: MediaItem? by mutableStateOf(remote.current)

    /**
     * A coroutine job that periodically updates the state of the properties of this class based on
     * the current playback status.
     * This job runs on a background thread and uses a fixed interval, usually 500ms, to check and
     * update the properties such as [progress], [sleepAfterMills], etc.
     * This job is started when the media player is prepared or resumed, and is cancelled when the
     * media player is stopped or released.
     */
    private var playbackMonitorJob: Job? = null

    // getter setters.
    override var shuffle: Boolean
        get() = _shuffle
        set(value) {
            remote.shuffle = value; _shuffle = remote.shuffle
        }

    override var progress: Float
        get() = _progress
        set(value) {
            // check if is seekable.
            if (_progress !in 0f..1f) return
            _progress = value
            // calculate the mills to seek
            val mills = (remote.duration * value).roundToLong()
            // seek the media item.
            suspended { remote.seekTo(mills = mills) }
        }

    override var favourite: Boolean
        get() = _favourite
        set(value) {
            suspended {
                val item = current ?: return@suspended
                // the playlist is created already.
                val uri = item.requestMetadata.mediaUri.toString()
                val playlist =
                    repository.getPlaylist(Playback.PLAYLIST_FAVOURITE) ?: return@suspended
                val res = if (!value)
                    repository.removeFromPlaylist(playlist.id, uri)
                else
                    repository.insert(
                        Member(
                            item,
                            playlist.id,
                            (repository.getLastPlayOrder(playlist.id) ?: 0) + 1
                        )
                    )
                // update the favourite
                _favourite = value && res
                if (!res)
                    showSnackbar(
                        message = R.string.msg_error_fav_playlist_update,
                        icon = R.drawable.ic_heart,
                        accent = Color.OrientRed
                    )
            }
        }

    override var sleepAfterMills: Long
        get() = _sleepAfterMills
        set(value) {
            suspended {
                // if not playing don't change.
                // Maybe show msg.
                if (!isPlaying)
                    return@suspended
                _sleepAfterMills = value
                // calculate mills frm epoc
                val mills =
                    if (value == UNINITIALIZED_SLEEP_TIME_MILLIS) value else System.currentTimeMillis() + value
                remote.setSleepTimeAt(mills)
            }
        }

    override var playbackSpeed: Float
        get() = _playbackSpeed
        set(value) {
            remote.playbackSpeed = value
            _playbackSpeed = remote.playbackSpeed
        }

    override var isPlaying: Boolean
        get() = _isPlaying
        set(value) {
            if (value) remote.play(true)
            else remote.pause()
        }

    override var repeatMode: Int
        get() = _repeatMode
        set(value) {
            TODO("Not yet implemented")
        }

    override fun cycleRepeatMode(): Int = remote.cycleRepeatMode()
    override fun playTrack(uri: Uri) = remote.playTrack(uri)
    override fun playTrackAt(position: Int) = seek(position = position)
    override fun clear(context: Context) {
        suspended {
            Toast.makeText(context, R.string.msg_clearing_playing_queue, Toast.LENGTH_SHORT).show()
            remote.clear()
        }
    }

    override fun remove(context: Context, key: Uri) {
        viewModelScope.launch {
            val removed = remote.remove(key)
            val msg =
                if (removed) R.string.msg_track_will_be_removed else R.string.msg_track_remove_error
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    override fun position(color: Color): AnnotatedString {
        if (_progress == -1f)
            return AnnotatedString("N/A")
        return buildAnnotatedString {
            // make it dependent on position state.
            append(android.text.format.DateUtils.formatElapsedTime((progress * remote.duration).roundToLong() / 1000))
            append(" - ")
            withSpanStyle(color) {
                append(android.text.format.DateUtils.formatElapsedTime(remote.duration / 1000))
            }
        }
    }

    override fun seek(mills: Long, position: Int) {
        if (position == -2)
            remote.skipToPrev()
        else if (position == -3)
            remote.skipToNext()
        if (mills == C.TIME_UNSET)
            return
        val newMills =
            if (remote.position != C.TIME_UNSET && remote.isCurrentMediaItemSeekable) remote.position + mills
            else
                C.TIME_UNSET
        remote.seekTo(mills = newMills)
    }

    init {
        suspended {
            remote.events
                .collect {
                    if (it == null) {
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

    private fun onPlayerEvent(event: Int) {

        // on any call to event; update the common properties here.
        neighbours = remote.neighbours

        // update individual events here.
        when (event) {
            // update the shuffle mode.
            Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED -> _shuffle = remote.shuffle
            // and repeat mode.
            Player.EVENT_REPEAT_MODE_CHANGED -> _repeatMode = remote.repeatMode
            // when state of playing.
            Player.EVENT_PLAY_WHEN_READY_CHANGED -> {
                _isPlaying = remote.playWhenReady
                // disable it
                _sleepAfterMills = UNINITIALIZED_SLEEP_TIME_MILLIS
                playbackMonitorJob?.cancel()
                playbackMonitorJob = null
                if (!remote.playWhenReady)
                    return
                playbackMonitorJob = suspended {
                    while (true) {
                        val scheduled = remote.getSleepTimeAt()
                        _sleepAfterMills =
                            if (scheduled == UNINITIALIZED_SLEEP_TIME_MILLIS) scheduled else scheduled - System.currentTimeMillis()
                        _progress = remote.progress
                        // delay for 1 sec and then reupdate.
                        delay(1000)
                    }
                }
            }

            Player.EVENT_MEDIA_ITEM_TRANSITION -> {
                val mediaItem = remote.current
                val uri = mediaItem?.mediaUri?.toString() ?: ""
                // update the current media id.
                current = mediaItem
                suspended {
                    _favourite = repository.isFavourite(uri)
                    val artworkUri = mediaItem?.artworkUri
                    if (artworkUri == null) {
                        artwork = null
                        return@suspended
                    }
                    // obtain the artwork. ad image bitmap.
                    artwork = context.imageLoader.execute(
                        ImageRequest.Builder(context).data(artworkUri).build()
                    ).drawable?.toBitmap()?.asImageBitmap()
                }
            }
        }
    }
}
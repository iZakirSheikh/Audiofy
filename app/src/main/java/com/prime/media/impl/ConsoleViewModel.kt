@file:SuppressLint("UnsafeOptInUsageError")

package com.prime.media.impl

import android.annotation.SuppressLint
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
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.ui.DefaultTrackNameProvider
import coil.imageLoader
import coil.request.ImageRequest
import com.prime.media.R
import com.prime.media.console.Console
import com.prime.media.console.TrackInfo
import com.prime.media.console.Visibility
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

/**
 * @return: [List] of tracks of [type] that user can select.
 */

private fun Player?.gatherSupportedTrackInfosOfType(
    type: Int,
    provider: DefaultTrackNameProvider
): List<TrackInfo> {
    // Get the current tracks from the player or return an empty list if the player is null
    val tracks = this?.currentTracks ?: return emptyList()
    // Get the track groups from the tracks
    val groups = tracks.groups
    // Create an empty list to store the track infos
    val list = ArrayList<TrackInfo>()
    // Loop through the indices of the track groups
    for (index in groups.indices) {
        // Get the track group at the current index
        val group = groups[index]
        // Skip the group if it is not of the given type
        if (group.type != type)
            continue
        // Loop through the tracks in the group
        for (trackIndex in 0 until group.length) {
            // Skip the track if it is not selected
            if (!group.isTrackSupported(trackIndex))
                continue
            // Get the format of the track
            val format = group.getTrackFormat(trackIndex)
            // Skip the track if it has the forced selection flag
            /*if (format.selectionFlags and C.SELECTION_FLAG_FORCED != 0) {
                continue
            }*/
            // Get the name of the track from the track name provider
            val name = provider.getTrackName(format)
            // Create a track selection override object with the group and the track index
            val params = TrackSelectionOverride(group.mediaTrackGroup, trackIndex)
            // Create a track info object with the name and the params
            list.add(TrackInfo(name, params))
        }
    }
    // Return the list of track infos
    return list
}

/**
 * Selects the track of the given type based on the given value.
 * If the value is null, the track selection is set to auto for audio tracks and off for text tracks.
 * If the value is not null, the track selection is set to the specified track.
 * This function only works if the player has the [Player.COMMAND_SET_TRACK_SELECTION_PARAMETERS] command available.
 * @param value The [TrackInfo] object that contains the track parameters to select, or null to clear the selection.
 * @param type The track type, either [C.TRACK_TYPE_TEXT] or [C.TRACK_TYPE_AUDIO].
 * @throws IllegalArgumentException If the track type is not valid.
 */

private fun Player?.select(value: TrackInfo?, type: Int) {
    val player = this ?: return // return early if player is null
    // check if the player can set track selection parameters
    if (!player.isCommandAvailable(Player.COMMAND_SET_TRACK_SELECTION_PARAMETERS)) return
    // Get the current track selection parameters and build a new one
    player.trackSelectionParameters =
        player.trackSelectionParameters
            .buildUpon()
            .apply {
                when{
                    type == C.TRACK_TYPE_TEXT && value == null -> {
                        // clear text track overrides and ignore forced text tracks
                        clearOverridesOfType(C.TRACK_TYPE_TEXT)
                        setIgnoredTextSelectionFlags(C.SELECTION_FLAG_FORCED.inv())
                    }
                    type == C.TRACK_TYPE_AUDIO && value == null -> {
                        // clear audio track overrides and enable audio track rendering
                        clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                            .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                    }
                    value != null -> {
                        // select the specified audio track and enable audio track rendering
                        setOverrideForType(value.params)
                        setTrackTypeDisabled(value.params.type, false)
                    }
                    else -> error("Track $value & $type cannot be null or invalid")
                }
            }.build()
}


/**
 * Gets the selected track info of the given type for the player, or null if no selection is made. The null in case of audio represents auto selection and null in case of text represents off state.
 * A selection is possible if the player has the [Player.COMMAND_SET_TRACK_SELECTION_PARAMETERS] command available and the current tracks have a group of the given type.
 * A selection is made if the player has a track selection override for the given type and the selected track is not disabled.
 * @param provider The [DefaultTrackNameProvider] object that provides the name of the track based on its format.
 * @param type The track type, either [C.TRACK_TYPE_TEXT] or [C.TRACK_TYPE_AUDIO].
 * @return The [TrackInfo] object that contains the name and the parameters of the selected track, or null if no selection is made or possible.
 * @throws IllegalArgumentException If the track type is not valid or null.
 * @see Player.trackSelectionParameters
 * @sample selectTrack
 */

private fun Player?.getSelectedTrack(
    provider: DefaultTrackNameProvider,
    type: Int
): TrackInfo? {
    val player = this ?: return null // return null if player is null
    // check if the player can set track selection parameters
    if (!player.isCommandAvailable(Player.COMMAND_SET_TRACK_SELECTION_PARAMETERS)) return null
    // get the current tracks and loop through the groups
    val groups = player.currentTracks.groups
    for (group in groups) {
        // skip the group if it is not of the given type
        if (group.type != type) continue
        // loop through the tracks in the group
        for (trackIndex in 0 until group.length) {
            // skip the track if it is not selected
            if (!group.isTrackSelected(trackIndex)) continue
            // get the format and the name of the track
            val format = group.getTrackFormat(trackIndex)
            val name = provider.getTrackName(format)
            // create a track selection override object with the group and the track index
            val params = TrackSelectionOverride(group.mediaTrackGroup, trackIndex)
            // create and return a track info object with the name and the params
            return TrackInfo(name, params)
        }
    }
    return null // no selection is made or possible
}

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
    override var isVideo: Boolean by mutableStateOf(remote.isCurrentMediaItemVideo)
    override var resizeMode: Int by mutableIntStateOf(Console.RESIZE_MORE_FIT)
    private var _visibility: Int by mutableIntStateOf(Console.VISIBILITY_ALWAYS)
    private var _message: CharSequence? by mutableStateOf(null)

    // simple properties
    override var artwork: ImageBitmap? by mutableStateOf(null)
    override val audioSessionId get() = remote.audioSessionId
    override var neighbours by mutableIntStateOf(remote.neighbours)
    override val queue: Flow<List<MediaItem>> = remote.queue
    override var current: MediaItem? by mutableStateOf(remote.current)
    override val player: Player? get() = remote.player

    /**
     * Manages an array of coroutine jobs associated with the ViewModel.
     *
     * The jobs are essential for tracking repetitive tasks and handling various aspects of the media player's behavior.
     *
     * - **Job 0:**  A coroutine job that periodically updates the state of the properties of this class based on
     *               the current playback status.
     *               This job runs on a background thread and uses a fixed interval, usually 500ms, to check and
     *               update the properties such as [progress], [sleepAfterMills], etc.
     *               This job is started when the media player is prepared or resumed, and is cancelled when the
     *               media player is stopped or released.
     *
     * - **Job 1:** The job at index 1 defines tasks related to showing and hiding the controller.
     *              It is active when the [visibility] is [Visibility.Limited] or [Visibility.Locked].
     *
     * - **Job 3:** Controls the message lifetime by resetting it to null if the user sets it to a non-null value.
     * @property jobs An array of coroutine jobs associated with the ViewModel.
     *               Index 0 corresponds to the periodic update job, and index 1 corresponds to the controller visibility job.
     *               Initialized with null values.
     */
    private var jobs: Array<Job?> = Array(3){ null }

    // getter setters.
    override var visibility: Int
        get() = _visibility
        set(value) {
            suspended {
                // Delay setting of value as it is causing a glitch when locking visibility.
                if (value == Console.VISIBILITY_LOCKED)
                    delay(50L)
                _visibility = value
                // Cancel the previous job if any, to avoid conflicting updates
                jobs[1]?.cancel()
                // Return if the current visibility is not 'visible'
                // Only in this case, automation for hiding is needed.
                if (_visibility != Console.VISIBILITY_VISIBLE)
                    return@suspended
                // Launch a new coroutine job to update the visibility after the duration
                jobs[1] = suspended {
                    // Wait for the specified duration
                    delay(Console.DEFAULT_CONTROLLER_VISIBILITY_MILLS)
                    // After the delay, update the visibility to either invisible or locked
                    _visibility = Console.VISIBILITY_HIDDEN
                }
            }
        }

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

    private val trackNameProvider by lazy {
        object : DefaultTrackNameProvider(resources) {
            override fun getTrackName(format: Format): String {
                var trackName = super.getTrackName(format)
                val label = format.label
                if (!label.isNullOrBlank() && !trackName.startsWith(label)) { // HACK
                    trackName += " - $label";
                }
                return trackName
            }
        }
    }

    override val subtiles: List<TrackInfo>
        get() = player.gatherSupportedTrackInfosOfType(C.TRACK_TYPE_TEXT, trackNameProvider)
    override val audios: List<TrackInfo>
        get() = player.gatherSupportedTrackInfosOfType(C.TRACK_TYPE_AUDIO, trackNameProvider)
    override var currAudioTrack: TrackInfo?
        get() = player?.getSelectedTrack(trackNameProvider, C.TRACK_TYPE_AUDIO)
        set(value) = player.select(value, C.TRACK_TYPE_AUDIO)

    override var currSubtitleTrack: TrackInfo?
        get() = player?.getSelectedTrack(trackNameProvider, C.TRACK_TYPE_TEXT)
        set(value) = player.select(value, C.TRACK_TYPE_TEXT)

    override var message: CharSequence?
        get() = _message
        set(value) {
            // Set the value to the message property
            _message = value

            // Return if the user has explicitly set the message to null
            // Cancel the previous job if any, to avoid conflicting updates
            jobs[2]?.cancel()

            // If the new message is null, no further action is needed
            if (value == null) return

            // Launch a new coroutine job to reset the message to null after a specified timeout
            jobs[2] = suspended {
                // Delay execution for the duration specified by DEFAULT_MESSAGE_TIME_OUT
                delay(Console.DEFAULT_MESSAGE_TIME_OUT)

                // After the delay, reset the message to null
                _message = null
            }
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
            append(" (${getText(R.string.playback_speed_dialog_x_f, playbackSpeed)})")
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
                        onPlayerEvent(Player.EVENT_PLAY_WHEN_READY_CHANGED)
                        onPlayerEvent(Player.EVENT_IS_PLAYING_CHANGED)
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
        // On any call to event; update the common properties here.
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
                jobs[0]?.cancel()
                if (!remote.playWhenReady)
                    return
                jobs[0] = suspended {
                    while (true) {
                        val scheduled = remote.getSleepTimeAt()
                        _sleepAfterMills =
                            if (scheduled == UNINITIALIZED_SLEEP_TIME_MILLIS) scheduled else scheduled - System.currentTimeMillis()
                        _progress = remote.progress
                        // delay for 1 sec and then re-update.
                        delay(1000)
                    }
                }
            }
            // Called when item is transitioned from current to another.
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
            // Called whenever some state change takes place
            Player.EVENT_IS_PLAYING_CHANGED -> {
                isVideo = remote.isCurrentMediaItemVideo
                // FixMe: Here I need to think clearly.
                //  It seems that I don't understand this part perfectly. Here are some questions:
                //  1. What happens if the visibility is set to 'locked' or 'always' and this event occurs?

                // For video scenarios, set the visibility to 'visible'; otherwise, set it to 'always'
                visibility = if (isVideo) Console.VISIBILITY_VISIBLE else Console.VISIBILITY_ALWAYS
            }
        }
    }
}
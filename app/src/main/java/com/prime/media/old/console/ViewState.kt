package com.prime.media.old.console

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import androidx.annotation.IntDef
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.AnnotatedString
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Player.RepeatMode
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.ui.AspectRatioFrameLayout
import kotlinx.coroutines.flow.Flow

/**
 * Represents a track, such as subtitle or audio, available in a [MediaItem].
 * Used to construct a [TrackSelectionOverride] for overriding the default track in [MediaItem].
 *
 * @property name The name of the track.
 * @property params The [TrackSelectionOverride] containing parameters for track selection.
 *
 * @see MediaItem
 * @see TrackSelectionOverride
 */
data class TrackInfo(val name: String, val params: TrackSelectionOverride)

@Stable
interface PlayingQueue {
    /**
     * The playing queue.
     */
    val queue: Flow<List<MediaItem>>
    var shuffle: Boolean
    val current: MediaItem?
    val playing: Boolean

    /**
     * Returns if current is last.
     */
    val isLast: Boolean

    /**
     * Play the track of the queue at [position]
     */
    fun playTrackAt(position: Int)

    /**
     * Play the track of the queue identified by the [uri]
     */
    fun playTrack(uri: Uri)

    /**
     * Remove the track from the queue identified by [key].
     */
    fun remove(context: Context, key: Uri)

    /**
     * Toggles the shuffle
     */
    fun toggleShuffle()

    fun clear(context: Context)
}

@Stable
interface Console : PlayingQueue {
    /**
     * Gets/Sets the progress of the playback, expressed as a fraction of the total duration.
     *
     * The [progress] property represents the advancement of playback, where a value from 0 to 1
     * indicates the percentage of the playback completed, with 0 at the beginning and 1 at the end.
     *
     * Note: The [MediaItem] is considered seekable if [progress] is between 0 and 1. A value of -1
     * indicates a live stream with undefined progress.
     */
    var progress: Float

    /**
     * Gets or sets whether the current [MediaItem] is marked as a favorite or not.
     */
    var favourite: Boolean

    /**
     * Gets or sets the sleep timer duration, in milliseconds, for the current session.
     *
     * If set to [com.prime.media.core.playback.Playback.UNINITIALIZED_SLEEP_TIME_MILLIS], the sleep timer is disabled.
     * Otherwise, it specifies the duration in milliseconds after which the session will automatically sleep.
     *
     * ***Note that the property represents a simple duration in milliseconds, not a time from the epoch.***
     */
    var sleepAfterMills: Long

    /**
     * Gets or sets the playback speed.
     *
     * A value of 1.0 denotes normal speed, while values greater than 1.0 accelerate playback, and
     * values less than 1.0 decelerate it.
     */
    var playbackSpeed: Float

    /**
     * Gets or sets whether the player is currently in a playing state.
     */
    var isPlaying: Boolean

    /**
     * Gets or sets the current [RepeatMode] set for the player.
     */
    @get:RepeatMode
    @setparam:RepeatMode
    var repeatMode: Int

    /**
     * Gets the current associated artwork, represented as an [ImageBitmap].
     */
    val artwork: ImageBitmap?

    /**
     * Gets the state of the neighbors (adjacent items) of the currently playing item.
     *
     * The value of this property indicates the availability of neighbors:
     * * `0` indicates that the [current] item has no neighboring items.
     * * `-1` indicates that the [current] item has a left neighbor only, enabling a "skip to previous" action.
     * * `1` indicates that the [current] item has a right neighbor only, enabling a "skip to next" action.
     * * `2` indicates that both left and right neighbors are available, allowing both "skip to previous" and "skip to next" actions.
     */
    val neighbours: Int

    /**
     * Gets the audio session ID associated with the audio playback.
     *
     * The [audioSessionId] property represents the unique identifier of the audio session used
     * for audio playback. It is an integer value that allows external audio processing or
     * visualization components to interact with the audio being played.
     */
    val audioSessionId: Int

    /**
     * Gets weather [current] is Video or just a audio.
     */
    val isVideo: Boolean

    /**
     * Gets/Sets The resizeMode for current Player.
     */
    var resizeMode: Int

    /**
     * Gets the underlying player instance.
     */
    val player: Player?

    /**
     * Seeks the track to a new position, either by adding or subtracting [mills] from the current
     * position, or by setting it to a specific [position].
     *
     * **Note: If [MediaItem] is not seekable, this method does nothing.**
     * @param mills: the amount of milliseconds to seek forward (positive value) or backward
     * (negative value) from the current position. If this parameter is [C.TIME_UNSET], it is
     * ignored. The default value is [C.TIME_UNSET].
     *
     * @param position: the new position to seek to. If this parameter is [C.INDEX_UNSET], it is ignored.
     * The default value is [C.INDEX_UNSET]. If this parameter is -2, it seeks to the previous track.
     * If this parameter is -3, it seeks to the next track.
     */
    fun seek(mills: Long = C.TIME_UNSET, position: Int = C.INDEX_UNSET)

    /**
     * Cycles through available repeat modes, changing the player's repeat behavior.
     *
     * This function allows you to cycle through the available repeat modes, altering the player's
     * repeat behavior. It ensures that the player switches to the next available repeat mode in the sequence.
     */
    @RepeatMode
    fun cycleRepeatMode(): Int

    /**
     * Gets the position - duration formatted as 01:22 - 05:01
     * @param color The color of the duration in result string; the color of position is determined by the
     * position of the [Text] composable.
     * @return The resultant formatted string.
     */
    fun position(color: Color): AnnotatedString

    // Extensions | Helpers
    /**
     * Checks weather [current] is first item of the list.
     */
    val isFirst get() = neighbours != -1 && neighbours != 2
    fun skipToPrev() = seek(position = -2)
    fun skipToNext() = seek(position = -3)
    fun toggleFav() {
        favourite = !favourite
    }

    val isSeekable get() = progress in 0f..1f
    fun togglePlay() {
        isPlaying = !isPlaying
    }

    override fun toggleShuffle() {
        shuffle = !shuffle
    }

    override val isLast: Boolean get() = neighbours <= 0
    override val playing: Boolean get() = isPlaying

    /**
     * Represents the list of all available audio tracks for the [current] [MediaItem].
     *
     * @property audios A list containing all available audio tracks.
     *
     * @see MediaItem
     * @see Tracks
     */
    val audios: List<TrackInfo>

    /**
     * Gets or sets the currently selected audio track for the player.
     *
     * This property returns a [TrackInfo] object that contains the name and the params of the
     * selected audio track, or null if no audio track is selected or the player is not ready.
     *
     * This property can be used to change the audio track selection parameters of the player by
     * setting it to a [TrackInfo] object that represents the desired audio track, or null to reset
     * the parameters to the default ones. The player will select the track that matches the
     * override specified by the [TrackInfo] object, or the best track based on the available tracks,
     * the track selection parameters, and the adaptive policy.
     *
     * Note that this property only works if the player supports changing the track selection
     * parameters, which can be checked by calling
     * [player.isCommandAvailable(Player.COMMAND_SET_TRACK_SELECTION_PARAMETERS)].
     *
     * For example, to get the name of the current audio track, you can write:
     * ```
     * val currAudioTrackName = currAudioTrack?.name
     * ```
     *
     * To reset the audio track selection parameters to the default ones, you can write:
     * ```
     * currAudioTrack = null
     * ```
     *
     * @see Tracks
     * @see TrackInfo
     * @see TrackSelectionParameters
     * @see TrackSelectionOverride
     */
    var currAudioTrack: TrackInfo?

    /**
     * @see audios
     */
    val subtiles: List<TrackInfo>

    /**
     * Note: passing or receiving null means disabled state.
     * @see currAudioTrack
     */
    var currSubtitleTrack: TrackInfo?

    /**
     * Gets/Sets the visibility of the Media Controller.
     */
    @get:Visibility
    @set:Visibility
    var visibility: Int

    /**
     * A message to be shown to the user for a limited time.
     * A null value means no message is set.
     * A non-null value is reset to null after [DEFAULT_MESSAGE_TIME_OUT] ms.
     *
     * @property message The message to display or null to hide it.
     * @see DEFAULT_MESSAGE_TIME_OUT
     */
    var message: CharSequence?


    companion object {
        const val route = "route_console"

        fun direction() = route

        @SuppressLint("UnsafeOptInUsageError")
        const val RESIZE_MORE_FIT = AspectRatioFrameLayout.RESIZE_MODE_FIT

        @SuppressLint("UnsafeOptInUsageError")
        const val RESIZE_MODE_FILL = AspectRatioFrameLayout.RESIZE_MODE_ZOOM

        /**
         * The duration (in ms) for showing a message to the user.
         * After this time, the [message] property becomes null.
         *
         * @see message
         */
        const val DEFAULT_MESSAGE_TIME_OUT = 1_000L

        /**
         * The default amount of time, in milliseconds, after which the value of [visibility]
         * changes from [VISIBILITY_VISIBLE] to [VISIBILITY_HIDDEN].
         *
         * @see visibility
         */
        const val DEFAULT_CONTROLLER_VISIBILITY_MILLS = 5_000L

        /**
         * The controller is visible and will hide after [DEFAULT_CONTROLLER_VISIBILITY_MILLS].
         */
        const val VISIBILITY_VISIBLE = 0

        /**
         * The controller is always visible and does not respond to any events or conditions.
         */
        const val VISIBILITY_ALWAYS = 2

        /**
         * The controller is hidden and can toggle to [VISIBILITY_VISIBLE] and back.
         */
        const val VISIBILITY_HIDDEN = 3

        /**
         * The controller is hidden and cannot be made visible.
         */
        const val VISIBILITY_LOCKED = 4
    }
}

/**
 * An annotation for the visibility of the media controller.
 * It defines the possible values for the [visibility] property.
 *
 * @see visibility
 */
@IntDef(
    Console.VISIBILITY_VISIBLE,
    Console.VISIBILITY_HIDDEN,
    Console.VISIBILITY_ALWAYS,
    Console.VISIBILITY_LOCKED
)
@Target(AnnotationTarget.PROPERTY_SETTER, AnnotationTarget.PROPERTY_GETTER)
@Retention(AnnotationRetention.SOURCE)
@MustBeDocumented
annotation class Visibility
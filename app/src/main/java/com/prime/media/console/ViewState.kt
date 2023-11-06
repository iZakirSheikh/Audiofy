package com.prime.media.console

import android.annotation.SuppressLint
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.AnnotatedString
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Player.RepeatMode
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.AspectRatioFrameLayout.ResizeMode
import com.prime.media.dialog.PlayingQueue

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
    fun toggleFav() { favourite = !favourite }
    val isSeekable get() = progress in 0f..1f
    fun togglePlay() { isPlaying = !isPlaying }
    override fun toggleShuffle() { shuffle = !shuffle }
    override val isLast: Boolean get() = neighbours <= 0
    override val playing: Boolean get() = isPlaying


    companion object {
        const val route = "route_console"
        fun direction() = route


        @SuppressLint("UnsafeOptInUsageError")
        const val RESIZE_MORE_FIT = AspectRatioFrameLayout.RESIZE_MODE_FIT
        @SuppressLint("UnsafeOptInUsageError")
        const val RESIZE_MODE_FILL = AspectRatioFrameLayout.RESIZE_MODE_FILL

    }
}
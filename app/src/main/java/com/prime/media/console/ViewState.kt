package com.prime.media.console

import androidx.compose.runtime.Stable
import com.prime.media.dialog.PlayingQueue
import java.util.concurrent.TimeUnit

@Stable
interface Console : PlayingQueue {
    val repeatMode: Int

    /**
     * returns position in mills.
     */
    val position: Long
    val favourite: Boolean

    /**
     * The time in milliseconds when the player will pause.
     *
     * Default value is [com.prime.media.core.playback.Playback.UNINITIALIZED_SLEEP_TIME_MILLIS].
     */
    val sleepAfterMills: Long

    // getters.
    val artwork get() = current?.mediaMetadata?.artworkUri
    val progress: Float get() = (position / duration.toFloat())
    val isFirst: Boolean

    /**
     * returns duration of the track in mills.
     */
    val duration: Long
    val audioSessionId: Int

    /**
     * Getter/Setter for [Player.PlaybackSpeed]
     */
    var playbackSpeed: Float

    fun togglePlay()

    fun skipToNext()

    fun skipToPrev()

    fun cycleRepeatMode()

    fun seekTo(mills: Long)

    /**
     * @see com.prime.media.core.playback.Remote.setSleepTimeAt
     */
    fun setSleepAfter(mills: Long)

    /**
     * Seek pct of [Remote.duration]
     */
    fun seekTo(pct: Float) =
        seekTo((pct * duration).toLong())

    fun toggleFav()

    /**
     * Replays [mills]. Default 10s
     */
    fun replay(mills: Long = TimeUnit.SECONDS.toMillis(10)) =
        seekTo((position - mills).coerceIn(0, duration))

    /**
     *   Forwards [mills]. Default 30s
     */
    fun forward(mills: Long = TimeUnit.SECONDS.toMillis(30)) =
        seekTo((position + mills).coerceIn(0, duration))

    companion object {
        const val route = "route_console"
        fun direction() = route
    }
}
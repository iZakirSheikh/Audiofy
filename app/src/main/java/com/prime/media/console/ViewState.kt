package com.prime.media.console

import androidx.compose.runtime.Stable
import androidx.media3.common.MediaItem
import com.prime.media.dialog.PlayingQueue

@Stable
interface Console : PlayingQueue {
    val playing: Boolean
    val repeatMode: Int
    val progress: Float
    val current: MediaItem?
    val favourite: Boolean

    // getters.
    val artwork get() = current?.mediaMetadata?.artworkUri
    val isLast: Boolean
    val isFirst: Boolean
    val duration: Long
    val audioSessionId: Int

    /**
     * Getter/Setter for [Player.PlaybackSpeed]
     */
    var playbackSpeed: Float

    /**
     * Checks weather this is initialized.
     */
    val isLoaded get() = current != null

    fun togglePlay()

    fun skipToNext()

    fun skipToPrev()

    fun cycleRepeatMode()

    fun seekTo(mills: Long)

    fun setSleepAfter(minutes: Int)

    /**
     * Seek pct of [Remote.duration]
     */
    fun seekTo(pct: Float) =
        seekTo((pct * duration).toLong())

    fun toggleFav()

    fun replay10()

    fun forward30()
}
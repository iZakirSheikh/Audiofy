package com.prime.media.dialog

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.media3.common.MediaItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

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
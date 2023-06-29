package com.prime.media.console

import android.net.Uri
import androidx.compose.runtime.State
import androidx.media3.common.MediaItem
import com.prime.media.core.db.Playlist
import kotlinx.coroutines.flow.Flow

interface Console {
    val playing: State<Boolean>
    val repeatMode: State<Int>
    val progress: State<Float>
    val sleepAfter: State<Long?>
    val current: State<MediaItem?>
    val shuffle: State<Boolean>
    val artwork: State<Uri?>
    val favourite: State<Boolean>
    val playlists: Flow<List<Playlist>>
    val queue: Flow<List<MediaItem>>

    // getters.
    val hasNextTrack: Boolean
    val hasPreviousTrack: Boolean
    val playbackSpeed: Float
    val duration: Long
    val audioSessionId: Int

    // listener to progress changes.
    val onProgressUpdate: () -> Unit
    fun onPlayerEvent(event: Int)
    fun togglePlay()
    fun skipToNext()
    fun skipToPrev()
    fun cycleRepeatMode()
    fun seekTo(mills: Long)

    /**
     * Seek pct of [Remote.duration]
     */
    fun seekTo(pct: Float)
    fun setSleepAfter(minutes: Int)
    fun toggleFav()
    fun toggleShuffle()
    fun playTrackAt(position: Int)

    @Deprecated("write new one independent of id.")
    fun playTrack(id: Long)
    fun playTrack(uri: Uri)

    @Deprecated("find alternate.")
    fun addToPlaylist(id: Playlist)
    fun remove(key: Uri)
    fun replay10()
    fun forward30()
    fun setPlaybackSpeed(value: Float)
}
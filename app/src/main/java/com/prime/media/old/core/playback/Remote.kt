package com.prime.media.old.core.playback

import android.media.audiofx.Equalizer
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*

/**
 * [`Remote`] is a wrapper around [MediaBrowser].
 *
 * The purpose of [Remote] is to streamline the usage of the [MediaBrowser] API, providing a
 * simplified interface for handling media-related operations. Its recommended scope is tied to the
 * Activity lifecycle to ensure proper initialization and cleanup.
 *
 * Note that the indices it accepts correspond to indexes in the playlist API and are not related to
 *      shuffled playlists unless explicitly stated.
 *
 * Usage of the [Remote] API does not require the use of suspend functions unless specific
 * requirements necessitate operations on non-UI threads. However, when such circumstances arise,
 * appropriate suspend functions may be provided to cater to those scenarios.
 *
 * Additionally, [Remote] provides convenient access through flows, offering an elegant solution
 * for asynchronous handling and data retrieval.
 */
interface Remote {
    /**
     * Gets or sets the shuffle mode for media playback.
     *
     * This property represents the shuffle mode for media playback. Setting this property to true
     * enables shuffle mode, while setting it to false disables shuffle mode.
     *
     * @return The current shuffle mode. It returns false if shuffle mode is disabled or not set.
     */
    var shuffle: Boolean

    /**
     * Returns the current playback position in milliseconds.
     *
     * This function provides the current position of the media playback in milliseconds.
     * If the position is unavailable or not set, it may return [C.INDEX_UNSET], indicating that the
     * position is not determined yet or is not applicable for the current playback state.
     *
     * @return The current position in milliseconds. It may return [C.INDEX_UNSET] if the position
     * is not set or unavailable.
     */
    val position: Long

    /**
     * Gets the duration of the current media being played in milliseconds.
     *
     * This property provides the duration of the media being played in milliseconds. If the duration is not available or not set,
     * it returns [C.TIME_UNSET], indicating that the duration is not determined yet or is not applicable for the current media.
     *
     * @return The duration of the media in milliseconds. It may return [C.TIME_UNSET] if the duration is not set or unavailable.
     */
    val duration: Long

    /**
     * Indicates whether media playback is currently active.
     *
     * This property returns true if media playback is ongoing, and false if playback is paused or
     * no media is being played.
     *
     * @return True if media playback is ongoing, false otherwise.
     */
    val isPlaying: Boolean

    /**
     * @see [Player.getPlayWhenReady]
     */
    val playWhenReady: Boolean

    /**
     * Gets the current repeat mode for media playback.
     *
     * This property provides the repeat mode for media playback. If the repeat mode is not
     * available or not set, it returns [Player.REPEAT_MODE_OFF], indicating that repeat is
     * currently disabled.
     *
     * @return The repeat mode for media playback. It may return [Player.REPEAT_MODE_OFF] if repeat
     *         is disabled or not set.
     */
    val repeatMode: Int

    /**
     * Gets the metadata associated with the currently playing media item.
     *
     * This property provides the metadata of the media currently being played.
     *
     * @return The metadata associated with the currently playing media item, or null if no
     * metadata is available.
     */
    val meta: MediaMetadata?

    /**
     * Gets the currently playing media item.
     *
     * This property provides the media item that is currently being played.
     *
     * @return The currently playing media item, or null if no media item is being played.
     */
    val current: MediaItem?

    /**
     * Gets the next media item in the playback queue.
     *
     * This property provides the media item that will be played next in the playback queue.
     *
     * @return The next media item, or null if there is no next item in the playback queue.
     */
    val next: MediaItem?

    /**
     * Gets or sets the audio session ID used for audio playback.
     *
     * This property represents the audio session ID used by the underlying audio player for audio playback.
     * Setting this property may be useful for some audio processing scenarios.
     */
    val audioSessionId: Int

    /**
     * Indicates whether there is a previous media item in the playback queue.
     *
     * This property returns true if there is a previous media item in the playback queue, which means it is possible
     * to navigate to the previous track during playback.
     *
     * @return True if there is a previous media item in the playback queue, false otherwise.
     */
    val hasPreviousTrack: Boolean

    /**
     * Gets or sets the playback speed for media playback.
     *
     * This property represents the current playback speed for media playback.
     * Setting this property allows adjusting the playback speed to values greater than 0.0.
     * The default value is 1.0, representing normal playback speed.
     */
    var playbackSpeed: Float

    /**
     * A Flow that emits the media queue.
     *
     * This Flow emits the list of media items representing the media queue for playback.
     * It starts by emitting a default root queue and then listens for changes in the queue using a channel.
     * The Flow debounces the changes and fetches the updated queue from the media browser.
     *
     * Note: The queue is updated based on the parent media item; you may modify the logic as needed.
     *
     * @return A Flow of a List of [MediaItem] representing the media queue.
     */
    val queue: Flow<List<MediaItem>>

    /**
     * A Flow that emits player events.
     *
     * This Flow emits events related to the media player, such as playback state changes, seek completion,
     * buffering, and more. It utilizes a callbackFlow to observe player events and emits them as they occur.
     *
     * Note: It's important to opt-in to DelicateCoroutinesApi due to the use of callbackFlow.
     * The Flow is shared among subscribers to avoid multiple observers registering on the same player.
     *
     * @return A Flow of [Player.Events] that emits player events.
     */
    val events: Flow<Player.Events?>

    /**
     * A Flow that emits the loading status of the media player.
     *
     * This Flow emits a boolean value indicating whether the media is loaded and ready for playback.
     * It maps the player events to the loading status by checking if the current media is not null.
     *
     * @return A Flow of Boolean values representing the loading status of the media player.
     */
    val loaded: Flow<Boolean>

    /**
     * @see MediaBrowser.getCurrentMediaItemIndex
     */
    val index: Int

    /**
     * @see MediaBrowser.getNextMediaItemIndex
     */
    val nextIndex: Int

    /**
    * Provides access to the [Player] responsible for media playback.
    * @see Player
    */
    val player: Player?

    /**
     * Indicates whether the current media item is seekable or not.
     * A seekable media item allows users to jump to specific positions during playback.
     */
    val isCurrentMediaItemSeekable: Boolean

    /**
     * Indicates whether the current media item is a video.
     * If `true`, the current media item is a video; otherwise, it's not.
     */
    val isCurrentMediaItemVideo: Boolean


    /**
     * Starts playing the underlying service.
     *
     * @param playWhenReady Pass true to start playback immediately, or false to start in a paused state.
     * @see [Player.playWhenReady]
     */
    fun play(playWhenReady: Boolean = true)

    /**
     * Pauses the underlying media service if it's currently playing; otherwise, does nothing.
     */
    fun pause()

    /**
     * Toggles the playback state.
     *
     * This function is used to toggle the playback state, which means it will pause the playback
     * if it's currently playing, and resume playback if it's currently paused. If the playback is
     * stopped or in any other state, calling this function will start playing the media.
     *
     * Note: This function does not handle the case when the media is not available or loaded yet.
     *       Ensure the media is prepared before calling this function to avoid any unexpected behavior.
     */
    fun togglePlay()

    /**
     * Skips to the next track in the playlist.
     *
     * This function allows skipping to the next track in the media playback queue. If there is no next track,
     * this function has no effect.
     *
     */
    fun skipToNext()

    /**
     * Skips to the previous track in the playlist.
     *
     * This function allows skipping to the previous track in the media playback queue. If there is no previous track,
     * this function has no effect.
     */
    fun skipToPrev()

    /**
     * Seeks to the specified position in the media playback.
     *
     * This function allows seeking to a specific position in the media playback, measured in milliseconds.
     *
     * @see Player.seekTo
     * @param mills The position in milliseconds to seek to in the media playback.
     */
    @Deprecated("use the seekTo with suspend")
    fun seekTo(mills: Long)

    /**
     * @see Player.seekTo
     */
    suspend fun seekTo(position: Int, mills: Long)

    /**
     * Cycles through different repeat modes for media playback.
     *
     * This function changes the repeat mode for media playback. Each time this function is called,
     * the media player will switch to the next available repeat mode in a circular manner.
     *
     * @return The new repeat mode after cycling.
     */
    fun cycleRepeatMode(): Int

    /**
     * Starts playing the track at the specified [position] in queue.
     *
     * This function initiates playback of the track located at the given [position] in the playlist or queue.
     * The position should be within a valid range of the playlist or queue.
     * @param position The index of the track to be played in the playlist or queue.
     */
    @Deprecated("use seekTo")
    fun playTrackAt(position: Int)

    /**
     * The [id] is not the valid way to move a track.
     */
    @Deprecated("use alternative by uri.")
    fun playTrack(id: Long)

    /**
     * Seeks to the specified position in the track associated with the given [uri].
     *
     * This function allows seeking to a specific position in the track associated with the provided [uri].
     * If the [uri] is not found or unavailable, this function will have no effect.
     *
     * @param uri The URI of the track for which the seek operation should be performed.
     */
    suspend fun seekTo(uri: Uri, mills: Long = C.TIME_UNSET): Boolean

    /**
     * @see seekTo
     */
    @Deprecated("use seek to instead.")
    fun playTrack(uri: Uri)

    /**
     * Removes the [MediaItem] identified by [key] from the [Playback] [queue].
     *
     * @param key The unique identifier (URI) of the [MediaItem] to be removed.
     * @return `true` if the [MediaItem] was successfully removed, `false` if the [MediaItem] was
     *         not found in the [queue].
     */
    suspend fun remove(key: Uri): Boolean

    @Deprecated("use the individual ones.")
    fun onRequestPlay(shuffle: Boolean, index: Int = C.INDEX_UNSET, values: List<MediaItem>)

    /**
     * Clears the queue if loaded otherwise does nothing.
     */
    suspend fun clear()

    /**
     * Clears the existing [queue] and replaces it with a new queue containing the specified [values].
     * Note: The queue must only contain unique [MediaItem.mediaUri] values to ensure uniqueness.
     * so, duplicate items will be dealt with automatically.
     *
     * @param values The list of [MediaItem]s to be set in the queue.
     * @return The number of items successfully added to the queue.
     */
    suspend fun set(vararg values: MediaItem): Int

    /**
     * @see set
     */
    @Deprecated("use set with vararg.")
    suspend fun set(values: List<MediaItem>): Int = set(*values.toTypedArray())

    /**
     * Adds the specified [values] to the queue. If [index] is -1, the items will be added to the
     * end of the queue; otherwise, they will be inserted at the provided index.
     * Note: The queue must only contain unique [MediaItem.mediaUri] values. If an item already
     *       exists in the queue, it will discarded.
     *
     * @param values The list of [MediaItem]s to be added to the queue.
     * @param index The optional index where the items should be inserted. If -1 (default), the
     *              items will be added to the end of the queue. Note: takes any index value
     * 				and maps it to `playlistIndex` if `shuffleModeEnabled` otherwise uses the same index.
     * @return The number of items successfully added to the queue.
     */
    suspend fun add(vararg values: MediaItem, index: Int = -1): Int

    /**
     * Toggles the shuffle mode for media playback and returns the new state of the shuffle mode.
     *
     * This function allows toggling the shuffle mode for media playback. If shuffle mode is currently enabled, calling
     * this function will disable it, and if it's currently disabled, calling this function will enable it.
     *
     * @return The new state of the shuffle mode after toggling. `true` if shuffle mode is enabled, `false` if it's disabled.
     */
    suspend fun toggleShuffle(): Boolean

    /**
     * Moves a media item from the source position [from] to the destination position [to] in the
     * playback queue.
     *
     * This function invokes the underlying media player's [Player.moveMediaItems] method to move a
     * media item from
     * the source position [from] to the destination position [to] in the playback queue. It returns
     * `true` if the move operation is successful, and `false` otherwise.
     *
     * @param from The source position of the media item to be moved.
     * @param to The destination position where the media item will be moved.
     * @return `true` if the move operation is successful, `false` otherwise.
     * @see Player.moveMediaItems
     */
    suspend fun move(from: Int, to: Int): Boolean

    /**
     * Gets the corresponding index of the specified [uri] from the playing queue.
     *
     * @param uri The URI for which the index is to be retrieved from the playing queue.
     * @return The index of the [uri] in the playing queue, or [C.INDEX_UNSET] if the [uri] is not
     * found in the queue.
     */
    suspend fun indexOf(uri: Uri): Int

    /**
     *  A function that sets the sleep timer for the media player
     *
     *  The sleep timer is a feature that allows the user to set a time in the future when the
     *  playback should be paused or slept.
     *  @param mills The time in milliseconds when playback is scheduled to be paused. If the value is
     *  [Playback.UNINITIALIZED_SLEEP_TIME_MILLIS], the sleep timer is disabled or removed.
     */
    suspend fun setSleepTimeAt(mills: Long)

    /**
     *   A function that returns the sleep timer value of the media player.
     *
     *   The sleep timer is a feature that allows the user to set a time in the future when the
     *   playback should be paused or slept.
     *   @return The time in milliseconds when playback is scheduled to be paused or
     *   [Playback.UNINITIALIZED_SLEEP_TIME_MILLIS] to cancel the already one.
     * */
    suspend fun getSleepTimeAt(): Long

    /**
     * Sets a new equalizer in the [Playback]. If the [eq] parameter is null, the equalizer settings
     * will be overridden and turned off. Note that after calling this function, the previous equalizer
     * will be automatically released.
     *
     * @param eq The new Equalizer instance to be set, or null to turn off the equalizer.
     */
    suspend fun setEqualizer(eq: Equalizer?)

    /**
     * Constructs a new equalizer based on the settings received through the playback and returns the
     * new Equalizer instance.
     *
     * The equalizer in the playback has its property set to 0. To activate the equalizer, use a value
     * higher than 0 for the [priority] parameter. If the equalizer in playback is not set up, an
     * Equalizer with default configuration will be returned.
     *
     * @param priority The priority level for the new equalizer. Use a value higher than 0 to activate it.
     * @return The newly created Equalizer instance.
     */
    suspend fun getEqualizer(priority: Int): Equalizer
}
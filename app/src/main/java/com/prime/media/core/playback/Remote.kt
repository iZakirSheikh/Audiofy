package com.prime.media.core.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.*
import androidx.media3.session.MediaBrowser.Listener
import androidx.media3.session.MediaLibraryService.LibraryParams
import com.prime.media.core.util.await
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*

private val MediaBrowser.nextMediaItem
    get() = if (hasNextMediaItem()) getMediaItemAt(nextMediaItemIndex) else null

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
}

/**
 * Creates and returns a new instance of the Remote interface using the provided [context].
 *
 * This factory function instantiates a RemoteImpl object, which implements the Remote interface,
 * and returns it. The [context] parameter is used to initialize the RemoteImpl instance with the
 * required data.
 *
 * @param context The Context used to initialize the RemoteImpl instance.
 * @return An instance of the Remote interface.
 */
fun Remote(context: Context): Remote = RemoteImpl(context)

private class RemoteImpl(context: Context) : Remote, Listener {
    /**
     * A simple channel to broadcast the [MediaBrowser.Listener.onChildrenChanged] parents.
     */
    private val channel = MutableSharedFlow<String>()

    //FixMe: Maybe there is alternate way to publish updates.
    override fun onChildrenChanged(
        browser: MediaBrowser, parentId: String, itemCount: Int, params: LibraryParams?
    ) {
        GlobalScope.launch { channel.emit(parentId) }
    }

    private val fBrowser = MediaBrowser.Builder(
        context, SessionToken(context, ComponentName(context, Playback::class.java))
    ).setListener(this).buildAsync()
    val browser get() = if (fBrowser.isDone) fBrowser.get() else null

    override val position
        get() = browser?.currentPosition ?: C.TIME_UNSET
    override val duration
        get() = browser?.duration ?: C.TIME_UNSET
    override val repeatMode: Int
        get() = browser?.repeatMode ?: Player.REPEAT_MODE_OFF
    override val meta: MediaMetadata?
        get() = browser?.mediaMetadata
    override val current: MediaItem?
        get() = browser?.currentMediaItem
    override val next: MediaItem?
        get() = browser?.nextMediaItem
    override val index: Int
        get() = browser?.currentMediaItemIndex ?: C.INDEX_UNSET
    override val nextIndex: Int
        get() = browser?.nextMediaItemIndex ?: C.INDEX_UNSET
    override var shuffle: Boolean
        get() = browser?.shuffleModeEnabled ?: false
        set(value) {
            browser?.shuffleModeEnabled = value
        }

    override val isPlaying: Boolean get() = browser?.isPlaying ?: false
    override var audioSessionId: Int = 0 // FixMe: return actual session ID.
    override val hasPreviousTrack: Boolean get() = browser?.hasPreviousMediaItem() ?: false

    override var playbackSpeed: Float
        get() = browser?.playbackParameters?.speed ?: 1f
        set(value) {
            browser?.setPlaybackSpeed(value)
        }

    @OptIn(DelicateCoroutinesApi::class)
    override val events: Flow<Player.Events?> =
        callbackFlow {
            val observer = object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    trySend(events)
                }
            }
            // init browser
            val browser = fBrowser.await()
            // init first one.
            trySend(null)
            // register
            browser.addListener(observer)
            // un-register on cancel
            awaitClose {
                browser.removeListener(observer)
            }
        }
            .flowOn(Dispatchers.Main)
            .shareIn(
                // what show I use to replace this.
                GlobalScope,
                // un-register when subscriber count is zero.
                SharingStarted.WhileSubscribed(2000, replayExpirationMillis = 5000),
                //
                1
            )
    override val loaded: Flow<Boolean> = events.map { current != null }

    @OptIn(FlowPreview::class)
    override val queue: Flow<List<MediaItem>> = channel
        // emit queue as first
        .onStart { emit(Playback.ROOT_QUEUE) }
        // filter queue
        .filter { it == Playback.ROOT_QUEUE }
        // debounce change
        .debounce(500)
        // map parent with children.
        .map { parent ->
            browser?.getChildren(parent, 0, Int.MAX_VALUE, null)?.await()?.value ?: emptyList()
        }

    init {
        //TODO: Find Suitable place for this event to occur.
        GlobalScope.launch(Dispatchers.Main) {
            val browser = fBrowser.await()
            browser.subscribe(Playback.ROOT_QUEUE, null)
        }
    }

    override suspend fun remove(key: Uri): Boolean {
        // obtain the corresponding index from the key
        val index = indexOf(key)
        // return false since we don't have the index.
        // this might be because the item is already removed.
        if (index == C.INDEX_UNSET)
            return false
        // remove the item
        val browser = fBrowser.await()
        browser.removeMediaItem(index)
        return true
    }

    override fun skipToNext() {
        val browser = browser ?: return
        browser.seekToNextMediaItem()
    }

    override fun skipToPrev() {
        val browser = browser ?: return
        browser.seekToPreviousMediaItem()
    }

    override fun togglePlay() {
        val browser = browser ?: return
        if (browser.isPlaying) {
            browser.pause()
            return
        }
        browser.prepare()
        browser.playWhenReady = true
    }

    // change the working of it.
    override fun cycleRepeatMode(): Int {
        val browser = browser ?: return Player.REPEAT_MODE_OFF // correct it
        val mode = browser.repeatMode
        browser.repeatMode = when (mode) {
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_OFF
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            else -> error("repeat mode $mode")
        }
        return browser.repeatMode
    }

    override fun seekTo(mills: Long) {
        val browser = browser ?: return
        browser.seekTo(mills)
    }

    @Deprecated("use seekTo")
    override fun playTrackAt(position: Int) {
        val browser = browser ?: return
        browser.seekTo(position, C.TIME_UNSET)
    }

    @Deprecated("use the individual ones.")
    override fun onRequestPlay(shuffle: Boolean, index: Int, values: List<MediaItem>) {
        val browser = browser ?: return
        // convert list to mutable list
        // the list should contain the unique items only.
        val l = ArrayList(values.distinctBy { it.mediaUri })
        // remove index
        val item = l.removeAt(index)
        // re-add index at 0
        l.add(0, item)
        browser.shuffleModeEnabled = shuffle
        browser.setMediaItems(l)
        browser.seekTo(0, C.TIME_UNSET)
        browser.prepare()
        browser.play()
    }

    @Deprecated("use alternative by uri.")
    override fun playTrack(id: Long) {
        val browser = browser ?: return
        repeat(browser.mediaItemCount) { pos ->
            val item = browser.getMediaItemAt(pos)
            if (item.mediaId == "$id") playTrackAt(pos)
        }
    }

    @Deprecated("use seek to instead.")
    override fun playTrack(uri: Uri) {
        // plays track if found otherwise does nothing.
        val browser = browser ?: return
        repeat(browser.mediaItemCount) { pos ->
            val item = browser.getMediaItemAt(pos)
            if (item.requestMetadata.mediaUri == uri) playTrackAt(pos)
        }
    }

    override suspend fun clear() {
        val browser = fBrowser.await()
        browser.clearMediaItems()
    }

    override fun play(playWhenReady: Boolean) {
        val browser = browser ?: return
        browser.playWhenReady = playWhenReady
        browser.play()
    }

    override fun pause() {
        val browser = browser ?: return
        browser.pause()
    }

    override suspend fun set(vararg values: MediaItem): Int {
        val browser = fBrowser.await()
        // make sure the items are distinct.
        val list = values.distinctBy { it.mediaUri }
        // set the media items; this will automatically clear the old ones.
        browser.setMediaItems(list)
        // return how many have been added to the list.
        return list.size
    }

    /**
     * Maps the given [index] to the real index based on the current configuration.
     *
     * If shuffle mode is enabled, this function returns the corresponding real index for the
     * shuffled playlist. If shuffle mode is disabled, it simply returns the same index.
     *
     * @param index The index to be mapped to the real index.
     * @return The real index if shuffle mode is enabled, or the same index if shuffle mode is disabled.
     * FixMe: currently there is no way to map index with real index.
     */
    private suspend fun map(index: Int): Int {
        val browser = fBrowser.await()
        if (!browser.shuffleModeEnabled)
            return index
        // FixMe: Return the shuffled index.
        return index
    }

    override suspend fun add(vararg values: MediaItem, index: Int): Int {
        // if the list is empty return
        if (values.isEmpty())
            return 0
        val browser = fBrowser.await()
        // add directly if mediaitemCount is 0. the uniqueness will be checked by set.
        if (browser.mediaItemCount == 0)
            return set(*values)
        val unique = values.distinctBy { it.mediaUri }.toMutableList()
        // remove any duplicates from the unique that are already in browser
        repeat(browser.mediaItemCount) {
            val item = browser.getMediaItemAt(it)
            unique.removeAll { it.mediaUri == item.mediaUri }
        }
        if (unique.isEmpty())
            return 0
        // map index with corresponding playlist index.
        // FixMe: currently it doesn't work with shuffleModeOn
        val newIndex = if (index == C.INDEX_UNSET) browser.mediaItemCount else map(index)
        // add media items.
        browser.addMediaItems(newIndex.coerceIn(0, browser.mediaItemCount), unique)
        return unique.size
    }

    override suspend fun seekTo(position: Int, mills: Long) {
        val browser = fBrowser.await()
        browser.seekTo(position, mills)
    }

    override suspend fun seekTo(uri: Uri, mills: Long): Boolean {
        // plays track if found otherwise does nothing.
        val index = indexOf(uri)
        if (index == C.INDEX_UNSET)
            return false
        seekTo(index, mills)
        return true
    }

    override suspend fun toggleShuffle(): Boolean {
        val browser = fBrowser.await()
        browser.shuffleModeEnabled = !browser.shuffleModeEnabled
        return browser.shuffleModeEnabled
    }

    override suspend fun move(from: Int, to: Int): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun indexOf(uri: Uri): Int {
        // plays track if found otherwise does nothing.
        val browser = fBrowser.await()
        repeat(browser.mediaItemCount) { pos ->
            val item = browser.getMediaItemAt(pos)
            if (item.requestMetadata.mediaUri == uri) return pos
        }
        return C.INDEX_UNSET
    }
}
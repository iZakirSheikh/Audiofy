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
 * [Remote] is a wrapper around [MediaBrowser]
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
        @Deprecated("Please use the function for better support.")
        set

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
        @Deprecated("Use the corresponding method.")
        set

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
    suspend fun seekTo(uri: Uri)

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
     *       exists in the queue, it will be removed from its old position and reinserted in the new
     *       position.
     *
     * @param values The list of [MediaItem]s to be added to the queue.
     * @param index The optional index where the items should be inserted. If -1 (default), the items will be added to the end of the queue.
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
        callbackFlow<Player.Events?> {
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
        val browser = fBrowser.await()
        repeat(browser.mediaItemCount) { pos ->
            val item = browser.getMediaItemAt(pos)
            if (item.requestMetadata.mediaUri == key) {
                browser.removeMediaItem(pos)
                return true
            }
        }
        return false
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
        val l = ArrayList(values)
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
        val list = values.distinctBy { it.mediaUri }
        browser.setMediaItems(list)
        return list.size
    }

    override suspend fun add(vararg values: MediaItem, index: Int): Int {
        TODO("Not yet implemented")
    }

    override suspend fun seekTo(position: Int, mills: Long) {
        val browser = fBrowser.await()
        browser.seekTo(position, mills)
    }

    override suspend fun seekTo(uri: Uri) {
        // plays track if found otherwise does nothing.
        val browser = fBrowser.await()
        repeat(browser.mediaItemCount) { pos ->
            val item = browser.getMediaItemAt(pos)
            if (item.requestMetadata.mediaUri == uri) playTrackAt(pos)
        }
    }

    override suspend fun toggleShuffle(): Boolean {
        val browser = fBrowser.await()
        browser.shuffleModeEnabled = !browser.shuffleModeEnabled
        return browser.shuffleModeEnabled
    }

    override suspend fun move(from: Int, to: Int): Boolean {
        TODO("Not yet implemented")
    }
}
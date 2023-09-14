package com.prime.media.impl

import android.content.ComponentName
import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaBrowser
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.SessionToken
import com.prime.media.core.playback.Playback
import com.prime.media.core.playback.Remote
import com.prime.media.core.playback.mediaUri
import com.prime.media.core.util.await
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.launch
import androidx.media3.session.SessionCommand as Command

private const val TAG = "Remote"

/**
 * @see Command
 */
private inline fun Command(action: String, args: Bundle.() -> Unit) =
    Command(action, Bundle().apply(args))

private val MediaBrowser.nextMediaItem
    get() = if (hasNextMediaItem()) getMediaItemAt(nextMediaItemIndex) else null

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

private class RemoteImpl(context: Context) : Remote, MediaBrowser.Listener {
    /**
     * A simple channel to broadcast the [MediaBrowser.Listener.onChildrenChanged] parents.
     */
    private val channel = MutableSharedFlow<String>()

    //FixMe: Maybe there is alternate way to publish updates.
    override fun onChildrenChanged(
        browser: MediaBrowser,
        parentId: String,
        itemCount: Int,
        params: MediaLibraryService.LibraryParams?
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
    override var audioSessionId: Int = AudioManager.AUDIO_SESSION_ID_GENERATE
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
            // Delay this for 3 sec maybe this is causing exceptions.
            delay(3_000)
            val browser = fBrowser.await()
            browser.subscribe(Playback.ROOT_QUEUE, null)
            // Init the audioSessionId using the fun
            audioSessionId = getAudioSessionID()
            Log.d(TAG, "Audio Session ID: $audioSessionId")
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

    /**
     * Gets the audio session ID associated with the media playback.
     *
     * This function sends a custom command to the media browser to obtain the current audio session ID.
     * @return The audio session ID as an integer, or [AudioManager.AUDIO_SESSION_ID_GENERATE] if unavailable.
     */
    suspend fun getAudioSessionID(): Int {
        // Get the media browser object from a deferred value
        val browser = fBrowser.await()
        // Send a custom command to the media browser with an empty bundle as arguments
        val result = browser.sendCustomCommand(
            Command(Playback.ACTION_AUDIO_SESSION_ID, Bundle.EMPTY),
            Bundle.EMPTY
        )
        // Get the extras bundle from the result or null if none
        val extras = result.await().extras
        // Get the audio session ID from the extras or C.AUDIO_SESSION_ID_UNSET if none
        return extras.getInt(
            Playback.EXTRA_AUDIO_SESSION_ID,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
    }

    override suspend fun setSleepTimeAt(mills: Long) {
        // Get the media browser object from a deferred value
        val browser = fBrowser.await()
        // Send a custom command to the media browser with an empty bundle as arguments
        browser.sendCustomCommand(
            Command(Playback.ACTION_SCHEDULE_SLEEP_TIME) {
                // Put the calculated future time into the extras bundle
                putLong(Playback.EXTRA_SCHEDULED_TIME_MILLS, mills)
            },
            Bundle.EMPTY
        )
    }

    override suspend fun getSleepTimeAt(): Long {
        // Get the media browser object from a deferred value
        val browser = fBrowser.await()
        // Send a custom command to the media browser with an empty bundle as arguments
        val result = browser.sendCustomCommand(
            // Create a custom command to query the sleep timer
            Command(Playback.ACTION_SCHEDULE_SLEEP_TIME, Bundle.EMPTY),
            Bundle.EMPTY
        )
        // Get the scheduled time from the result or use the uninitialized value
        return result.await().extras.getLong(Playback.EXTRA_SCHEDULED_TIME_MILLS)
    }
}

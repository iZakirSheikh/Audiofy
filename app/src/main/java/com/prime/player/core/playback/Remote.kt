package com.prime.player.core.playback

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.*
import androidx.media3.session.MediaBrowser.Listener
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.work.await
import com.prime.player.Audiofy
import com.prime.player.core.getAlbumArt
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*

private val MediaBrowser.nextMediaItem
    get() = if (hasNextMediaItem()) getMediaItemAt(
        nextMediaItemIndex
    ) else null

/**
 * [Remote] is a wrapper around [MediaBrowser]
 */
interface Remote {

    val position: Long
    val duration: Long
    val isPlaying: Boolean
    val shuffle: Boolean
    val repeatMode: Int
    val meta: MediaMetadata?
    val current: MediaItem?
    val next: MediaItem?
    val audioSessionId: Int
    val hasPreviousTrack: Boolean
    var playbackSpeed: Float


    /**
     * Observe the [List] [MediaItem]s
     */
    val queue: Flow<List<MediaItem>>

    /**
     * observe the [Player.Events]
     */
    val events: Flow<Player.Events?>

    /**
     * observes if the [Playback] is loaded and ready.
     */
    val loaded: Flow<Boolean>

    fun skipToNext()

    fun skipToPrev()

    fun togglePlay()

    fun cycleRepeatMode()

    fun seekTo(mills: Long)

    fun toggleShuffle()

    fun playTrackAt(position: Int)

    @Deprecated("use alternative by uri.")
    fun playTrack(id: Long)

    /**
     * Remove the [MediaItem] from [Playback] [queue] identified by [key].
     */
    suspend fun remove(key: Uri): Boolean

    fun onRequestPlay(shuffle: Boolean, index: Int = C.INDEX_UNSET, values: List<MediaItem>)

    @Deprecated("find better alternative.")
    suspend fun artwork(): Bitmap
}


private class RemoteImpl(private val context: Context) : Remote {

    /**
     * A simple channel to broadcast the [MediaBrowser.Listener.onChildrenChanged] parents.
     */
    private val channel = MutableSharedFlow<String>()

    private val fBrowser = MediaBrowser
        .Builder(context, SessionToken(context, ComponentName(context, Playback::class.java)))
        .setListener(
            object : Listener {
                override fun onChildrenChanged(
                    browser: MediaBrowser, parentId: String, itemCount: Int, params: LibraryParams?
                ) {
                    channel.tryEmit(parentId)
                }
            }
        ).buildAsync()

    val browser get() = if (fBrowser.isDone) fBrowser.get() else null

    override val position get() = browser?.currentPosition ?: C.TIME_UNSET
    override val duration get() = browser?.duration ?: C.TIME_UNSET
    override val shuffle: Boolean
        get() = browser?.shuffleModeEnabled ?: false
    override val repeatMode: Int
        get() = browser?.repeatMode ?: Player.REPEAT_MODE_OFF
    override val meta: MediaMetadata?
        get() = browser?.mediaMetadata
    override val current: MediaItem?
        get() = browser?.currentMediaItem
    override val next: MediaItem?
        get() = browser?.nextMediaItem

    override val isPlaying: Boolean
        get() = browser?.isPlaying ?: false

    override var audioSessionId: Int = 0
    override val hasPreviousTrack: Boolean
        get() = browser?.hasPreviousMediaItem() ?: false

    override var playbackSpeed: Float
        get() = browser?.playbackParameters?.speed ?: 1f
        set(value) {
            browser?.setPlaybackSpeed(value)
        }

    @OptIn(DelicateCoroutinesApi::class)
    override val events: Flow<Player.Events?> = callbackFlow {
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
        // convert it to shared flow on GlobalScope.
        .shareIn(
            // what show I use to replace this.
            GlobalScope,
            // un-register when subscriber count is zero.
            SharingStarted.WhileSubscribed(2000, replayExpirationMillis = 2200),
            //
            1
        )
    override val loaded: Flow<Boolean> = events.onStart { delay(3000) }.map { current != null }

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

    override suspend fun remove(key: Uri): Boolean {
        val browser = browser ?: return false
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

    override fun cycleRepeatMode() {
        val browser = browser ?: return
        val mode = browser.repeatMode
        browser.repeatMode = when (mode) {
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_OFF
            Player.REPEAT_MODE_OFF -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_ALL
            else -> error("repeat mode $mode")
        }
    }

    override fun seekTo(mills: Long) {
        val browser = browser ?: return
        browser.seekTo(mills)
    }

    override fun toggleShuffle() {
        val browser = browser ?: return
        browser.shuffleModeEnabled = !browser.shuffleModeEnabled
    }

    override fun playTrackAt(position: Int) {
        val browser = browser ?: return
        browser.seekTo(position, C.TIME_UNSET)
    }

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


    @Deprecated("find better alternative.")
    override suspend fun artwork(): Bitmap {
        val uri = current?.mediaMetadata?.artworkUri ?: return Audiofy.DEFAULT_ALBUM_ART
        return context.getAlbumArt(uri)?.toBitmap() ?: Audiofy.DEFAULT_ALBUM_ART
    }
}

fun Remote(context: Context): Remote = RemoteImpl(context)
package com.prime.player.core

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import androidx.annotation.IntRange
import androidx.core.graphics.drawable.toBitmap
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.*
import androidx.media3.session.MediaBrowser.Listener
import androidx.media3.session.MediaLibraryService.LibraryParams
import androidx.work.await
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.ListenableFuture
import com.prime.player.Audiofy
import com.prime.player.common.getAlbumArt
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.runBlocking


/**
 * [Remote] is a wrapper around [MediaBrowser]
 */
interface Remote {

    val position: Long
    val duration: Long
    val isPLaying: Boolean
    val shuffle: Boolean
    val repeatMode: Int
    val meta: MediaMetadata?
    val current: MediaItem?
    val nextMediaItem: MediaItem?


    fun add(value: Listener): Boolean

    fun remove(value: Listener): Boolean

    fun add(value: Player.Listener)

    fun remove(value: Player.Listener)

    fun subscribe(parentId: String, params: LibraryParams? = null)

    fun unsubscribe(parentId: String)

    fun observe(parent: String, params: LibraryParams? = null): Flow<List<MediaItem>>

    fun skipToNext()

    fun skipToPrev()

    fun togglePlay()

    fun cycleRepeatMode()

    fun seekTo(mills: Long)

    fun toggleShuffle()

    fun playTrackAt(position: Int)

    fun onRequestPlay(shuffle: Boolean, index: Int = C.INDEX_UNSET, values: List<MediaItem>)

    suspend fun artwork(): Bitmap

    suspend fun await()
}

fun Remote(context: Context): Remote = RemoteImpl(context)


private val MediaBrowser.nextMediaItem
    get() = if (hasNextMediaItem()) getMediaItemAt(
        nextMediaItemIndex
    ) else null

private class RemoteImpl(private val context: Context) : Remote {
    private val listeners =
        mutableSetOf<Listener>()

    private val listener =
        object : Listener {
            override fun onDisconnected(controller: MediaController) {
                listeners.forEach { it.onDisconnected(controller) }
            }

            override fun onSetCustomLayout(
                controller: MediaController,
                layout: MutableList<CommandButton>
            ): ListenableFuture<SessionResult> {
                return super.onSetCustomLayout(controller, layout)
            }

            override fun onExtrasChanged(controller: MediaController, extras: Bundle) {
                listeners.forEach { it.onExtrasChanged(controller, extras) }
            }

            override fun onChildrenChanged(
                browser: MediaBrowser,
                parentId: String,
                itemCount: Int,
                params: LibraryParams?
            ) {
                listeners.forEach { it.onChildrenChanged(browser, parentId, itemCount, params) }
            }

            override fun onSearchResultChanged(
                browser: MediaBrowser,
                query: String,
                itemCount: Int,
                params: LibraryParams?
            ) {
                listeners.forEach { it.onSearchResultChanged(browser, query, itemCount, params) }
            }
        }

    private val fBrowser =
        MediaBrowser.Builder(
            context,
            SessionToken(context, ComponentName(context, Playback::class.java))
        )
            .setListener(listener)
            .buildAsync()

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
    override val nextMediaItem: MediaItem?
        get() = browser?.nextMediaItem

    override val isPLaying: Boolean
        get() = browser?.playWhenReady ?: false

    suspend fun getChildren(
        parentId: String,
        @IntRange(from = 0) page: Int,
        @IntRange(from = 1) pageSize: Int,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val browser = fBrowser.await()
        return browser.getChildren(parentId, page, pageSize, params)
    }

    override fun observe(parent: String, params: LibraryParams?) =
        callbackFlow {
            val listener = object : Listener {
                override fun onChildrenChanged(
                    browser: MediaBrowser,
                    parentId: String,
                    itemCount: Int,
                    params: LibraryParams?
                ) {
                    runBlocking {
                        val children = getChildren(parentId, 0, Int.MAX_VALUE, params).await()
                        trySend(children.value as List<MediaItem>)
                    }
                }
            }

            // block
            await()
            listener.onChildrenChanged(browser!!, parent, Int.MAX_VALUE, params)
            add(listener)
            awaitClose {
                remove(listener)
            }
        }

    override fun add(value: Listener) = listeners.add(value)

    override fun remove(value: Listener) = listeners.remove(value)

    override fun subscribe(parentId: String, params: LibraryParams?) {
        val browser = browser ?: return
        browser.subscribe(parentId, params)
    }

    override fun unsubscribe(parentId: String) {
        val browser = browser ?: return
        browser.unsubscribe(parentId)
    }

    override fun add(value: Player.Listener) {
        val browser = browser ?: return
        browser.addListener(value)
    }

    override fun remove(value: Player.Listener) {
        val browser = browser ?: return
        browser.removeListener(value)
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
        browser.setMediaItems(values)
        browser.seekTo(index, C.TIME_UNSET)
        browser.prepare()
        browser.play()
    }


    override suspend fun artwork(): Bitmap {
        val uri = current?.mediaMetadata?.artworkUri ?: return Audiofy.DEFAULT_ALBUM_ART
        return context.getAlbumArt(uri)?.toBitmap() ?: Audiofy.DEFAULT_ALBUM_ART
    }

    override suspend fun await() {
        val x = fBrowser.await()
        x.subscribe(Playback.ROOT_PLAYLIST, null)
        x.subscribe(Playback.ROOT_RECENT, null)
    }
}


val Audio.toMediaItem
    get() =
        MediaItem.Builder()
            .setMediaId("$id")
            .setRequestMetadata(
                MediaItem.RequestMetadata.Builder()
                    .setMediaUri(Audiofy.toAudioTrackUri(id))
                    .build()
            )
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setArtworkUri(Audiofy.toAlbumArtUri(albumId))
                    .setTitle(title)
                    .setSubtitle(artist)
                    .setFolderType(MediaMetadata.FOLDER_TYPE_NONE)
                    .setIsPlayable(true)
                    // .setExtras(bundleOf(ARTIST_ID to artistId, ALBUM_ID to albumId))
                    .build()
            )
            .build()

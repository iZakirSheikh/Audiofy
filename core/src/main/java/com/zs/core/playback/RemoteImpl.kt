/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 13-05-2025.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zs.core.playback

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionToken
import com.zs.core.common.await
import com.zs.core.common.debounceAfterFirst
import com.zs.core.playback.mediaUri
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

// TODO: currently a quickfix requirement. find better alternative.
private fun Context.browser(listener: MediaBrowser.Listener) =
    MediaBrowser
        .Builder(this, SessionToken(this, ComponentName(this, Playback::class.java)))
        .setListener(listener)
        .buildAsync()

private val MediaBrowser.state: NowPlaying?
    get() {
        val current = currentMediaItem
        if (current == null) return null
        return NowPlaying(
            current.title?.toString(),
            current.subtitle?.toString(),
            current.artworkUri,
            this.playbackParameters.speed,
            this.contentDuration,
            this.contentPosition,
            false,
            this.playWhenReady,
            current.mimeType,
            this.playbackState,
            this.repeatMode,
        )
    }

/**
 * Player events that trigger state change.
 */
private val UPDATE_EVENTS = intArrayOf(
    Player.EVENT_TIMELINE_CHANGED,
    Player.EVENT_PLAYBACK_STATE_CHANGED,
    Player.EVENT_REPEAT_MODE_CHANGED,
    Player.EVENT_IS_PLAYING_CHANGED,
    Player.EVENT_IS_LOADING_CHANGED,
    Player.EVENT_PLAYBACK_PARAMETERS_CHANGED,
    Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
    Player.EVENT_MEDIA_ITEM_TRANSITION,
)


internal class RemoteImpl(private val context: Context) : Remote, MediaBrowser.Listener {
    // TODO: A quickfix, find better alternative of doing this.
    // The fBrowser variable is lazily initialized with context.browser(this).
    // Whenever fBrowser is accessed, the getter checks if the current value is cancelled.
    // If it is cancelled, it re-initializes fBrowser with a new context.browser(this).
    // Otherwise, it retains the current value.
    // The goal is to ensure that fBrowser always holds a valid browser context,
    // reinitializing it if the current one has been cancelled.
    private var fBrowser = context.browser(this)
        get() {
            field = if (field.isCancelled) context.browser(this) else field
            return field
        }


    override val state: Flow<NowPlaying?> = callbackFlow {
        // init browser
        val browser = fBrowser.await()
        val observer = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                if (!events.containsAny(*UPDATE_EVENTS))
                    return
                trySend(browser.state)
            }
        }

        // register
        send(browser.state)
        browser.addListener(observer)
        // un-register on cancel
        awaitClose {
            browser.removeListener(observer)
        }
    }.debounceAfterFirst(100)


    override val queue: Flow<List<MediaFile>> get() = TODO("Not yet implemented")

    override suspend fun setMediaFiles(values: List<MediaFile>): Int {
        val browser = fBrowser.await()
        // make sure the items are distinct.
        val list = values.distinctBy { it.mediaUri }
        // set the media items; this will automatically clear the old ones.
        browser.setMediaItems(list.map(MediaFile::value))
        // return how many have been added to the list.
        return list.size
    }

    override suspend fun shuffle(shuffle: Boolean) {
        val browser = fBrowser.await()
        browser.shuffleModeEnabled = shuffle
    }

    override suspend fun clear() {
        val browser = fBrowser.await()
        browser.clearMediaItems()
    }

    override suspend fun pause() {
        val browser = fBrowser.await()
        browser.pause()
    }

    override suspend fun setRepeatMode(mode: Int) {
        val browser = fBrowser.await()
        browser.repeatMode = mode
    }

    override suspend fun togglePlay() {
        val browser = fBrowser.await()
        if (browser.isPlaying)
            pause()
        else {
            play(true)
        }
    }

    override suspend fun skipToNext() {
        val browser = fBrowser.await()
        browser.seekToNextMediaItem()
    }

    override suspend fun skipToPrevious() {
        val browser = fBrowser.await()
        browser.seekToPreviousMediaItem()
    }

    override suspend fun seekTo(pct: Float) {
        val browser = fBrowser.await()
        val duration = browser.duration
        if (!browser.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) || duration == Remote.TIME_UNSET)
            return
        browser.seekTo((duration * pct).toLong())
    }

    override suspend fun indexOf(uri: Uri): Int {
        val browser = fBrowser.await()
        repeat(browser.mediaItemCount) { pos ->
            val item = browser.getMediaItemAt(pos)
            if (item.requestMetadata.mediaUri == uri) return pos
        }
        return Remote.INDEX_UNSET
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

    override suspend fun seekTo(index: Int, mills: Long): Boolean {
        val browser = fBrowser.await()
        if (index == Remote.INDEX_UNSET && mills == Remote.TIME_UNSET) return false
        browser.seekTo(index, mills)
        return true
    }

    override suspend fun play(playWhenReady: Boolean) {
        val browser = fBrowser.await()
        browser.playWhenReady = playWhenReady
        browser.play()
    }

    override suspend fun add(values: List<MediaFile>, index: Int): Int {
        // if the list is empty return
        if (values.isEmpty())
            return 0
        val browser = fBrowser.await()
        // add directly if mediaitemCount is 0. the uniqueness will be checked by set.
        if (browser.mediaItemCount == 0)
            return setMediaFiles(values)
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
        browser.addMediaItems(
            newIndex.coerceIn(0, browser.mediaItemCount),
            unique.map(MediaFile::value)
        )
        return unique.size
    }

    override suspend fun getNextMediaItemIndex(): Int {
        val browser = fBrowser.await()
        return browser.nextMediaItemIndex

    }

    override suspend fun getCurrentMediaItemIndex(): Int {
        val browser = fBrowser.await()
        return browser.currentMediaItemIndex
    }
}
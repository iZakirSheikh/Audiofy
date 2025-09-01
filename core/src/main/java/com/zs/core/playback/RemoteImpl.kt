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

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.core.os.bundleOf
import androidx.media3.common.C
import androidx.media3.common.Player
import com.zs.core.common.await
import com.zs.core.common.debounceAfterFirst
import com.zs.core.db.playlists.Playlists
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.withContext

internal class RemoteImpl(private val context: Context) : Remote {

    private val TAG = "RemoteImpl"

    // TODO: A quickfix, find better alternative of doing this.
    // The fBrowser variable is lazily initialized with context.browser(this).
    // Whenever fBrowser is accessed, the getter checks if the current value is cancelled.
    // If it is cancelled, it re-initializes fBrowser with a new context.browser(this).
    // Otherwise, it retains the current value.
    // The goal is to ensure that fBrowser always holds a valid browser context,
    // reinitializing it if the current one has been cancelled.
    private var fBrowser = MediaBrowser(context)
        get() {
            field = if (field.isCancelled) MediaBrowser(context) else field
            return field
        }

    override suspend fun getViewProvider(): VideoProvider = VideoProvider(fBrowser.await())

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
        if (browser.isPlaying) pause()
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
        if (!browser.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) || duration == Remote.TIME_UNSET) return
        browser[Remote.SCRUBBING_MODE] = bundleOf(
            Remote.EXTRA_SCRUBBING_MODE_ENABLED to true
        )
        delay(10)
        withContext(Dispatchers.Main) {
            browser.seekTo((duration * pct).toLong())
        }
        browser[Remote.SCRUBBING_MODE] = bundleOf(
            Remote.EXTRA_SCRUBBING_MODE_ENABLED to false
        )
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
        if (!browser.shuffleModeEnabled) return index
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
        if (values.isEmpty()) return 0
        val browser = fBrowser.await()
        // add directly if mediaitemCount is 0. the uniqueness will be checked by set.
        if (browser.mediaItemCount == 0) return setMediaFiles(values)
        val unique = values.distinctBy { it.mediaUri }.toMutableList()
        // remove any duplicates from the unique that are already in browser
        repeat(browser.mediaItemCount) {
            val item = browser.getMediaItemAt(it)
            unique.removeAll { it.mediaUri == item.mediaUri }
        }
        if (unique.isEmpty()) return 0
        // map index with corresponding playlist index.
        // FixMe: currently it doesn't work with shuffleModeOn
        val newIndex = if (index == C.INDEX_UNSET) browser.mediaItemCount else map(index)
        // add media items.
        browser.addMediaItems(
            newIndex.coerceIn(0, browser.mediaItemCount), unique.map(MediaFile::value)
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

    override suspend fun cycleRepeatMode(): Int {
        val browser = fBrowser.await()
        val current = browser.repeatMode
        val new = when (current) {
            Remote.REPEAT_MODE_OFF -> Remote.REPEAT_MODE_ONE
            Remote.REPEAT_MODE_ONE -> Remote.REPEAT_MODE_ALL
            else -> Remote.REPEAT_MODE_OFF
        }
        browser.repeatMode = new
        return new
    }

    override suspend fun toggleLike(index: Int) {
        val browser = fBrowser.await()
        browser[Remote.TOGGLE_LIKE] = bundleOf()
    }

    override suspend fun remove(uri: Uri): Boolean {
        // obtain the corresponding index from the key
        val index = indexOf(uri)
        // return false since we don't have the index.
        // this might be because the item is already removed.
        if (index == C.INDEX_UNSET)
            return false
        // remove the item
        val browser = fBrowser.await()
        browser.removeMediaItem(index)
        return true
    }

    override suspend fun skipTo(uri: Uri): Boolean {
        // obtain the corresponding index from the key
        val index = indexOf(uri)
        // return false since we don't have the index.
        // this might be because the item is already removed.
        if (index == C.INDEX_UNSET)
            return false
        // remove the item
        val browser = fBrowser.await()
        browser.seekTo(index, C.TIME_UNSET)
        return true
    }


    @OptIn(DelicateCoroutinesApi::class)
    private val remoteScope: CoroutineScope = GlobalScope
    private val playlists = Playlists(context)

    private val autostopPolicy =
        SharingStarted.WhileSubscribed(5_000, 5_000)

    // This flow emits Player.Events from the MediaBrowser.
    // It's designed to ensure only one listener is registered with the MediaBrowser,
    // even if multiple clients collect this flow.
    // The `autostopSharing` (5 seconds) helps preserve the listener during brief disconnections/reconnections,
    // preventing unnecessary unregistering and reregistering.
    private val events = callbackFlow {
        // init browser
        val browser = fBrowser.await()
        val observer = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                trySend(events)
            }
        }
        // register
        send(null)
        browser.addListener(observer)
        // un-register on cancel
        awaitClose {
            Log.d(TAG, "state: un-registering")
            browser.removeListener(observer)
        }
    }


    override val state: StateFlow<NowPlaying?> = events
        .filter { it?.containsAny(*Remote.STATE_UPDATE_EVENTS) == true }
        .debounceAfterFirst(200)
        .transform { events ->
            Log.d(TAG, "onEvents: $events")
            // If the events are not null and do not contain any of the relevant state update events,
            // then there's no need to update the NowPlaying state, so we return early.
            // if (events != null && !events.containsAny(*Remote.STATE_UPDATE_EVENTS)) return@transform
            // Await the MediaBrowser instance.
            val provider = fBrowser.await()
            // Get the current media item from the provider.
            val current = provider.currentMediaItem
            // If there's no current media item, emit null (or the previous state will be retained by stateIn)
            // and return early.
            if (current == null) return@transform
            // Construct a new NowPlaying object with the current media item's details.
            val state = NowPlaying(
                title = current.title?.toString(),
                subtitle = current.subtitle?.toString(),
                artwork = current.artworkUri,
                speed = provider.playbackParameters.speed,
                shuffle = provider.shuffleModeEnabled,
                duration = provider.contentDuration,
                position = provider.contentPosition,
                // Check if the current media item is in the "favourites" playlist.
                favourite = playlists.contains(
                    Remote.PLAYLIST_FAVOURITE,
                    current.mediaUri.toString()
                ),
                playWhenReady = provider.playWhenReady,
                mimeType = current.mimeType,
                state = provider.playbackState,
                repeatMode = provider.repeatMode,
                error = null,
                videoSize = VideoSize(provider.videoSize),
                data = current.mediaUri,
                // Determine the presence of next and previous items.
                // 2: both next and previous exist
                // -1: only previous exists
                // 1: only next exists
                // 0: neither next nor previous exist
                neighbours = when {
                    provider.hasNextMediaItem() && provider.hasPreviousMediaItem() -> 2
                    provider.hasPreviousMediaItem() -> -1
                    provider.hasNextMediaItem() -> 1
                    else -> 0
                },
                sleepAt = getSleepTimeAt()
            ) // Emit the newly created NowPlaying state.
            emit(state)
        }
        .flowOn(Dispatchers.Main)
        .stateIn(remoteScope, autostopPolicy, null)

    override val queue: Flow<List<MediaFile>?> = events
        .filter {
            // Check if the received events are relevant for a queue update.
            // If `events` is null (initial emission from callbackFlow) or if it doesn't contain
            // `Player.EVENT_TIMELINE_CHANGED`, it means the queue hasn't changed,
            // so we don't need to re-fetch and emit it.
            it == null ||
                    it.contains(Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED) ||
                    it.contains(Player.EVENT_TIMELINE_CHANGED)
        }
        .debounceAfterFirst(200)
        .transform {
            // Await the MediaBrowser instance to ensure it's connected and ready.
            val provider = fBrowser.await()
            // Retrieve the current queue from the provider and map each item to a MediaFile object.
            val list = provider.getChildren(Remote.ROOT_QUEUE, 0, Int.MAX_VALUE, null).await().value
            // Emit the updated list of MediaFile objects.
            emit(list?.map(::MediaFile))
        }

    override suspend fun setPlaybackSpeed(value: Float): Boolean {
        val browser = fBrowser.await() // Await the MediaBrowser instance.
        browser.playbackParameters = browser.playbackParameters.withSpeed(value) // Set the new playback speed.
        return browser.playbackParameters.speed == value // Verify if the speed was set correctly.
    }

    override suspend fun getPlaybackSpeed(): Float {
        val browser = fBrowser.await()
        return browser.playbackParameters.speed
    }

    override suspend fun setSleepTimeAt(mills: Long) {
        val browser = fBrowser.await()
        browser[Remote.SCHEDULE_SLEEP_TIME] = bundleOf(
            Remote.EXTRA_SCHEDULED_TIME_MILLS to mills
        )
    }

    override suspend fun getSleepTimeAt(): Long {
        val browser = fBrowser.await()
        val result = browser[Remote.SCHEDULE_SLEEP_TIME]
        // Get the scheduled time from the result or use the uninitialized value
        return result.extras.getLong(Remote.EXTRA_SCHEDULED_TIME_MILLS)
    }

    override suspend fun isPlaying(): Boolean {
        val browser = fBrowser.await()
        return browser.playWhenReady
    }
}



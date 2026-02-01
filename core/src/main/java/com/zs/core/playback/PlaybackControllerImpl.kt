/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 17-11-2024.
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

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.session.MediaBrowser
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import androidx.media3.ui.DefaultTrackNameProvider
import com.zs.core.await
import com.zs.core.playback.PlaybackController.Companion.INDEX_UNSET
import com.zs.core.playback.PlaybackController.Companion.TIME_UNSET
import com.zs.core.playback.PlaybackController.TrackInfo
import com.zs.core.util.debounceAfterFirst
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.withContext

private const val TAG = "PlaybackControllerImpl"

// TODO: currently a quickfix requirement. find better alternative.
private fun Context.browser(listener: MediaBrowser.Listener) =
    MediaBrowser
        .Builder(this, SessionToken(this, ComponentName(this, Playback::class.java)))
        .setListener(listener)
        .buildAsync()

internal class PlaybackControllerImpl(
    val context: Context
) : PlaybackController, MediaBrowser.Listener {

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


    override suspend fun setMediaFiles(values: List<MediaFile>, index: Int , position: Long): Int {
        val browser = fBrowser.await()
        // make sure the items are distinct.
        val list = values.distinctBy { it.mediaUri }
        // set the media items; this will automatically clear the old ones.
        browser.setMediaItems(list.map { it.value }, index, position)
        // return how many have been added to the list.
        return list.size
    }

    override suspend fun play(playWhenReady: Boolean) {
        val browser = fBrowser.await()
        browser.playWhenReady = playWhenReady
        browser.play()
    }

    override suspend fun clear() {
        val browser = fBrowser.await()
        browser.clearMediaItems()
    }

    override suspend fun indexOf(uri: Uri): Int {
        val browser = fBrowser.await()
        repeat(browser.mediaItemCount) { pos ->
            val item = browser.getMediaItemAt(pos)
            if (item.requestMetadata.mediaUri == uri) return pos
        }
        return INDEX_UNSET
    }

    override suspend fun seekTo(index: Int, mills: Long): Boolean {
        val browser = fBrowser.await()
        if (index == INDEX_UNSET && mills == TIME_UNSET) return false
        browser.seekTo(index, mills)
        return true
    }

    suspend fun getRemainingSleepTime(): Long {
        // Get the media browser object from a deferred value
        val browser = fBrowser.await()
        // Send a custom command to the media browser with an empty bundle as arguments
        val result = browser.sendCustomCommand(
            // Create a custom command to query the sleep timer
            SessionCommand(Playback.ACTION_SCHEDULE_SLEEP_TIME, Bundle.EMPTY),
            Bundle.EMPTY
        )
        // Get the scheduled time from the result or use the uninitialized value
        return result.await().extras.getLong(Playback.EXTRA_SCHEDULED_TIME_MILLS)
    }

    override suspend fun getNowPlaying(): NowPlaying2? {
        // If the events are not null and do not contain any of the relevant state update events,
        // then there's no need to update the NowPlaying state, so we return early.
        // if (events != null && !events.containsAny(*Remote.STATE_UPDATE_EVENTS)) return@transform
        // Await the MediaBrowser instance.
        val provider = fBrowser.await()
        // Get the current media item from the provider.
        val current = provider.currentMediaItem ?: return null
        // If there's no current media item, emit null (or the previous state will be retained by stateIn)
        // and return early.
        // Emit the newly created NowPlaying state.
        return NowPlaying2(
            title = current.title?.toString(),
            subtitle = current.subtitle?.toString(),
            artwork = current.artworkUri,
            speed = provider.playbackParameters.speed,
            shuffle = provider.shuffleModeEnabled,
            duration = if (provider.playbackState == PlaybackController.PLAYER_STATE_IDLE) PlaybackController.TIME_UNSET else provider.contentDuration,
            position = provider.contentPosition,
            // Check if the current media item is in the "favourites" playlist.
            favourite = false,
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
            sleepAt = getRemainingSleepTime()
        )
    }

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
    @OptIn(DelicateCoroutinesApi::class)
    private val remoteScope: CoroutineScope = GlobalScope
    override val state: StateFlow<NowPlaying2?> = events
        .filter { it == null || it.containsAny(*PlaybackController.STATE_UPDATE_EVENTS) }
        .debounceAfterFirst(200)
        .map { getNowPlaying() }
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
            val list = provider.getChildren(Playback.ROOT_QUEUE, 0, Int.MAX_VALUE, null).await().value
            // Emit the updated list of MediaFile objects.
            emit(list?.map(::MediaFile))
        }

    override suspend fun getPlaybackState(): Int {
        val browser = fBrowser.await()
        return  browser.playbackState
    }

    override suspend fun shuffle(shuffle: Boolean) {
        val browser = fBrowser.await()
        browser.shuffleModeEnabled = shuffle
    }

    override suspend fun togglePlay() {
        val browser = fBrowser.await()
        if (browser.isPlaying) browser.pause()
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

    override suspend fun cycleRepeatMode(): Int {
        val browser = fBrowser.await()
        val current = browser.repeatMode
        val new = when (current) {
            PlaybackController.REPEAT_MODE_OFF -> PlaybackController.REPEAT_MODE_ONE
            PlaybackController.REPEAT_MODE_ONE -> PlaybackController.REPEAT_MODE_ALL
            else -> PlaybackController.REPEAT_MODE_OFF
        }
        browser.repeatMode = new
        return new
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
        val isCurrentIndex = browser.currentMediaItemIndex == index
        if (isCurrentIndex)
            browser.seekToNextMediaItem()
        browser.removeMediaItem(index)
        return true
    }

    override suspend fun setPlaybackSpeed(value: Float): Boolean {
        val browser = fBrowser.await() // Await the MediaBrowser instance.
        browser.playbackParameters =
            browser.playbackParameters.withSpeed(value) // Set the new playback speed.
        return browser.playbackParameters.speed == value // Verify if the speed was set correctly.
    }

    override suspend fun getPlaybackSpeed(): Float {
        val browser = fBrowser.await()
        return browser.playbackParameters.speed
    }

    override suspend fun getVideoView(): VideoProvider = VideoProvider(fBrowser.await())

    override suspend fun seekTo(pct: Float) {
        val browser = fBrowser.await()
        val duration = browser.duration
        if (!browser.isCommandAvailable(Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM) || duration == TIME_UNSET) return
        withContext(Dispatchers.Main) {
            browser.seekTo((duration * pct).toLong())
        }
    }

    override suspend fun seekBy(increment: Long): Boolean {
        val browser = fBrowser.await()
        if (!browser.isCommandAvailable(Player.COMMAND_SEEK_TO_MEDIA_ITEM)) return false
        val newMills = browser.currentPosition + increment
        browser.seekTo(browser.currentMediaItemIndex, newMills)
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

    override suspend fun setSleepAtMs(mills: Long) {
        // Get the media browser object from a deferred value
        val browser = fBrowser.await()
        // Send a custom command to the media browser with an empty bundle as arguments
        browser.sendCustomCommand(
            SessionCommand(Playback.ACTION_SCHEDULE_SLEEP_TIME, Bundle().apply {
                putLong(Playback.EXTRA_SCHEDULED_TIME_MILLS, mills)
            }) ,
            Bundle.EMPTY
        )
    }

    override suspend fun isPlaying(): Boolean {
        val browser = fBrowser.await()
        return browser.playWhenReady
    }

    @SuppressLint("UnsafeOptInUsageError")
    override suspend fun getAvailableTracks(type: Int): List<TrackInfo> {
        val browser = fBrowser.await()
        val provider = DefaultTrackNameProvider(context.resources)
        // Get the current tracks from the player or return an empty list if the player is null
        val tracks = browser.currentTracks
        // Get the track groups from the tracks
        val groups = tracks.groups
        // Create an empty list to store the track infos
        val list = ArrayList<TrackInfo>()
        // Loop through the indices of the track groups
        for (index in groups.indices) {
            // Get the track group at the current index
            val group = groups[index]
            // Skip the group if it is not of the given type
            if (group.type != type)
                continue
            // Loop through the tracks in the group
            for (trackIndex in 0 until group.length) {
                // Skip the track if it is not selected
                if (!group.isTrackSupported(trackIndex))
                    continue
                // Get the format of the track
                val format = group.getTrackFormat(trackIndex)
                // Skip the track if it has the forced selection flag
                /*if (format.selectionFlags and C.SELECTION_FLAG_FORCED != 0) {
                    continue
                }*/
                // Get the name of the track from the track name provider
                val name = provider.getTrackName(format)
                // Create a track selection override object with the group and the track index
                val params = TrackSelectionOverride(group.mediaTrackGroup, trackIndex)
                // Create a track info object with the name and the params
                list.add(TrackInfo(name, params))
            }
        }
        // Return the list of track infos
        return list
    }

    @SuppressLint("UnsafeOptInUsageError")
    override suspend fun getSelectedTrackFor(type: Int): TrackInfo? {
        val player = fBrowser.await()// return null if player is null
        // check if the player can set track selection parameters
        if (!player.isCommandAvailable(Player.COMMAND_SET_TRACK_SELECTION_PARAMETERS)) return null
        // get the current tracks and loop through the groups
        val groups = player.currentTracks.groups
        val provider = DefaultTrackNameProvider(context.resources)
        for (group in groups) {
            // skip the group if it is not of the given type
            if (group.type != type) continue
            // loop through the tracks in the group
            for (trackIndex in 0 until group.length) {
                // skip the track if it is not selected
                if (!group.isTrackSelected(trackIndex)) continue
                // get the format and the name of the track
                val format = group.getTrackFormat(trackIndex)
                val name = provider.getTrackName(format)
                // create a track selection override object with the group and the track index
                val params = TrackSelectionOverride(group.mediaTrackGroup, trackIndex)
                // create and return a track info object with the name and the params
                return TrackInfo(name, params)
            }
        }
        return null // no selection is made or possible
    }

    override suspend fun setCheckedTrack(info: TrackInfo?, type: Int): Boolean {
        val player = fBrowser.await()
        // check if the player can set track selection parameters
        if (!player.isCommandAvailable(Player.COMMAND_SET_TRACK_SELECTION_PARAMETERS)) return false
        // Get the current track selection parameters and build a new one
        player.trackSelectionParameters =
            player.trackSelectionParameters
                .buildUpon()
                .apply {
                    when {
                        type == C.TRACK_TYPE_TEXT && info == null -> {
                            // clear text track overrides and ignore forced text tracks
                            clearOverridesOfType(C.TRACK_TYPE_TEXT)
                            setIgnoredTextSelectionFlags(C.SELECTION_FLAG_FORCED.inv())
                        }

                        type == C.TRACK_TYPE_AUDIO && info == null -> {
                            // clear audio track overrides and enable audio track rendering
                            clearOverridesOfType(C.TRACK_TYPE_AUDIO)
                                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, false)
                        }

                        type == C.TRACK_TYPE_VIDEO && info == null -> {
                            // clear video track overrides and enable audio track rendering
                            clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                                .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, false)
                        }

                        info != null -> {
                            // select the specified audio track and enable track rendering
                            setOverrideForType(info.params)
                            setTrackTypeDisabled(info.params.type, false)
                        }

                        else -> error("Track $info & $type cannot be null or invalid")
                    }
                }.build()
        return true
    }
}
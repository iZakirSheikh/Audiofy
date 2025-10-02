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

import android.annotation.SuppressLint
import android.content.Context
import android.media.MediaMetadataRetriever
import android.media.audiofx.Equalizer
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.os.bundleOf
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import coil3.annotation.InternalCoilApi
import coil3.util.MimeTypeMap
import com.zs.core.common.await
import com.zs.core.common.debounceAfterFirst
import com.zs.core.common.runCatching
import com.zs.core.db.playlists.Playlists
import com.zs.core.playback.Remote.TrackInfo
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
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

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

    override suspend fun seekBy(increment: Long): Boolean {
        val browser = fBrowser.await()
        if (!browser.isCommandAvailable(Player.COMMAND_SEEK_TO_MEDIA_ITEM)) return false
        val newMills = browser.currentPosition + increment
        browser.seekTo(browser.currentMediaItemIndex, newMills)
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
        val isCurrentIndex = browser.currentMediaItemIndex == index
        if (isCurrentIndex)
            browser.seekToNextMediaItem()
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


    override suspend fun getNowPlaying(): NowPlaying? {
        // If the events are not null and do not contain any of the relevant state update events,
        // then there's no need to update the NowPlaying state, so we return early.
        // if (events != null && !events.containsAny(*Remote.STATE_UPDATE_EVENTS)) return@transform
        // Await the MediaBrowser instance.
        val provider = fBrowser.await()
        // Get the current media item from the provider.
        val current = provider.currentMediaItem
        // If there's no current media item, emit null (or the previous state will be retained by stateIn)
        // and return early.
        if (current == null)
            return null
        // Emit the newly created NowPlaying state.
        return NowPlaying(
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
            sleepAt = getRemainingSleepTime()
        )
    }

    override val state: StateFlow<NowPlaying?> = events
        .filter { it == null || it.containsAny(*Remote.STATE_UPDATE_EVENTS) }
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
            val list = provider.getChildren(Remote.ROOT_QUEUE, 0, Int.MAX_VALUE, null).await().value
            // Emit the updated list of MediaFile objects.
            emit(list?.map(::MediaFile))
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

    override suspend fun setSleepTimer(mills: Long) {
        val browser = fBrowser.await()
        browser[Remote.SCHEDULE_SLEEP_TIME] = bundleOf(
            Remote.EXTRA_SCHEDULED_TIME_MILLS to mills
        )
    }

    override suspend fun getRemainingSleepTime(): Long {
        val browser = fBrowser.await()
        val result = browser[Remote.SCHEDULE_SLEEP_TIME]
        // Get the scheduled time from the result or use the uninitialized value
        return result.extras.getLong(Remote.EXTRA_SCHEDULED_TIME_MILLS)
    }

    override suspend fun isPlaying(): Boolean {
        val browser = fBrowser.await()
        return browser.playWhenReady
    }

    override suspend fun setEqualizer(eq: Equalizer?) {
        // Extract the enabled state and properties from the provided Equalizer instance.
        val enabled = eq?.enabled ?: false
        val properties = eq?.properties?.toString()

        // Release the provided Equalizer instance.
        eq?.release()

        // Get the media browser object from a deferred value
        val browser = fBrowser.await()
        // Send a custom command to the Playback with required equalizer args.
        browser[Remote.EQUALIZER_CONFIG] = bundleOf(
            Remote.EXTRA_EQUALIZER_ENABLED to enabled,
            Remote.EXTRA_EQUALIZER_PROPERTIES to properties
        )
    }

    override suspend fun getEqualizer(priority: Int): Equalizer {
        // construct an equalizer from the settings received from the service
        val browser = fBrowser.await()

        val id = browser[Remote.AUDIO_SESSION_ID].extras.getInt(Remote.EXTRA_AUDIO_SESSION_ID)
        return Equalizer(priority, id).apply {
            val properties =
                browser[Remote.EQUALIZER_CONFIG].extras.getString(Remote.EXTRA_EQUALIZER_PROPERTIES)
            if (!properties.isNullOrBlank())
                setProperties(Equalizer.Settings(properties))
            enabled =
                browser[Remote.EQUALIZER_CONFIG].extras.getBoolean(Remote.EXTRA_EQUALIZER_ENABLED)
        }
    }

    @OptIn(InternalCoilApi::class)
    @SuppressLint("UnsafeOptInUsageError")
    override suspend fun setMediaItem(uri: Uri) {
        var retriever: MediaMetadataRetriever? = null
        try {
            // Initialize MediaMetadataRetriever to extract metadata from the media file.
            retriever = MediaMetadataRetriever()
            val artwork: Uri?
            withContext(Dispatchers.IO) {
                // Set the data source for the retriever.
                when (uri.scheme) {
                    "content" -> retriever.setDataSource(context, uri)
                    "file" -> retriever.setDataSource(uri.path)
                    else -> retriever.setDataSource(uri.toString(), hashMapOf())
                }
                // Attempt to extract and cache the embedded album artwork.
                artwork = runCatching(TAG) {
                    // Create a temporary file in the cache directory to store the artwork.
                    val file = File(context.cacheDir, "tmp_artwork.png")
                    // Delete the old cached artwork file, if it exists.
                    // This ensures that the latest album artwork is used, even if the track previously lacked artwork.
                    file.delete()
                    // Retrieve the embedded picture raw data. If null, no artwork exists, so return null.
                    val bytes = retriever.embeddedPicture ?: return@runCatching null
                    // Write the artwork bytes to the temporary file.
                    val fos = FileOutputStream(file)
                    fos.write(bytes)
                    fos.close()
                    // Return the URI of the cached artwork file.
                    Uri.fromFile(file)
                }

            }
            // Create a MediaFile object using the extracted metadata.
            // If a metadata field is not found, default to an empty string.
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                ?: uri.lastPathSegment
                ?: uri.path?.substringAfterLast('/') // fallback if lastPathSegment is null

            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
                ?: MimeTypeMap.getMimeTypeFromUrl(uri.toString())
            val item = MediaFile(
                subtitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST) ?: "",
                uri = uri,
                artwork = artwork,
                mimeType = mimeType,
                title =  title ?: "",
            )
            // Set the created MediaFile as the current media item in the player.
            setMediaFiles(listOf(item))
        } catch (e: Exception) {
            Log.d(TAG, "setMediaItem: error: ${e.message}")
        } finally {
            // We must call 'close' on API 29+ to avoid a strict mode warning.
            if (Build.VERSION.SDK_INT >= 29) retriever?.close() else retriever?.release()
        }
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
                    when{
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
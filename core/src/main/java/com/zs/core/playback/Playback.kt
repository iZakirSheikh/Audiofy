@file:SuppressLint("UnsafeOptInUsageError")/*
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
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.audiofx.Equalizer
import android.media.audiofx.Equalizer.Settings
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.media3.common.C
import androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.session.LibraryResult
import androidx.media3.session.MediaLibraryService
import androidx.media3.session.MediaLibraryService.MediaLibrarySession.Callback
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSession.ConnectionResult
import androidx.media3.session.MediaSession.ControllerInfo
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.zs.core.R
import com.zs.core.common.future
import com.zs.core.common.get
import com.zs.core.common.getValue
import com.zs.core.common.runCatching
import com.zs.core.common.set
import com.zs.core.common.showPlatformToast
import com.zs.core.db.playlists.Playlist
import com.zs.core.db.playlists.Playlists
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.random.Random
import androidx.media3.common.AudioAttributes.Builder as AudioAttributes

private const val TAG = "Playback"

// Keys for SharedPreferences.
private const val PREF_KEY_SHUFFLE_MODE = "_shuffle"
private const val PREF_KEY_REPEAT_MODE = "_repeat_mode"
private const val PREF_KEY_INDEX = "_index"
private const val PREF_KEY_BOOKMARK = "_bookmark"
private const val PREF_KEY_RECENT_PLAYLIST_LIMIT = "_max_recent_size"
private const val PREF_KEY_EQUALIZER_ENABLED = "_equalizer_enabled"
private const val PREF_KEY_EQUALIZER_PROPERTIES = "_equalizer_properties"
private const val PREF_KEY_CLOSE_WHEN_REMOVED = "_stop_playback_when_removed"
private const val PREF_KEY_ORDERS = "_orders"

//
private const val SAVE_POSITION_DELAY_MILLS = 5_000L
private const val LIST_ITEM_DELIMITER = ';'

// Buttons
private val LikeButton = CommandButton(R.drawable.ic_star, "Like", Remote.TOGGLE_LIKE)
private val UnlikeButton = CommandButton(R.drawable.ic_star_border, "Unlike", Remote.TOGGLE_LIKE)

class Playback : MediaLibraryService(), Callback, Player.Listener {
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private lateinit var preferences: SharedPreferences
    private lateinit var playlists: Playlists

    /**
     *  A coroutine job that monitors the playback state and performs some actions based on it.
     *  It saves the current playback position every 5 seconds and pauses the player if the sleep time is reached.
     *  The job is cancelled when the service is destroyed or the playback is stopped.
     */
    private var playbackMonitorJob: Job? = null

    /**
     * The timestamp, in milliseconds, indicating a future point in time when the player is
     * scheduled to be paused or [Remote.TIME_UNSET].
     */
    private var scheduledPauseTimeMillis = Remote.TIME_UNSET

    /**
     * The PendingIntent associated with the underlying activity.
     * It launches the main activity when triggered.
     */
    private val activity by lazy {
        val intent = packageManager.getLaunchIntentForPackage(packageName)
        PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE)
    }

    /**
     * Lazily initializes and configures an ExoPlayer instance for audio playback.
     * The player is configured with audio attributes suitable for music content,
     * and it automatically handles audio becoming noisy.
     */
    private val player: Player by lazy {
        // Create a Renderer factory. Use DynamicRendererFactory if available,
        // otherwise use DefaultRenderersFactory.
        // Enable decoder fallback to handle different media formats.
        val factory = DynamicRendererFactory(applicationContext) ?: DefaultRenderersFactory(
            applicationContext
        )
        factory.setEnableDecoderFallback(true)
        factory.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)

        val attr =
            AudioAttributes().setContentType(AUDIO_CONTENT_TYPE_MUSIC).setUsage(C.USAGE_MEDIA)
                .build()

        ExoPlayer.Builder(this).setRenderersFactory(factory).setAudioAttributes(attr, true)
            .setHandleAudioBecomingNoisy(true).build()
    }

    /**
     * Lazily initializes a [MediaLibrarySession] for managing media playback.
     * It restores the saved state, adds the current class as a listener to the player,
     * and sets the session activity using the [activity] PendingIntent.
     */
    private val session: MediaLibrarySession by lazy {
        // Build and configure the MediaLibrarySession
        MediaLibrarySession.Builder(this, player, this).setId("playback")
            .setSessionActivity(activity).build()
    }

    /**
     * Manages the audio equalizer for enhancing audio effects.
     *
     * This class is responsible for configuring and controlling the audio equalizer
     * to improve the quality of audio output.
     *
     * @see ACTION_EQUALIZER_CONFIG
     */
    private var equalizer: Equalizer? = null

    // init
    override fun onCreate() {
        super.onCreate()
        playlists = Playlists(this)
        preferences = getSharedPreferences("playback_prefs", Context.MODE_PRIVATE)
        // Init
        // Asynchronously restore the saved state of the service
        scope.launch {
            runCatching(TAG) {
                // Restore shuffle mode and repeat mode
                player.shuffleModeEnabled = preferences[PREF_KEY_SHUFFLE_MODE, false]
                player.repeatMode = preferences[PREF_KEY_REPEAT_MODE, Player.REPEAT_MODE_OFF]
                // Set media items from the QUEUE playlist
                val items = withContext(Dispatchers.IO) {
                    val data = playlists.getTracks(Remote.PLAYLIST_QUEUE)
                    data.map(Playlist.Track::toMediaSource)
                }
                player.setMediaItems(items)
                // Restore the saved shuffle order
                val orders by runCatching {
                    val value = preferences[PREF_KEY_ORDERS, ""].split(LIST_ITEM_DELIMITER)
                    if (value.isEmpty()) null else value.map(String::toInt).toIntArray()
                }
                (player as ExoPlayer).setShuffleOrder(
                    DefaultShuffleOrder(orders ?: IntArray(0), Random.nextLong())
                )
                // Seek to the saved playback position
                val index = preferences[PREF_KEY_INDEX, C.INDEX_UNSET]
                if (index != C.INDEX_UNSET) {
                    player.seekTo(index, preferences[PREF_KEY_BOOKMARK, C.TIME_UNSET])
                    // Now if the currentMediaItem is 3rd party uri.
                    // just remove it.
                    if (player.currentMediaItem?.mediaUri?.isThirdPartyUri == true) player.removeMediaItem(
                        index
                    )
                }
            }
            // Regardless of whether errors occurred during state restoration or not, add the current
            // class as a player listener
            player.addListener(this@Playback)
            // Initialize the audio effects;
            onAudioSessionIdChanged(-1)
        }
    }

    /**
     * Forces the player to emit a shuffle mode change event to its listeners.
     *
     * Temporarily toggles [shuffleModeEnabled] so that `onShuffleModeChanged`
     * callbacks are invoked, even if the shuffle mode ultimately remains unchanged.
     */
    fun Player.emit(){
        scope.launch {
            // poke player listeners
            val shuffle = shuffleModeEnabled
            shuffleModeEnabled = !shuffle
            delay(5)
            shuffleModeEnabled = shuffle
        }
    }

    /**
     * Returns the session associated with the provided [controllerInfo].
     * @param controllerInfo The controller information.
     * @return The associated session.
     */
    override fun onGetSession(controllerInfo: ControllerInfo) = session

    // Returns a list of media items converted to media sources to be added to the player.
    override fun onAddMediaItems(
        mediaSession: MediaSession, controller: ControllerInfo, mediaItems: MutableList<MediaItem>
    ): ListenableFuture<List<MediaItem>> =
        Futures.immediateFuture(mediaItems.map(MediaItem::asMediaSource))

    // FIXME: The impact of this function on the application's behavior is unclear.
    // It returns a library result of a media item with the [BROWSER_ROOT_QUEUE] identifier and optional parameters.
    @SuppressLint("UnsafeOptInUsageError")
    override fun onGetLibraryRoot(
        session: MediaLibrarySession, browser: ControllerInfo, params: LibraryParams?
    ): ListenableFuture<LibraryResult<MediaItem>> =
        Futures.immediateFuture(LibraryResult.ofItem(MediaRoot(Remote.ROOT_QUEUE), params))

    // Returns the individual media item identified by the given [mediaId].
    // FixMe - We don't support this.
    override fun onGetItem(
        session: MediaLibrarySession, browser: ControllerInfo, mediaId: String
    ): ListenableFuture<LibraryResult<MediaItem>> {
        val item = player.mediaItems.find { it.mediaId == mediaId }
        val result = if (item == null) LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
        else LibraryResult.ofItem(item, /* params = */ null)
        return Futures.immediateFuture(result)
    }

    // TODO: Determine how to return the playing queue with upcoming items only.
    //  This function is intended to handle subscriptions to media items under a parent.
    override fun onSubscribe(
        session: MediaLibrarySession,
        browser: ControllerInfo,
        parentId: String,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<Void>> {
        val children = when (parentId) {
            Remote.ROOT_QUEUE -> player.queue
            else -> return Futures.immediateFuture(
                LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
            )
        }
        session.notifyChildrenChanged(browser, parentId, children.size, params)
        return Futures.immediateFuture(LibraryResult.ofVoid())
    }

    //  * TODO: Consider adding support for paging to efficiently retrieve children.
    // This function retrieves the children of a specified parent item for a given page and page size.
    override fun onGetChildren(
        session: MediaLibrarySession,
        browser: ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val children = when (parentId) {
            Remote.ROOT_QUEUE -> player.queue
            else -> return Futures.immediateFuture(
                LibraryResult.ofError(SessionError.ERROR_BAD_VALUE)
            )
        }
        return Futures.immediateFuture(LibraryResult.ofItemList(children, params))
    }

    // Called when a media item transition occurs in the player.
    override fun onMediaItemTransition(
        mediaItem: MediaItem?, reason: Int
    ) {
        // Save the current media item index in SharedPreferences.
        scope.launch { preferences[PREF_KEY_INDEX] = player.currentMediaItemIndex }
        // Notify connected controllers that the children of the root queue have changed.
        // This ensures UIs are updated to reflect the new current item.
        session.notifyChildrenChanged(Remote.ROOT_QUEUE, 0, null)

        // If the mediaItem is null (e.g., end of playlist) or it's a third-party URI,
        // do not proceed with adding it to the "Recent" playlist.
        if (mediaItem == null || mediaItem.mediaUri?.isThirdPartyUri == true) return
        // update like button
        scope.launch {
            val key = player.currentMediaItem?.mediaUri?.toString() ?: ""
            val liked = playlists.contains(Remote.PLAYLIST_FAVOURITE, key)
            session.setMediaButtonPreferences(
                listOf(if (liked) LikeButton else UnlikeButton)
            )
        }
        // Save the file in the "Recent" playlist.
        // TODO: Implement a delay before saving to "Recent". This ensures only items
        //  viewed for a certain duration are added, improving the relevance of the "Recent" list.
        // TODO: Also, consider saving bookmarks for these recent items, using the URI as the key.
        scope.launch {
            // Get the limit for the number of items in the "Recent" playlist from preferences.
            val limit = preferences[PREF_KEY_RECENT_PLAYLIST_LIMIT, 50]

            // Get the ID of the "Recent" playlist. If it doesn't exist, create it.
            val playlistId = playlists[Remote.PLAYLIST_RECENT]?.id
                ?: playlists.insert(Playlist(name = Remote.PLAYLIST_RECENT))

            // Retrieve the "Recent" playlist.
            val playlist = playlists[playlistId]!!
            // Update the 'dateModified' of the playlist.
            // FIXME: Find a more efficient way to update the playlist's modification timestamp
            //  without necessarily needing to do this.
            playlists.update(playlist = playlist.copy(desc = ""))

            // Check if the media item already exists in the "Recent" playlist.
            val member = playlists._get(playlistId, mediaItem.requestMetadata.mediaUri.toString())
            if (member != null) {
                // If the item exists, update its order to 0 (move it to the top).
                playlists.update(member = member.copy(order = 0))
            } else {
                // If the item doesn't exist, delete the oldest item if the playlist exceeds the limit,
                // and then insert the new item at the top (order 0).
                playlists._delete(playlistId, limit)
                playlists.insert(listOf(mediaItem.toTrack(playlistId, 0)))
            }
        }
    }

    // Update shuffle mode pref.
    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
        scope.launch() { preferences[PREF_KEY_SHUFFLE_MODE] = shuffleModeEnabled }
        session.notifyChildrenChanged(Remote.ROOT_QUEUE, 0, null)
    }

    // update repeat mode pref.
    override fun onRepeatModeChanged(repeatMode: Int) {
        scope.launch() { preferences[PREF_KEY_REPEAT_MODE] = repeatMode }
    }

    // Called when the player's timeline changes, indicating a change in the playlist.
    override fun onTimelineChanged(timeline: Timeline, reason: Int) {
        // Only proceed if the timeline change is due to a playlist modification.
        if (reason != Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) return

        // Notify connected controllers that the children of the root queue have changed.
        // This prompts them to refresh their view of the queue.
        session.notifyChildrenChanged(Remote.ROOT_QUEUE, 0, null)
        // Launch a coroutine to handle database operations asynchronously.
        scope.launch {
            // Obtain the current list of media items from the player.
            val items = player.mediaItems
            // TODO: Consider moving this logic to onDestroy. This would allow handling
            //  queue operations (move, remove, add) without frequent database updates,
            //  potentially improving performance and reducing database load.

            // Get the ID of the "queue" playlist. If it doesn't exist, create it.
            val id = playlists[Remote.PLAYLIST_QUEUE]?.id
                ?: playlists.insert(Playlist(Remote.PLAYLIST_QUEUE, ""))

            // Clear the existing tracks from the "queue" playlist in the database.
            playlists.clear(id)

            // Convert the MediaItems to Track objects and insert them into the database.
            val tracks = items.mapIndexed { index, mediaItem -> mediaItem.toTrack(id, index) }
            playlists.insert(tracks)
            // Save the current shuffle order to preferences.
            preferences[PREF_KEY_ORDERS] =
                player.orders.joinToString(LIST_ITEM_DELIMITER.toString())
        }
    }

    //
    override fun onPlayerError(error: PlaybackException) {
        // TODO - Show toast only when in foreground.
        //      Maybe add preference for moving to next.
        // Display a simple toast message indicating an unplayable file
        Toast.makeText(this, "Unplayable file", Toast.LENGTH_SHORT).show()
        // You may choose to handle the error here or take other actions like seeking to the next media item
        player.seekToNextMediaItem()
    }

    override fun onPlayWhenReadyChanged(isPlaying: Boolean, reason: Int) {
        super.onPlayWhenReadyChanged(isPlaying, reason)
        //  Just cancel the job in case is playing false
        if (!isPlaying) {
            playbackMonitorJob?.cancel()
            // change this to uninitialized
            scheduledPauseTimeMillis = Remote.TIME_UNSET
            player.emit()
        }
        // Launch a new job
        else playbackMonitorJob = scope.launch {
            var isPlaying = player.playWhenReady
            do {
                // Save the current playback position to preferences
                preferences[PREF_KEY_BOOKMARK] = player.currentPosition
                Log.i(TAG, "Saved playback position: ${player.currentPosition}")

                // Check if playback is scheduled to be paused.
                if (scheduledPauseTimeMillis != Remote.TIME_UNSET && scheduledPauseTimeMillis <= System.currentTimeMillis()) {
                    // Pause the player as the scheduled pause time has been reached.
                    player.pause()

                    // Once the scheduled pause has been triggered, reset the scheduled time to uninitialized.
                    scheduledPauseTimeMillis = Remote.TIME_UNSET
                    player.emit()
                }
                // Delay for the specified time
                delay(SAVE_POSITION_DELAY_MILLS)
                isPlaying = player.isPlaying
            } while (isPlaying)
        }
    }

    /**
     * Handles a connection request from a media controller to this media session.
     * Add the supported custom commands to the session.
     *@see [MediaLibraryService.onConnect]
     * @see [ConnectionResult]
     * @see [SessionCommand1]
     * @see [ACTION_AUDIO_SESSION_ID]
     */
    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun onConnect(session: MediaSession, controller: ControllerInfo): ConnectionResult {
        // obtain the available commands
        // TODO: Maybe add more buttons to notifications.
        val available = ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()

        // add commands to session.
        for (command in Remote.commands) {
            available.add(command)
        }


        // return immediately with default button (e.g. LikeButton)
        val result = ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(available.build())
            .build()

        // update button async once we know the liked state
        scope.launch {
            val key = player.currentMediaItem?.mediaUri?.toString() ?: ""
            val liked = playlists.contains(Remote.PLAYLIST_FAVOURITE, key)
            session.setMediaButtonPreferences(
                listOf(if (liked) LikeButton else UnlikeButton)
            )
        }

        return result
    }

    //
//    Initialize audio effects here. If the [audioSessionId] is -1, it indicates that someone called
//    this function locally, and there is no need to create a new instance of the effect.
//    Instead, only initialize the settings of the effect.
//
//    If the effect is null, it should be created if supported; otherwise, it should not be created.
    @SuppressLint("UnsafeOptInUsageError")
    override fun onAudioSessionIdChanged(audioSessionId: Int) {
        if (audioSessionId != -1) super.onAudioSessionIdChanged(audioSessionId)

        // Initialize the equalizer with the audio session ID from the ExoPlayer.
        // We only reinitialize it if the 'audioSessionId' is not -1,
        // which means the system called this function with a new session ID.
        // In this case, we reinitialize the audio effect; otherwise, we just apply settings.
        if (equalizer == null || audioSessionId != -1) {
            equalizer?.release()
            // TODO: Find the real reason why equalizer is not init when calling from onCreate.
            equalizer = runCatching {
                Equalizer(0, (player as ExoPlayer).audioSessionId)
            }.getOrNull()
        }

        scope.launch {
            // Enable the equalizer.
            equalizer?.enabled = preferences[PREF_KEY_EQUALIZER_ENABLED, false]

            // Retrieve equalizer properties from preferences.
            val properties = preferences[PREF_KEY_EQUALIZER_PROPERTIES, ""]

            // Apply equalizer properties only if they are not null or blank.
            if (properties.isNotBlank()) {
                equalizer?.properties = Settings(properties)
            }
        }
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun onCustomCommand(
        session: MediaSession, controller: ControllerInfo, command: SessionCommand, args: Bundle
    ): ListenableFuture<SessionResult> {
        return when (command) {
            // Handle the command to retrieve the audio session ID from the player.
            Remote.AUDIO_SESSION_ID -> {
                // Retrieve the audio session ID from the ExoPlayer.
                val audioSessionId = (player as ExoPlayer).audioSessionId

                // Create a result with the audio session ID and return it immediately.
                val result = SessionResult(SessionResult.RESULT_SUCCESS) {
                    putInt(Remote.EXTRA_AUDIO_SESSION_ID, audioSessionId)
                }

                // Return the result with the audio session ID.
                Futures.immediateFuture(result)
            }
            // Handle sleep timer commands
            Remote.SCHEDULE_SLEEP_TIME -> {
                // Retrieve the scheduled time in milliseconds from the command's custom extras
                val newTimeMills = args.getLong(Remote.EXTRA_SCHEDULED_TIME_MILLS)
                // If the new time is 0, it means the client wants to retrieve the sleep timer.
                // If the new time is not zero, set the new sleep timer.
                if (newTimeMills != 0L) {
                    player.emit()
                    scheduledPauseTimeMillis = if (newTimeMills == Remote.TIME_UNSET) Remote.TIME_UNSET else newTimeMills + System.currentTimeMillis()
                }

                // Regardless of whether the client wants to set or retrieve the timer,
                // include the current or updated timer value in the response to the client.
                Futures.immediateFuture(
                    SessionResult(SessionResult.RESULT_SUCCESS) {
                        putLong(
                            Remote.EXTRA_SCHEDULED_TIME_MILLS, if (scheduledPauseTimeMillis == Remote.TIME_UNSET) Remote.TIME_UNSET else scheduledPauseTimeMillis - System.currentTimeMillis()
                        )
                    })
            }
            // config
            Remote.EQUALIZER_CONFIG -> {
                // obtain the extras that are accompanied with this action.
                val extras = args

                // if extras are empty means user wants to retrieve the equalizer saved config.
                if (!extras.isEmpty) {
                    val isEqualizerEnabled =
                        args.getBoolean(Remote.EXTRA_EQUALIZER_ENABLED)
                    val properties = args.getString(
                        Remote.EXTRA_EQUALIZER_PROPERTIES, null
                    )
                    // save in pref
                    scope.launch() {
                        preferences[PREF_KEY_EQUALIZER_PROPERTIES] = properties
                        preferences[PREF_KEY_EQUALIZER_ENABLED] = isEqualizerEnabled
                        delay(100) // delay to detach old equalizer.
                        onAudioSessionIdChanged(-1)
                    }
                }

                // in both cases weather retrieve or set; return the extras
                // include the current config of equalizer response to the client.
                Futures.immediateFuture(
                    SessionResult(SessionResult.RESULT_SUCCESS) {
                        putBoolean(
                            Remote.EXTRA_EQUALIZER_ENABLED,
                            runBlocking() { preferences[PREF_KEY_EQUALIZER_ENABLED, false] })
                        putString(
                            Remote.EXTRA_EQUALIZER_PROPERTIES,
                            runBlocking() { preferences[PREF_KEY_EQUALIZER_PROPERTIES, ""] })
                    })
            }
            // Handle scrubbing mode commands
            Remote.SCRUBBING_MODE -> {
                val enabled = args.getBoolean(Remote.EXTRA_SCRUBBING_MODE_ENABLED)
                (player as ExoPlayer).isScrubbingModeEnabled = enabled
                Log.d(TAG, "onCustomCommand: $enabled")
                Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
            }
            // Like/Unlike
            Remote.TOGGLE_LIKE -> scope.future {
                val item = player.currentMediaItem
                    ?: return@future SessionResult(SessionError.ERROR_INVALID_STATE)
                val isLiked = playlists.toggleLike(item)
                session.setMediaButtonPreferences(listOf(if (isLiked) LikeButton else UnlikeButton))
                // poke player listeners
                player.emit()
                showPlatformToast(if (isLiked) "Liked ❤️✨" else "Unliked 💔")
                SessionResult(SessionResult.RESULT_SUCCESS)
            }
            // Handle unrecognized or unsupported commands.
            else -> Futures.immediateFuture(SessionResult(SessionError.ERROR_UNKNOWN))
        }
    }

    /**
     * Releases resources and cancels any associated scope before destroying the service.
     * @see Service.onDestroy
     */
    override fun onDestroy() {
        // Release the media player to free up system resources.
        player.removeListener(this)
        player.release()
        // Release the audio session associated with the media player.
        session.release()
        // Release the equalizer (if it exists) to free up resources.
        equalizer?.release()
        // Cancel any ongoing coroutines associated with this scope.
        scope.cancel()
        // Call the superclass method to properly handle onDestroy().
        super.onDestroy()
    }

    // stop player if user wished.
    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        player.playWhenReady = false
        if (runBlocking { preferences[PREF_KEY_CLOSE_WHEN_REMOVED, false] }) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
                stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }
}
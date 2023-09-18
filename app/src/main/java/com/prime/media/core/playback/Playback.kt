package com.prime.media.core.playback

import android.annotation.SuppressLint
import android.app.*
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.widget.Toast
import androidx.media3.common.*
import androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.session.*
import androidx.media3.session.MediaLibraryService.MediaLibrarySession.Callback
import androidx.media3.session.MediaSession.ConnectionResult
import androidx.media3.session.MediaSession.ControllerInfo
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.prime.media.BuildConfig
import com.prime.media.R
import com.prime.media.console.Widget
import com.prime.media.core.db.Playlist
import com.prime.media.core.db.Playlists
import com.prime.media.core.playback.Playback.Companion.UNINITIALIZED_SLEEP_TIME_MILLIS
import com.primex.preferences.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import org.json.JSONArray
import javax.inject.Inject
import kotlin.Boolean
import kotlin.Int
import kotlin.IntArray
import kotlin.String
import kotlin.Unit
import kotlin.apply
import kotlin.getValue
import kotlin.lazy
import kotlin.random.Random
import kotlin.runCatching
import kotlin.with
import androidx.media3.session.SessionCommand as Command
import androidx.media3.session.SessionResult as Result

/**
 * @see [Result]
 */
private inline fun Result(code: Int, args: Bundle.() -> Unit) =
    Result(code, Bundle().apply(args))

private const val TAG = "Playback"

/**
 * The name of the global 'Favourite' playlist in [Playlists].
 */
private val PLAYLIST_FAVOURITE = Playlists.PRIVATE_PLAYLIST_PREFIX + "favourite"

/**
 * The name of the global 'Recent' playlist in [Playlists].
 */
private val PLAYLIST_RECENT = Playlists.PRIVATE_PLAYLIST_PREFIX + "recent"

/**
 * The name of the global 'Queue' playlist in [Playlists].
 */
private val PLAYLIST_QUEUE = Playlists.PRIVATE_PLAYLIST_PREFIX + "queue"

/**
 * The root identifier for the queue-related content served by this service.
 * Used as a key to access queue-related content in the application's data structure.
 */
private const val ROOT_QUEUE = "com.prime.player.queue"

/**
 * A MediaItem impl of [ROOT_QUEUE]
 */
private val BROWSER_ROOT_QUEUE =
    MediaItem.Builder()
        .setMediaId(ROOT_QUEUE)
        .setMediaMetadata(
            MediaMetadata.Builder().setIsBrowsable(true)
                .setIsPlayable(false)
                .build()
        )
        .build()

// Keys used for saving various states in SharedPreferences.
private val PREF_KEY_SHUFFLE_MODE = booleanPreferenceKey("_shuffle", false)
private val PREF_KEY_REPEAT_MODE = intPreferenceKey("_repeat_mode", Player.REPEAT_MODE_OFF)
private val PREF_KEY_INDEX = intPreferenceKey("_index", C.INDEX_UNSET)
private val PREF_KEY_BOOKMARK = longPreferenceKey("_bookmark", C.TIME_UNSET)
private val PREF_KEY_RECENT_PLAYLIST_LIMIT = intPreferenceKey("_max_recent_size", 50)

/**
 * Key for saving custom orders of items in SharedPreferences.
 * Uses a custom serializer/deserializer for [IntArray].
 */
private val PREF_KEY_ORDERS = stringPreferenceKey(
    "_orders",
    IntArray(0),
    object : StringSaver<IntArray> {
        override fun restore(value: String): IntArray {
            val arr = JSONArray(value)
            return IntArray(arr.length()) {
                arr.getInt(it)
            }
        }

        override fun save(value: IntArray): String {
            val arr = JSONArray(value)
            return arr.toString()
        }
    }
)

/**
 * Audio attributes configuration for playback in the [Playback] service.
 * These attributes specify the content type as [AUDIO_CONTENT_TYPE_MUSIC] and the usage as [C.USAGE_MEDIA].
 */
private val PlaybackAudioAttr = AudioAttributes.Builder()
    .setContentType(AUDIO_CONTENT_TYPE_MUSIC)
    .setUsage(C.USAGE_MEDIA)
    .build()

/**
 * The delay duration, in milliseconds, for periodically saving the playback position.
 * This constant specifies the time interval at which the current playback position
 * should be saved to preferences during playback.
 */
private const val SAVE_POSITION_DELAY_MILLS = 5_000L

/**
 * A action string for [SessionCommand1] for getting [EXTRA_AUDIO_SESSION_ID].
 *
 *  The client can use this action to send a custom command to the service and request the current audio session id.
 */
private const val ACTION_AUDIO_SESSION_ID = BuildConfig.APPLICATION_ID + ".action.AUDIO_SESSION_ID"

/**
 * A key for the extra bundle that contains the audio session id
 */
private const val EXTRA_AUDIO_SESSION_ID = BuildConfig.APPLICATION_ID + ".extra.AUDIO_SESSION_ID"

/**
 * Action string for scheduling sleep time for getting/setting [EXTRA_SCHEDULED_TIME_MILLS]
 *
 * Clients can use this action to send a custom command to the service and schedule sleep time.
 */
private const val ACTION_SCHEDULE_SLEEP_TIME =
    BuildConfig.APPLICATION_ID + ".action.SCHEDULE_SLEEP_TIME"

/**
 * Key for the extra bundle that contains the scheduled time in milliseconds for sleep.
 *
 * This key is used in conjunction with [ACTION_SCHEDULE_SLEEP_TIME] to specify or retrieve the scheduled time for sleep in milliseconds.
 * A value of [UNINITIALIZED_SLEEP_TIME_MILLIS] represents no scheduled time or cancels any previously scheduled sleep time.
 */
private const val EXTRA_SCHEDULED_TIME_MILLS =
    BuildConfig.APPLICATION_ID + ".extra.AUDIO_SESSION_ID"

/**
 * Constant representing an uninitialized or canceled scheduled time in milliseconds.
 * When no sleep timer is scheduled or if it has been canceled, this value is used.
 */
private const val UNINITIALIZED_SLEEP_TIME_MILLIS = -1L

/**
 * Checks if the given URI is from a third-party source.
 *
 * This property evaluates whether the URI scheme is "content://" and the authority
 * is not equal to [MediaStore.AUTHORITY]. If these conditions are met, it indicates
 * that the URI is from a third-party source.
 *
 * @param uri The URI to be checked.
 * @return `true` if the URI is from a third-party source, `false` otherwise.
 */
private val Uri.isThirdPartyUri get() = scheme == ContentResolver.SCHEME_CONTENT && authority != MediaStore.AUTHORITY

/**
 * The Playback Service class that utilizes Media3 for media playback.
 *
 * This service class extends [MediaLibraryService] and implements the [Callback] and [Player.Listener]
 * interfaces, allowing it to manage media playback, handle media library requests, and respond to
 * player events.
 *
 * @see MediaLibraryService
 * @see Callback
 * @see Player.Listener
 */
@AndroidEntryPoint
class Playback : MediaLibraryService(), Callback, Player.Listener {

    //These dependencies are injected to manage the state of this service
    //using persistent storage and handle playlists
    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var playlists: Playlists

    // Create a coroutine scope tied to the service's lifecycle
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /**
     *  A coroutine job that monitors the playback state and performs some actions based on it.
     *  It saves the current playback position every 5 seconds and pauses the player if the sleep time is reached.
     *  The job is cancelled when the service is destroyed or the playback is stopped.
     */
    private var playbackMonitorJob: Job? = null

    /**
     * The timestamp, in milliseconds, representing the scheduled time to pause playback.
     * This variable is used to store the timestamp when playback should be paused in the future.
     * If no future pause is scheduled, the value is set to -1.
     */
    private var scheduledPauseTimeMillis = UNINITIALIZED_SLEEP_TIME_MILLIS

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
        ExoPlayer.Builder(this)
            .setAudioAttributes(PlaybackAudioAttr, true)
            .setHandleAudioBecomingNoisy(true).build()
    }

    /**
     * Lazily initializes a [MediaLibrarySession] for managing media playback.
     * It restores the saved state, adds the current class as a listener to the player,
     * and sets the session activity using the [activity] PendingIntent.
     */
    private val session: MediaLibrarySession by lazy {
        // // Asynchronously restore the saved state of the service
        scope.launch {
            runCatching { onRestoreSavedState() }
            // Regardless of whether errors occurred during state restoration or not, add the current
            // class as a player listener
            player.addListener(this@Playback)
        }

        // Build and configure the MediaLibrarySession
        MediaLibrarySession.Builder(this, player, this)
            .setSessionActivity(activity)
            .build()
    }

    /**
     * Restore the saved state of this service, including shuffle mode, repeat mode, media items,
     * shuffle order, and playback position.
     */
    @SuppressLint("UnsafeOptInUsageError")
    private suspend fun onRestoreSavedState() {
        with(player) {
            // Restore shuffle mode and repeat mode
            shuffleModeEnabled = preferences.value(PREF_KEY_SHUFFLE_MODE)
            repeatMode = preferences.value(PREF_KEY_REPEAT_MODE)

            // Set media items from the QUEUE playlist
            val items = withContext(Dispatchers.IO) {
                playlists.getMembers(PLAYLIST_QUEUE).map(Playlist.Member::toMediaSource)
            }
            setMediaItems(items)

            // Restore the saved shuffle order
            (this as ExoPlayer).setShuffleOrder(
                DefaultShuffleOrder(preferences.value(PREF_KEY_ORDERS), Random.nextLong())
            )

            // Seek to the saved playback position
            val index = preferences.value(PREF_KEY_INDEX)
            if (index != C.INDEX_UNSET) {
                seekTo(index, preferences.value(PREF_KEY_BOOKMARK))

                // Now if the currentMediaItem is 3rd party uri.
                // just remove it.
                if (currentMediaItem?.mediaUri?.isThirdPartyUri == true)
                    player.removeMediaItem(index)
            }
        }
    }

    /**
     * Returns the session associated with the provided [controllerInfo].
     * @param controllerInfo The controller information.
     * @return The associated session.
     */
    override fun onGetSession(controllerInfo: ControllerInfo) = session

    /**
     * Returns a list of media items converted to media sources to be added to the player.
     *
     * @param mediaSession The media session.
     * @param controller The controller information.
     * @param mediaItems The list of media items to be added.
     * @return A ListenableFuture containing the list of media sources.
     */
    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: ControllerInfo,
        mediaItems: MutableList<MediaItem>
    ): ListenableFuture<List<MediaItem>> =
        Futures.immediateFuture(mediaItems.map(MediaItem::toMediaSource))

    // FIXME: The impact of this function on the application's behavior is unclear.
    // It returns a library result of a media item with the [BROWSER_ROOT_QUEUE] identifier and optional parameters.
    @SuppressLint("UnsafeOptInUsageError")
    override fun onGetLibraryRoot(
        session: MediaLibrarySession, browser: ControllerInfo, params: LibraryParams?
    ): ListenableFuture<LibraryResult<MediaItem>> =
        Futures.immediateFuture(LibraryResult.ofItem(BROWSER_ROOT_QUEUE, params))

    /**
     * Returns the individual media item identified by the given [mediaId].
     *
     * @param session The media library session.
     * @param browser The controller information.
     * @param mediaId The unique identifier of the media item to retrieve.
     * @return A ListenableFuture containing the library result of the specified media item.
     */
    override fun onGetItem(
        session: MediaLibrarySession, browser: ControllerInfo, mediaId: String
    ): ListenableFuture<LibraryResult<MediaItem>> {
        val item = player.mediaItems.find { it.mediaId == mediaId }
        val result = if (item == null) LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
        else LibraryResult.ofItem(item, /* params = */ null)
        return Futures.immediateFuture(result)
    }

    /**
     * TODO: Determine how to return the playing queue with upcoming items only.
     * This function is intended to handle subscriptions to media items under a parent.
     *
     * @param session The media library session.
     * @param browser The controller information.
     * @param parentId The unique identifier of the parent item.
     * @param params Optional library parameters.
     * @return A ListenableFuture containing the library result.
     */
    override fun onSubscribe(
        session: MediaLibrarySession,
        browser: ControllerInfo,
        parentId: String,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<Void>> {
        val children = when (parentId) {
            ROOT_QUEUE -> player.queue
            else -> return Futures.immediateFuture(
                LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
            )
        }
        session.notifyChildrenChanged(browser, parentId, children.size, params)
        return Futures.immediateFuture(LibraryResult.ofVoid())
    }

    /**
     * TODO: Consider adding support for paging to efficiently retrieve children.
     *
     * This function retrieves the children of a specified parent item for a given page and page size.
     *
     * @see MediaLibraryService.onGetChildren
     */
    override fun onGetChildren(
        session: MediaLibrarySession,
        browser: ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        val children = when (parentId) {
            ROOT_QUEUE -> player.queue
            else -> return Futures.immediateFuture(
                LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
            )
        }
        return Futures.immediateFuture(LibraryResult.ofItemList(children, params))
    }

    /**
     * Releases resources and cancels any associated scope before destroying the service.
     * @see Service.onDestroy
     */
    override fun onDestroy() {
        player.release()
        session.release()
        // Cancel the coroutine scope when the service is destroyed
        scope.cancel()
        super.onDestroy()
    }

    /**
     * Called when a media item transition occurs in the player.
     *
     * @param mediaItem The new media item being played.
     * @param reason The reason for the media item transition.
     */
    override fun onMediaItemTransition(
        mediaItem: MediaItem?,
        reason: Int
    ) {
        // save current index in preference
        preferences[PREF_KEY_INDEX] = player.currentMediaItemIndex

        // Add the media item to the "recent" playlist if the mediaItem is not null
        // and its media URI is not from a third-party source. Third-party content URIs
        // are unplayable after an app reboot.
        if (mediaItem != null && mediaItem.mediaUri?.isThirdPartyUri == false) {
            val limit = preferences.value(PREF_KEY_RECENT_PLAYLIST_LIMIT)
            scope.launch(Dispatchers.IO) { playlists.addToRecent(mediaItem, limit.toLong()) }
            session.notifyChildrenChanged(ROOT_QUEUE, 0, null)
        }
    }

    /**
     * Called when the shuffle mode is enabled or disabled in the player.
     *
     * @param shuffleModeEnabled True if shuffle mode is enabled, false otherwise.
     */
    override fun onShuffleModeEnabledChanged(
        shuffleModeEnabled: Boolean
    ) {
        preferences[PREF_KEY_SHUFFLE_MODE] = shuffleModeEnabled
        session.notifyChildrenChanged(ROOT_QUEUE, 0, null)
    }

    /**
     * Called when the repeat mode is changed in the player.
     *
     * @param repeatMode The new repeat mode (e.g., [Player.REPEAT_MODE_OFF], [Player.REPEAT_MODE_ONE], [Player.REPEAT_MODE_ALL]).
     */
    override fun onRepeatModeChanged(
        repeatMode: Int
    ) {
        preferences[PREF_KEY_REPEAT_MODE] = repeatMode
    }

    /**
     * Called when the player's timeline changes, indicating a change in the playlist.
     *
     * @param timeline The new timeline of media items.
     * @param reason The reason for the timeline change (e.g., [Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED]).
     */
    override fun onTimelineChanged(
        timeline: Timeline,
        reason: Int
    ) {
        // construct list and update.
        if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {

            //Obtain the media_items to be saved.
            val items = player.mediaItems

            // Save the media_items in the playlist on change in time line
            scope.launch(Dispatchers.IO) { playlists.save(items) }

            // Save the orders in preferences
            preferences[PREF_KEY_ORDERS] = player.orders
            session.notifyChildrenChanged(ROOT_QUEUE, 0, null)
        }
    }

    /**
     * Called when an update to the media session's notification is required, possibly to start it in foreground mode.
     *
     * @param session The media session.
     * @param startInForegroundRequired True if the notification should be started in foreground mode.
     */
    override fun onUpdateNotification(
        session: MediaSession,
        startInForegroundRequired: Boolean
    ) {
        super.onUpdateNotification(session, startInForegroundRequired)

        // Send an intent for updating the widget
        val intent = Intent(this, Widget::class.java)
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE)

        // Retrieve widget IDs
        val ids = AppWidgetManager.getInstance(application)
            .getAppWidgetIds(ComponentName(application, Widget::class.java))
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)

        // Broadcast the intent to update the widget
        sendBroadcast(intent)
    }

    /**
     * Called when a player error occurs.
     *
     * @param error The playback exception representing the error.
     */
    override fun onPlayerError(
        error: PlaybackException
    ) {
        // Display a simple toast message indicating an unplayable file
        Toast.makeText(this, getString(R.string.msg_unplayable_file), Toast.LENGTH_SHORT).show()

        // You may choose to handle the error here or take other actions like seeking to the next media item
        player.seekToNextMediaItem()
    }

    override fun onIsPlayingChanged(
        isPlaying: Boolean
    ) {
        super.onIsPlayingChanged(isPlaying)
        //  Just cancel the job in case is playing false
        if (!isPlaying) {
            playbackMonitorJob?.cancel()
            // change this to uninitialized
            scheduledPauseTimeMillis = UNINITIALIZED_SLEEP_TIME_MILLIS
        }
        // Launch a new job
        else playbackMonitorJob = scope.launch {
            var isPlaying = player.isPlaying
            do {
                // Save the current playback position to preferences
                preferences[PREF_KEY_BOOKMARK] = player.currentPosition
                Log.i(TAG, "Saved playback position: ${player.currentPosition}")

                // Check if playback is scheduled to be paused.
                if (scheduledPauseTimeMillis != UNINITIALIZED_SLEEP_TIME_MILLIS && scheduledPauseTimeMillis <= System.currentTimeMillis()) {
                    // Pause the player as the scheduled pause time has been reached.
                    player.pause()

                    // Once the scheduled pause has been triggered, reset the scheduled time to uninitialized.
                    scheduledPauseTimeMillis = UNINITIALIZED_SLEEP_TIME_MILLIS
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
    override fun onConnect(
        session: MediaSession,
        controller: ControllerInfo
    ): ConnectionResult {
        // obtain the available commands
        // TODO: Maybe add more buttons to notifications.
        val available = ConnectionResult.DEFAULT_SESSION_AND_LIBRARY_COMMANDS.buildUpon()

        // add commands to session.
        available.add(Command(ACTION_AUDIO_SESSION_ID, Bundle.EMPTY))
        available.add(Command(ACTION_SCHEDULE_SLEEP_TIME, Bundle.EMPTY))

        // return the result.
        return ConnectionResult.AcceptedResultBuilder(session)
            .setAvailableSessionCommands(available.build())
            .build()
    }

    @androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
    override fun onCustomCommand(
        session: MediaSession,
        controller: ControllerInfo,
        command: Command,
        args: Bundle
    ): ListenableFuture<Result> {
        val action = command.customAction
        return when (action) {
            // Handle the command to retrieve the audio session ID from the player.
            ACTION_AUDIO_SESSION_ID -> {
                // Retrieve the audio session ID from the ExoPlayer.
                val audioSessionId = (player as ExoPlayer).audioSessionId

                // Create a result with the audio session ID and return it immediately.
                val result = Result(Result.RESULT_SUCCESS) {
                    putInt(EXTRA_AUDIO_SESSION_ID, audioSessionId)
                }

                // Return the result with the audio session ID.
                Futures.immediateFuture(result)
            }

            // Handle sleep timer commands
            ACTION_SCHEDULE_SLEEP_TIME -> {
                // Retrieve the scheduled time in milliseconds from the command's custom extras
                val newTimeMills = command.customExtras.getLong(EXTRA_SCHEDULED_TIME_MILLS)
                // If the new time is 0, it means the client wants to retrieve the sleep timer.
                // If the new time is not zero, set the new sleep timer.
                // FixMe: Consider setting the timer only when it's not equal to the default value
                //  and greater than the current time.
                if (newTimeMills != 0L)
                    scheduledPauseTimeMillis = newTimeMills

                // Regardless of whether the client wants to set or retrieve the timer,
                // include the current or updated timer value in the response to the client.
                Futures.immediateFuture(
                    Result(Result.RESULT_SUCCESS) {
                        putLong(
                            EXTRA_SCHEDULED_TIME_MILLS,
                            scheduledPauseTimeMillis
                        )
                    }
                )
            }

            // Handle unrecognized or unsupported commands.
            else -> Futures.immediateFuture(Result(Result.RESULT_ERROR_UNKNOWN))
        }
    }


    companion object {
        /**
         * @see [com.prime.media.core.playback.PLAYLIST_FAVOURITE]
         */
        @JvmField
        val PLAYLIST_FAVOURITE = com.prime.media.core.playback.PLAYLIST_FAVOURITE

        /**
         * @see [com.prime.media.core.playback.PLAYLIST_RECENT]
         */
        @JvmField
        val PLAYLIST_RECENT = com.prime.media.core.playback.PLAYLIST_RECENT

        /**
         * @see [com.prime.media.core.playback.PLAYLIST_QUEUE]
         */
        @JvmField
        val PLAYLIST_QUEUE = com.prime.media.core.playback.PLAYLIST_QUEUE

        /**
         * @see [com.prime.media.core.playback.ROOT_QUEUE]
         */
        const val ROOT_QUEUE = com.prime.media.core.playback.ROOT_QUEUE

        /**
         * @see [com.prime.media.core.playback.PREF_KEY_RECENT_PLAYLIST_LIMIT]
         */
        @JvmField
        val PREF_KEY_RECENT_PLAYLIST_LIMIT =
            com.prime.media.core.playback.PREF_KEY_RECENT_PLAYLIST_LIMIT

        /**
         * @see com.prime.media.core.playback.ACTION_AUDIO_SESSION_ID
         */
        const val ACTION_AUDIO_SESSION_ID = com.prime.media.core.playback.ACTION_AUDIO_SESSION_ID

        /**
         * @see com.prime.media.core.playback.EXTRA_AUDIO_SESSION_ID
         */
        const val EXTRA_AUDIO_SESSION_ID = com.prime.media.core.playback.EXTRA_AUDIO_SESSION_ID

        /**
         * @see com.prime.media.core.playback.ACTION_SCHEDULE_SLEEP_TIME
         */
        const val ACTION_SCHEDULE_SLEEP_TIME =
            com.prime.media.core.playback.ACTION_SCHEDULE_SLEEP_TIME

        /**
         * @see com.prime.media.core.playback.EXTRA_SCHEDULED_TIME_MILLS
         */
        const val EXTRA_SCHEDULED_TIME_MILLS =
            com.prime.media.core.playback.EXTRA_SCHEDULED_TIME_MILLS

        /**
         * @see com.prime.media.core.playback.UNINITIALIZED_SLEEP_TIME_MILLIS
         */
        const val UNINITIALIZED_SLEEP_TIME_MILLIS =
            com.prime.media.core.playback.UNINITIALIZED_SLEEP_TIME_MILLIS
    }
}
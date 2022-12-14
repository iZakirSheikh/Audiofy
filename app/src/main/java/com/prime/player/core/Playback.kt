package com.prime.player.core

import android.annotation.SuppressLint
import android.app.*
import android.app.PendingIntent.FLAG_IMMUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Intent
import android.graphics.Typeface
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.style.StyleSpan
import android.util.Log
import android.widget.Toast
import androidx.media3.common.*
import androidx.media3.common.C.AUDIO_CONTENT_TYPE_MUSIC
import androidx.media3.common.util.Util
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ShuffleOrder
import androidx.media3.exoplayer.source.ShuffleOrder.DefaultShuffleOrder
import androidx.media3.session.*
import androidx.media3.session.MediaLibraryService.MediaLibrarySession.Callback
import androidx.media3.session.MediaSession.ControllerInfo
import com.google.common.collect.ImmutableList
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.prime.player.MainActivity
import com.prime.player.R
import kotlinx.coroutines.*

private const val TAG = "Playback"

/* Do nothing. */
private fun ListenableFuture<SessionResult>.ignore() = Unit

/** The action which starts playback.  */
private const val ACTION_PLAY = "com.prime.player.play"

/** The action which pauses playback.  */
private const val ACTION_PAUSE = "com.prime.player.pause"

/** The action which skips to the previous window.  */
private const val ACTION_PREVIOUS = "com.prime.player.prev"

/** The action which skips to the next window.  */
private const val ACTION_NEXT = "com.prime.player.next"

/** The action which stops playback.  */
private const val ACTION_STOP = "com.prime.player.stop"

/**
 * Returns all the [MediaItem]s of [Player]
 */
private inline val Player.mediaItems
    get() = List(this.mediaItemCount) {
        getMediaItemAt(it)
    }

/**
 * Ensures the notification channel exists, if not creates it.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
private fun NotificationManager.ensureNotificationChannel(
    channelId: String,
    channelName: CharSequence
) {
    if (Util.SDK_INT < 26 || getNotificationChannel(channelId) != null) return
    // Need to create a notification channel.
    val channel = NotificationChannel(
        channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT
    ).also {
        it.setShowBadge(false)
        it.setSound(null, null)
    }
    createNotificationChannel(channel)
}

/**
 * The Playback Service class using media3.
 */
class Playback : MediaLibraryService(), Callback {

    companion object {
        /**
         * The lookup key for the browser service that indicates the kind of root to return.
         *
         * When creating a media browser for a given media browser service, this key can be
         * supplied as a root hint for retrieving media items.
         *
         * @see [EXTRA_RECENT]
         * @see [ROOT_PLAYLIST]
         */
        val EXTRA_ROOT = "com.prime.player.core.root"

        /**
         * The root for queued [MediaItem]s.
         * @see [ROOT_RECENT]
         */
        val ROOT_PLAYLIST = "com.prime.player.core.PLAYLIST"

        /**
         * The root key for recently played [MediaItem]s.
         * @see [ROOT_PLAYLIST]
         */
        val ROOT_RECENT = "com.prime.player.core.RECENT"

        /**
         * Returns a spannable string representation of [value]
         */
        fun Title(value: CharSequence): CharSequence =
            SpannableStringBuilder(value).apply {
                setSpan(StyleSpan(Typeface.BOLD), 0, value.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            }
    }

    // This helps in implement the state of this service using persistent storage .
    private val storage by lazy { Storage(this) }

    /**
     * The pending intent for the underlying activity.
     */
    private val activity by lazy {
        // create the activity intent and set with session
        TaskStackBuilder.create(this).run {
            addNextIntent(Intent(this@Playback, MainActivity::class.java))
            val immutableFlag = if (Build.VERSION.SDK_INT >= 23) FLAG_IMMUTABLE else 0
            getPendingIntent(0, immutableFlag or FLAG_UPDATE_CURRENT)
        }
    }

    private lateinit var player: Player
    private lateinit var session: MediaLibrarySession

    /**
     * A listener for [Player] must be set after [onRestoreState]
     */
    private val listener =
        object : Player.Listener {
            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                // save current index in preference
                storage.index = player.currentMediaItemIndex
                if (mediaItem != null) {
                    storage.addToRecent(mediaItem)
                    session.notifyChildrenChanged(ROOT_RECENT, storage.recent.size, null)
                }
            }

            override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
                storage.shuffle = shuffleModeEnabled
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
                storage.repeatMode = repeatMode
            }

            override fun onTimelineChanged(timeline: Timeline, reason: Int) {
                // construct list and update.
                if (reason == Player.TIMELINE_CHANGE_REASON_PLAYLIST_CHANGED) {
                    storage.list = player.mediaItems
                    session.notifyChildrenChanged(ROOT_PLAYLIST, player.mediaItemCount, null)
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                // make a simple toast
                Toast.makeText(
                    this@Playback,
                    getString(R.string.unplayable_file),
                    Toast.LENGTH_SHORT
                ).show()
                //player.seekToNextMediaItem()
            }
        }

    // init all the objects and restore the state.
    override fun onCreate() {
        super.onCreate()
        // init player
        player =
            ExoPlayer
                .Builder(this)
                .setAudioAttributes(

                    // set audio attributes to it
                    AudioAttributes
                        .Builder()
                        .setContentType(AUDIO_CONTENT_TYPE_MUSIC)
                        .setUsage(C.USAGE_MEDIA)
                        .build(),
                    true
                )
                .setHandleAudioBecomingNoisy(true)
                .build()

        // init session and add callback, player etc.
        session =
            MediaLibrarySession.Builder(this, player, this)
                .setSessionActivity(activity)
                .build()

        onRestoreSavedState()
        player.addListener(listener)
    }

    /**
     * Restore the saved state of this service.
     */
    @SuppressLint("UnsafeOptInUsageError")
    private fun onRestoreSavedState() {
        with(player) {
            shuffleModeEnabled = storage.shuffle
            repeatMode = storage.repeatMode
            setMediaItems(storage.list)
            // seek to current position
            val index = storage.index
            if (index != C.INDEX_UNSET)
                seekTo(index, storage.bookmark)
        }
    }

    // here return the session
    override fun onGetSession(controllerInfo: ControllerInfo) = session

    // return the items to add to player
    override fun onAddMediaItems(
        mediaSession: MediaSession,
        controller: ControllerInfo,
        mediaItems: MutableList<MediaItem>
    ): ListenableFuture<List<MediaItem>> {
        return Futures.immediateFuture(
            mediaItems.map { item ->
                item.buildUpon()
                    .setUri(item.requestMetadata.mediaUri)
                    .setMediaMetadata(
                        item.mediaMetadata
                            .buildUpon()
                            .setTitle(Title(item.mediaMetadata.title ?: "Unknown"))
                            // since user is only going to pass the title and subtitle.
                            // but the notification displays the title and the artist; hence this.
                            .setArtist(item.mediaMetadata.subtitle)
                            .build()
                    )
                    .build()
            }
        )
    }



    // FixMe: Don't currently know how this affects.
    @SuppressLint("UnsafeOptInUsageError")
    override fun onGetLibraryRoot(
        session: MediaLibrarySession,
        browser: ControllerInfo,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<MediaItem>> {
        val key = params?.extras?.getString(EXTRA_ROOT)
            ?: throw IllegalAccessError("No $EXTRA_ROOT provided")
        val root = when (key) {
            ROOT_PLAYLIST -> MediaItem.Builder().setMediaId(ROOT_PLAYLIST)
            ROOT_RECENT -> MediaItem.Builder().setMediaId(ROOT_RECENT)
            else -> throw java.lang.IllegalArgumentException("No such root exists: $key")
        }.build()
        return Futures.immediateFuture(LibraryResult.ofItem(root, params))
    }


    // return the individual media item pointed out by the [mediaId]
    override fun onGetItem(
        session: MediaLibrarySession,
        browser: ControllerInfo,
        mediaId: String
    ): ListenableFuture<LibraryResult<MediaItem>> {
        val item = player.mediaItems.find { it.mediaId == mediaId }

        val result = if (item == null)
            LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
        else
            LibraryResult.ofItem(item, /* params = */ null)
        return Futures.immediateFuture(result)
    }

    //TODO: Find how can i return he playing queue with upcoming items only.
    override fun onSubscribe(
        session: MediaLibrarySession,
        browser: ControllerInfo,
        parentId: String,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<Void>> {
        val children = when (parentId) {
            ROOT_PLAYLIST -> player.mediaItems
            ROOT_RECENT -> storage.recent
            else -> return Futures.immediateFuture(
                LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
            )
        }
        session.notifyChildrenChanged(browser, parentId, children.size, params)
        return Futures.immediateFuture(LibraryResult.ofVoid())
    }


    override fun onGetChildren(
        session: MediaLibrarySession,
        browser: ControllerInfo,
        parentId: String,
        page: Int,
        pageSize: Int,
        params: LibraryParams?
    ): ListenableFuture<LibraryResult<ImmutableList<MediaItem>>> {
        //TODO(Maybe add support for paging.)
        val children = when (parentId) {
            ROOT_PLAYLIST -> player.mediaItems
            ROOT_RECENT -> storage.recent
            else -> return Futures.immediateFuture(
                LibraryResult.ofError(LibraryResult.RESULT_ERROR_BAD_VALUE)
            )
        }
        return Futures.immediateFuture(LibraryResult.ofItemList(children, params))
    }

    // release resources
    // cancel scope etc.
    override fun onDestroy() {
        player.release()
        session.release()
        super.onDestroy()
    }
}
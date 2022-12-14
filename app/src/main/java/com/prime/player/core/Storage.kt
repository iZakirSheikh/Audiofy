package com.prime.player.core

import android.content.Context
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import org.json.JSONObject

/**
 * This is a helper class that helps in saving the state of the [Playback]
 */
internal interface Storage {
    /**
     * Adds the [MediaItem] to [Playlist] recent.
     */
    fun addToRecent(mediaItem: MediaItem)

    /**
     * The items of the recent playlist.
     */
    val recent: List<MediaItem>

    /**
     * The items in the queue of the [Player]
     */
    var list: List<MediaItem>

    /**
     * @see [Player.setShuffleModeEnabled]
     */
    var shuffle: Boolean

    /**
     * @see [Player.RepeatMode]
     */
    var repeatMode: Int
        @Player.RepeatMode get


    /**
     * The current index of the [Player]
     */
    var index: Int

    /**
     * The current bookmark of the [Player]
     */
    var bookmark: Long
}

/*Different keys for saving the state*/
private const val PREFERENCES_NAME = "media_service_state.db"
private const val PREF_KEY_PLAYLIST = "_playlist"
private const val PREF_KEY_RECENT_LIST = "_recent"
private const val PREF_KEY_SHUFFLE_MODE = "_shuffle"
private const val PREF_KEY_REPEAT_MODE = "_repeat_mode"
private const val PREF_KEY_INDEX = "_index"
private const val PREF_KEY_BOOKMARK = "_bookmark"

/**
 * Only these fields are saved in the playlist.
 */
private const val KEY_ID = "_id" // the id of the MediaItem
private const val KEY_URI = "_uri" // The media Uri of the MediaItem
private const val KEY_TITLE = "_title" // The title of the MediaItem.
private const val KEY_ARTWORK = "_artwork" // The artwork of the [MediaItem]
private const val KEY_SUBTITLE =
    "_subtitle" // The subtitle of the MediaItem // init artist with it.

/**
 * Constructs a [MediaItem]
 */
private fun MediaItem(
    uri: Uri,
    id: String = MediaItem.DEFAULT_MEDIA_ID,
    artwork: Uri? = null,
    subtitle: CharSequence? = null,
    title: CharSequence? = null
) = MediaItem.Builder().setMediaId(id)
    // as this is going to be passed to MediaBrowserService and hence can't be initiated
    .setUri(uri).setRequestMetadata(
        MediaItem.RequestMetadata.Builder().setMediaUri(uri).build()
    ).setMediaMetadata(
        MediaMetadata.Builder().setFolderType(MediaMetadata.FOLDER_TYPE_NONE).setIsPlayable(true)
            .setTitle(title).setArtist(subtitle).setArtworkUri(artwork)
            .setSubtitle(subtitle).build()
    ).build()


private val MediaItem.toJson: String
    get() {
        return JSONObject().apply {
            put(KEY_ID, mediaId)
            put(KEY_URI, requestMetadata.mediaUri)
            put(KEY_TITLE, mediaMetadata.title)
            put(KEY_ARTWORK, mediaMetadata.artworkUri)
            put(KEY_SUBTITLE, mediaMetadata.subtitle)
        }.toString()
    }

/**
 * Catches error and returns null in that case.
 */
private inline fun <R> runCatching(block: () -> R): R? {
    return try {
        block()
    } catch (e: Throwable) {
        null
    }
}

private inline fun JSONObject.getStringOrNull(key: String): String? =
    com.prime.player.core.runCatching {
        getString(key)
    }

private inline fun JSONObject.getIntOrNull(key: String): Int? = com.prime.player.core.runCatching {
    getInt(key)
}

/**
 * The no. of items that are allowed in recent.
 */
private const val RECENT_LIMIT = 100

/**
 * Creates [MediaItem] from [JSON] [source] String.
 */
private fun MediaItem(source: String): MediaItem = JSONObject(source).run {
    MediaItem(
        uri = Uri.parse(getString(KEY_URI)),
        id = getString(KEY_ID),
        artwork = getStringOrNull(KEY_ARTWORK)?.run { Uri.parse(this) },
        subtitle = getStringOrNull(KEY_SUBTITLE),
        title = (getStringOrNull(KEY_TITLE) ?: "Unknown").let { Playback.Title(it) },
    )
}

internal fun Storage(context: Context) =
    object : Storage {

        /**
         * Store any data which must persist between restarts, such as the most recently played song.
         */
        private val preferences by lazy {
            context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)
        }

        override var list: List<MediaItem>
            get() = preferences.getStringSet(PREF_KEY_PLAYLIST, null).let { set ->
                if (set.isNullOrEmpty()) emptyList()
                else set.map { MediaItem(it) }
            }
            set(value) {
                val set = value.map { it.toJson }.toSet()
                preferences.edit().putStringSet(PREF_KEY_PLAYLIST, set).apply()
            }


        override fun addToRecent(mediaItem: MediaItem) {
            val oldIndex =
                cache.indexOfFirst { it.requestMetadata.mediaUri == mediaItem.requestMetadata.mediaUri }
            if (oldIndex != -1) cache.removeAt(oldIndex)
            cache.add(mediaItem)
            while (cache.size > RECENT_LIMIT) cache.removeAt(0)
            preferences.edit()
                .putStringSet(PREF_KEY_RECENT_LIST, cache.map { it.toJson }.toSet())
                .apply()
        }

        override var shuffle: Boolean = preferences.getBoolean(PREF_KEY_SHUFFLE_MODE, false)
            set(value) {
                field = value
                preferences.edit().putBoolean(PREF_KEY_SHUFFLE_MODE, value).apply()
            }

        private val cache by lazy {
            preferences.getStringSet(PREF_KEY_RECENT_LIST, emptySet())!!.map { MediaItem(it) }
                .toMutableList()
        }

        override val recent: List<MediaItem>
            get() = cache

        override var repeatMode: Int
            get() = preferences.getInt(PREF_KEY_REPEAT_MODE, Player.REPEAT_MODE_OFF)
            set(value) {
                preferences.edit().putInt(PREF_KEY_REPEAT_MODE, value).apply()
            }
        override var index: Int
            get() = preferences.getInt(PREF_KEY_INDEX, C.INDEX_UNSET)
            set(value) {
                preferences.edit().putInt(PREF_KEY_INDEX, value).apply()
            }
        override var bookmark: Long
            get() = preferences.getLong(PREF_KEY_BOOKMARK, C.TIME_UNSET)
            set(value) {
                preferences.edit().putLong(PREF_KEY_BOOKMARK, value).apply()
            }
    }
package com.prime.player.core

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import com.primex.preferences.*
import org.json.JSONObject

/**
 * A Utility interface for saving the state of [Playback] persistently.
 */
interface Storage {
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

    /**
     * A shuffled array of positions of [list]
     */
    var shuffled: IntArray
}

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

/*Different keys for saving the state*/
private val KEY_PLAYLIST = stringSetPreferenceKey("_playlist", emptySet())
private val PREF_KEY_RECENT_LIST = stringSetPreferenceKey("_recent", emptySet())
private val PREF_KEY_SHUFFLE_MODE = booleanPreferenceKey("_shuffle", false)
private val PREF_KEY_REPEAT_MODE = intPreferenceKey("_repeat_mode", Player.REPEAT_MODE_OFF)
private val PREF_KEY_INDEX = intPreferenceKey("_index", C.INDEX_UNSET)
private val PREF_KEY_BOOKMARK = longPreferenceKey("_bookmark", C.TIME_UNSET)
private val PREF_KEY_SHUFFLED = stringSetPreferenceKey("_shuffled", emptySet())

fun Storage(preferences: Preferences) =
    object : Storage {

        override var list: List<MediaItem>
            get() = preferences.value(KEY_PLAYLIST).map { MediaItem(it) }
            set(value) {
                preferences[KEY_PLAYLIST] = value.map { it.toJson }.toSet()
            }


        val cache by lazy {
            preferences.value(PREF_KEY_RECENT_LIST).map { MediaItem(it) }
                .toMutableList()
        }

        override val recent: List<MediaItem>
            get() = cache

        override var repeatMode: Int
            get() = preferences.value(PREF_KEY_REPEAT_MODE)
            set(value) {
                preferences[PREF_KEY_REPEAT_MODE] = value
            }

        override var index: Int
            get() = preferences.value(PREF_KEY_INDEX)
            set(value) {
                preferences[PREF_KEY_INDEX] = value
            }

        override var shuffle: Boolean
            get() = preferences.value(PREF_KEY_SHUFFLE_MODE)
            set(value) {
                preferences[PREF_KEY_SHUFFLE_MODE] = value
            }

        override var bookmark: Long
            get() = preferences.value(PREF_KEY_BOOKMARK)
            set(value) {
                preferences[PREF_KEY_BOOKMARK] = value
            }

        override var shuffled: IntArray
            get() = preferences.value(PREF_KEY_SHUFFLED).map { it.toInt() }.toIntArray()
            set(value) {
                preferences[PREF_KEY_SHUFFLED] = value.map { "$it" }.toSet()
            }

        override fun addToRecent(mediaItem: MediaItem) {
            val oldIndex =
                cache.indexOfFirst { it.requestMetadata.mediaUri == mediaItem.requestMetadata.mediaUri }
            if (oldIndex != -1) cache.removeAt(oldIndex)
            cache.add(mediaItem)
            while (cache.size > RECENT_LIMIT) cache.removeAt(0)
            preferences[PREF_KEY_RECENT_LIST] = cache.map { it.toJson }.toSet()
        }
    }

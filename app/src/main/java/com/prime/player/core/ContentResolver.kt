package com.prime.player.core

import android.content.ContentResolver
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.prime.player.common.FileUtils
import com.prime.player.common.name
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext as using

private const val TAG = "ContentResolver2"


/**
 * A kotlin coroutine extension for [ContentResolver]
 * @see ContentResolver.query
 */
suspend fun ContentResolver.query2(
    uri: Uri,
    projection: Array<String>? = null,
    selection: String? = null,
    args: Array<String>? = null,
    order: String? = null
): Cursor? {
    return using(Dispatchers.Default) {
        query(uri, projection, selection, args, order)
    }
}


/**
 * An advance [query2] method that returns a closable cursor in [transform]
 * @see query2
 * @see ContentResolver.query
 */
suspend inline fun <T> ContentResolver.query2(
    uri: Uri,
    projection: Array<String>? = null,
    selection: String? = null,
    args: Array<String>? = null,
    order: String? = null,
    transform: (Cursor) -> T
): T? {
    return query2(uri, projection, selection, args, order)?.use {
        transform(it)
    }
}


@Stable
data class Audio(
    @JvmField val id: Long,
    @JvmField val name: String,
    @JvmField val albumId: Long,
    @JvmField val data: String,
    @JvmField val album: String,
    @JvmField val artist: String,
    @JvmField val composer: String,
    @JvmField val mimeType: String,
    @JvmField val track: Int,
    @JvmField val dateAdded: Long,
    @JvmField val dateModified: Long,
    @JvmField val duration: Int,
    @JvmField val size: Long,
    @JvmField val year: Int
)

/**
 * Maps [Cursor] at current position to Audio.
 */
private inline val Cursor.toAudio
    get() = Audio(
        id = getLong(0),
        name = getString(1),
        albumId = getLong(4),
        data = getString(8),
        album = getString(3),
        artist = getString(2),
        composer = getString(6),
        mimeType = getString(10),
        track = getInt(11),
        dateAdded = getLong(5),
        dateModified = getLong(13),
        duration = getInt(9),
        size = getLong(12),
        year = getInt(7)
    )


private val AUDIO_PROJECTION
    get() = arrayOf(
        MediaStore.Audio.Media._ID, //0
        replaceIfNull(MediaStore.Audio.Media.TITLE, MediaStore.UNKNOWN_STRING), // 1
        replaceIfNull(MediaStore.Audio.Media.ARTIST, MediaStore.UNKNOWN_STRING), // 2
        replaceIfNull(MediaStore.Audio.Media.ALBUM, MediaStore.UNKNOWN_STRING), // 3
        MediaStore.Audio.Media.ALBUM_ID, // 4
        "${MediaStore.Audio.Media.DATE_ADDED} * 1000",  //5
        replaceIfNull(MediaStore.Audio.Media.COMPOSER, MediaStore.UNKNOWN_STRING), // , // 6
        MediaStore.Audio.Media.YEAR, // 7
        MediaStore.Audio.Media.DATA, // 8
        MediaStore.Audio.Media.DURATION, // 9
        MediaStore.Audio.Media.MIME_TYPE, // 10
        MediaStore.Audio.Media.TRACK, // 11
        MediaStore.Audio.Media.SIZE, //12
        "${MediaStore.Audio.Media.DATE_MODIFIED} * 1000", // 14
    )


//language=SQL
private const val DEFAULT_AUDIO_SELECTION = "${MediaStore.Audio.Media.IS_MUSIC} != 0"

//language=SQL
private const val DEFAULT_AUDIO_ORDER =
    "${MediaStore.Audio.Media.DATE_ADDED} DESC, ${MediaStore.Audio.Media.TITLE} ASC"

/**
 * replaces column string field to [value] if equal to null
 */
//language=SQL
private inline fun replaceIfNull(field: String, value: String = MediaStore.UNKNOWN_STRING) =
    "CASE WHEN $field IS NULL THEN '$value' ELSE $field END"


/**
 *
 */
@kotlin.jvm.Throws(IllegalStateException::class)
suspend fun ContentResolver.getAudios(
    filter: String? = null,
    order: String = DEFAULT_AUDIO_ORDER,
    offset: Int = 0,
    limit: Int = Int.MAX_VALUE
): List<Audio> {
    return query2(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection = AUDIO_PROJECTION,
        selection = DEFAULT_AUDIO_SELECTION + if (filter != null) " AND ${MediaStore.Audio.Media.TITLE} LIKE ?" else "",
        if (filter != null) arrayOf("%$filter%") else null,
        "$order LIMIT $limit OFFSET $offset",
        transform = { c ->
            List(c.count) {
                c.moveToPosition(it);
                c.toAudio
            }
        },
    ) ?: throw IllegalStateException("$TAG can't access cursor!")
}


data class Folder(
    //@JvmField val albumId: Long,
    @JvmField val path: String,
    @JvmField val cardinality: Int,
    @JvmField val size: Long, // size in bytes
    @JvmField val dateModified: Long,
    @JvmField val duration: Int
)

//language=SQL
private val FOLDER_PROJECTION
    get() = arrayOf(
        COLUMN_BUCKET_PATH, // 0
        "COUNT(*)", // 1,
        "SUM(${MediaStore.Audio.Media.SIZE})", // 2
        "MAX(${MediaStore.Audio.Media.DATE_MODIFIED}) * 1000", // 3
        "SUM(${MediaStore.Audio.Media.DURATION})", // 4
    )


private inline val Cursor.toFolder
    get() = Folder(
        path = getString(0),
        cardinality = getInt(1),
        size = getLong(2),
        duration = getInt(4),
        dateModified = getLong(3)
    )


val Folder.name: String
    get() = FileUtils.name(path)

/**
 * Strips display name from [MediaStore.Audio.Media.DATA] -> which now points to folder.
 */
//language=SQL
private const val COLUMN_BUCKET_PATH =
    "TRIM(TRIM(${MediaStore.Audio.Media.DATA}, ${MediaStore.Audio.Media.DISPLAY_NAME}), '/')"

private const val DUMMY_SELECTION = "${MediaStore.Audio.Media._ID} != 0"

//language=SQL
private const val DEFAULT_FOLDERS_ORDER = "${COLUMN_BUCKET_PATH} ASC"

@kotlin.jvm.Throws(IllegalStateException::class)
suspend fun ContentResolver.getFolders(
    filter: String? = null,
    order: String = DEFAULT_FOLDERS_ORDER,
    offset: Int = 0,
    limit: Int = Int.MAX_VALUE
): List<Folder> {

    val like = if (filter == null) DUMMY_SELECTION else "$COLUMN_BUCKET_PATH LIKE ?"
    val selection = "$like) GROUP BY ($COLUMN_BUCKET_PATH"

    return query2(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        FOLDER_PROJECTION,
        selection,
        if (filter == null) null else arrayOf("%$filter%"),
        "$order LIMIT $limit OFFSET $offset",
        transform = { c ->
            List(c.count) {
                c.moveToPosition(it)
                c.toFolder
            }
        }
    ) ?: throw IllegalStateException("$TAG can't access cursor!")
}


@Stable
data class Artist(
    @JvmField val name: String,
    @JvmField val tracks: Int,
    @JvmField val albums: Int,
    @JvmField val size: Long,
    @JvmField val duration: Int,
)

private val ARTIST_PROJECTION
    get() = arrayOf(
        replaceIfNull(MediaStore.Audio.Media.ARTIST),//0
        "COUNT(*)", // 1,
        "SUM(${MediaStore.Audio.Media.SIZE})", // 2
        "SUM(${MediaStore.Audio.Media.DURATION})", // 3
        "COUNT(DISTINCT ${replaceIfNull(MediaStore.Audio.Media.ALBUM)})", //4
    )

private inline val Cursor.toArtist
    get() = Artist(
        name = getString(0),
        tracks = getInt(1),
        albums = getInt(4),
        size = getLong(2),
        duration = getInt(3)
    )

private const val DEFAULT_ARTISTS_ORDER = "${MediaStore.Audio.Artists.ARTIST} ASC"

@kotlin.jvm.Throws(IllegalStateException::class)
suspend fun ContentResolver.getArtists(
    filter: String? = null,
    order: String = DEFAULT_ARTISTS_ORDER,
    offset: Int = 0,
    limit: Int = Int.MAX_VALUE
): List<Artist> {
    val like =
        if (filter == null) DUMMY_SELECTION else "${replaceIfNull(MediaStore.Audio.Media.ARTIST)} LIKE ?"
    val selection = "$like) GROUP BY (${replaceIfNull(MediaStore.Audio.Media.ARTIST)}"
    return query2(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection = ARTIST_PROJECTION,
        selection = selection,
        if (filter != null) arrayOf("%$filter%") else null,
        "$order LIMIT $limit OFFSET $offset",
        transform = { c ->
            List(c.count) {
                c.moveToPosition(it);
                c.toArtist
            }
        },
    ) ?: throw IllegalStateException("$TAG can't access cursor!")
}


data class Album(
    @JvmField val id: Long,
    @JvmField val title: String,
    @JvmField val firstYear: Int,
    @JvmField val lastYear: Int,
    @JvmField val size: Long,
    @JvmField val duration: Int,
    @JvmField val tracks: Int,
)

private val ALBUM_PROJECTION
    get() = arrayOf(
        MediaStore.Audio.Media.ALBUM_ID, //0
        replaceIfNull(MediaStore.Audio.Media.ALBUM),//1
        "MIN(${MediaStore.Audio.Media.YEAR})", //2
        "MAX(${MediaStore.Audio.Media.YEAR})", //3
        "COUNT(*)", // 4,
        "SUM(${MediaStore.Audio.Media.SIZE})", // 5
        "SUM(${MediaStore.Audio.Media.DURATION})", // 6
    )

private inline val Cursor.toAlbum
    get() = Album(
        id = getLong(0),
        title = getString(1),
        firstYear = getInt(2),
        lastYear = getInt(3),
        size = getLong(5),
        duration = getInt(6),
        tracks = getInt(4)
    )

//language=SQL
private val DEFAULT_ALBUMS_ORDER = "${MediaStore.Audio.Albums.ALBUM} ASC"

@kotlin.jvm.Throws(IllegalStateException::class)
suspend fun ContentResolver.getAlbums(
    filter: String? = null,
    order: String = DEFAULT_ALBUMS_ORDER,
    offset: Int = 0,
    limit: Int = Int.MAX_VALUE
): List<Album> {
    val like =
        if (filter == null) DUMMY_SELECTION else "${replaceIfNull(MediaStore.Audio.Media.ALBUM)} LIKE ?"
    val selection = "$like) GROUP BY (${replaceIfNull(MediaStore.Audio.Media.ALBUM)}"
    return query2(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection = ALBUM_PROJECTION,
        selection = selection,
        if (filter != null) arrayOf("%$filter%") else null,
        "$order LIMIT $limit OFFSET $offset",
        transform = { c ->
            List(c.count) {
                c.moveToPosition(it);
                c.toAlbum
            }
        },
    ) ?: throw IllegalStateException("$TAG can't access cursor!")
}

data class Genre(
    @JvmField val name: String,
    @JvmField val tracks: Int,
    @JvmField val size: Long,
    @JvmField val duration: Int,
)

private val GENRE_PROJECTION
    get() = arrayOf(
        replaceIfNull(MediaStore.Audio.Genres.NAME),
        MediaStore.Audio.Genres._COUNT,
    )

private inline val Cursor.toGenre
    get() = Genre(
        getString(0),
        getInt(1),
        -1,
        -1
    )

private const val DEFAULT_GENRE_ORDER = "${MediaStore.Audio.Genres.DEFAULT_SORT_ORDER} ASC"

@kotlin.jvm.Throws(IllegalStateException::class)
suspend fun ContentResolver.getGenres(
    filter: String? = null,
    order: String = DEFAULT_ALBUMS_ORDER,
    offset: Int = 0,
    limit: Int = Int.MAX_VALUE
): List<Genre> {
    val like =
        if (filter == null) DUMMY_SELECTION else "${replaceIfNull(MediaStore.Audio.Genres.NAME)} LIKE ?"
    val selection = "$like) GROUP BY (${replaceIfNull(MediaStore.Audio.Genres.NAME)}"
    return query2(
        MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
        projection = GENRE_PROJECTION,
        selection = selection,
        if (filter != null) arrayOf("%$filter%") else null,
        "$order LIMIT $limit OFFSET $offset",
        transform = { c ->
            List(c.count) {
                c.moveToPosition(it);
                c.toGenre
            }
        },
    ) ?: throw IllegalStateException("$TAG can't access cursor!")

}

/**
 * Register [ContentObserver] for change in [uri]
 */
inline fun ContentResolver.register(uri: Uri, crossinline onChanged: () -> Unit): ContentObserver {
    val observer = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) {
            onChanged()
        }
    }
    registerContentObserver(uri, false, observer)
    return observer
}

/**
 * Register an observer class that gets callbacks when data identified by a given content URI
 * changes.
 */
fun ContentResolver.observe(uri: Uri) = callbackFlow {
    val observer = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) {
            trySend(selfChange)
        }
    }
    registerContentObserver(uri, false, observer)
    // trigger first.
    trySend(false)
    awaitClose {
        unregisterContentObserver(observer)
    }
}

fun ContentResolver.audios(
    filter: String? = null,
    order: String = DEFAULT_AUDIO_ORDER,
) = observe(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI).map {
    getAudios(filter, order)
}

fun ContentResolver.folders(
    filter: String? = null,
    order: String = DEFAULT_FOLDERS_ORDER,
) = observe(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI).map {
    getFolders(filter, order)
}


fun ContentResolver.artists(
    filter: String? = null,
    order: String = DEFAULT_ARTISTS_ORDER,
) = observe(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI).map {
    getArtists(filter, order)
}

fun ContentResolver.albums(
    filter: String? = null,
    order: String = DEFAULT_ALBUMS_ORDER,
) = observe(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI).map {
    getAlbums(filter, order)
}

@kotlin.jvm.Throws(IllegalStateException::class)
private suspend inline fun ContentResolver.getBucketAudios(
    selection: String,
    args: Array<String>,
    order: String = DEFAULT_AUDIO_ORDER,
    offset: Int = 0,
    limit: Int = Int.MAX_VALUE
): List<Audio> {
    return query2(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection = AUDIO_PROJECTION,
        selection = selection,
        args,
        "$order LIMIT $limit OFFSET $offset",
        transform = { c ->
            List(c.count) {
                c.moveToPosition(it);
                c.toAudio
            }
        },
    ) ?: throw IllegalStateException("$TAG can't access cursor!")
}

/**
 * Returns the [Audio]s of the [Album] [name]
 */
suspend fun ContentResolver.getAlbum(
    name: String,
    filter: String? = null,
    order: String = DEFAULT_AUDIO_ORDER,
    offset: Int = 0,
    limit: Int = Int.MAX_VALUE
): List<Audio> {
    val like = if (filter != null) " AND ${MediaStore.Audio.Media.TITLE} LIKE ?" else ""
    val selection = "${replaceIfNull(MediaStore.Audio.Media.ALBUM)} == ?" + like
    val args = if (filter != null) arrayOf(name, "%$filter%") else arrayOf(name)
    return getBucketAudios(selection, args, order, offset, limit)
}

suspend fun ContentResolver.getArtist(
    name: String,
    filter: String? = null,
    order: String = DEFAULT_AUDIO_ORDER,
    offset: Int = 0,
    limit: Int = Int.MAX_VALUE
): List<Audio> {
    val like = if (filter != null) " AND ${MediaStore.Audio.Media.TITLE} LIKE ?" else ""
    val selection = "${replaceIfNull(MediaStore.Audio.Media.ARTIST)} == ?" + like
    val args = if (filter != null) arrayOf(name, "%$filter%") else arrayOf(name)
    return getBucketAudios(selection, args, order, offset, limit)
}

suspend fun ContentResolver.getFolder(
    path: String,
    filter: String? = null,
    order: String = DEFAULT_AUDIO_ORDER,
    offset: Int = 0,
    limit: Int = Int.MAX_VALUE
): List<Audio> {
    val like = if (filter != null) " AND ${MediaStore.Audio.Media.TITLE} LIKE ?" else ""
    val selection = "${COLUMN_BUCKET_PATH} == ?" + like
    val args = if (filter != null) arrayOf(path, "%$filter%") else arrayOf(path)
    return getBucketAudios(selection, args, order, offset, limit)
}


suspend fun ContentResolver.getGenre(
    name: String,
    filter: String? = null,
    order: String = DEFAULT_AUDIO_ORDER,
    offset: Int = 0,
    limit: Int = Int.MAX_VALUE
): List<Audio> {
    TODO()
}

fun ContentResolver.album(
    name: String,
    filter: String? = null,
    order: String = DEFAULT_AUDIO_ORDER,
) = observe(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI).map {
    getAlbum(name, filter, order)
}

fun ContentResolver.artist(
    name: String,
    filter: String? = null,
    order: String = DEFAULT_AUDIO_ORDER,
) = observe(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI).map {
    getArtist(name, filter, order)
}

fun ContentResolver.genre(
    name: String,
    filter: String? = null,
    order: String = DEFAULT_AUDIO_ORDER,
) = observe(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI).map {
    getGenre(name, filter, order)
}

fun ContentResolver.folder(
    path: String,
    filter: String? = null,
    order: String = DEFAULT_AUDIO_ORDER,
) = observe(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI).map {
    getFolder(path, filter, order)
}

suspend fun ContentResolver.findAudio(id: Long): Audio? {
    return query2(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        AUDIO_PROJECTION,
        "${MediaStore.Audio.Media._ID} ==?",
        arrayOf("$id"),
        null,
    ) {
        if (!it.moveToFirst()) return@query2 null else it.toAudio
    }
}

suspend fun ContentResolver.findAlbum(name: String): Album? {
    return query2(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        ALBUM_PROJECTION,
        "${replaceIfNull(MediaStore.Audio.Media.ALBUM)} == ?",
        arrayOf(name),
        null,
    ) {
        if (!it.moveToFirst()) return@query2 null else it.toAlbum
    }
}

suspend fun ContentResolver.findArtist(name: String): Artist? {
    return query2(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        ARTIST_PROJECTION,
        "${replaceIfNull(MediaStore.Audio.Media.ARTIST)} == ?",
        arrayOf(name),
        null,
    ) {
        if (!it.moveToFirst()) return@query2 null else it.toArtist
    }
}


suspend fun ContentResolver.findFolder(path: String): Folder? {
    return query2(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        FOLDER_PROJECTION,
        "${COLUMN_BUCKET_PATH} == ?",
        arrayOf(path),
        null,
    ) {
        if (!it.moveToFirst()) return@query2 null else it.toFolder
    }
}

suspend fun ContentResolver.findGenre(name: String): Genre? {
    return query2(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        FOLDER_PROJECTION,
        "$COLUMN_BUCKET_PATH == ?",
        arrayOf(name),
        null,
    ) {
        TODO()
    }
}

@Composable
@Preview
fun Preview() {
    val context = LocalContext.current
    LaunchedEffect(key1 = true) {
        val tm = context.contentResolver.getAudios()
        Log.d(TAG, "Preview: $tm")
    }
}
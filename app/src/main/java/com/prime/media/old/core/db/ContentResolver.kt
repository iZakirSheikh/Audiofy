package com.prime.media.old.core.db

import android.app.Activity
import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.database.ContentObserver
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Stable
import com.prime.media.old.core.playback.MediaItem
import com.prime.media.old.common.util.PathUtils
import com.prime.media.old.common.util.registerActivityResultLauncher
import com.prime.media.old.common.util.removeId
import com.prime.media.old.impl.Repository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.withContext as using


private const val TAG = "ContentResolver2"

private const val DUMMY_SELECTION = "${MediaStore.Audio.Media._ID} != 0"

/**
 * An advanced of [ContentResolver.query]
 * @see ContentResolver.query
 * @param order valid column to use for orderBy.
 */
suspend fun ContentResolver.query2(
    uri: Uri,
    projection: Array<String>? = null,
    selection: String = DUMMY_SELECTION,
    args: Array<String>? = null,
    order: String = MediaStore.MediaColumns._ID,
    ascending: Boolean = true,
    offset: Int = 0,
    limit: Int = Int.MAX_VALUE
): Cursor? {
    return using(Dispatchers.Default) {
        // use only above android 10
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // compose the args
            val args2 = Bundle().apply {
                // Limit & Offset
                putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
                putInt(ContentResolver.QUERY_ARG_OFFSET, offset)

                // order
                putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(order))
                putInt(
                    ContentResolver.QUERY_ARG_SORT_DIRECTION,
                    if (ascending) ContentResolver.QUERY_SORT_DIRECTION_ASCENDING else ContentResolver.QUERY_SORT_DIRECTION_DESCENDING
                )
                // Selection and groupBy
                if (args != null) putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, args)
                // add selection.
                // TODO: Consider adding group by.
                // currently I experienced errors in android 10 for groupBy and arg groupBy is supported
                // above android 10.
                putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
            }
            query(uri, projection, args2, null)
        }
        // below android 0
        else {
            //language=SQL
            val order2 =
                order + (if (ascending) " ASC" else " DESC") + " LIMIT $limit OFFSET $offset"
            // compose the selection.
            query(uri, projection, selection, args, order2)
        }
    }
}

/**
 * @see query2
 */
internal suspend inline fun <T> ContentResolver.query2(
    uri: Uri,
    projection: Array<String>? = null,
    selection: String = DUMMY_SELECTION,
    args: Array<String>? = null,
    order: String = MediaStore.MediaColumns._ID,
    ascending: Boolean = true,
    offset: Int = 0,
    limit: Int = Int.MAX_VALUE,
    transform: (Cursor) -> T
): T? {
    return query2(uri, projection, selection, args, order, ascending, offset, limit)?.use(transform)
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
        name = getString(1) ?: MediaStore.UNKNOWN_STRING,
        albumId = getLong(4),
        data = getString(8),
        album = getString(3) ?: MediaStore.UNKNOWN_STRING,
        artist = getString(2) ?: MediaStore.UNKNOWN_STRING,
        composer = getString(6) ?: MediaStore.UNKNOWN_STRING,
        // If the 'mimeType' is null, we set it to a default value of "audio/*"
        // This handles cases where the MIME type for the audio file couldn't be determined.
        mimeType = getString(10) ?: "audio/*",
        track = getInt(11),
        dateAdded = getLong(5) * 1000,
        dateModified = getLong(13) * 1000,
        duration = getInt(9),
        size = getLong(12),
        year = getInt(7)
    )

private val AUDIO_PROJECTION
    get() = arrayOf(
        MediaStore.Audio.Media._ID, //0
        MediaStore.Audio.Media.TITLE, // 1
        MediaStore.Audio.Media.ARTIST, // 2
        MediaStore.Audio.Media.ALBUM, // 3
        MediaStore.Audio.Media.ALBUM_ID, // 4
        MediaStore.Audio.Media.DATE_ADDED,  //5
        MediaStore.Audio.Media.COMPOSER, // , // 6
        MediaStore.Audio.Media.YEAR, // 7
        MediaStore.Audio.Media.DATA, // 8
        MediaStore.Audio.Media.DURATION, // 9
        MediaStore.Audio.Media.MIME_TYPE, // 10
        MediaStore.Audio.Media.TRACK, // 11
        MediaStore.Audio.Media.SIZE, //12
        MediaStore.Audio.Media.DATE_MODIFIED, // 14
    )

//language=SQL
private const val DEFAULT_AUDIO_SELECTION = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
private const val DEFAULT_AUDIO_ORDER = MediaStore.Audio.Media.TITLE

/**
 * @return [Audio]s from the [MediaStore].
 */
//@RequiresPermission(anyOf = [READ_EXTERNAL_STORAGE, WRITE_EXTERNAL_STORAGE])
suspend fun ContentResolver.getAudios(
    filter: String? = null,
    order: String = DEFAULT_AUDIO_ORDER,
    ascending: Boolean = true,
    offset: Int = 0,
    limit: Int = Int.MAX_VALUE
): List<Audio> {
    val title = MediaStore.Audio.Media.TITLE
    val artist = MediaStore.Audio.Media.ARTIST
    val album = MediaStore.Audio.Media.ALBUM
    return query2(
        uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection = AUDIO_PROJECTION,
        ascending = ascending,
        selection = DEFAULT_AUDIO_SELECTION + if (filter != null) " AND $title || $artist || $album LIKE ?" else "",
        args = if (filter != null) arrayOf("%$filter%") else null,
        order = order,
        offset = offset,
        limit = limit,
        transform = { c ->
            List(c.count) {
                c.moveToPosition(it)
                c.toAudio
            }
        },
    ) ?: emptyList()
}

@Stable
data class Artist(
    @JvmField val id: Long,
    @JvmField val name: String,
    @JvmField val tracks: Int,
    @JvmField val albums: Int,
)

private val ARTIST_PROJECTION
    get() = arrayOf(
        MediaStore.Audio.Artists._ID,//0
        MediaStore.Audio.Artists.ARTIST, // 1,
        MediaStore.Audio.Artists.NUMBER_OF_TRACKS, // 2
        MediaStore.Audio.Artists.NUMBER_OF_ALBUMS, // 3
    )

private inline val Cursor.toArtist
    get() = Artist(
        id = getLong(0),
        name = getString(1) ?: MediaStore.UNKNOWN_STRING,
        tracks = getInt(2),
        albums = getInt(3),
    )


suspend fun ContentResolver.getArtists(
    filter: String? = null,
    order: String = MediaStore.Audio.Media.ARTIST,
    ascending: Boolean = true,
    offset: Int = 0,
    limit: Int = Int.MAX_VALUE
): List<Artist> {
    return query2(
        MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
        projection = ARTIST_PROJECTION,
        selection = if (filter == null) DUMMY_SELECTION else "${MediaStore.Audio.Artists.ARTIST} LIKE ?",
        args = if (filter != null) arrayOf("%$filter%") else null,
        order,
        ascending = ascending,
        offset = offset,
        limit = limit,
        transform = { c ->
            List(c.count) {
                c.moveToPosition(it)
                c.toArtist
            }
        },
    ) ?: emptyList()
}

private val ALBUM_PROJECTION
    get() = arrayOf(
        MediaStore.Audio.Albums._ID, //0
        MediaStore.Audio.Albums.ALBUM,//1
        MediaStore.Audio.Albums.ARTIST, //2
        MediaStore.Audio.Albums.FIRST_YEAR, //3
        MediaStore.Audio.Albums.LAST_YEAR, // 4,
        MediaStore.Audio.Albums.NUMBER_OF_SONGS, // 5
    )

@Stable
data class Album(
    @JvmField val id: Long,
    @JvmField val title: String,
    @JvmField val artist: String,
    @JvmField val firstYear: Int,
    @JvmField val lastYear: Int,
    @JvmField val cardinality: Int,
)

private inline val Cursor.toAlbum
    get() = Album(
        id = getLong(0),
        title = getString(1) ?: MediaStore.UNKNOWN_STRING,
        artist = getString(2) ?: MediaStore.UNKNOWN_STRING,
        firstYear = getInt(3),
        lastYear = getInt(4),
        cardinality = getInt(5),
    )


suspend fun ContentResolver.getAlbums(
    filter: String? = null,
    order: String = MediaStore.Audio.Albums.ALBUM,
    ascending: Boolean = true,
    offset: Int = 0,
    limit: Int = Int.MAX_VALUE
): List<Album> {
    return query2(
        MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
        projection = ALBUM_PROJECTION,
        selection = if (filter == null) DUMMY_SELECTION else "${MediaStore.Audio.Albums.ALBUM} LIKE ?",
        if (filter != null) arrayOf("%$filter%") else null,
        order,
        ascending = ascending,
        offset = offset,
        limit = limit,
        transform = { c ->
            List(c.count) {
                c.moveToPosition(it)
                c.toAlbum
            }
                // FixMe: The albums are not distinct.
                // For now apply the function
                .distinctBy { it.title }
        },
    ) ?: emptyList()
}

private val GENRE_PROJECTION
    get() = arrayOf(
        MediaStore.Audio.Genres._ID, //0
        MediaStore.Audio.Genres.NAME,//1
    )

@Stable
data class Genre(
    @JvmField val id: Long, @JvmField val name: String, @JvmField val cardinality: Int = -1
)

private inline val Cursor.toGenre
    get() = Genre(
        id = getLong(0),
        name = getString(1) ?: MediaStore.UNKNOWN_STRING,
    )


suspend fun ContentResolver.getGenres(
    filter: String? = null,
    order: String = MediaStore.Audio.Genres.NAME,
    ascending: Boolean = true,
    offset: Int = 0,
    limit: Int = Int.MAX_VALUE
): List<Genre> {
    return query2(
        MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
        projection = GENRE_PROJECTION,
        selection = if (filter == null) DUMMY_SELECTION else "${MediaStore.Audio.Genres.NAME} LIKE ?",
        if (filter != null) arrayOf("%$filter%") else null,
        order,
        ascending = ascending,
        offset = offset,
        limit = limit,
        transform = { c ->
            List(c.count) {
                c.moveToPosition(it)
                c.toGenre
            }
        },
    ) ?: emptyList()
}

@JvmInline
@Stable
value class Folder(
    //@JvmField val albumId: Long,
    @JvmField val path: String,
)

val Folder.name: String
    get() = PathUtils.name(path)


suspend fun ContentResolver.getFolders(
    filter: String? = null, ascending: Boolean = true, offset: Int = 0, limit: Int = Int.MAX_VALUE
): List<Folder> {

    return query2(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        arrayOf(MediaStore.Audio.Media.DATA),
        selection = if (filter == null) DUMMY_SELECTION else "${MediaStore.Audio.Media.DATA} LIKE ?",
        if (filter != null) arrayOf("%$filter%") else null,
        order = MediaStore.Audio.Media.DATA,
        ascending = ascending
    ) { c ->
        val result = List(c.count) {
            c.moveToPosition(it); Folder(PathUtils.parent(c.getString(0)))
        }.distinct()
        // Fix. TODO: return limit to make consistent with others.
        // val fromIndex = if (offset > l.size - 1) l.size -1 else offset
        // val toIndex = if (offset + limit > l.size -1 ) TODO()
        result
    } ?: emptyList()
}

private suspend inline fun ContentResolver.getBucketAudios(
    selection: String,
    args: Array<String>,
    order: String = MediaStore.Audio.Media.TITLE,
    ascending: Boolean = true,
    offset: Int = 0,
    limit: Int = Int.MAX_VALUE
): List<Audio> {
    return query2(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection = AUDIO_PROJECTION,
        selection = selection,
        args,
        order,
        ascending,
        offset,
        limit,
        transform = { c ->
            List(c.count) {
                c.moveToPosition(it)
                c.toAudio
            }
        },
    ) ?: emptyList()
}

/**
 * Returns the [Audio]s of the [Album] [name]
 */
suspend fun ContentResolver.getAlbum(
    name: String,
    filter: String? = null,
    order: String = MediaStore.Audio.Media.TITLE,
    ascending: Boolean = true,
    offset: Int = 0,
    limit: Int = Int.MAX_VALUE
): List<Audio> {
    val like = if (filter != null) " AND ${MediaStore.Audio.Media.TITLE} LIKE ?" else ""
    val selection = "${MediaStore.Audio.Media.ALBUM} == ?" + like
    val args = if (filter != null) arrayOf(name, "%$filter%") else arrayOf(name)
    return getBucketAudios(selection, args, order, ascending, offset, limit)
}

suspend fun ContentResolver.getArtist(
    name: String,
    filter: String? = null,
    order: String = MediaStore.Audio.Media.TITLE,
    ascending: Boolean = true,
    offset: Int = 0,
    limit: Int = Int.MAX_VALUE
): List<Audio> {
    val like = if (filter != null) " AND ${MediaStore.Audio.Media.TITLE} LIKE ?" else ""
    val selection = "${MediaStore.Audio.Media.ARTIST} == ?" + like
    val args = if (filter != null) arrayOf(name, "%$filter%") else arrayOf(name)
    return getBucketAudios(selection, args, order, ascending, offset, limit)
}

suspend fun ContentResolver.getFolder(
    path: String,
    filter: String? = null,
    order: String = MediaStore.Audio.Media.TITLE,
    ascending: Boolean = true,
    offset: Int = 0,
    limit: Int = Int.MAX_VALUE
): List<Audio> {
    val like = if (filter != null) " AND ${MediaStore.Audio.Media.TITLE} LIKE ?" else ""
    val selection = "${MediaStore.Audio.Media.DATA} LIKE ?" + like
    val args = if (filter != null) arrayOf("$path%", "%$filter%") else arrayOf("$path%")
    return getBucketAudios(selection, args, order, ascending, offset, limit)
}

suspend fun ContentResolver.getGenre(
    name: String,
    filter: String? = null,
    order: String = MediaStore.Audio.Media.TITLE,
    ascending: Boolean = true,
    offset: Int = 0,
    limit: Int = Int.MAX_VALUE
): List<Audio> {
    //maybe for api 30 we can use directly the genre name.
    // find the id.
    val id = query2(
        MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
        arrayOf(MediaStore.Audio.Genres._ID),
        "${MediaStore.Audio.Genres.NAME} == ?",
        arrayOf(name),
        limit = 1
    ) {
        if (it.count == 0) return emptyList()
        it.moveToPosition(0)
        it.getLong(0)
    } ?: return emptyList()

    // calculate the ids.
    val list = query2(
        MediaStore.Audio.Genres.Members.getContentUri("external", id),
        arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID),
    ) { c ->
        if (c.count == 0) return emptyList()
        val buffer = StringBuilder()
        while (c.moveToNext()) {
            if (!c.isFirst) buffer.append(",")
            val element = c.getLong(0)
            buffer.append("'$element'")
        }
        buffer.toString()
    } ?: return emptyList()

    val like = if (filter != null) " AND ${MediaStore.Audio.Media.TITLE} LIKE ?" else ""
    return query2(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        AUDIO_PROJECTION,
        //language=SQL
        DEFAULT_AUDIO_SELECTION + " AND ${MediaStore.Audio.Media._ID} IN ($list)" + like,
        if (filter != null) arrayOf("%$filter%") else null,
        order,
        ascending,
        offset,
        limit
    ) { c ->
        List(c.count) {
            c.moveToPosition(it)
            c.toAudio
        }
    } ?: emptyList()
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
    registerContentObserver(uri, true, observer)
    // trigger first.
    trySend(false)
    awaitClose {
        unregisterContentObserver(observer)
    }
}


suspend fun ContentResolver.findAudio(id: Long): Audio? {
    return query2(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        AUDIO_PROJECTION,
        "${MediaStore.Audio.Media._ID} ==?",
        arrayOf("$id"),
    ) {
        if (!it.moveToFirst()) return@query2 null else it.toAudio
    }
}

suspend fun ContentResolver.findAlbum(name: String): Album? {
    return query2(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        ALBUM_PROJECTION,
        "${MediaStore.Audio.Media.ALBUM} == ?",
        arrayOf(name),
    ) {
        if (!it.moveToFirst()) return@query2 null else it.toAlbum
    }
}

suspend fun ContentResolver.findArtist(name: String): Artist? {
    return query2(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        ARTIST_PROJECTION,
        "${MediaStore.Audio.Media.ARTIST} == ?",
        arrayOf(name),
    ) {
        if (!it.moveToFirst()) return@query2 null else it.toArtist
    }
}


suspend fun ContentResolver.findFolder(path: String): Folder? {
    return query2(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        arrayOf(MediaStore.Audio.Media.DATA),
        "${MediaStore.Audio.Media.DATA} LIKE ?",
        arrayOf("$path%"),
    ) {
        if (!it.moveToFirst()) return@query2 null else Folder(PathUtils.parent(it.getString(0)))
    }
}

fun ContentResolver.audios(
    filter: String? = null,
    order: String = DEFAULT_AUDIO_ORDER,
    ascending: Boolean = true,
) = observe(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI).map {
    getAudios(filter, order, ascending)
}

fun ContentResolver.folders(
    filter: String? = null,
    order: String = MediaStore.Audio.Media.DATA,
    ascending: Boolean = true,
) = observe(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI).map {
    getFolders(filter, ascending)
}


fun ContentResolver.artists(
    filter: String? = null,
    order: String = MediaStore.Audio.Artists.ARTIST,
    ascending: Boolean = true,
) = observe(MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI).map {
    getArtists(filter, order, ascending)
}

fun ContentResolver.albums(
    filter: String? = null,
    order: String = MediaStore.Audio.Albums.ALBUM,
    ascending: Boolean = true,
) = observe(MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI).map {
    getAlbums(filter, order, ascending)
}

fun ContentResolver.genres(
    filter: String? = null,
    order: String = MediaStore.Audio.Genres.NAME,
    ascending: Boolean = true,
) = observe(MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI).map {
    getGenres(filter, order, ascending)
}

fun ContentResolver.album(
    name: String,
    filter: String? = null,
    order: String = DEFAULT_AUDIO_ORDER,
    ascending: Boolean = true,
) = observe(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI).map {
    getAlbum(name, filter, order, ascending)
}

fun ContentResolver.artist(
    name: String,
    filter: String? = null,
    order: String = DEFAULT_AUDIO_ORDER,
    ascending: Boolean = true,
) = observe(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI).map {
    getArtist(name, filter, order, ascending)
}

fun ContentResolver.genre(
    name: String,
    filter: String? = null,
    order: String = DEFAULT_AUDIO_ORDER,
    ascending: Boolean = true,
) = observe(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI).map {
    getGenre(name, filter, order, ascending)
}

fun ContentResolver.folder(
    path: String,
    filter: String? = null,
    order: String = DEFAULT_AUDIO_ORDER,
    ascending: Boolean = true,
) = observe(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI).map {
    getFolder(path, filter, order, ascending)
}

/**
 * Finds the audio associated with the provided [path].
 */
suspend fun ContentResolver.findAudio(path: String): Audio? {
    return query2(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        AUDIO_PROJECTION,
        "${MediaStore.Audio.Media.DATA} == ?",
        arrayOf("$path"),
        limit = 1
    ) { if (!it.moveToFirst()) return@query2 null else it.toAudio }
}

/**
 * Find the audio associated with the media store content [uri]
 */
private suspend fun ContentResolver.findAudio(uri: Uri): Audio? {
    return query2(uri, AUDIO_PROJECTION, limit = 1) {
        if (!it.moveToFirst()) return@query2 null else it.toAudio
    }
}


private const val EXTERNAL_STORAGE_DOCUMENT_AUTHORITY = "com.android.externalstorage.documents"
private const val DOWNLOAD_DOCUMENT_AUTHORITY = "com.android.providers.downloads.documents"
private const val MEDIA_DOCUMENT_AUTHORITY = "com.android.providers.media.documents"

private const val EXTERNAL_PATH = "/storage/"

/**
 * Finds the audio associated with the [uri].
 * * Note the [uri] might be document, content uri or something else.
 * @see <a href="https://gist.github.com/Linus-DuJun/34d1b346c4643f482c17960335f5960d">Source 1 </a
 * @see <a href="https://gist.github.com/r0b0t3d/492f375ec6267a033c23b4ab8ab11e6a">Source 2</a>
 */
suspend fun Context.findAudio(uri: Uri): Audio? {
    if (ContentResolver.SCHEME_FILE.equals(uri.scheme, ignoreCase = true)) // File
        return contentResolver.findAudio(path = uri.path ?: return null)
    if (uri.scheme == ContentResolver.SCHEME_CONTENT && uri.authority == MediaStore.AUTHORITY)
        return contentResolver.findAudio(uri)
    if (!DocumentsContract.isDocumentUri(this, uri))
        return null
    // handle different cases.
    when (uri.authority) {
        EXTERNAL_STORAGE_DOCUMENT_AUTHORITY -> {
            val docId = DocumentsContract.getDocumentId(uri) ?: ""
            val split = docId.split(":")
            val path = if (split[0].equals("primary", ignoreCase = true)) {
                Environment.getExternalStorageDirectory().toString() + "/" + split[1]
            } else {
                EXTERNAL_PATH + split[0] + "/" + split[1]
            }
            return contentResolver.findAudio(path)
        }

        DOWNLOAD_DOCUMENT_AUTHORITY -> {
            val id = DocumentsContract.getDocumentId(uri) ?: return null
            val uri = ContentUris.withAppendedId(
                Uri.parse("content://downloads/public_downloads"), id.toLong()
            )
            return findAudio(uri)
        }

        MEDIA_DOCUMENT_AUTHORITY -> {
            val docId = DocumentsContract.getDocumentId(uri)
            val split = docId.split(":")
            val id = split[1].toLong() // id
            return contentResolver.findAudio(id)
        }

        else -> return null
    }
}

/**
 * Returns the content URI for this audio file, using the [MediaStore.Audio.Media.EXTERNAL_CONTENT_URI]
 * and appending the file's unique ID.
 *
 * @return the content URI for the audio file
 */
val Audio.uri
    get() = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

/**
 * Returns the content URI for the album art image of this audio file's album, using the
 * [MediaStore.Images.Media.EXTERNAL_CONTENT_URI] and appending the album ID to the end of the URI.
 *
 * @return the content URI for the album art image of this audio file's album
 */
val Audio.albumUri
    get() = Repository.toAlbumArtUri(albumId)

/**
 * Returns the content URI for the album art image of this album, using the [MediaStore.Images.Media.EXTERNAL_CONTENT_URI]
 * and appending the album's unique ID to the end of the URI.
 *
 * @return the content URI for the album art image of this album
 */
val Album.uri
    get() = Repository.toAlbumArtUri(id)

/**
 * Returns the content URI for this audio file as a string, using the [uri] property of the audio file.
 *
 * @return the content URI for this audio file as a string
 */
val Audio.key get() = uri.toString()

/**
 * Returns a [MediaItem] object that represents this audio file as a playable media item.
 *
 * @return the [MediaItem] object that represents this audio file
 */
inline val Audio.toMediaItem
    get() = MediaItem(uri, name, artist, "$id", albumUri)

/**
 * Deletes list uri from the device persistently.
 * @return -1 if error occurred otherwise -2 indicating an dialog is about to be shown to user
 * and he needs to confirm before deleting.
 */
@RequiresApi(Build.VERSION_CODES.R)
fun ContentResolver.delete(activity: Activity, vararg uri: Uri): Int {
    val deleteRequest = MediaStore.createDeleteRequest(this, uri.toList()).intentSender
    activity.startIntentSenderForResult(deleteRequest, 100, null, 0, 0, 0)
    return -2 // dialog is about to be shown.
}

/**
 * Deletes the specified URIs from the device's persistent storage permanently.
 *
 *  Note hat this fun works only unto android 10.
 * @param uri The URIs to delete.
 * @return The number of items that were deleted, or -1 if an error occurred.
 */
suspend fun ContentResolver.delete(vararg uri: Uri): Int {
    if (uri.isEmpty())
        return 0
    // construct which ids have been removed.
    val ids = uri.joinToString(", ") {
        "${ContentUris.parseId(it)}"
    }
    val parent = uri[0].removeId
    val projection = arrayOf(MediaStore.MediaColumns.DATA)
    val res =
        query2(parent, projection, "${MediaStore.MediaColumns._ID} IN ($ids)") { c ->
            List(c.count) { c.moveToPosition(it);c.getString(0) }
        }
    // check if res in null
    if (res == null) return -1 // error
    var count = delete(parent, "${MediaStore.MediaColumns._ID} IN ($ids)", null)
    if (count == 0) return -1 // error
    res.forEach {
        // decrease count in case files cant be deleted
        if (!File(it).delete())
            count--
    }
    // error
    return if (count == 0) -1 else count
}

/**
 * Deletes the specified URIs from the device's content permanently.
 *
 * @param activity The [ComponentActivity] that is making the call.
 * @param uri The URIs of the content to be deleted.
 * @return The number of items that were deleted, or -1 if an error occurred. -3 if cancelled by user.
 */
@RequiresApi(Build.VERSION_CODES.R)
suspend fun ContentResolver.delete(activity: ComponentActivity, vararg uri: Uri): Int {
    if (uri.isEmpty()) return -1 // error
    return suspendCoroutine { continuation ->
        // Create a lazy ActivityResultLauncher object
        var launcher: ActivityResultLauncher<IntentSenderRequest>? = null
        // Assign result to launcher in such a way tha it allows us to
        // unregister later.
        val contract = ActivityResultContracts.StartIntentSenderForResult()
        launcher = activity.registerActivityResultLauncher(contract) {
            // unregister launcher
            launcher?.unregister()
            // user cancelled
            if (it.resultCode == Activity.RESULT_CANCELED) {
                continuation.resume(-3 /*cancelled*/)
                return@registerActivityResultLauncher
            }
            // some unknown error occurred
            if (it.resultCode != Activity.RESULT_OK) {
                continuation.resume(-1 /*Error*/)
                return@registerActivityResultLauncher
            }
            // construct which ids have been removed.
            val ids = uri.joinToString(", ") {
                "${ContentUris.parseId(it)}"
            }
            // assuming that all uri's are content uris.
            val parent = ContentUris.removeId(uri[0])
            // check how many are still there; maybe some error occurred while deleting some files.
            val projection = arrayOf(MediaStore.MediaColumns._ID)
            val count =
                query(
                    parent,
                    projection,
                    "${MediaStore.MediaColumns._ID} IN ($ids)",
                    null,
                    null
                ).use { it?.count ?: -1 }
            // resume with how many files have been deleted or error code.
            continuation.resume(if (count > 0) uri.size - count else if (count == 0) uri.size else count)
        }
        val request = MediaStore.createDeleteRequest(this, uri.toList()).intentSender
        // Create an IntentSenderRequest object from the IntentSender object
        val intentSenderRequest = IntentSenderRequest.Builder(request).build()
        // Launch the activity for result using the IntentSenderRequest object
        launcher.launch(intentSenderRequest)
    }
}

/**
 * A simple extension method that trashes files insetead of deleting them.
 * @see delete
 */
@RequiresApi(Build.VERSION_CODES.R)
suspend fun ContentResolver.trash(activity: ComponentActivity, vararg uri: Uri): Int {
    if (uri.isEmpty()) return -1 // error
    return suspendCoroutine { continuation ->
        // Create a lazy ActivityResultLauncher object
        var launcher: ActivityResultLauncher<IntentSenderRequest>? = null
        // Assign result to launcher in such a way tha it allows us to
        // unregister later.
        val contract = ActivityResultContracts.StartIntentSenderForResult()
        launcher = activity.registerActivityResultLauncher(contract) {
            // unregister launcher
            launcher?.unregister()
            // user cancelled
            if (it.resultCode == Activity.RESULT_CANCELED) {
                continuation.resume(-3 /*cancelled*/)
                return@registerActivityResultLauncher
            }
            // some unknown error occurred
            if (it.resultCode != Activity.RESULT_OK) {
                continuation.resume(-1 /*Error*/)
                return@registerActivityResultLauncher
            }
            // construct which ids have been removed.
            val ids = uri.joinToString(", ") {
                "${ContentUris.parseId(it)}"
            }
            // assuming that all uri's are content uris.
            val parent = ContentUris.removeId(uri[0])
            // check how many are still there; maybe some error occurred while deleting some files.
            val projection = arrayOf(MediaStore.MediaColumns._ID)
            val count =
                query(
                    parent,
                    projection,
                    "${MediaStore.MediaColumns._ID} IN ($ids)",
                    null,
                    null
                ).use { it?.count ?: -1 }
            // resume with how many files have been trashed or error code.
            continuation.resume(if (count > 0) uri.size - count else if (count == 0) uri.size else count)
        }
        val request = MediaStore.createTrashRequest(this, uri.toList(), true).intentSender
        // Create an IntentSenderRequest object from the IntentSender object
        val intentSenderRequest = IntentSenderRequest.Builder(request).build()
        // Launch the activity for result using the IntentSenderRequest object
        launcher.launch(intentSenderRequest)
    }
}

/**
 * A simple extension method that trashes files instead of deleting them.
 * @see delete
 */
@RequiresApi(Build.VERSION_CODES.R)
fun ContentResolver.trash(activity: Activity, vararg uri: Uri): Int {
    val deleteRequest = MediaStore.createTrashRequest(this, uri.toList(), true).intentSender
    activity.startIntentSenderForResult(deleteRequest, 100, null, 0, 0, 0)
    return -2 // dialog is about to be shown.
}
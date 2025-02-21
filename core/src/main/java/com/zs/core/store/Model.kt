/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 21-09-2024.
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

package com.zs.core.store

import android.content.ContentUris
import android.database.Cursor
import android.provider.MediaStore
import com.zs.core.util.PathUtils

private val FILE_PROJECTION = arrayOf(
    MediaProvider.COLUMN_ID, // 0
    MediaProvider.COLUMN_NAME, // 1
    MediaProvider.COLUMN_MIME_TYPE, // 2
    MediaProvider.COLUMN_PATH, // 3
    MediaProvider.COLUMN_DATE_ADDED,  // 4
    MediaProvider.COLUMN_DATE_MODIFIED, // 5
    MediaProvider.COLUMN_SIZE // 6
)

/**
 * A generic interface representing a file, containing common attributes like ID, name, size, etc.
 *
 * @property id The unique ID of the file.
 * @property name The name of the file.
 * @property mimeType The MIME type of the file, e.g., "image/jpeg", "video/mp4".
 * @property path The absolute path to the file.
 * @property dateAdded The timestamp when the file was added, in milliseconds since epoch.
 * @property dateModified The timestamp when the file was last modified, in milliseconds since epoch.
 * @property size The size of the file in bytes.
 */
interface File {
    val id: Long
    val name: String
    val mimeType: String
    val path: String
    val dateAdded: Long
    val dateModified: Long
    val size: Long


    val contentUri
        get() = when (this) {
            is Video -> ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
            else -> error("Type(${this.javaClass.simpleName}) not registered")
        }
}


internal val AUDIO_PROJECTION = FILE_PROJECTION + arrayOf(
    MediaProvider.COLUMN_MEDIA_DURATION, // 7
    MediaProvider.COLUMN_AUDIO_ALBUM, // 8
    MediaProvider.COLUMN_AUDIO_ARTIST, // 9
    MediaProvider.COLUMN_AUDIO_ALBUM_ID, // 10
    MediaProvider.COLUMN_AUDIO_COMPOSER, // 11
    MediaProvider.COLUMN_AUDIO_YEAR, // 12
    MediaProvider.COLUMN_AUDIO_NUMBER_OF_TRACKS,  //13
    MediaProvider.COLUMN_ALBUM_ARTIST // 14
)

/**
 * Represents an audio file, extending the base [File] class.
 * It includes metadata specific to audio files like duration, album, artist etc.
 * @param duration The duration of the audio file in milliseconds.
 * @param album The name of the album this audio file belongs to.
 * @param artist The name of the artist of this audio file.
 * @param albumId The ID of the album this audio file belongs to.
 * @param composer The composer of this audio file.
 * @param year The year this audio file was released.
 * @param tracks The total number of tracks in the album this audio file belongs to.
 * @param albumArtist The artist of the album this audio file belongs to.
 *
 * @see android.provider.MediaStore.Audio
 * @see File
 */
data class Audio(
    override val id: Long,
    override val name: String,
    override val mimeType: String,
    override val path: String,
    override val dateAdded: Long,
    override val dateModified: Long,
    override val size: Long,
    val duration: Int,
    val album: String,
    val artist: String,
    val albumId: Long,
    val composer: String,
    val year: Int,
    val tracks: Int,
    val albumArtist: String,
) : File {

    internal constructor(cursor: Cursor) : this(
        id = cursor.getLong(0),
        name = cursor.getString(1) ?: MediaProvider.UNKNOWN_STRING,
        mimeType = cursor.getString(2),
        path = cursor.getString(3),
        dateAdded = cursor.getLong(4) * 1000,
        dateModified = cursor.getLong(5) * 1000,
        size = cursor.getLong(6),
        duration = cursor.getInt(7),
        album = cursor.getString(8) ?: MediaProvider.UNKNOWN_STRING,
        artist = cursor.getString(9) ?: MediaProvider.UNKNOWN_STRING,
        albumId = cursor.getLong(10),
        composer = cursor.getString(11) ?: MediaProvider.UNKNOWN_STRING,
        year = cursor.getInt(12),
        tracks = cursor.getInt(13),
        albumArtist = cursor.getString(14) ?: MediaProvider.UNKNOWN_STRING
    )
}

internal val VIDEO_PROJECTION = FILE_PROJECTION + arrayOf(
    MediaProvider.COLUMN_MEDIA_DURATION, // 7
    MediaProvider.COLUMN_WIDTH, // 8
    MediaProvider.COLUMN_HEIGHT, // 9
    MediaProvider.COLUMN_ORIENTATION // 10
)

/**
 * Represents a video file, extending the base [File] class.
 * It includes metadata specific to video files like duration, width, height, etc.
 *
 * @param id The unique ID of the video file.
 * @param name The name of the video file.
 * @param mimeType The MIME type of the video file, e.g., "video/mp4".
 * @param path The absolute path to the video file.
 * @param dateAdded The timestamp when the file was added, in milliseconds since epoch.
 * @param dateModified The timestamp when the file was last modified, in milliseconds since epoch.
 * @param size The size of the video file in bytes.
 * @param duration The duration of the video in milliseconds.
 * @param width The width of the video in pixels.
 * @param height The height of the video in pixels.
 * @param orientation The orientation of the video (e.g., 0, 90, 180, 270).
 *
 * @see android.provider.MediaStore.Video
 */
data class Video(
    override val id: Long,
    override val name: String,
    override val mimeType: String,
    override val path: String,
    override val dateAdded: Long,
    override val dateModified: Long,
    override val size: Long,
    val duration: Long,
    val width: Int,
    val height: Int,
    val orientation: Int
) : File {
    internal constructor(cursor: Cursor) : this(
        id = cursor.getLong(0),
        name = cursor.getString(1) ?: MediaProvider.UNKNOWN_STRING,
        mimeType = cursor.getString(2),
        path = cursor.getString(3),
        dateAdded = cursor.getLong(4) * 1000,
        dateModified = cursor.getLong(5) * 1000,
        size = cursor.getLong(6),
        duration = cursor.getLong(7),
        width = cursor.getInt(8),
        height = cursor.getInt(9),
        orientation = cursor.getInt(10)
    )
}


internal val TRASHED_PROJECTION =
    FILE_PROJECTION + MediaProvider.COLUMN_DATE_EXPIRES // 7

/**
 * Represents a Trashed file on the device.
 * @property expires The timestamp (in milliseconds) when the trashed file will be permanently deleted.
 */
data class Trashed(
    override val id: Long,
    override val name: String,
    override val mimeType: String,
    override val path: String,
    override val dateAdded: Long,
    override val dateModified: Long,
    override val size: Long,
    val expires: Long,
) : File {

    internal constructor(cursor: Cursor) : this(
        id = cursor.getLong(0),
        name = cursor.getString(1) ?: MediaProvider.UNKNOWN_STRING,
        mimeType = cursor.getString(2),
        path = cursor.getString(3),
        dateAdded = cursor.getLong(4) * 1000,
        dateModified = cursor.getLong(5) * 1000,
        size = cursor.getLong(6),
        expires = cursor.getLong(7) * 1000
    )
}

internal val ALBUM_PROJECTION
    get() = arrayOf(
        MediaStore.Audio.Albums._ID, //0
        MediaStore.Audio.Albums.ALBUM,//1
        MediaStore.Audio.Albums.ARTIST, //2
        MediaStore.Audio.Albums.FIRST_YEAR, //3
        MediaStore.Audio.Albums.LAST_YEAR, // 4,
        MediaStore.Audio.Albums.NUMBER_OF_SONGS, // 5
    )

/**
 * Represents a music album.
 *
 * @param id The unique ID of the album.
 * @param title The title of the album.
 * @param artist The name of the artist of the album.
 * @param firstYear The first year the album was released.
 * @param lastYear The last year the album was released (for albums released over multiple years).
 * @param cardinality The number of songs in the album.
 */
data class Album(
    @JvmField val id: Long,
    @JvmField val title: String,
    @JvmField val artist: String,
    @JvmField val firstYear: Int,
    @JvmField val lastYear: Int,
    @JvmField val cardinality: Int,
) {
    internal constructor(cursor: Cursor) : this(
        id = cursor.getLong(0),
        title = cursor.getString(1) ?: MediaProvider.UNKNOWN_STRING,
        artist = cursor.getString(2) ?: MediaProvider.UNKNOWN_STRING,
        firstYear = cursor.getInt(3),
        lastYear = cursor.getInt(4),
        cardinality = cursor.getInt(5),
    )

    val artworkUri get() = MediaProvider.buildAlbumArtUri(id)
}

internal val ARTIST_PROJECTION
    get() = arrayOf(
        MediaStore.Audio.Artists._ID,// 0
        MediaStore.Audio.Artists.ARTIST, // 1,
        MediaStore.Audio.Artists.NUMBER_OF_TRACKS, // 2
        MediaStore.Audio.Artists.NUMBER_OF_ALBUMS, // 3
    )

/**
 * Represents a audio artist.
 *
 * @param id The unique ID of the artist.
 * @param name The name of the artist.
 * @param tracks The total number of tracks by this artist.
 * @param albums The total number of albums by this artist.
 */
data class Artist(
    @JvmField val id: Long,
    @JvmField val name: String,
    @JvmField val tracks: Int,
    @JvmField val albums: Int,
) {
    internal constructor(cursor: Cursor) : this(
        id = cursor.getLong(0),
        name = cursor.getString(1) ?: MediaProvider.UNKNOWN_STRING,
        tracks = cursor.getInt(2),
        albums = cursor.getInt(3),
    )
}

/**
 * Represents a folder with associated properties.
 *
 * @property artwork The artwork associated with the folder. -1 if none.
 * @property path The path of the folder.
 * @property count The count of items within the folder.
 * @property size The size of the folder in bytes.
 */
data class Folder(
    @JvmField val artworkID: Long,
    @JvmField val path: String,
    @JvmField val count: Int,
    @JvmField val size: Int,
    @JvmField val lastModified: Long
) {
    val name: String get() = PathUtils.name(path)
    val artworkUri get() = if (artworkID == -1L) null else MediaProvider.buildAlbumArtUri(artworkID)
}

internal val GENRE_PROJECTION
    get() = arrayOf(
        MediaStore.Audio.Genres._ID, //0
        MediaStore.Audio.Genres.NAME,//1
    )

/**
 * This data class represents a music genre.
 *
 * @param id The unique ID of the genre.
 * @param name The name of the genre.
 * @param cardinality The number of songs in this genre. Defaults to -1 if unknown.
 *
 * @see android.provider.MediaStore.Audio.Genres
 */
data class Genre(
    @JvmField val id: Long, @JvmField val name: String, @JvmField val cardinality: Int = -1
) {
    internal constructor(cursor: Cursor) : this(
        id = cursor.getLong(0),
        name = cursor.getString(1) ?: MediaProvider.UNKNOWN_STRING,
    )
}
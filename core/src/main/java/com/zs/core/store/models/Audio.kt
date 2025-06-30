/*
 * Copyright 2025 sheik
 *
 * Created by sheik on 09-05-2025.
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

package com.zs.core.store.models

import android.content.ContentUris
import android.database.Cursor
import android.provider.MediaStore
import com.zs.core.store.MediaProvider

/**
 * Represents an audio file.
 * It includes metadata specific to audio files like duration, album, artist etc.
 * @param id The unique ID of the file.
 * @param name The name of the file.
 * @param mimeType The MIME type of the file, e.g., "image/jpeg", "video/mp4".
 * @param path The absolute path to the file.
 * @param dateAdded The timestamp when the file was added, in milliseconds since epoch.
 * @param dateModified The timestamp when the file was last modified, in milliseconds since epoch.
 * @param size The size of the file in bytes.
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
 */
class Audio(
    @JvmField val id: Long,
    @JvmField val name: String,
    @JvmField val mimeType: String,
    @JvmField val path: String,
    @JvmField val dateAdded: Long,
    @JvmField val dateModified: Long,
    @JvmField val size: Long,
    @JvmField val duration: Int,
    @JvmField val album: String,
    @JvmField val artist: String,
    @JvmField val albumId: Long,
    @JvmField val composer: String,
    @JvmField val year: Int,
    @JvmField val tracks: Int,
    @JvmField val albumArtist: String,
) {
    val uri
        get() = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
    val artworkUri get() = MediaProvider.buildAlbumArtUri(albumId)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Audio

        if (id != other.id) return false
        if (dateAdded != other.dateAdded) return false
        if (dateModified != other.dateModified) return false
        if (size != other.size) return false
        if (duration != other.duration) return false
        if (albumId != other.albumId) return false
        if (year != other.year) return false
        if (tracks != other.tracks) return false
        if (name != other.name) return false
        if (mimeType != other.mimeType) return false
        if (path != other.path) return false
        if (album != other.album) return false
        if (artist != other.artist) return false
        if (composer != other.composer) return false
        if (albumArtist != other.albumArtist) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + dateAdded.hashCode()
        result = 31 * result + dateModified.hashCode()
        result = 31 * result + size.hashCode()
        result = 31 * result + duration
        result = 31 * result + albumId.hashCode()
        result = 31 * result + year
        result = 31 * result + tracks
        result = 31 * result + name.hashCode()
        result = 31 * result + mimeType.hashCode()
        result = 31 * result + path.hashCode()
        result = 31 * result + album.hashCode()
        result = 31 * result + artist.hashCode()
        result = 31 * result + composer.hashCode()
        result = 31 * result + albumArtist.hashCode()
        return result
    }

    override fun toString(): String {
        return "Audio(id=$id, name='$name', mimeType='$mimeType', path='$path', dateAdded=$dateAdded, dateModified=$dateModified, size=$size, duration=$duration, album='$album', artist='$artist', albumId=$albumId, composer='$composer', year=$year, tracks=$tracks, albumArtist='$albumArtist')"
    }

    internal constructor(cursor: Cursor) : this(
        id = cursor.getLong(0),
        name = cursor.getString(1) ?: MediaProvider.Companion.UNKNOWN_STRING,
        mimeType = cursor.getString(2),
        path = cursor.getString(3),
        dateAdded = cursor.getLong(4) * 1000,
        dateModified = cursor.getLong(5) * 1000,
        size = cursor.getLong(6),
        duration = cursor.getInt(7),
        album = cursor.getString(8) ?: MediaProvider.Companion.UNKNOWN_STRING,
        artist = cursor.getString(9) ?: MediaProvider.Companion.UNKNOWN_STRING,
        albumId = cursor.getLong(10),
        composer = cursor.getString(11) ?: MediaProvider.Companion.UNKNOWN_STRING,
        year = cursor.getInt(12),
        tracks = cursor.getInt(13),
        albumArtist = cursor.getString(14) ?: MediaProvider.Companion.UNKNOWN_STRING
    )

    /**
     * This data class represents a audio genre.
     *
     * @param id The unique ID of the genre.
     * @param name The name of the genre.
     * @param cardinality The number of tracks in this genre. Defaults to -1 if unknown.
     *
     * @see android.provider.MediaStore.Audio.Genres
     */
    class Genre(
        @JvmField val id: Long,
        @JvmField val name: String,
        @JvmField val cardinality: Int = -1
    ) {
        internal constructor(cursor: Cursor) : this(
            id = cursor.getLong(0),
            name = cursor.getString(1) ?: MediaProvider.UNKNOWN_STRING,
        )

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Genre

            if (id != other.id) return false
            if (cardinality != other.cardinality) return false
            if (name != other.name) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + cardinality
            result = 31 * result + name.hashCode()
            return result
        }

        override fun toString(): String {
            return "Genre(id=$id, name='$name', cardinality=$cardinality)"
        }
    }

    /**
     * Represents a audio artist.
     *
     * @param id The unique ID of the artist.
     * @param name The name of the artist.
     * @param tracks The total number of tracks by this artist.
     * @param albums The total number of albums by this artist.
     */
    class Artist(
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

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Artist

            if (id != other.id) return false
            if (tracks != other.tracks) return false
            if (albums != other.albums) return false
            if (name != other.name) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + tracks
            result = 31 * result + albums
            result = 31 * result + name.hashCode()
            return result
        }

        override fun toString(): String {
            return "Artist(id=$id, name='$name', tracks=$tracks, albums=$albums)"
        }
    }

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
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Album

            if (id != other.id) return false
            if (firstYear != other.firstYear) return false
            if (lastYear != other.lastYear) return false
            if (cardinality != other.cardinality) return false
            if (title != other.title) return false
            if (artist != other.artist) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + firstYear
            result = 31 * result + lastYear
            result = 31 * result + cardinality
            result = 31 * result + title.hashCode()
            result = 31 * result + artist.hashCode()
            return result
        }

        override fun toString(): String {
            return "Album(id=$id, title='$title', artist='$artist', firstYear=$firstYear, lastYear=$lastYear, cardinality=$cardinality)"
        }
    }
}
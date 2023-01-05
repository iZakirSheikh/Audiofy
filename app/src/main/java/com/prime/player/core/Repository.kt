package com.prime.player.core

import android.content.ContentResolver
import android.content.ContentUris
import android.net.Uri
import android.provider.MediaStore
import androidx.annotation.WorkerThread
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import com.prime.player.core.Playlist.Member
import com.prime.player.core.Repository.Companion.toAlbumArtUri
import dagger.hilt.android.scopes.ActivityRetainedScoped
import dagger.hilt.android.scopes.ActivityScoped
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

private const val TAG = "Repository"


val Audio.uri get() = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
val Audio.albumUri get() = toAlbumArtUri(albumId)

val Album.uri get() = toAlbumArtUri(id)

private inline fun Audio.toMember(playlistId: Long, order: Int) =
    Member(playlistId, "$id", order, uri.toString(), name, artist, albumUri.toString())


/**
 * This Composes the [MediaStore.Audio.Media.EXTERNAL_CONTENT_URI] with the provided [Audio] [id]
 */
private fun toAudioTrackUri(id: Long) =
    ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

/**
 * The [Audio.key] is a simple extension fun that returns uri as [String]
 */
val Audio.key get() = uri.toString()


/**
 * A simple extension fun that returns a Playable [MediaItem]
 */
inline val Audio.toMediaItem
    get() = MediaItem.Builder()
        .setMediaId("$id")
        .setRequestMetadata(
            MediaItem.RequestMetadata.Builder()
                .setMediaUri(uri)
                .build()
        )
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setArtworkUri(albumUri)
                .setTitle(name)
                .setSubtitle(artist)
                .setFolderType(MediaMetadata.FOLDER_TYPE_NONE)
                .setIsPlayable(true)
                // .setExtras(bundleOf(ARTIST_ID to artistId, ALBUM_ID to albumId))
                .build()
        )
        .build()


@ActivityRetainedScoped
class Repository @Inject constructor(
    private val playlistz: Playlists,
    private val resolver: ContentResolver
) {

    companion object {
        private const val ALBUM_ART_URI: String = "content://media/external/audio/albumart"

        /**
         * This Composes the [MediaStore.Audio.Media.EXTERNAL_CONTENT_URI] from the provided Album [id]
         */
        fun toAlbumArtUri(id: Long): Uri = ContentUris.withAppendedId(Uri.parse(ALBUM_ART_URI), id)
    }

    init {
        // ensure that playlist favourites exists.
        GlobalScope.launch {
            if (!playlistz.exists(Playback.PLAYLIST_FAVOURITE)) playlistz.insert(
                Playlist(
                    Playback.PLAYLIST_FAVOURITE
                )
            )
        }
    }

    /**
     * Returns the [MediaStore.Audio.Media.ALBUM_ID] upto [limit] as flow.
     */
    fun recent(limit: Int) =
        resolver.observe(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
            .map {
                resolver.query2(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    arrayOf(MediaStore.Audio.Media.ALBUM_ID),
                    order = MediaStore.Audio.Media.DATE_MODIFIED,
                    limit = limit,
                ) { c -> Array(c.count) { c.moveToPosition(it); c.getLong(0) } } ?: emptyArray()
            }

    // TODO: Replace this with just uri or something as future versions of this app will cause problems
    val favourite: Flow<List<Audio>?>
        get() = playlistz.observe2(Playback.PLAYLIST_FAVOURITE)
            .map {
                it.mapNotNull { getAudioById(it.id.toLong()) }
            }

    /**
     * The playlists excluding the special ones like [PLAYLIST_RECENT] etc.
     */
    val playlists =
        playlistz
            .observe()
            .map { playlists ->
                // drop private playlists.
                playlists.dropWhile { it.name.indexOf(Playlists.PRIVATE_PLAYLIST_PREFIX) == 0 }
            }


    val folders = resolver.folders()
    val artists = resolver.artists()
    val albums = resolver.albums()
    val genres = resolver.genres()
    fun audios(query: String? = null) = resolver.audios(query)
    fun folder(path: String) = resolver.folder(path)
    fun genre(name: String) = resolver.genre(name)
    fun artist(name: String) = resolver.artist(name)
    fun album(title: String) = resolver.album(title)
    fun playlist(id: Long): Flow<List<Audio>> =
        playlistz.observe2(id).map { it.mapNotNull { getAudioById(it.id.toLong()) } }

    fun playlist(name: String) =
        playlistz.observe2(name).map { it.mapNotNull { getAudioById(it.id.toLong()) } }

    fun getPlaylist(name: String) = runBlocking { playlistz.get(name) }

    /**
     * @return [Audio] specified by [id] or null
     */
    @WorkerThread
    fun getAudioById(id: Long) = runBlocking { resolver.findAudio(id) }

    @WorkerThread
    fun isFavourite(audioID: Long): Boolean =
        runBlocking {
            val id = playlistz
                .get(Playback.PLAYLIST_FAVOURITE)?.id ?: return@runBlocking false
            val key = toAudioTrackUri(audioID).toString()
            playlistz.exists(playlistId = id, key)
        }

    suspend fun exists(playlistName: String): Boolean = playlistz.get(playlistName) != null

    /**
     * Creates playlist if not exist otherwise returns the -1L
     */
    suspend fun createPlaylist(
        name: String,
        desc: String = ""
    ): Long =
        when {
            exists(playlistName = name) -> -1L
            else -> playlistz.insert(Playlist(name = name, desc = desc))
        }

    suspend fun deletePlaylist(playlist: Playlist): Boolean =
        playlistz.delete(playlist) == 1

    suspend fun updatePlaylist(value: Playlist): Boolean = playlistz.update(value) == 1

    suspend fun addToPlaylist(
        audioID: Long,
        name: String
    ): Boolean {
        val playlistsDb = playlistz
        val id = playlistsDb.get(name)?.id ?: createPlaylist(name)
        //TODO: Update dateModified.
        val older = playlistsDb.lastPlayOrder(id) ?: 0
        val audio = getAudioById(audioID) ?: return false
        val member = audio.toMember(id, older + 1)
        val memberId = playlistsDb.insert(member)
            .also {
                val playlist = playlistsDb.get(id) ?: return false
                if (it != -1L)
                    playlistsDb.update(playlist.copy(dateModified = System.currentTimeMillis()))
            }
        return memberId != -1L
    }

    suspend fun addToPlaylist(audios: List<Long>, name: String): List<Boolean> {
        return audios.map {
            addToPlaylist(it, name)
        }
    }


    /**
     * Remove from playlist if first [Playlist] found and then if successfully removed.
     */
    suspend fun removeFromPlaylist(name: String, audioID: Long): Boolean {
        val playlistsDb = playlistz
        val playlist = playlistsDb.get(name) ?: return false
        val key = toAudioTrackUri(audioID).toString()
        val count = playlistsDb.delete(playlist.id, key)
            .also {
                if (it == 1)
                    playlistsDb.update(playlist.copy(dateModified = System.currentTimeMillis()))
            }
        return count == 1
    }

    suspend fun toggleFav(audioID: Long): Boolean {
        val favourite = isFavourite(audioID = audioID)
        val op = if (favourite)
            removeFromPlaylist(Playback.PLAYLIST_FAVOURITE, audioID)
        else
            addToPlaylist(audioID, Playback.PLAYLIST_FAVOURITE,)
        return !favourite && op
    }
}
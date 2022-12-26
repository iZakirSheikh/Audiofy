package com.prime.player.core

import android.content.ContentResolver
import android.provider.MediaStore
import androidx.annotation.WorkerThread
import com.prime.player.Audiofy
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "Repository"


private val PLAYLIST_RECENT = Audiofy.PRIVATE_PLAYLIST_PREFIX + "recent"
private val PLAYLIST_FAV = Audiofy.PLAYLIST_FAVOURITES

private const val CAROUSEL_LIMIT = 30

@Singleton
class Repository @Inject constructor(
    private val localDb: Playlists,
    private val resolver: ContentResolver
) {
    /**
     * The playlists excluding the special ones like [PLAYLIST_RECENT] etc.
     */
    val playlists =
        localDb
            .playlists()
            .map { playlists ->
                // drop private playlists.
                playlists.dropWhile {
                    it.name.indexOf(Audiofy.PRIVATE_PLAYLIST_PREFIX) == 0
                }
            }


    val folders = resolver.folders()
    val artists = resolver.artists()
    val albums = resolver.albums()
    val genres = flow<List<Genre>> {
        emit(emptyList())
    }

    fun audios(query: String? = null) = resolver.audios(query)
    fun folder(path: String) = resolver.folder(path)
    fun genre(name: String) = resolver.genre(name)
    fun artist(name: String) = resolver.artist(name)
    fun album(title: String) = resolver.album(title)
    fun playlist(id: Long): Flow<List<Audio>> = localDb.playlist(
        id
    ).map {
        it.mapNotNull {
            getAudioById(it.id.toLong())
        }
    }

    val favourite: Flow<List<Audio>?>
        get() = localDb
            .playlist(
                runBlocking {
                    localDb.get(PLAYLIST_FAV)?.id ?: createPlaylist(PLAYLIST_FAV)
                }
            )
            .map {
                it.mapNotNull {
                    getAudioById(it.id.toLong())
                }
            }


    val carousel = resolver.observe(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI).map {
        resolver.query2(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            arrayOf(
                MediaStore.Audio.Media.ALBUM_ID
            ),
            order = "${MediaStore.Audio.Media.DATE_MODIFIED} DESC LIMIT $CAROUSEL_LIMIT OFFSET 0"
        ) { c ->
            List(c.count) {
                c.moveToPosition(it)
                c.getLong(0)
            }
        } ?: emptyList()
    }

    fun getPlaylist(name: String) = runBlocking { localDb.get(name) }

    @WorkerThread
    fun getAudioById(id: Long) =
        runBlocking {
            resolver.findAudio(id)
        }

    @WorkerThread
    fun isFavourite(audioID: Long): Boolean =
        runBlocking {
            val id =
                localDb
                    .get(PLAYLIST_FAV)?.id
                    ?: return@runBlocking false
            localDb.exists(
                playlistId = id,
                Audiofy.toAudioTrackUri(audioID).toString()
            )
        }

    /**
     * Creates playlist if not exist otherwise returns the -1L
     */
    suspend fun createPlaylist(
        name: String,
        desc: String = ""
    ): Long =
        when {
            exists(playlistName = name) -> -1L
            else ->
                localDb
                    .insert(
                        Playlist(
                            name = name,
                            desc = desc,
                            dateCreated = System.currentTimeMillis(),
                            dateModified = System.currentTimeMillis(),
                        )
                    )
        }

    suspend fun exists(playlistName: String): Boolean =
        localDb
            .get(playlistName) != null

    suspend fun deletePlaylist(playlist: Playlist): Boolean =
        localDb
            .delete(playlist) == 1

    suspend fun updatePlaylist(value: Playlist): Boolean =
        localDb.update(value) == 1

    suspend fun addToPlaylist(
        audioID: Long,
        name: String
    ): Boolean {
        val playlistsDb = localDb
        val id = playlistsDb.get(name)?.id ?: createPlaylist(name)
        //TODO: Update dateModified.
        val older = playlistsDb.lastPlayOrder(id) ?: 0
        val audio = getAudioById(audioID) ?: return false
        val member = Playlist.Member(
            id,
            "$audioID",
            order = older + 1,
            Audiofy.toAudioTrackUri(audioID).toString(),
            audio.name,
            audio.artist,
            Audiofy.toAlbumArtUri(id).toString()
        )

        val memberId = localDb
            .insert(member)
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
        val playlistsDb = localDb
        val playlist = playlistsDb.get(name) ?: return false
        val count = playlistsDb.delete(playlist.id, "$audioID")
            .also {
                if (it == 1)
                    playlistsDb.update(playlist.copy(dateModified = System.currentTimeMillis()))
            }
        return count == 1
    }

    suspend fun toggleFav(audioID: Long): Boolean {
        val favourite = isFavourite(audioID = audioID)
        val op = if (favourite)
            removeFromPlaylist(PLAYLIST_FAV, audioID)
        else
            addToPlaylist(audioID, PLAYLIST_FAV)
        return !favourite && op
    }
}
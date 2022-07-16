package com.prime.player.core

import android.content.Context
import android.graphics.BitmapFactory
import androidx.annotation.WorkerThread
import com.prime.player.Tokens
import com.primex.core.runCatching
import com.primex.preferences.Preferences
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "Repository"

private const val PLAYLIST_UNIQUE_TAG = "local_audios"

private const val PLAYLIST_RECENT = "_recent"

private const val CAROUSEL_LIMIT = 30

@Singleton
class Repository @Inject constructor(
    //TODO: Remove this; only required for init SyncWorker
    @ApplicationContext private val context: Context,
    private val audiosDb: Audios,
    private val playlistsDb: Playlists,
    preferences: Preferences
) {

    init {
        // run after every cold start
        //TODO: I guess required only if API < N
        SyncWorker.run(context)
    }


    /**
     * The reel is a [Pair ] of Duration and Bitmap.
     */
    val reel =
        preferences[SyncWorker.KEY_AUDIO_POSTER_MILLS].map {
            if (it == null)
                return@map null
            // just ignore the version as it is none of my use currently
            // just re-fetch the poster from storage.
            runCatching(TAG) {
                val bitmap =
                    context.openFileInput(SyncWorker.KEY_AUDIO_POSTER_MILLS.name)
                        .use { fis ->
                            BitmapFactory.decodeStream(fis)
                        }
                if (bitmap == null)
                    null
                else
                    it to bitmap
            }
        }

    /**
     * The carousal of Albums.
     */
    val carousel =
        audiosDb.observeRecentAlbums(CAROUSEL_LIMIT)

    /**
     * The recent playlist.
     */
    val recent =
        playlistsDb.observe2(PLAYLIST_RECENT, PLAYLIST_UNIQUE_TAG)
            .map { map ->
                when (map) {
                    null -> emptyList()
                    else -> map.second
                }
            }

    /**
     * The favourite playlist.
     */
    val favourite =
        playlistsDb.observe2(Tokens.Audio.PLAYLIST_FAVOURITES, PLAYLIST_UNIQUE_TAG)

    /**
     * A second favorite that returns only the audio files in the [Playlist]
     */
    val favouriteList =
        favourite.map {
            if (it == null)
                return@map emptyList()
            it.second
        }

    /**
     * The playlists excluding the special ones like [PLAYLIST_RECENT] etc.
     */
    val playlists =
        playlistsDb.observe().map { playlists ->
            // drop system default playlists.
            playlists.dropWhile {
                it.name == Tokens.Audio.PLAYLIST_FAVOURITES || it.name == PLAYLIST_RECENT
            }
        }


    /**
     * Returns all the buckets
     */
    val folders =
        audiosDb.buckets()

    /**
     * Returns a [Flow]able list of all [Artist]s
     */
    val artists =
        audiosDb.artists()

    /**
     * Returns a [Flow]able list of all [Album]s
     */
    val albums =
        audiosDb.albums()

    /**
     * Returns a [Flow]able list of all [Genre]s
     */
    val genres =
        audiosDb.genres()

    /**
     * @return [Flow]able list of [Audio] matched against [query] paired with [Audio.Info]
     */
    fun audios2(query: String? = null) =
        audiosDb.observe(query = query).map {
            val info = audiosDb.getInfo()
            info to it
        }

    /**
     * @return [Flow]able list of [Audio] matched against [query]
     */
    fun audios(query: String? = null) =
        audiosDb.observe(query)

    /**
     * Returns [Bucket] represented by [path] paired with its content
     */
    fun folder(path: String) =
        audiosDb.bucket2(path)

    /**
     * Returns a [Flow]able pair of [Genre] paired with its content.
     */
    fun genre(name: String) =
        audiosDb.genre2(name)

    /**
     * Returns a [Flow]able pair of [Audio.Artist] paired with its content.
     */
    fun artist(name: String) =
        audiosDb.artist2(name)

    /**
     * Returns a [Flow]able pair of [Audio.Album] paired with its content.
     */
    fun album(title: String) =
        audiosDb.albums2(title)

    /**
     * Returns a [Flow]able pair of [Playlist] paired with its content.
     */
    fun playlist(name: String) =
        playlistsDb.observe2(name, PLAYLIST_UNIQUE_TAG)


    @WorkerThread
    fun getAudioById(id: Long) =
        runBlocking {
            audiosDb.get(id)
        }

    @WorkerThread
    fun isFavourite(audioID: Long): Boolean =
        runBlocking {
            playlistsDb.exists(
                Tokens.Audio.PLAYLIST_FAVOURITES,
                PLAYLIST_UNIQUE_TAG,
                "$audioID"
            )
        }

    /**
     * Remove from playlist if first [Playlist] found and then if successfully removed.
     */
    fun removeFromPlaylist(name: String, audioID: Long): Int =
        runBlocking {
            val playlist = playlistsDb.get(PLAYLIST_UNIQUE_TAG, name) ?: return@runBlocking 0
            playlistsDb.delete(playlist.id, "$audioID").also {
                if (it == 1)
                    playlistsDb.update(playlist.copy(dateModified = System.currentTimeMillis()))
            }
        }

    @WorkerThread
    fun toggleFav(audioID: Long) {
        if (isFavourite(audioID = audioID))
            removeFromPlaylist(Tokens.Audio.PLAYLIST_FAVOURITES, audioID)
        else
            addToPlaylist(audioID, Tokens.Audio.PLAYLIST_FAVOURITES)
    }

    @WorkerThread
    fun createPlaylist(name: String): Long? {
        return runBlocking {
            playlistsDb.insert(
                Playlist(
                    name = name,
                    desc = "",
                    dateCreated = System.currentTimeMillis(),
                    dateModified = System.currentTimeMillis(),
                    tag = PLAYLIST_UNIQUE_TAG
                )
            )
        }
    }

    @WorkerThread
    fun addToPlaylist(audioID: Long, name: String) {
        runBlocking {
            val id = playlistsDb.get(PLAYLIST_UNIQUE_TAG, name)?.id ?: createPlaylist(name)!!

            //TODO: Update dateModified.
            val older = playlistsDb.lastPlayOrder(id) ?: 0
            playlistsDb.insert(
                Playlist.Member(id, "$audioID", order = older + 1)
            ).also {
                val playlist = playlistsDb.get(id) ?: return@runBlocking
                if (it != -1L)
                    playlistsDb.update(playlist.copy(dateModified = System.currentTimeMillis()))
            }
        }
    }

    @WorkerThread
    fun addToPlaylist(audios: List<Long>, name: String) {
        runBlocking {
            val id = playlistsDb.get(PLAYLIST_UNIQUE_TAG, name)?.id ?: createPlaylist(name)!!
            //TODO: Update dateModified.
            var older = playlistsDb.lastPlayOrder(id) ?: 0
            val members = audios.map {
                Playlist.Member(id, "$it", order = older++)
            }
            playlistsDb.insert(members)
        }
    }


    suspend fun exists(playlistName: String): Boolean =
        playlistsDb.get(PLAYLIST_UNIQUE_TAG, playlistName) != null

    @WorkerThread
    fun deletePlaylist(playlist: Playlist): Boolean {
        return runBlocking {
            playlistsDb.delete(playlist) > 0
        }
    }

    suspend fun updatePlaylist(value: Playlist): Boolean =
        playlistsDb.update(value) == 1

    @WorkerThread
    fun addToRecent(audioID: Long) {
        runBlocking {
            //TODO: Update Working.
            val playlistId =
                playlistsDb.get(PLAYLIST_UNIQUE_TAG, PLAYLIST_RECENT)?.id
                    ?: createPlaylist(PLAYLIST_RECENT)
                    ?: return@runBlocking
            val playlist = playlistsDb.get(PLAYLIST_UNIQUE_TAG, PLAYLIST_RECENT)!!
                .copy(dateModified = System.currentTimeMillis())

            val member = Playlist.Member(
                playlistId,
                "$audioID",
                0
            )

            playlistsDb.insert(
                member
            )
            playlistsDb.update(playlist)
        }
    }
}
package com.prime.player.core

import android.content.Context
import android.graphics.BitmapFactory
import androidx.annotation.WorkerThread
import com.prime.player.Audiofy
import com.primex.core.runCatching
import com.primex.preferences.Preferences
import com.primex.preferences.value
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "Repository"

private const val PLAYLIST_UNIQUE_TAG = "local_audios"

private val PLAYLIST_RECENT = Audiofy.PRIVATE_PLAYLIST_PREFIX + "recent"
private val PLAYLIST_FAV = Audiofy.PLAYLIST_FAVOURITES

private const val CAROUSEL_LIMIT = 30

@Singleton
class Repository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val localDb: LocalDb,
    private val preferences: Preferences
) {

    //TODO: Find suitable for executing SyncWorker.
    init {
        SyncWorker.run(context)
    }

    /**
     * Returns the image associated with the reel
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


    val recent: Flow<List<Audio>?>
        get() = localDb
            .audios
            .playlist(
                runBlocking {
                    localDb.playlists.get(PLAYLIST_UNIQUE_TAG, PLAYLIST_RECENT)?.id
                        ?: createPlaylist(
                            PLAYLIST_RECENT
                        )
                }
            )

    val favourite: Flow<List<Audio>?>
        get() = localDb
            .audios
            .playlist(
                runBlocking {
                    localDb.playlists.get(PLAYLIST_UNIQUE_TAG, PLAYLIST_FAV)?.id ?: createPlaylist(
                        PLAYLIST_FAV
                    )
                }
            )

    /**
     * The carousal of Albums.
     */
    val carousel =
        localDb.audios.observeRecentAlbums(CAROUSEL_LIMIT)

    /**
     * The playlists excluding the special ones like [PLAYLIST_RECENT] etc.
     */
    val playlists =
        localDb
            .playlists
            .observe()
            .map { playlists ->
                // drop private playlists.
                playlists.dropWhile {
                    it.name.indexOf(Audiofy.PRIVATE_PLAYLIST_PREFIX) == 0
                }
            }


    /**
     * Returns all the buckets
     */
    val folders = localDb.audios.buckets()

    /**
     * Returns a [Flow]able list of all [Artist]s
     */
    val artists = localDb.audios.artists()

    /**
     * Returns a [Flow]able list of all [Album]s
     */
    val albums = localDb.audios.albums()

    /**
     * Returns a [Flow]able list of all [Genre]s
     */
    val genres = localDb.audios.genres()

    /**
     * @return [Flow]able list of [Audio] matched against [query]
     */
    fun audios(query: String? = null) = localDb.audios.observe(query = query)

    /**
     * Returns [Bucket] represented by [path] paired with its content
     */
    fun folder(path: String) = localDb.audios.bucket(path)

    /**
     * Returns a [Flow]able pair of [Genre] paired with its content.
     */
    fun genre(name: String) = localDb.audios.genre(name)

    /**
     * Returns a [Flow]able pair of [Audio.Artist] paired with its content.
     */
    fun artist(name: String) = localDb.audios.artist(name)

    /**
     * Returns a [Flow]able pair of [Audio.Album] paired with its content.
     */
    fun album(title: String) = localDb.audios.album(title)

    /**
     * Returns a [Flow]able pair of [Playlist] paired with its content.
     */
    fun playlist(id: Long): Flow<List<Audio>> = localDb.audios.playlist(id)

    fun getPlaylist(name: String) = runBlocking { localDb.playlists.get(PLAYLIST_UNIQUE_TAG, name) }

    @WorkerThread
    fun getAudioById(id: Long) =
        runBlocking {
            localDb.audios.get(id)
        }

    @WorkerThread
    fun isFavourite(audioID: Long): Boolean =
        runBlocking {
            val id =
                localDb
                    .playlists
                    .get(PLAYLIST_UNIQUE_TAG, PLAYLIST_FAV)?.id
                    ?: return@runBlocking false
            localDb.members.exists(
                playlistId = id,
                "$audioID"
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
                    .playlists
                    .insert(
                        Playlist(
                            name = name,
                            desc = desc,
                            dateCreated = System.currentTimeMillis(),
                            dateModified = System.currentTimeMillis(),
                            tag = PLAYLIST_UNIQUE_TAG
                        )
                    )
        }

    suspend fun exists(playlistName: String): Boolean =
        localDb
            .playlists
            .get(PLAYLIST_UNIQUE_TAG, playlistName) != null

    suspend fun deletePlaylist(playlist: Playlist): Boolean =
        localDb
            .playlists
            .delete(playlist) == 1

    suspend fun updatePlaylist(value: Playlist): Boolean =
        localDb.playlists.update(value) == 1

    suspend fun addToPlaylist(
        audioID: Long,
        name: String
    ): Boolean {
        val playlistsDb = localDb.playlists
        val id = playlistsDb.get(PLAYLIST_UNIQUE_TAG, name)?.id ?: createPlaylist(name)
        //TODO: Update dateModified.
        val older = playlistsDb.lastPlayOrder(id) ?: 0
        val member = Playlist.Member(id, "$audioID", order = older + 1)

        val memberId = localDb
            .members
            .insert(member)
            .also {
                val playlist = playlistsDb.get(id) ?: return false
                if (it != -1L)
                    playlistsDb.update(playlist.copy(dateModified = System.currentTimeMillis()))
            }
        return memberId != -1L
    }

    suspend fun addToPlaylist(audios: List<Long>, name: String): List<Boolean> {
        val id = localDb.playlists.get(PLAYLIST_UNIQUE_TAG, name)?.id ?: createPlaylist(name)
        //TODO: Update dateModified.
        var older = localDb.playlists.lastPlayOrder(id) ?: 0
        val members = audios.map {
            Playlist.Member(id, "$it", order = older++)
        }
        return localDb.members.insert(members).map {
            it != -1L
        }
    }


    /**
     * Remove from playlist if first [Playlist] found and then if successfully removed.
     */
    suspend fun removeFromPlaylist(name: String, audioID: Long): Boolean {
        val playlistsDb = localDb.playlists
        val members = localDb.members
        val playlist = playlistsDb.get(PLAYLIST_UNIQUE_TAG, name) ?: return false
        val count = members.delete(playlist.id, "$audioID")
            .also {
                if (it == 1)
                    playlistsDb.update(playlist.copy(dateModified = System.currentTimeMillis()))
            }
        return count == 1
    }

    fun toggleFav(audioID: Long) {
        GlobalScope.launch {
            if (isFavourite(audioID = audioID))
                removeFromPlaylist(PLAYLIST_FAV, audioID)
            else
                addToPlaylist(audioID, PLAYLIST_FAV)
        }
    }

    fun addToRecent(audioID: Long) {
        //TODO make addToRecent suspend
        GlobalScope.launch {

            val playlistId =
                localDb
                    .playlists
                    .get(PLAYLIST_UNIQUE_TAG, PLAYLIST_RECENT)?.id
                    ?: createPlaylist(PLAYLIST_RECENT)
            // here two cases arise
            // case 1 the member already exists:
            // in this case we just have to update the order and nothing else
            // case 2 the member needs to be inserted.
            // In both cases the playlist's dateModified needs to be updated.
            val playlist = localDb.playlists.get(playlistId)!!
            localDb.playlists.update(playlist = playlist.copy(dateModified = System.currentTimeMillis()))

            val member = localDb.members.get(playlistId, "$audioID")

            when (member != null) {
                // exists
                true -> {
                    //localDb.members.update(member.copy(order = 0))
                    // updating the member doesn't work as expected.
                    // localDb.members.delete(member)
                    localDb.members.update(member = member.copy(order = 0))
                }
                else -> {
                    // check the limit in this case
                    val limit = preferences.value(Audiofy.MAX_RECENT_PLAYLIST_SIZE).toLong() - 1
                    // delete above member
                    localDb.members.delete(playlistId, limit)
                    localDb.members.insert(
                        Playlist.Member(playlistId, "$audioID", 0)
                    )
                }
            }
        }
    }
}

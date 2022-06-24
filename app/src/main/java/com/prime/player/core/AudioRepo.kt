package com.prime.player.core

import android.app.Application
import android.content.ContentResolver
import android.content.Context
import android.database.ContentObserver
import android.net.Uri
import android.provider.MediaStore
import androidx.annotation.WorkerThread
import com.prime.player.core.models.*
import com.prime.player.core.playback.UIHandler
import com.prime.player.preferences.Preferences
import com.prime.player.preferences.recentSize
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.json.JSONArray
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext


class AudioRepo private constructor(context: Application) {

    private val lock = Mutex()

    private val scope = object : CoroutineScope {
        fun sync(
            context: CoroutineContext = EmptyCoroutineContext,
            start: CoroutineStart = CoroutineStart.DEFAULT,
            block: suspend CoroutineScope.() -> Unit
        ) {
            launch(context = context, start = start) {
                lock.withLock {
                    block()
                }
            }
        }

        override val coroutineContext: CoroutineContext
            get() = SupervisorJob() + Dispatchers.Main
    }

    private val REFRESH: Int = UIHandler.generateToken()

    private val handler = UIHandler.get()
    val callback = UIHandler.Callback { message ->
        if (message.what == REFRESH) {
            refresh()
            true
        } else
            false
    }.also {
        handler.addCallback(it)
    }

    private val playlistDb = LocalPlaylists.get(context = context)


    val audios: StateFlow<List<Audio>> = MutableStateFlow(emptyList())
    val artists: StateFlow<List<Artist>> = MutableStateFlow(emptyList())
    val albums: StateFlow<List<Album>> = MutableStateFlow(emptyList())
    val folders: StateFlow<List<Folder>> = MutableStateFlow(emptyList())
    val genres: StateFlow<List<Genre>> = MutableStateFlow(emptyList())
    val playlists: StateFlow<List<Playlist>> =
        playlistDb.getPlaylists()
            .stateIn(scope, SharingStarted.WhileSubscribed(STATE_TIMEOUT), emptyList())

    private val prefs = Preferences.get(context = context)

    val recent = prefs.getString(KEY_PREF_RECENT, "").combine(audios) { stringfied, audios ->
        lock.withLock {
            when {
                stringfied.isBlank() || audios.isEmpty() -> emptyList()
                else -> {
                    val ids = JSONArray(stringfied)
                    val list = ArrayList<Audio>()
                    for (i in 0 until ids.length()) {
                        val id = ids.getLong(i)
                        val audio = audios.find { it.id == id }
                        if (audio != null)
                            list.add(audio)
                    }
                    list.reversed()
                }
            }
        }
    }.stateIn(scope, SharingStarted.WhileSubscribed(STATE_TIMEOUT), null)

    private val observer = object : ContentObserver(null) {
        override fun onChange(selfChange: Boolean) {
            onChange(selfChange, null)
        }

        override fun onChange(selfChange: Boolean, uri: Uri?) {
            if (handler.hasMessages(REFRESH))
                handler.removeMessages(REFRESH)
            handler.sendEmptyMessageDelayed(REFRESH, 500)
        }
    }

    private val resolver = context.contentResolver.also {
        it.registerContentObserver(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            true,
            observer
        )
        it.registerContentObserver(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            true,
            observer
        )
        it.registerContentObserver(
            MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
            true,
            observer
        )
        it.registerContentObserver(
            MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI,
            true,
            observer
        )
    }

    init {
        handler.sendEmptyMessage(REFRESH)
    }

    private fun refresh() {
        scope.sync(Dispatchers.IO) {
            with(resolver) {
                val a1 = getArtists()
                val a2 = getAlbums()
                val a3 = getAudios(a1, a2)

                //emit
                (artists as MutableStateFlow).emit(a1)
                (albums as MutableStateFlow).emit(a2)
                (audios as MutableStateFlow).emit(a3)

                //
                (genres as MutableStateFlow).emit(getGenres(a3))
                (folders as MutableStateFlow).emit(getFolders(a3))
            }
        }
    }


    fun release() {
        scope.cancel()
        resolver.unregisterContentObserver(observer)
        handler.removeUpdateCallback(callback)
    }

    @WorkerThread
    fun getAudioById(id: Long) = runBlocking {
        lock.withLock {
            audios.value.find {
                id == it.id
            }
        }
    }

    @WorkerThread
    fun isFavourite(audioID: Long): Boolean {
        return playlistDb.getPlaylistByName(PLAYLIST_FAVORITES)?.audios?.contains(audioID)
            ?: return false
    }

    /**
     * Remove from playlist if first [Playlist] found and then if successfully removed.
     */
    fun removeFromPlaylist(name: String, audioID: Long) {
        playlistDb.getPlaylistByName(name)?.let {
            val removed = (it.audios as MutableList).remove(audioID)
            if (removed)
                playlistDb.update(it)
        }
    }

    fun toggleFav(audioID: Long) {
        if (isFavourite(audioID = audioID))
            removeFromPlaylist(PLAYLIST_FAVORITES, audioID)
        else
            addToPlaylist(audioID, PLAYLIST_FAVORITES)
    }


    @WorkerThread
    fun createPlaylist(name: String): Long? {
        return runBlocking {
            lock.withLock {
                val playlists = playlists.value
                playlists.find { it.name == name } ?: kotlin.run {
                    return@withLock playlistDb.insert(Playlist(0, name))
                }
                return@withLock null
            }
        }
    }

    fun addToPlaylist(audioID: Long, name: String) {
        val playlist = playlistDb.getPlaylistByName(name) ?: kotlin.run {
            val playlist = Playlist(0, name)
            val id = playlistDb.insert(playlist)
            playlist.copy(id)
        }
        val newLst = playlist.audios as MutableList
        if (!newLst.contains(audioID)) {
            newLst.add(audioID)
            playlistDb.update(playlist.copy(updateTime = System.currentTimeMillis()))
        }
    }

    fun addToPlaylist(audios: List<Long>, name: String) {
        val playlist = playlistDb.getPlaylistByName(name) ?: kotlin.run {
            val playlist = Playlist(0, name)
            val id = playlistDb.insert(playlist)
            playlist.copy(id)
        }
        val newList = playlist.audios as MutableList
        audios.forEach {
            if (!newList.contains(it))
                newList.add(it)
        }
        playlistDb.update(playlist.copy(updateTime = System.currentTimeMillis()))
    }

    fun deletePlaylist(playlist: Playlist): Boolean {
        return playlistDb.delete(playlist.id) != 0
    }

    fun addToRecent(audioID: Long) {
        scope.launch(Dispatchers.IO) {
            with(prefs) {
                val array = getString(KEY_PREF_RECENT, "").transform { value ->
                    emit(if (value.isEmpty()) JSONArray() else JSONArray(value))
                }.collectBlocking()
                var index = -1
                for (i in 0 until array.length()) {
                    if (array.getLong(i) == audioID)
                        index = i
                }
                if (index != -1) array.remove(index)
                array.put(audioID)
                val size = recentSize().collectBlocking().let {
                    if (it == -1)
                        RECENT_SIZE
                    else
                        it
                }
                while (array.length() > size)
                    array.remove(0)
                setString(KEY_PREF_RECENT, array.toString())
            }
        }
    }


    companion object {
        const val NO_TITLE = "Unknown"
        const val INVALID_TITLE = "<unknown>"

        private const val TAG = "AudioRepo"
        private const val KEY_PREF_RECENT = TAG + "_recent"
        const val PLAYLIST_FAVORITES = "Favorites"

        private const val RECENT_SIZE = 30
        private val STATE_TIMEOUT = TimeUnit.SECONDS.toMillis(30)

        // Singleton prevents multiple instances of repository opening at the
        // same time.
        @Volatile
        private var INSTANCE: AudioRepo? = null

        @JvmStatic
        fun get(context: Context): AudioRepo {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the repository
            return INSTANCE ?: synchronized(this) {
                val instance = AudioRepo(context.applicationContext as Application)
                INSTANCE = instance
                instance
            }
        }
    }
}

private fun ContentResolver.getAudios(artists: List<Artist>, albums: List<Album>): List<Audio> {
    val list = ArrayList<Audio>()
    val contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.MIME_TYPE,
        MediaStore.Audio.Media.TITLE,
        MediaStore.Audio.Media.TRACK,
        MediaStore.Audio.Media.YEAR,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DATA,
        MediaStore.Audio.Media.DATE_MODIFIED,
        MediaStore.Audio.Media.ARTIST_ID,
        MediaStore.Audio.Media.ALBUM_ID
    )
    val selection = MediaStore.Audio.Media.IS_MUSIC + " != 0"
    //val sortOrder = MediaStore.Audio.Media.DATE_MODIFIED
    query(contentUri, projection, selection, null, null)?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val mimeTypeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)
        val artistIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
        val albumIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
        val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
        val trackColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TRACK)
        val yearColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.YEAR)
        val durationColumn = cursor.getColumnIndex(MediaStore.Audio.Media.DURATION)
        val data = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)
        val dateColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)

        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val mimeType = cursor.getString(mimeTypeColumn)
            val duration = cursor.getInt(durationColumn)
            val title = cursor.getString(titleColumn).replaceMaybe()
            val trackNo = cursor.getInt(trackColumn)
            val year = cursor.getInt(yearColumn)
            val path = if (data != -1) cursor.getString(data) else null
            val modified = cursor.getLong(dateColumn)

            //
            val audio = Audio(id, title, mimeType, duration).apply {
                trackNumber = trackNo
                this.year = year
                this.path = path
                this.dateModified = modified
            }

            list.add(audio)
            if (!cursor.isNull(albumIdColumn)) {
                val albumid = cursor.getLong(albumIdColumn)
                val album = albums.find { it.id == albumid }
                album?.audioList?.add(audio)
                audio.album = album
            }

            if (!cursor.isNull(artistIdColumn)) {
                val artistId = cursor.getLong(artistIdColumn)
                val artist = artists.find { it.id == artistId }
                audio.artist = artist
                artist?.audioList?.add(audio)
                audio.album?.let { it1 ->
                    if (artist?.albumList?.contains(it1) == false) artist.albumList.add(it1)
                }
            }
            audio.album?.artist = audio.artist
        }
    }
    return list
}

private fun ContentResolver.getGenres(audios: List<Audio>): List<Genre> {
    val list = ArrayList<Genre>()
    val contentUri = MediaStore.Audio.Genres.EXTERNAL_CONTENT_URI
    val projection = arrayOf(MediaStore.Audio.Genres._ID, MediaStore.Audio.Genres.NAME)
    //val sortOrder = MediaStore.Audio.Genres.DEFAULT_SORT_ORDER
    query(contentUri, projection, null, null, null)?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Genres._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.NAME)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            // empty name if null
            val name = cursor.getString(nameColumn).replaceMaybe()
            val genre = Genre(id, name)
            list.add(genre)
            genre.loadAudios(this, audios)
        }
    }
    return list
}


private fun Genre.loadAudios(resolver: ContentResolver, audios: List<Audio>) {
    val contentUri = MediaStore.Audio.Genres.Members.getContentUri("external", id)
    val projection = arrayOf(MediaStore.Audio.Genres.Members.AUDIO_ID)
    resolver.query(contentUri, projection, null, null, null)?.use { cursor ->
        val audioIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Genres.Members.AUDIO_ID)
        while (cursor.moveToNext()) {
            val audioId = cursor.getLong(audioIdColumn)
            audios.find { it.id == audioId }?.also { audio ->
                this.audios.add(audio)
                audio.genre = this
            }
        }
    }
}

private fun ContentResolver.getAlbums(): List<Album> {
    val list = ArrayList<Album>()
    val contentUri = MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI
    // TODO MediaStore.Audio.Albums.ARTIST_ID
    val projection = arrayOf(
        MediaStore.Audio.Albums._ID,
        MediaStore.Audio.Media.ARTIST_ID,
        MediaStore.Audio.Albums.ALBUM
    )
    //val sortOrder = MediaStore.Audio.Albums.DEFAULT_SORT_ORDER
    query(contentUri, projection, null, null, null)?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
        // TODO MediaStore.Audio.Albums.ARTIST_ID
        //val artistIdColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST_ID)
        val titleColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val name = cursor.getString(titleColumn).replaceMaybe()
            val album = Album(id, name)
            list.add(album)
        }
    }
    return list
}

private fun ContentResolver.getArtists(): List<Artist> {
    val list = ArrayList<Artist>()
    val contentUri = MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI
    val projection = arrayOf(MediaStore.Audio.Artists._ID, MediaStore.Audio.Artists.ARTIST)
    //val sortOrder = MediaStore.Audio.Artists.DEFAULT_SORT_ORDER
    query(contentUri, projection, null, null, null)?.use { cursor ->
        val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID)
        val nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)
        while (cursor.moveToNext()) {
            val id = cursor.getLong(idColumn)
            val name = cursor.getString(nameColumn).replaceMaybe()
            val artist = Artist(id, name = name)
            list.add(artist)
        }
    }
    return list
}


private fun getFolders(audios: List<Audio>): List<Folder> {
    val list = ArrayList<Folder>()
    audios.forEach { audio ->
        audio.path?.let { path ->
            val file = File(path).parentFile!!
            val folder = list.find { folder -> folder.name == file.name }
                ?: Folder(file.name.replaceMaybe(), file.path).also { list.add(it) }
            folder.audios.add(audio)
        } ?: run {
            val folder = list.find { folder -> folder.path.isEmpty() }
                ?: Folder(AudioRepo.NO_TITLE, "").also { list.add(it) }
            folder.audios.add(audio)
        }
    }
    return list
}

private fun String?.replaceMaybe() =
    if (this.isNullOrEmpty() || this == AudioRepo.INVALID_TITLE)
        AudioRepo.NO_TITLE
    else
        this.trim()

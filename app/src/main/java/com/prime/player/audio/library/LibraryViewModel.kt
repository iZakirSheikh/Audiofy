package com.prime.player.audio.library

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.util.Log
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prime.player.core.AudioRepo
import com.prime.player.core.models.Audio
import com.prime.player.extended.Quad
import com.prime.player.extended.Resource
import com.prime.player.extended.success
import com.prime.player.preferences.Preferences
import com.prime.player.utils.getAlbumArt
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.FileNotFoundException
import javax.inject.Inject

typealias Page = Quad<String, String, ImageVector?, Long>


private val EMPTY = Page("", "", null, -1L)
typealias PlaybackHistory = Resource<State<List<Audio>?>>

@HiltViewModel
class LibraryViewModel @Inject constructor(context: Application) : ViewModel() {

    private val repo = AudioRepo.get(context = context)

    val megaArt: StateFlow<Pair<Bitmap, Long>?> = repo.audios.transform { value ->
        val prefs = Preferences.get(context = context)

        // info is a string set of size 2
        // first one is size of audio when bitmap was computed
        // 2nd part contains the Animation duration
        var size = 0
        var duration = 30L * BITMAP_DURATION
        with(prefs) { getString(PREF_KEY_ARTWORK, "").collectBlocking() }.also { info ->
            if (info.isNotEmpty()) {
                val split = info.split(" ")
                size = split[0].toInt()
                duration = split[1].toLong()
            }
        }


        val saved = if (size != value.size) null else {
            var bitmap: Bitmap? = null
            try {
                val fis = context.openFileInput(PREF_KEY_ARTWORK)
                bitmap = BitmapFactory.decodeStream(fis)
                fis.close()
            } catch (e: FileNotFoundException) {
                Log.d(TAG, "$PREF_KEY_ARTWORK: not found")
            }
            bitmap
        }

        val result = saved ?: kotlin.run {
            // saved is empty recompute
            val bitmaps = ArrayList<Bitmap>()
            value.reversed().forEach { audio ->
                if (bitmaps.size >= 30)
                    return@forEach
                audio.album
                    ?.let { context.getAlbumArt(it.id, ARTWORK_SIZE) }
                    ?.let { bitmaps.add(it) }
            }
            bitmaps.shuffle()
            if (bitmaps.isNotEmpty()) {
                val width = bitmaps.size * ARTWORK_SIZE
                val height = ARTWORK_SIZE
                val background = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(background)
                var x = 0f
                bitmaps.forEach { bitmap ->
                    canvas.drawBitmap(bitmap, x, 0f, null)
                    x += ARTWORK_SIZE
                }
                size = value.size
                duration = 30L * BITMAP_DURATION
                //save in folder
                try {
                    val fos = context.openFileOutput(PREF_KEY_ARTWORK, Context.MODE_PRIVATE)
                    background.compress(Bitmap.CompressFormat.JPEG, 60, fos)
                    fos.close()
                    //save new key
                    with(prefs) { setString(PREF_KEY_ARTWORK, "$size $duration") }
                } catch (e: Exception) {
                    Log.d(TAG, "$PREF_KEY_ARTWORK: error saving in storage")
                }
                background
            } else null
        }
        result.run {
            emit(this to duration)
        }
    }.catch {
        Log.d(TAG, "${it.message}: ")
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.Lazily, null)

    // The audios
    val audios: StateFlow<Page> = repo.audios.transform { value ->
        val albumId = value.lastOrNull()?.album?.id ?: -1
        emit(
            Page(
                "Audios",
                "${value.size} items",
                Icons.Outlined.Audiotrack,
                albumId
            )
        )
    }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(), EMPTY)

    val artists: StateFlow<Page> = repo.artists.transform { value ->
        val albumId = value.lastOrNull()?.albumList?.lastOrNull()?.id ?: -1
        emit(
            Page(
                "Artists",
                "${value.size} items",
                Icons.Outlined.Person,
                albumId
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), EMPTY)

    val albums: StateFlow<Page> = repo.albums.transform { value ->
        val albumId = if (value.isNotEmpty()) value.last().id else -1L
        emit(
            Page(
                "Albums",
                "${value.size} items",
                Icons.Outlined.Album,
                albumId
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), EMPTY)

    val folders: StateFlow<Page> = repo.folders.transform { value ->
        val albumId = value.lastOrNull()?.audios?.lastOrNull()?.album?.id ?: -1L
        emit(
            Page(
                "Folders",
                "${value.size} items",
                Icons.Outlined.Folder,
                albumId
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), EMPTY)

    val genres: StateFlow<Page> = repo.genres.transform { value ->
        val albumId = value.lastOrNull()?.audios?.lastOrNull()?.id ?: -1
        emit(
            Page(
                "Genres",
                "${value.size} items",
                Icons.Default.Grain,
                albumId
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), EMPTY)


    val playlists: StateFlow<Page> = repo.playlists.transform { value ->
        val audio = value.sortedByDescending { it.updateTime }.lastOrNull()?.audios?.lastOrNull()
        val albumId = audio?.let { repo.getAudioById(it)?.album?.id ?: -1 } ?: -1
        emit(
            Page(
                "Playlists",
                "${value.size} items",
                Icons.Outlined.FeaturedPlayList,
                albumId
            )
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), EMPTY)

    val recent: PlaybackHistory = PlaybackHistory(mutableStateOf(null)).also { history ->
        viewModelScope.launch {
            repo.recent.collect { audios ->
                when (audios.isNullOrEmpty()) {
                    true -> history.empty("Empty!! No Recent Items")
                    else -> history.success(audios)
                }
            }
        }
    }

    companion object {
        private const val PREF_KEY_ARTWORK = "artwork"
        private const val TAG = "LibraryViewModel"

        private const val ARTWORK_SIZE = 256
        private const val BITMAP_DURATION = 4 * 1000

    }
}
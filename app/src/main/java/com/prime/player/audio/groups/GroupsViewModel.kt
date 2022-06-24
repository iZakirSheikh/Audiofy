package com.prime.player.audio.groups

import android.app.Application
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prime.player.audio.GroupOf
import com.prime.player.core.AudioRepo
import com.prime.player.core.models.*
import com.prime.player.extended.Resource
import com.prime.player.extended.castTo
import com.prime.player.extended.success
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject


typealias GrpResult = Resource<State<Map<String, List<Any>>?>>

@OptIn(FlowPreview::class)
@HiltViewModel
class GroupsViewModel @Inject constructor(context: Application) : ViewModel() {

    private val repo = AudioRepo.get(context)
    private lateinit var groupOf: GroupOf

    /**
     * The title of the group
     */
    lateinit var title: String

    val result: GrpResult = Resource(mutableStateOf(null))

    /**
     * Search channel
     * Query [String], Ascending [Boolean],
     */
    val channel: StateFlow<Pair<String, Boolean>> = MutableStateFlow("" to true)

    fun init(of: GroupOf) {
        if (::groupOf.isInitialized)
            return
        groupOf = of
        val list = when (of) {
            GroupOf.PLAYLISTS -> {
                title = "Playlists"
                repo.playlists
            }
            GroupOf.FOLDERS -> {
                title = "Folders"
                repo.folders
            }
            GroupOf.ARTISTS -> {
                title = "Artists"
                repo.artists
            }
            GroupOf.ALBUMS -> {
                title = "Albums"
                repo.albums
            }
            GroupOf.GENRES -> {
                title = "Genres"
                repo.genres
            }
            else -> error("No such Group type: $of")
        }
        viewModelScope.launch {
            channel
                .debounce(300)
                .distinctUntilChanged()
                .combineTransform(list) { (query, ascending), list ->
                    val grouped = when (of) {
                        GroupOf.ARTISTS -> filter(castTo<List<Artist>>(list), query)
                            .groupBy { artist -> artist.name.uppercase(Locale.getDefault())[0].toString() }
                        GroupOf.ALBUMS -> filter(castTo<List<Album>>(list), query)
                            .groupBy { album -> album.title.uppercase(Locale.getDefault())[0].toString() }
                        GroupOf.GENRES -> filter(castTo<List<Genre>>(list), query)
                            .groupBy { genre -> genre.name.uppercase(Locale.getDefault())[0].toString() }
                        GroupOf.FOLDERS -> filter(castTo<List<Folder>>(list), query)
                            .groupBy { folder -> folder.name.uppercase(Locale.getDefault())[0].toString() }
                        GroupOf.PLAYLISTS -> filter(castTo<List<Playlist>>(list), query)
                            .groupBy { playlist -> playlist.name.uppercase(Locale.getDefault())[0].toString() }
                        else -> error("")
                    }
                    emit(
                        if (!ascending) grouped.toSortedMap(reverseOrder()) else grouped.toSortedMap()
                    )
                }
                .catch { e ->
                    Log.i(TAG, "init: ${e.message}")
                    result.error("An unknown error occurred!!. ")
                }
                .collect { map ->
                    when (map.isNullOrEmpty()) {
                        true -> result.empty("Empty!! No Items in bucket")
                        else -> result.success(map)
                    }
                }
        }
    }

    companion object {
        const val TYPE = "type"
        private const val TAG = "GroupsViewModel"
    }
}


@JvmName("filter1")
private fun filter(cast: List<Artist>, query: String): List<Artist> = cast.filter { artist ->
    artist.name.lowercase(Locale.getDefault()).contains(query)
}

@JvmName("filter2")
private fun filter(cast: List<Album>, query: String): List<Album> =
    cast.filter { album -> album.title.lowercase(Locale.getDefault()).contains(query) }

@JvmName("filter3")
private fun filter(cast: List<Genre>, query: String): List<Genre> =
    cast.filter { album -> album.name.lowercase(Locale.getDefault()).contains(query) }

@JvmName("filter4")
private fun filter(cast: List<Folder>, query: String): List<Folder> =
    cast.filter { album -> album.name.lowercase(Locale.getDefault()).contains(query) }

private fun filter(cast: List<Playlist>, query: String): List<Playlist> =
    cast.filter { album -> album.name.lowercase(Locale.getDefault()).contains(query) }
package com.prime.player.audio.tracks

import android.app.Application
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prime.player.audio.GroupOf
import com.prime.player.core.AudioRepo
import com.prime.player.core.models.*
import com.prime.player.core.playback.PlaybackService
import com.prime.player.extended.*
import com.prime.player.utils.share
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import org.json.JSONArray
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.collections.ArrayList

private const val TAG = "TracksViewModel"


typealias Filter = Triple<String, GroupBy, Boolean>
typealias TracksResult = Resource<State<Pair<String, Map<String, List<Audio>>>?>>

@OptIn(FlowPreview::class)
@HiltViewModel
class TracksViewModel @Inject constructor(private val context: Application) : ViewModel() {
    private val repo = AudioRepo.get(context = context)

    private lateinit var arg: Pair<GroupOf, String>

    /**
     *  The Selected Audios
     **/
    val selected = mutableStateListOf<Long>()

    /**
     * Search channel
     * Query [String], Ascending [Boolean],
     */
    val channel: StateFlow<Filter> = MutableStateFlow(Filter("", GroupBy.NAME, true))

    /**
     * The title of this screen.
     */
    val title: StateFlow<String> = MutableStateFlow("")

    /**
     * The favourite list
     */
    val favourite = repo.playlists.transform {
        val playlist = it.find {
            it.name == AudioRepo.PLAYLIST_FAVORITES
        }
        emit(playlist)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), null)

    val result: TracksResult = TracksResult(mutableStateOf(null))

    private fun toAudioList(list: List<Long>): List<Audio> {
        val audios = ArrayList<Audio>()
        list.forEach { id ->
            repo.getAudioById(id)?.let {
                audios.add(it)
            }
        }
        return audios
    }

    fun toggleFav(id: Long) {
        repo.toggleFav(id)
    }

    fun toggleSelection(id: Long) {
        viewModelScope.launch {
            if (selected.contains(id))
                selected.remove(id)
            else
                selected.add(id)
        }
    }

    fun sortBy(groupBy: GroupBy, messenger: Messenger) {
        viewModelScope.launch {
            val old = channel.value
            if (old.second != groupBy) {
                (channel as MutableStateFlow).emit(Filter(old.first, groupBy, old.third))
                val msg = when (groupBy) {
                    GroupBy.NAME -> "Grouping list by Name"
                    GroupBy.ARTIST -> "Grouping list by artist"
                    GroupBy.ALBUM -> "Grouping list by album"
                    GroupBy.DURATION -> "Grouping list by duration."
                    GroupBy.NONE -> "No grouping strategy set."
                }
                messenger.send(msg)
            }
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getPlaylist(id: Long): Flow<Playlist> {
        return repo.playlists.transformLatest { playlists ->
            val playlist = playlists.find { playlist ->
                playlist.id == id
            }!!
            emit(playlist)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getFolder(name: String): Flow<Folder> {
        return repo.folders.transformLatest { folders ->
            val folder = folders.find { folder ->
                folder.name == name
            }!!
            emit(folder)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getGenre(id: Long): Flow<Genre> {
        return repo.genres.transformLatest { genres ->
            val genre = genres.find { genre ->
                genre.id == id
            }!!
            emit(genre)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getArtist(id: Long): Flow<Artist> {
        return repo.artists.transformLatest { artists ->
            val artist = artists.find { artist ->
                artist.id == id
            }!!
            emit(artist)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun getAlbum(id: Long): Flow<Album> {
        return repo.albums.transformLatest { albums ->
            val album = albums.find { album ->
                album.id == id
            }!!
            emit(album)
        }
    }

    fun onRequestPlay(indexOf: Audio?, shuffle: Boolean, messenger: Messenger) {
        viewModelScope.launch {

            val list = ArrayList<Audio>()
            val map = result.data.value!!.second
            map.forEach {
                list.addAll(it.value)
            }
            val index = indexOf?.let { list.indexOf(it) } ?: 0

            messenger.send(
                message = "Playing queue ${title.value}"
            )

            val intent = Intent(context, PlaybackService::class.java).apply {
                action = PlaybackService.ACTION_LOAD_LIST
                putExtra(PlaybackService.PARAM_NAME, title.value)
                putExtra(PlaybackService.PARAM_SHUFFLE, shuffle)
                putExtra(PlaybackService.PARAM_FROM_INDEX, index)
                putExtra(PlaybackService.PARAM_START_PLAYING, true)
                val array = JSONArray()
                list.forEach {
                    array.put(it.id)
                }
                putExtra(PlaybackService.PARAM_LIST, array.toString())
            }
            //start normal service
            context.startService(intent)
        }
    }

    fun share(context: Context) {
        viewModelScope.launch {
            val list = ArrayList<Audio>()
            selected.forEach {
                val audio = repo.getAudioById(it)
                audio?.let {
                    list.add(it)
                }
            }
            context.share(list)
        }
    }


    /**
     * Must be called exactly after obtaining the instance
     * @param group: The group it belongs to
     * @param uniqueID: The id of the group.
     */
    fun init(group: GroupOf, uniqueID: String) {
        if (::arg.isInitialized)
            return
        arg = group to uniqueID
        viewModelScope.launch {
            channel
                .debounce(300)
                .distinctUntilChanged()
                .combineTransform(
                    when (group) {
                        GroupOf.AUDIOS -> repo.audios
                        GroupOf.PLAYLISTS -> getPlaylist(uniqueID.toLong())
                        GroupOf.FOLDERS -> getFolder(uniqueID)
                        GroupOf.ARTISTS -> getArtist(uniqueID.toLong())
                        GroupOf.ALBUMS -> getAlbum(uniqueID.toLong())
                        GroupOf.GENRES -> getGenre(uniqueID.toLong())
                    }
                ) { filter, from ->

                    val (query, groupBy, ascending) = filter

                    val (audios, title) = when (from) {
                        is Artist -> from.audioList to from.name
                        is Album -> from.audioList to from.title
                        is Playlist -> toAudioList(from.audios) to from.name
                        is Folder -> from.audios to from.name
                        is Genre -> from.audios to from.name
                        else -> castTo<List<Audio>>(from) to "Audios"
                    }

                    //emit title
                    (this@TracksViewModel.title as MutableStateFlow).emit(title)

                    val filteredList = if (query.isEmpty()) audios else audios.filter { audio ->
                        audio.title.lowercase(Locale.ROOT).contains(query)
                    }

                    val grouped = when (groupBy) {

                        GroupBy.NONE -> mapOf(
                            "" to filteredList
                        )

                        GroupBy.NAME -> filteredList.groupBy { audio ->
                            audio.title.uppercase(Locale.ROOT)[0].toString()
                        }
                        GroupBy.ARTIST -> filteredList.groupBy { audio ->
                            audio.artist?.name ?: "Unknown"
                        }
                        GroupBy.ALBUM -> filteredList.groupBy { audio ->
                            audio.album?.title ?: "Unknown"
                        }
                        else -> filteredList.groupBy { audio ->
                            when {
                                audio.duration < TimeUnit.MINUTES.toMillis(2) -> "Less 2 Min"
                                audio.duration < TimeUnit.MINUTES.toMillis(5) -> "Less than 5 Min"
                                audio.duration < TimeUnit.MINUTES.toMillis(10) -> "Less than 10 Min"
                                else -> "Greater than 10 Min"
                            }
                        }
                    }

                    var duration = 0L
                    filteredList.forEach {
                        duration += it.duration
                    }

                    val subtitle = "${filteredList.size} tracks -  ${
                        com.prime.player.utils.toDuration(
                            context,
                            duration
                        )
                    } of playback"

                    emit(
                        subtitle to if (!ascending) grouped.toSortedMap(reverseOrder()) else grouped.toSortedMap(),
                    )
                }
                .catch { e ->
                    Log.i(TAG, "init: ${e.message}")
                    result.error("An unknown error occurred!!. ")
                }
                .collect {
                    when (it.second.isEmpty()) {
                        true -> result.empty("Empty!! No Items in bucket")
                        else -> result.success(it)
                    }
                }
        }
    }

    companion object {
        private const val TAG = "TracksViewModel"
        const val UNIQUE_ID = "unique_key"
        const val FROM = "from_group"
    }

}
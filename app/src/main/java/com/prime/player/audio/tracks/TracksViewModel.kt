package com.prime.player.audio.tracks

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prime.player.Audiofy
import com.prime.player.R
import com.prime.player.audio.Type
import com.prime.player.audio.tracks.args.TracksRouteArgsFactory
import com.prime.player.common.*
import com.prime.player.common.compose.ToastHostState
import com.prime.player.common.compose.show
import com.prime.player.core.*
import com.primex.core.Result
import com.primex.core.Text
import com.primex.core.buildResult
import com.primex.core.raw
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

typealias TrackFilter = Pair<GroupBy, Boolean>
typealias TrackResult = Map<Text, List<Audio>>


private const val DEBOUNCE_MIN_DELAY = 300L //ms

@Immutable
@Stable
data class Meta(
    val cardinality: Int,
    val duration: Int,
    val size: Long,
    val subtitle: Text? = null,
    val artwork: Uri? = null,
)


@HiltViewModel
class TracksViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val repository: Repository,
    private val remote: Remote,
) : ViewModel() {

    private val args = TracksRouteArgsFactory.fromSavedStateHandle(handle = handle)

    // extract all the args from the saved state handle.
    val type = args.type.let { Type.valueOf(it) }

    /**
     * The unique id used to extract the contents of the particular bucket.
     */
    private val uuid: String = args.id

    val query = args.query?.let { if (it == "@null") null else it }

    val title: Text =
        when (type) {
            Type.AUDIOS -> Text("Audios")
            Type.FOLDERS -> Text(FileUtils.name(uuid))
            Type.PLAYLISTS -> Text(if (uuid.indexOf(Playlists.PRIVATE_PLAYLIST_PREFIX) != -1) "Favourites" else uuid)
            else -> Text(uuid)
        }

    val header: State<Meta?> = mutableStateOf(null)


    /**
     *  The Selected Audios
     **/
    val selected = mutableStateListOf<Long>()

    /**
     * Toggles the selection of the given ID.
     */
    fun select(id: Long) {
        viewModelScope.launch {
            if (selected.contains(id))
                selected.remove(id)
            else
                selected.add(id)
        }
    }

    /**
     * Search channel
     * Query [String], Ascending [Boolean],
     */
    val filter: StateFlow<TrackFilter> =
        MutableStateFlow(TrackFilter(GroupBy.NAME, true))

    /**
     * Filter the results of the list.
     */
    fun filter(
        value: GroupBy,
        ascending: Boolean,
        channel: ToastHostState? = null
    ) {
        viewModelScope.launch {
            val old = filter.value
            val new = old.copy(first = value, second = ascending)
            (filter as MutableStateFlow).emit(new)
            // TODO: Publish to channel
        }
    }

    /**
     * The favourite list
     */
    val favourite =
        repository
            .favourite
            .map { list -> list?.map { it.id } ?: emptyList() }
            .asComposeState(emptyList())

    /**
     * Toggle the favourite state of the audio file.
     */
    fun toggleFav(
        id: Long,
        channel: ToastHostState? = null
    ) {
        viewModelScope.launch {
            repository.toggleFav(id)
            // TODO: Publish to channel
        }
    }

    /**
     * Share the [selected] items fro the available list.
     */
    fun share(
        context: Context,
        channel: ToastHostState? = null
    ) {
        viewModelScope.launch {
            val list = ArrayList<Audio>()
            selected.forEach {
                val audio = repository.getAudioById(it)
                audio?.let {
                    list.add(it)
                }
            }
            context.share(list)
        }
    }


    /**
     * Create [Play], [Intent] adn launches [PlaybackService] with the intent
     */
    fun onRequestPlay(
        context: Context,
        shuffle: Boolean = false,
        indexOf: Audio? = null,
        channel: ToastHostState? = null
    ) {
        viewModelScope.launch {

            val list = ArrayList<Audio>()
            val map = result.data.value.entries
            map.forEach {
                list.addAll(it.value)
            }
            val index = indexOf?.let { list.indexOf(it) } ?: 0

            val title = title.let {
                if (it.raw is String)
                    it.raw as String
                else
                    context.getString(it.raw as Int)
            }

            launch {
                channel?.show(
                    message = "Playing queue $title",
                    label = "Dismiss"
                )
            }
            remote.onRequestPlay(shuffle, index, list.map { it.toMediaItem })
        }
    }

    @OptIn(FlowPreview::class)
    private val observable: Flow<List<Audio>?> =
        when (type) {
            Type.AUDIOS -> repository.audios(query)

            Type.PLAYLISTS -> repository.playlist(
                // must not be null
                // because clicked on it so it must be real.
                repository.getPlaylist(uuid)!!.id
            )

            Type.FOLDERS -> repository.folder(uuid)
            Type.ARTISTS -> repository.artist(uuid)
            Type.ALBUMS -> repository.album(uuid)
            Type.GENRES -> repository.genre(uuid)
        }
            .debounce(DEBOUNCE_MIN_DELAY)

    fun addToPlaylist(playlist: Playlist, ids: List<Long>) {
        viewModelScope.launch {
            repository.addToPlaylist(
                name = playlist.name,
                audios = ids
            )
        }
    }

    val playlists = repository.playlists

    @OptIn(FlowPreview::class)
    val result: Result<TrackResult> =
        buildResult(emptyMap()) {
            // collect the flow here
            filter
                .debounce(DEBOUNCE_MIN_DELAY)
                .distinctUntilChanged()
                .combine(observable) { (groupBy, ascending), data ->

                    if (data == null) {
                        //this.emit(null)
                        return@combine null
                    }

                    if (data.isEmpty()) {
                        return@combine null to emptyMap()
                    }

                    val list = data

                    val filteredList =
                        if (query.isNullOrEmpty()) list else list.filter { audio ->
                            audio.name.lowercase().contains(query)
                        }
                    val grouped =
                        when (groupBy) {
                            // just return the list
                            // mapped with empty string
                            GroupBy.NONE -> mapOf(Text("") to filteredList)

                            // sort the filtered list
                            // then reverse it may be
                            // then group it according to the firstTitleChar
                            GroupBy.NAME -> filteredList
                                .sortedBy { it.name }
                                .let { if (ascending) it else it.asReversed() }
                                .groupBy { audio -> Text(audio.firstTitleChar) }


                            GroupBy.ARTIST -> filteredList
                                .sortedBy { it.name }
                                .let { if (ascending) it else it.asReversed() }
                                .groupBy { audio -> Text(audio.artist) }


                            GroupBy.ALBUM -> filteredList
                                .sortedBy { it.name }
                                .let { if (ascending) it else it.asReversed() }
                                .groupBy { audio -> Text(audio.album) }


                            GroupBy.DURATION -> filteredList
                                .sortedBy { it.duration }
                                .let { if (ascending) it else it.asReversed() }
                                .groupBy { audio ->
                                    when {
                                        audio.duration < TimeUnit.MINUTES.toMillis(2) ->
                                            Text(R.string.list_title_less_then_2_mins)

                                        audio.duration < TimeUnit.MINUTES.toMillis(5) ->
                                            Text(R.string.list_title_less_than_5_mins)

                                        audio.duration < TimeUnit.MINUTES.toMillis(10) ->
                                            Text(R.string.list_title_less_than_10_mins)

                                        else -> Text(R.string.list_title_greater_than_10_mins)
                                    }
                                }
                        }

                    // emit the pair of original info and grouped items.
                    list.meta to grouped
                }
                .onEach { pair ->
                    val first = pair?.first
                    val data = pair?.second


                    // emit data
                    // emit title & header
                    emit(data ?: emptyMap())
                    (this@TracksViewModel.header as MutableState).value = first

                    // emit state of this result
                    // FixMe - Maybe it's good to emit Text as error value
                    //  as we are here dealing with UI.
                    if (data == null)
                        emit(Result.State.Error(null))
                    else if (data.isEmpty())
                        emit(Result.State.Empty)

                    Log.i("TracksViewModel", "count: ")
                }
                // emit the error maybe.
                .catch { emit(Result.State.Error(null)) }
                .launchIn(viewModelScope)
        }
}


private inline val List<Audio>.meta: Meta
    get() {
        val latest = maxByOrNull { it.dateModified }
        return Meta(
            cardinality = size,
            duration = sumOf { it.duration },
            size = sumOf { it.size },
            artwork = latest?.let { Repository.toAlbumArtUri(it.albumId) },
            subtitle = latest?.let {
                Text(
                    value = Util.formatAsRelativeTimeSpan(it.dateModified)
                )
            }
        )
    }

private val Audio.firstTitleChar
    inline get() = name.uppercase(Locale.ROOT)[0].toString()
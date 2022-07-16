package com.prime.player.audio.tracks

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.*
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.prime.player.R
import com.prime.player.audio.Type
import com.prime.player.audio.tracks.args.TracksRouteArgsFactory
import com.prime.player.common.MediaUtil
import com.prime.player.common.Utils
import com.prime.player.common.asComposeState
import com.prime.player.common.compose.SnackDataChannel
import com.prime.player.common.compose.Text
import com.prime.player.core.Audio
import com.prime.player.core.Playlist
import com.prime.player.core.Repository
import com.prime.player.core.name
import com.primex.core.Result
import com.primex.core.buildResult
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

@HiltViewModel
class TracksViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val repository: Repository
) : ViewModel() {

    private val args = TracksRouteArgsFactory.fromSavedStateHandle(handle = handle)

    // extract all the args from the saved state handle.
    val type = args.type.let { Type.valueOf(it) }

    /**
     * The unique id used to extract the contents of the particular bucket.
     */
    private val uuid: String = args.id

    val query = args.query?.let { if (it == "@null") null else it }

    val title: State<Text> = mutableStateOf(Text(""))

    val header: State<Header?> = mutableStateOf(null)

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
        channel: SnackDataChannel? = null
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
            .favouriteList
            .map { list -> list.map { it.id } }
            .asComposeState(emptyList())

    /**
     * Toggle the favourite state of the audio file.
     */
    fun toggleFav(
        id: Long,
        channel: SnackDataChannel? = null
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
        channel: SnackDataChannel? = null
    ) {
        //TODO("Not Implemented yet!")
    }


    /**
     * Create [Play], [Intent] adn launches [PlaybackService] with the intent
     */
    fun onRequestPlay(
        context: Context,
        shuffle: Boolean = false,
        indexOf: Audio? = null,
        channel: SnackDataChannel? = null
    ) {
        //TODO("Not Implemented yet!")
    }

    @OptIn(FlowPreview::class)
    private val observable: Flow<Pair<Any, List<Audio>>?> =
        when (type) {
            Type.AUDIOS -> repository.audios2(query)
            Type.PLAYLISTS -> repository.playlist(uuid)
            Type.FOLDERS -> repository.folder(uuid)
            Type.ARTISTS -> repository.artist(uuid)
            Type.ALBUMS -> repository.album(uuid)
            Type.GENRES -> repository.genre(uuid)
        }
            .debounce(DEBOUNCE_MIN_DELAY)


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

                    val (info, list) = data

                    val filteredList =
                        if (query.isNullOrEmpty()) list else list.filter { audio ->
                            audio.title.lowercase().contains(query)
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
                                .sortedBy { it.title }
                                .let { if (ascending) it else it.asReversed() }
                                .groupBy { audio -> Text(audio.firstTitleChar) }


                            GroupBy.ARTIST -> filteredList
                                .sortedBy { it.title }
                                .let { if (ascending) it else it.asReversed() }
                                .groupBy { audio -> Text(audio.artist) }


                            GroupBy.ALBUM -> filteredList
                                .sortedBy { it.title }
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
                    info to grouped
                }
                .onEach { pair ->
                    val first = pair?.first
                    val data = pair?.second

                    // convert first/header to its Ui Model
                    // also emit the title.
                    val info =
                        when (first) {
                            null -> null
                            is Audio.Artist -> first.asListInfo to first.title
                            is Audio.Album -> first.asListInfo to first.title2
                            is Audio.Genre -> first.asListInfo to first.title
                            is Audio.Bucket -> first.asListInfo to first.title
                            is Audio.Info -> first.asListInfo to first.title
                            is Playlist -> first.asListInfo to first.title
                            else -> error("TrackResult: Unknown type")
                        }

                    // emit data
                    // emit title & header
                    emit(data ?: emptyMap())
                    (this@TracksViewModel.title as MutableState).value = info?.second ?: Text("")
                    (this@TracksViewModel.header as MutableState).value = info?.first

                    // emit state of this result
                    // FixMe - Maybe it's good to emit Text as error value
                    //  as we are here dealing with UI.
                    if (data == null)
                        emit(Result.State.Error(NullPointerException("Check why data is null")))
                    else if (data.isEmpty())
                        emit(Result.State.Empty)

                    Log.i("TracksViewModel", "count: ")
                }
                .catch { emit(Result.State.Error(it)) }
                .launchIn(viewModelScope)
        }
}

@Immutable
@Stable
data class Header(
    val subtitle: Text? = null,
    val artwork: Uri? = null,
    val cardinality: Int,
    val duration: Int,
    val size: Long
)

private val Audio.Info.asListInfo
    get() = Header(
        artwork = MediaUtil.composeAlbumArtUri(value.albumId),
        subtitle = Text("Last Modified: ${Utils.formatAsRelativeTimeSpan(value.dateModified)}"),
        cardinality = cardinality,
        duration = duration,
        size = size.toLong(),
    )

private val Audio.Info.title inline get() = Text("Audios")

private val Audio.Genre.asListInfo
    get() = Header(
        artwork = MediaUtil.composeAlbumArtUri(value.albumId),
        subtitle = null,
        cardinality = tracks,
        duration = duration,
        size = size,
    )

private val Audio.Genre.title
    inline get() = Text(name)


private val Audio.Album.asListInfo
    get() = Header(
        artwork = MediaUtil.composeAlbumArtUri(id),
        subtitle = Text("First Year: $firstYear | Last Year $lastYear"),
        cardinality = tracks,
        duration = duration,
        size = size,
    )

private val Audio.Album.title2
    inline get() = Text(title)

private val Audio.Bucket.asListInfo
    get() = Header(
        artwork = MediaUtil.composeAlbumArtUri(value.albumId),
        subtitle = null,
        cardinality = cardinality,
        duration = -1,
        size = value.size,
    )

private val Audio.Bucket.title
    inline get() = Text(name)

private val Audio.Artist.asListInfo
    get() = Header(
        subtitle = null,
        artwork = MediaUtil.composeAlbumArtUri(value.albumId),
        cardinality = tracks,
        size = value.size,
        duration = value.duration,
    )

private val Audio.Artist.title
    inline get() = Text(name)

private val Playlist.asListInfo
    get() = Header(
        subtitle = Text("Date Created: $dateCreated | Date Modified $dateModified"),
        artwork = null,
        cardinality = -1,
        size = -1,
        duration = -1,
    )

private val Playlist.title
    inline get() = Text(name)


private val Audio.firstTitleChar
    inline get() = title.uppercase(Locale.ROOT)[0].toString()
package com.prime.media.impl.store

import android.content.Context
import android.provider.MediaStore
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.MoreTime
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.prime.media.R
import com.prime.media.core.compose.channel.Channel
import com.prime.media.core.compose.directory.Action
import com.prime.media.core.compose.directory.GroupBy
import com.prime.media.core.db.Audio
import com.prime.media.core.playback.Remote
import com.prime.media.core.util.DateUtil
import com.prime.media.core.util.FileUtils
import com.prime.media.core.util.addDistinct
import com.prime.media.core.util.share
import com.prime.media.store.Audios
import com.prime.media.store.Audios.Companion.GET_EVERY
import com.prime.media.store.Audios.Companion.GET_FROM_ALBUM
import com.prime.media.store.Audios.Companion.GET_FROM_ARTIST
import com.prime.media.store.Audios.Companion.GET_FROM_FOLDER
import com.prime.media.store.Audios.Companion.GET_FROM_GENRE
import com.prime.media.impl.DirectoryViewModel
import com.prime.media.impl.Mapped
import com.prime.media.impl.MetaData
import com.prime.media.impl.Repository
import com.prime.media.impl.albumUri
import com.prime.media.impl.toMediaItem
import com.prime.media.impl.toMember
import com.primex.core.Amber
import com.primex.core.MetroGreen
import com.primex.core.Rose
import com.primex.core.Text
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.random.Random

private const val TAG = "AudiosViewModel"
private val Audio.firstTitleChar
    inline get() = name.uppercase(Locale.ROOT)[0].toString()

@HiltViewModel
class AudiosViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val repository: Repository,
    private val channel: Channel,
    private val remote: Remote,
) : DirectoryViewModel<Audio>(handle), Audios {


    private val type: String = handle.get<String>(Audios.PARAM_TYPE)!!

    /**
     * The items that are in [Playback] favourite playlist list.
     */
    override val favourites = repository.favourite
    override val playlists = repository.playlists

    override fun toggleViewType() {
        // we only currently support single viewType. Maybe in future might support more.
        viewModelScope.launch {
            channel.show("Toggle not implemented/supported yet.", "ViewType")
        }
    }

    override val actions: List<Action> =
        mutableStateListOf(
            Action.PlaylistAdd,
            Action.PlayNext,
            Action.AddToQueue,
            Action.Delete,
            Action.Share,
            Action.Properties,
        )

    init {
        // emit the name to meta
        meta = MetaData(
            Text(
                buildAnnotatedString {
                    append(
                        when (type) {
                            Audios.GET_EVERY -> "Audios"
                            Audios.GET_FROM_GENRE -> "Genre"
                            GET_FROM_FOLDER -> "Folder"
                            GET_FROM_ARTIST -> "Artist"
                            GET_FROM_ALBUM -> "Album"
                            else -> error("no such audios key.")
                        }
                    )
                    withStyle(SpanStyle(fontSize = 9.sp)) {
                        // new line
                        append("\n")
                        // name of the album.
                        append(
                            when (type) {
                                GET_EVERY -> "All Local Audio Files"
                                GET_FROM_FOLDER -> FileUtils.name(key)
                                else -> key
                            }
                        )
                    }
                }
            )
        )
        // if not artist/ exclude some options.
        if (type != GET_FROM_ARTIST)
            (actions as MutableList).add(actions.size - 2, Action.GoToArtist)
        if (type != GET_FROM_ALBUM)
            (actions as MutableList).add(actions.size - 2, Action.GoToAlbum)
    }

    override val orders: List<GroupBy> =
        listOf(
            GroupBy.None,
            GroupBy.Name,
            GroupBy.DateModified,
            GroupBy.Album,
            GroupBy.Artist,
            GroupBy.Length
        )
    override val mActions: List<Action?> = listOf(null, Action.Play, Action.Shuffle)
    inline val GroupBy.toMediaOrder
        get() = when (this) {
            GroupBy.Album -> MediaStore.Audio.Media.ALBUM
            GroupBy.Artist -> MediaStore.Audio.Media.ARTIST
            GroupBy.DateAdded -> MediaStore.Audio.Media.DATE_ADDED
            GroupBy.DateModified -> MediaStore.Audio.Media.DATE_MODIFIED
            GroupBy.Folder -> MediaStore.Audio.Media.DATA
            GroupBy.Length -> MediaStore.Audio.Media.DURATION
            GroupBy.None, GroupBy.Name -> MediaStore.Audio.Media.TITLE
            else -> error("$this order not supported.")
        }

    /**
     * Retrieves a list of audio sources based on the specified query, order, and sort order.
     *
     * @param query The search query used to filter the results.
     * @param order The property used to order the results.
     * @param ascending Whether to sort the results in ascending or descending order.
     * @return A list of audio sources based on the specified criteria.
     */
    private suspend fun source(query: String?, order: String, ascending: Boolean) =
        when (type) {
            GET_EVERY -> repository.getAudios(query, order, ascending)
            GET_FROM_ALBUM -> repository.getAudiosOfAlbum(key, query, order, ascending)
            GET_FROM_ARTIST -> repository.getAudiosOfArtist(key, query, order, ascending)
            GET_FROM_FOLDER -> repository.getAudiosOfFolder(key, query, order, ascending)
            GET_FROM_GENRE -> repository.getAudiosOfGenre(key, query, order, ascending)
            else -> error("invalid type $type")
        }

    // Actual implementation
    override val data: Flow<Mapped<Audio>> =
        repository.observe(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
            .combine(filter) { f1, f2 -> f2 }
            .map {
                val (order, query, ascending) = it
                val list = source(query, order.toMediaOrder, ascending)

                // Don't know if this is correct place to emit changes to Meta.
                val latest = list.maxByOrNull { it.dateModified }
                meta = meta?.copy(
                    artwork = latest?.albumUri.toString(),
                    cardinality = list.size,
                    dateModified = latest?.dateModified ?: -1
                )

                when (order) {
                    GroupBy.Album -> list.groupBy { audio -> Text(audio.album) }
                    GroupBy.Artist -> list.groupBy { audio -> Text(audio.artist) }
                    GroupBy.DateAdded -> TODO()
                    GroupBy.DateModified -> list.groupBy {
                        Text(
                            value = DateUtil.formatAsRelativeTimeSpan(
                                it.dateModified
                            )
                        )
                    }

                    GroupBy.Folder -> TODO()
                    GroupBy.Name -> list.groupBy { audio -> Text(audio.firstTitleChar) }
                    GroupBy.None -> mapOf(Text("") to list)
                    GroupBy.Length -> list.groupBy { audio ->
                        when {
                            audio.duration < TimeUnit.MINUTES.toMillis(2) -> Text(R.string.list_title_less_then_2_mins)
                            audio.duration < TimeUnit.MINUTES.toMillis(5) -> Text(R.string.list_title_less_than_5_mins)
                            audio.duration < TimeUnit.MINUTES.toMillis(10) -> Text(R.string.list_title_less_than_10_mins)
                            else -> Text(R.string.list_title_greater_than_10_mins)
                        }
                    }

                    else -> error("$order invalid")
                }
            }
            .catch {
                // any exception.
                channel.show(
                    "Some unknown error occured!. $it",
                    "Error",
                    leading = Icons.Outlined.Error,
                    accent = Color.Rose,
                    duration = Channel.Duration.Indefinite
                )
            }

    /**
     * Updates the list of available actions based on the currently selected key(s).
     * {@inheritDoc}
     */
    override fun select(key: String) {
        super.select(key)
        // add actions if selected.size == 1
        val mutable = actions as SnapshotStateList
        when {
            selected.isEmpty() -> {
                if (type != GET_FROM_ARTIST)
                    mutable.addDistinct(Action.GoToArtist)
                if (type != GET_FROM_ALBUM)
                    mutable.addDistinct(Action.GoToAlbum)
                // action is invisible // hide it.
                mutable.remove(Action.SelectAll)
                mutable.addDistinct(Action.Properties)
            }

            selected.size == 1 -> mutable.addDistinct(Action.SelectAll)
            selected.size == 2 -> {
                mutable.remove(Action.Properties)
                mutable.remove(Action.GoToArtist)
                mutable.remove(Action.GoToAlbum)
            }
        }
    }

    /**
     * Loads the items into the [Playback] service and starts playing.
     *
     * The algorithm works as follows:
     * 1. If nothing is selected, all items based on the current filter are obtained. If shuffle is
     * enabled, the index is set to a random item.
     * 2. If the "focused" item is not empty, the index is set to that item. Otherwise, the index is
     * set to the first item in the list.
     * 3. If items are selected, only those items are consumed. If shuffle is enabled, the index is
     * set to a random item from the selected items. Otherwise, the index is set to the first selected item in the list.
     *
     * @param shuffle if true, the items are played in random order
     */
    override fun play(shuffle: Boolean) {
        viewModelScope.launch {
            // obtain the value of the filter.
            val (f1, f2, f3) = filter.value
            // because the order is necessary to play intented item first.
            val list =
                source(f2, f1.toMediaOrder, f3).let {
                    // return same list if selected is empty else return the only selected items from the list.
                    val arr = ArrayList(selected)
                    // consume selected.
                    clear()
                    if (arr.isEmpty())
                        it
                    else
                        arr.mapNotNull { id -> it.find { "${it.id}" == id } }
                }
            // don't do anything
            if (list.isEmpty()) return@launch
            val focused = focused.toLongOrNull() ?: -1L
            // check which is focused
            val index = when {
                // pick random
                shuffle -> Random.nextInt(0, list.size)
                // find focused
                focused != -1L -> list.indexOfFirst { it.id == focused }
                    .let { if (it == -1) 0 else it }

                else -> 0
            }
            remote.onRequestPlay(shuffle, index, list.map { it.toMediaItem })
            channel.show(title = "Playing", message = "Playing tracks enjoy.")
        }
    }

    /**
     * Adds the selected or focused item(s) to the specified playlist.
     *
     * The algorithm works as follows:
     * 1. Determine whether the action was called on a selected item or a focused one.
     * 2. Retrieve the keys/ids of the selected or focused item(s).
     * 3. Clear the selection.
     * 4. Retrieve the specified playlist from the repository.
     * 5. If the playlist doesn't exist, display an error message and return.
     * 6. Retrieve the last play order of the playlist from the repository, if it exists.
     * 7. Map the keys/ids to audio items, with the corresponding playlist ID and play order.
     * 8. Insert or update the audio items in the repository.
     * 9. Display a success or warning message, depending on the number of items added to the playlist.
     *
     * @param name The name of the playlist to add the item(s) to.
     */
    override fun addToPlaylist(name: String) {
        // focus or selected.
        viewModelScope.launch {
            // The algo goes like this.
            // This fun is called on selected item or focused one.
            // so obtain the keys/ids
            val list = when {
                focused.isNotBlank() -> listOf(focused)
                selected.isNotEmpty() -> ArrayList(selected)
                else -> {
                    channel.show("No item selected.", "Message")
                    return@launch
                }
            }

            // consume selected
            clear()

            val playlist = repository.getPlaylist(name)
            if (playlist == null) {
                channel.show(
                    "It seems the playlist doesn't exist.",
                    "Error",
                    leading = Icons.Outlined.Error
                )
                return@launch
            }

            var order = repository.getLastPlayOrder(playlist.id) ?: -1

            // you can;t just add to playlist using the keys.
            val audios = list.mapNotNull {
                repository.findAudio(it.toLongOrNull() ?: 0)?.toMember(playlist.id, order++)
            }

            var count = 0
            audios.forEach {
                val success = repository.upsert(it)
                if (success)
                    count++
            }

            if (count < list.size)
                channel.show(
                    "Added only $count items to $name",
                    "Warning",
                    leading = Icons.Outlined.Warning,
                    accent = Color.Amber,
                )
            else
                channel.show(
                    "Added $count items to $name",
                    "Success",
                    leading = Icons.Outlined.CheckCircle,
                    accent = Color.MetroGreen,
                )
        }
    }

    override fun toggleFav() {
        viewModelScope.launch {
            val focused = focused.toLongOrNull() ?: return@launch
            val res = repository.toggleFav(focused)
            channel.show(
                if (res) "Added to favourite" else "Removed from favourite",
                "Favourite"
            )
        }
    }

    override fun playNext() {
        viewModelScope.launch {
            channel.show(
                title = "Coming soon.",
                message = "Requires more polishing. Please wait!",
                leading = Icons.Outlined.MoreTime
            )
        }
    }

    override fun addToQueue() {
        viewModelScope.launch {
            channel.show(
                title = "Coming soon.",
                message = "Requires more polishing. Please wait!",
                leading = Icons.Outlined.MoreTime
            )
        }
    }

    override fun delete() {
        viewModelScope.launch {
            channel.show(
                title = "Coming soon.",
                message = "Requires more polishing. Please wait!",
                leading = Icons.Outlined.MoreTime
            )
        }
    }

    override fun selectAll() {
        viewModelScope.launch {
            val list = source(filter.value.second, MediaStore.Audio.Media.DEFAULT_SORT_ORDER, true)
            list.forEach {
                val key = "${it.id}"
                if (!selected.contains(key))
                    select(key)
            }
        }
    }

    /**
     * Shares the selected or focused item(s).
     *
     * The algorithm works as follows:
     * 1. If an item is focused, add its ID to the list.
     * 2. else if there are selected items, add their IDs to the list.
     * 2. Consume the selected items.
     * 3. Retrieve the metadata for each ID in the list.
     * 4. If the list is empty, do nothing.
     * 5. Share the metadata with the specified context.
     *
     * @param context the context to share the metadata with.
     */
    override fun share(context: Context) {
        viewModelScope.launch {
            // The algo goes like this.
            // This fun is called on selected item or focused one.
            // so obtain the keys/ids
            val list = when {
                focused.isNotBlank() -> listOf(focused)
                selected.isNotEmpty() -> ArrayList(selected)
                else -> {
                    channel.show("No item selected.", "Message")
                    return@launch
                }
            }

            // consume selected
            clear()
            val audios = list.mapNotNull { repository.findAudio(it.toLongOrNull() ?: 0) }
            // currently don't do anything.
            if (audios.isEmpty()) return@launch
            context.share(audios)
        }
    }


    override fun toArtist(controller: NavHostController) {
        viewModelScope.launch {
            val artist = repository.findAudio(focused.toLongOrNull() ?: 0)?.artist ?: return@launch
            val direction = Audios.direction(GET_FROM_ARTIST, artist)
            controller.navigate(direction)
        }
    }

    override fun toAlbum(controller: NavHostController) {
        viewModelScope.launch {
            val album = repository.findAudio(focused.toLongOrNull() ?: 0)?.album ?: return@launch
            val direction = Audios.direction(GET_FROM_ALBUM, album)
            controller.navigate(direction)
        }
    }
}
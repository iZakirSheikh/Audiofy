package com.prime.media.directory.local

import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import com.prime.media.R
import com.prime.media.Theme
import com.prime.media.common.ContentElevation
import com.prime.media.common.ContentPadding
import com.prime.media.common.LocalNavController
import com.prime.media.common.composable
import com.prime.media.core.*
import com.prime.media.core.compose.Image
import com.prime.media.core.compose.ToastHostState
import com.prime.media.core.compose.show
import com.prime.media.core.db.Audio
import com.prime.media.core.playback.Remote
import com.prime.media.directory.*
import com.prime.media.directory.dialogs.Playlists
import com.prime.media.directory.dialogs.Properties
import com.primex.core.*
import com.primex.ui.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.collections.ArrayList
import kotlin.random.Random.Default.nextInt

private const val TAG = "AudiosViewModel"

private val Audio.firstTitleChar
    inline get() = name.uppercase(Locale.ROOT)[0].toString()

typealias Audios = AudiosViewModel.Companion

@HiltViewModel
class AudiosViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val repository: Repository,
    private val toaster: ToastHostState,
    private val remote: Remote,
) : DirectoryViewModel<Audio>(handle) {

    companion object {

        const val GET_EVERY = "_every"
        const val GET_FROM_FOLDER = "_folder"
        const val GET_FROM_ARTIST = "_artist"
        const val GET_FROM_GENRE = "_genre"
        const val GET_FROM_ALBUM = "_album"

        private const val HOST = "_local_audios"

        private const val PARAM_TYPE = "_param_type"

        val route = compose("$HOST/{${PARAM_TYPE}}")
        fun direction(
            of: String,
            key: String = NULL_STRING,
            query: String = NULL_STRING,
            order: GroupBy = GroupBy.Name,
            ascending: Boolean = true,
            viewType: ViewType = ViewType.List
        ) = compose("$HOST/$of", Uri.encode(key), query, order, ascending, viewType)
    }

    private val type: String = handle.get<String>(PARAM_TYPE)!!

    /**
     * The items that are in [Playback] favourite playlist list.
     */
    val favourites = repository.favourite
    val playlists = repository.playlists

    override fun toggleViewType() {
        // we only currently support single viewType. Maybe in future might support more.
        viewModelScope.launch {
            toaster.show("Toggle not implemented/supported yet.", "ViewType")
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
                            GET_EVERY -> "Audios"
                            GET_FROM_GENRE -> "Genre"
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
        listOf(GroupBy.None, GroupBy.Name, GroupBy.Album, GroupBy.Artist, GroupBy.Length)
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
                    GroupBy.DateModified -> TODO()
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
                toaster.show(
                    "Some unknown error occured!.",
                    "Error",
                    leading = Icons.Outlined.Error,
                    accent = Color.Rose,
                    duration = ToastHostState.Duration.Indefinite
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
    fun play(shuffle: Boolean) {
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
                shuffle -> nextInt(0, list.size)
                // find focused
                focused != -1L -> list.indexOfFirst { it.id == focused }
                    .let { if (it == -1) 0 else it }
                else -> 0
            }
            remote.onRequestPlay(shuffle, index, list.map { it.toMediaItem })
            toaster.show(title = "Playing", message = "Playing tracks enjoy.")
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
    fun addToPlaylist(name: String) {
        // focus or selected.
        viewModelScope.launch {
            // The algo goes like this.
            // This fun is called on selected item or focused one.
            // so obtain the keys/ids
            val list = when {
                focused.isNotBlank() -> listOf(focused)
                selected.isNotEmpty() -> kotlin.collections.ArrayList(selected)
                else -> {
                    toaster.show("No item selected.", "Message")
                    return@launch
                }
            }

            // consume selected
            clear()

            val playlist = repository.getPlaylist(name)
            if (playlist == null) {
                toaster.show(
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
                toaster.show(
                    "Added only $count items to $name",
                    "Warning",
                    leading = Icons.Outlined.Warning,
                    accent = Color.Amber,
                )
            else
                toaster.show(
                    "Added $count items to $name",
                    "Success",
                    leading = Icons.Outlined.CheckCircle,
                    accent = Color.MetroGreen,
                )
        }
    }

    fun toggleFav() {
        viewModelScope.launch {
            val focused = focused.toLongOrNull() ?: return@launch
            val res = repository.toggleFav(focused)
            toaster.show(
                if (res) "Added to favourite" else "Removed from favourite",
                "Favourite"
            )
        }
    }

    fun playNext() {
        viewModelScope.launch {
            toaster.show(
                title = "Coming soon.",
                message = "Requires more polishing. Please wait!",
                leading = Icons.Outlined.MoreTime
            )
        }
    }

    fun addToQueue() {
        viewModelScope.launch {
            toaster.show(
                title = "Coming soon.",
                message = "Requires more polishing. Please wait!",
                leading = Icons.Outlined.MoreTime
            )
        }
    }

    fun delete() {
        viewModelScope.launch {
            toaster.show(
                title = "Coming soon.",
                message = "Requires more polishing. Please wait!",
                leading = Icons.Outlined.MoreTime
            )
        }
    }

    fun selectAll() {
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
    fun share(context: Context) {
        viewModelScope.launch {
            // The algo goes like this.
            // This fun is called on selected item or focused one.
            // so obtain the keys/ids
            val list = when {
                focused.isNotBlank() -> listOf(focused)
                selected.isNotEmpty() -> kotlin.collections.ArrayList(selected)
                else -> {
                    toaster.show("No item selected.", "Message")
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


    fun toArtist(controller: NavHostController) {
        viewModelScope.launch {
            val artist = repository.findAudio(focused.toLongOrNull() ?: 0)?.artist ?: return@launch
            val direction = direction(GET_FROM_ARTIST, artist)
            controller.navigate(direction)
        }
    }

    fun toAlbum(controller: NavHostController) {
        viewModelScope.launch {
            val album = repository.findAudio(focused.toLongOrNull() ?: 0)?.album ?: return@launch
            val direction = direction(GET_FROM_ALBUM, album)
            controller.navigate(direction)
        }
    }
}

private val ARTWORK_SIZE = 48.dp

@OptIn(ExperimentalMaterialApi::class)
@Composable
@NonRestartableComposable
private fun Audio(
    value: Audio,
    actions: List<Action>,
    modifier: Modifier = Modifier,
    focused: Boolean = false,
    checked: Boolean = false,
    favourite: Boolean = false,
    onAction: (Action) -> Unit
) {
    ListTile(
        selected = checked,
        centreVertically = false,
        modifier = modifier,

        overlineText = {
            Label(
                text = value.name,
                style = Theme.typography.body1,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
        },
        text = {
            Label(
                style = Theme.typography.caption,
                text = value.album,
                modifier = Modifier.padding(top = ContentPadding.small),
                color = LocalContentColor.current.copy(ContentAlpha.disabled),
                fontWeight = FontWeight.SemiBold,
            )
        },
        secondaryText = {
            Label(
                text = value.artist,
                fontWeight = FontWeight.SemiBold,
                style = Theme.typography.caption
            )
        },
        leading = {
            Image(
                data = value.albumUri,
                fallback = painterResource(id = R.drawable.default_art),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .border(2.dp, Color.White, shape = CircleShape)
                    .shadow(ContentElevation.high, shape = CircleShape)
                    .size(ARTWORK_SIZE)
                    .wrapContentSize(Alignment.TopCenter)
                    .requiredSize(70.dp),
            )
        },
        trailing = {
            IconButton(
                contentDescription = null,
                imageVector = if (favourite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                onClick = { onAction(Action.Make) },
                // TODO: Currently we don't know how to grant this focus
                // Hence we have disabled it. if not in focus.
                enabled = focused
            )
        },
        bottom = composable(focused) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(
                        start = ARTWORK_SIZE,
                        end = ContentPadding.normal
                    )
            ) {
                actions.forEach {
                    val color = ChipDefaults.outlinedChipColors(backgroundColor = Color.Transparent)
                    Chip(
                        onClick = { onAction(it) },
                        colors = color,
                        border =
                        BorderStroke(
                            1.dp, Theme.colors.primary.copy(ChipDefaults.OutlinedBorderOpacity)
                        ),
                        modifier = Modifier.padding(ContentPadding.small)
                    ) {
                        Label(
                            text = it.title.obtain,
                            modifier = Modifier.padding(end = ContentPadding.small),
                            style = Theme.typography.caption
                        )
                        Icon(
                            imageVector = it.icon,
                            contentDescription = "",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Audios(viewModel: AudiosViewModel) {
    val favourites by viewModel.favourites.collectAsState(initial = emptyList())
    val selected = viewModel.selected
    var confirm by rememberState<Action?>(initial = null)

    // show conform irrespective of it is called on single or multiple items.
    if (confirm == Action.PlaylistAdd) {
        Playlists(
            value = viewModel.playlists.collectAsState(initial = emptyList()).value,
            expanded = true,
            onPlaylistClick = {
                if (it != null) {
                    viewModel.addToPlaylist(it.name)
                }
                confirm = null
            }
        )
    }

    // handle the action logic.
    val navigator = LocalNavController.current
    val context = LocalContext.current
    val onPerformAction = { action: Action ->
        when (action) {
            // show dialog
            Action.PlaylistAdd, Action.Properties -> confirm = action
            Action.Make -> viewModel.toggleFav()
            Action.Share -> viewModel.share(context)
            Action.AddToQueue -> viewModel.addToQueue()
            Action.PlayNext -> viewModel.playNext()
            Action.Delete -> viewModel.delete()
            Action.SelectAll -> viewModel.selectAll()
            Action.GoToAlbum -> viewModel.toAlbum(navigator)
            Action.GoToArtist -> viewModel.toArtist(navigator)
            Action.Shuffle -> viewModel.play(true)
            Action.Play -> viewModel.play(false)
            else -> error("Action: $action not supported.")
        }
    }

    // extend the Directory.
    Directory(
        viewModel = viewModel,
        cells = GridCells.Fixed(1),
        onAction = onPerformAction,
        key = { it.id },
    ) { audio ->
        // emit checked for each item.
        val checked by remember {
            derivedStateOf {
                selected.contains("${audio.id}")
            }
        }
        val favourite by remember {
            derivedStateOf {
                favourites.contains(audio.key)
            }
        }
        val focused = viewModel.focused == "${audio.id}"

        // if is focused and action is properties
        // show the dialog.
        with(audio) {
            Properties(expanded = confirm == Action.Properties && focused) {
                confirm = null
            }
        }

        // actual content
        Audio(
            value = audio,
            actions = viewModel.actions,
            favourite = favourite,
            checked = checked,
            focused = focused,
            // TODO: need to update focus state on interaction.
            onAction = onPerformAction,
            modifier = Modifier
                .animateContentSize()
                .animateItemPlacement()
                .combinedClickable(
                    onClick = {
                        when {
                            selected.isNotEmpty() -> viewModel.select("${audio.id}")
                            // change focused to current.
                            !focused -> viewModel.focused = "${audio.id}"
                            // cause the playlist to start playing from current track.
                            else -> viewModel.play(false)
                        }
                    },
                    onLongClick = {
                        viewModel.select("${audio.id}")
                    }
                )
                .padding(horizontal = ContentPadding.medium)
        )
    }
}
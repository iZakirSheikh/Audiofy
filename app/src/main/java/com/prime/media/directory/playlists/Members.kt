package com.prime.media.directory.playlists

import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.prime.media.Material
import com.prime.media.R
import com.prime.media.core.*
import com.prime.media.core.compose.*
import com.prime.media.core.db.Playlist.Member
import com.prime.media.core.playback.Playback
import com.prime.media.core.playback.Remote
import com.prime.media.core.util.addDistinct
import com.prime.media.directory.*
import com.prime.media.directory.dialogs.Playlists
import com.prime.media.impl.Repository
import com.prime.media.core.util.toMediaItem
import com.primex.core.*
import com.primex.material2.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject
import kotlin.random.Random
import com.primex.core.Text

private const val TAG = "AlbumsViewModel"

typealias Members = MembersViewModel.Companion

private val Member.firstTitleChar
    inline get() = title.uppercase()[0].toString()

@HiltViewModel
class MembersViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val repository: Repository,
    private val toaster: Channel,
    private val remote: Remote,
) : DirectoryViewModel<Member>(handle) {

    companion object {
        private const val HOST = "_local_playlist_members"

        val route = compose(HOST)
        fun direction(
            key: String,
            query: String = NULL_STRING,
            order: GroupBy = GroupBy.Name,
            ascending: Boolean = true,
            viewType: ViewType = ViewType.List
        ) = compose(HOST, Uri.encode(key), query, order, ascending, viewType)
    }

    private val title = when (key) {
        Playback.PLAYLIST_FAVOURITE -> "Favourites"
        Playback.PLAYLIST_RECENT -> "History"
        else -> key
    }

    init {
        // emit the name to meta
        //TODO: Add other fields in future versions.
        meta = MetaData(
            Text(title)
        )
    }


    val playlists = repository.playlists

    override val actions: List<Action> =
        mutableStateListOf(Action.PlaylistAdd, Action.PlayNext, Action.AddToQueue, Action.Delete)
    override val orders: List<GroupBy> = listOf(GroupBy.None, GroupBy.Name)
    override val mActions: List<Action?> = listOf(null, Action.Play, Action.Shuffle)

    override val data: Flow<Mapped<Member>> =
        filter.flatMapLatest { (order, query, ascending) ->
            repository
                .playlist(key)
                .map { data ->
                    val filtered =
                        if (query == null)
                            data
                        else
                            data.filter { it.title.contains(query, true) }
                    val src = if (ascending) filtered else filtered.reversed()

                    // Don't know if this is correct place to emit changes to Meta.
                    val latest = src.maxByOrNull { it.order  }
                    meta = meta?.copy(
                        artwork = latest?.artwork.toString(),
                        cardinality = src.size
                    )

                    when (order) {
                        GroupBy.None -> mapOf(Text("") to src)
                        GroupBy.Name -> src.groupBy { Text(it.firstTitleChar) }
                        else -> error("$order invalid")
                    }
                }
        }
            .catch {
            // any exception.
            toaster.show(
                "Some unknown error occured!.",
                "Error",
                leading = Icons.Outlined.Error,
                accent = Color.Rose,
                duration = Channel.Duration.Indefinite
            )
        }.stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    override fun toggleViewType() {
        // we only currently support single viewType. Maybe in future might support more.
        viewModelScope.launch {
            toaster.show("Toggle not implemented yet.", "ViewType")
        }
    }

    /**
     * @see AudiosViewModel.play
     */
    fun play(shuffle: Boolean) {
        viewModelScope.launch {
            // because the order is necessary to play intented item first.
            val src = data.firstOrNull()?.values?.flatten() ?: return@launch
            val list =
                src.let {
                    // return same list if selected is empty else return the only selected items from the list.
                    val arr = ArrayList(selected)
                    // consume selected.
                    clear()
                    if (arr.isEmpty())
                        it
                    else
                        arr.mapNotNull { id -> it.find { it.uri == id } }
                }
            // don't do anything
            if (list.isEmpty()) return@launch
            val focused = focused
            // check which is focused
            val index = when {
                // pick random
                shuffle -> Random.nextInt(0, list.size)
                // find focused
                focused.isNotBlank() -> list.indexOfFirst { it.uri == focused }.let { if (it == -1) 0 else it }
                else -> 0
            }
            remote.onRequestPlay(shuffle, index, list.map { it.toMediaItem })
            toaster.show(title = "Playing", message = "Playing tracks enjoy.")
        }
    }

    /**
     * Deletes the selected or focused item(s) from the playlist.
     * If no item is selected, shows a toast message and returns.
     * If an item is focused, deletes that item.
     * If multiple items are selected, deletes all selected items.
     * Shows a toast message indicating the number of items deleted.
     */
    fun delete() {
        viewModelScope.launch {
            val list = when {
                focused.isNotBlank() -> listOf(focused)
                selected.isNotEmpty() -> ArrayList(selected)
                else -> {
                    toaster.show("No item selected.", "Message")
                    return@launch
                }
            }
            // consume selected
            clear()

            var count = 0
            list.forEach {
                val deleted = repository.removeFromPlaylist(key, it)
                if (deleted)
                    count++
            }
            if (count < list.size)
                toaster.show(
                    "Deleted $count items from $title",
                    "Delete",
                    leading = Icons.Outlined.Error,
                    accent = Color.Rose,
                )
        }
    }


    fun playNext() {
        viewModelScope.launch {
            val index = remote.nextIndex
            // might return -1 if nextIndex is unset. in this case the item will be added to end of
            // queue.
            addToQueue(index)
        }
    }


    fun addToQueue(index: Int = -1) {
        viewModelScope.launch {
            with(toaster) {
                // because the order is necessary to play intented item first.
                val src = data.firstOrNull()?.values?.flatten() ?: return@launch
                val list = when {
                    focused.isNotBlank() -> listOf(focused)
                    selected.isNotEmpty() -> ArrayList(selected)
                    else -> {
                        toaster.show("No item selected.", "Message")
                        return@launch
                    }
                }
                // consume selected
                clear()
                // show warning
                show(
                    R.string.msg_queue_add_experimental_warning,
                    R.string.warning,
                    ResourcesCompat.ID_NULL,
                    Icons.Outlined.Warning,
                    Color.DahliaYellow,
                    Channel.Duration.Short
                )
                // map keys to media item
                val audios = list.mapNotNull {id ->
                    src.find { it.uri == id}?.toMediaItem
                }
                // This case is just a guard case here.
                // As this will never be called under normal circumstances.
                if (audios.isEmpty())
                    return@launch
                val count = remote.add(*audios.toTypedArray(), index = index)
                show(
                    buildPluralResource(id = R.plurals.msg_queue_updated_dd, count, count, audios.size),
                    buildTextResource(if (count == 0) R.string.msg_error_status_uncertain else R.string.success),
                    null,
                    Icons.Outlined.Queue,
                    if (count == 0) Color.RedViolet else Color.MetroGreen
                )
            }
        }
    }

    /**
     * Overrides the `select` method of the parent class to add or remove the given item key from the
     * list of selected items. Also adds or removes the `SelectAll` action from the list of available
     * actions depending on the number of selected items.
     *
     * The algorithm of this function is as follows:
     *
     * 1. Call the `select` method of the parent class with the given item key to add or remove it
     *    from the list of selected items.
     *
     * 2. Get the mutable list of actions by casting the `actions` property to a `SnapshotStateList`.
     *
     * 3. Depending on the number of selected items, add or remove the `SelectAll` action from the list
     *    of available actions:
     *    - If no items are selected, remove the `SelectAll` action if it exists.
     *    - If one item is selected, add the `SelectAll` action if it doesn't exist.
     *
     * @param key The key of the item to select or deselect.
     */
    override fun select(key: String) {
        super.select(key)
        // add actions if selected.size == 1
        val mutable = actions as SnapshotStateList
        when {
            selected.isEmpty() -> mutable.remove(Action.SelectAll)
            selected.size == 1 -> mutable.addDistinct(Action.SelectAll)
        }
    }

    /**
     * @see AudiosViewModel.addToPlaylist
     */
    fun addToPlaylist(name: String) {
        // focus or selected.
        viewModelScope.launch {
            if(key == name){
                toaster.show(
                    "The tracks are already in the playlist",
                    "Message",
                    leading = Icons.Outlined.Message
                )
                return@launch
            }

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
                repository.getPlaylistMember(playlist.id, it)?.copy(order = order++)
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

    fun selectAll() {}
}

private val ARTWORK_SIZE = 56.dp
private val MEMBER_ICON_SHAPE = RoundedCornerShape(30)

@OptIn(ExperimentalMaterialApi::class)
@Composable
@NonRestartableComposable
private fun Member(
    value: Member,
    actions: List<Action>,
    modifier: Modifier = Modifier,
    focused: Boolean = false,
    checked: Boolean = false,
    onAction: (Action) -> Unit
) {
    ListTile(
        selected = checked,
        centreVertically = false,
        modifier = modifier,
        overlineText = {
            Label(
                style = Material.typography.caption,
                text = value.subtitle,
                color = LocalContentColor.current.copy(ContentAlpha.medium),
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Label(
                text = value.title,
                style = Material.typography.body1,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
            )
        },
        leading = {
            Image(
                data = value.artwork,
                fallback = painterResource(id = R.drawable.default_art),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .border(2.dp, Color.White, shape = MEMBER_ICON_SHAPE)
                    .shadow(ContentElevation.medium, shape = MEMBER_ICON_SHAPE)
                    .size(ARTWORK_SIZE),
            )
        },
        trailing = {
            IconButton(
                contentDescription = null,
                imageVector = Icons.Default.DragIndicator,
                onClick = { /*TODO: Add drag logic in future,*/ },
            )
        },

        bottom = composable(focused) {
            Row(
                Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(start = ARTWORK_SIZE, end = ContentPadding.normal)
            ) {
                actions.forEach {
                    val color = ChipDefaults.outlinedChipColors(backgroundColor = Color.Transparent)
                    Chip(
                        onClick = { onAction(it) },
                        colors = color,
                        border =
                        BorderStroke(
                            1.dp,
                            Material.colors.primary.copy(ChipDefaults.OutlinedBorderOpacity)
                        ),
                        modifier = Modifier.padding(ContentPadding.small)
                    ) {
                        Label(
                            text = it.title.get,
                            modifier = Modifier.padding(end = ContentPadding.small),
                            style = Material.typography.caption
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
fun Members(viewModel: MembersViewModel) {
    val selected = viewModel.selected
    // The confirm is a stata variable
    // that holds the value of current confirmation action
    var confirm by rememberState<Action?>(initial = null)
    // show confirm dialog for playlist Add.
    //irrespective of it is called on single or multiple items.
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

    if (confirm == Action.Delete)
        com.primex.material2.dialog.AlertDialog(
            title = "Delete",
            message = "You are about to remove items. This can't be undone. \nAre you sure?",
            vectorIcon = Icons.Outlined.DeleteForever,
            onDismissRequest = { action ->
                when (action) {
                    true -> viewModel.delete()
                    else -> {}
                }
                confirm = null
            }
        )


    // perform action. show dialogs maybe.
    val onPerformAction = { action: Action ->
        when (action) {
            // show dialog
            Action.PlaylistAdd, Action.Properties -> confirm = action
            Action.AddToQueue -> viewModel.addToQueue()
            Action.PlayNext -> viewModel.playNext()
            Action.Delete -> confirm = action
            Action.SelectAll -> viewModel.selectAll()
            Action.Shuffle -> viewModel.play(true)
            Action.Play -> viewModel.play(false)
            else -> error("Action: $action not supported.")
        }
    }
    // The actual content of the directory
    Directory(
        viewModel = viewModel,
        cells = GridCells.Fixed(1),
        onAction = onPerformAction,
        key = { it.uri },
    ) { member ->
        // emit checked for each item.
        val checked by remember {
            derivedStateOf {
                selected.contains(member.uri)
            }
        }
        // Check if the current member has the focus.
        // if true make it show more options.
        val focused = viewModel.focused == member.uri
        Member(
            value = member,
            onAction = onPerformAction,
            checked = checked,
            focused = focused,
            actions = viewModel.actions,
            modifier = Modifier
                .animateContentSize()
                .animateItemPlacement()
                .combinedClickable(
                    onClick = {
                        when {
                            selected.isNotEmpty() -> viewModel.select(member.uri)
                            // change focused to current.
                            !focused -> viewModel.focused = member.uri
                            // cause the playlist to start playing from current track.
                            else -> viewModel.play(false)
                        }
                    },
                    onLongClick = {
                        viewModel.select(member.uri)
                    }
                )
                .padding(horizontal = ContentPadding.normal)
        )
    }
}
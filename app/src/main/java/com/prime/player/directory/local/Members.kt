package com.prime.player.directory.local

import android.net.Uri
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.DragIndicator
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Error
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.prime.player.R
import com.prime.player.Theme
import com.prime.player.caption2
import com.prime.player.common.ContentElevation
import com.prime.player.common.ContentPadding
import com.prime.player.common.composable
import com.prime.player.core.Repository
import com.prime.player.core.addDistinct
import com.prime.player.core.albumUri
import com.prime.player.core.compose.Image
import com.prime.player.core.compose.ToastHostState
import com.prime.player.core.compose.show
import com.prime.player.core.db.Audio
import com.prime.player.core.db.Playlist.Member
import com.prime.player.core.key
import com.prime.player.core.playback.Playback
import com.prime.player.core.playback.Remote
import com.prime.player.directory.*
import com.prime.player.directory.dialogs.Playlists
import com.primex.core.Text
import com.primex.core.obtain
import com.primex.core.rememberState
import com.primex.ui.IconButton
import com.primex.ui.Label
import com.primex.ui.ListTile
import com.primex.ui.Rose
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

private const val TAG = "AlbumsViewModel"

typealias Members = MembersViewModel.Companion

private val Member.firstTitleChar
    inline get() = title.uppercase()[0].toString()

@HiltViewModel
class MembersViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val repository: Repository,
    private val toaster: ToastHostState,
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

    init {
        // emit the name to meta
        //TODO: Add other fields in future versions.
        meta = MetaData(
            Text(
                when (key) {
                    Playback.PLAYLIST_FAVOURITE -> "Favourites"
                    Playback.PLAYLIST_RECENT -> "History"
                    else -> key
                }
            )
        )
    }

    val playlists = repository.playlists

    override val actions: List<Action> =
        mutableStateListOf(Action.PlaylistAdd, Action.PlayNext, Action.AddToQueue, Action.Delete)
    override val orders: List<GroupBy> = listOf(GroupBy.None, GroupBy.Name)
    override val mActions: List<Action?> = listOf(Action.Play, Action.Shuffle)

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
                    when (order) {
                        GroupBy.None -> mapOf(Text("") to src)
                        GroupBy.Name -> src.groupBy { Text(it.firstTitleChar) }
                        else -> error("$order invalid")
                    }
                }
        }.catch {
            // any exception.
            toaster.show(
                "Some unknown error occured!.",
                "Error",
                leading = Icons.Outlined.Error,
                accent = Color.Rose,
                duration = ToastHostState.Duration.Indefinite
            )
        }

    override fun toggleViewType() {
        // we only currently support single viewType. Maybe in future might support more.
        viewModelScope.launch {
            toaster.show("Toggle not implemented yet.", "ViewType")
        }
    }

    fun play(shuffle: Boolean) {}

    fun delete() {}

    fun playNext() {}

    fun addToQueue() {}

    override fun select(key: String) {
        super.select(key)
        // add actions if selected.size == 1
        val mutable = actions as SnapshotStateList
        when {
            selected.isEmpty() -> mutable.remove(Action.SelectAll)
            selected.size == 1 -> mutable.addDistinct(Action.SelectAll)
        }
    }

    fun addToPlaylist(name: String) {}

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
                style = Theme.typography.caption,
                text = value.subtitle,
                color = LocalContentColor.current.copy(ContentAlpha.medium),
                fontWeight = FontWeight.SemiBold,
            )
        },
        text = {
            Label(
                text = value.title,
                style = Theme.typography.body1,
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
                            Theme.colors.primary.copy(ChipDefaults.OutlinedBorderOpacity)
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
    // perform action. show dialogs maybe.
    val onPerformAction = { action: Action ->
        when (action) {
            // show dialog
            Action.PlaylistAdd, Action.Properties -> confirm = action
            Action.AddToQueue -> viewModel.addToQueue()
            Action.PlayNext -> viewModel.playNext()
            Action.Delete -> viewModel.delete()
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
        key = { it.id },
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
                        // remove focus
                        viewModel.focused = ""
                        viewModel.select(member.uri)
                    }
                )
                .padding(horizontal = ContentPadding.normal)
        )
    }
}
package com.prime.media.directory.store

import android.provider.MediaStore
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.Error
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.prime.media.*
import com.prime.media.R
import com.prime.media.core.ContentPadding
import com.prime.media.core.compose.LocalNavController
import com.prime.media.impl.Repository
import com.prime.media.core.compose.Channel
import com.prime.media.core.db.*
import com.prime.media.core.util.PathUtils
import com.prime.media.directory.*
import com.prime.media.impl.block
import com.prime.media.settings.Settings
import com.primex.core.DahliaYellow
import com.primex.core.Text
import com.primex.material2.Label
import com.primex.core.Rose
import com.primex.preferences.Preferences
import com.primex.preferences.value
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

private const val TAG = "AlbumsViewModel"


typealias Folders = FoldersViewModel.Companion

private val Folder.firstTitleChar
    inline get() = name.uppercase(Locale.ROOT)[0].toString()

@HiltViewModel
class FoldersViewModel @Inject constructor(
    handle: SavedStateHandle,
    private val repository: Repository,
    private val toaster: Channel,
    private val preferences: Preferences
) : DirectoryViewModel<Folder>(handle) {

    companion object {
        private const val HOST = "_local_folders"

        val route = compose(HOST)
        fun direction(
            query: String = NULL_STRING,
            order: GroupBy = GroupBy.Name,
            ascending: Boolean = true,
            viewType: ViewType = ViewType.List
        ) = compose(HOST, NULL_STRING, query, order, ascending, viewType)
    }

    init {
        // emit the name to meta
        //TODO: Add other fields in future versions.
        meta = MetaData(Text("Folders"))
    }

    override fun toggleViewType() {
        // we only currently support single viewType. Maybe in future might support more.
        viewModelScope.launch {
            toaster.show("Toggle not implemented yet.", "ViewType")
        }
    }

    override val actions: List<Action> = emptyList()
    override val orders: List<GroupBy> = listOf(GroupBy.None, GroupBy.Name)
    override val mActions: List<Action?> = emptyList()

    override val data: Flow<Any> =
        repository.observe(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)
            .combine(filter) { f1, f2 -> f2 }.map {
                val (order, query, ascending) = it
                val list = repository.getFolders(query, ascending)
                when (order) {
                    GroupBy.None -> mapOf(Text("") to list)
                    GroupBy.Name -> list.groupBy { folder -> Text(folder.firstTitleChar) }
                    else -> error("$order invalid")
                }
            }
            .catch {
                // any exception.
                toaster.show(
                    "Some unknown error occured!. ${it.message}",
                    "Error",
                    leading = Icons.Outlined.Error,
                    accent = Color.Rose,
                    duration = Channel.Duration.Indefinite
                )
            }
            .stateIn(viewModelScope, SharingStarted.Lazily, emptyMap())

    fun exclude(path: String) {
        viewModelScope.launch {
            with(toaster) {
                val list = preferences.value(Settings.BLACKLISTED_FILES)
                val name = PathUtils.name(path)
                val response = show(
                    Text(R.string.block_folder_warning_msg, name),
                    Text(id = R.string.warning),
                    Text(id = R.string.add),
                    Icons.Outlined.Block,
                    Color.DahliaYellow,
                    duration = Channel.Duration.Short,
                )
                if (response == Channel.Result.Dismissed)
                    return@launch
                val result = preferences.block(path)
                if (result > 0)
                    show(
                        Text(R.string.block_success_msg, name),
                        Text(R.string.blacklist),
                        leading = Icons.Outlined.Block,
                        accent = Color.Rose,
                        duration = Channel.Duration.Short,
                    )
                else
                    show(R.string.error_msg, R.string.error)
            }
        }
    }
}

private val TILE_WIDTH = 80.dp
private val GridItemPadding =
    PaddingValues(vertical = 6.dp, horizontal = 10.dp)
private val folderIcon = Icons.Default.Folder

@Composable
fun Folder(
    value: Folder,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            // clip the ripple
            .clip(Material.shapes.medium)
            .then(modifier)
            // add padding after size.
            .padding(GridItemPadding)
            // add preferred size with min/max width
            .then(Modifier.width(TILE_WIDTH))
            // wrap the height of the content
            .wrapContentHeight(),
    ) {

        Icon(
            imageVector = folderIcon,
            contentDescription = null,
            modifier = Modifier
                .sizeIn(maxWidth = 70.dp)
                .fillMaxWidth()
                .aspectRatio(1.0f)
        )


        // title
        Label(
            text = value.name,
            maxLines = 2,
            modifier = Modifier.padding(top = ContentPadding.medium),
            style = Material.typography.caption,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Folders(viewModel: FoldersViewModel) {
    val navigator = LocalNavController.current
    Directory(
        viewModel = viewModel,
        cells = GridCells.Adaptive(TILE_WIDTH + (4.dp * 2)),
        onAction = {},
        key = { it.path },
        contentPadding = PaddingValues(horizontal = ContentPadding.normal),
    ) {
        Folder(
            value = it,
            modifier = Modifier
                .combinedClickable(
                    onClick = {
                        val direction = Audios.direction(Audios.GET_FROM_FOLDER, it.path)
                        navigator.navigate(direction)
                    },
                    onLongClick = { viewModel.exclude(it.path) }
                )
                .animateItemPlacement()
        )
    }
}
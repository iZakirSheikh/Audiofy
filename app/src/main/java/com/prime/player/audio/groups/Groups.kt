package com.prime.player.audio.groups


import android.annotation.SuppressLint
import androidx.annotation.DrawableRes
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prime.player.PlayerTheme
import com.prime.player.R
import com.prime.player.audio.AlbumArt
import com.prime.player.audio.AudioNavigationActions
import com.prime.player.audio.GroupOf
import com.prime.player.audio.Toolbar
import com.prime.player.core.LocalPlaylists
import com.prime.player.core.models.*
import com.prime.player.extended.*
import com.prime.player.extended.managers.LocalAdvertiser

private const val CHUNK_SIZE = 3

@Composable
fun Groups(padding: State<PaddingValues>, viewModel: GroupsViewModel) {
    with(viewModel) {
        Scaffold(
            topBar = { Toolbar(title = title) }
        ) { inner ->
            val state by result.state.collectAsState()
            Crossfade(
                targetState = state,
                modifier = Modifier
                    .padding(inner)
                    .fillMaxSize()
            ) { value ->
                when (value) {
                    Resource.State.Loading -> PlaceHolder(
                        lottieResource = R.raw.loading,
                        message = "Loading...",
                        modifier = Modifier.fillMaxSize()
                    )
                    Resource.State.Success -> result.Grid(padding = padding)
                    Resource.State.Error -> PlaceHolder(
                        lottieResource = R.raw.error,
                        message = result.message ?: "",
                        modifier = Modifier.fillMaxSize()
                    )
                    Resource.State.Empty -> PlaceHolder(
                        lottieResource = R.raw.empty,
                        message = result.message ?: "",
                        modifier = Modifier.fillMaxSize()
                    )
                }

            }
        }
    }
}

@Composable
private fun GrpResult.Grid(padding: State<PaddingValues>) {
    val padding by padding
    val data by data
    data?.let { map ->
        LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = padding) {
            map.forEach { (header, list) ->
                val char = header[0]
                item(key = header) {
                    val primary = PlayerTheme.colors.secondary
                    Header(
                        text = "$char",
                        modifier = Modifier
                            .padding(
                                horizontal = Padding.EXTRA_LARGE
                            ),
                        style = PlayerTheme.typography.h4,
                        fontWeight = FontWeight.SemiBold,
                        color = primary
                    )
                    Divider(
                        modifier = Modifier
                            .padding(
                                start = Padding.EXTRA_LARGE,
                                end = Padding.LARGE
                            )
                            .padding(
                                vertical = Padding.MEDIUM
                            ),
                        color = primary.copy(0.12f)
                    )
                }

                list.chunked(CHUNK_SIZE).forEach { chunk ->
                    item {
                        Row(
                            modifier = Modifier
                                .padding(horizontal = Padding.LARGE)
                                .fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            emitOrElse(chunk = chunk) { value ->
                                Group(group = value)
                            }
                        }
                    }
                }

            }
        }
    }
}


@SuppressLint("ComposableNaming")
@Composable
private inline fun RowScope.emitOrElse(chunk: List<Any>, block: @Composable (Any) -> Unit) {
    for (i in 0 until CHUNK_SIZE) {
        val item = chunk.getOrNull(i)
        if (item == null)
            Spacer(modifier = Modifier.weight(1f))
        else
            block(item)
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RowScope.Group(group: Any) {
    val navigator = LocalNavActionProvider.current as AudioNavigationActions
    val advertiser = LocalAdvertiser.current

    val renameDialog = memorize {
        val playlist = group as Playlist
        val db = LocalPlaylists.get(LocalContext.current)
        TextInputDialog(
            title = "Rename Dialog",
            onDismissRequest = { newText ->
                newText?.let {
                    val newList = playlist.copy(name = it)
                    db.update(newList)
                }
                advertiser.show(false)
                hide()
            },
            subtitle = "Enter new playlist name",
            defaultValue = playlist.name,
            keyboardOptions = KeyboardOptions.Default.copy(
                keyboardType = KeyboardType.Text
            ),
            vectorIcon = Icons.Outlined.Edit
        )
    }

    val alertDialog = memorize {
        val playlist = group as Playlist
        val db = LocalPlaylists.get(LocalContext.current)
        AlertDialog(
            title = "Delete Alert",
            message = "You are about to delete playlist ${playlist.name}. This can't be undone. \nAre you sure!",
            vectorIcon = Icons.Outlined.DeleteForever,
            onDismissRequest = { confirm ->
                if (confirm) db.delete(playlist.id)
                advertiser.show(false)
                hide()
            }
        )
    }

    val playlistMenu = memorize {
        DropdownMenu(
            expanded = isVisible(),
            items = listOf(
                Icons.Default.Delete to "Delete",
                Icons.Default.Edit to "Rename"
            ),
            onDismissRequest = { hide() },
            onItemClick = { index ->
                when (index) {
                    0 -> alertDialog.show()
                    1 -> renameDialog.show()
                }
            }
        )
    }


    when (group) {
        is Album -> Album(
            title = group.title,
            count = group.audioList.size,
            albumID = group.id,
            modifier = Modifier
                .weight(1f)
                .aspectRatio(0.60f)
                .clickable {
                    navigator.toGroupViewer(GroupOf.ALBUMS, "${group.id}")
                    advertiser.show(false)
                }
        )
        is Folder -> Folder(
            title = group.name,
            count = group.audios.size,
            modifier = Modifier
                .weight(1f)
                .aspectRatio(0.8f)
                .clickable {
                    navigator.toGroupViewer(GroupOf.FOLDERS, group.name)
                    advertiser.show(false)
                }
        )
        is Artist -> Circular(
            title = group.name,
            count = group.audioList.size,
            id = R.drawable.ic_artist,
            modifier = Modifier
                .weight(1f)
                .aspectRatio(0.8f)
                .clickable {
                    navigator.toGroupViewer(GroupOf.ARTISTS, "${group.id}")
                    advertiser.show(false)
                }
        )
        is Playlist -> Circular(
            title = group.name,
            count = group.audios.size,
            id = R.drawable.ic_playlist,
            modifier = Modifier
                .weight(1f)
                .aspectRatio(0.8f)
                .combinedClickable(
                    onClick = {
                        navigator.toGroupViewer(GroupOf.PLAYLISTS, "${group.id}")
                        advertiser.show(false)
                    },
                    onLongClick = { playlistMenu.show() }
                )
        )
        is Genre -> Genre(
            title = group.name,
            count = group.audios.size,
            modifier = Modifier
                .weight(1f)
                .aspectRatio(0.8f)
                .clickable {
                    navigator.toGroupViewer(GroupOf.GENRES, "${group.id}")
                    advertiser.show(false)
                }
        )
    }
}


@Composable
private fun Album(
    modifier: Modifier = Modifier, title: String,
    count: Int,
    albumID: Long,
) {
    val resources = LocalContext.current.resources
    Column(modifier = modifier) {
        Frame(
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .padding(top = Padding.SMALL)
                .align(Alignment.CenterHorizontally)
                .widthIn(max = 80.dp)
                .weight(0.65f),
            elevation = Elevation.HIGH
        ) {
            AlbumArt(
                contentDescription = null,
                albumId = albumID,
                modifier = Modifier.fillMaxSize()
            )
        }

        Label(
            text = title,
            maxLines = 2,
            modifier = Modifier
                .padding(top = Padding.MEDIUM, start = Padding.MEDIUM, end = Padding.MEDIUM)
                .fillMaxWidth(),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
        )
        Caption(
            text = resources.getQuantityString(
                R.plurals.item,
                count,
                count
            ),
            modifier = Modifier.padding(
                start = Padding.MEDIUM,
                end = Padding.MEDIUM,
                bottom = Padding.SMALL
            )
        )
    }
}

@Composable
private fun Genre(modifier: Modifier = Modifier, title: String, count: Int) {
    val resources = LocalContext.current.resources
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Frame(
            color = Color.Transparent,
            shape = CircleShape,
            modifier = Modifier
                .sizeIn(maxWidth = 70.dp)
                .fillMaxWidth()
                .aspectRatio(1.0f),
            contentColor = PlayerTheme.colors.onBackground,
            border = BorderStroke(3.dp, PlayerTheme.colors.onBackground),
        ) {
            val char = title[0].uppercaseChar()
            Header(
                text = "$char",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.wrapContentSize(),
                style = PlayerTheme.typography.h4
            )
        }

        Label(
            text = title,
            maxLines = 2,
            modifier = Modifier
                .padding(top = Padding.MEDIUM, start = Padding.MEDIUM, end = Padding.MEDIUM)
                .fillMaxWidth(),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Caption(
            text = resources.getQuantityString(
                R.plurals.item,
                count,
                count
            ),
            modifier = Modifier.padding(
                start = Padding.MEDIUM,
                end = Padding.MEDIUM,
                bottom = Padding.SMALL
            ),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun Folder(
    modifier: Modifier = Modifier,
    count: Int,
    title: String
) {
    val resources = LocalContext.current.resources
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Folder, contentDescription = null, modifier = Modifier
                .sizeIn(maxWidth = 70.dp)
                .fillMaxWidth()
                .aspectRatio(1.0f)
        )

        Label(
            text = title,
            maxLines = 2,
            modifier = Modifier
                .padding(top = Padding.MEDIUM, start = Padding.MEDIUM, end = Padding.MEDIUM)
                .fillMaxWidth(),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Caption(
            text = resources.getQuantityString(
                R.plurals.item,
                count,
                count
            ),
            modifier = Modifier.padding(
                start = Padding.MEDIUM,
                end = Padding.MEDIUM,
                bottom = Padding.SMALL
            ),
            textAlign = TextAlign.Center
        )
    }
}


@Composable
private fun Circular(
    modifier: Modifier = Modifier,
    count: Int,
    title: String,
    @DrawableRes id: Int
) {
    val resources = LocalContext.current.resources
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Frame(
            color = PlayerTheme.colors.surface,
            shape = CircleShape,
            modifier = Modifier
                .padding(top = Padding.MEDIUM)
                .sizeIn(maxWidth = 70.dp)
                .fillMaxWidth()
                .aspectRatio(1.0f),
            elevation = Elevation.MEDIUM
        ) {
            Icon(
                painter = painterResource(id = id),
                contentDescription = null,
                modifier = Modifier.requiredSize(40.dp)
            )
        }

        Label(
            text = title,
            maxLines = 2,
            modifier = Modifier
                .padding(top = Padding.MEDIUM, start = Padding.MEDIUM, end = Padding.MEDIUM)
                .fillMaxWidth(),
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center
        )
        Caption(
            text = resources.getQuantityString(
                R.plurals.item,
                count,
                count
            ),
            modifier = Modifier.padding(
                start = Padding.MEDIUM,
                end = Padding.MEDIUM,
                bottom = Padding.SMALL
            ),
            textAlign = TextAlign.Center
        )
    }
}
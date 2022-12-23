@file:OptIn(ExperimentalMaterialApi::class)
@file:Suppress("FunctionName")

package com.prime.player.audio.buckets

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.twotone.Delete
import androidx.compose.material.icons.twotone.Edit
import androidx.compose.material.icons.twotone.PlaylistAdd
import androidx.compose.material.icons.twotone.ReplyAll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prime.player.*
import com.prime.player.R
import com.prime.player.audio.Image
import com.prime.player.audio.Type
import com.prime.player.audio.tracks.TracksRoute
import com.prime.player.common.FileUtils
import com.prime.player.common.Util
import com.prime.player.common.compose.*
import com.prime.player.common.formatAsRelativeTimeSpan
import com.prime.player.common.toFormattedDataUnit
import com.prime.player.core.*
import com.primex.core.*
import com.primex.ui.*
import cz.levinzonr.saferoute.core.annotations.Route
import cz.levinzonr.saferoute.core.annotations.RouteArg
import cz.levinzonr.saferoute.core.annotations.RouteNavGraph
import cz.levinzonr.saferoute.core.navigateTo


private val AlbumShape =
    RoundedCornerShape(8.dp)

private val GridItemPadding =
    PaddingValues(vertical = 6.dp, horizontal = 10.dp)

/**
 * The min grid cell size.
 */
private val MIN_CELL_SIZE = 80.dp
private val MAX_CELL_SIZE = 110.dp


private val TileSize =
    Modifier.sizeIn(minWidth = MIN_CELL_SIZE, maxWidth = MAX_CELL_SIZE)

@Composable
private inline fun Generic(
    title: String,
    subtitle: String,
    clickable: Modifier,
    icon: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            // clip the ripple
            .clip(Material.shapes.medium)
            .then(clickable)
            // add padding after size.
            .padding(GridItemPadding)
            // add preferred size with min/max width
            .then(TileSize)
            // wrap the height of the content
            .wrapContentHeight(),
    ) {

        // place here the icon
        icon()

        // title
        Label(
            text = title,
            maxLines = 2,
            modifier = Modifier.padding(top = ContentPadding.medium),
            style = Material.typography.caption,
        )

        // Subtitle
        Label(
            text = subtitle,
            style = Material.typography.caption2
        )
    }
}

@Composable
private fun Album(
    value: Album,
    fallback: Painter,
    modifier: Modifier = Modifier,
    onAlbumClick: () -> Unit
) {
    val count = value.tracks
    val context = LocalContext.current
    Generic(
        title = value.title,
        clickable = modifier.clickable(onClick = onAlbumClick),

        subtitle = stringQuantityResource(R.plurals.file, count, count) +
                " " + FileUtils.toFormattedDataUnit(context, value.size),

        // icon of this album
        icon = {
            Surface(
                shape = AlbumShape,
                elevation = ContentElevation.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.65f),
                content = {
                    Image(
                        albumId = value.id,
                        fallback = fallback
                    )
                },
            )
        }
    )
}

val GenreShape = CircleShape

@Composable
private fun Genre(
    value: Genre,
    modifier: Modifier = Modifier,
    onGenreClick: () -> Unit
) {
    val context = LocalContext.current
    Generic(
        title = value.name,
        clickable = modifier.clickable(onClick = onGenreClick),

        subtitle = run {
            val count = value.tracks
            stringQuantityResource(R.plurals.file, count, count) +
                    " " + FileUtils.toFormattedDataUnit(context, value.size)
        },
        icon = {
            Surface(
                color = Color.Transparent,
                border = BorderStroke(3.dp, Material.colors.onBackground),
                shape = GenreShape,

                modifier = Modifier
                    .sizeIn(maxWidth = 70.dp)
                    .aspectRatio(1.0f),

                content = {
                    Label(
                        text = "${value.name[0].uppercaseChar()}",
                        fontWeight = FontWeight.Bold,
                        style = Material.typography.h4,
                        modifier = Modifier.wrapContentSize(Alignment.Center)
                    )
                }
            )
        }
    )
}


private val folderIcon = Icons.Default.Folder

@Composable
private fun Folder(
    value: Folder,
    modifier: Modifier = Modifier,
    onFolderClick: () -> Unit
) {
    val context = LocalContext.current
    Generic(
        title = value.name,
        clickable = modifier.clickable(onClick = onFolderClick),

        subtitle = run {
            val count = value.cardinality
            stringQuantityResource(R.plurals.file, count, count) +
                    " " + FileUtils.toFormattedDataUnit(context, value.size)
        },

        icon = {
            Icon(
                imageVector = folderIcon,
                contentDescription = null,
                modifier = Modifier
                    .sizeIn(maxWidth = 70.dp)
                    .fillMaxWidth()
                    .aspectRatio(1.0f)
            )
        }
    )
}

@Composable
private fun Artist(
    value: Artist,
    modifier: Modifier = Modifier,
    onArtistClick: () -> Unit
) {
    val context = LocalContext.current
    Generic(
        title = value.name,
        clickable = modifier.clickable(onClick = onArtistClick),

        subtitle = run {
            val count = value.tracks
            stringQuantityResource(R.plurals.file, count, count) +
                    " " + FileUtils.toFormattedDataUnit(context, value.size)
        },

        icon = {

            Neumorphic(
                shape = CircleShape,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .sizeIn(maxWidth = 66.dp)
                    .aspectRatio(1.0f),
                elevation = ContentElevation.low,
                lightShadowColor = Material.colors.lightShadowColor,
                darkShadowColor = Material.colors.darkShadowColor,

                content = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_artist),
                        contentDescription = null,
                        modifier = Modifier
                            .requiredSize(40.dp)
                            .wrapContentSize(Alignment.Center)
                    )
                }
            )
        }
    )
}


private val PlaylistShape = RoundedCornerShape(20)
private val PlaylistIcon = Icons.Outlined.PlaylistPlay

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun Playlist(
    value: Playlist,
    modifier: Modifier = Modifier,
    onPlaylistClick: () -> Unit,
    onLongClick: () -> Unit
) {
    Generic(
        title = value.name,
        subtitle = Util.formatAsRelativeTimeSpan(value.dateCreated),

        clickable = modifier.combinedClickable(
            onClick = onPlaylistClick,
            onLongClick = onLongClick
        ),

        icon = {
            Neumorphic(
                shape = PlaylistShape,
                modifier = Modifier
                    .sizeIn(maxWidth = 70.dp)
                    .aspectRatio(1.0f),
                elevation = ContentElevation.low,
                lightShadowColor = Material.colors.lightShadowColor,
                darkShadowColor = Material.colors.darkShadowColor,

                content = {
                    Icon(
                        imageVector = PlaylistIcon,
                        contentDescription = null,
                        modifier = Modifier
                            .requiredSize(40.dp)
                            .wrapContentSize(Alignment.Center)
                    )
                }
            )
        }
    )
}


@Composable
private inline fun EditDialog(
    expanded: Boolean,
    title: String,
    placeholder: String,
    noinline onDismissRequest: (name: String?) -> Unit
) {
    if (expanded)
        TextInputDialog(
            title = title,
            onDismissRequest = onDismissRequest,
            label = placeholder,
            vectorIcon = Icons.TwoTone.Edit,
            textFieldShape = RoundedCornerShape(20),
            topBarContentColor = Material.colors.onBackground,
            topBarBackgroundColor = Material.colors.overlay
        )
}

context (BucketsViewModel) @Composable
private fun More(
    value: Playlist,
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable () -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {},
        content = {
            var showRenameDialog by rememberState(initial = false)
            val channel = LocalContext.toastHostState
            EditDialog(
                expanded = showRenameDialog,
                title = "Rename",
                placeholder = "Enter new name",
                onDismissRequest = { new ->
                    when (new) {
                        // just dismiss
                        null -> {}
                        // try to rename to new name.
                        else -> rename(value, new, channel)
                    }
                    showRenameDialog = false
                }
            )

            var showDeleteDialog by rememberState(initial = false)
            if (showDeleteDialog)
                AlertDialog(
                    title = "Delete",
                    message = "You are about to delete playlist ${value.name}. This can't be undone. \nAre you sure?",
                    vectorIcon = Icons.Outlined.DeleteForever,
                    onDismissRequest = { action ->
                        when (action) {
                            true -> delete(value, channel)
                            else -> {}
                        }
                        showDeleteDialog = false
                    }
                )

            content()
            // actual menu.
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = onDismissRequest,
                items = listOf(
                    Icons.TwoTone.Delete to "Delete",
                    Icons.TwoTone.Edit to "Rename"
                ),
                onItemClick = { index ->
                    when (index) {
                        0 -> showDeleteDialog = true
                        else -> showRenameDialog = true
                    }
                }
            )

        }
    )
}


private const val CONTENT_TYPE_GRID_ITEM = "grid_item"
private const val CONTENT_TYPE_HEADER = "grid_header"

@Composable
private fun Header(text: String) {
    val color = Material.colors.secondary
    Header(
        text = text,
        style = Material.typography.h4,
        fontWeight = FontWeight.Bold,
        color = color,

        modifier = Modifier
            .drawHorizontalDivider(
                color = color,
                indent = PaddingValues(bottom = ContentPadding.normal)
            )
            .fillMaxWidth()
            .padding(
                horizontal = ContentPadding.large,
                vertical = ContentPadding.normal
            ),
    )
}


context (BucketsViewModel) private inline fun LazyGridScope.Playlists(value: List<Playlist>) {
    items(value, contentType = { CONTENT_TYPE_GRID_ITEM }, key = { it.id }) { playlist: Playlist ->
        val navigator = LocalNavController.current
        var expanded by rememberState(initial = false)
        More(value = playlist, expanded = expanded, onDismissRequest = { expanded = false }) {
            Playlist(
                value = playlist,
                onLongClick = { expanded = true },
                onPlaylistClick = {
                    val encoded = Uri.encode(playlist.name)
                    navigator.navigateTo(
                        TracksRoute(Type.PLAYLISTS.name, encoded)
                    )
                }
            )
        }
    }
}

private inline fun LazyGridScope.Genres(value: List<Genre>) {
    items(value, contentType = { CONTENT_TYPE_GRID_ITEM }, key = { it.name }) { genre ->
        val navigator = LocalNavController.current
        Genre(
            value = genre,
            onGenreClick = {
                val encoded = Uri.encode(genre.name)
                navigator.navigateTo(
                    TracksRoute(Type.GENRES.name, encoded)
                )
            }
        )
    }
}

private inline fun LazyGridScope.Albums(value: List<Album>, fallback: Painter) {
    items(value, contentType = { CONTENT_TYPE_GRID_ITEM }) { album ->
        val navigator = LocalNavController.current
        Album(
            value = album,
            fallback = fallback,
            onAlbumClick = {
                val encoded = Uri.encode(album.title)
                navigator.navigateTo(
                    TracksRoute(Type.ALBUMS.name, encoded)
                )
            }
        )
    }
}

private inline fun LazyGridScope.Buckets(value: List<Folder>) {
    items(value, contentType = { CONTENT_TYPE_GRID_ITEM }, key = { it.path }) { bucket ->
        val navigator = LocalNavController.current
        Folder(
            value = bucket,
            onFolderClick = {
                val encoded = Uri.encode(bucket.path)
                navigator.navigateTo(
                    TracksRoute(Type.FOLDERS.name, encoded)
                )
            }
        )
    }
}

private inline fun LazyGridScope.Artists(value: List<Artist>) {
    items(value, contentType = { CONTENT_TYPE_GRID_ITEM }, key = { it.name }) { artist ->
        val navigator = LocalNavController.current
        Artist(
            value = artist,
            onArtistClick = {
                val uri = Uri.encode(artist.name)
                navigator.navigateTo(
                    TracksRoute(Type.ARTISTS.name, uri)
                )
            }
        )
    }
}


@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Route(
    args = [RouteArg(name = "type", type = String::class)],
    navGraph = RouteNavGraph(start = false)
)
@Composable
fun Buckets(
    viewModel: BucketsViewModel
) {
    Scaffold(
        topBar = {
            val colorize by Material.colorStatusBar
            val navigator = LocalNavController.current

            NeumorphicTopAppBar(
                title = { Label(text = viewModel.type.name) },
                elevation = ContentElevation.low,
                shape = CircleShape,
                lightShadowColor = Material.colors.lightShadowColor,
                darkShadowColor = Material.colors.darkShadowColor,

                modifier = Modifier
                    .statusBarsPadding2(
                        color = if (colorize) Material.colors.primaryVariant else Color.Transparent,
                        darkIcons = !colorize && Material.colors.isLight
                    )
                    .drawHorizontalDivider(color = Material.colors.onSurface)
                    .padding(vertical = ContentPadding.medium),

                navigationIcon = {
                    IconButton(
                        onClick = { navigator.navigateUp() },
                        imageVector = Icons.TwoTone.ReplyAll,
                        contentDescription = null
                    )
                },
            )
        },

        floatingActionButton = {
            val show = viewModel.isTypePlaylists
            if (show) {
                val channel = LocalContext.toastHostState
                var show by rememberState(initial = false)

                // Dialog to add the name of the playlist
                EditDialog(
                    expanded = show,
                    title = "Create Playlist",
                    placeholder = "Enter playlist name",
                    onDismissRequest = { name ->
                        when (name) {
                            // simply do-no and  dismiss
                            null -> {}
                            // other wise if name might exist.
                            // else create the playlist.
                            else -> viewModel.createPlaylist(name, channel)
                        }
                        show = false
                    }
                )

                // actual content
                FloatingActionButton(
                    onClick = { show = true },
                    shape = RoundedCornerShape(30),
                    modifier = Modifier.padding(LocalWindowPadding.current),
                    content = {
                        // the icon of the Fab
                        Icon(
                            imageVector = Icons.TwoTone.PlaylistAdd,
                            contentDescription = null
                        )
                    }
                )
            }
        },

        content = {
            val result = viewModel.result
            val fallback = painterResource(id = R.drawable.default_art)
            val fullLineSpan: (LazyGridItemSpanScope.() -> GridItemSpan) =
                { GridItemSpan(maxLineSpan) }


            Placeholder(
                value = result,
                modifier = Modifier.padding(it)
            ) { data ->
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(MIN_CELL_SIZE + (4.dp * 2)),
                    contentPadding = PaddingValues(horizontal = ContentPadding.normal) + LocalWindowPadding.current
                ) {
                    val type = viewModel.type
                    data.forEach { (header, list) ->

                        // Header
                        item(
                            key = header,
                            span = fullLineSpan,
                            contentType = CONTENT_TYPE_HEADER,
                            content = {
                                Header(text = header)
                            }
                        )

                        when (type) {
                            Type.PLAYLISTS -> with(viewModel) { Playlists(castTo(list)) }
                            Type.FOLDERS -> Buckets(castTo(list))
                            Type.ARTISTS -> Artists(castTo(list))
                            Type.ALBUMS -> Albums(castTo(list), fallback)
                            Type.GENRES -> Genres(castTo(list))
                            else -> error("Unknown type $type")
                        }
                    }
                }
            }
        }
    )
}
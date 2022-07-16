@file:OptIn(ExperimentalMaterialApi::class)
@file:Suppress("FunctionName")

package com.prime.player.audio.buckets

import android.annotation.SuppressLint
import android.net.Uri
import androidx.compose.animation.Crossfade
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prime.player.Material
import com.prime.player.R
import com.prime.player.audio.AsyncImage
import com.prime.player.audio.Tokens
import com.prime.player.audio.Type
import com.prime.player.audio.tracks.TracksRoute
import com.prime.player.caption2
import com.prime.player.common.FileUtils
import com.prime.player.common.Utils
import com.prime.player.common.castTo
import com.prime.player.common.compose.*
import com.prime.player.core.Audio
import com.prime.player.core.Playlist
import com.prime.player.core.name
import com.prime.player.primary
import com.prime.player.settings.GlobalKeys
import com.primex.core.Result
import com.primex.core.plus
import com.primex.core.rememberState
import com.primex.preferences.LocalPreferenceStore
import com.primex.ui.Header
import com.primex.ui.Label
import com.primex.ui.TextInputDialog
import cz.levinzonr.saferoute.core.annotations.Route
import cz.levinzonr.saferoute.core.annotations.RouteArg
import cz.levinzonr.saferoute.core.annotations.RouteNavGraph
import cz.levinzonr.saferoute.core.navigateTo


private val AlbumShape = RoundedCornerShape(8.dp)
private val GridItemPadding = PaddingValues(vertical = 6.dp, horizontal = 10.dp)

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
            style = Material.typography.caption2,
            color = LocalContentColor.current.copy(ContentAlpha.medium)
        )
    }
}

@Composable
private fun TopAppBar(
    title: String,
    modifier: Modifier = Modifier
) {
    val navigator = LocalNavController.current
    NeumorphicTopAppBar(
        title = { Label(text = title) },
        modifier = modifier.padding(top = ContentPadding.medium),
        elevation = ContentElevation.low,
        shape = Tokens.DefaultCircleShape,
        navigationIcon = {
            IconButton(
                onClick = { navigator.navigateUp() },
                imageVector = Icons.TwoTone.ReplyAll,
                contentDescription = null
            )
        },
    )
}


@Composable
private fun Album(
    value: Audio.Album,
    fallback: Painter,
    modifier: Modifier = Modifier,
    onAlbumClick: () -> Unit
) {
    val count = value.tracks
    Generic(
        title = value.title,
        clickable = modifier.clickable(onClick = onAlbumClick),

        subtitle = stringQuantityResource(R.plurals.file, count, count) +
                " " + FileUtils.toFormattedDataUnit(value.size),

        // icon of this album
        icon = {
            Surface(
                shape = AlbumShape,
                elevation = ContentElevation.high,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.65f),
                content = {
                    AsyncImage(
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
    value: Audio.Genre,
    modifier: Modifier = Modifier,
    onGenreClick: () -> Unit
) {

    Generic(
        title = value.name,
        clickable = modifier.clickable(onClick = onGenreClick),

        subtitle = run {
            val count = value.tracks
            stringQuantityResource(R.plurals.file, count, count) +
                    " " + FileUtils.toFormattedDataUnit(value.size)
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
    value: Audio.Bucket,
    modifier: Modifier = Modifier,
    onFolderClick: () -> Unit
) {
    Generic(
        title = value.name,
        clickable = modifier.clickable(onClick = onFolderClick),

        subtitle = run {
            val count = value.cardinality
            stringQuantityResource(R.plurals.file, count, count) +
                    " " + FileUtils.toFormattedDataUnit(value.size)
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
    value: Audio.Artist,
    modifier: Modifier = Modifier,
    onArtistClick: () -> Unit
) {
    Generic(
        title = value.name,
        clickable = modifier.clickable(onClick = onArtistClick),

        subtitle = run {
            val count = value.tracks
            stringQuantityResource(R.plurals.file, count, count) +
                    " " + FileUtils.toFormattedDataUnit(value.size)
        },

        icon = {

            Neumorphic(
                shape = CircleShape,
                modifier = Modifier
                    .padding(top = 6.dp)
                    .sizeIn(maxWidth = 66.dp)
                    .aspectRatio(1.0f),
                elevation = ContentElevation.low,

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
        subtitle = Utils.formatAsRelativeTimeSpan(value.dateCreated),

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
            val channel = LocalSnackDataChannel.current
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
                com.primex.ui.AlertDialog(
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
            com.primex.ui.DropdownMenu(
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

context (BucketsViewModel)
        private inline fun LazyGridScope.Playlists(value: List<Playlist>) {
    items(value, contentType = { CONTENT_TYPE_GRID_ITEM }, key = { it.id }) { playlist: Playlist ->
        val navigator = LocalNavController.current
        var expanded by rememberState(initial = false)
        More(value = playlist, expanded = expanded, onDismissRequest = { expanded = false }) {
            Playlist(
                value = playlist,
                onLongClick = { expanded = true },
                onPlaylistClick = {
                    val uri = Uri.encode(playlist.name)
                    navigator.navigateTo(
                        TracksRoute(Type.PLAYLISTS.name, uri)
                    )
                }
            )
        }
    }
}

private inline fun LazyGridScope.Artists(value: List<Audio.Artist>) {
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

private inline fun LazyGridScope.Genres(value: List<Audio.Genre>) {
    items(value, contentType = { CONTENT_TYPE_GRID_ITEM }, key = { it.name }) { genre ->
        val navigator = LocalNavController.current
        Genre(
            value = genre,
            onGenreClick = {
                val uri = Uri.encode(genre.name)
                navigator.navigateTo(
                    TracksRoute(Type.GENRES.name, uri)
                )
            }
        )
    }
}

private inline fun LazyGridScope.Albums(value: List<Audio.Album>, fallback: Painter) {
    items(value, contentType = { CONTENT_TYPE_GRID_ITEM }, key = { it.id }) { album ->
        val navigator = LocalNavController.current
        Album(
            value = album,
            fallback = fallback,
            onAlbumClick = {
                val uri = Uri.encode(album.title)
                navigator.navigateTo(
                    TracksRoute(Type.ALBUMS.name, uri)
                )
            }
        )
    }
}

private inline fun LazyGridScope.Buckets(value: List<Audio.Bucket>) {
    items(value, contentType = { CONTENT_TYPE_GRID_ITEM }, key = { it.path }) { bucket ->
        val navigator = LocalNavController.current
        Folder(
            value = bucket,
            onFolderClick = {
                val uri = Uri.encode(bucket.path)
                navigator.navigateTo(
                    TracksRoute(Type.FOLDERS.name, uri)
                )
            }
        )
    }
}


context(BucketsViewModel) @Composable
private fun Grid(
    value: BucketResult,
    modifier: Modifier = Modifier
) {
    val contentPadding = LocalWindowPadding.current
    val fallback = painterResource(id = R.drawable.default_art)

    LazyVerticalGrid(
        modifier = modifier,
        columns = GridCells.Adaptive(MIN_CELL_SIZE + (4.dp * 2)),
        contentPadding = PaddingValues(horizontal = ContentPadding.normal) + contentPadding
    ) {
        val fullLineSpan: (LazyGridItemSpanScope.() -> GridItemSpan) =
            { GridItemSpan(maxLineSpan) }

        value.forEach { (header, list) ->
            item(key = header, span = fullLineSpan, contentType = CONTENT_TYPE_HEADER) {
                val color = Material.colors.secondary
                Header(
                    text = header,
                    style = Material.typography.h4,
                    fontWeight = FontWeight.Bold,
                    color = color,

                    modifier = Modifier
                        .drawHorizontalDivider(color = color, indent = PaddingValues(bottom = ContentPadding.normal))
                        .fillMaxWidth()
                        .padding(horizontal = ContentPadding.large, vertical = ContentPadding.normal),
                )
            }

            when (type) {
                Type.PLAYLISTS -> Playlists(castTo(list))
                Type.FOLDERS -> Buckets(castTo(list))
                Type.ARTISTS -> Artists(castTo(list))
                Type.ALBUMS -> Albums(castTo(list), fallback)
                Type.GENRES -> Genres(castTo(list))
                else -> error("Unknown type $type")
            }
        }
    }
}


@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Route(
    args = [RouteArg(name = "type", type = String::class)],
    navGraph = RouteNavGraph(start = false)
)
@Composable
fun Buckets(viewModel: BucketsViewModel) {
    with(viewModel) {

        Scaffold(
            topBar = {
                val colorStatusBar by with(LocalPreferenceStore.current) {
                    this[GlobalKeys.COLOR_STATUS_BAR].observeAsState()
                }
                TopAppBar(
                    title = type.name,

                    modifier = Modifier
                        .statusBarsPadding2(
                            color = Material.colors.primary(colorStatusBar, Color.Transparent),
                            darkIcons = !colorStatusBar && Material.colors.isLight
                        )
                        .drawHorizontalDivider(color = Material.colors.onSurface)
                        .padding(bottom = ContentPadding.medium)
                )
            },

            // floating action button
            // only in case type is playlist
            floatingActionButton = {
                if (isTypePlaylists) {
                    val channel = LocalSnackDataChannel.current
                    var show by rememberState(initial = false)
                    FloatingActionButton(
                        onClick = { show = true },
                        shape = RoundedCornerShape(30),
                        content = {

                            // the icon of the Fab
                            Icon(
                                imageVector = Icons.TwoTone.PlaylistAdd,
                                contentDescription = null
                            )

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
                                        else -> createPlaylist(name, channel)
                                    }
                                    show = false
                                }
                            )
                        }
                    )
                }
            },

            // the actual content
            // crossFading between states.
            content = {
                val (state, data) = result
                Crossfade(targetState = state) { value ->
                    val stateModifier = Modifier
                        .fillMaxSize()
                        .padding(LocalWindowPadding.current)
                    when (value) {
                        Result.State.Loading ->
                            Placeholder(
                                iconResId = R.raw.lt_loading_dots_blue,
                                title = "Loading",
                                modifier = stateModifier
                            )
                        is Result.State.Processing ->
                            Placeholder(
                                iconResId = R.raw.lt_loading_hand,
                                title = "Processing.",
                                modifier = stateModifier
                            )
                        is Result.State.Error ->
                            Placeholder(
                                iconResId = R.raw.lt_error,
                                title = "Error",
                                modifier = stateModifier
                            )
                        Result.State.Empty ->
                            Placeholder(
                                iconResId = R.raw.lt_empty_box,
                                title = "Oops Empty!!",
                                modifier = stateModifier
                            )
                        Result.State.Success -> Grid(data)
                    }
                }
            }
        )
    }
}
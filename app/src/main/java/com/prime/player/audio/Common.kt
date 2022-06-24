package com.prime.player.audio

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.widget.Toast
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer3
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import coil.annotation.ExperimentalCoilApi
import coil.compose.rememberImagePainter
import com.airbnb.lottie.LottieDrawable
import com.google.accompanist.insets.statusBarsPadding
import com.prime.player.App
import com.prime.player.PlayerTheme
import com.prime.player.R
import com.prime.player.core.AudioRepo
import com.prime.player.core.models.Audio
import com.prime.player.core.models.Playlist
import com.prime.player.extended.*
import com.prime.player.preferences.Preferences
import com.prime.player.preferences.requiresAccentThoroughly
import com.prime.player.preferences.requiresColoringStatusBar
import com.prime.player.utils.getAlbumArtUri
import com.prime.player.utils.toDuration
import com.prime.player.utils.toRelativeTimeSpan
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


@Composable
fun resolveAccentColor(): Color {
    val prefs = Preferences.get(LocalContext.current)
    val requires by with(prefs) { requiresAccentThoroughly().collectAsState() }
    return when {
        requires -> PlayerTheme.colors.primary
        else -> if (isLight()) PlayerTheme.colors.background else Color.JetBlack
    }
}

@OptIn(ExperimentalCoilApi::class)
@Composable
fun AlbumArt(
    modifier: Modifier = Modifier,
    contentDescription: String?,
    contentScale: ContentScale = ContentScale.Crop,
    error: Bitmap = App.DEFUALT_ALBUM_ART,
    alignment: Alignment = Alignment.Center,
    durationMillis: Int = Anim.DURATION_MEDIUM,
    albumId: Long,
) {
    val errorD by remember(error) {
        lazy {
            BitmapDrawable(Resources.getSystem(), error)
        }
    }
    Image(
        painter = rememberImagePainter(
            getAlbumArtUri(albumId),
            builder = {
                error(errorD)
            }
        ),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        alignment = alignment,
    )
}


@Composable
fun AlbumArt(
    modifier: Modifier = Modifier,
    contentDescription: String?,
    contentScale: ContentScale = ContentScale.Crop,
    error: Bitmap = App.DEFUALT_ALBUM_ART,
    alignment: Alignment = Alignment.Center,
    durationMillis: Int = Anim.DURATION_LONG,
    bitmap: Bitmap?
) {

    val value = bitmap ?: error

    Crossfade(
        targetState = value,
        modifier = modifier,
        animationSpec = tween(durationMillis)
    ) { value ->
        Image(
            bitmap = value.asImageBitmap(),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
            alignment = alignment,
        )
    }
}


@Composable
fun Track(
    modifier: Modifier = Modifier,
    overline: String? = null,
    title: String,
    subtitle: String,
    albumID: Long,
    playing: Boolean = false,
) {

    val icon = @Composable {
        Frame(
            modifier = Modifier.requiredSize(60.dp),
            shape = CircleShape,
            border = BorderStroke(3.dp, Color.White),
            elevation = Elevation.MEDIUM
        ) {
            AlbumArt(contentDescription = null, albumId = albumID)
            if (playing)
                Lottie(
                    res = R.raw.playback_indicator,
                    modifier = Modifier.requiredSize(24.dp),
                    repeatX = LottieDrawable.INFINITE
                )
        }
    }

    val overliner: @Composable() (() -> Unit)? = overline?.let {
        @Composable {
            Label(text = it)
        }
    }

    val text = @Composable {
        Label(
            text = title,
            fontWeight = FontWeight.SemiBold
        )
    }

    val secondary = @Composable {
        Label(
            text = subtitle,
            fontWeight = FontWeight.SemiBold
        )
    }

    ListItem(
        modifier = Modifier
            .fillMaxWidth()
            .then(modifier)
            .padding(horizontal = Padding.LARGE, vertical = Padding.MEDIUM),
        icon = icon,
        overlineText = overliner,
        text = text,
        secondaryText = secondary
    )
}


@Composable
private fun Property(title: String, subtitle: String, icon: ImageVector) {
    ListItem(
        secondaryText = {
            Label(
                text = subtitle,
                maxLines = 3,
                modifier = Modifier.padding(start = Padding.LARGE),
                fontWeight = FontWeight.SemiBold
            )
        },
        icon = {
            Icon(imageVector = icon, contentDescription = null)
        },
        modifier = Modifier.padding(top = Padding.MEDIUM)
    ) {
        Caption(
            text = title,
            modifier = Modifier.padding(start = Padding.LARGE),
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun TrackInfo(of: Audio, onDismissRequest: () -> Unit) {
    PrimeDialog(
        title = "Properties",
        onDismissRequest = onDismissRequest,
        vectorIcon = Icons.Outlined.Info,
        button2 = stringResource(id = R.string.dismiss) to onDismissRequest,
    ) {
        Column(modifier = Modifier.padding(horizontal = Padding.LARGE, vertical = Padding.MEDIUM)) {
            Property(title = "Title", subtitle = of.title, icon = Icons.Outlined.Title)
            Property(
                title = "Path",
                subtitle = of.path ?: "Not available",
                icon = Icons.Outlined.LocationCity
            )
            Property(
                title = "Album",
                subtitle = of.album?.title ?: stringResource(id = R.string.unknown),
                icon = Icons.Outlined.Album
            )
            Property(
                title = "Artist",
                subtitle = of.artist?.name ?: stringResource(id = R.string.unknown),
                icon = Icons.Outlined.Person
            )
            Property(
                title = "Track number",
                subtitle = "${of.trackNumber}",
                icon = Icons.Outlined.FormatListNumbered
            )
            Property(title = "Year", subtitle = "${of.year}", icon = Icons.Outlined.DateRange)
            Property(
                title = "Duration",
                subtitle = toDuration(of.duration),
                icon = Icons.Default.Timer3
            )
            Property(
                title = "Date Modified",
                subtitle = toRelativeTimeSpan(of.dateModified).toString(),
                icon = Icons.Outlined.Update
            )
        }
    }
}


@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AddToPlaylist(audios: List<Long>, onDismissRequest: () -> Unit) {
    val context = LocalContext.current
    val repo = AudioRepo.get(context = context)
    val playlists by repo.playlists.collectAsState()
    val scope = rememberCoroutineScope()
    val iDialog = memorize {
        TextInputDialog(
            title = "Create Playlist",
            vectorIcon = Icons.Outlined.Edit,
            label = "Enter playlist name."
        ) { value ->
            value?.let { new ->
                scope.launch {
                    val msg = when {
                        playlists.find { new == it.name } != null -> "Playlist $new already exists"
                        else -> {
                            val id = repo.createPlaylist(new)
                            if (id == null)
                                "An error occurred while creating playlist $new!"
                            else
                                "Playlist $new created successfully!"
                        }
                    }
                    Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
                    hide()
                }
            } ?: run {
                //just dismiss it
                hide()
            }
        }
    }

    if (!iDialog.isVisible())
        PrimeDialog(
            title = "Add to Playlist",
            onDismissRequest = onDismissRequest,
            vectorIcon = Icons.Outlined.PlaylistAdd,
            button2 = stringResource(id = R.string.dismiss) to onDismissRequest,
            subtitle = "Click to add tracks to playlist.",
        ) {
            Scaffold(
                modifier = Modifier.heightIn(
                    max = with(LocalDensity.current) { displayHeight * 0.4f },
                    min = 200.dp
                ),
                backgroundColor = Color.Transparent,
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = { iDialog.show() },
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "add new playlist"
                        )
                    }
                }
            ) {
                Crossfade(targetState = playlists) {
                    when {
                        it.isEmpty() -> PlaceHolder(
                            message = "No Playlist available",
                            lottieResource = R.raw.empty,
                            modifier = Modifier.fillMaxSize()
                        )
                        else -> PlaylistGrid(playlists = it) {
                            Toast.makeText(
                                context,
                                "Adding tracks to Playlist ${it.name}.",
                                Toast.LENGTH_SHORT
                            ).show()
                            repo.addToPlaylist(audios, name = it.name)
                        }
                    }
                }
            }
        }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlaylistGrid(playlists: List<Playlist>, onClick: (Playlist) -> Unit) {
    val repo = AudioRepo.get(LocalContext.current)
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(vertical = Padding.LARGE)
    ) {
        items(playlists) { playlist ->
            val last = playlist.audios.lastOrNull()
            val albumID = last?.let { repo.getAudioById(it) }?.album?.id ?: -1
            Playlist(albumID = albumID, title = playlist.name, modifier = Modifier.clickable {
                onClick(playlist)
            })
        }
    }
}

/*
* Representing recent Item
* */

@Composable
private fun Playlist(
    modifier: Modifier = Modifier,
    albumID: Long,
    title: String
) {
    val circle = CircleShape
    val elevationPx = with(LocalDensity.current) { 8.dp.toPx() }
    Column(
        modifier = modifier.width(70.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        val forceAccent by with(Preferences.get(LocalContext.current)) { requiresAccentThoroughly().collectAsState() }
        val color = if (forceAccent) PlayerTheme.colors.primary else PlayerTheme.colors.onSurface

        AlbumArt(
            contentDescription = "Artwork",
            albumId = albumID,
            modifier = Modifier
                //.padding(8.dp)
                .graphicsLayer {
                    this.shadowElevation = elevationPx
                    shape = circle
                }
                .border(width = 2.dp, color = color, shape = circle)
                .padding(Padding.SMALL)
                .clip(circle)
                .verticalGradient()
                .requiredSize(50.dp)
        )

        Caption(
            text = title,
            modifier = Modifier
                .padding(vertical = Padding.SMALL)
                .fillMaxWidth(),
            textAlign = TextAlign.Center,
            maxLines = 2
        )
    }
}


@Composable
fun Toolbar(
    title: String,
    actions: @Composable RowScope.() -> Unit = {},
) {
    val prefs = Preferences.get(LocalContext.current)

    val forceUtilizeAccent by with(prefs) { requiresAccentThoroughly().collectAsState() }
    val colorizeStatusBar by with(prefs) { requiresColoringStatusBar().collectAsState() }

    val statusBg by animateColorAsState(
        targetValue = if (colorizeStatusBar) PlayerTheme.colors.primary else PlayerTheme.colors.surface
    )
    val barBg by animateColorAsState(
        targetValue = if (forceUtilizeAccent) PlayerTheme.colors.primaryVariant else PlayerTheme.colors.surface
    )

    val darkIcons = isLight() && !colorizeStatusBar

    val systemUI = LocalSystemUiController.current

    //TODO: Fix Problem with SideEffect
    LaunchedEffect(key1 = darkIcons) {
        delay(200)
        systemUI.setStatusBarColor(Color.Transparent, darkIcons)
    }

    //Status Bar background
    Spacer(
        modifier = Modifier
            .zIndex(1f)
            .background(statusBg)
            .fillMaxWidth()
            .statusBarsPadding()
    )


    val iNavActions = LocalNavActionProvider.current

    TopAppBar(
        modifier = Modifier
            .zIndex(0.5f)
            .statusBarsPadding(),
        title = {
            Label(text = title)
        },
        backgroundColor = barBg,
        navigationIcon = {
            IconButton(onClick = { iNavActions.navigateUp() }) {
                Icon(
                    imageVector = Icons.Outlined.Reply,
                    contentDescription = "navigate back"
                )
            }
        },
        contentColor = suggestContentColorFor(barBg),
        elevation = Elevation.MEDIUM,
        actions = actions
    )
}
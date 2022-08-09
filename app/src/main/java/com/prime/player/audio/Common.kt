package com.prime.player.audio

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer3
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.prime.player.*
import com.prime.player.R
import com.prime.player.common.MediaUtil
import com.prime.player.common.Utils
import com.prime.player.common.compose.*
import com.prime.player.core.Audio
import com.prime.player.core.Playlist
import com.primex.core.Result
import com.primex.ui.*


@Composable
@NonRestartableComposable
fun Image(
    albumId: Long,
    modifier: Modifier = Modifier,
    fallback: Painter? = painterResource(id = R.drawable.default_art),
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    fadeMills: Int = AnimationConstants.DefaultDurationMillis,
) {
    val context = LocalContext.current
    val request =
        remember(albumId) {
            ImageRequest.Builder(context)
                .data(MediaUtil.composeAlbumArtUri(albumId))
                .crossfade(fadeMills)
                .build()
        }

    AsyncImage(
        model = request,
        contentDescription = null,
        error = fallback,
        placeholder = fallback,
        modifier = modifier,
        contentScale = contentScale,
        alignment = alignment,
    )
}


@Composable
@NonRestartableComposable
fun Image(
    data: Any?,
    modifier: Modifier = Modifier,
    fallback: Painter = painterResource(id = R.drawable.default_art),
    contentScale: ContentScale = ContentScale.Crop,
    alignment: Alignment = Alignment.Center,
    fadeMills: Int = AnimationConstants.DefaultDurationMillis,
) {
    val context = LocalContext.current
    val request =
        remember(data) {
            ImageRequest.Builder(context)
                .data(data)
                .crossfade(fadeMills)
                .build()
        }

    AsyncImage(
        model = request,
        contentDescription = null,
        error = fallback,
        placeholder = fallback,
        modifier = modifier,
        contentScale = contentScale,
        alignment = alignment,
    )
}

@Composable
fun Image(
    bitmap: Bitmap?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    error: Bitmap = App.DEFUALT_ALBUM_ART,
    alignment: Alignment = Alignment.Center,
    durationMillis: Int = AnimationConstants.DefaultDurationMillis,
) {
    val value = bitmap ?: error

    Crossfade(
        targetState = value,
        modifier = modifier,
        animationSpec = tween(durationMillis)
    ) { value ->
        Image(
            bitmap = value.asImageBitmap(),
            contentDescription = null,
            modifier = Modifier.fillMaxSize(),
            contentScale = contentScale,
            alignment = alignment,
        )
    }
}


@Composable
private fun Property(
    title: String,
    subtitle: String,
    icon: ImageVector
) {
    ListTile(
        secondaryText = {
            Label(
                text = subtitle,
                maxLines = 3,
                modifier = Modifier.padding(start = ContentPadding.normal),
                fontWeight = FontWeight.SemiBold
            )
        },
        centreVertically = true,
        leading = { Icon(imageVector = icon, contentDescription = null) },
        text = {
            Caption(
                text = title,
                modifier = Modifier.padding(start = ContentPadding.normal),
                fontWeight = FontWeight.SemiBold
            )
        }
    )
}


/**
 * A [Dialog] to show the properties of the [Audio] file.
 */
@Composable
fun Audio.Properties(
    expanded: Boolean,
    onDismissRequest: () -> Unit
) {
    val audio = this
    if (expanded) {
        PrimeDialog(
            title = "Properties",
            onDismissRequest = onDismissRequest,
            vectorIcon = Icons.Outlined.Info,
            button2 = stringResource(id = R.string.dismiss) to onDismissRequest,
            topBarBackgroundColor = Material.colors.overlay,
            topBarContentColor = Material.colors.onSurface,
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = ContentPadding.normal,
                    vertical = ContentPadding.medium
                )
            ) {
                Property(
                    title = "Title",
                    subtitle = audio.title,
                    icon = Icons.Outlined.Title
                )

                Property(
                    title = "Path",
                    subtitle = audio.path,
                    icon = Icons.Outlined.LocationCity
                )

                Property(
                    title = "Album",
                    subtitle = audio.album,
                    icon = Icons.Outlined.Album
                )

                Property(
                    title = "Artist",
                    subtitle = audio.artist,
                    icon = Icons.Outlined.Person
                )

                Property(
                    title = "Track number",
                    subtitle = "${audio.track}",
                    icon = Icons.Outlined.FormatListNumbered
                )

                Property(
                    title = "Year",
                    subtitle = "${audio.year}",
                    icon = Icons.Outlined.DateRange
                )

                Property(
                    title = "Duration",
                    subtitle = Utils.formatAsDuration(audio.duration),
                    icon = Icons.Default.Timer3
                )

                Property(
                    title = "Date Modified",
                    subtitle = Utils.formatAsRelativeTimeSpan(audio.dateModified).toString(),
                    icon = Icons.Outlined.Update
                )
            }
        }
    }
}


@Composable
private fun Playlist(
    value: Playlist,
    onPlaylistClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier
            // clip the ripple
            .clip(Material.shapes.medium)
            .clickable(onClick = onPlaylistClick)
            // add padding after size.
            .padding(
                vertical = 6.dp,
                horizontal = 10.dp
            )           // add preferred size with min/max width
            .then(Modifier.sizeIn(110.dp, 80.dp))
            // wrap the height of the content
            .wrapContentHeight()
            .then(modifier),
    ) {

        // place here the icon
        Neumorphic(
            shape = RoundedCornerShape(20),
            modifier = Modifier
                .sizeIn(maxWidth = 70.dp)
                .aspectRatio(1.0f),
            elevation = ContentElevation.low,
            lightShadowColor = Material.colors.lightShadowColor,
            darkShadowColor = Material.colors.darkShadowColor,

            content = {
                Icon(
                    imageVector = Icons.Outlined.PlaylistPlay,
                    contentDescription = null,
                    modifier = Modifier
                        .requiredSize(40.dp)
                        .wrapContentSize(Alignment.Center)
                )
            }
        )

        // title
        Label(
            text = value.name,
            maxLines = 2,
            modifier = Modifier.padding(top = ContentPadding.medium),
            style = Material.typography.caption,
        )

        // Subtitle
        Label(
            text = "Modified - ${Utils.formatAsRelativeTimeSpan(value.dateModified)}",
            style = Material.typography.caption2
        )
    }
}



/**
 * A [Dialog] to show the available [Playlist]s and register [onPlaylistClick] event on any or dismiss.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Playlists(
    value: List<Playlist>,
    expanded: Boolean,
    onPlaylistClick: (id: Playlist?) -> Unit
) {
    if (expanded) {
        val onDismissRequest = {
            onPlaylistClick(null)
        }
        PrimeDialog(
            title = "Properties",
            onDismissRequest = onDismissRequest,
            vectorIcon = Icons.Outlined.Info,
            button2 = stringResource(id = R.string.dismiss) to onDismissRequest,
            topBarBackgroundColor = Material.colors.overlay,
            topBarContentColor = Material.colors.onSurface,
        ) {
            Crossfade(
                targetState = value.isEmpty(),
                modifier = Modifier.heightIn(max = 350.dp),
            ) {
                when(it){
                    false -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(80.dp + (4.dp * 2)),
                            contentPadding = PaddingValues(
                                vertical = ContentPadding.medium,
                                horizontal = ContentPadding.normal
                            )
                        ){
                            items(value, key = {it.id}){value ->
                                Playlist(
                                    value = value,
                                    onPlaylistClick = {onPlaylistClick(value) },
                                    modifier = Modifier.animateItemPlacement()
                                )
                            }
                        }
                    }
                    else -> {
                        Placeholder(
                            iconResId = R.raw.lt_empty_box,
                            title = "Oops Empty!!",
                            message = "Please go to Playlists to new ones."
                        )
                    }
                }
            }
        }
    }
}


@Composable
inline fun <T> Placeholder(
    value: Result<T>,
    modifier: Modifier = Modifier,
    crossinline success: @Composable (data: T) -> Unit,
) {
    val (state, data) = value
    Crossfade(
        targetState = state,
        animationSpec = tween(Anim.ActivityLongDurationMills),
        modifier = modifier
    ) {
        when (it) {
            Result.State.Loading ->
                Placeholder(
                    iconResId = R.raw.lt_loading_dots_blue,
                    title = "Loading",
                )
            is Result.State.Processing ->
                Placeholder(
                    iconResId = R.raw.lt_loading_hand,
                    title = "Processing."
                )
            is Result.State.Error ->
                Placeholder(
                    iconResId = R.raw.lt_error,
                    title = "Error"
                )
            Result.State.Empty ->
                Placeholder(
                    iconResId = R.raw.lt_empty_box,
                    title = "Oops Empty!!"
                )
            Result.State.Success -> success(data)
        }
    }
}


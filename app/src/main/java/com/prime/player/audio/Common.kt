package com.prime.player.audio

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timer3
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.prime.player.App
import com.prime.player.Material
import com.prime.player.R
import com.prime.player.common.MediaUtil
import com.prime.player.common.Utils
import com.prime.player.common.compose.ContentPadding
import com.prime.player.core.Audio
import com.prime.player.surfaceVariant
import com.primex.ui.Caption
import com.primex.ui.Label
import com.primex.ui.ListTile
import com.primex.ui.PrimeDialog


@Composable
@NonRestartableComposable
fun AsyncImage(
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
fun AsyncImage(
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
fun AsyncImage(
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
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    ListTile(
        modifier = modifier,
        leading = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(end = ContentPadding.normal)
            )
        },

        text = {
            Caption(
                text = title,
                fontWeight = FontWeight.SemiBold
            )
        },

        secondaryText = {
            Label(
                text = subtitle,
                maxLines = 3,
                fontWeight = FontWeight.SemiBold
            )
        }
    )
}


@Composable
fun Audio.InfoDialog(
    expanded: Boolean,
    onDismissRequest: () -> Unit
) {
    if (expanded)
        PrimeDialog(
            title = "Properties",
            onDismissRequest = onDismissRequest,
            vectorIcon = Icons.Outlined.Info,
            button2 = stringResource(id = R.string.dismiss) to onDismissRequest,
            topBarBackgroundColor = Material.colors.surfaceVariant
        ) {
            Column {
                Property(title = "Title", subtitle = title, icon = Icons.Outlined.Title)
                Property(
                    title = "Path",
                    subtitle = path,
                    icon = Icons.Outlined.LocationCity
                )
                Property(
                    title = "Album",
                    subtitle = album,
                    icon = Icons.Outlined.Album
                )
                Property(
                    title = "Artist",
                    subtitle = artist,
                    icon = Icons.Outlined.Person
                )
                Property(
                    title = "Track number",
                    subtitle = "$track",
                    icon = Icons.Outlined.FormatListNumbered
                )
                Property(title = "Year", subtitle = "$year", icon = Icons.Outlined.DateRange)
                Property(
                    title = "Duration",
                    subtitle = Utils.formatAsDuration(duration),
                    icon = Icons.Default.Timer3
                )
                Property(
                    title = "Date Modified",
                    subtitle = Utils.formatAsRelativeTimeSpan(dateModified),
                    icon = Icons.Outlined.Update
                )
            }
        }
}
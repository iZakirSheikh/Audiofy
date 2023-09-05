@file:OptIn(ExperimentalTextApi::class)

package com.prime.media.dialog

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.os.Build
import android.text.format.DateUtils
import android.text.format.Formatter
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prime.media.Material
import com.prime.media.R
import com.prime.media.core.ContentPadding
import com.prime.media.outline
import com.prime.media.small2
import com.primex.core.drawHorizontalDivider
import com.primex.core.textResource
import com.primex.core.withParagraphStyle
import com.primex.core.withSpanStyle
import com.primex.material2.Dialog
import com.primex.material2.Label
import com.primex.material2.Text
import com.primex.material2.TextButton
import java.io.File

private const val TAG = "Properties"

private val MediaMetadataRetriever.embeddedBitmap: ImageBitmap?
    get() {
        val array = embeddedPicture ?: return null
        return BitmapFactory.decodeByteArray(array, 0, array.size).asImageBitmap()
    }
private val MediaMetadataRetriever.title get() = extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
private val MediaMetadataRetriever.mimeType get() = extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)
private val MediaMetadataRetriever.bitrate get() = extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toLongOrNull()
private val MediaMetadataRetriever.duration get() = extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
private val MediaMetadataRetriever.year get() = extractMetadata(MediaMetadataRetriever.METADATA_KEY_YEAR)?.toIntOrNull()
private val MediaMetadataRetriever.diskNumber get() = extractMetadata(MediaMetadataRetriever.METADATA_KEY_DISC_NUMBER)?.toIntOrNull()
private val MediaMetadataRetriever.trackNumber get() = extractMetadata(MediaMetadataRetriever.METADATA_KEY_CD_TRACK_NUMBER)
private val MediaMetadataRetriever.artist get() = extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
private val MediaMetadataRetriever.album get() = extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
private val MediaMetadataRetriever.genre get() = extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)
private val MediaMetadataRetriever.composer get() = extractMetadata(MediaMetadataRetriever.METADATA_KEY_COMPOSER)
private val MediaMetadataRetriever.author get() = extractMetadata(MediaMetadataRetriever.METADATA_KEY_AUTHOR)
private val MediaMetadataRetriever.writer get() = extractMetadata(MediaMetadataRetriever.METADATA_KEY_WRITER)

private val MediaMetadataRetriever.sampleRate
    get() = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) extractMetadata(
        MediaMetadataRetriever.METADATA_KEY_SAMPLERATE
    )?.toIntOrNull() else null)
private val MediaMetadataRetriever.bitsPerSample
    get() = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) extractMetadata(
        MediaMetadataRetriever.METADATA_KEY_BITS_PER_SAMPLE
    )?.toIntOrNull() else null)


@Composable
@NonRestartableComposable
private fun Toolbar(
    modifier: Modifier = Modifier
) {
    androidx.compose.material.TopAppBar(
        title = { Label(text = stringResource(id = R.string.properties)) },
        navigationIcon = {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                modifier = Modifier.padding(ContentPadding.small)
            )
        },
        backgroundColor = MaterialTheme.colors.background,
        modifier = modifier.drawHorizontalDivider(Material.colors.outline),
        elevation = 0.dp
    )
}

context(LazyGridScope)
private inline fun Property(
    @StringRes title: Int,
    value: CharSequence,
    noinline span: (LazyGridItemSpanScope.() -> GridItemSpan)? = null,
) {
    item(key = title, contentType = "property", span = span) {
        val name = textResource(id = title)
        val color = LocalContentColor.current
        Text(style = MaterialTheme.typography.caption,
            color = color.copy(ContentAlpha.medium),
            modifier = Modifier.padding(ContentPadding.normal, ContentPadding.medium),
            text = buildAnnotatedString {
                withParagraphStyle(lineHeight = 20.sp) {
                    append(name)
                }
                withSpanStyle(fontSize = 16.sp, color = color) {
                    append(value)
                }
            })
    }
}

private val fullLineSpan: (LazyGridItemSpanScope.() -> GridItemSpan) = { GridItemSpan(maxLineSpan) }

@Composable
private fun Layout(
    retriver: MediaMetadataRetriever, modifier: Modifier = Modifier, file: File? = null
) {
    val bitmap = remember(retriver::embeddedBitmap)
    val notAvailable = textResource(id = R.string.not_available_abbv)
    val ctx = LocalContext.current
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        verticalArrangement = Arrangement.SpaceAround,
        modifier = modifier
    ) {
        // Artwork
        if (bitmap != null) item(contentType = "artwork", span = fullLineSpan) {
            Image(
                bitmap = bitmap,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .padding(bottom = ContentPadding.normal)
                    .fillMaxWidth()
                    .aspectRatio(1.77f) // different when width > height
            )
        }
        // Title
        Property(
            title = R.string.title, value = retriver.title ?: notAvailable, span = fullLineSpan
        )

        // Path
        Property(
            title = R.string.file_path, value = file?.path ?: notAvailable, span = fullLineSpan
        )

        // Size
        var value =
            if (file != null) Formatter.formatShortFileSize(ctx, file.length()) else notAvailable
        Property(
            title = R.string.size,
            value = value,
        )

        // Format
        Property(
            title = R.string.format, value = retriver.mimeType ?: notAvailable
        )

        // Bitrate
        val bitrate = retriver.bitrate
        value = if (bitrate != null) Formatter.formatShortFileSize(
            ctx, ((retriver.bitrate ?: -1) / 8)
        ) + "/s" else notAvailable
        Property(
            title = R.string.bitrate, value = value
        )

        // SampleRate
        val sampler = retriver.sampleRate
        value = if (sampler != null) "${sampler}Hz" else notAvailable
        Property(
            title = R.string.sampling_rate, value = value
        )

        // BitsPerSample
        val bitsPerSample = retriver.bitsPerSample
        value = if (bitsPerSample != null) "$bitsPerSample" else notAvailable
        Property(
            title = R.string.bits_per_sample, value = value
        )

        // Duration
        val duration = retriver.duration
        value =
            if (duration != null) DateUtils.formatElapsedTime(duration / 1000L) else notAvailable
        Property(
            title = R.string.duration, value = value
        )

        // Year
        Property(
            title = R.string.year, value = "${retriver.year ?: notAvailable}"
        )


        // Disk Number
        Property(
            title = R.string.disk_number, value = "${retriver.diskNumber ?: notAvailable}"
        )

        // Track Number
        Property(
            title = R.string.track_number, value = "${retriver.trackNumber ?: notAvailable}"
        )

        // Artist
        Property(
            title = R.string.artist, value = retriver.artist ?: notAvailable
        )
        // Album
        Property(
            title = R.string.album, value = retriver.album ?: notAvailable
        )

        // Genre
        Property(
            title = R.string.genre, value = retriver.genre ?: notAvailable
        )

        // Composer
        Property(
            title = R.string.composer, value = retriver.composer ?: notAvailable
        )
        // Author
        Property(
            title = R.string.author, value = retriver.author ?: notAvailable
        )
        // Writer
        Property(
            title = R.string.writer, value = retriver.writer ?: notAvailable
        )
    }
}

@Composable
@NonRestartableComposable
private inline fun Properties(
    expanded: Boolean,
    file: File? = null,
    retriver: MediaMetadataRetriever,
    noinline onDismissRequest: () -> Unit
) {
    Dialog(expanded = expanded, onDismissRequest = onDismissRequest) {
        Scaffold(topBar = { Toolbar() },
            content = {
                Layout(
                    retriver = retriver,
                    file = file,
                    modifier = Modifier
                        .padding(it)
                        .drawHorizontalDivider(Material.colors.outline)
                )
            },
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.50f) // different when width > height
                .clip(Material.shapes.small2),
            backgroundColor = MaterialTheme.colors.surface,
            bottomBar = {
                Row(
                    modifier = Modifier
                        .padding(horizontal = ContentPadding.normal)
                        .height(50.dp)
                        .fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(label = textResource(id = R.string.done), onClick = onDismissRequest)
                }
            })
    }
}

@Composable
@NonRestartableComposable
fun Properties(
    expanded: Boolean, path: String, onDismissListener: () -> Unit
) {
    var retriver: MediaMetadataRetriever? by remember {
        mutableStateOf(null)
    }
    DisposableEffect(key1 = path) {
        val obj = MediaMetadataRetriever()
        obj.setDataSource(path)
        retriver = obj
        onDispose {
            // Release the MediaMetadataRetriever object when disposed
            obj.release()
        }
    }
    val x = retriver ?: return
    val file = remember {
        File(path)
    }
    Properties(expanded = expanded, file = file, retriver = x, onDismissListener)
}
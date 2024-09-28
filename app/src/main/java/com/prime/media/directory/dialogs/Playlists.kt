package com.prime.media.directory.dialogs

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prime.media.R
import com.zs.core_ui.ContentElevation
import com.zs.core_ui.ContentPadding
import com.prime.media.common.util.DateUtils
import com.zs.core.db.Playlist
import com.primex.material2.*
import com.primex.material2.dialog.PrimeDialog
import com.primex.material2.neumorphic.Neumorphic
import com.zs.core_ui.AppTheme

@Composable
@Deprecated("Re-write this")
private fun Playlist(
    value: Playlist,
    onPlaylistClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = Modifier
            // clip the ripple
            .clip(AppTheme.shapes.medium)
            .clickable(onClick = onPlaylistClick)
            // add padding after size.
            .padding(
                vertical = 6.dp, horizontal = 10.dp
            )           // add preferred size with min/max width
            .then(Modifier.sizeIn(110.dp, 80.dp))
            // wrap the height of the content
            .wrapContentHeight()
            .then(modifier),
    ) {

        // place here the icon
        Neumorphic(shape = RoundedCornerShape(20),
            modifier = Modifier
                .sizeIn(maxWidth = 70.dp)
                .aspectRatio(1.0f),
            elevation = ContentElevation.low,
            lightShadowColor = AppTheme.colors.lightShadowColor,
            darkShadowColor = AppTheme.colors.darkShadowColor,

            content = {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.PlaylistPlay,
                    contentDescription = null,
                    modifier = Modifier
                        .requiredSize(40.dp)
                        .wrapContentSize(Alignment.Center)
                )
            })

        // title
        Label(
            text = value.name,
            maxLines = 2,
            modifier = Modifier.padding(top = ContentPadding.medium),
            style = AppTheme.typography.caption,
        )

        // Subtitle
        Label(
            text = "Modified - ${DateUtils.formatAsRelativeTimeSpan(value.dateModified)}",
            style = AppTheme.typography.caption
        )
    }
}

/**
 * A [Dialog] to show the available [Playlist]s and register [onPlaylistClick] event on any or dismiss.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
@Deprecated("Re-write this.")
fun Playlists(
    value: List<Playlist>, expanded: Boolean, onPlaylistClick: (id: Playlist?) -> Unit
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
            topBarBackgroundColor = AppTheme.colors.background(0.5.dp),
            topBarContentColor = AppTheme.colors.onBackground,
        ) {
            Crossfade(
                targetState = value.isEmpty(),
                modifier = Modifier.height(350.dp),
            ) {
                when (it) {
                    false -> {
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(80.dp + (4.dp * 2)),
                            contentPadding = PaddingValues(
                                vertical = ContentPadding.medium, horizontal = ContentPadding.normal
                            )
                        ) {
                            items(value, key = { it.id }) { value ->
                                Playlist(
                                    value = value,
                                    onPlaylistClick = { onPlaylistClick(value) },
                                //    modifier = Modifier.animateItemPlacement()
                                )
                            }
                        }
                    }
                    else -> {
                        com.prime.media.common.Placeholder(
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



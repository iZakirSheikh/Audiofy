package com.prime.media.dialogs

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
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prime.media.*
import com.prime.media.R
import com.prime.media.core.ContentElevation
import com.prime.media.core.ContentPadding
import com.prime.media.core.util.DateUtil
import com.prime.media.core.compose.*
import com.prime.media.core.db.Playlist
import com.primex.material2.*
import com.primex.material2.dialog.PrimeDialog
import com.primex.material2.neumorphic.Neumorphic

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
            .clip(Theme.shapes.medium)
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
            lightShadowColor = Theme.colors.lightShadowColor,
            darkShadowColor = Theme.colors.darkShadowColor,

            content = {
                Icon(
                    imageVector = Icons.Outlined.PlaylistPlay,
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
            style = Theme.typography.caption,
        )

        // Subtitle
        Label(
            text = "Modified - ${DateUtil.formatAsRelativeTimeSpan(value.dateModified)}",
            style = Theme.typography.caption2
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
            topBarBackgroundColor = Theme.colors.overlay,
            topBarContentColor = Theme.colors.onSurface,
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



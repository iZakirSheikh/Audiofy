package com.prime.media.directory.store

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.prime.media.R
import com.prime.media.Theme
import com.prime.media.caption2
import com.prime.media.core.ContentElevation
import com.prime.media.core.ContentPadding
import com.prime.media.core.compose.*
import com.prime.media.core.compose.directory.Directory

import com.prime.media.core.db.Album
import com.prime.media.impl.store.AlbumsViewModel
import com.prime.media.impl.uri
import com.prime.media.small2
import com.primex.material2.Label

private val TILE_WIDTH = 80.dp
private val GridItemPadding =
    PaddingValues(vertical = 6.dp, horizontal = 10.dp)

@Composable
fun Album(
    value: Album,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            // clip the ripple
            .clip(Theme.shapes.medium)
            .then(modifier)
            // add padding after size.
            .padding(GridItemPadding)
            // add preferred size with min/max width
            .then(Modifier.width(TILE_WIDTH))
            // wrap the height of the content
            .wrapContentHeight(),
    ) {

        Surface(
            shape = Theme.shapes.small2,
            elevation = ContentElevation.medium,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(0.65f),
            content = {
                Image(
                    data = value.uri,
                    fallback = painterResource(id = R.drawable.default_art)
                )
            },
        )

        // title
        Label(
            text = value.title,
            maxLines = 2,
            modifier = Modifier.padding(top = ContentPadding.medium),
            style = Theme.typography.caption,
        )

        // Subtitle
        Label(
            text = "Year: ${value.firstYear}",
            style = Theme.typography.caption2
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Albums(viewModel: Albums) {
    val navigator = LocalNavController.current
    Directory(
        viewModel = viewModel,
        cells = GridCells.Adaptive(TILE_WIDTH + (4.dp * 2)),
        onAction = { /*TODO: Currently we don't support more actions.*/},
        key = { it.id },
        contentPadding = PaddingValues(horizontal = ContentPadding.normal),
    ) {
        Album(
            value = it,
            modifier = Modifier
                .clickable {
                    val direction = Audios.direction(Audios.GET_FROM_ALBUM, it.title)
                    navigator.navigate(direction)
                }
                .animateItemPlacement()
        )
    }
}
package com.prime.media.directory.store

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prime.media.*
import com.prime.media.core.ContentPadding
import com.prime.media.core.compose.LocalNavController
import com.prime.media.core.compose.directory.Directory

import com.prime.media.core.db.Genre
import com.primex.material2.Label


private val TILE_WIDTH = 80.dp
private val GridItemPadding =
    PaddingValues(vertical = 6.dp, horizontal = 10.dp)

@Composable
fun Genre(
    value: Genre,
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
            color = Color.Transparent,
            border = BorderStroke(3.dp, Theme.colors.onBackground),
            shape = CircleShape,

            modifier = Modifier
                .sizeIn(maxWidth = 70.dp)
                .aspectRatio(1.0f),

            content = {
                Label(
                    text = "${value.name[0].uppercaseChar()}",
                    fontWeight = FontWeight.Bold,
                    style = Theme.typography.h4,
                    modifier = Modifier.wrapContentSize(Alignment.Center)
                )
            }
        )

        // title
        Label(
            text = value.name,
            maxLines = 2,
            modifier = Modifier.padding(top = ContentPadding.medium),
            style = Theme.typography.caption,
        )

        // Subtitle
        Label(
            text = "${value.cardinality} Tracks",
            style = Theme.typography.caption2
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Genres(state: Genres) {
    val navigator = LocalNavController.current
    Directory(
        viewModel = state,
        cells = GridCells.Adaptive(TILE_WIDTH + (4.dp * 2)),
        onAction = {},
        key = { it.id },
        contentPadding = PaddingValues(horizontal = ContentPadding.normal),
    ) {
        Genre(
            value = it,
            modifier = Modifier
                .clickable {
                    val direction = Audios.direction(Audios.GET_FROM_GENRE, it.name)
                    navigator.navigate(direction)
                }
                .animateItemPlacement()
        )
    }
}
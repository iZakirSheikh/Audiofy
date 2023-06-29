package com.prime.media.directory.store

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.prime.media.*
import com.prime.media.core.ContentPadding
import com.prime.media.core.compose.LocalNavController
import com.prime.media.core.compose.directory.Directory

import com.prime.media.core.db.*
import com.primex.material2.Label


private val TILE_WIDTH = 80.dp
private val GridItemPadding =
    PaddingValues(vertical = 6.dp, horizontal = 10.dp)
private val folderIcon = Icons.Default.Folder

@Composable
fun Folder(
    value: Folder,
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

        Icon(
            imageVector = folderIcon,
            contentDescription = null,
            modifier = Modifier
                .sizeIn(maxWidth = 70.dp)
                .fillMaxWidth()
                .aspectRatio(1.0f)
        )


        // title
        Label(
            text = value.name,
            maxLines = 2,
            modifier = Modifier.padding(top = ContentPadding.medium),
            style = Theme.typography.caption,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Folders(state: Folders) {
    val navigator = LocalNavController.current
    Directory(
        viewModel = state,
        cells = GridCells.Adaptive(TILE_WIDTH + (4.dp * 2)),
        onAction = {},
        key = { it.path },
        contentPadding = PaddingValues(horizontal = ContentPadding.normal),
    ) {
        Folder(
            value = it,
            modifier = Modifier
                .clickable {
                    val direction = Audios.direction(Audios.GET_FROM_FOLDER, it.path)
                    navigator.navigate(direction)
                }
                .animateItemPlacement()
        )
    }
}
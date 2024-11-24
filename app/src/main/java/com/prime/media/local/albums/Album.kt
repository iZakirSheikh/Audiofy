package com.prime.media.local.albums

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.prime.media.R
import com.primex.material2.Label
import com.zs.core.store.Album
import com.zs.core_ui.AppTheme
import coil.compose.rememberAsyncImagePainter as Painter
import com.primex.core.textResource as stringResource
import com.zs.core_ui.ContentPadding as CP

@Composable
fun Album(
    value: Album,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .clip(AppTheme.shapes.medium)
            .then(modifier)
            .padding(vertical = 6.dp, horizontal = 10.dp),
        content = {
            // Artwork
            Image(
                painter = Painter(value.artworkUri),
                modifier = Modifier
                    .shadow(8.dp, AppTheme.shapes.compact)
                    .background(AppTheme.colors.background)
                    .fillMaxWidth()
                    .aspectRatio(0.65f),
                contentDescription = value.title,
                contentScale = ContentScale.Crop,
            )

            // Label
            Label(
                text = value.title,
                maxLines = 2,
                modifier = Modifier.padding(top = CP.medium),
                style = AppTheme.typography.caption,
            )

            // Caption
            Label(
                text = stringResource(R.string.albums_scr_year_d, value.firstYear),
                style = AppTheme.typography.caption2,
                color = LocalContentColor.current.copy(ContentAlpha.medium)
            )
        }
    )
}
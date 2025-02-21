/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 04-02-2025.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.prime.media.local

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.prime.media.R
import com.prime.media.common.Route
import com.prime.media.old.common.LocalNavController
import com.prime.media.old.directory.store.Audios
import com.primex.material2.Label
import com.zs.core.store.Album
import com.zs.core_ui.AppTheme
import coil.compose.rememberAsyncImagePainter as Painter
import com.primex.core.textResource as stringResource
import com.zs.core_ui.ContentPadding as CP

object RouteAlbums : Route

@Composable
private fun Album(
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

@Composable
@NonRestartableComposable
fun Albums(viewState: DirectoryViewState<Album>) {
    val navController = LocalNavController.current
    Directory(
        viewState,
        key = Album::id,
        itemContent = {
            Album(
                it,
                modifier = Modifier
                    .animateItem()
                    .clickable {
                        val direction = Audios.direction(Audios.GET_FROM_ALBUM, it.title)
                        navController.navigate(direction)
                    },
            )
        }
    )
}
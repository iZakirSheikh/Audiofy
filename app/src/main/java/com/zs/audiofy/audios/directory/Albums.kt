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

package com.zs.audiofy.audios.directory

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.zs.audiofy.R
import com.zs.audiofy.audios.RouteAudios
import com.zs.audiofy.common.Route
import com.zs.audiofy.common.compose.LocalNavController
import com.zs.audiofy.common.compose.directory.Directory
import com.zs.audiofy.common.compose.directory.DirectoryViewState
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.ContentAlpha
import com.zs.compose.theme.LocalContentColor
import com.zs.compose.theme.text.Label
import com.zs.core.store.models.Audio.Album
import com.zs.audiofy.common.compose.ContentPadding as CP
import com.zs.compose.foundation.textResource as stringResource

object RouteAlbums : Route

@Composable
private fun Album(
    value: Album,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .clip(AppTheme.shapes.small)
            .then(modifier)
            .padding(vertical = 6.dp, horizontal = 10.dp),
        content = {
            // Artwork
            AsyncImage(
                model = value.artworkUri,
                modifier = Modifier
                    .shadow(8.dp, AppTheme.shapes.medium)
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
                style = AppTheme.typography.body3,
            )

            // Caption
            Label(
                text = stringResource(R.string.scr_albums_year_d, value.firstYear),
                style = AppTheme.typography.body2,
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
                        navController.navigate(RouteAudios(RouteAudios.SOURCE_ALBUM, "${it.id}"))
                    },
            )
        }
    )
}
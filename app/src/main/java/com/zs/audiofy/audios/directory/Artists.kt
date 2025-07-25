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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zs.audiofy.R
import com.zs.audiofy.audios.RouteAudios
import com.zs.audiofy.common.Route
import com.zs.audiofy.common.compose.LocalNavController
import com.zs.audiofy.common.compose.directory.Directory
import com.zs.audiofy.common.compose.directory.DirectoryViewState
import com.zs.compose.foundation.shadow
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.Icon
import com.zs.compose.theme.Surface
import com.zs.compose.theme.text.Label
import com.zs.core.store.models.Audio.Artist
import com.zs.audiofy.common.compose.ContentPadding as CP

object RouteArtists : Route


private val IconModifier = Modifier
    .wrapContentSize(Alignment.Center)
    .scale(1.45f)
private val shape = RoundedCornerShape(50, 50, 50, 15)

@Composable
private fun Artist(
    value: Artist,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .clip(AppTheme.shapes.small)
            .then(modifier)
            .padding(vertical = 6.dp, horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(CP.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = {
            //
            Surface(
                modifier = Modifier
                    .aspectRatio(1.0f)
                    .shadow(
                        elevation = 4.dp,
                        lightShadowColor = AppTheme.colors.lightShadowColor,
                        darkShadowColor = AppTheme.colors.darkShadowColor,
                        shape = shape,
                    )
                    .background(AppTheme.colors.background),
                color = AppTheme.colors.background(1.dp),
                contentColor = AppTheme.colors.onBackground,
                content = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_artist),
                        contentDescription = null,
                        modifier = IconModifier,
                    )
                }
            )

            // title
            Label(
                text = value.name,
                maxLines = 2,
                style = AppTheme.typography.label3,
                textAlign = TextAlign.Center
            )
        }
    )
}

@Composable
@NonRestartableComposable
fun Artists(viewState: DirectoryViewState<Artist>) {
    val navController = LocalNavController.current
    Directory(
        viewState,
        minSize = 80.dp,
        key = Artist::id,
        itemContent = {
            Artist(
                it,
                modifier = Modifier
                    .animateItem()
                    .clickable {
                        navController.navigate(RouteAudios(RouteAudios.SOURCE_ARTIST, "${it.id}"))
                    },
            )
        }
    )
}
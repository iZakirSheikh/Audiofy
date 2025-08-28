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

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zs.audiofy.audios.RouteAudios
import com.zs.audiofy.common.Route
import com.zs.audiofy.common.compose.LocalNavController
import com.zs.audiofy.common.compose.directory.Directory
import com.zs.audiofy.common.compose.directory.DirectoryViewState
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.Surface
import com.zs.compose.theme.text.Label
import com.zs.core.store.models.Audio.Genre
import com.zs.audiofy.common.compose.ContentPadding as CP

@Composable
private fun Genre(
    value: Genre,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .clip(AppTheme.shapes.small)
            .then(modifier)
            .padding(vertical = 6.dp, horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(CP.small),
        content = {
            //
            Surface (
                color = Color.Transparent,
                contentColor = AppTheme.colors.onBackground,
                border = BorderStroke(3.dp, AppTheme.colors.onBackground),
                shape = CircleShape,
                modifier = Modifier.aspectRatio(1.0f),
                content = {
                    Label(
                        text = "${value.name[0].uppercaseChar()}",
                        fontWeight = FontWeight.Bold,
                        style = AppTheme.typography.display3,
                        modifier = Modifier.wrapContentSize(Alignment.Center)
                    )
                }
            )

            // title
            Label(
                text = value.name,
                maxLines = 2,
                style = AppTheme.typography.body3,
            )
        }
    )
}

object RouteGenres : Route

@Composable
@NonRestartableComposable
fun Genres(viewState: DirectoryViewState<Genre>) {
    val navController = LocalNavController.current
    Directory(
        viewState,
        key = Genre::id,
        itemContent = {
            Genre(
                it,
                modifier = Modifier
                    .animateItem()
                    .clickable {
                        navController.navigate(RouteAudios(RouteAudios.SOURCE_GENRE, "${it.id}"))
                    },
            )
        }
    )
}
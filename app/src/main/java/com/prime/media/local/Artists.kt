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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prime.media.R
import com.prime.media.common.Route
import com.prime.media.old.common.LocalNavController
import com.prime.media.old.directory.store.Audios
import com.primex.material2.Label
import com.primex.material2.neumorphic.Neumorphic
import com.zs.core.store.Artist
import com.zs.core_ui.AppTheme
import com.zs.core_ui.ContentElevation
import com.zs.core_ui.ContentPadding as CP

object RouteArtists: Route

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun Artist(
    value: Artist,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .clip(AppTheme.shapes.compact)
            .then(modifier)
            .padding(vertical = 6.dp, horizontal = 10.dp),
        verticalArrangement = Arrangement.spacedBy(CP.medium),
        content = {
            //
            Neumorphic(
                shape = CircleShape,
                modifier = Modifier.aspectRatio(1.0f),
                elevation = ContentElevation.low,
                lightShadowColor = AppTheme.colors.lightShadowColor,
                darkShadowColor = AppTheme.colors.darkShadowColor,
                content = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_artist),
                        contentDescription = null,
                        modifier = Modifier
                            .requiredSize(40.dp)
                            .wrapContentSize(Alignment.Center)
                    )
                }
            )

            // title
            Label(
                text = value.name,
                maxLines = 2,
                style = AppTheme.typography.caption,
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
        key = Artist::id,
        itemContent = {
            Artist(
                it,
                modifier = Modifier
                    .animateItem()
                    .clickable {
                        val direction = Audios.direction(Audios.GET_FROM_ARTIST, it.name)
                        navController.navigate(direction)
                    },
            )
        }
    )
}
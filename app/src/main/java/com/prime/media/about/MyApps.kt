/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 22-06-2024.
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

package com.prime.media.about

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prime.media.BuildConfig
import com.prime.media.common.App
import com.prime.media.common.LocalSystemFacade
import com.prime.media.old.common.Artwork
import com.primex.material2.Label
import com.zs.core_ui.AppTheme
import com.zs.core_ui.ContentPadding
import com.prime.media.common.Registry as Re





/**
 * Represents my apps that needs to be showcased in AboutUs for gaining more downloads.
 */
@Composable
fun MyApps(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(ContentPadding.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val facade = LocalSystemFacade.current
        Re.featuredApps.forEach { app ->
            App(
                value = app,
                onClick = { facade.launchAppStore(app.third) }
            )
        }
    }
}
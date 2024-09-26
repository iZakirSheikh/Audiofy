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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.zs.core_ui.ContentPadding
import com.prime.media.common.Artwork
import com.prime.media.common.LocalSystemFacade
import com.primex.material2.Label
import com.zs.core_ui.AppTheme

/**
 * Represents an app with its title, image URL, and Play Store URL.
 *
 * @property first The title of theapp.
 * @property second The URL of the app's image.
 * @property third The URL of the app's Play Store page.
 */
private typealias App = Triple<String, String, String>

private val MyAppList = listOf(
    App(
        "Unit Converter",
        "https://play-lh.googleusercontent.com/TtUj94noX7g5B6Vs84A2PpVSCreYWVye5mHz32mSMHXCojT0xxDRtXBwXbc1q42AaA=s256-rw",
        "com.prime.toolz2"
    ),
    App(
        "Scientific Calculator",
        "https://play-lh.googleusercontent.com/ZK1RCWbqO5faf4Z1diQM6HtoaGbmM5dYudYY5yXXP1yZawHrElerat7ix0slYzAxHZRq=s256-rw",
        "com.prime.calculator.paid"
    ),
    App(
        "Gallery - Photos & Videos",
        "https://play-lh.googleusercontent.com/HlADK_i_qZoBn_4GNdjgCDt3Ah-h1ZbL_jUy1j_kDUo9Hvoq3AiUPI_ZxZXY95ftl7hu=w240-h480-rw",
        "com.googol.android.apps.photos"
    )
)

@Composable
private fun App(
    value: App,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(AppTheme.shapes.compact)
            .clickable(onClick = onClick)
            .padding(ContentPadding.small),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Artwork(
            data = value.second,
            modifier = Modifier
                .shadow(4.dp, RoundedCornerShape(24), true)
                .size(64.dp)
        )

        Label(
            text = value.first,
            maxLines = 2,
            textAlign = TextAlign.Center,
            style = AppTheme.typography.caption,
            modifier = Modifier.width(56.dp)
        )
    }
}


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
        MyAppList.forEach { app ->
            App(
                value = app,
                onClick = { facade.launchAppStore(app.third) }
            )
        }
    }
}
/*
 *  Copyright (c) 2025 Zakir Sheikh
 *
 *  Created by Zakir Sheikh on $today.date.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

@file:OptIn(ExperimentalMaterialApi::class)

package com.prime.media.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prime.media.BuildConfig
import com.prime.media.R
import com.prime.media.common.LocalSystemFacade
import com.primex.core.textResource
import com.primex.material2.Label
import com.primex.material2.ListTile
import com.primex.material2.Preference
import com.primex.material2.TextButton
import com.zs.core_ui.AppTheme
import com.zs.core_ui.ContentPadding as CP

context(_: RouteSettings, scope: ColumnScope)
@Composable
fun AboutUs() {
    with(scope){
        // The app version and check for updates.
        val facade = LocalSystemFacade.current
        ListTile(
            headline = {Label(textResource(R.string.version), fontWeight = FontWeight.Bold)},
            subtitle = {
                Label(
                    textResource(R.string.version_info_s, BuildConfig.VERSION_NAME)
                )
            },
            leading = {
                Icon(
                    imageVector = Icons.Outlined.NewReleases,
                    contentDescription = null
                )
            },
            footer = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(CP.small),
                    content = {
                        TextButton(
                            textResource(R.string.update_audiofy),
                            onClick = { facade.initiateUpdateFlow(true) })
                        TextButton(
                            textResource(R.string.join_the_beta),
                            onClick = { facade.launch(Settings.JoinBetaIntent) },
                            enabled = false
                        )
                    }
                )
            },
        )

        // Privacy Policy
        Preference(
            text = textResource(R.string.pref_privacy_policy),
            icon = Icons.Outlined.PrivacyTip,
            modifier = Modifier
                .clip(AppTheme.shapes.medium)
                .clickable { facade.launch(Settings.PrivacyPolicyIntent) },
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(CP.small)
        ) {
            val colors = ChipDefaults.chipColors(
                backgroundColor = AppTheme.colors.background(1.dp),
                contentColor = AppTheme.colors.accent
            )
            Chip (
                content = { Label(textResource(R.string.rate_us)) },
                leadingIcon = { Icon(Icons.Outlined.Star, null) },
                onClick = facade::launchAppStore,
                colors = colors,
                shape = AppTheme.shapes.small
            )

            Chip(
                content = { Label(textResource(R.string.share_app_label)) },
                leadingIcon = { Icon(Icons.Outlined.Share, null) },
                onClick = { facade.launch(Settings.ShareAppIntent) },
                colors = colors,
                shape = AppTheme.shapes.small
            )
        }
    }
}
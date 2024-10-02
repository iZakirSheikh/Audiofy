/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 26-08-2024.
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

@file:Suppress("NOTHING_TO_INLINE")

package com.prime.media.old.config

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Scaffold
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReplyAll
import androidx.compose.material.icons.twotone.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.prime.media.R
import com.zs.core_ui.ContentPadding
import com.prime.media.old.common.LocalNavController
import com.prime.media.old.common.LocalSystemFacade
import com.prime.media.old.common.preference
import com.prime.media.old.settings.Settings
import com.primex.core.plus
import com.primex.core.textResource
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.zs.core_ui.AppTheme
import com.zs.core_ui.toast.Toast

private const val TAG = "Personalize"

@Composable
@NonRestartableComposable
private fun Toolbar(modifier: Modifier = Modifier) {
    TopAppBar(
        title = {
            Label(
                text = textResource(R.string.scr_personalize_title_desc),
                maxLines = 2
            )
        },
        windowInsets = WindowInsets.statusBars,
        navigationIcon = {
            val navController = LocalNavController.current
            IconButton(
                imageVector = Icons.AutoMirrored.Outlined.ReplyAll,
                onClick = navController::navigateUp
            )
        },
        actions = {
            val facade = LocalSystemFacade.current
            IconButton(
                Icons.TwoTone.Info,
                onClick = {
                    facade.show(
                        R.string.msg_scr_personalize_customize_everywhere,
                        duration = Toast.DURATION_INDEFINITE
                    )
                }
            )
        },
        backgroundColor = AppTheme.colors.background(1.dp),
        contentColor = AppTheme.colors.onBackground,
        elevation = 0.dp,
        modifier = modifier
    )
}

@Composable
fun Personalize(viewState: PersonalizeViewState) {
    Scaffold(
        topBar = { Toolbar() },
        content = {
            val facade = LocalSystemFacade.current
            val details by facade.inAppProductDetails.collectAsState()

            // The selected Widget
            val selected by preference(Settings.GLANCE)
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ContentPadding.medium),
                contentPadding = PaddingValues(
                    horizontal = ContentPadding.large,
                    vertical = ContentPadding.normal
                ) + it,
                modifier = Modifier.fillMaxSize()
            ) {
                // emit widgets
                widgets(selected, details, onRequestApply = viewState::setInAppWidget)
            }
        },
    )
}


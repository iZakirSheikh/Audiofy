/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 16-11-2024.
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

@file:OptIn(ExperimentalMaterialApi::class)

package com.prime.media.local.videos

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReplyAll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.prime.media.R
import com.prime.media.common.Filters
import com.prime.media.common.ListHeader
import com.prime.media.common.Mapped
import com.prime.media.common.emit
import com.prime.media.common.menu.Action
import com.prime.media.old.common.LocalNavController
import com.prime.media.old.console.Console
import com.primex.core.drawHorizontalDivider
import com.primex.core.fadingEdge
import com.primex.core.findActivity
import com.primex.core.plus
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.neumorphic.NeumorphicTopAppBar
import com.zs.core.store.Video
import com.zs.core_ui.AppTheme
import com.zs.core_ui.adaptive.TwoPane
import com.zs.core_ui.adaptive.contentInsets
import androidx.compose.foundation.layout.PaddingValues as Padding
import com.primex.core.textResource as stringResource
import com.zs.core_ui.ContentElevation as CE
import com.zs.core_ui.ContentPadding as CP

private val C_PADDING = Padding(CP.normal)

@Composable
private fun TopBar(
    modifier: Modifier = Modifier
) {
    // the actual content
    NeumorphicTopAppBar(
        shape = CircleShape,
        elevation = CE.low,
        modifier = Modifier
            .padding(top = CP.medium)
            .then(modifier),
        lightShadowColor = AppTheme.colors.lightShadowColor,
        darkShadowColor = AppTheme.colors.darkShadowColor,
        // The label must not fill width
        // this will surely make the look and feel of the app bad.
        title = {
            Label(
                text = stringResource(R.string.videos),
                modifier = Modifier.padding(end = CP.normal),
                maxLines = 2
            )
        },
        // The Toolbar will surely require navigate to back.
        navigationIcon = {
            val navigator = LocalNavController.current
            IconButton(
                // remove focus else navigateUp
                onClick = navigator::navigateUp,
                imageVector = Icons.AutoMirrored.Outlined.ReplyAll,
                contentDescription = null
            )
        }
    )
}

/**
 * The actaul content of the list
 */
private fun LazyListScope.content(
    data: Mapped<Video>,
    actions: List<Action>,
    onAction: (action: Action?, video: Video) -> Unit,
) {
    for ((header, values) in data) {
        if (header.isNotBlank()) // only show this if non-blank.
            stickyHeader(
                header,
                contentType = "header",
                content = {
                    ListHeader(
                        header,
                        modifier = Modifier.animateItem()
                    )
                }
            )
        // rest of the items
        items(
            values,
            key = Video::id,
            contentType = { "album" },
            itemContent = {
                Video(
                    value = it,
                    modifier = Modifier.animateItem(),
                    actions = actions,
                    onAction = onAction
                )
            }
        )
    }
}

@Composable
fun Videos(viewState: VideosViewState) {
    val inAppNavBarInsets = WindowInsets.contentInsets
    TwoPane(
        topBar = {
            TopBar(
                modifier = Modifier
                    .statusBarsPadding()
                    .drawHorizontalDivider(color = AppTheme.colors.onBackground)
                    .padding(bottom = CP.medium),
            )
        },
        primary = {
            // Collect the data from the viewState, initially null representing loading state.
            // Get the grid item size multiplier from user preferences.
            val data by viewState.data.collectAsState()
            val context = LocalContext.current
            val navController = LocalNavController.current
            val state = rememberLazyListState()
            val onAction = { action: Action?, video: Video ->
                when (action) {
                    VideosViewState.ACTION_DELETE -> viewState.delete(context.findActivity(), video)
                    VideosViewState.ACTION_SHARE -> viewState.share(context.findActivity(), video)
                    null -> {
                        viewState.play(video)
                        navController.navigate(Console.route)
                    }

                    else -> TODO("$action not implemented!!")
                }
            }
            LazyColumn(
                state = state,
                // Apply padding for content insets and in-app navigation bar.
                contentPadding = Padding(CP.normal) + (WindowInsets.navigationBars.union(inAppNavBarInsets)).asPaddingValues(),
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.contentInsets)
                    .fadingEdge(state, false, 16.dp),
                content = {
                    val values = emit(data) ?: return@LazyColumn
                    // Filters: Display the filters section.
                    item(
                        "",
                        contentType = "filters",
                        content = {
                            Filters(
                                viewState.order,
                                viewState.orders,
                                onRequest = {
                                    when {
                                        it == null -> viewState.filter(!viewState.order.first)
                                        else -> viewState.filter(order = it)
                                    }
                                }
                            )
                        }
                    )

                    // Rest of the items
                    content(values, viewState.actions, onAction)
                }
            )
        }
    )
}



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

package com.prime.media.common.compose.directory

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReplyAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.prime.media.R
import com.prime.media.common.compose.Filters
import com.prime.media.common.compose.FloatingLargeTopAppBar
import com.prime.media.common.compose.LocalNavController
import com.prime.media.common.compose.background
import com.prime.media.common.compose.emit
import com.prime.media.common.compose.fadingEdge2
import com.prime.media.common.compose.preference
import com.prime.media.common.compose.rememberAcrylicSurface
import com.prime.media.common.compose.source
import com.prime.media.settings.Settings
import com.zs.compose.foundation.background
import com.zs.compose.foundation.fullLineSpan
import com.zs.compose.foundation.plus
import com.zs.compose.foundation.stickyHeader
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.Icon
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.LocalWindowSize
import com.zs.compose.theme.adaptive.FabPosition
import com.zs.compose.theme.adaptive.Scaffold
import com.zs.compose.theme.adaptive.contentInsets
import com.zs.compose.theme.appbar.AppBarDefaults
import com.zs.compose.theme.text.Label
import com.zs.compose.theme.text.LocalTextStyle
import com.zs.compose.theme.text.Text
import com.zs.compose.theme.text.TextField
import com.zs.compose.theme.text.TextFieldDefaults
import com.zs.compose.theme.text.TonalHeader
import androidx.compose.foundation.layout.PaddingValues as Padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState as GridState
import com.prime.media.common.compose.ContentPadding as CP
import com.zs.compose.foundation.textResource as stringResource

private val DEFAULT_MIN_SIZE = 80.dp

private val ContentPadding = Padding(start = CP.normal, end = CP.normal, bottom = CP.normal)

/**
 * A generic composable for displaying categorized lists (directories) like playlists, folders, etc.
 *
 * Provides a standard UI with top bar, search, filters, and a lazy grid.
 *
 * @param T The type of data items displayed.
 * @param viewState The state of the directory view, including data, title, query, and filters.
 * @param itemSize The minimum size of items in the grid. Defaults to [DEFAULT_MIN_SIZE].
 * @param key A stable key for each item to optimize updates.
 * @param itemContent The composable function for rendering each item.
 */
@Composable
fun <T> Directory(
    viewState: DirectoryViewState<T>,
    minSize: Dp = DEFAULT_MIN_SIZE,
    key: ((item: T) -> Any)? = null,
    itemContent: @Composable LazyGridItemScope.(item: T) -> Unit
) {
    val state = GridState()
    var isSearchVisible by remember { mutableStateOf(false) }
    val (width, height) = LocalWindowSize.current
    // Properties
    val surface = rememberAcrylicSurface()
    val topAppBarScrollBehavior = AppBarDefaults.exitUntilCollapsedScrollBehavior()
    val colors = AppTheme.colors

    BackHandler(isSearchVisible) {
        isSearchVisible = !isSearchVisible
    }

    Scaffold(
        fabPosition = if (width > height) FabPosition.End else FabPosition.Center,
        topBar = {
            FloatingLargeTopAppBar(
                title = { Label(viewState.title, maxLines = 2) },
                scrollBehavior = topAppBarScrollBehavior,
                background = colors.background(surface),
                insets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
                navigationIcon = {
                    val navController = LocalNavController.current
                    IconButton(
                        icon = Icons.AutoMirrored.Filled.ReplyAll,
                        contentDescription = null,
                        onClick = navController::navigateUp
                    )
                },
                actions = {
                    IconButton(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        onClick = { isSearchVisible = !isSearchVisible }
                    )
                }
            )
        },
        floatingActionButton = {
            // animate visibility based on show is
            AnimatedVisibility(
                isSearchVisible,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically(),
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(bottom = CP.medium)
                    .windowInsetsPadding(WindowInsets.ime),
                content = {
                    val requester = remember { FocusRequester() }
                    TextField(
                        state = viewState.query,
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null
                            )
                        },
                        placeholder = { Label(text = stringResource(R.string.search_placeholder)) },
                        label = { Label(text = stringResource(R.string.search)) },
                        colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.Transparent),
                        modifier = Modifier
                            .widthIn(min = 320.dp)
                            .focusRequester(requester)
                            .clip(AppTheme.shapes.medium)
                            .background(AppTheme.colors.background(surface)),
                        trailingIcon = {
                            IconButton(
                                Icons.Default.Close,
                                onClick = {
                                    if (viewState.query.text.isNotEmpty())
                                        viewState.query.clearText()
                                    else
                                        isSearchVisible = false
                                },
                                contentDescription = null
                            )
                        },
                    )

                    // Request focus when the search view becomes visible
                    DisposableEffect(Unit) {
                        requester.requestFocus()
                        onDispose(requester::restoreFocusedChild)
                    }
                }
            )
        },
        content = {
            // Collect the data from the viewState, initially null representing loading state.
            // Get the grid item size multiplier from user preferences.
            val data by viewState.data.collectAsState()
            val multiplier by preference(Settings.GRID_ITEM_SIZE_MULTIPLIER)
            LazyVerticalGrid(
                state = state,
                columns = GridCells.Adaptive(minSize * multiplier),
                verticalArrangement = CP.SmallArrangement,
                horizontalArrangement = CP.SmallArrangement,
                contentPadding = ContentPadding + WindowInsets.contentInsets + WindowInsets.systemBars.only(
                    WindowInsetsSides.Bottom
                ).asPaddingValues(),
                modifier = Modifier
                    .source(surface)
                    .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                    .fadingEdge2(50.dp),
                content = {
                    // Emit state: Get the album data, if null, return from LazyVerticalGrid content.
                    val values = emit(data) ?: return@LazyVerticalGrid
                    // Filters: Display the filters section.
                    item(
                        "",
                        contentType = "filters",
                        span = fullLineSpan,
                        content = {
                            Filters(
                                viewState.filter,
                                viewState.orders,
                                onRequest = {
                                    when {
                                        it == null -> viewState.filter(!viewState.filter.first)
                                        else -> viewState.filter(order = it)
                                    }
                                }
                            )
                        }
                    )
                    //
                    for ((header, values) in values) {
                        if (header.isNotBlank()) // only show this if non-blank.
                            stickyHeader(
                                state,
                                header,
                                contentType = "header",
                                content = {
                                    Box(
                                        modifier = Modifier
                                            .animateItem()
                                            .padding(horizontal = 6.dp),
                                        content = { TonalHeader(header) }
                                    )
                                }
                            )

                        items(
                            values,
                            key = key,
                            contentType = { "item" },
                            itemContent = itemContent
                        )

                        // Spacer
                        item(contentType = "spacer", span = fullLineSpan, key = "${header}_items_end") {
                            Spacer(Modifier.padding(vertical = CP.normal))
                        }
                    }
                }
            )
        }
    )
}
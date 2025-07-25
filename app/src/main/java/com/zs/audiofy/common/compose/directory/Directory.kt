/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 19-05-2025.
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

@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.zs.audiofy.common.compose.directory

import androidx.activity.compose.BackHandler
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReplyAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.PlaylistPlay
import androidx.compose.material.icons.outlined.RemoveCircle
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material.icons.rounded.ShuffleOn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zs.audiofy.R
import com.zs.audiofy.common.Action
import com.zs.audiofy.common.SelectionTracker
import com.zs.audiofy.common.compose.Filters
import com.zs.audiofy.common.compose.FloatingActionMenu
import com.zs.audiofy.common.compose.FloatingLargeTopAppBar
import com.zs.audiofy.common.compose.LocalNavController
import com.zs.audiofy.common.compose.OverflowMenu
import com.zs.audiofy.common.compose.ProvideAnimationScope
import com.zs.audiofy.common.compose.background
import com.zs.audiofy.common.compose.emit
import com.zs.audiofy.common.compose.fadingEdge2
import com.zs.audiofy.common.compose.preference
import com.zs.audiofy.common.compose.rememberAcrylicSurface
import com.zs.audiofy.common.compose.shine
import com.zs.audiofy.common.compose.source
import com.zs.audiofy.settings.Settings
import com.zs.compose.foundation.Background
import com.zs.compose.foundation.background
import com.zs.compose.foundation.fullLineSpan
import com.zs.compose.foundation.plus
import com.zs.compose.foundation.stickyHeader
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.Button
import com.zs.compose.theme.ButtonDefaults
import com.zs.compose.theme.FilledTonalButton
import com.zs.compose.theme.FloatingActionButton
import com.zs.compose.theme.Icon
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.LocalContentColor
import com.zs.compose.theme.LocalWindowSize
import com.zs.compose.theme.adaptive.FabPosition
import com.zs.compose.theme.adaptive.Scaffold
import com.zs.compose.theme.adaptive.content
import com.zs.compose.theme.appbar.AppBarDefaults
import com.zs.compose.theme.minimumInteractiveComponentSize
import com.zs.compose.theme.sharedBounds
import com.zs.compose.theme.text.Label
import com.zs.compose.theme.text.Text
import com.zs.compose.theme.text.TextField
import com.zs.compose.theme.text.TextFieldDefaults
import com.zs.compose.theme.text.TonalHeader
import dev.chrisbanes.haze.HazeState
import androidx.compose.foundation.layout.PaddingValues as Padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState as GridState
import com.zs.audiofy.common.compose.ContentPadding as CP
import com.zs.compose.foundation.textResource as stringResource

private val DEFAULT_MIN_SIZE = 80.dp

// The default padding to content
private val ContentPadding = Padding(start = CP.normal, end = CP.normal, bottom = CP.normal)

//
private const val SHOW_FAB = 0
private const val SHOW_SEARCH_VIEW = 1
private const val SHOW_ACTION_MENU = 2
private const val SHOW_NONE = -1

//
val FabSharedAnimModifier = Modifier.sharedBounds("fab")

//
@Composable
@NonRestartableComposable
private fun SearchView(
    state: TextFieldState,
    background: Background,
    onCloseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val requester = remember { FocusRequester() }
    val colors = AppTheme.colors
    TextField(
        state = state,
        leadingIcon = {
            Icon(
                imageVector = Icons.Outlined.Search,
                contentDescription = null
            )
        },
        placeholder = { Label(text = stringResource(R.string.search_placeholder)) },
        label = { Label(text = stringResource(R.string.search)) },
        colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.Transparent),
        modifier = modifier
            .widthIn(min = 320.dp)
            .focusRequester(requester)
            .border(colors.shine, AppTheme.shapes.small)
            .shadow(6.dp, AppTheme.shapes.small)
            .background(background),
        trailingIcon = {
            IconButton(
                Icons.Default.Close,
                onClick = onCloseClick,
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

/**
 * A generic composable for displaying categorized lists (directories) like playlists, folders, etc.
 *
 * Provides a standard UI with top bar, search, filters, and a lazy grid.
 *
 * @param T The type of data items displayed.
 * @param viewState The state of the directory view, including data, title, query, and filters.
 * @param minSize The minimum size of items in the grid. Defaults to [DEFAULT_MIN_SIZE].
 * @param key A stable key for each item to optimize updates.
 * @param itemContent The composable function for rendering each item.
 */
@Composable
fun <T> Directory(
    viewState: DirectoryViewState<T>,
    minSize: Dp = DEFAULT_MIN_SIZE,
    onActionClick: ((item: Action) -> Unit)? = null,
    key: ((item: T) -> Any)? = null,
    itemContent: @Composable LazyGridItemScope.(item: T) -> Unit
) {
    val state = GridState()
    var isSearchVisible by remember { mutableStateOf(false) }
    val (width, height) = LocalWindowSize.current
    // Properties
    val surface = rememberAcrylicSurface()
    val inAppNavInsets = WindowInsets.content
    val topAppBarScrollBehavior = AppBarDefaults.exitUntilCollapsedScrollBehavior()
    val colors = AppTheme.colors
    // Handle back navigation.
    val navController = LocalNavController.current
    val onNavigateUp: () -> Unit = {
        when {
            isSearchVisible -> isSearchVisible = false
            viewState.focused != null -> viewState.focused = null
            else -> navController.navigateUp()
        }
    }
    BackHandler(enabled = isSearchVisible || viewState.focused != null, onNavigateUp)
    //
    val showFab = when {
        viewState.focused != null -> SHOW_ACTION_MENU
        isSearchVisible -> SHOW_SEARCH_VIEW
        viewState.primaryAction != null -> SHOW_FAB
        else -> SHOW_NONE
    }
    //
    Scaffold(
        fabPosition = FabPosition.End,
        //
        topBar = {
            FloatingLargeTopAppBar(
                title = {
                    Text(
                        viewState.title,
                        maxLines = 2,
                        fontWeight = FontWeight.Light,
                        lineHeight = 24.sp,
                    )
                },
                scrollBehavior = topAppBarScrollBehavior,
                background = colors.background(surface),
                insets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
                navigationIcon = {
                    val favicon = viewState.favicon
                    if (favicon != null)
                        return@FloatingLargeTopAppBar Icon(
                            favicon,
                            contentDescription = viewState.title.toString(),
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                    // Else add navigate-up button
                    IconButton(
                        icon = Icons.AutoMirrored.Filled.ReplyAll,
                        contentDescription = null,
                        onClick = onNavigateUp
                    )
                },
                actions = {
                    IconButton(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        onClick = { isSearchVisible = !isSearchVisible }
                    )
                    val actions = viewState.actions
                    if (actions.isEmpty() || viewState.focused != null)
                        return@FloatingLargeTopAppBar
                    // SOnce actions are non- empty
                    // therefore onActionClick must not be null.
                    OverflowMenu(items = actions, onActionClick!!)
                }
            )
        },
        //
        floatingActionButton = {
            ProvideAnimationScope(
                showFab,
                Modifier.windowInsetsPadding(
                    WindowInsets.ime
                        .union(inAppNavInsets)
                        .union(WindowInsets.navigationBars)
                ),
                content = { value ->
                    when (value) {
                        SHOW_FAB -> {
                            val action = viewState.primaryAction!!
                            FloatingActionButton(
                                onClick = { onActionClick!!.invoke(action) },
                                modifier = FabSharedAnimModifier,
                                content = {
                                    Icon(
                                        imageVector = viewState.primaryAction!!.icon!!,
                                        contentDescription = null
                                    )
                                }
                            )
                        }

                        SHOW_ACTION_MENU -> FloatingActionMenu(
                            colors.background(surface),
                            modifier = FabSharedAnimModifier
                        ) {
                            OverflowMenu(viewState.actions, onActionClick!!, collapsed = 4)
                        }

                        SHOW_SEARCH_VIEW -> SearchView(
                            viewState.query,
                            colors.background(surface),
                            onCloseClick = { isSearchVisible = false },
                            modifier = FabSharedAnimModifier
                        )
                    }
                }
            )
        },
        //
        content = {
            // Collect the data from the viewState, initially null representing loading state.
            // Get the grid item size multiplier from user preferences.
            val data by viewState.data.collectAsState()
            val multiplier by preference(Settings.GRID_ITEM_SIZE_MULTIPLIER)
            val padding = ContentPadding +
                    inAppNavInsets.union(WindowInsets.content).union(WindowInsets.systemBars)
                        .asPaddingValues()
            LazyVerticalGrid(
                state = state,
                columns = GridCells.Adaptive(minSize * multiplier),
                verticalArrangement = CP.SmallArrangement,
                horizontalArrangement = CP.SmallArrangement,
                contentPadding = padding,
                modifier = Modifier
                    .fillMaxSize()
                    .source(surface)
                    .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                    .fadingEdge2(50.dp),
                content = {
                    // Emit state: Get the album data, if null, return from LazyVerticalGrid content.
                    val values = emit(data) ?: return@LazyVerticalGrid
                    // Filters: Display the filters section.
                    item(
                        "directory_filters",
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
                        // only show this if non-blank.
                        if (header.isNotBlank()) stickyHeader(
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

                        //
                        items(
                            values,
                            key = key,
                            contentType = { "item" },
                            itemContent = itemContent
                        )

                        // Spacer
                        item(
                            contentType = "spacer",
                            span = fullLineSpan,
                            key = "${header}_items_end"
                        ) {
                            Spacer(Modifier.padding(vertical = CP.normal))
                        }
                    }

                }
            )
        }
    )
}

private val SelectionTracker.Level.toImageVector
    get() = when (this) {
        SelectionTracker.Level.NONE -> Icons.Outlined.Circle
        SelectionTracker.Level.PARTIAL -> Icons.Outlined.RemoveCircle
        SelectionTracker.Level.FULL -> Icons.Outlined.Verified
    }

private val FilesHorizontalPadding = CP.large
private val FilesHeaderPadding = Padding(horizontal = CP.large, vertical = 4.dp)

@Composable
fun <T> Files(
    viewState: FilesViewState<T>,
    onTapAction: (value: Action) -> Unit,
    key: ((item: T) -> Any)? = null,
    surface: HazeState = rememberAcrylicSurface(),
    itemContent: @Composable LazyItemScope.(item: T) -> Unit
) {
    val state = rememberLazyListState()
    var isSearchVisible by remember { mutableStateOf(false) }
    val (width, height) = LocalWindowSize.current
    // Properties
    val inAppNavInsets = WindowInsets.content
    val topAppBarScrollBehavior = AppBarDefaults.exitUntilCollapsedScrollBehavior()
    val colors = AppTheme.colors
    // Handle back navigation.
    val navController = LocalNavController.current
    val onNavigateUp: () -> Unit = {
        when {
            isSearchVisible -> isSearchVisible = false
            viewState.isInSelectionMode -> viewState.clear()
            else -> navController.navigateUp()
        }
    }
    BackHandler(enabled = isSearchVisible || viewState.isInSelectionMode, onNavigateUp)
    //
    val showFab = when {
        viewState.isInSelectionMode -> SHOW_ACTION_MENU
        isSearchVisible -> SHOW_SEARCH_VIEW
        else -> SHOW_NONE
    }

    //
    Scaffold(
        fabPosition = FabPosition.End,
        // Collapsable TopAppBar
        topBar = {
            FilesTopAppBar(
                info = viewState.info,
                behavior = topAppBarScrollBehavior,
                background = colors.background(surface),
                actions = {
                    IconButton(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        onClick = { isSearchVisible = !isSearchVisible }
                    )
                }
            )
        },

        // Fab as Search, FAB or Selection Menu.
        // Maybe allow it to show names of items.
        floatingActionButton = {
            ProvideAnimationScope(
                showFab,
                Modifier.windowInsetsPadding(
                    WindowInsets.ime
                        .union(inAppNavInsets)
                        .union(WindowInsets.navigationBars)
                ),
                content = { value ->
                    when (value) {
                        SHOW_ACTION_MENU -> FloatingActionMenu(
                            colors.background(surface),
                            modifier = FabSharedAnimModifier
                        ) {
                            OverflowMenu(viewState.actions, onTapAction, collapsed = 4)
                        }

                        SHOW_SEARCH_VIEW -> SearchView(
                            viewState.query,
                            colors.background(surface),
                            onCloseClick = { isSearchVisible = false },
                            modifier = FabSharedAnimModifier
                        )
                    }
                }
            )
        },

        // The  real content.
        content = {
            // Collect the data from the viewState, initially null representing loading state.
            // Get the grid item size multiplier from user preferences.
            val data = viewState.data
            val insets = inAppNavInsets.union(WindowInsets.content).union(WindowInsets.systemBars)
            LazyColumn(
                state = state,
                contentPadding = insets.asPaddingValues(),
                modifier = Modifier
                    .fillMaxSize()
                    .source(surface)
                    .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                    .fadingEdge2(50.dp),
                content = {
                    val values = emit(data) ?: return@LazyColumn

                    // Play/Pause buttons
                    item(
                        contentType = "play_buttons",
                        key = "key_play_buttons",
                        content = {
                            // Buttons
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = FilesHorizontalPadding)
                                    .widthIn(max = 500.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = CP.MediumArrangement,
                                content = {
                                    val modifier = Modifier.weight(1f).height(42.dp)
                                    // Shuffle
                                    FilledTonalButton (
                                        content = {
                                            Icon(Icons.Rounded.ShuffleOn, contentDescription = null)
                                            Spacer(Modifier.padding(horizontal = ButtonDefaults.IconSpacing))
                                            Label(text = androidx.compose.ui.res.stringResource(id = R.string.shuffle))
                                        },
                                        onClick = viewState::shuffle,
                                        shapes = ButtonDefaults.shapes,
                                        modifier = modifier
                                    )

                                    // play
                                    Button (
                                        content = {
                                            Icon(Icons.Outlined.PlaylistPlay, contentDescription = null)
                                            Spacer(Modifier.padding(horizontal = ButtonDefaults.IconSpacing))
                                            Label(text = androidx.compose.ui.res.stringResource(R.string.play))
                                        },
                                        onClick = viewState::play,
                                        shapes = ButtonDefaults.shapes,
                                        modifier = modifier
                                    )
                                }
                            )
                        }
                    )

                    // Filters: Display the filters section.
                    item(
                        "directory_filters",
                        contentType = "filters",
                        content = {
                            Filters(
                                viewState.filter,
                                viewState.orders,
                                modifier = Modifier.padding(
                                    horizontal = FilesHorizontalPadding,
                                    vertical = CP.normal
                                ),
                                onRequest = {
                                    when {
                                        it == null -> viewState.filter(ascending = !viewState.filter.first)
                                        else -> viewState.filter(order = it)
                                    }
                                }
                            )
                        }
                    )

                    // content
                    for ((header, values) in values) {
                        // Selection level of the group.
                        if (header.isNotBlank()) {
                            val level by viewState.isGroupSelected(header.toString())
                            stickyHeader(state, key = header, contentType = "header") {
                                Row(
                                    Modifier
                                        .padding(FilesHeaderPadding)
                                        .fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                    content = {
                                        TonalHeader(header)
                                        // toggle
                                        IconButton(
                                            icon = level.toImageVector,
                                            contentDescription = null,
                                            tint = if (level == SelectionTracker.Level.FULL) AppTheme.colors.accent else LocalContentColor.current,
                                            onClick = { viewState.select(header.toString()) }
                                        )
                                    }
                                )
                            }

                        }

                        //
                        items(
                            values,
                            key = key,
                            contentType = { "item" },
                            itemContent = itemContent
                        )

                        // Spacer
                        item(
                            contentType = "spacer",
                            key = "${header}_items_end",
                            content = {
                                Spacer(Modifier.padding(bottom = CP.normal))
                            }
                        )
                    }
                }
            )
        }
    )
}
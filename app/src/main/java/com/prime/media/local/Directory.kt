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

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReplyAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prime.media.R
import com.prime.media.common.Filters
import com.prime.media.common.ListHeader
import com.prime.media.common.Mapped
import com.prime.media.common.Regular
import com.prime.media.common.dynamicBackdrop
import com.prime.media.common.emit
import com.prime.media.common.fullLineSpan
import com.prime.media.old.common.LocalNavController
import com.prime.media.settings.AppConfig
import com.primex.core.plus
import com.primex.core.thenIf
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.zs.core_ui.AppTheme
import com.zs.core_ui.AppTheme.colors
import com.zs.core_ui.Colors
import com.zs.core_ui.None
import com.zs.core_ui.adaptive.TwoPane
import com.zs.core_ui.adaptive.contentInsets
import com.zs.core_ui.stickyHeader
import dev.chrisbanes.haze.HazeStyle
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.PaddingValues as Padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState as GridState
import androidx.compose.runtime.rememberCoroutineScope as CoroutineScope
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient as VerticalGradient
import com.prime.media.common.rememberHazeState as BackdropObserver
import com.primex.core.textResource as stringResource
import com.zs.core_ui.ContentPadding as CP
import dev.chrisbanes.haze.HazeState as BackdropProvider
import dev.chrisbanes.haze.haze as observerBackdrop

private const val TAG = "Directory"

private val FloatingTopBarShape = RoundedCornerShape(20)
private val Colors.floatingTopBarBorder: BorderStroke
@Composable
inline get() =  BorderStroke(0.5.dp, VerticalGradient(
    listOf(
        if (isLight) colors.background(2.dp) else Color.Gray.copy(0.24f),
        if (isLight) colors.background(2.dp) else Color.Gray.copy(0.24f),
    )
))

/**
 * Represents a Top app bar for this screen.
 */
@Composable
@NonRestartableComposable
private fun FloatingTopAppBar(
    title: CharSequence,
    modifier: Modifier = Modifier,
    onToggleSearch: () -> Unit,
    backdropProvider: BackdropProvider? = null,
    insets: WindowInsets = WindowInsets.None,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                VerticalGradient(
                    listOf(
                        colors.background(1.dp),
                        colors.background.copy(alpha = 0.5f),
                        Color.Transparent
                    )
                )
            ),
        content = {
            TopAppBar(
                navigationIcon = {
                    val navController = LocalNavController.current
                    IconButton(
                        imageVector = Icons.AutoMirrored.Filled.ReplyAll,
                        onClick = navController::navigateUp
                    )
                },
                title = {
                    Label(
                        text = title,
                        style = AppTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                windowInsets = WindowInsets.None,
                actions = {
                    IconButton(
                        Icons.Outlined.Search,
                        onClick = onToggleSearch
                    )
                },
                backgroundColor = Color.Transparent,
                elevation = 0.dp,
                modifier = Modifier
                    .widthIn(max = 500.dp)
                    .windowInsetsPadding(insets)
                    .padding(horizontal = CP.xLarge, vertical = CP.small)
                    .shadow(8.dp, FloatingTopBarShape)
                    .border(colors.floatingTopBarBorder, FloatingTopBarShape)
                    .height(52.dp)
                    .dynamicBackdrop(
                        backdropProvider,
                        HazeStyle.Regular(colors.background(0.4.dp)),
                        colors.background,
                        colors.accent
                    )
            )
        }
    )
}

private val GRID_ITEM_SPACING = Arrangement.spacedBy(CP.small)
private fun <T> LazyGridScope.content(
    data: Mapped<T>,
    state: LazyGridState,
    key: ((item: T) -> Any)? = null,
    itemContent: @Composable LazyGridItemScope.(item: T) -> Unit
) {
    for ((header, values) in data) {
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
                        content = { ListHeader(header) }
                    )
                }
            )

        for (value in values)
            item(key = if (key != null) key(value) else null, content = { itemContent(value) })
    }
}

/**
 * Represents the search view.
 *
 * @param visible Boolean indicating whether the search view is visible.
 * @param state The [TextFieldState] for managing the search query text.
 */
@Composable
@NonRestartableComposable
private fun SearchView(
    visible: Boolean,
    state: TextFieldState,
    modifier: Modifier = Modifier
) {
    // animate visibility based on show is
    AnimatedVisibility(
        visible,
        modifier = modifier,
        content = {
            val requester = remember { FocusRequester() }
            OutlinedTextField(
                state = state,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null
                    )
                },
                placeholder = { Label(text = stringResource(R.string.search_placeholder)) },
                label = { Label(text = stringResource(R.string.search)) },
                shape = AppTheme.shapes.compact,
                modifier = Modifier.focusRequester(requester),
                trailingIcon = {
                    IconButton(Icons.Default.Close, onClick = state::clearText)
                },
            )

            // Request focus when the search view becomes visible
            DisposableEffect(Unit) {
                requester.requestFocus()
                onDispose { }
            }
        }
    )
}

@Composable
fun <T> Directory(
    viewState: DirectoryViewState<T>,
    key: ((item: T) -> Any)? = null,
    itemContent: @Composable LazyGridItemScope.(item: T) -> Unit
) {
    val inAppNavBarInsets = WindowInsets.contentInsets.add(WindowInsets.navigationBars).asPaddingValues()
    val state = GridState()
    var isSearchVisible by remember { mutableStateOf(false) }

    // Properties
    val observer = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> BackdropObserver()
        else -> null
    }
    // content
    TwoPane(
        topBar = {
            val scope = CoroutineScope()
            FloatingTopAppBar(
                insets = WindowInsets.statusBars,
                // Toggle the visibility of the search bar.
                // If search is visible, scroll to the top of the grid.
                onToggleSearch = {
                    isSearchVisible = !isSearchVisible
                    if (isSearchVisible)
                        scope.launch() { state.scrollToItem(0) }
                },
                backdropProvider = observer,
                title = viewState.title
            )
        },
        primary = {
            // Collect the data from the viewState, initially null representing loading state.
            // Get the grid item size multiplier from user preferences.
            val data by viewState.data.collectAsState()
            val multiplier = AppConfig.gridItemSizeMultiplier
            LazyVerticalGrid(
                state = state,
                columns = GridCells.Adaptive(80.dp * multiplier),
                verticalArrangement = GRID_ITEM_SPACING,
                horizontalArrangement = GRID_ITEM_SPACING,
                // Apply padding for content insets and in-app navigation bar.
                contentPadding = Padding(horizontal = CP.normal) + inAppNavBarInsets + WindowInsets.contentInsets.asPaddingValues(),
                modifier = Modifier.thenIf(observer != null) { observerBackdrop(observer!!) },
                content = {
                    // Search Node
                    // Display the search view when isSearchVisible is true.
                    item(
                        contentType = "search_view",
                        key = "grid_search_view",
                        span = fullLineSpan,
                        content = {
                            SearchView(
                                isSearchVisible,
                                viewState.query,
                                if (!isSearchVisible) Modifier else Modifier.padding(
                                    horizontal = CP.normal,
                                    vertical = CP.small
                                )
                            )
                        }
                    )
                    // Emit state: Get the album data, if null, return from LazyVerticalGrid content.
                    val values = emit(data) ?: return@LazyVerticalGrid
                    // Filters: Display the filters section.
                    item(
                        "",
                        contentType = "filters",
                        span = fullLineSpan,
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
                    content(values, state, key, itemContent)
                }
            )
        }
    )
}
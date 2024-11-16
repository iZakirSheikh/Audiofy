@file:OptIn(ExperimentalMaterialApi::class)

package com.prime.media.local.albums

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.Chip
import androidx.compose.material.ChipDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FilterChip
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ReplyAll
import androidx.compose.material.icons.automirrored.outlined.Sort
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.prime.media.R
import com.prime.media.common.Filter
import com.prime.media.common.Mapped
import com.prime.media.common.emit
import com.prime.media.common.fullLineSpan
import com.prime.media.common.menu.Action
import com.prime.media.common.preference
import com.prime.media.old.common.LocalNavController
import com.prime.media.old.directory.store.Audios
import com.prime.media.settings.Settings
import com.primex.core.plus
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.appbar.TopAppBarDefaults
import com.primex.material2.appbar.TopAppBarScrollBehavior
import com.zs.core.store.Album
import com.zs.core_ui.AppTheme
import com.zs.core_ui.CollapsableNeumorphicLargeAppBar
import com.zs.core_ui.Header
import com.zs.core_ui.None
import com.zs.core_ui.adaptive.TwoPane
import com.zs.core_ui.adaptive.contentInsets
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.PaddingValues as Padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState as GridState
import androidx.compose.foundation.rememberScrollState as ScrollState
import androidx.compose.runtime.rememberCoroutineScope as CoroutineScope
import com.primex.core.textResource as stringResource
import com.zs.core_ui.ContentPadding as CP

private const val TAG = "Albums"

/**
 * Represents a Top app bar for this screen.
 */
@Composable
@NonRestartableComposable
private fun TopAppBar(
    modifier: Modifier = Modifier,
    insets: WindowInsets = WindowInsets.None,
    onToggleSearch: () -> Unit,
    behaviour: TopAppBarScrollBehavior? = null
) {
    CollapsableNeumorphicLargeAppBar(
        modifier = modifier,
        title = { Label(text = stringResource(id = R.string.albums)) },
        navigationIcon = {
            val navController = LocalNavController.current
            IconButton(
                imageVector = Icons.AutoMirrored.Filled.ReplyAll,
                onClick = navController::navigateUp
            )
        },
        scrollBehavior = behaviour,
        insets = insets,
        actions = {
            IconButton(
                Icons.Outlined.Search,
                onClick = onToggleSearch
            )
        }
    )
}

/**
 * Item header.
 * //TODO: Handle padding in parent composable.
 */
private val HEADER_MARGIN = Padding(CP.medium, CP.large, CP.medium, CP.normal)

@NonRestartableComposable
@Composable
private fun ListHeader(
    value: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(HEADER_MARGIN),
        content = {
            when {
                // If the value has only one character, display it as a circular header.
                // Limit the width of the circular header
                value.length == 1 -> Header(
                    text = value,
                    style = AppTheme.typography.headlineSmall,
                    modifier = Modifier
                        .background(AppTheme.colors.background(1.dp), CircleShape)
                        .widthIn(100.dp)
                        .padding(CP.xLarge, vertical = CP.medium),
                )
                // If the value has more than one character, display it as a label.
                // Limit the label to a maximum of two lines
                // Limit the width of the label
                else -> Label(
                    text = value,
                    maxLines = 2,
                    fontWeight = FontWeight.Normal,
                    style = AppTheme.typography.titleSmall,
                    modifier = Modifier
                        .widthIn(max = 220.dp)
                        .background(AppTheme.colors.background(1.dp), CircleShape)
                        .padding(horizontal = CP.normal, vertical = CP.small)
                )
            }
        }
    )
}

private val GRID_ITEM_SPACING = Arrangement.spacedBy(CP.small)
private fun LazyGridScope.content(
    navController: NavHostController,
    data: Mapped<Album>
) {
    for ((header, values) in data) {
        if (header.isNotBlank()) // only show this if non-blank.
            item(
                header,
                span = fullLineSpan,
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
            key = Album::id,
            contentType = { "album" },
            itemContent = {
                Album(
                    it,
                    modifier = Modifier
                        .animateItem()
                        .clickable {
                            val direction = Audios.direction(Audios.GET_FROM_ALBUM, it.title)
                            navController.navigate(direction)
                        },
                )
            }
        )
    }
}

/**
 * Represents a [Row] of [Chip]s for ordering and filtering.
 *
 * @param current The currently selected filter.
 * @param values The list of supported filter options.
 * @param onRequest Callback function to be invoked when a filter option is selected. null
 * represents ascending/descending toggle.
 */
@Composable
private inline fun Filters(
    current: Filter,
    values: List<Action>,
    crossinline onRequest: (order: Action?) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(ScrollState()),
        horizontalArrangement = GRID_ITEM_SPACING,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Chip for ascending/descending order
        val (ascending, order) = current
        val padding = Padding(vertical = 6.dp)
        Chip(
            onClick = { onRequest(null) },
            leadingIcon = {
                Icon(
                    Icons.AutoMirrored.Outlined.Sort,
                    contentDescription = "ascending",
                    modifier = Modifier.rotate(if (ascending) 0f else 180f)
                )
            },
            content = {
                Label(if (ascending) "Ascending" else "Descending", modifier = Modifier.padding(padding))
            },
            colors = ChipDefaults.chipColors(
                backgroundColor = AppTheme.colors.accent,
                contentColor = AppTheme.colors.onAccent
            ),
            modifier = Modifier.defaultMinSize(minHeight = 37.dp).padding(end = CP.medium),
            shape = AppTheme.shapes.compact
        )
        // Rest of the chips for selecting filter options
        val colors = ChipDefaults.filterChipColors(
            backgroundColor = AppTheme.colors.background(1.dp),
            selectedBackgroundColor = AppTheme.colors.background(2.dp),
            selectedContentColor = AppTheme.colors.accent
        )

        for (value in values) {
            val selected = value == order
            FilterChip(
                selected = selected,
                onClick = { onRequest(value) },
                content = { Label(stringResource(value.label), modifier = Modifier.padding(padding)) },
                colors = colors,
                border = if (!selected) null else BorderStroke(Dp.Hairline, AppTheme.colors.accent)
            )
        }
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
                placeholder = { Label(text = "Type here to search!!") },
                label = { Label(text = "Search") },
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
fun Albums(viewState: AlbumsViewState) {
    //
    val inAppNavBarInsets = WindowInsets.contentInsets
    val behaviour = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val state = GridState()
    var isSearchVisible by remember { mutableStateOf(false) }
    TwoPane(
        topBar = {
            val scope = CoroutineScope()
            TopAppBar(
                behaviour = behaviour,
                insets = WindowInsets.statusBars,
                // Toggle the visibility of the search bar.
                // If search is visible, scroll to the top of the grid.
                onToggleSearch = {
                    isSearchVisible = !isSearchVisible
                    if (isSearchVisible)
                        scope.launch() { state.scrollToItem(0) }
                },
            )
        },
        primary = {
            // Collect the data from the viewState, initially null representing loading state.
            // Get the grid item size multiplier from user preferences.
            val data by viewState.data.collectAsState()
            val multiplier by preference(Settings.GRID_ITEM_SIZE_MULTIPLIER)
            val navController = LocalNavController.current
            LazyVerticalGrid(
                state = state,
                columns = GridCells.Adaptive(80.dp * multiplier),
                verticalArrangement = GRID_ITEM_SPACING,
                horizontalArrangement = GRID_ITEM_SPACING,
                // Apply padding for content insets and in-app navigation bar.
                contentPadding = Padding(horizontal = CP.normal) + inAppNavBarInsets + WindowInsets.contentInsets,
                modifier = Modifier
                    .nestedScroll(behaviour.nestedScrollConnection),
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
                                if(!isSearchVisible) Modifier else Modifier.padding(horizontal = CP.normal, vertical = CP.small)
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
                    content(navController, values)
                }
            )
        }
    )
}

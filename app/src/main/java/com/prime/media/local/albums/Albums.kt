@file:OptIn(ExperimentalMaterialApi::class)

package com.prime.media.local.albums

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material.ExperimentalMaterialApi
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.prime.media.R
import com.prime.media.common.Filters
import com.prime.media.common.ListHeader
import com.prime.media.common.Mapped
import com.prime.media.common.Regular
import com.prime.media.common.dynamicBackdrop
import com.prime.media.common.emit
import com.prime.media.common.fullLineSpan
import com.prime.media.common.preference
import com.prime.media.old.common.LocalNavController
import com.prime.media.old.directory.store.Audios
import com.prime.media.settings.Settings
import com.primex.core.plus
import com.primex.core.thenIf
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.zs.core.store.Album
import com.zs.core_ui.AppTheme
import com.zs.core_ui.AppTheme.colors
import com.zs.core_ui.None
import com.zs.core_ui.adaptive.TwoPane
import com.zs.core_ui.adaptive.contentInsets
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

private const val TAG = "Albums"

private val FloatingTopBarShape = CircleShape

/**
 * Represents a Top app bar for this screen.
 */
@Composable
@NonRestartableComposable
private fun FloatingTopAppBar(
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
                        text = stringResource(id = R.string.albums),
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
                    .widthIn(max = 550.dp)
                    .windowInsetsPadding(insets)
                    .padding(horizontal = CP.xLarge, vertical = CP.small)
                    .clip(FloatingTopBarShape)
                    .border(
                        0.5.dp,
                        VerticalGradient(
                            listOf(
                                if (colors.isLight) colors.background(2.dp) else Color.Gray.copy(
                                    0.24f
                                ),
                                Color.Transparent,
                                Color.Transparent,
                                if (colors.isLight) colors.background(2.dp) else Color.Gray.copy(
                                    0.24f
                                ),
                            )
                        ),
                        FloatingTopBarShape
                    )
                    .height(48.dp)
                    .dynamicBackdrop(
                        backdropProvider,
                        HazeStyle.Regular(colors.background),
                        colors.background,
                        colors.accent
                    )

            )
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
                    Box(
                        content = {
                            ListHeader(
                                header,
                                modifier = Modifier.animateItem()
                            )
                        }
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
    val inAppNavBarInsets = WindowInsets.contentInsets
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
                backdropProvider = observer
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
                    content(navController, values)
                }
            )
        }
    )
}
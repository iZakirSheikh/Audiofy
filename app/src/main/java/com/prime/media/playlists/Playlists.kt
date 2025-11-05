/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 19-10-2024.
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

package com.prime.media.playlists

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.FabPosition
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material.icons.outlined.FolderDelete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.prime.media.R
import com.prime.media.common.FloatingActionMenu
import com.prime.media.common.LocalSystemFacade
import com.prime.media.common.emit
import com.prime.media.old.common.LocalNavController
import com.prime.media.old.directory.playlists.Members
import com.primex.core.composableOrNull
import com.primex.core.plus
import com.primex.core.thenIf
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.appbar.LargeTopAppBar
import com.primex.material2.appbar.TopAppBarDefaults
import com.primex.material2.appbar.TopAppBarScrollBehavior
import com.zs.core.db.Playlist
import com.zs.core_ui.AppTheme
import com.zs.core_ui.LocalWindowSize
import com.zs.core_ui.None
import com.zs.core_ui.Range
import com.zs.core_ui.WindowSize
import com.zs.core_ui.adaptive.TwoPane
import com.zs.core_ui.adaptive.contentInsets
import com.zs.core_ui.scale
import androidx.compose.foundation.combinedClickable as clickable
import androidx.compose.foundation.layout.PaddingValues as Padding
import com.primex.core.textResource as stringResource
import com.zs.core_ui.ContentPadding as CP
import com.zs.core_ui.WindowStyle as Flags

private const val TAG = "Playlists"

// Applies when the top bar does not occupy the entire screen width.
private val FloatingTopBarShape = RoundedCornerShape(15)

/**
 * Represents the Top App Bar for this screen.
 *
 * Ensures proper layout by handling padding and margins based on the provided shape.
 *
 * @param modifier Modifier to apply to this Top App Bar.
 * @param shape Shape of the Top App Bar. Defaults to `null`. If non-null, the Top App Bar will be
 * considered as a Floating App Bar.
 * @param behaviour TopAppBarScrollBehavior to manage scroll behavior.
 */
@Composable
@NonRestartableComposable
private fun Toolbar(
    modifier: Modifier = Modifier,
    insets: WindowInsets = WindowInsets.None,
    shape: Shape? = null,
    onNavigateBack: () -> Unit,
    behaviour: TopAppBarScrollBehavior? = null
) {
    val isFloating = shape != null
    LargeTopAppBar(
        modifier = modifier.thenIf(isFloating) {
            windowInsetsPadding(insets)
                .padding(horizontal = CP.normal)
                .clip(shape!!)
        },
        title = { Label(text = stringResource(id = R.string.playlists)) },
        navigationIcon = {
            IconButton(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                onClick = onNavigateBack
            )
        },
        scrollBehavior = behaviour,
        windowInsets = if (!isFloating) insets else WindowInsets.None,
        style = TopAppBarDefaults.largeAppBarStyle(
            scrolledContainerColor = AppTheme.colors.background(3.dp),
            scrolledContentColor = AppTheme.colors.onBackground,
            containerColor = AppTheme.colors.background,
            contentColor = AppTheme.colors.onBackground,
        ),
    )
}

private val MIN_CELL_WIDTH = 120.dp
private val GridArrangement = Arrangement.spacedBy(CP.small)
private val fabShape = RoundedCornerShape(25)

private val WindowSize.dialogAlignment
    get() = when{
        widthRange == Range.Compact && heightRange > Range.Medium ->  Alignment.BottomCenter
        widthRange > Range.Medium && heightRange < widthRange -> Alignment.CenterEnd
        else -> Alignment.Center
    }

/**
 * Represents the Playlists screen.
 */
@Composable
fun Playlists(viewState: PlaylistsViewState) {
    // Retrieve the current window size
    val clazz = LocalWindowSize.current
    val (width, _) = clazz
    // Determine if the screen is in compact width mode
    // Compact width screens, typically phones in portrait mode, do not require a floating toolbar.
    // A small width means that the in-app navigation bar is a BottomBar, not a Navigation Rail.
    val isCompactWidth = width < Range.Medium
    // Define the scroll behavior for the top app bar
    val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    // obtain the padding of BottomNavBar/NavRail
    val inAppNavBarInsets = WindowInsets.contentInsets.asPaddingValues()
    var highlighted: Playlist? by remember { mutableStateOf(null) }
    var showNewPlaylistDialog by remember { mutableStateOf(false) }
    val navController = LocalNavController.current
    // BackHandler .
    // This section manages back navigation events, including those triggered by:
    // - System hardware back button
    // - Toolbar navigation back button
    // - Two-pane dismiss callback
    // Prioritized actions are performed first, and the back press event is consumed to prevent default behavior.
    val onNavigateBack = {
        when {
            // If the secondary pane is visible, hide it.
            showNewPlaylistDialog -> showNewPlaylistDialog = false
            // If an item is highlighted, clear the highlighting.
            highlighted != null -> highlighted = null
        }
    }
    BackHandler(enabled = showNewPlaylistDialog || highlighted != null, onNavigateBack)
    // FAB Menu
    val floatingActionMenu = @Composable {
        Crossfade(
            targetState = highlighted == null,
            Modifier.padding(inAppNavBarInsets).thenIf(!isCompactWidth){ navigationBarsPadding()},
            label = "",
            content = { value ->
                when {
                    value -> FloatingActionButton(
                        shape = fabShape,
                        onClick = { showNewPlaylistDialog = true },
                        content = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) }
                    )

                    else -> FloatingActionMenu(
                        content = {
                            // Delete
                            IconButton(
                                imageVector = Icons.Outlined.FolderDelete,
                                onClick = { viewState.delete(highlighted ?: return@IconButton) }
                            )

                            // Rename
                            IconButton(
                                imageVector = Icons.Default.DriveFileRenameOutline,
                                onClick = { showNewPlaylistDialog = true }
                            )
                        }
                    )
                }
            }
        )
    }
    // Actual Content
    TwoPane(
        spacing = CP.normal,
        fabPosition = FabPosition.End,
        floatingActionButton = floatingActionMenu,
        topBar = {
            Toolbar(
                behaviour = topAppBarScrollBehavior,
                insets = TopAppBarDefaults.windowInsets,
                onNavigateBack = onNavigateBack,
                shape = if (isCompactWidth) null else FloatingTopBarShape,
            )
        },
        modifier = Modifier.animateContentSize(),
        dialog = composableOrNull(showNewPlaylistDialog) {
            Box(
                modifier = Modifier
                    .clickable(null, null){ showNewPlaylistDialog = false }
                    .background(Color.Black.copy(alpha = 0.5f))
                    .animateContentSize()
                    .fillMaxSize(),
                contentAlignment = clazz.dialogAlignment,
                content = {
                    NewPlaylist(
                        highlighted,
                        onConfirm = { newPlaylist ->
                            // Callback function executed when the user confirms the new/updated playlist
                            val isUpdate =
                                highlighted != null // Determine if we're updating an existing playlist
                            highlighted = null // Reset the highlighted playlist after confirmation
                            if (newPlaylist == null) {
                                // If the newPlaylist is null (user canceled), hide the secondary pane and return
                                showNewPlaylistDialog = false
                                return@NewPlaylist
                            }
                            // If it's an update, call viewState.update(); otherwise, call viewState.create()
                            when {
                                isUpdate -> viewState.update(newPlaylist)
                                else -> viewState.create(newPlaylist)
                            }
                            // Hide the secondary pane after creating or updating the playlist
                            showNewPlaylistDialog = false
                        },
                        modifier = Modifier.safeContentPadding()
                            .widthIn(max=360.dp)
                    )
                }
            )
        },
        primary = {
            val data by viewState.data.collectAsState(null)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(MIN_CELL_WIDTH),
                contentPadding = Padding(
                    if (isCompactWidth) CP.large else CP.medium,
                    vertical = CP.normal
                ) + inAppNavBarInsets,
                verticalArrangement = GridArrangement,
                horizontalArrangement = GridArrangement,
                // padding due to app bar
                modifier = Modifier
                    .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                    .windowInsetsPadding(WindowInsets.contentInsets),
                content = {
                    // emit state; if not return non-null values
                    val values = emit(data) ?: return@LazyVerticalGrid
                    // place the grid items
                    items(
                        values,
                        key = Playlist::id,
                        contentType = { "playlist_item" },
                        itemContent = { playlist ->
                            // TODO Explore the use of derived state to calculate the 'focused' value
                            //  instead of directly comparing 'highlighted' and 'playlist'. Derived state can improve
                            //  performance and ensure consistency.
                            // Determine if the current playlist is focused (selected)
                            val focused = highlighted == playlist
                            PlaylistItem(
                                value = playlist,
                                focused = focused,
                                modifier = Modifier
                                    .animateItem()
                                    .clickable(
                                        null,
                                        indication = scale(),
                                        // On long click, toggle the focused state of the playlist item
                                        onLongClick = {
                                            highlighted = if (!focused) playlist else null
                                        },
                                        onClick = {
                                            when {
                                                focused -> highlighted = null // If already focused, unfocused it
                                                highlighted != null -> highlighted = playlist // make it move focus
                                                else -> navController.navigate(Members.direction(playlist.name)) // Navigate to the playlist details
                                            }
                                        }
                                    )
                            )
                        }
                    )
                }
            )
        }
    )
    // Get the current system facade from the LocalSystemFacade
    val facade = LocalSystemFacade.current

    // SideEffect: Runs during composition or recomposition of the Composable.
    // Used for performing side-effects that affect external state like window styles.
    SideEffect {
        val style = facade.style  // Capture the current window style
        // Update the window style based on the strategy.
        // If the strategy is StackedTwoPaneStrategy, add the FLAG_APP_NAV_BAR_HIDDEN flag.
        // Otherwise, remove the FLAG_APP_NAV_BAR_HIDDEN flag.
        facade.style = when {
            showNewPlaylistDialog -> style + Flags.FLAG_APP_NAV_BAR_HIDDEN + Flags.FLAG_SYSTEM_BARS_APPEARANCE_DARK
            else -> style - Flags.FLAG_APP_NAV_BAR_HIDDEN - Flags.FLAG_SYSTEM_BARS_APPEARANCE_DARK
        }
        // Log the updated window style for debugging purposes.
        Log.d(TAG, "Playlists: ${facade.style}")
    }

    // DisposableEffect: Executes side-effects when a Composable enters or leaves the composition.
    // It ensures proper resource cleanup when the Composable is disposed (removed from the composition).
    DisposableEffect(Unit) {
        // Capture the original window style when the Composable is first composed.
        val original = facade.style
        // onDispose: This block is triggered when the Composable is disposed.
        // It restores the original window style to maintain a consistent state.
        onDispose {
            facade.style = original  // Reset to the original style on disposal
        }
    }
}
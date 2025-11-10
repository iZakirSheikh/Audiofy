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

package com.prime.media.settings

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Textsms
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.prime.media.R
import com.prime.media.common.FloatingLargeTopAppBar
import com.prime.media.common.LocalSystemFacade
import com.prime.media.common.Route
import com.prime.media.common.fadingEdge2
import com.prime.media.common.rememberHazeState
import com.primex.core.plus
import com.primex.core.textResource
import com.primex.core.thenIf
import com.primex.material2.Text
import com.primex.material2.appbar.TopAppBarDefaults
import com.zs.core_ui.AppTheme
import com.zs.core_ui.Header
import com.zs.core_ui.LocalWindowSize
import com.zs.core_ui.Range
import com.zs.core_ui.adaptive.HorizontalTwoPaneStrategy
import com.zs.core_ui.adaptive.SinglePaneStrategy
import com.zs.core_ui.adaptive.TwoPane
import com.zs.core_ui.adaptive.contentInsets
import com.zs.core_ui.sharedBounds
import dev.chrisbanes.haze.hazeSource
import androidx.compose.foundation.layout.PaddingValues as Padding
import androidx.compose.foundation.layout.WindowInsetsSides as WIS
import com.zs.core_ui.ContentPadding as CP

object RouteSettings : Route {

    // Used to style individual items within a preference section.
    val TopTileShape = RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp)
    val CentreTileShape = RectangleShape
    val BottomTileShape = RoundedCornerShape(0.dp, 0.dp, 24.dp, 24.dp)
    val SingleTileShape = RoundedCornerShape(24.dp)

    const val CONTENT_TYPE_HEADER = "header"
    val HeaderPadding = Padding(vertical = CP.normal, horizontal = CP.small)

    // The max width of the secondary pane
    private val sPaneMaxWidth = 320.dp

    @Composable
    operator fun invoke(viewState: SettingsViewState) {
        // Retrieve the current window size
        val (width, _) = LocalWindowSize.current
        // Determine the two-pane strategy based on window width range
        // when in mobile portrait; we don't show second pane;
        val strategy = when {
            width < Range.Medium -> SinglePaneStrategy
            else -> HorizontalTwoPaneStrategy(0.5f) // Use horizontal layout with 50% split for large screens
        }
        // obtain the padding of BottomNavBar/NavRail
        val inAppNavBarInsets = WindowInsets.contentInsets
        val surface = if (AppConfig.isBackgroundBlurEnabled) rememberHazeState() else null
        val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
        val colors = AppTheme.colors

        //
        BackHandler(enabled = viewState.save) {
            viewState.discard()
        }

        // Place the content
        TwoPane(
            strategy = strategy,
            topBar = {
                FloatingLargeTopAppBar(
                    title = {
                        Text(
                            text = textResource(R.string.settings),
                         //   maxLines = 2,
                           // fontWeight = FontWeight.Light,
                         //   lineHeight = 24.sp,
                           // overflow = TextOverflow.Ellipsis
                        )
                    },
                    scrollBehavior = topAppBarScrollBehavior,
                    insets = WindowInsets.systemBars.only(WIS.Top),
                    navigationIcon = {
                        Icon(
                            Icons.Default.Settings,
                            null,
                            modifier = Modifier.minimumInteractiveComponentSize()
                        )
                    },
                    actions = {
                        // Join our telegram channel
                        val facade = LocalSystemFacade.current
                        com.primex.material2.IconButton (
                            imageVector = Icons.Outlined.Textsms,
                            contentDescription = null,
                            onClick = { facade.launch(Settings.TelegramIntent) },
                        )
                        // Report Bugs on Github.
                        com.primex.material2.IconButton(
                            imageVector = Icons.Outlined.BugReport,
                            contentDescription = null,
                            onClick = { facade.launch(Settings.GitHubIssuesPage) },
                        )
                    },
                    backdrop = surface,
                )
            },
            // this will not be called when in single pane mode
            // this is just for decoration
            secondary = {
                if (strategy is SinglePaneStrategy) return@TwoPane
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(top = CP.small)
                        .widthIn(max = sPaneMaxWidth)
                        .windowInsetsPadding(
                            WindowInsets.systemBars.union(inAppNavBarInsets).only(
                                WIS.Vertical + WIS.End
                            )
                        ),
                    content = {
                        Header(
                            stringResource(R.string.about_us),
                            color = AppTheme.colors.accent,
                            drawDivider = true,
                            style = AppTheme.typography.titleLarge,
                            contentPadding = HeaderPadding
                        )
                        AboutUs()
                    }
                )
            },
            //
            floatingActionButton = {
                if (!viewState.save)
                    return@TwoPane
                val facade = LocalSystemFacade.current
                FloatingActionButton(
                    onClick = { viewState.commit(facade) },
                    shape = RoundedCornerShape(25),
                    modifier = Modifier.windowInsetsPadding(inAppNavBarInsets.union(WindowInsets.navigationBars)),
                    content = {
                        Icon(Icons.Outlined.Save, contentDescription = null)
                    }
                )
            },
            // MainContent
            primary = {
                val state = rememberLazyListState()
                LazyColumn(
                    state = state,
                    // In immersive mode, add horizontal padding to prevent settings from touching the screen edges.
                    // Immersive layouts typically have a bottom app bar, so extra padding improves aesthetics.
                    // Non-immersive layouts only need vertical padding.
                    contentPadding = Padding(horizontal = CP.large, CP.large) +
                            (WindowInsets.contentInsets.union(WindowInsets.systemBars)
                                .union(inAppNavBarInsets).only(
                                    WIS.Vertical
                                )).asPaddingValues(),
                    modifier = Modifier
                        .thenIf(surface != null){hazeSource(surface!!)}
                        .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                        .fadingEdge2(length = 56.dp),
                    content = {
                        //Sponsor
                        item(contentType = "sponsor") { Sponsor() }

                        // upgrades
                        item(contentType = CONTENT_TYPE_HEADER) {
                            Header(
                                stringResource(R.string.upgrades),
                                color = AppTheme.colors.accent,
                                drawDivider = true,
                                style = AppTheme.typography.titleSmall,
                                contentPadding = HeaderPadding
                            )
                        }
                        item { Upgrades() }
                        // App Preferences and Flags
                        preferences(viewState)
                        // AboutUs
                        // Load AboutUs here if this is mobile port
                        if (strategy !is SinglePaneStrategy)
                            return@LazyColumn
                        item(contentType = CONTENT_TYPE_HEADER) {
                            Header(
                                stringResource(R.string.about_us),
                                color = AppTheme.colors.accent,
                                drawDivider = true,
                                style = AppTheme.typography.titleSmall,
                                contentPadding = HeaderPadding
                            )
                        }

                        item(contentType = "about_us") { Column { AboutUs() } }
                    }
                )
            }
        )
    }
}
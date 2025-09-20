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

package com.zs.audiofy.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material.icons.outlined.Textsms
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zs.audiofy.R
import com.zs.audiofy.common.Route
import com.zs.audiofy.common.compose.FloatingLargeTopAppBar
import com.zs.audiofy.common.compose.LocalSystemFacade
import com.zs.audiofy.common.compose.background
import com.zs.audiofy.common.compose.fadingEdge2
import com.zs.audiofy.common.compose.rememberAcrylicSurface
import com.zs.audiofy.common.compose.source
import com.zs.compose.foundation.plus
import com.zs.compose.foundation.textResource
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.Icon
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.LocalWindowSize
import com.zs.compose.theme.TonalIconButton
import com.zs.compose.theme.WindowSize.Category
import com.zs.compose.theme.adaptive.HorizontalTwoPaneStrategy
import com.zs.compose.theme.adaptive.SinglePaneStrategy
import com.zs.compose.theme.adaptive.TwoPane
import com.zs.compose.theme.adaptive.content
import com.zs.compose.theme.appbar.AppBarDefaults
import com.zs.compose.theme.minimumInteractiveComponentSize
import com.zs.compose.theme.text.Header
import com.zs.compose.theme.text.Text
import androidx.compose.foundation.layout.PaddingValues as Padding
import androidx.compose.foundation.layout.WindowInsetsSides as WIS
import com.zs.audiofy.common.compose.ContentPadding as CP

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
            width < Category.Medium -> SinglePaneStrategy
            else -> HorizontalTwoPaneStrategy(0.5f) // Use horizontal layout with 50% split for large screens
        }
        // obtain the padding of BottomNavBar/NavRail
        val inAppNavBarInsets = WindowInsets.content
        val surface = rememberAcrylicSurface()
        val topAppBarScrollBehavior = AppBarDefaults.exitUntilCollapsedScrollBehavior()
        val colors = AppTheme.colors
        // Place the content
        TwoPane(
            strategy = strategy,
            topBar = {
                FloatingLargeTopAppBar(
                    title = {
                        Text(
                            textResource(R.string.scr_settings_title),
                            maxLines = 2,
                            fontWeight = FontWeight.Light,
                            lineHeight = 24.sp,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    scrollBehavior = topAppBarScrollBehavior,
                    background = colors.background(surface),
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
                        IconButton(
                            icon = Icons.Outlined.Textsms,
                            contentDescription = null,
                            onClick = { facade.launch(Settings.TelegramIntent) },
                        )

                        // Save Btn
                        if (viewState.save)
                            TonalIconButton(
                                icon = Icons.Outlined.Save,
                                onClick = { viewState.commit(facade) },
                                contentDescription = null
                            )

                        // Report Bugs on Github.
                        IconButton(
                            icon = Icons.Outlined.BugReport,
                            contentDescription = null,
                            onClick = { facade.launch(Settings.GitHubIssuesPage) },
                        )
                    }
                )
            },
            secondary = {
                // this will not be called when in single pane mode
                // this is just for decoration
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
                            style = AppTheme.typography.title3,
                            contentPadding = HeaderPadding
                        )
                        AboutUs()
                    }
                )
            },
            primary = {
                val state = rememberLazyListState()
                LazyColumn(
                    state = state,
                    // In immersive mode, add horizontal padding to prevent settings from touching the screen edges.
                    // Immersive layouts typically have a bottom app bar, so extra padding improves aesthetics.
                    // Non-immersive layouts only need vertical padding.
                    contentPadding = Padding(horizontal = CP.large, CP.normal) +
                            (WindowInsets.content.union(WindowInsets.systemBars)
                                .union(inAppNavBarInsets).only(
                                    WIS.Vertical
                                )).asPaddingValues(),
                    modifier = Modifier
                        .source(surface)
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
                                style = AppTheme.typography.title3,
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
                                style = AppTheme.typography.title3,
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
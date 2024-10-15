/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 17-10-2024.
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

@file:OptIn(ExperimentalLayoutApi::class)

package com.prime.media.personalize

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.prime.media.MainActivity
import com.prime.media.R
import com.prime.media.common.LocalSystemFacade
import com.prime.media.common.ScenicAppBar
import com.prime.media.old.common.LocalNavController
import com.primex.core.thenIf
import com.primex.material2.IconButton
import com.primex.material2.Label
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import com.prime.media.BuildConfig
import com.prime.media.common.preference
import com.prime.media.settings.Settings
import com.primex.core.plus
import com.primex.material2.appbar.LargeTopAppBar
import com.primex.material2.appbar.TopAppBarDefaults
import com.primex.material2.appbar.TopAppBarScrollBehavior
import com.zs.core.paymaster.Paymaster
import com.zs.core.paymaster.ProductInfo
import com.zs.core_ui.AppTheme
import com.zs.core_ui.ContentPadding
import com.zs.core_ui.Header
import com.zs.core_ui.LocalWindowSize
import com.zs.core_ui.None
import com.zs.core_ui.Range
import com.zs.core_ui.adaptive.HorizontalTwoPaneStrategy
import com.zs.core_ui.adaptive.SinglePaneStrategy
import com.zs.core_ui.adaptive.TwoPane
import com.zs.core_ui.adaptive.TwoPaneStrategy
import com.zs.core_ui.adaptive.contentInsets
import com.zs.core_ui.lottieAnimationPainter
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import androidx.compose.foundation.layout.PaddingValues as Padding
import com.primex.core.textResource as stringResource
import com.zs.core_ui.ContentPadding as CP


typealias ViewState = PersonalizeViewState

private val hPadding = CP.large
private val hPaddingValues = Padding(horizontal = hPadding)
private val vPadding = CP.normal
private val vPaddingValues = Padding(vertical = vPadding)
private val Padding = Padding(hPadding, vPadding)
private val HeaderPadding = Padding(CP.medium, CP.normal, CP.medium, CP.medium)

private val SecondaryPaneMaxWidth = 320.dp

private val topAppBarShape = RoundedCornerShape(8)

/**
 * Represents a Top app bar for this screen.
 *
 * Handles padding/margins based on shape to ensure proper layout.
 *
 * @param modifier [Modifier] to apply to this top app bar.
 * @param shape [Shape] of the top app bar. Defaults to `null`.
 * @param behaviour [TopAppBarScrollBehavior] for scroll behavior.
 */
@Composable
@NonRestartableComposable
private fun TopAppBar(
    modifier: Modifier = Modifier,
    insets: WindowInsets = WindowInsets.None,
    shape: Shape? = null,
    behaviour: TopAppBarScrollBehavior? = null
) {
    ScenicAppBar(
        modifier = modifier.thenIf(shape != null) {
            windowInsetsPadding(insets)
                .padding(hPaddingValues)
                .clip(shape!!)
        },
        title = {
            Label(
                text = stringResource(id = R.string.scr_personalize_title_desc),
                maxLines = 2
            )
        },
        navigationIcon = {
            val navController = LocalNavController.current
            IconButton(
                imageVector = Icons.Default.ArrowBack,
                onClick = navController::navigateUp
            )
        },
        scrollBehavior = behaviour,
        windowInsets = if (shape == null) insets else WindowInsets.None,
        style = TopAppBarDefaults.largeAppBarStyle(
            scrolledContainerColor = AppTheme.colors.background(2.dp),
            scrolledContentColor = AppTheme.colors.onBackground,
            containerColor = AppTheme.colors.background,
            maxHeight = 220.dp,
            scrolledTitleTextStyle = AppTheme.typography.headlineMedium,
            titleTextStyle = AppTheme.typography.titleMedium
        ),
        background = {
            Image(
                painter = lottieAnimationPainter(R.raw.lt_bg_baloon_in_air),
                modifier = Modifier.fillMaxSize(),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        }
    )
}

/**
 * Represents the personalize screen.
 */
@Composable
fun Personalize(viewState: ViewState) {
    // Retrieve the current window size
    val (wRange, _) = LocalWindowSize.current
    // Determine the two-pane strategy based on window width range
    // when in mobile portrait; we don't show second pane;
    val strategy = when {
        wRange < Range.Large -> SinglePaneStrategy // Use SinglePane for small screens.
        else -> HorizontalTwoPaneStrategy(0.5f) // Use horizontal layout with 50% split for large screens
    }
    // The layouts of the screen can be in only 2 modes: mobile portrait or landscape.
    // The landscape mode is the general mode; it represents screens where width > large.
    // In this mode, two panes are shown; otherwise, just one pane is shown.
    val isMobilePortrait = strategy is SinglePaneStrategy
    // Define the scroll behavior for the top app bar
    val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    // obtain the padding of BottomNavBar/NavRail
    val navBarPadding = WindowInsets.contentInsets
    // FixMe This doesn't work in preview mode
    //  Actual content
    val activity = LocalSystemFacade.current as? MainActivity ?: return
    // Obtain the data to be showed in the app.
    val data by remember {
        activity.paymaster.details.map { it.associateBy { it.id } }
    }.collectAsState(emptyMap())
    // The Actual Content
    TwoPane(
        spacing = hPadding,
        strategy = strategy,
        modifier = Modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                behaviour = topAppBarScrollBehavior,
                insets = WindowInsets.statusBars,
                shape = if (isMobilePortrait) null else topAppBarShape,
            )
        },
        primary = {
            // The selected Widget
            val selected by preference(Settings.GLANCE)
            LazyColumn(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ContentPadding.medium),
                contentPadding = Padding + WindowInsets.navigationBars.asPaddingValues(),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.contentInsets)
            ) {
                if (isMobilePortrait) {
                    item(
                        contentType = "header",
                        content = {
                            Header(
                                "Upgrades",
                                drawDivider = true,
                                color = AppTheme.colors.accent,
                                style = AppTheme.typography.bodyMedium,
                                contentPadding = HeaderPadding
                            )
                        }
                    )
                    item("upgrades", content = { Upgrades(data) })
                }
                item() {
                    Header(
                        "Widgets",
                        drawDivider = true,
                        color = AppTheme.colors.accent,
                        style = AppTheme.typography.bodyMedium,
                        contentPadding = HeaderPadding
                    )
                }
                // emit widgets
                widgets(selected, data, onRequestApply = viewState::setInAppWidget)
            }
        },
        secondary = {
            if (isMobilePortrait) return@TwoPane
            Column(
                content = {
                    Header(
                        "Upgrades",
                        drawDivider = true,
                        color = AppTheme.colors.accent,
                        style = AppTheme.typography.bodyMedium,
                        contentPadding = HeaderPadding
                    )
                    Upgrades(data)
                },
                modifier = Modifier.widthIn(max = SecondaryPaneMaxWidth).systemBarsPadding()
            )
        }
    )
}

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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Deck
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.prime.media.MainActivity
import com.prime.media.R
import com.prime.media.common.LocalSystemFacade
import com.prime.media.common.ScenicAppBar
import com.prime.media.common.collectNowPlayingAsState
import com.prime.media.common.preference
import com.prime.media.old.common.LocalNavController
import com.prime.media.settings.ColorizationStrategy
import com.prime.media.settings.Settings
import com.primex.core.plus
import com.primex.core.textResource
import com.primex.core.thenIf
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.appbar.TopAppBarDefaults
import com.primex.material2.appbar.TopAppBarScrollBehavior
import com.zs.core.playback.PlaybackController
import com.zs.core_ui.AppTheme
import com.zs.core_ui.ColorPickerDialog
import com.zs.core_ui.Header
import com.zs.core_ui.LocalWindowSize
import com.zs.core_ui.None
import com.zs.core_ui.Range
import com.zs.core_ui.ToggleButton
import com.zs.core_ui.adaptive.HorizontalTwoPaneStrategy
import com.zs.core_ui.adaptive.SinglePaneStrategy
import com.zs.core_ui.adaptive.TwoPane
import com.zs.core_ui.adaptive.contentInsets
import com.zs.core_ui.lottieAnimationPainter
import kotlinx.coroutines.flow.map
import androidx.compose.foundation.layout.PaddingValues as Padding
import com.primex.core.textResource as stringResource
import com.zs.core_ui.ContentPadding as CP


typealias ViewState = PersonalizeViewState

private val hPadding = CP.large
private val hPaddingValues = Padding(horizontal = hPadding)
private val vPadding = CP.normal
private val vPaddingValues = Padding(vertical = vPadding)
private val cPadding = Padding(hPadding, vPadding)
private val HeaderPadding = Padding(CP.medium, CP.normal, CP.medium, CP.medium)

private val ComponentSpacing = Arrangement.spacedBy(CP.medium)

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
                painter = lottieAnimationPainter(if (AppTheme.colors.isLight) R.raw.lt_bg_blur else R.raw.lt_bg_baloon_in_air),
                modifier = Modifier.fillMaxSize(),
                contentDescription = null,
                contentScale = ContentScale.Crop
            )
        }
    )
}


@Composable
private fun ColorizationStrategyRow(
    viewState: ViewState,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(start = CP.normal),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = ComponentSpacing
    ) {

        var expanded by remember { mutableStateOf(false) }
        val isLightTheme = AppTheme.colors.isLight
        ColorPickerDialog(
            expanded,
            AppTheme.colors.accent,
            onColorPicked = { pickedColor ->
                if (pickedColor.isSpecified)
                    viewState[if (isLightTheme) Settings.COLOR_ACCENT_LIGHT else Settings.COLOR_ACCENT_DARK] =
                        pickedColor
                expanded = false
            }
        )

        // Manual
        val current by preference(Settings.COLORIZATION_STRATEGY)
        ToggleButton(
            selected = current == ColorizationStrategy.Manual,
            onClick = {
                if (current == ColorizationStrategy.Manual)
                    expanded = true
                // TODO - Allow only users to use this strategy if they have paid version.
                viewState[Settings.COLORIZATION_STRATEGY] = ColorizationStrategy.Manual;
            },
            label = textResource(R.string.manual),
            icon = Icons.Default.ColorLens,
        )

        // Default
        ToggleButton(
            selected = current == ColorizationStrategy.Default,
            onClick = { viewState[Settings.COLORIZATION_STRATEGY] = ColorizationStrategy.Default },
            label = textResource(R.string.defult),
            icon = Icons.Default.Deck,
        )

        ToggleButton(
            selected = current == ColorizationStrategy.Wallpaper,
            onClick = {
                viewState[Settings.COLORIZATION_STRATEGY] = ColorizationStrategy.Wallpaper
            },
            label = textResource(R.string.dynamic),
            icon = Icons.Default.Devices,
        )

        ToggleButton(
            selected = current == ColorizationStrategy.Artwork,
            onClick = { viewState[Settings.COLORIZATION_STRATEGY] = ColorizationStrategy.Artwork },
            label = textResource(R.string.album_art),
            icon = Icons.Default.Image
        )

        Spacer(Modifier.weight(1f))
    }
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
    val navBarPadding = WindowInsets.contentInsets.asPaddingValues()
    // FixMe This doesn't work in preview mode
    //  Actual content
    val activity = LocalSystemFacade.current as? MainActivity ?: return
    // Obtain the data to be showed in the app.
    val data by remember {
        activity.paymaster.details.map { it.associateBy { it.id } }
    }.collectAsState(emptyMap())
    // The Actual Content
    val state by PlaybackController.collectNowPlayingAsState()
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
                verticalArrangement = ComponentSpacing,
                contentPadding = cPadding + WindowInsets.navigationBars.asPaddingValues(),
                modifier = Modifier
                    .fillMaxSize()
                    .windowInsetsPadding(WindowInsets.contentInsets)
            ) {
                // Tweaks
                if (isMobilePortrait) {
                    item() {
                        Header(
                            stringResource(R.string.fine_tuning),
                            drawDivider = true,
                            color = AppTheme.colors.accent,
                            style = AppTheme.typography.bodyMedium,
                            contentPadding = HeaderPadding
                        )
                    }
                    item() { Tweaks(viewState, modifier = Modifier.fillMaxWidth()) }

                    // Artwork Shapes
                    item() {
                        Header(
                            textResource(R.string.personalize_scr_artwork_style),
                            drawDivider = true,
                            color = AppTheme.colors.accent,
                            style = AppTheme.typography.bodyMedium,
                            contentPadding = HeaderPadding
                        )
                    }
                    item {
                        val selected by preference(Settings.ARTWORK_SHAPE_KEY)
                        ArtworkShapeRow(
                            state.artwork,
                            selected,
                            data,
                            onRequestApply = viewState::setArtworkShapeKey,
                            modifier = Modifier.padding(top = CP.medium)
                        )
                    }
                }

                // Widgets
                item() {
                    Header(
                        stringResource(R.string.widgets),
                        drawDivider = true,
                        color = AppTheme.colors.accent,
                        style = AppTheme.typography.bodyMedium,
                        contentPadding = HeaderPadding
                    )
                }

                // emit widgets
                widgets(state, selected, data, onRequestApply = viewState::setInAppWidget)
            }
        },
        secondary = {
            if (isMobilePortrait) return@TwoPane
            Column (
                content = {

                    Header(
                        textResource(R.string.personalize_scr_artwork_style),
                        drawDivider = true,
                        color = AppTheme.colors.accent,
                        style = AppTheme.typography.bodyMedium,
                        contentPadding = HeaderPadding
                    )
                    val selected by preference(Settings.ARTWORK_SHAPE_KEY)
                    ArtworkShapeRow(
                        state.artwork,
                        selected,
                        data,
                        onRequestApply = viewState::setArtworkShapeKey,
                        modifier = Modifier.padding(top = CP.medium)
                    )

                    Header(
                        textResource(R.string.fine_tuning),
                        drawDivider = true,
                        color = AppTheme.colors.accent,
                        style = AppTheme.typography.bodyMedium,
                        contentPadding = HeaderPadding
                    )
                    Tweaks(viewState, modifier = Modifier.fillMaxWidth())

                },
                modifier = Modifier
                    .widthIn(max = SecondaryPaneMaxWidth)
                    .systemBarsPadding()
            )
        }
    )
}

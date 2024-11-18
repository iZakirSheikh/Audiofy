/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 14-10-2024.
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

@file:Suppress("NOTHING_TO_INLINE")

package com.prime.media.settings

import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Recycling
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Straighten
import androidx.compose.material.icons.outlined.Textsms
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prime.media.BuildConfig
import com.prime.media.R
import com.prime.media.common.Banner
import com.prime.media.common.LocalSystemFacade
import com.prime.media.common.preference
import com.prime.media.old.common.LocalNavController
import com.primex.core.fadeEdge
import com.primex.core.thenIf
import com.primex.material2.Button
import com.primex.material2.DropDownPreference
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.ListTile
import com.primex.material2.Preference
import com.primex.material2.SliderPreference
import com.primex.material2.SwitchPreference
import com.primex.material2.Text
import com.primex.material2.TextButton
import com.primex.material2.appbar.LargeTopAppBar
import com.primex.material2.appbar.TopAppBarDefaults
import com.primex.material2.appbar.TopAppBarScrollBehavior
import com.zs.ads.AdSize
import com.zs.core_ui.AppTheme
import com.zs.core_ui.Colors
import com.zs.core_ui.Header
import com.zs.core_ui.LocalWindowSize
import com.zs.core_ui.NightMode
import com.zs.core_ui.None
import com.zs.core_ui.Range
import com.zs.core_ui.adaptive.HorizontalTwoPaneStrategy
import com.zs.core_ui.adaptive.SinglePaneStrategy
import com.zs.core_ui.adaptive.TwoPane
import com.zs.core_ui.adaptive.contentInsets
import androidx.compose.foundation.layout.PaddingValues as Padding
import androidx.compose.foundation.shape.RoundedCornerShape as Rounded
import androidx.compose.ui.graphics.RectangleShape as Rectangle
import com.primex.core.rememberVectorPainter as painter
import com.primex.core.textResource as stringResource
import com.zs.core_ui.ContentPadding as CP

// The max width of the secondary pane
private val sPaneMaxWidth = 300.dp

// Used to style individual items within a preference section.
private val TopTileShape = Rounded(24.dp, 24.dp, 0.dp, 0.dp)
private val CentreTileShape = Rectangle
private val BottomTileShape = Rounded(0.dp, 0.dp, 24.dp, 24.dp)
private val SingleTileShape = Rounded(24.dp)

private val Colors.tileBackgroundColor
    @ReadOnlyComposable @Composable inline get() = background(elevation = 1.dp)

// when topBar doesn't fill the screen; this is for that case.
private val RoundedTopBarShape = Rounded(15)

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
    LargeTopAppBar(
        modifier = modifier.thenIf(shape != null) {
            windowInsetsPadding(insets)
                .padding(horizontal = CP.medium)
                .clip(shape!!)
        },
        title = { Label(text = stringResource(id = R.string.settings)) },
        navigationIcon = {
            val navController = LocalNavController.current
            IconButton(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                onClick = navController::navigateUp
            )
        },
        scrollBehavior = behaviour,
        windowInsets = if (shape == null) insets else WindowInsets.None,
        style = TopAppBarDefaults.largeAppBarStyle(
            scrolledContainerColor = AppTheme.colors.background(2.dp),
            scrolledContentColor = AppTheme.colors.onBackground,
            containerColor = AppTheme.colors.background,
            contentColor = AppTheme.colors.onBackground
        ),
        actions = {
            val facade = LocalSystemFacade.current
            // Feedback
            IconButton(
                imageVector = Icons.Outlined.AlternateEmail,
                onClick = { facade.launch(Settings.FeedbackIntent) },
            )
            // Star on Github
            IconButton(
                imageVector = Icons.Outlined.DataObject,
                onClick = { facade.launch(Settings.GithubIntent) },
            )
            // Report Bugs on Github.
            IconButton(
                imageVector = Icons.Outlined.BugReport,
                onClick = { facade.launch(Settings.GitHubIssuesPage) },
            )
            // Join our telegram channel
            IconButton(
                imageVector = Icons.Outlined.Textsms,
                onClick = { facade.launch(Settings.TelegramIntent) },
            )
        }
    )
}

private val HeaderPadding = PaddingValues(horizontal = CP.large, vertical = CP.xLarge)

@Composable
private inline fun GroupHeader(
    text: CharSequence,
    modifier: Modifier = Modifier,
    paddingValues: Padding = HeaderPadding,
) {
    Text(
        text = text,
        modifier = Modifier
            .padding(paddingValues)
            .then(modifier),
        color = AppTheme.colors.accent,
        style = AppTheme.typography.titleSmall
    )
}

/**
 * Represents the general section.
 */
@Composable
private inline fun General(
    viewState: SettingsViewState
) {
    // Enable/Disable Trash Can
    val trashcan by preference(Settings.TRASH_CAN_ENABLED)
    val facade = LocalSystemFacade.current
    SwitchPreference(
        title = stringResource(R.string.pref_enable_trash_can),
        checked = trashcan,
        summery = stringResource(R.string.pref_enable_trash_can_summery),
        onCheckedChange = {
            viewState.set(Settings.TRASH_CAN_ENABLED, it)
            facade.showAd(true)
        },
        icon = Icons.Outlined.Recycling,
        modifier = Modifier.background(AppTheme.colors.tileBackgroundColor, TopTileShape),
    )

    // Legacy Artwork Method
    val legacyArtwork by preference(Settings.USE_LEGACY_ARTWORK_METHOD)
    SwitchPreference(
        title = stringResource(R.string.pref_fetch_artwork_from_media_store),
        checked = legacyArtwork,
        summery = stringResource(R.string.pref_fetch_artwork_from_media_store_summery),
        onCheckedChange = {
            viewState.set(Settings.USE_LEGACY_ARTWORK_METHOD, it)
            facade.showAd(true)
        },
        icon = Icons.Outlined.Camera,
        modifier = Modifier.background(AppTheme.colors.tileBackgroundColor, CentreTileShape),
    )

    // Exclude Track Duration
    // The duration from which below tracks are excluded from the library.
    val excludeTrackDuration by preference(Settings.MIN_TRACK_LENGTH_SECS)
    SliderPreference(
        title = stringResource(R.string.pref_minimum_track_length),
        defaultValue = excludeTrackDuration.toFloat(),
        summery = stringResource(R.string.pref_minimum_track_length_summery),
        onValueChange = { viewState.set(Settings.MIN_TRACK_LENGTH_SECS, it.toInt()) },
        valueRange = 0f..100f,
        steps = 5,
        icon = Icons.Outlined.Straighten,
        preview = {
            Text(
                text = "${excludeTrackDuration}s",
                style = AppTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .size(60.dp)
                    .wrapContentSize(Alignment.Center),
                textAlign = TextAlign.Center
            )
        },
        modifier = Modifier
            .background(AppTheme.colors.tileBackgroundColor, CentreTileShape),
    )

    // Use Inbuilt Audio FX
    // Whether to use inbuilt audio effects or inApp.
    val useInbuiltAudioFx by preference(Settings.USE_IN_BUILT_AUDIO_FX)
    SwitchPreference(
        title = stringResource(R.string.pref_use_inbuilt_audio_effects),
        checked = useInbuiltAudioFx,
        summery = stringResource(R.string.pref_use_inbuilt_audio_effects_summery),
        onCheckedChange = {
            viewState.set(Settings.USE_IN_BUILT_AUDIO_FX, it)
            facade.showAd(true)
        },
        icon = Icons.Outlined.Tune,
        modifier = Modifier.background(AppTheme.colors.tileBackgroundColor, BottomTileShape)
    )
}

private val Resources.entriesNightMode
    get() = listOf(
        "Dark" to NightMode.YES,
        "Light" to NightMode.NO,
        "Sync with System" to NightMode.FOLLOW_SYSTEM
    )

/**
 * Represents the appearance section.
 */
@Composable
private inline fun Appearance(
    viewState: SettingsViewState
) {
    // Night Mode Strategy
    // The strategy to use for night mode.
    val facade = LocalSystemFacade.current
    val nightModeStrategy by preference(Settings.NIGHT_MODE)
    DropDownPreference(
        title = stringResource(R.string.pref_app_theme),
        defaultValue = nightModeStrategy,
        icon = Icons.Default.LightMode,
        entries = LocalContext.current.resources.entriesNightMode,
        onRequestChange = {
            viewState.set(Settings.NIGHT_MODE, it)
            facade.showAd(force = true)
        },
        modifier = Modifier
            .background(AppTheme.colors.tileBackgroundColor, TopTileShape)
    )

    // Colorization Strategy
    val colorizationStrategy by preference(Settings.COLORIZATION_STRATEGY)
    SwitchPreference(
        checked = colorizationStrategy == ColorizationStrategy.Artwork,
        title = stringResource(R.string.pref_colorization_strategy),
        summery = stringResource(R.string.pref_colorization_strategy_summery),
        onCheckedChange = { should: Boolean ->
            val strategy =
                if (should) ColorizationStrategy.Artwork else ColorizationStrategy.Default
            viewState.set(Settings.COLORIZATION_STRATEGY, strategy)
            facade.showAd(force = true)
        },
        modifier = Modifier
            .background(AppTheme.colors.tileBackgroundColor, CentreTileShape)
    )

    // App font scale
    // The font scale to use for the app if -1 is used, the system font scale is used.
    val scale by preference(Settings.FONT_SCALE)
    SliderPreference(
        defaultValue = scale,
        title = stringResource(R.string.pref_font_scale),
        summery = stringResource(R.string.pref_font_scale_summery),
        valueRange = 0.7f..2f,
        // (2.0 - 0.7) / 0.1 = 13 steps
        steps = 13,
        icon = Icons.Outlined.FormatSize,
        preview = {
            Label(
                text = if (scale == -1f) "System" else "%.1fx".format(scale),
                fontWeight = FontWeight.Bold
            )
        },
        onValueChange = { value: Float ->
            val newValue = if (value < 0.76f) -1f else value
            viewState.set(Settings.FONT_SCALE, newValue)
            facade.showAd(force = true)
        },
        modifier = Modifier
            .background(AppTheme.colors.tileBackgroundColor, CentreTileShape)
    )

    // Grid Item Multiplier
    // The multiplier increases/decreases the size of the grid item from 0.6 to 2f
    val gridItemSizeMultiplier by preference(Settings.GRID_ITEM_SIZE_MULTIPLIER)
    SliderPreference(
        defaultValue = gridItemSizeMultiplier,
        title = stringResource(R.string.pref_grid_item_size_multiplier),
        summery = stringResource(R.string.pref_grid_item_size_multiplier_summery),
        valueRange = 0.6f..2f,
        steps = 14, // (2.0 - 0.7) / 0.1 = 13 steps
        icon = Icons.Outlined.Dashboard,
        preview = {
            Label(
                text = "%.1fx".format(gridItemSizeMultiplier),
                fontWeight = FontWeight.Bold
            )
        },
        onValueChange = { value: Float ->
            viewState.set(Settings.GRID_ITEM_SIZE_MULTIPLIER, value)
            facade.showAd(force = true)
        },
        modifier = Modifier
            .background(AppTheme.colors.tileBackgroundColor, CentreTileShape)
    )

    // Translucent System Bars
    // Whether System Bars are rendered as translucent or Transparent.
    val translucentSystemBars by preference(Settings.TRANSLUCENT_SYSTEM_BARS)
    SwitchPreference(
        checked = translucentSystemBars,
        title = stringResource(R.string.pref_translucent_system_bars),
        summery = stringResource(R.string.pref_translucent_system_bars_summery),
        onCheckedChange = { should: Boolean ->
            viewState.set(Settings.TRANSLUCENT_SYSTEM_BARS, should)
            viewState.set(Settings.TRANSLUCENT_SYSTEM_BARS, should)
            facade.showAd(force = true)
        },
        modifier = Modifier
            .background(AppTheme.colors.tileBackgroundColor, CentreTileShape)
    )

    // Hide/Show SystemBars for Immersive View
    // Whether System Bars are hidden for immersive view or not.
    val immersiveView by preference(Settings.IMMERSIVE_VIEW)
    SwitchPreference(
        checked = immersiveView,
        title = stringResource(R.string.pref_immersive_view),
        summery = stringResource(R.string.pref_immersive_view_summery),
        onCheckedChange = { should: Boolean ->
            viewState.set(Settings.IMMERSIVE_VIEW, should)
            facade.showAd(force = true)
        },
        modifier = Modifier
            .background(AppTheme.colors.tileBackgroundColor, BottomTileShape)
    )
}

@Composable
private inline fun AboutUs() {
    // The app version and check for updates.
    val facade = LocalSystemFacade.current
    ListTile(
        headline = { Label("Audiofy Version", fontWeight = FontWeight.Bold) },
        subtitle = { Label("Version: ${BuildConfig.VERSION_NAME}") },
        footer = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CP.medium)
            ) {
                TextButton("Update Audiofy", onClick = { facade.initiateUpdateFlow(true) })
                TextButton("Join the Beta", onClick = { facade.launch(Settings.JoinBetaIntent) })
            }
        },
        leading = { Icon(imageVector = Icons.Outlined.NewReleases, contentDescription = null) },
    )

    // Privacy Policy
    Preference(
        title = stringResource(R.string.pref_privacy_policy),
        summery = stringResource(R.string.pref_privacy_policy_summery),
        icon = Icons.Outlined.PrivacyTip,
        modifier = Modifier
            .clip(AppTheme.shapes.medium)
            .clickable { facade.launch(Settings.PrivacyPolicyIntent) },
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(CP.medium)
    ) {
        Button(
            label = "Rate App",
            icon = painter(Icons.Outlined.Star),
            onClick = facade::launchAppStore,
            colors = ButtonDefaults.buttonColors(
                backgroundColor = AppTheme.colors.background(2.dp),
                contentColor = AppTheme.colors.accent
            ),
            elevation = null,
            shape = AppTheme.shapes.small
        )

        Button(
            label = "Share with Friends",
            icon = painter(Icons.Outlined.Share),
            onClick = { facade.launch(Settings.ShareAppIntent) },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = AppTheme.colors.background(2.dp),
                contentColor = AppTheme.colors.accent
            ),
            elevation = null,
            shape = AppTheme.shapes.small
        )
    }
}

/**
 * Represents the Settings screen.
 */
@Composable
fun Settings(viewState: SettingsViewState) {
    // Retrieve the current window size
    val (width, _) = LocalWindowSize.current
    // Determine the two-pane strategy based on window width range
    // when in mobile portrait; we don't show second pane;
    val strategy = when {
        width < Range.Medium -> SinglePaneStrategy // Use stacked layout with bias to centre for small screens
        else -> HorizontalTwoPaneStrategy(0.5f) // Use horizontal layout with 50% split for large screens
    }
    // Layout Modes:
    // When the width exceeds the "Compact" threshold, the layout is no longer immersive.
    // This is because a navigation rail is likely displayed, requiring content to be
    // indented rather than filling the entire screen width.
    //
    // The threshold helps to dynamically adjust the UI for different device form factors
    // and orientations, ensuring appropriate use of space. In non-compact layouts,
    // elements like the navigation rail or side panels prevent an immersive, full-width
    // layout, making the design more suitable for larger screens.
    val immersive = width < Range.Medium
    // Define the scroll behavior for the top app bar
    val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    // obtain the padding of BottomNavBar/NavRail
    val navBarPadding = WindowInsets.contentInsets
    // Place the content
    // FIXME: Width < 650dp then screen is single pane what if navigationBars are at end.
    TwoPane(
        spacing = CP.normal,
        strategy = strategy,
        topBar = {
            TopAppBar(
                behaviour = topAppBarScrollBehavior,
                insets = WindowInsets.statusBars,
                shape = if (immersive) null else RoundedTopBarShape,
            )
        },
        secondary = {
            // this will not be called when in single pane mode
            // this is just for decoration
            if (strategy is SinglePaneStrategy) return@TwoPane
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(top = CP.medium)
                    .widthIn(max = sPaneMaxWidth)
                    .systemBarsPadding()
                    .padding(navBarPadding),
                content = {
                    Header(
                        stringResource(R.string.about_us),
                        color = AppTheme.colors.accent,
                        drawDivider = true,
                        style = AppTheme.typography.titleSmall,
                        contentPadding = PaddingValues(vertical = CP.normal, horizontal = CP.medium)
                    )
                    AboutUs()
                }
            )
        },
        primary = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                    // padding due to app bar
                    .padding(WindowInsets.contentInsets)
                    .fadeEdge(AppTheme.colors.background(2.dp), scrollState, false)
                    .verticalScroll(scrollState)
                    // the padding due to nav_bar
                    .padding(navBarPadding)
                    .thenIf(immersive) { navigationBarsPadding() }
                    // In immersive mode, add horizontal padding to prevent settings from touching the screen edges.
                    // Immersive layouts typically have a bottom app bar, so extra padding improves aesthetics.
                    // Non-immersive layouts only need vertical padding.
                    .padding(vertical = CP.normal, horizontal = if (immersive) CP.large else CP.medium),
                content = {
                    GroupHeader(
                        text = stringResource(id = R.string.general),
                        paddingValues = PaddingValues(CP.normal, CP.small, CP.normal, CP.xLarge)
                    )
                    General(viewState = viewState)
                    // BannerAd
                    // Load a banner ad if the app is not in AdFree mode.
                    val facade = LocalSystemFacade.current
                    if (!facade.isAdFree)
                        Banner(
                            modifier = Modifier
                                .padding(vertical = CP.normal)
                                .align(Alignment.CenterHorizontally),
                            AdSize.LARGE_BANNER,
                            key = "Banner2"
                        )
                    GroupHeader(text = stringResource(id = R.string.appearance))
                    Appearance(viewState = viewState)
                    // AboutUs
                    // Load AboutUs here if this is mobile port
                    if (strategy !is SinglePaneStrategy)
                        return@TwoPane
                    Header(
                        stringResource(R.string.about_us),
                        color = AppTheme.colors.accent,
                        drawDivider = true,
                        style = AppTheme.typography.titleSmall,
                        contentPadding = PaddingValues(vertical = CP.normal, horizontal = CP.medium)
                    )
                    AboutUs()
                }
            )
        },
    )
}






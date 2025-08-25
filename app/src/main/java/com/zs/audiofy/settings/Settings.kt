@file:Suppress("NOTHING_TO_INLINE")

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

package com.zs.audiofy.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Camera
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.RateReview
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zs.audiofy.BuildConfig
import com.zs.audiofy.R
import com.zs.audiofy.common.ColorizationStrategy
import com.zs.audiofy.common.IAP_BUY_ME_COFFEE
import com.zs.audiofy.common.NightMode
import com.zs.audiofy.common.compose.FloatingLargeTopAppBar
import com.zs.audiofy.common.compose.LocalSystemFacade
import com.zs.audiofy.common.compose.background
import com.zs.audiofy.common.compose.fadingEdge2
import com.zs.audiofy.common.compose.preference
import com.zs.audiofy.common.compose.rememberAcrylicSurface
import com.zs.audiofy.common.compose.source
import com.zs.compose.foundation.plus
import com.zs.compose.foundation.textArrayResource
import com.zs.compose.foundation.textResource
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.BaseListItem
import com.zs.compose.theme.Button
import com.zs.compose.theme.ButtonDefaults
import com.zs.compose.theme.Chip
import com.zs.compose.theme.ChipDefaults
import com.zs.compose.theme.Colors
import com.zs.compose.theme.DropDownPreference
import com.zs.compose.theme.FilledTonalButton
import com.zs.compose.theme.Icon
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.LocalWindowSize
import com.zs.compose.theme.Preference
import com.zs.compose.theme.SliderPreference
import com.zs.compose.theme.Surface
import com.zs.compose.theme.SwitchPreference
import com.zs.compose.theme.TextButton
import com.zs.compose.theme.WindowSize.Category
import com.zs.compose.theme.adaptive.HorizontalTwoPaneStrategy
import com.zs.compose.theme.adaptive.SinglePaneStrategy
import com.zs.compose.theme.adaptive.TwoPane
import com.zs.compose.theme.adaptive.content
import com.zs.compose.theme.appbar.AppBarDefaults
import com.zs.compose.theme.minimumInteractiveComponentSize
import com.zs.compose.theme.text.Header
import com.zs.compose.theme.text.Label
import com.zs.compose.theme.text.Text
import com.zs.core.billing.Paymaster
import kotlin.math.roundToInt
import androidx.compose.foundation.layout.PaddingValues as Padding
import com.zs.audiofy.common.compose.ContentPadding as CP

private const val TAG = "Settings"

// The max width of the secondary pane
private val sPaneMaxWidth = 280.dp

// Used to style individual items within a preference section.
private val TopTileShape = RoundedCornerShape(24.dp, 24.dp, 0.dp, 0.dp)
private val CentreTileShape = RectangleShape
private val BottomTileShape = RoundedCornerShape(0.dp, 0.dp, 24.dp, 24.dp)
private val SingleTileShape = RoundedCornerShape(24.dp)

private val Colors.tileBackgroundColor
    @ReadOnlyComposable @Composable inline get() = background(elevation = 1.dp)

/**
 * Represents the group header of [Preference]s
 */
@Composable
@NonRestartableComposable
private fun GroupHeader(
    text: CharSequence,
    modifier: Modifier = Modifier,
    padding: Padding? = null,
) = Text(
    text = text,
    modifier = Modifier
        .let() {
            if (padding == null)
                it.padding(horizontal = CP.large, vertical = CP.xLarge)
            else
                it.padding(padding)
        }
        .then(modifier),
    color = AppTheme.colors.accent,
    style = AppTheme.typography.title3
)

private const val CONTENT_TYPE_HEADER = "header"
private const val CONTENT_TYPE_PREF = "preference"

/** Represents the settings of General */
private inline fun LazyListScope.General(
    viewState: SettingsViewState,
) {
    // Recycle Bin
    item(contentType = CONTENT_TYPE_PREF) {
        val enabled by preference(Settings.TRASH_CAN_ENABLED)
        SwitchPreference(
            text = textResource(R.string.pref_enable_trash_can),
            checked = enabled,
            onCheckedChange = { viewState.set(Settings.TRASH_CAN_ENABLED, it) },
            icon = Icons.Outlined.Recycling,
            modifier = Modifier.background(AppTheme.colors.tileBackgroundColor, TopTileShape),
        )
    }

    // Legacy Artwork Method
    item(contentType = CONTENT_TYPE_PREF) {

        val enabled by preference(Settings.USE_LEGACY_ARTWORK_METHOD)
        SwitchPreference(
            text = textResource(R.string.pref_fetch_artwork_from_media_store),
            checked = enabled,
            onCheckedChange = { viewState.set(Settings.USE_LEGACY_ARTWORK_METHOD, it) },
            icon = Icons.Outlined.Camera,
            modifier = Modifier.background(AppTheme.colors.tileBackgroundColor, CentreTileShape),
        )

    }

    // Exclude Track Duration
    // The duration from which below tracks are excluded from the library.
    item(contentType = CONTENT_TYPE_PREF) {
        val exclude by preference(Settings.MIN_TRACK_LENGTH_SECS)
        SliderPreference(
            text = textResource(R.string.pref_minimum_track_length),
            value = exclude.toFloat(),
            onRequestChange = { viewState.set(Settings.MIN_TRACK_LENGTH_SECS, it.toInt()) },
            valueRange = 0f..100f,
            steps = 5,
            icon = Icons.Outlined.Straighten,
            preview = {
                Label(
                    text = textResource(R.string.postfix_s_d, it.roundToInt()),
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .wrapContentSize(Alignment.Center)
                )
            },
            modifier = Modifier.background(AppTheme.colors.tileBackgroundColor, CentreTileShape),
        )
    }

    // Use Inbuilt Audio FX
    // Whether to use inbuilt audio effects or inApp.
    item(contentType = CONTENT_TYPE_PREF) {
        val enabled by preference(Settings.USE_IN_BUILT_AUDIO_FX)
        SwitchPreference(
            text = textResource(R.string.pref_use_inbuilt_audio_effects),
            checked = enabled,
            onCheckedChange = { viewState.set(Settings.USE_IN_BUILT_AUDIO_FX, it) },
            icon = Icons.Outlined.Tune,
            modifier = Modifier.background(AppTheme.colors.tileBackgroundColor, BottomTileShape)
        )
    }
}

/** Represents items that are related to appearance of the App. */
private inline fun LazyListScope.Appearence(
    viewState: SettingsViewState,
) {
    // Night Mode Strategy
    // The strategy to use for night mode.
    item(contentType = CONTENT_TYPE_PREF) {
        val strategy by preference(Settings.NIGHT_MODE)
        val entries = textArrayResource(R.array.pref_night_mode_entries)
        DropDownPreference(
            text = textResource(R.string.pref_app_theme_s, entries[strategy.ordinal]),
            value = strategy,
            icon = Icons.Default.LightMode,
            entries = entries,
            onRequestChange = { viewState.set(Settings.NIGHT_MODE, it) },
            values = NightMode.values(),
            modifier = Modifier.background(AppTheme.colors.tileBackgroundColor, TopTileShape)
        )
    }

    item(contentType = CONTENT_TYPE_PREF) {
        val use by preference(Settings.USE_ACCENT_IN_NAV_BAR)
        SwitchPreference(
            checked = use,
            text = textResource(R.string.pref_accent_nav),
            onCheckedChange = { should: Boolean ->
                viewState.set(Settings.USE_ACCENT_IN_NAV_BAR, should)
            },
            modifier = Modifier.background(AppTheme.colors.tileBackgroundColor, CentreTileShape)
        )
    }

    // Colorization Strategy
    item(contentType = CONTENT_TYPE_PREF) {
        val colorizationStrategy by preference(Settings.COLORIZATION_STRATEGY)
        SwitchPreference(
            checked = colorizationStrategy == ColorizationStrategy.Artwork,
            text = textResource(R.string.pref_colorization_strategy),
            onCheckedChange = { should: Boolean ->
                val strategy =
                    if (should) ColorizationStrategy.Wallpaper else ColorizationStrategy.Default
                viewState.set(Settings.COLORIZATION_STRATEGY, strategy)
            },
            modifier = Modifier.background(AppTheme.colors.tileBackgroundColor, CentreTileShape)
        )
    }

    // App font scale
    // The font scale to use for the app if -1 is used, the system font scale is used.
    item(contentType = CONTENT_TYPE_PREF) {
        val scale by preference(Settings.FONT_SCALE)
        SliderPreference(
            value = scale,
            text = textResource(R.string.pref_font_scale),
            valueRange = 0.7f..2f,
            steps = 13,   // (2.0 - 0.7) / 0.1 = 13 steps
            icon = Icons.Outlined.FormatSize,
            preview = {
                Label(
                    text = if (it < 0.76f) textResource(R.string.system) else textResource(
                        R.string.postfix_x_f,
                        it
                    ),
                    fontWeight = FontWeight.Bold
                )
            },
            onRequestChange = { value: Float ->
                val newValue = if (value < 0.76f) -1f else value
                viewState.set(Settings.FONT_SCALE, newValue)
            },
            modifier = Modifier.background(AppTheme.colors.tileBackgroundColor, CentreTileShape)
        )
    }

    // Grid Item Multiplier
    // The multiplier increases/decreases the size of the grid item from 0.6 to 2f
    item(contentType = CONTENT_TYPE_PREF) {
        val gridItemSizeMultiplier by preference(Settings.GRID_ITEM_SIZE_MULTIPLIER)
        SliderPreference(
            value = gridItemSizeMultiplier,
            text = textResource(R.string.pref_grid_item_size_multiplier),
            valueRange = 0.6f..2f,
            steps = 14, // (2.0 - 0.7) / 0.1 = 13 steps
            icon = Icons.Outlined.Dashboard,
            preview = {
                Label(
                    text = textResource(R.string.postfix_x_f, it),
                    fontWeight = FontWeight.Bold
                )
            },
            onRequestChange = { value: Float ->
                viewState.set(Settings.GRID_ITEM_SIZE_MULTIPLIER, value)
            },
            modifier = Modifier.background(AppTheme.colors.tileBackgroundColor, CentreTileShape)
        )

    }

    // Translucent System Bars
    // Whether System Bars are rendered as translucent or Transparent.
    item(contentType = CONTENT_TYPE_PREF) {
        val translucentSystemBars by preference(Settings.TRANSPARENT_SYSTEM_BARS)
        SwitchPreference(
            checked = translucentSystemBars,
            text = textResource(R.string.pref_translucent_system_bars),
            onCheckedChange = { should: Boolean ->
                viewState.set(Settings.TRANSPARENT_SYSTEM_BARS, should)
            },
            modifier = Modifier.background(AppTheme.colors.tileBackgroundColor, CentreTileShape)
        )

    }

    // Hide/Show SystemBars for Immersive View
    // Whether System Bars are hidden for immersive view or not.
    item(contentType = CONTENT_TYPE_PREF) {
        val immersiveView by preference(Settings.IMMERSIVE_VIEW)
        SwitchPreference(
            checked = immersiveView,
            text = textResource(R.string.pref_immersive_view),
            onCheckedChange = { should: Boolean ->
                viewState.set(Settings.IMMERSIVE_VIEW, should)
            },
            modifier = Modifier
                .background(AppTheme.colors.tileBackgroundColor, BottomTileShape)
        )
    }
}

@Composable
private fun Sponsor(modifier: Modifier = Modifier) {
    BaseListItem(
        modifier = modifier
            .offset(y = -CP.normal)
            .background(AppTheme.colors.tileBackgroundColor, SingleTileShape),
        centerAlign = true,
        contentColor = AppTheme.colors.onBackground,
        // App name.
        overline = {
            Text(
                text = textResource(R.string.app_name),
                style = AppTheme.typography.display3,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.DancingScriptFontFamily,
                color = AppTheme.colors.onBackground
            )
        },
        // Build version info.
        heading = {
            Text(
                text = textResource(R.string.version_info_s, BuildConfig.VERSION_NAME),
                style = AppTheme.typography.label3,
                fontWeight = FontWeight.Normal
            )
        },
        // app icon
        leading = {
            Surface(
                color = AppTheme.colors.background(4.dp),
                shape = AppTheme.shapes.large,
                modifier = Modifier.size(64.dp),
                content = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        tint = Color.Unspecified
                    )
                }
            )
        },
        // RateUs + Buy me a Coffee Button.
        footer = {
            Row(
                modifier = Modifier.padding(top = CP.normal),
                horizontalArrangement = Arrangement.spacedBy(CP.normal),
                verticalAlignment = Alignment.CenterVertically,
                content = {
                    val facade = LocalSystemFacade.current

                    // RateUs
                    FilledTonalButton(
                        textResource(R.string.rate_us),
                        icon = Icons.Outlined.RateReview,
                        onClick = facade::launchAppStore,
                        colors = ButtonDefaults.filledTonalButtonColors(
                            backgroundColor = AppTheme.colors.background(
                                4.dp
                            )
                        )
                    )

                    // Coffee
                    Button(
                        "Buy me a coffee",
                        icon = Icons.Outlined.DataObject,
                        onClick = { facade.initiatePurchaseFlow(Paymaster.IAP_BUY_ME_COFFEE) },
                    )
                }
            )
        }
    )
}

@Composable
@NonRestartableComposable
private fun ColumnScope.AboutUs() {
    // The app version and check for updates.
    val facade = LocalSystemFacade.current
    BaseListItem(
        heading = { Label(textResource(R.string.version), fontWeight = FontWeight.Bold) },
        subheading = {
            Label(
                textResource(R.string.version_info_s, BuildConfig.VERSION_NAME)
            )
        },
        footer = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(CP.medium),
                content = {
                    TextButton(
                        textResource(R.string.update_audiofy),
                        onClick = { facade.initiateUpdateFlow(true) })
                    TextButton(
                        textResource(R.string.join_the_beta),
                        onClick = { facade.launch(Settings.JoinBetaIntent) },
                        enabled = false
                    )
                }
            )
        },
        leading = {
            Icon(
                imageVector = Icons.Outlined.NewReleases,
                contentDescription = null
            )
        },
    )

    // Privacy Policy
    Preference(
        text = textResource(R.string.pref_privacy_policy),
        icon = Icons.Outlined.PrivacyTip,
        modifier = Modifier
            .clip(AppTheme.shapes.medium)
            .clickable { facade.launch(Settings.PrivacyPolicyIntent) },
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(CP.medium)
    ) {
        val colors = ChipDefaults.chipColors(
            backgroundColor = AppTheme.colors.background(1.dp),
            contentColor = AppTheme.colors.accent
        )
        Chip(
            content = { Label(textResource(R.string.rate_us)) },
            leadingIcon = { Icon(Icons.Outlined.Star, null) },
            onClick = facade::launchAppStore,
            colors = colors,
            shape = AppTheme.shapes.xSmall
        )

        Chip(
            content = { Label(textResource(R.string.share_app_label)) },
            leadingIcon = { Icon(Icons.Outlined.Share, null) },
            onClick = { facade.launch(Settings.ShareAppIntent) },
            colors = colors,
            shape = AppTheme.shapes.xSmall
        )
    }
}

/**
 * Represents the settings screen.
 */
@Composable
fun Settings(viewState: SettingsViewState) {
    // Retrieve the current window size
    val (width, _) = LocalWindowSize.current
    // Determine the two-pane strategy based on window width range
    // when in mobile portrait; we don't show second pane;
    val strategy = when {
        // TODO  -Replace with OnePane Strategy when updating TwoPane Layout.
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
        spacing = CP.normal,
        strategy = strategy,
        topBar = {
            FloatingLargeTopAppBar(
                title = { Text(
                    textResource(R.string.scr_settings_title),
                    maxLines = 2,
                    fontWeight = FontWeight.Light,
                    lineHeight = 24.sp,
                    overflow = TextOverflow.Ellipsis
                ) },
                scrollBehavior = topAppBarScrollBehavior,
                background = colors.background(surface),
                insets = WindowInsets.systemBars.only(WindowInsetsSides.Top),
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
                    .padding(top = CP.medium)
                    .widthIn(max = sPaneMaxWidth)
                    .windowInsetsPadding(
                        WindowInsets.systemBars.union(inAppNavBarInsets).only(
                            WindowInsetsSides.Vertical + WindowInsetsSides.End
                        )
                    ),
                content = {
                    Header(
                        stringResource(R.string.about_us),
                        color = AppTheme.colors.accent,
                        // drawDivider = true,
                        style = AppTheme.typography.title3,
                        contentPadding = Padding(
                            vertical = CP.normal,
                            horizontal = CP.medium
                        )
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
                contentPadding = Padding(
                    horizontal = if (strategy is SinglePaneStrategy) CP.large else CP.medium, CP.normal
                ) + (WindowInsets.content.union(WindowInsets.systemBars)
                    .union(inAppNavBarInsets).only(
                        WindowInsetsSides.Vertical
                    )).asPaddingValues(),
                modifier = Modifier
                    .source(surface)
                    .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                    .fadingEdge2(length = 56.dp),
                content = {
                    //Sponsor
                    item(contentType = "sponsor") {
                        Sponsor()
                    }

                    // General
                    item(contentType = CONTENT_TYPE_HEADER) {
                        GroupHeader(
                            text = stringResource(id = R.string.general),
                            padding = Padding(CP.normal, CP.small, CP.normal, CP.xLarge)
                        )
                    }
                    General(viewState)

                    // Appearance
                    item(CONTENT_TYPE_HEADER) { GroupHeader(text = stringResource(id = R.string.appearance)) }
                    Appearence(viewState = viewState)

                    // AboutUs
                    // Load AboutUs here if this is mobile port
                    if (strategy !is SinglePaneStrategy)
                        return@LazyColumn

                    item(contentType = CONTENT_TYPE_HEADER) {
                        Header(
                            stringResource(R.string.about_us),
                            color = AppTheme.colors.accent,
                            //drawDivider = true,
                            style = AppTheme.typography.title3,
                            contentPadding = Padding(
                                vertical = CP.normal,
                                horizontal = CP.medium
                            )
                        )
                    }

                    item(contentType = "about_us") {
                        Column { AboutUs() }
                    }
                }
            )
        }
    )
}
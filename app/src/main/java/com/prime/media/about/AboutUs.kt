@file:OptIn(ExperimentalSharedTransitionApi::class, ExperimentalMaterialApi::class)

/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 21-06-2024.
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

package com.prime.media.about

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReplyAll
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Textsms
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prime.media.BuildConfig
import com.prime.media.R
import com.prime.media.common.LocalSystemFacade
import com.prime.media.common.Route
import com.prime.media.old.common.LocalNavController
import com.prime.media.settings.DancingScriptFontFamily
import com.prime.media.settings.Settings
import com.primex.core.fadingEdge
import com.primex.core.shapes.SquircleShape
import com.primex.core.textResource
import com.primex.core.thenIf
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.ListTile
import com.primex.material2.Text
import com.primex.material2.appbar.LargeTopAppBar
import com.primex.material2.appbar.TopAppBarDefaults
import com.primex.material2.appbar.TopAppBarScrollBehavior
import com.zs.core_ui.AppTheme
import com.zs.core_ui.Header
import com.zs.core_ui.LocalWindowSize
import com.zs.core_ui.None
import com.zs.core_ui.Range
import com.zs.core_ui.adaptive.HorizontalTwoPaneStrategy
import com.zs.core_ui.adaptive.SinglePaneStrategy
import com.zs.core_ui.adaptive.TwoPane
import com.zs.core_ui.adaptive.contentInsets
import androidx.compose.foundation.layout.PaddingValues as Padding
import com.zs.core_ui.ContentPadding as CP

private const val TAG = "AboutUs"

object RouteAboutUs : Route

private val hPadding = CP.large
private val hPaddingValues = Padding(horizontal = hPadding)
private val vPadding = CP.normal
private val vPaddingValues = Padding(vertical = vPadding)
private val DefaultPaddingValues = Padding(hPadding, vPadding)
private val HeaderPadding = Padding(CP.medium, CP.normal, CP.medium, CP.medium)

private val AvatarShape = SquircleShape(0.7f)
private val DefaultComponentShape = RoundedCornerShape(8)

private val SecondaryPaneMaxWidth = 340.dp

@Suppress("NOTHING_TO_INLINE")
@Composable
private inline fun Info(
    text: CharSequence,
    modifier: Modifier = Modifier
) = Text(
    text,
    modifier = modifier
        .background(AppTheme.colors.background(1.dp), DefaultComponentShape)
        .padding(horizontal = hPadding, vertical = vPadding),
    style = AppTheme.typography.bodyMedium,
)

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
                .padding(hPaddingValues)
                .clip(shape!!)
        },
        title = { Label(text = textResource(id = R.string.about_us)) },
        navigationIcon = {
            val navController = LocalNavController.current
            IconButton(
                imageVector = Icons.AutoMirrored.Outlined.ReplyAll,
                onClick = navController::navigateUp
            )
        },
        scrollBehavior = behaviour,
        windowInsets = if (shape == null) insets else WindowInsets.None,
        style = TopAppBarDefaults.largeAppBarStyle(
            scrolledContainerColor = AppTheme.colors.background(2.dp),
            scrolledContentColor = AppTheme.colors.onBackground,
            containerColor = AppTheme.colors.background(0.1.dp)
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

@Composable
private fun GetToKnowUs(
    modifier: Modifier = Modifier,
) = ListTile(
    modifier = modifier,
    centerAlign = true,
    padding = AppTheme.emptyPadding,
    // App version info.
    headline = {
        Text(
            text = textResource(
                R.string.pref_get_to_know_us_subttile_s,
                BuildConfig.VERSION_NAME
            ),
            style = AppTheme.typography.caption,
        )
    },
    // Build version info.
    overline = {
        Text(
            text = textResource(id = R.string.app_name),
            style = AppTheme.typography.displaySmall,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.DancingScriptFontFamily
        )
    },
    leading = {
        Surface(
            color = AppTheme.colors.background(2.dp),
            shape = AvatarShape,
            modifier = Modifier.size(64.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
            )
        }
    },
    footer = {
        Row(
            modifier = Modifier.padding(top = CP.large),
            horizontalArrangement = Arrangement.spacedBy(CP.normal),
            verticalAlignment = Alignment.CenterVertically,
            content = {
                val facade = LocalSystemFacade.current
                val buttonModifier = Modifier
                    .weight(1f)
                val buttonShape = RoundedCornerShape(20)
                val padding = Padding(horizontal = 8.dp, vertical = 8.dp)

                // Donate
                Button(
                    onClick = { facade.initiatePurchaseFlow(BuildConfig.IAP_BUY_ME_COFFEE) },
                    modifier = buttonModifier,
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = AppTheme.colors.background(1.dp)
                    ),
                    shape = buttonShape,
                    contentPadding = padding,
                    elevation = null,
                    content = {
                        IconButton(
                            imageVector = Icons.Outlined.Info,
                            onClick = {
                                facade.showToast(
                                    message = R.string.msg_library_buy_me_a_coffee,
                                    icon = Icons.Outlined.Coffee
                                )
                            }
                        )
                        Label(text = textResource(R.string.sponsor))
                        Spacer(modifier = Modifier.weight(1f))
                    }
                )

                androidx.compose.material.OutlinedButton(
                    onClick = facade::launchAppStore,
                    modifier = buttonModifier,
                    colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent),
                    shape = buttonShape,
                    elevation = null,
                    contentPadding = padding,
                    content = {
                        IconButton(
                            imageVector = Icons.Outlined.Lightbulb,
                            onClick = {
                                facade.showToast(
                                    message = R.string.msg_library_rate_us,
                                    icon = Icons.Outlined.Coffee
                                )
                            }
                        )
                        Label(
                            text = textResource(id = R.string.rate_us),
                            style = AppTheme.typography.bodyMedium
                        )
                        Spacer(modifier = Modifier.weight(1f))
                    }
                )
            }
        )
    }
)

/**
 * Represents the AboutUs Screen.
 */
@Composable
fun AboutUs() {
    // Retrieve the current window size
    val (wRange, _) = LocalWindowSize.current
    // Determine the two-pane strategy based on window width range
    // when in mobile portrait; we don't show second pane;
    val strategy = when {
        wRange < Range.Large -> SinglePaneStrategy // Use stacked layout with bias to centre for small screens
        else -> HorizontalTwoPaneStrategy(0.5f) // Use horizontal layout with 50% split for large screens
    }
    // The layouts of the screen can be in only 2 modes: mobile portrait or landscape.
    // The landscape mode is the general mode; it represents screens where width > large.
    // In this mode, two panes are shown; otherwise, just one pane is shown.
    val isMobilePortrait = strategy is SinglePaneStrategy
    // Define the scroll behavior for the top app bar
    val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    TwoPane(
        spacing = hPadding,
        strategy = strategy,
        modifier = Modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
        topBar = {
            TopAppBar(
                behaviour = topAppBarScrollBehavior,
                insets = WindowInsets.statusBars,
                shape = if (isMobilePortrait) null else DefaultComponentShape,
            )
        },
        secondary = {
            // in mobile port mode we don't show details pane.
            if (isMobilePortrait) return@TwoPane
            Column(
                modifier = Modifier
                    .padding(top = CP.medium)
                    .widthIn(max = SecondaryPaneMaxWidth)
                    .systemBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(CP.normal),
                content = {
                    GetToKnowUs()
                    MyApps()
                }
            )
        },
        primary = {
            val scrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets.contentInsets)
                    .fadingEdge(scrollState, false)
                    .verticalScroll(scrollState)
                    .thenIf(isMobilePortrait) { navigationBarsPadding() }
                    .padding(DefaultPaddingValues),
                verticalArrangement = Arrangement.spacedBy(CP.medium),
                content = {
                    if (isMobilePortrait) {
                        GetToKnowUs()
                        MyApps()
                    }
                    Header(
                        textResource(R.string.what_s_new),
                        drawDivider = true,
                        color = AppTheme.colors.accent,
                        style = AppTheme.typography.bodyMedium,
                        contentPadding = HeaderPadding
                    )

                    Info(textResource(R.string.what_s_new_latest),)

                    Header(
                        textResource(R.string.about_us),
                        drawDivider = true,
                        color = AppTheme.colors.accent,
                        style = AppTheme.typography.bodyMedium,
                        contentPadding = HeaderPadding
                    )
                    Info(textResource(R.string.pref_about_us_desc),)
                }
            )
        }
    )
}





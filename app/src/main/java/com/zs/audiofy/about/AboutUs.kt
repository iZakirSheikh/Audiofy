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

package com.zs.audiofy.about

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReplyAll
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.material.icons.outlined.Textsms
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.zs.audiofy.BuildConfig
import com.zs.audiofy.R
import com.zs.audiofy.common.IAP_BUY_ME_COFFEE
import com.zs.audiofy.common.Route
import com.zs.audiofy.common.compose.FloatingLargeTopAppBar
import com.zs.audiofy.common.compose.LocalNavController
import com.zs.audiofy.common.compose.LocalSystemFacade
import com.zs.audiofy.common.compose.background
import com.zs.audiofy.common.compose.fadingEdge2
import com.zs.audiofy.common.compose.rememberAcrylicSurface
import com.zs.audiofy.common.compose.source
import com.zs.audiofy.settings.DancingScriptFontFamily
import com.zs.audiofy.settings.Settings
import com.zs.compose.foundation.plus
import com.zs.compose.foundation.textArrayResource
import com.zs.compose.foundation.textResource
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.BaseListItem
import com.zs.compose.theme.Button
import com.zs.compose.theme.ButtonDefaults
import com.zs.compose.theme.FilledTonalButton
import com.zs.compose.theme.Icon
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.LocalWindowSize
import com.zs.compose.theme.Surface
import com.zs.compose.theme.WindowSize.Category
import com.zs.compose.theme.adaptive.HorizontalTwoPaneStrategy
import com.zs.compose.theme.adaptive.SinglePaneStrategy
import com.zs.compose.theme.adaptive.TwoPane
import com.zs.compose.theme.adaptive.content
import com.zs.compose.theme.appbar.AppBarDefaults
import com.zs.compose.theme.text.Header
import com.zs.compose.theme.text.Label
import com.zs.compose.theme.text.Text
import com.zs.core.billing.Paymaster
import androidx.compose.foundation.layout.PaddingValues as Padding
import androidx.compose.foundation.layout.WindowInsetsSides as WIS
import com.zs.audiofy.common.compose.ContentPadding as CP

// Represents route to pref screen.
object RouteAboutUs : Route

// Used to style individual items within a preference section.
private val TileShape = RoundedCornerShape(24.dp)

/**
 * Represents an app with its title, image URL, and Play Store URL.
 *
 * @property first The title of theapp.
 * @property second The URL of the app's image.
 * @property third The URL of the app's Play Store page.
 */
private typealias App = Triple<String, String, String>

private val MyAppList = listOf(
    App(
        "Unit Converter",
        "https://play-lh.googleusercontent.com/TtUj94noX7g5B6Vs84A2PpVSCreYWVye5mHz32mSMHXCojT0xxDRtXBwXbc1q42AaA=s256-rw",
        "com.prime.toolz2"
    ),
    App(
        "Scientific Calculator",
        "https://play-lh.googleusercontent.com/ZK1RCWbqO5faf4Z1diQM6HtoaGbmM5dYudYY5yXXP1yZawHrElerat7ix0slYzAxHZRq=s256-rw",
        "com.prime.calculator.paid"
    ),
    App(
        "Gallery - Photos & Videos",
        "https://play-lh.googleusercontent.com/HlADK_i_qZoBn_4GNdjgCDt3Ah-h1ZbL_jUy1j_kDUo9Hvoq3AiUPI_ZxZXY95ftl7hu=w240-h480-rw",
        "com.googol.android.apps.photos"
    )
)

@Composable
private fun App(
    value: App,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(AppTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(CP.medium),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        AsyncImage(
            model = value.second,
            contentDescription = null,
            modifier = Modifier
                .shadow(4.dp, RoundedCornerShape(24), true)
                .size(60.dp),
            onError = {
                Log.d("about_us", "App: ${it.result.throwable.message}")
            }
        )

        Label(
            text = value.first,
            maxLines = 2,
            textAlign = TextAlign.Center,
            style = AppTheme.typography.label3,
            modifier = Modifier.width(56.dp)
        )
    }
}

@Composable
fun Release(
    info: CharSequence,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = TileShape,
        color = AppTheme.colors.background(1.dp),
        modifier = modifier
            .padding(top = CP.normal)
            .fillMaxWidth()
    ) {
        Text(
            info,
            style = AppTheme.typography.body2,
            modifier = Modifier.padding(CP.medium)
        )
    }
}

@Composable
private fun Sponsor(modifier: Modifier = Modifier) {
    BaseListItem(
        modifier = modifier
            .offset(y = -CP.normal)
            .background(AppTheme.colors.background(1.dp), TileShape),
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
fun AboutUs() {
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

    // TwoPane(
    TwoPane(
        strategy = strategy,
        secondary = {
            // this will not be called when in single pane mode
            // this is just for decoration
            if (strategy is SinglePaneStrategy) return@TwoPane
            Spacer(Modifier.width(300.dp))
        },
        topBar = {
            FloatingLargeTopAppBar(
                title = {
                    Text(
                        text = textResource(id = R.string.scr_about_us_title),
                        fontWeight = FontWeight.Light,
                        maxLines = 2,
                        lineHeight = 23.sp,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                scrollBehavior = topAppBarScrollBehavior,
                background = colors.background(surface),
                insets = WindowInsets.systemBars.only(WIS.Top),
                navigationIcon = {
                    val navController = LocalNavController.current
                    IconButton(
                        icon = Icons.AutoMirrored.Outlined.ReplyAll,
                        contentDescription = null,
                        onClick = navController::navigateUp
                    )
                },
                actions = {
                    val facade = LocalSystemFacade.current
                    // Feedback
                    IconButton(
                        icon = Icons.Outlined.AlternateEmail,
                        contentDescription = null,
                        onClick = { /*facade.launch(Settings.FeedbackIntent)*/ },
                    )
                    // Star on Github
                    IconButton(
                        icon = Icons.Outlined.DataObject,
                        contentDescription = null,
                        onClick = { facade.launch(Settings.GithubIntent) },
                    )
                    // Report Bugs on Github.
                    IconButton(
                        icon = Icons.Outlined.BugReport,
                        contentDescription = null,
                        onClick = { facade.launch(Settings.GitHubIssuesPage) },
                    )
                    // Join our telegram channel
                    IconButton(
                        icon = Icons.Outlined.Textsms,
                        contentDescription = null,
                        onClick = { facade.launch(Settings.TelegramIntent) },
                    )
                }
            )
        },
        primary = {
            val state = rememberLazyListState()
            val changelog = textArrayResource(R.array.changelog)
            LazyColumn(
                state = state,
                // In immersive mode, add horizontal padding to prevent settings from touching the screen edges.
                // Immersive layouts typically have a bottom app bar, so extra padding improves aesthetics.
                // Non-immersive layouts only need vertical padding.
                contentPadding = Padding(
                    horizontal = if (strategy is SinglePaneStrategy) CP.large else CP.normal,
                    CP.normal
                ) + (WindowInsets.content.union(WindowInsets.systemBars)
                    .union(inAppNavBarInsets).only(
                        WindowInsetsSides.Vertical
                    )).asPaddingValues(),
                modifier = Modifier
                    .source(surface)
                    .nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
                    .fadingEdge2(length = 56.dp),
                content = {
                    // sponsor
                    item(contentType = "sponsor") {
                        Sponsor()
                    }
                    // uprades
                    item(contentType = "header") {
                        Header(
                            "More from Us",
                            color = colors.accent,
                            drawDivider = true,
                            style = AppTheme.typography.title3,
                            contentPadding = Padding(vertical = CP.medium)
                        )
                    }

                    // apps
                    item(contentType = "our_apps") {
                        Row(
                            horizontalArrangement = CP.SmallArrangement,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val facade = LocalSystemFacade.current
                            MyAppList.forEach { app ->
                                App(
                                    value = app,
                                    onClick = { facade.launchAppStore(app.third) }
                                )
                            }
                        }

                    }

                    // latest.
                    item("release2") {
                        Release(textResource(R.string.release_notes))
                    }

                    // Changelog
                    item(contentType = "header") {
                        Header(
                            "Changelog",
                            color = colors.accent,
                            drawDivider = true,
                            style = AppTheme.typography.title3,
                            contentPadding = Padding(vertical = CP.medium)
                        )
                    }

                    items(changelog, contentType = {"release"}){item ->
                        Release(item, modifier = Modifier.padding(top = CP.normal))
                    }
                }
            )
        }
    )
}

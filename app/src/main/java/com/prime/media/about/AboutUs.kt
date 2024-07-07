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

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.DataObject
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.ReplyAll
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prime.media.BuildConfig
import com.prime.media.Material
import com.prime.media.R
import com.prime.media.backgroundColorAtElevation
import com.prime.media.caption2
import com.prime.media.core.ContentPadding
import com.prime.media.core.billing.purchased
import com.prime.media.core.compose.Banner
import com.prime.media.core.compose.LocalNavController
import com.prime.media.core.compose.LocalSystemFacade
import com.prime.media.core.compose.purchase
import com.prime.media.settings.Settings
import com.prime.media.surfaceColorAtElevation
import com.primex.core.drawHorizontalDivider
import com.primex.core.textResource
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.ListTile
import com.primex.material2.Text
import com.primex.material2.appbar.LargeTopAppBar
import com.primex.material2.appbar.TopAppBarDefaults
import com.primex.material2.appbar.TopAppBarScrollBehavior
import com.zs.ads.AdSize

private const val TAG = "AboutUs"

object AboutUs {
    val route = "route_about_us"
    fun direction() = route
}

@Composable
@NonRestartableComposable
private fun TopAppBar(
    modifier: Modifier = Modifier,
    behaviour: TopAppBarScrollBehavior? = null
) {
    LargeTopAppBar(
        modifier = modifier,
        title = { Label(text = textResource(id = R.string.about_us)) },
        navigationIcon = {
            val navController = LocalNavController.current
            IconButton(imageVector = Icons.Outlined.ReplyAll, onClick = navController::navigateUp)
        },
        scrollBehavior = behaviour,
        style = TopAppBarDefaults.largeAppBarStyle(
            scrolledContainerColor = Material.colors.surfaceColorAtElevation(1.dp),
            scrolledContentColor = Material.colors.onSurface,
            containerColor = Material.colors.backgroundColorAtElevation(0.1.dp)
        ),
        actions = {
            val facade = LocalSystemFacade.current
            // Feedback
            IconButton(
                imageVector = Icons.Outlined.AlternateEmail,
                onClick = {
                    facade.show(
                        message = "Something not working as expected? Share your feedback to help us improve.",
                        action = "Proceed",
                        onAction = { facade.launch(Settings.FeedbackIntent) }
                    )
                },
            )

            // Star on Github
            IconButton(
                imageVector = Icons.Outlined.DataObject,
                onClick = {
                    facade.show(
                        message = "Curious about the code? Check out our GitHub repository and don't forget to star us if you like it!",
                        action = "View",
                        onAction = { facade.launch(Settings.GithubIntent) }
                    )
                },
            )

            // Report Bugs on Github.
            IconButton(
                imageVector = Icons.Outlined.BugReport,
                onClick = {
                    facade.show(
                        message = "Spot a bug, typo, or have a feature request? Let us know on GitHub!",
                        action = "Proceed",
                        onAction = { facade.launch(Settings.GitHubIssuesPage) }
                    )
                },
            )

            // Join our telegram channel
            IconButton(
                imageVector = Icons.Outlined.SupportAgent,
                onClick = {
                    facade.show(
                        message = "Join our Telegram community for support, discussions, and updates!",
                        action = "Join",
                        onAction = { facade.launch(Settings.TelegramIntent) }
                    )
                },
            )
        }
    )
}

@Composable
private fun AppInfoBanner(
    modifier: Modifier = Modifier
) {
    ListTile(
        modifier = modifier,
        color = Color.Transparent,
        centerAlign = false,
        // Build version info.
        overline = {
            Text(
                text = textResource(id = R.string.app_name),
                style = Material.typography.h3,
                fontWeight = FontWeight.Bold,
                fontFamily = Settings.DancingScriptFontFamily
            )
        },
        // Title
        headline = {
            Text(
                text = textResource(
                    R.string.pref_get_to_know_us_subttile_s,
                    BuildConfig.VERSION_NAME
                ),
                style = Material.typography.caption2,
                color = LocalContentColor.current.copy(ContentAlpha.medium)
            )
        },
        trailing = {
            Icon(
                painter = painterResource(id = R.drawable.ic_launcher_foreground),
                contentDescription = null,
                // tint = Material.colors.primary,
                tint = Color.Unspecified,
                modifier = Modifier
                    .scale(3f)
                    .size(56.dp)
                    .offset(x = -ContentPadding.medium, y = ContentPadding.small)
            )
        },
        footer = {
            Row(
                modifier = Modifier.padding(top = ContentPadding.large),
                horizontalArrangement = Arrangement.spacedBy(ContentPadding.normal),
                verticalAlignment = Alignment.CenterVertically,
                content = {
                    val facade = LocalSystemFacade.current
                    val buttonModifier = Modifier
                        .weight(1f)
                        .height(56.dp)
                    val buttonColors = ButtonDefaults.buttonColors(
                        backgroundColor = Material.colors.backgroundColorAtElevation(1.dp)
                    )
                    val buttonShape = RoundedCornerShape(20)
                    val padding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)

                    // Donate
                    Button(
                        onClick = { facade.launchBillingFlow(BuildConfig.IAP_BUY_ME_COFFEE) },
                        modifier = buttonModifier,
                        colors = buttonColors,
                        shape = buttonShape,
                        contentPadding = padding,
                        elevation = null,
                        content = {
                            Label(text = "Donate")
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                imageVector = Icons.Outlined.Lightbulb,
                                onClick = {
                                    facade.show(
                                        message = R.string.msg_library_buy_me_a_coffee,
                                        icon = Icons.Outlined.Coffee
                                    )
                                }
                            )
                        }
                    )

                    Button(
                        onClick = facade::launchAppStore,
                        modifier = buttonModifier,
                        colors = buttonColors,
                        shape = buttonShape,
                        elevation = null,
                        contentPadding = padding,
                        content = {
                            Label(
                                text = textResource(id = R.string.rate_us),
                                style = Material.typography.body2
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(
                                imageVector = Icons.Outlined.Lightbulb,
                                onClick = {
                                    facade.show(
                                        message = R.string.msg_library_rate_us,
                                        icon = Icons.Outlined.Coffee
                                    )
                                }
                            )
                        }
                    )
                }
            )
        }
    )
}

private val HEADER_PADDING = PaddingValues(
    start = ContentPadding.large,
    end = ContentPadding.large,
    top = ContentPadding.normal,
    bottom = ContentPadding.medium
)

@Suppress("NOTHING_TO_INLINE")
@Composable
private inline fun Header(
    text: CharSequence
) {
    val primary = Material.colors.secondary
    Label(
        text = text,
        fontWeight = FontWeight.SemiBold,
        maxLines = 2,
        color = primary,
        modifier = Modifier
            .padding(HEADER_PADDING)
            .fillMaxWidth()
            .drawHorizontalDivider(color = primary)
            .padding(bottom = ContentPadding.medium),
        style = Material.typography.body2
    )
}

@Composable
fun AboutUs() {
    val topAppBarScrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    Scaffold(
        contentWindowInsets = WindowInsets.systemBars,
        topBar = { TopAppBar(behaviour = topAppBarScrollBehavior) },
        modifier = Modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection),
        content = { padding ->
            val verticalScrollState = rememberScrollState()
            Column(
                modifier = Modifier
                    .padding(padding)
                    .verticalScroll(verticalScrollState)
            ) {
                // The app banner for compact form for devices like phones in portrait mode.
                AppInfoBanner(
                    Modifier
                        .padding(horizontal = ContentPadding.normal)
                        .padding(top = ContentPadding.medium)
                )

                // Show Banner if not AdFree.
                val purchase by purchase(id = BuildConfig.IAP_NO_ADS)
                if (!purchase.purchased)
                    Banner(
                        Modifier
                            .padding(horizontal = ContentPadding.large)
                            .padding(top = ContentPadding.medium),
                        size = AdSize.MEDIUM_RECTANGLE
                    )

                // Upgrades
                val facade = LocalSystemFacade.current
                val products by facade.inAppProductDetails.collectAsState()
                if (products.isNotEmpty()) {
                    Header(text = "Upgrades")
                    Upgrades(
                        products,
                        Modifier
                            .padding(horizontal = ContentPadding.large)
                            .padding(top = ContentPadding.medium)
                    )
                }
                // App List showcasing my apps.
                Header(text = "Discover More")

                MyApps(
                    Modifier
                        .padding(horizontal = ContentPadding.large)
                        .padding(top = ContentPadding.medium)
                )

                // Header(text = "More")
            }
        }
    )
}
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
import androidx.compose.material.icons.outlined.ReplyAll
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.SupportAgent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prime.media.Audiofy
import com.prime.media.BuildConfig
import com.prime.media.Material
import com.prime.media.R
import com.prime.media.backgroundColorAtElevation
import com.prime.media.caption2
import com.prime.media.core.ContentPadding
import com.prime.media.core.compose.LocalNavController
import com.prime.media.core.compose.LocalSystemFacade
import com.prime.media.settings.Settings
import com.prime.media.surfaceColorAtElevation
import com.primex.core.drawHorizontalDivider
import com.primex.core.rememberVectorPainter
import com.primex.core.textResource
import com.primex.material2.Button
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.ListTile
import com.primex.material2.Text
import com.primex.material2.appbar.LargeTopAppBar
import com.primex.material2.appbar.TopAppBarDefaults
import com.primex.material2.appbar.TopAppBarScrollBehavior

private const val TAG = "AboutUs"

object AboutUs {
    val route = "route_about_us"
    fun direction() = route
}


private val FeedbackIntent = Intent(Intent.ACTION_SENDTO).apply {
    data = Uri.parse("mailto:helpline.prime.zs@gmail.com")
    putExtra(Intent.EXTRA_SUBJECT, "Feedback/Suggestion for Audiofy")
}
private val PrivacyPolicyIntent = Intent(Intent.ACTION_VIEW).apply {
    data =
        Uri.parse("https://docs.google.com/document/d/1AWStMw3oPY8H2dmdLgZu_kRFN-A8L6PDShVuY8BAhCw/edit?usp=sharing")
}
private val GitHubIssuesPage = Intent(Intent.ACTION_VIEW).apply {
    data = Uri.parse("https://github.com/iZakirSheikh/Audiofy/issues")
}
private val TelegramIntent = Intent(Intent.ACTION_VIEW).apply {
    data = Uri.parse("https://t.me/audiofy_support")
}
private val GithubIntent = Intent(Intent.ACTION_VIEW).apply {
    data = Uri.parse("https://github.com/iZakirSheikh/Audiofy")
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
                        action = "Launch",
                        onAction = { facade.launch(FeedbackIntent) }
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
                        onAction = { facade.launch(GithubIntent) }
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
                        onAction = { facade.launch(GitHubIssuesPage) }
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
                        onAction = { facade.launch(TelegramIntent) }
                    )
                },
            )
        },
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
                verticalAlignment = Alignment.CenterVertically
            ) {

                val facade = LocalSystemFacade.current
                Button(
                    label = "Donate",
                    icon = rememberVectorPainter(
                        image = Icons.Outlined.Coffee
                    ),
                    //  border = ButtonDefaults.outlinedBorder,
                    shape = RoundedCornerShape(20),
                    elevation = null,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    /*.padding(top = ContentPadding.normal)
                    .offset(x = -ContentPadding.normal)
                    .scale(0.85f)*/
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Material.colors.backgroundColorAtElevation(
                            1.dp
                        )
                    ),
                    onClick = {
                        facade.show(
                            message = "Thank you for considering a donation! Your support makes a difference.",
                            action = "Contribute",
                            icon = Icons.Outlined.Coffee,
                            onAction = { facade.launchBillingFlow(BuildConfig.IAP_BUY_ME_COFFEE)}
                        )
                    },
                )

                Button(
                    label = "Rate on Playstore",
                    icon = rememberVectorPainter(
                        image = Icons.Outlined.StarOutline
                    ),
                    // border = ButtonDefaults.outlinedBorder,
                    shape = RoundedCornerShape(20),
                    elevation = null,
                    modifier = Modifier
                        .weight(1f)
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = Material.colors.backgroundColorAtElevation(
                            1.dp
                        )
                    ),
                    onClick = {
                        facade.show(
                            message = "Your 5-star ratings help us reach more users and make Audiofy even better!",
                            action = "Proceed",
                            onAction = facade::launchAppStore
                        )
                    },
                )
            }
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
        modifier = Modifier.nestedScroll(topAppBarScrollBehavior.nestedScrollConnection)
    ) { padding ->
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

            // Upgrades
            Header(text = "Upgrades")

            Upgrades(
                Modifier
                    .padding(horizontal = ContentPadding.large)
                    .padding(top = ContentPadding.medium)
            )

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
}


/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 23-05-2025.
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

package com.zs.audiofy.common.compose.directory

import android.text.format.DateUtils
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ReplyAll
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.FormatListNumbered
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.zs.audiofy.common.compose.LocalNavController
import com.zs.audiofy.common.compose.shine
import com.zs.compose.foundation.Background
import com.zs.compose.foundation.background
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.HorizontalDivider
import com.zs.compose.theme.Icon
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.LocalContentColor
import com.zs.compose.theme.None
import com.zs.compose.theme.VerticalDivider
import com.zs.compose.theme.appbar.AppBarDefaults
import com.zs.compose.theme.appbar.CollapsableTopBarLayout
import com.zs.compose.theme.appbar.TopAppBarScrollBehavior
import com.zs.compose.theme.minimumInteractiveComponentSize
import com.zs.compose.theme.text.Label
import com.zs.compose.theme.text.ProvideTextStyle
import com.zs.compose.theme.text.Text
import com.zs.audiofy.common.compose.ContentPadding as CP

private val MIN_HEIGHT = 56.dp
private val ARTWORK_WIDTH = 75.dp

@Composable
fun FilesTopAppBar(
    info: MetaData,
    behavior: TopAppBarScrollBehavior,
    background: Background,
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
) {
    val style =
        AppBarDefaults.largeAppBarStyle(maxHeight = 210.dp, height = MIN_HEIGHT)
    val insets = WindowInsets.systemBars.only(WindowInsetsSides.Top + WindowInsetsSides.End)

    CollapsableTopBarLayout(
        height = style.height,
        maxHeight = style.maxHeight,
        insets = WindowInsets.None,
        modifier = modifier
            .widthIn(max = AppBarDefaults.FLOATING_TOP_APP_BAR_MAX_WIDTH)
            .windowInsetsPadding(insets)
            .padding(horizontal = 30.dp),
        scrollBehavior = behavior
    ) {

        val contentColor = style.contentColor(1 - fraction)
        val textStyle = style.titleTextStyle(fraction)
        val colors = AppTheme.colors
        CompositionLocalProvider(LocalContentColor provides contentColor) {
            ProvideTextStyle(textStyle) {
                // Background
                Spacer(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer() {
                            this.alpha =
                                androidx.compose.ui.util.lerp(1f, 0f, fraction * 3f)
                            shadowElevation = lerp(12.dp, 0.dp, fraction / 0.5f).toPx()
                            shape = AppBarDefaults.FloatingTopBarShape
                            clip = true
                        }
                        .layoutId(AppBarDefaults.ID_BACKGROUND)
                        .let {
                            if (fraction >= 0.5f) it.background(colors.background)
                            else
                                it.border(colors.shine, AppBarDefaults.FloatingTopBarShape)
                                    .background(background)
                        }
                )

                // Representational icon
                val icon = info.icon
                when {
                    // Display the icon representing the list
                    icon != null -> Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .padding(horizontal = CP.small)
                            .layoutId(AppBarDefaults.ID_NAVICON)
                    )
                    // else show the back button
                    else -> {
                        val navController = LocalNavController.current
                        IconButton(
                            icon = Icons.AutoMirrored.Outlined.ReplyAll,
                            contentDescription = null,
                            onClick = navController::navigateUp,
                            modifier = Modifier.layoutId(AppBarDefaults.ID_NAVICON)
                        )
                    }
                }

                // Actions
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.layoutId(AppBarDefaults.ID_ACTION),
                    content = actions
                )

                //
                val alphaModifier = Modifier.graphicsLayer() {
                    this.alpha = androidx.compose.ui.util.lerp(0f, 1f, (fraction - 0.25f) / 0.75f)
                }

                // Divider
                HorizontalDivider(
                    modifier = Modifier
                        .then(alphaModifier)
                        .road(Alignment.TopStart, Alignment.TopStart)
                        .offset(0.dp, MIN_HEIGHT - CP.xSmall),
                    thickness = 1.dp
                )

                // Artwork
                AsyncImage(
                    model = info.artwork,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .offset {
                            lerp(
                                IntOffset.Zero,
                                IntOffset(0, (MIN_HEIGHT + 12.dp).roundToPx()),
                                fraction
                            )
                        }
                        .size(ARTWORK_WIDTH, 125.dp)
                        .then(alphaModifier)
                        .shadow(8.dp, AppTheme.shapes.medium)
                        .background(AppTheme.colors.background(1.dp))
                        .road(Alignment.TopStart, Alignment.TopStart)
                )

                // TITLE
                Text(
                    text = info.title,
                    maxLines = 2,
                    fontWeight = FontWeight.Light,
                    lineHeight = 24.sp,
                    modifier = Modifier
                        .road(Alignment.CenterStart, Alignment.TopStart)
                        .layoutId(AppBarDefaults.ID_COLLAPSABLE_TITLE)
                        .offset {
                            lerp(
                                IntOffset.Zero,
                                IntOffset(
                                    (ARTWORK_WIDTH + CP.large).roundToPx(),
                                    (MIN_HEIGHT + 6.dp).roundToPx()
                                ),
                                fraction
                            )
                        },
                )

                // Info
                Row(
                    modifier = Modifier
                        .then(alphaModifier)
                        .road(Alignment.TopStart, Alignment.TopStart)
                        .offset((ARTWORK_WIDTH + CP.large), ((MIN_HEIGHT + 6.dp) * 2 + 10.dp)),
                    horizontalArrangement = CP.LargeArrangement,
                    verticalAlignment = Alignment.CenterVertically,
                    content = {
                        // count
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = CP.SmallArrangement,
                            content = {
                                Icon(
                                    Icons.Outlined.FormatListNumbered,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Label(
                                    "${info.cardinality} Files",
                                    style = AppTheme.typography.label2
                                )
                            }
                        )

                        //Divider 2
                        VerticalDivider(
                            modifier = Modifier.height(40.dp),
                        )

                        // Date
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = CP.SmallArrangement,
                            content = {
                                Icon(
                                    Icons.Outlined.CalendarMonth,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Label(
                                    "${
                                        if (info.dateModified == -1L) "N/A" else DateUtils.formatDateTime(
                                            LocalContext.current,
                                            info.dateModified,
                                            DateUtils.FORMAT_ABBREV_MONTH
                                        )
                                    }", style = AppTheme.typography.label2
                                )
                            }
                        )


                    }
                )
            }
        }
    }
}
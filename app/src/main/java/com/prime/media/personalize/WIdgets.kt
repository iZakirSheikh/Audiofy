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

package com.prime.media.personalize

import android.net.Uri
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.prime.media.BuildConfig
import com.prime.media.common.LocalSystemFacade
import com.prime.media.common.collectNowPlayingAsState
import com.prime.media.common.ellipsize
import com.prime.media.common.isFreemium
import com.prime.media.common.isPurchasable
import com.prime.media.common.purchase
import com.prime.media.common.richDesc
import com.prime.media.old.core.playback.MediaItem
import com.prime.media.widget.Glance
import com.primex.core.textResource
import com.primex.material2.Button
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.OutlinedButton
import com.zs.core.paymaster.purchased
import com.zs.core.playback.NowPlaying
import com.zs.core.playback.PlaybackController
import com.zs.core_ui.AppTheme
import com.zs.core_ui.ContentPadding
import com.zs.core_ui.Header
import com.zs.core.paymaster.ProductInfo as Product
import com.prime.media.R
import com.zs.core_ui.shape.TagShape

/**
 * A map of widget bundles to their corresponding product IDs.
 */
private val widgets = mapOf(
    BuildConfig.IAP_WIDGETS_PLATFORM to listOf(
        BuildConfig.IAP_PLATFORM_WIDGET_IPHONE,
        BuildConfig.IAP_PLATFORM_WIDGET_RED_VIOLET_CAKE,
        BuildConfig.IAP_PLATFORM_WIDGET_SNOW_CONE,
        BuildConfig.IAP_PLATFORM_WIDGET_TIRAMISU,
        BuildConfig.IAP_PLATFORM_WIDGET_DISK_DYNAMO,
        BuildConfig.IAP_PLATFORM_WIDGET_ELONGATE_BEAT,
        BuildConfig.IAP_PLATFORM_WIDGET_SKEWED_DYNAMIC
    ),
    BuildConfig.IAP_COLOR_CROFT_WIDGET_BUNDLE to listOf(
        BuildConfig.IAP_COLOR_CROFT_GRADIENT_GROVES,
        BuildConfig.IAP_COLOR_CROFT_GOLDEN_DUST,
        BuildConfig.IAP_COLOR_CROFT_WAVY_GRADIENT_DOTS,
        BuildConfig.IAP_COLOR_CROFT_ROTATING_GRADEINT,
        BuildConfig.IAP_COLOR_CROFT_MISTY_DREAM
    )
)

/**
 * A sample media item used for testing and demonstration purposes.
 */
private val SampleMediaItem =
    MediaItem(
        Uri.EMPTY,
        "Sample Title",
        "Sample Artist",
        artwork = Uri.parse("https://upload.wikimedia.org/wikipedia/en/c/c0/LetitbleedRS.jpg")
    )

private val DEBUG get() = BuildConfig.DEBUG

private val WIDGET_MAX_WIDTH = 400.dp

/**
 * Displays a list of widgets as a column in a [LazyColumn].
 *
 * @param selected The ID of the currently selected widget.
 * @param details A map of widget IDs to their details.
 * @param onRequestApply Callback invoked when a widget is selected for application.
 */
fun LazyListScope.widgets(
    state: NowPlaying,
    selected: String,
    details: Map<String, Product>,
    onRequestApply: (widget: String) -> Unit
) {
    for ((group, children) in widgets) {
        val groupInfo = details[group] ?: continue
        // Emit group header
        item(
            group,
            contentType = "group_header",
            content = {
                val facade = LocalSystemFacade.current
                Header(
                    text = groupInfo.title.ellipsize(15),
                    contentPadding = PaddingValues(bottom = ContentPadding.large),
                    style = AppTheme.typography.titleLarge,
                    // Show a toast with the group's rich description on click
                    leading = {
                        IconButton(
                            imageVector = Icons.Outlined.Info,
                            onClick = { facade.showToast(groupInfo.richDesc) }
                        )
                    },
                    action = {
                        // If the group is not purchasable, skip this section
                        if (!groupInfo.isPurchasable)
                            return@Header
                        // If the group is purchasable, check its purchase state
                        // If the group is not purchased, display a purchase button
                        val purchase by purchase(group)
                        if (!purchase.purchased)
                            androidx.compose.material.Button(
                                content = {
                                    Label(
                                        textResource(R.string.scr_personalize_unlock_all_s,  groupInfo.formattedPrice ?: "N/A"),
                                        maxLines = 2,
                                        textAlign = TextAlign.Center
                                    )
                                },
                                elevation = null,
                                shape = TagShape,
                                // Initiate purchase flow on click
                                onClick = { facade.initiatePurchaseFlow(group) },
                                border = if (AppTheme.colors.isLight) null else ButtonDefaults.outlinedBorder
                            )
                    }
                )
            }
        )

        // Emit a widget for each child
        for (child in children) {
            item(
                child + group,
                content = {
                    Glance(child, state, {}, showcase = true)
                }
            )

            // Emit a footer item for the current child widget
            // This section defines the footer for each widget item in the list
            val childInfo = details[child]
                ?: continue // Retrieve details for the child; skip if not found (shouldn't happen)
            item(
                key = "footer_$child",
                contentType = "widget_footer",
                content = {
                    Header(
                        text = childInfo.title.ellipsize(30),
                        contentPadding = PaddingValues(
                            bottom = ContentPadding.normal,
                            start = ContentPadding.normal
                        ),
                        style = AppTheme.typography.bodyMedium,
                        action = {
                            // Show a toast with the child's rich description on click
                            val facade = LocalSystemFacade.current
                            IconButton(
                                imageVector = Icons.Outlined.Info,
                                onClick = { facade.showToast(childInfo.richDesc) }
                            )

                            // Purchase and selection state
                            val widget by purchase(child) // Observe the purchase state of the child widget
                            val bundle by purchase(group) // Observe the purchase state of the parent group/bundle
                            val isApplied =
                                selected == child // Check if this child widget is currently selected
                            // If freemium, purchased, debuggable or part of a purchased bundle, show a radio button
                            if (childInfo.isFreemium || widget.purchased || bundle.purchased || DEBUG)
                                return@Header androidx.compose.material.RadioButton(
                                    isApplied,
                                    onClick = { onRequestApply(child) },
                                    enabled = !isApplied
                                )
                            // If not freemium or purchased, display a purchase button
                            // Display an outlined button to purchase the widget
                            OutlinedButton(
                                label = "Get - ${childInfo.formattedPrice}",
                                onClick = { facade.initiatePurchaseFlow(child) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    backgroundColor = Color.Transparent,
                                ),
                                shape = AppTheme.shapes.compact,
                                border = ButtonDefaults.outlinedBorder
                            )
                        },
                        modifier = Modifier.widthIn(max = WIDGET_MAX_WIDTH)
                    )
                }
            )
        }
    }
}


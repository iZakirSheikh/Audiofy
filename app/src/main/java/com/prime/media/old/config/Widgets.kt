/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 20-09-2024.
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

package com.prime.media.old.config


import android.net.Uri
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.RadioButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zs.core.paymaster.ProductInfo as ProductDetails
import com.prime.media.BuildConfig
import com.zs.core_ui.ContentPadding
import com.zs.core.paymaster.purchased
import com.prime.media.old.common.Header
import com.prime.media.common.LocalSystemFacade
import com.prime.media.common.purchase
import com.prime.media.old.core.playback.MediaItem
import com.prime.media.old.widget.GoldenDust
import com.prime.media.old.widget.GradientGroves
import com.prime.media.old.widget.Iphone
import com.prime.media.old.widget.RedVelvetCake
import com.prime.media.old.widget.SnowCone
import com.prime.media.old.widget.Tiramisu
import com.primex.core.drawHorizontalDivider
import com.primex.core.withSpanStyle
import com.primex.material2.Button
import com.primex.material2.IconButton
import com.primex.material2.OutlinedButton
import com.zs.core_ui.AppTheme
import com.zs.core_ui.Divider

private const val TAG = "Widgets"

private val WIDGET_MAX_WIDTH = 400.dp

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

/**
 * A map of widget bundles to their corresponding product IDs.
 */
private val widgets = mapOf(
    BuildConfig.IAP_WIDGETS_PLATFORM to listOf(
        BuildConfig.IAP_PLATFORM_WIDGET_IPHONE,
        BuildConfig.IAP_PLATFORM_WIDGET_RED_VIOLET_CAKE,
        BuildConfig.IAP_PLATFORM_WIDGET_SNOW_CONE,
        BuildConfig.IAP_PLATFORM_WIDGET_TIRAMISU
    ),
    BuildConfig.IAP_COLOR_CROFT_WIDGET_BUNDLE to listOf(
        BuildConfig.IAP_COLOR_CROFT_GRADIENT_GROVES,
        BuildConfig.IAP_COLOR_CROFT_GOLDEN_DUST
    )
)

/**
 * @return - Indicates if the widget is free in the Play Console.
 * Some widgets are listed for providing localized names and descriptions,
 * but their price cannot be set to 0.
 */
private val ProductDetails.isFreemium: Boolean
    get() {
        return when (id) {
            BuildConfig.IAP_PLATFORM_WIDGET_IPHONE, BuildConfig.IAP_COLOR_CROFT_GRADIENT_GROVES -> true
            else -> false
        }
    }

/**
 * Controls whether this item should be showcased for purchase.
 * Sometimes the group is not fully prepared and needs further improvements.
 * This variable can be used to control whether to showcase this item for purchase.
 */
private val ProductDetails.isPurchasable: Boolean
    get() {
        return when (id) {
            BuildConfig.IAP_COLOR_CROFT_WIDGET_BUNDLE -> false
            else -> true
        }
    }

/**
 * A formatted message combining the product name and description.
 *
 * This property constructs an [AnnotatedString] that displays the product name in bold,
 * followed by the description in gray with a smaller font size.
 */
private val ProductDetails.formattedProductDetails
    get() = buildAnnotatedString {
        withSpanStyle(fontWeight = FontWeight.Bold) {
            appendLine(title)
        }
        withStyle(SpanStyle(color = Color.Gray, fontSize = 13.sp)) {
            append(description)
        }
    }

/**
 * Emits a widget corresponding to the id.
 */
@NonRestartableComposable
@Composable
private fun Preview(
    id: String,
    modifier: Modifier = Modifier
) = when (id) {
    BuildConfig.IAP_PLATFORM_WIDGET_IPHONE -> Iphone(item = SampleMediaItem, modifier)
    BuildConfig.IAP_PLATFORM_WIDGET_RED_VIOLET_CAKE -> RedVelvetCake(
        item = SampleMediaItem,
        modifier
    )

    BuildConfig.IAP_PLATFORM_WIDGET_SNOW_CONE -> SnowCone(item = SampleMediaItem, modifier)
    BuildConfig.IAP_PLATFORM_WIDGET_TIRAMISU -> Tiramisu(item = SampleMediaItem, modifier)
    BuildConfig.IAP_COLOR_CROFT_GOLDEN_DUST -> GoldenDust(SampleMediaItem, modifier)
    BuildConfig.IAP_COLOR_CROFT_GRADIENT_GROVES -> GradientGroves(SampleMediaItem, modifier)
    else -> error("Unknown widget $id")
}

/**
 * Displays a list of widgets as a column in a [LazyColumn].
 *
 * @param selected The ID of the currently selected widget.
 * @param details A map of widget IDs to their details.
 * @param onRequestApply Callback invoked when a widget is selected for application.
 */
fun LazyListScope.widgets(
    selected: String,
    details: Map<String, ProductDetails>,
    onRequestApply: (widget: String) -> Unit
) {
    for ((group, children) in widgets) {
        // emit the bundle as header.
        item(
            group,
            content = {
                // it will never be null
                val info = details[group] ?: return@item
                Header(
                    text = info.title,
                    contentPadding = PaddingValues(bottom = ContentPadding.medium),
                    modifier = Modifier.drawHorizontalDivider(AppTheme.colors.onBackground.copy(ContentAlpha.Divider)),
                    action = {
                        // emit purchase button.
                        val facade = LocalSystemFacade.current
                        IconButton(
                            imageVector = Icons.Outlined.Info,
                            onClick = { facade.showToast(info.formattedProductDetails) }
                        )
                        if (!info.isPurchasable)
                            return@Header
                        // show price tag; if not purchased yet.
                        val bundle by purchase(group)
                        if (!bundle.purchased)
                            Button(
                                label = "Get - ${info.formattedPrice}",
                                onClick = { facade.initiatePurchaseFlow(group) },
                            )
                    }
                )
            }
        )
        // emit children
        for (child in children) {
            // emit preview
            item(
                child + group,
                content = {
                    Preview(
                        id = child,
                        modifier = Modifier.widthIn(max = WIDGET_MAX_WIDTH)
                    )
                }
            )

            // emit footer
            // emit footer
            // footer of each item
            item(key = "footer_$child", contentType = "widget_footer") {
                // hopefully it will not be null
                val info = details[child] ?: return@item
                Header(
                    text = info.title,
                    contentPadding = PaddingValues(bottom = ContentPadding.normal),
                    style = AppTheme.typography.bodyMedium,
                    action = {
                        // info
                        val facade = LocalSystemFacade.current
                        IconButton(
                            imageVector = Icons.Outlined.Info,
                            onClick = { facade.showToast(info.formattedProductDetails) }
                        )

                        val widget by purchase(child)
                        val bundle by purchase(group)
                        val isApplied = selected == child
                        // only show this if purchases
                        if (info.isFreemium || widget.purchased || bundle.purchased)
                            return@item RadioButton(
                                isApplied,
                                onClick = { onRequestApply(child) },
                                enabled = !isApplied
                            )
                        // get this for below price.
                        OutlinedButton(
                            label = "Get - ${info.formattedPrice}",
                            onClick = { facade.initiatePurchaseFlow(child) },
                            colors = ButtonDefaults.outlinedButtonColors(
                                backgroundColor = Color.Transparent,
                            ),
                            shape = AppTheme.shapes.compact,
                            border = ButtonDefaults.outlinedBorder
                        )
                    }
                )
            }
        }
    }
}
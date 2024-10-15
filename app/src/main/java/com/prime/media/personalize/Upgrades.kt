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

@file:OptIn(ExperimentalMaterialApi::class, ExperimentalLayoutApi::class)

package com.prime.media.personalize

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CutCornerShape
import androidx.compose.material.ButtonDefaults.OutlinedBorderSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prime.media.BuildConfig
import com.prime.media.common.LocalSystemFacade
import com.prime.media.common.dynamicFeatureRequest
import com.prime.media.common.dynamicModuleName
import com.prime.media.common.ellipsize
import com.prime.media.common.isDynamicFeature
import com.prime.media.common.purchase
import com.prime.media.common.richDesc
import com.prime.media.settings.OutfitFontFamily
import com.primex.core.withSpanStyle
import com.primex.material2.Label
import com.primex.material2.Text
import com.zs.core.paymaster.purchased
import com.zs.core_ui.AppTheme
import com.zs.core_ui.Colors
import com.zs.core_ui.toast.Toast
import com.zs.core.paymaster.ProductInfo as Product
import com.zs.core_ui.ContentPadding as CP

private val Colors.border
    @Composable @ReadOnlyComposable
    get() = BorderStroke(OutlinedBorderSize, background(5.dp))

private val ProductShape = CutCornerShape(10)

/**
 * Represents a product that can be purchased within the UI.
 *
 * This composable displays a product with its associated state and an optional action button.
 *
 * @param value The info about the product.
 * @param state A descriptive text indicating the current state of the product
 * (e.g., "Unlocked", "Installed").
 * @param onClick The callback function to be executed when the product or its action button is clicked.
 * @param action An optional text label for the action button (e.g., "Unlock", "Install"). If empty,
 * the action button is not displayed.
 */
@Composable
private fun Upgrade(
    value: Product,
    state: CharSequence,
    onClick: () -> Unit,
    action: String? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = ProductShape,
        color = Color.Transparent,
        modifier = modifier,
        border = AppTheme.colors.border,
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier
                //.shimmer(shimmerColors, 60.dp, BlendMode.HardLight)
                .padding(CP.medium),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally,
            content = {
                // Top Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    content = {
                        // info
                        val facade = LocalSystemFacade.current
                        com.primex.material2.IconButton(
                            imageVector = Icons.Outlined.Info,
                            onClick = {
                                facade.showToast(
                                    value.richDesc,
                                    duration = Toast.DURATION_INDEFINITE
                                )
                            },
                         //   modifier = Modifier.scale(0.9f)
                        )
                        // title/state
                        Label(
                            text = buildAnnotatedString {
                                appendLine(value.title.ellipsize(12))
                                withSpanStyle(color = AppTheme.colors.accent, fontSize = 9.sp){
                                    appendLine(state)
                                }
                            },
                            style = AppTheme.typography.caption,
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Bold,
                            maxLines = 2
                        )
                    }
                )

                // Product formatted price
                Text(
                    text = value.formattedPrice ?: "N/A",
                    modifier = Modifier.padding(vertical = CP.small),
                    style = AppTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.OutfitFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // action
                Label(
                    text = action ?: return@Column,
                    style = AppTheme.typography.caption,
                    fontWeight = FontWeight.SemiBold,
                    color = AppTheme.colors.accent,
                    modifier = Modifier
                        .align(Alignment.Start)
                        .padding(start = CP.medium),
                    fontSize = 10.sp
                )
            }
        )
    }
}

val DefaultArrangement = Arrangement.spacedBy(CP.normal)

private val Upgrades = listOf(
    BuildConfig.IAP_CODEX,
    BuildConfig.IAP_NO_ADS,
    BuildConfig.IAP_TAG_EDITOR_PRO
)

/**
 * Represents all the upgrades offered by the user
 */
@Composable
fun Upgrades(
    data: Map<String, Product>,
    modifier: Modifier = Modifier
) {
    FlowRow(
        maxItemsInEachRow = 2,
        horizontalArrangement = DefaultArrangement,
        verticalArrangement = DefaultArrangement,
        modifier = modifier
    ) {
        val facade = LocalSystemFacade.current
        data.forEach { (key, value) ->
            // Skip this iteration if the key is not a valid Upgrade type
            if (key !in Upgrades) return@forEach
            // Observe the purchase state for the current key using a delegated property
            val purchase by purchase(key)
            // Create an Upgrade composable to display and handle interactions for this product
            Upgrade(
                value = value,
                onClick = {
                    // If the product is NOT already purchased, initiate the purchase flow
                    if (!purchase.purchased) {
                        facade.initiatePurchaseFlow(key)
                        return@Upgrade // don't proceed further.
                    }

                    // If the product is a dynamic feature and is NOT installed, initiate the feature installation
                    if (value.isDynamicFeature && !facade.isInstalled(value.dynamicModuleName)) {
                        facade.initiateFeatureInstall(value.dynamicFeatureRequest)
                    }
                    // Otherwise (product is purchased or not a dynamic feature that needs installation), do nothing
                },
                // Determine the state text to display based on the product's current status
                state = when {
                    // If it's a dynamic feature and installed, show "Installed"
                    value.isDynamicFeature && facade.isInstalled(value.dynamicModuleName) -> "Installed"
                    // If it's purchased, show "Purchased"
                    purchase.purchased -> "Purchased"
                    // Otherwise, show "Unlock" (indicating it needs to be purchased)
                    else -> "Unlock"
                },
                // Determine the action text for the button based on the product's status
                action = when {
                    // If it's a dynamic feature and not installed, show "Tap to Install"
                    value.isDynamicFeature && !facade.isInstalled(value.dynamicModuleName) -> "Tap to Install"
                    // If it's not purchased, show "Tap to Purchase"
                    !purchase.purchased -> "Tap to Purchase"
                    // Otherwise, show no action (empty string)
                    else -> ""
                },
                modifier = Modifier.weight(0.5f)
            )
        }
        Spacer(Modifier.weight(0.5f))
    }
}
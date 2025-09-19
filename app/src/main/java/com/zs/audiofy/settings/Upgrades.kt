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

package com.zs.audiofy.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zs.audiofy.MainActivity
import com.zs.audiofy.R
import com.zs.audiofy.common.IAP_CODEX
import com.zs.audiofy.common.IAP_TAG_EDITOR_PRO
import com.zs.audiofy.common.compose.LocalSystemFacade
import com.zs.audiofy.common.compose.purchase
import com.zs.audiofy.common.dynamicFeatureRequest
import com.zs.audiofy.common.dynamicModuleName
import com.zs.audiofy.common.ellipsize
import com.zs.audiofy.common.isDynamicFeature
import com.zs.audiofy.common.richDesc
import com.zs.audiofy.common.shapes.NotchedCornerShape
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.Colors
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.Surface
import com.zs.compose.theme.ripple
import com.zs.compose.theme.snackbar.SnackbarDuration
import com.zs.compose.theme.text.Label
import com.zs.compose.theme.text.Text
import com.zs.core.billing.Paymaster
import com.zs.core.billing.Product
import com.zs.core.billing.purchased
import kotlinx.coroutines.flow.map
import com.zs.audiofy.common.compose.ContentPadding as CP

private val ProductShape = NotchedCornerShape(10.dp)
private val Colors.border
    @Composable @ReadOnlyComposable
    get() = BorderStroke(1.dp, background(15.dp))

/**
 * Represents a product that can be purchased within the UI.
 *
 * This composable displays a product with its associated state and an optional action button.
 *
 * @param value The info about the product.
 * @param state A descriptive text indicating the current state of the product
 * (e.g., "Unlocked", "Installed").
 * @param onClick The callback function to be executed when the product or its action button is clicked.
 */
@Composable
private fun Upgrade(
    value: Product,
    state: CharSequence,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        shape = ProductShape,
        color = Color.Transparent,
        contentColor = AppTheme.colors.onBackground,
        modifier = modifier,
        border = AppTheme.colors.border,
        onClick = onClick,
        indication = ripple(color = AppTheme.colors.accent),
        content = {
            Column {
                // Product formatted price
                Text(
                    text = value.formattedPrice ?: stringResource(R.string.abbr_not_available),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = CP.medium),
                    style = AppTheme.typography.headline2,
                    fontWeight = FontWeight.Medium,
                    fontFamily = FontFamily.OutfitFontFamily,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )

                // Top Row - Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    content = {
                        // info
                        val facade = LocalSystemFacade.current
                        IconButton(
                            icon = Icons.Outlined.Info,
                            contentDescription = null,
                            onClick = {
                                facade.showSnackbar(
                                    value.richDesc,
                                    duration = SnackbarDuration.Indefinite
                                )
                            }
                        )

                        // title/state
                        Label(
                            text = buildAnnotatedString {
                                appendLine(value.title.ellipsize(15))
                                withStyle(
                                    SpanStyle(
                                        color = AppTheme.colors.accent,
                                        fontSize = 9.sp
                                    )
                                ) {
                                    appendLine(state)
                                }
                            },
                            style = AppTheme.typography.label3,
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.Bold,
                            maxLines = 2
                        )
                    }
                )
            }
        }
    )
}

private val upgrades = arrayOf(Paymaster.IAP_CODEX, Paymaster.IAP_TAG_EDITOR_PRO)

@Composable
context(_: RouteSettings)
fun Upgrades(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = CP.LargeArrangement,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val facade = LocalSystemFacade.current
        val paymaster = (facade as MainActivity).paymaster
        val upgrades by remember { paymaster.details.map { it.filter { it.id in upgrades } } }
            .collectAsState(null)
        // leave early if null
        if (upgrades.isNullOrEmpty())
            return@Row
        for (upgrade in upgrades) {
            val purchase by purchase(upgrade.id)
            Upgrade(
                value = upgrade,
                modifier = Modifier.weight(0.5f),
                // Determine the state text to display based on the product's current status
                state = when {
                    // If it's a dynamic feature and installed, show "Installed"
                    upgrade.isDynamicFeature && facade.isFeatureInstalled(upgrade.dynamicModuleName) -> stringResource(
                        R.string.installed
                    )
                    // If it's a dynamic feature and not installed, show "Tap to Install"
                    upgrade.isDynamicFeature && !facade.isFeatureInstalled(upgrade.dynamicModuleName) -> stringResource(
                        R.string.tap_to_install
                    )
                    // If it's purchased, show "Purchased"
                    purchase.purchased -> stringResource(R.string.purchased)
                    // Otherwise, show "Unlock" (indicating it needs to be purchased)
                    else -> stringResource(R.string.unlock)
                },
                onClick = {
                    // If the product is NOT already purchased, initiate the purchase flow
                    if (!purchase.purchased) {
                        facade.initiatePurchaseFlow(upgrade.id)
                        return@Upgrade // don't proceed further.
                    }

                    // If the product is a dynamic feature and is NOT installed, initiate the feature installation
                    if (upgrade.isDynamicFeature && !facade.isFeatureInstalled(upgrade.dynamicModuleName)) {
                        facade.initiateFeatureInstall(upgrade.dynamicFeatureRequest)
                        return@Upgrade
                    }
                    // Otherwise (product is purchased or not a dynamic feature that needs installation), do nothing
                    facade.showToast(R.string.msg_settings_upgrade_unlocked)
                }
            )
        }
    }
}
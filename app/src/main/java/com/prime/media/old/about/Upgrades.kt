/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 22-06-2024.
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

package com.prime.media.old.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DoNotDisturbAlt
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zs.core.paymaster.ProductInfo as ProductDetails
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.prime.media.BuildConfig
import com.zs.core_ui.ContentPadding
import com.zs.core.paymaster.purchased
import com.prime.media.old.common.LocalSystemFacade
import com.prime.media.old.common.purchase
import com.zs.core_ui.shimmer.shimmer
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.zs.core_ui.AppTheme
import com.zs.core_ui.toast.Toast

/**
 * The name of the on-demand module for the Codex feature.
 */
private const val ON_DEMAND_MODULE_CODEX = "codex"

/**
 * A list of all supported in-app product IDs.
 */
private val SUPPORTED_PRODUCTS = listOf(
    BuildConfig.IAP_CODEX,
    BuildConfig.IAP_TAG_EDITOR_PRO,
    BuildConfig.IAP_NO_ADS
)

/**
 * Provides the appropriate icon based on the product ID.
 *
 * @param id The ID of the in-app product.
 * @return The corresponding ImageVector for the product.
 */
private inline fun provideIcon(id: String): ImageVector {
    return when (id) {
        BuildConfig.IAP_CODEX -> Icons.Outlined.Extension
        BuildConfig.IAP_NO_ADS -> Icons.Outlined.DoNotDisturbAlt
        else -> Icons.Outlined.Tag
    }
}

/**
 * Checks if the product represents a dynamic feature.
 */
private val ProductDetails.isDynamicFeature
    inline get() = this.id == BuildConfig.IAP_CODEX

/**
 * Returns the name of the dynamic module associated with the product.
 *
 * @throws IllegalStateException if the product is not a dynamic module.
 */
private val ProductDetails.dynamicModuleName
    inline get() = when (id) {
        BuildConfig.IAP_CODEX -> ON_DEMAND_MODULE_CODEX
        else -> error("$id is not a dynamic module.")
    }

/**
 * Checks if a dynamic module with the given name is installed.
 *
 * @param id The name of the dynamic module.
 * @return True if the module is installed, false otherwise.
 */
private fun SplitInstallManager.isInstalled(id: String): Boolean =
    installedModules.contains(id)

/**
 * Creates a SplitInstallRequest for the dynamic feature associated with the product.
 */
private val ProductDetails.dynamicFeatureRequest
    inline get() =
        SplitInstallRequest.newBuilder().addModule(dynamicModuleName).build()


private val ProductDetailsShape = RoundedCornerShape(16)

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun Product(
    details: ProductDetails,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = ProductDetailsShape,
        color = Color.Transparent,
        modifier = modifier,
        border = ButtonDefaults.outlinedBorder,
        onClick = onClick,
        content = {
            val shimmerColors = listOf(
                Color.Unspecified,
                AppTheme.colors.onBackground.copy(0.05f),
                AppTheme.colors.onBackground.copy(0.1f),
                AppTheme.colors.onBackground.copy(0.05f),
                Color.Unspecified
            )

            Column(
                modifier = Modifier
                    .shimmer(shimmerColors, 60.dp, BlendMode.Hardlight)
                    .padding(ContentPadding.medium)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally,
                content = {
                    // top row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        content = {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Label(
                                text = details.title,
                                style = AppTheme.typography.caption,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = ContentPadding.medium),
                                fontWeight = FontWeight.Bold
                            )
                            // info
                            // onTap the info button show the button.
                            val facade = LocalSystemFacade.current
                            IconButton(
                                imageVector = Icons.Outlined.Info,
                                onClick = {
                                    facade.show(
                                        details.description,
                                        duration = Toast.DURATION_INDEFINITE
                                    )
                                }
                            )
                        }
                    )
                    // price row
                    Label(
                        text = details.formattedPrice ?: "N/A",
                        modifier = Modifier.padding(vertical = ContentPadding.medium),
                        style = AppTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Light
                    )
                    Label(
                        text = details.description,
                        style = AppTheme.typography.caption,
                        color = AppTheme.colors.onBackground.copy(ContentAlpha.disabled)
                    )
                }
            )
        }
    )
}

/**
 * Represents the inAppPurchases showcased in AboutUs Screen of the app.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Upgrades(
    details: Map<String, ProductDetails>,
    modifier: Modifier = Modifier
) {
    FlowRow(
        maxItemsInEachRow = 2,
        horizontalArrangement = Arrangement.spacedBy(ContentPadding.normal),
        verticalArrangement = Arrangement.spacedBy(ContentPadding.normal),
        modifier = modifier
    ) {
        val facade = LocalSystemFacade.current
        details.forEach { (id, details) ->
            // continue
            if (id !in SUPPORTED_PRODUCTS) return@forEach
            // FixMe - provide a non-observable check in SystemFacade.
            val state by purchase(id = id)
            Product(
                details = details,
                icon = provideIcon(id),
                onClick = {
                    val isDynamicFeature = details.isDynamicFeature
                    when {
                        !state.purchased -> facade.launchBillingFlow(id)
                        else -> facade.show(
                            "You already own ${details.title}! \nThanks for your support \uD83D\uDE0A"
                        )
                    }
                },
                modifier = Modifier.weight(1f)
            )
        }
        // If there are odd no. of items in details
        // emit a spacer; so that a even no. of items are in FlowRow
        // which will make sure that 2 items are in each row.
        if (details.size % 2 != 0) Spacer(modifier = Modifier.weight(1f))
    }
}
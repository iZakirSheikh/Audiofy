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

package com.prime.media.about

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
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Upgrade
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.prime.media.BuildConfig
import com.prime.media.Material
import com.prime.media.R
import com.prime.media.caption2
import com.prime.media.core.ContentPadding
import com.prime.media.core.billing.purchased
import com.prime.media.core.compose.LocalSystemFacade
import com.prime.media.core.compose.purchase
import com.prime.media.core.compose.shimmer.shimmer
import com.primex.material2.Label

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun PriceTag(
    title: String,
    price: String,
    description: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16),
        color = Material.colors.secondary.copy(0f),
        modifier = modifier,
        border = ButtonDefaults.outlinedBorder,
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .shimmer(
                    width = 60.dp,
                    colors = listOf(
                        Color.Unspecified,
                        Material.colors.onSurface.copy(0.05f),
                        Material.colors.onSurface.copy(0.1f),
                        Material.colors.onSurface.copy(0.05f),
                        Color.Unspecified
                    ),
                    blendMode = BlendMode.Hardlight
                )
                .padding(ContentPadding.medium)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(16.dp))
                Label(
                    text = title,
                    style = Material.typography.caption,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = ContentPadding.small),
                    fontWeight = FontWeight.Bold
                )
                Icon(
                    imageVector = Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
            }
            Label(
                text = price,
                modifier = Modifier.padding(vertical = ContentPadding.medium),
                style = Material.typography.h5,
                fontWeight = FontWeight.Light
            )
            Label(
                text = description,
                style = Material.typography.caption2,
                color = Material.colors.onSurface.copy(ContentAlpha.disabled)
            )
        }
    }
}

private val ON_DEMAND_MODULE_CODEX = "codex"
private val CODEX_INSTALL_REQUEST = SplitInstallRequest.newBuilder().addModule(ON_DEMAND_MODULE_CODEX).build()

/**
 * Represents the inAppPurchases showcased in AboutUs Screen of the app.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun Upgrades(
    modifier: Modifier = Modifier
) {
    FlowRow(
        maxItemsInEachRow = 2,
        horizontalArrangement = Arrangement.spacedBy(ContentPadding.normal),
        verticalArrangement = Arrangement.spacedBy(ContentPadding.normal),
        modifier = modifier
    ) {
        val facade = LocalSystemFacade.current
        val upgrades by facade.inAppProductDetails.collectAsState()
        // return from here if empty
        if (upgrades.isEmpty()) return@FlowRow
        var upgrade = upgrades[BuildConfig.IAP_NO_ADS]
        val isAdFree by purchase(id = BuildConfig.IAP_NO_ADS)
        PriceTag(
            title = upgrade?.title ?: "",
            price = upgrade?.oneTimePurchaseOfferDetails?.formattedPrice ?: "",
            description = upgrade?.description ?: "",
            icon = ImageVector.vectorResource(id = (R.drawable.ic_remove_ads)),
            modifier = Modifier.weight(1f),
            onClick = {
                if (isAdFree?.purchased ?: return@PriceTag)
                  return@PriceTag facade.show("You rock! Thanks for upgrading to ad-free.")
                facade.show(
                    message = upgrade?.description ?: "",
                    action = "Go Ad-Free",
                    onAction = {
                        facade.launchBillingFlow(BuildConfig.IAP_NO_ADS)
                    }
                )
            },
        )
        Spacer(modifier = Modifier.weight(1f))

        upgrade = upgrades[BuildConfig.IAP_CODEX]
        val codex by purchase(id = BuildConfig.IAP_CODEX)
        val context = LocalContext.current
        val manager = remember { SplitInstallManagerFactory.create(context) }
        val installed = remember {
            manager.installedModules.contains(ON_DEMAND_MODULE_CODEX)
        }
        PriceTag(
            title = upgrade?.title ?: "",
            price = upgrade?.oneTimePurchaseOfferDetails?.formattedPrice ?: "",
            description = upgrade?.description ?: "",
            icon = Icons.Outlined.Upgrade,
            onClick = {
                if (codex == null) return@PriceTag
                if (codex.purchased && installed)
                    return@PriceTag facade.show("The codex is already installed. enjoy")
                if (!codex.purchased)
                return@PriceTag facade.show(
                    message = upgrade?.description ?: "",
                    action = "Proceed",
                    onAction = {
                        facade.launchBillingFlow(BuildConfig.IAP_CODEX)
                    }
                )
                if (!installed)
                    return@PriceTag facade.show(
                        message = upgrade?.description ?: "",
                        action = "Install",
                        onAction = {
                            manager.startInstall(CODEX_INSTALL_REQUEST)
                        }
                    )
            },
            modifier = Modifier.weight(1f)
        )

        upgrade = upgrades[BuildConfig.IAP_TAG_EDITOR_PRO]
        val tagEditor by purchase(id = BuildConfig.IAP_TAG_EDITOR_PRO)
        PriceTag(
            title = upgrade?.title ?: "",
            price = upgrade?.oneTimePurchaseOfferDetails?.formattedPrice ?: "",
            description = upgrade?.description ?:"",
            icon = Icons.Outlined.Tag,
            onClick = {
                if (tagEditor?.purchased ?: return@PriceTag)
                    return@PriceTag facade.show("Tag Editor is already unlocked.")
                facade.show(
                    message = upgrade?.description ?:"",
                    action = "Unlock",
                    onAction = {
                        facade.launchBillingFlow(BuildConfig.IAP_TAG_EDITOR_PRO)
                    }
                )
            },
            modifier = Modifier.weight(1f)
        )
    }
}


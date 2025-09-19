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

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.zs.audiofy.BuildConfig
import com.zs.audiofy.R
import com.zs.audiofy.common.IAP_BUY_ME_COFFEE
import com.zs.audiofy.common.IAP_NO_ADS
import com.zs.audiofy.common.compose.LocalSystemFacade
import com.zs.audiofy.common.compose.purchase
import com.zs.compose.foundation.textResource
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.BaseListItem
import com.zs.compose.theme.Button
import com.zs.compose.theme.ButtonDefaults
import com.zs.compose.theme.FilledTonalButton
import com.zs.compose.theme.Icon
import com.zs.compose.theme.Surface
import com.zs.compose.theme.text.Text
import com.zs.core.billing.Paymaster
import com.zs.core.billing.purchased
import com.zs.audiofy.common.compose.ContentPadding as CP

@Composable
context(_: RouteSettings)
fun Sponsor(modifier: Modifier = Modifier) {
    BaseListItem(
        modifier = modifier
            .offset(y = -CP.normal)
            .background(AppTheme.colors.background(1.dp), RouteSettings.SingleTileShape),
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
        // RateUs + Sponsor/Ad-free.
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
                    val adFreePurchase by purchase(Paymaster.IAP_NO_ADS)
                    when {
                        // Coffee
                        adFreePurchase.purchased -> Button(
                            "Say Thanks",
                            icon = Icons.Outlined.FavoriteBorder,
                            onClick = { facade.initiatePurchaseFlow(Paymaster.IAP_BUY_ME_COFFEE) },
                        )
                        else -> Button(
                            "Unlock Ad-free",
                            icon = ImageVector.vectorResource(R.drawable.ic_remove_ads),
                            onClick = { facade.initiatePurchaseFlow(Paymaster.IAP_BUY_ME_COFFEE) },
                        )
                    }
                }
            )
        }
    )
}
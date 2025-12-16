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

package com.prime.media.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.RateReview
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.prime.media.BuildConfig
import com.prime.media.R
import com.prime.media.common.LocalSystemFacade
import com.prime.media.common.purchase
import com.primex.core.textResource
import com.primex.material2.ListTile
import com.primex.material2.Text
import com.zs.core.paymaster.purchased
import com.zs.core_ui.AppTheme
import com.zs.core_ui.ContentPadding as CP

@Composable
context(_: RouteSettings)
fun Sponsor(modifier: Modifier = Modifier) {
    ListTile(
        modifier = modifier
            .offset(y = -CP.normal)
            .background(AppTheme.colors.background(1.dp), RouteSettings.SingleTileShape),
        centerAlign = true,
        onColor = AppTheme.colors.onBackground,
        // App name.
        overline = {
            Text(
                text = textResource(R.string.app_name),
                style = AppTheme.typography.displaySmall,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.DancingScriptFontFamily,
                color = AppTheme.colors.onBackground
            )
        },
        // Build version info.
        headline = {
            Text(
                text = textResource(R.string.version_info_s, BuildConfig.VERSION_NAME),
                style = AppTheme.typography.caption2,
                fontWeight = FontWeight.Normal
            )
        },
        // app icon
        leading = {
            Surface(
                color = Color.Black,
                shape = Settings.mapKeyToShape(BuildConfig.IAP_ARTWORK_SHAPE_SQUIRCLE),
                modifier = Modifier.size(50.dp),
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
                    com.primex.material2.Button (
                        textResource(R.string.rate_us),
                        icon = rememberVectorPainter(Icons.Outlined.RateReview),
                        onClick = facade::launchAppStore,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = AppTheme.colors.background(
                                4.dp
                            )
                        ),
                        shape = AppTheme.shapes.compact,
                        elevation = null
                    )
                    val adFreePurchase by purchase(BuildConfig.IAP_NO_ADS)
                    when {
                        // Coffee
                        adFreePurchase.purchased -> com.primex.material2.Button(
                            "Say Thanks",
                            icon = rememberVectorPainter(Icons.Outlined.FavoriteBorder),
                            onClick = { facade.initiatePurchaseFlow(BuildConfig.IAP_BUY_ME_COFFEE) },
                            shape = AppTheme.shapes.compact,
                            elevation = null
                        )
                        else -> com.primex.material2.Button(
                            "Unlock Ad-free",
                            icon = painterResource(R.drawable.ic_remove_ads),
                            onClick = { facade.initiatePurchaseFlow(BuildConfig.IAP_NO_ADS) },
                            shape = AppTheme.shapes.compact,
                            elevation = null
                        )
                    }
                }
            )
        }
    )
}
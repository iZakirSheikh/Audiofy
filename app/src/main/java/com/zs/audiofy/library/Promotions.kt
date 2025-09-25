/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 23-11-2024.
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

package com.zs.audiofy.library

import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.repeatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.HotelClass
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ShopTwo
import androidx.compose.material.icons.outlined.Translate
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.zs.audiofy.R
import com.zs.audiofy.common.IAP_BUY_ME_COFFEE
import com.zs.audiofy.common.IAP_CODEX
import com.zs.audiofy.common.IAP_NO_ADS
import com.zs.audiofy.common.IAP_TAG_EDITOR_PRO
import com.zs.audiofy.common.IAP_WIDGETS_PLATFORM
import com.zs.audiofy.common.compose.LocalSystemFacade
import com.zs.audiofy.common.compose.preference
import com.zs.audiofy.common.compose.purchase
import com.zs.audiofy.common.richDesc
import com.zs.audiofy.settings.Settings
import com.zs.compose.foundation.Amber
import com.zs.compose.foundation.AzureBlue
import com.zs.compose.foundation.MetroGreen
import com.zs.compose.foundation.SkyBlue
import com.zs.compose.foundation.composableIf
import com.zs.compose.foundation.effects.shimmer
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.BaseListItem
import com.zs.compose.theme.Button
import com.zs.compose.theme.ButtonDefaults
import com.zs.compose.theme.Icon
import com.zs.compose.theme.text.Text
import com.zs.core.billing.Paymaster
import com.zs.core.billing.Product
import com.zs.core.billing.purchased
import androidx.compose.foundation.layout.PaddingValues as Padding
import androidx.compose.ui.graphics.Brush.Companion.linearGradient as LinearGradient
import com.zs.audiofy.common.compose.ContentPadding as CP
import com.zs.compose.foundation.textResource as stringResource

private const val TAG = "Promotions"

private val PROMOTION_PADDING =
    Padding(CP.xSmall, CP.small, CP.small, CP.small)

private val SHIMMER_ANIM_SPEC =
    repeatable<Float>(1, animation = tween(1_000, 1_000, LinearEasing))

@Composable
private fun Promotion(
    expanded: Boolean,
    onValueChange: (Boolean) -> Unit,
    message: CharSequence,
    icon: ImageVector?,
    modifier: Modifier = Modifier,
    accent: Color = AppTheme.colors.accent,
    action: @Composable (() -> Unit)? = null,
) {
    val movableAction = when {
        action == null -> null
        else -> remember(message) { movableContentOf(action) }
    }
    // Real Content
    BaseListItem(
        trailing = composableIf(!expanded) { movableAction?.invoke() },
        footer = composableIf(expanded) { movableAction?.invoke() },
        heading = {
            Text(
                text = message,
                style = AppTheme.typography.body2,
                maxLines = if (!expanded) 3 else 10,
                overflow = TextOverflow.Ellipsis,
            )
        },
        spacing = CP.small,
        padding = PROMOTION_PADDING,
        contentColor = AppTheme.colors.onBackground,
        leading = composableIf(icon != null) {
            Icon(
                imageVector = icon ?: Icons.Outlined.Info,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.padding(vertical = CP.xSmall)
            )
        },
        modifier = modifier
            .border(
                0.5.dp,
                LinearGradient(
                    listOf(
                        accent.copy(0.24f),
                        Color.Transparent,
                        Color.Transparent,
                        accent.copy(0.24f),
                    ),
                ),
                AppTheme.shapes.medium
            )
            .clip(AppTheme.shapes.medium)
            .background(
                LinearGradient(
                    0.0f to accent.copy(0.12f),
                    0.4f to AppTheme.colors.background,
                    0.9f to AppTheme.colors.background,
                    1.0f to accent.copy(0.06f)
                )
            )
            .shimmer(
                listOf(
                    Color.Transparent,
                    accent.copy(if (AppTheme.colors.isLight) 0.24f else 0.12f),
                    Color.Transparent
                ),
                width = 150.dp,
                blendMode = BlendMode.Hardlight,
                animationSpec = SHIMMER_ANIM_SPEC
            )
            .scale(0.92f)
            .toggleable(expanded, null, indication = null/*scale()*/, onValueChange = onValueChange)
            .animateContentSize()
    )
}

@Composable
@NonRestartableComposable
private fun RateUs(
    expanded: Boolean = false,
    onValueChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val facade = LocalSystemFacade.current
    Promotion(
        expanded,
        onValueChange,
        message = stringResource(id = R.string.msg_library_rate_us),
        icon = Icons.Outlined.HotelClass,
        modifier = modifier,
        accent = Color.MetroGreen,
        action = {
            Button(
                text = stringResource(id = R.string.rate_us).toString().uppercase(),
                onClick = facade::launchAppStore,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.MetroGreen.copy(0.12f)
                ),
                shape = AppTheme.shapes.small,
                elevation = null,
                modifier = Modifier.scale(0.9f),
                border = BorderStroke(Dp.Hairline, Color.MetroGreen.copy(0.24f))
            )
        }
    )
}

@Composable
@NonRestartableComposable
private fun JoinUs(
    expanded: Boolean = false,
    onValueChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val facade = LocalSystemFacade.current
    Promotion(
        expanded,
        onValueChange,
        message = stringResource(id = R.string.msg_library_join_us),
        icon = Icons.Outlined.Chat,
        modifier = modifier,
        accent = Color.SkyBlue,
        action = {
            Button(
                text = stringResource(id = R.string.telegram),
                onClick = { facade.launch(Settings.TelegramIntent) },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.SkyBlue.copy(0.12f)
                ),
                shape = AppTheme.shapes.small,
                elevation = null,
                modifier = Modifier.scale(0.9f),
                border = BorderStroke(Dp.Hairline, Color.SkyBlue.copy(0.24f))
            )
        }
    )
}

@Composable
@NonRestartableComposable
private fun GetApp(
    expanded: Boolean = false,
    onValueChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val facade = LocalSystemFacade.current
    val pkg = "com.googol.android.apps.photos"
    // Check if the Gallery app is already installed
    val ctx = LocalContext.current
    Promotion(
        expanded,
        onValueChange,
        message = stringResource(id = R.string.msg_promotion_gallery_app),
        icon = Icons.Outlined.HotelClass,
        modifier = modifier,
        accent = Color.Amber,
        action = {
            Button(
                text = stringResource(id = R.string.dive_in),
                onClick = { facade.launchAppStore(pkg) },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Amber.copy(0.12f)
                ),
                shape = AppTheme.shapes.small,
                elevation = null,
                modifier = Modifier.scale(0.9f),
                border = BorderStroke(Dp.Hairline, Color.Amber.copy(0.24f))
            )
        }
    )
}


@Composable
private fun HelpTranslate(
    expanded: Boolean = false,
    onValueChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val facade = LocalSystemFacade.current
    Promotion(
        expanded,
        onValueChange,
        message = stringResource(id = R.string.msg_library_help_translate),
        icon = Icons.Outlined.Translate,
        modifier = modifier,
        accent = Color.AzureBlue,
        action = {
            Button(
                text = stringResource(id = R.string.translate),
                onClick = { facade.launch(Settings.TranslateIntent) },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.SkyBlue.copy(0.12f)
                ),
                shape = AppTheme.shapes.small,
                elevation = null,
                modifier = Modifier.scale(0.9f),
                border = BorderStroke(Dp.Hairline, Color.SkyBlue.copy(0.24f))
            )
        }
    )
}

private val Product.action
    get() = when (id) {
        Paymaster.IAP_BUY_ME_COFFEE -> R.string.sponsor
        else -> R.string.unlock
    }

@Composable
private fun InAppPurchase(
    id: String,
    expanded: Boolean = false,
    onValueChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val purchase by purchase(id)
    Log.d(TAG, "InAppPurchase: purchase $id")
    // Check if the current composition is in inspection mode.
    // Inspection mode is typically used during UI testing or debugging to isolate and analyze
    // specific UI components. If in inspection mode, return to avoid executing the rest of the code.
    if (purchase.purchased || LocalInspectionMode.current) return Spacer(modifier)
    val facade = LocalSystemFacade.current
    val info = facade.getProductInfo(id)
    if (info == null) {
        // This would never happen
        Log.d(TAG, "InAppPurchase: details for $id not found")
        return Spacer(modifier)
    }
    val details = info ?: return
    Promotion(
        expanded,
        onValueChange,
        message = details.richDesc,
        icon = Icons.Outlined.ShopTwo,
        modifier = modifier,
        action = {
            Button(
                text = stringResource(id = details.action),
                onClick = { facade.initiatePurchaseFlow(id) },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = AppTheme.colors.background(4.dp)
                ),
                shape = AppTheme.shapes.small,
                elevation = null,
                modifier = Modifier.scale(0.9f),
                border = BorderStroke(Dp.Hairline, AppTheme.colors.background(10.dp))
            )
        }
    )
}

private const val PROMOTIONS_COUNT = 8

/**
 * Displays a series of promotional items, cycling through them with delays.
 *
 * This composable manages the display and transitions between different promotional
 * items, such as in-app purchases, rating prompts, and app recommendations.
 *
 * @param modifier Modifier used to adjust the layout or appearance of the promotions.
 */
@Composable
fun Promotions(
    modifier: Modifier = Modifier
) {
    // current: Index of the currently displayed promotion item.
    // Starts with ID_NONE to indicate no promotion is initially shown.
    val count by preference(Settings.KEY_LAUNCH_COUNTER)
    // expanded: State variable to track if a promotion item is expanded (details shown).
    // onValueChange: Callback to update the expanded state.
    val (expanded, onValueChange) = remember { mutableStateOf(false) }

    // AnimatedContent: Composable to handle animated transitions between promotion items.
    Box(
        modifier = modifier,
        content = {
            // Display the appropriate promotion item based on the current index.
            when ((count ?: 0) % PROMOTIONS_COUNT) {
                0 -> InAppPurchase(Paymaster.IAP_NO_ADS, expanded, onValueChange)
                1 -> InAppPurchase(Paymaster.IAP_CODEX, expanded, onValueChange)
                2 -> InAppPurchase(Paymaster.IAP_TAG_EDITOR_PRO, expanded, onValueChange)
                3 -> InAppPurchase(Paymaster.IAP_WIDGETS_PLATFORM, expanded, onValueChange)
                4 -> RateUs(expanded, onValueChange)
                5 -> JoinUs(expanded, onValueChange)
                6 -> GetApp(expanded, onValueChange)
                7 -> HelpTranslate(expanded, onValueChange)
            }
        },
    )
}
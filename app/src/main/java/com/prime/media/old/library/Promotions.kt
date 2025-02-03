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

package com.prime.media.old.library

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.prime.media.BuildConfig
import com.prime.media.MainActivity
import com.prime.media.R
import com.prime.media.common.LocalSystemFacade
import com.prime.media.common.observeProductInfoAsState
import com.prime.media.common.preference
import com.prime.media.common.purchase
import com.prime.media.common.richDesc
import com.prime.media.settings.Settings
import com.primex.core.Amber
import com.primex.core.AzureBlue
import com.primex.core.MetroGreen
import com.primex.core.SkyBlue
import com.primex.core.composableOrNull
import com.primex.material2.ListTile
import com.primex.material2.Text
import com.zs.core.paymaster.ProductInfo
import com.zs.core.paymaster.purchased
import com.zs.core_ui.AppTheme
import com.zs.core_ui.scale
import com.zs.core_ui.shimmer.shimmer
import androidx.compose.foundation.layout.PaddingValues as Padding
import com.primex.core.textResource as stringResource
import com.zs.core_ui.ContentPadding as CP

private const val TAG = "Promotions"

private val PromotionShape = RoundedCornerShape(12)
private val PROMOTION_PADDING = Padding(CP.small, CP.medium, CP.medium, CP.medium)

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
    ListTile(
        trailing = composableOrNull(!expanded) { movableAction?.invoke() },
        footer = composableOrNull(expanded) { movableAction?.invoke() },
        headline = {
            Text(
                text = message,
                style = AppTheme.typography.bodyMedium,
                maxLines = if (!expanded) 3 else 10,
                overflow = TextOverflow.Ellipsis,
            )
        },
        spacing = CP.medium,
        padding = PROMOTION_PADDING,
        onColor = AppTheme.colors.onBackground,
        leading = composableOrNull(icon != null) {
            Icon(
                imageVector = icon ?: Icons.Outlined.Info,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.padding(vertical = CP.small)
            )
        },
        modifier = modifier
            .border(
                0.5.dp,
                Brush.linearGradient(
                    listOf(
                        accent.copy(0.24f),
                        Color.Transparent,
                        Color.Transparent,
                        accent.copy(0.24f),
                    ),
                ),
                PromotionShape
            )
            .clip(PromotionShape)
            .background(
                Brush.linearGradient(
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
            .toggleable(expanded, null, indication = scale(), onValueChange = onValueChange)
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
            com.primex.material2.Button(
                label = stringResource(id = R.string.rate_us).toString().uppercase(),
                onClick = facade::launchAppStore,
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.MetroGreen.copy(0.12f)
                ),
                shape = AppTheme.shapes.compact,
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
            com.primex.material2.Button(
                label = stringResource(id = R.string.telegram),
                onClick = { facade.launch(Settings.TelegramIntent) },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.SkyBlue.copy(0.12f)
                ),
                shape = AppTheme.shapes.compact,
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
    val isInstalled = remember(pkg) {
        com.primex.core.runCatching(TAG) {
            ctx.packageManager.getPackageInfo(pkg, 0)
        } != null
    }
    if (isInstalled) return
    Promotion(
        expanded,
        onValueChange,
        message = stringResource(id = R.string.msg_promotion_gallery_app),
        icon = Icons.Outlined.HotelClass,
        modifier = modifier,
        accent = Color.Amber,
        action = {
            com.primex.material2.Button(
                label = stringResource(id = R.string.dive_in),
                onClick = { facade.launchAppStore(pkg) },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.Amber.copy(0.12f)
                ),
                shape = AppTheme.shapes.compact,
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
            com.primex.material2.Button(
                label = stringResource(id = R.string.translate),
                onClick = { facade.launch(Settings.TranslateIntent) },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = Color.SkyBlue.copy(0.12f)
                ),
                shape = AppTheme.shapes.compact,
                elevation = null,
                modifier = Modifier.scale(0.9f),
                border = BorderStroke(Dp.Hairline, Color.SkyBlue.copy(0.24f))
            )
        }
    )
}

private val ProductInfo.action
    get() = when (id) {
        BuildConfig.IAP_BUY_ME_COFFEE -> R.string.sponsor
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
    val info by facade.observeProductInfoAsState(id)
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
            com.primex.material2.Button(
                label = stringResource(id = details.action),
                onClick = { facade.initiatePurchaseFlow(id) },
                colors = ButtonDefaults.buttonColors(
                    backgroundColor = AppTheme.colors.background(4.dp)
                ),
                shape = AppTheme.shapes.compact,
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
                0 -> InAppPurchase(BuildConfig.IAP_NO_ADS, expanded, onValueChange)
                1 -> InAppPurchase(BuildConfig.IAP_CODEX, expanded, onValueChange)
                2 -> InAppPurchase(BuildConfig.IAP_TAG_EDITOR_PRO, expanded, onValueChange)
                3 -> InAppPurchase(BuildConfig.IAP_WIDGETS_PLATFORM, expanded, onValueChange)
                4 -> RateUs(expanded, onValueChange)
                5 -> JoinUs(expanded, onValueChange)
                6 -> GetApp(expanded, onValueChange)
                7 -> HelpTranslate(expanded, onValueChange)
            }
        },
    )
}

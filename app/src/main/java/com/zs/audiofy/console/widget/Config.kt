package com.zs.audiofy.console.widget

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddShoppingCart
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.unit.dp
import com.zs.audiofy.MainActivity
import com.zs.audiofy.R
import com.zs.audiofy.common.IAP_COLOR_CROFT_GOLDEN_DUST
import com.zs.audiofy.common.IAP_COLOR_CROFT_GRADIENT_GROVES
import com.zs.audiofy.common.IAP_COLOR_CROFT_MISTY_DREAM
import com.zs.audiofy.common.IAP_COLOR_CROFT_ROTATING_GRADIENT
import com.zs.audiofy.common.IAP_COLOR_CROFT_WAVY_GRADIENT_DOTS
import com.zs.audiofy.common.IAP_COLOR_CROFT_WIDGET_BUNDLE
import com.zs.audiofy.common.IAP_PLATFORM_WIDGET_DISK_DYNAMO
import com.zs.audiofy.common.IAP_PLATFORM_WIDGET_ELONGATE_BEAT
import com.zs.audiofy.common.IAP_PLATFORM_WIDGET_IPHONE
import com.zs.audiofy.common.IAP_PLATFORM_WIDGET_RED_VIOLET_CAKE
import com.zs.audiofy.common.IAP_PLATFORM_WIDGET_SKEWED_DYNAMIC
import com.zs.audiofy.common.IAP_PLATFORM_WIDGET_SNOW_CONE
import com.zs.audiofy.common.IAP_PLATFORM_WIDGET_TIRAMISU
import com.zs.audiofy.common.IAP_WIDGETS_PLATFORM
import com.zs.audiofy.common.WindowStyle
import com.zs.audiofy.common.compose.ContentPadding
import com.zs.audiofy.common.compose.LocalSystemFacade
import com.zs.audiofy.common.compose.background
import com.zs.audiofy.common.compose.preference
import com.zs.audiofy.common.compose.shine
import com.zs.audiofy.common.isFreemium
import com.zs.audiofy.common.richDesc
import com.zs.audiofy.console.widget.styles.DiskDynamo
import com.zs.audiofy.console.widget.styles.ElongatedBeat
import com.zs.audiofy.console.widget.styles.GoldenDust
import com.zs.audiofy.console.widget.styles.GradientGroves
import com.zs.audiofy.console.widget.styles.MistyDream
import com.zs.audiofy.console.widget.styles.MistyTunes
import com.zs.audiofy.console.widget.styles.RedVioletCake
import com.zs.audiofy.console.widget.styles.RotatingColorGradient
import com.zs.audiofy.console.widget.styles.SkewedDynamic
import com.zs.audiofy.console.widget.styles.SnowCone
import com.zs.audiofy.console.widget.styles.Tiramisu
import com.zs.audiofy.console.widget.styles.WavyGradientDots
import com.zs.audiofy.settings.Settings
import com.zs.compose.theme.AppTheme
import com.zs.compose.theme.Button
import com.zs.compose.theme.Icon
import com.zs.compose.theme.IconButton
import com.zs.compose.theme.None
import com.zs.compose.theme.SplitButtonLayout
import com.zs.compose.theme.TrailingSplitButton
import com.zs.compose.theme.appbar.TopAppBar
import com.zs.compose.theme.snackbar.SnackbarDuration
import com.zs.compose.theme.text.Label
import com.zs.core.billing.Paymaster
import com.zs.core.billing.Product
import com.zs.core.billing.Purchase
import com.zs.core.billing.purchased
import com.zs.core.playback.NowPlaying
import dev.chrisbanes.haze.HazeState
import kotlinx.coroutines.flow.combine


private typealias Purchasable = Pair<Purchase?, Product?>?

private const val TAG = "Config"

/**
 * Represents all the widgets offered by the app.
 */
private val WIDGETS = arrayOf(
    // Widgets Platform
    Paymaster.IAP_PLATFORM_WIDGET_IPHONE,
    Paymaster.IAP_PLATFORM_WIDGET_RED_VIOLET_CAKE,
    Paymaster.IAP_PLATFORM_WIDGET_SNOW_CONE,
    Paymaster.IAP_PLATFORM_WIDGET_TIRAMISU,
    Paymaster.IAP_PLATFORM_WIDGET_DISK_DYNAMO,
    Paymaster.IAP_PLATFORM_WIDGET_ELONGATE_BEAT,
    Paymaster.IAP_PLATFORM_WIDGET_SKEWED_DYNAMIC,

    // widgets COLOR_CROFT
    Paymaster.IAP_COLOR_CROFT_GRADIENT_GROVES,
    Paymaster.IAP_COLOR_CROFT_GOLDEN_DUST,
    Paymaster.IAP_COLOR_CROFT_WAVY_GRADIENT_DOTS,
    Paymaster.IAP_COLOR_CROFT_ROTATING_GRADIENT,
    Paymaster.IAP_COLOR_CROFT_MISTY_DREAM,
)

/** @return the bungle key in which widget [key] resides.*/
private fun bundleKeyOf(key: String): String {
    return when (key) {
        // Widgets Platform
        Paymaster.IAP_PLATFORM_WIDGET_IPHONE,
        Paymaster.IAP_PLATFORM_WIDGET_RED_VIOLET_CAKE,
        Paymaster.IAP_PLATFORM_WIDGET_SNOW_CONE,
        Paymaster.IAP_PLATFORM_WIDGET_TIRAMISU,
        Paymaster.IAP_PLATFORM_WIDGET_DISK_DYNAMO,
        Paymaster.IAP_PLATFORM_WIDGET_ELONGATE_BEAT,
        Paymaster.IAP_PLATFORM_WIDGET_SKEWED_DYNAMIC -> Paymaster.IAP_WIDGETS_PLATFORM

        // widgets COLOR_CROFT
        Paymaster.IAP_COLOR_CROFT_GRADIENT_GROVES,
        Paymaster.IAP_COLOR_CROFT_GOLDEN_DUST,
        Paymaster.IAP_COLOR_CROFT_WAVY_GRADIENT_DOTS,
        Paymaster.IAP_COLOR_CROFT_ROTATING_GRADIENT,
        Paymaster.IAP_COLOR_CROFT_MISTY_DREAM -> Paymaster.IAP_COLOR_CROFT_WIDGET_BUNDLE

        else -> error("No mapping key found for key: $key")
    }
}

/**
 * Represents the widget preview flyout
 */
context(_: Widget)
@Composable
fun Config(
    state: NowPlaying,
    surface: HazeState,
    onRequest: (code: Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
        // viewpager
        val selected by preference(Settings.GLANCE)
        val pagerController =
            rememberPagerState(WIDGETS.indexOf(selected)) { WIDGETS.size }

        // Pager
        HorizontalPager(
            pagerController
        ) { index ->
            when (WIDGETS[index]) {
                Paymaster.IAP_PLATFORM_WIDGET_DISK_DYNAMO -> DiskDynamo(state, onRequest)
                Paymaster.IAP_PLATFORM_WIDGET_ELONGATE_BEAT ->
                    ElongatedBeat(state, onRequest)

                Paymaster.IAP_COLOR_CROFT_GOLDEN_DUST -> GoldenDust(state, onRequest)
                Paymaster.IAP_COLOR_CROFT_GRADIENT_GROVES -> GradientGroves(state, onRequest)
                Paymaster.IAP_PLATFORM_WIDGET_IPHONE -> MistyTunes(state, surface, onRequest)
                Paymaster.IAP_COLOR_CROFT_MISTY_DREAM -> MistyDream(state, onRequest)
                Paymaster.IAP_PLATFORM_WIDGET_RED_VIOLET_CAKE ->
                    RedVioletCake(state, onRequest)

                Paymaster.IAP_COLOR_CROFT_ROTATING_GRADIENT ->
                    RotatingColorGradient(state, onRequest)

                Paymaster.IAP_PLATFORM_WIDGET_SKEWED_DYNAMIC ->
                    SkewedDynamic(state, onRequest)

                Paymaster.IAP_PLATFORM_WIDGET_SNOW_CONE -> SnowCone(state, onRequest)
                Paymaster.IAP_PLATFORM_WIDGET_TIRAMISU -> Tiramisu(state, onRequest)
                Paymaster.IAP_COLOR_CROFT_WAVY_GRADIENT_DOTS ->
                    WavyGradientDots(state, onRequest)
            }
        }
        // Indicator

        // BottomBar
        val facade = LocalSystemFacade.current
        val activity =  facade as? MainActivity ?: return@Column
        val paymaster = activity.paymaster
        val widget: Purchasable? by produceState(null, pagerController.currentPage) {
            val key = WIDGETS[pagerController.currentPage]
            paymaster.purchases.combine(paymaster.details) { purchases, details ->
                val purchsae = purchases.find { it.id == key }
                val info = details.find { it.id == key }
                purchsae to info
            }.collect { value = it }
        }
        val bundle: Purchasable? by produceState(null, WIDGETS[pagerController.currentPage]) {
            val key = bundleKeyOf(WIDGETS[pagerController.currentPage])
            paymaster.purchases.combine(paymaster.details) { purchases, details ->
                val purchsae = purchases.find { it.id == key }
                val info = details.find { it.id == key }
                purchsae to info
            }.collect { value = it }
        }

        TopAppBar(
            shape = CircleShape,
            elevation = 8.dp,
            modifier = Modifier.padding(top = ContentPadding.medium),
            border = AppTheme.colors.shine,
            background = AppTheme.colors.background(surface),
            windowInsets = WindowInsets.None,
            title = {
                Column {
                    Label(widget?.second?.title ?: "", style = AppTheme.typography.label3)
                    Label(
                        bundle?.second?.title ?: "",
                        style = AppTheme.typography.title3
                    )
                }
            },
            navigationIcon = {
                IconButton(
                    Icons.Outlined.Info,
                    contentDescription = null,
                    modifier = Widget.SmallIconBtn,
                    onClick = {
                        val info = bundle?.second?.richDesc
                        if (info != null)
                            activity.showSnackbar(
                                info,
                                icon = Icons.Outlined.Info,
                                duration = SnackbarDuration.Indefinite
                            )
                    }
                )
            },
            actions = {
                SplitButtonLayout(
                    modifier = Modifier.scale(0.85f),
                    leadingButton = {
                        val (purchase, info) = widget ?: return@SplitButtonLayout
                        val key = WIDGETS[pagerController.currentPage]
                        // An item is considered unlocked and available for use if any of the following conditions are met:
                        // 1. It is a freemium item.
                        // 2. The user has purchased the entire bundle to which this item belongs.
                        // 3. The user has specifically purchased this individual item.
                        val unlocked =
                            purchase.purchased || info?.isFreemium == true || bundle?.first?.purchased == true
                        Button(
                            if (unlocked) "APPLY" else info?.formattedPrice ?: "",
                            onClick = {
                                if (!unlocked)
                                    activity.initiatePurchaseFlow(key)
                                // else show apply.
                            },
                            enabled = key != selected
                        )
                    },
                    trailingButton = {
                        val (purchase, _) = bundle ?: return@SplitButtonLayout
                        val key = bundleKeyOf(WIDGETS[pagerController.currentPage])
                        TrailingSplitButton(
                            shapes = AppTheme.shapes.small to AppTheme.shapes.large,
                            content = {
                                Icon(
                                    Icons.Outlined.AddShoppingCart,
                                    contentDescription = null
                                )
                            },
                            onClick = {
                                if (!purchase.purchased)
                                    activity.initiatePurchaseFlow(key)
                                else
                                    activity.showToast(R.string.msg_settings_upgrade_unlocked)
                            },
                        )
                    }
                )
            }
        )

        // The DisposableEffect is used here to manage the visibility of the app's navigation bar
        // when this composable enters or leaves the composition.
        DisposableEffect(Unit) {
            // When the composable is first displayed (or when the effect is launched),
            // this code hides the app's navigation bar by updating the window style.
            facade.style = facade.style + WindowStyle.FLAG_APP_NAV_BAR_HIDDEN
            // The onDispose block is executed when the composable is removed from the screen
            // (e.g., when the user navigates away).
            onDispose {
                // This restores the default behavior of the navigation bar, making it visible again.
                facade.style = facade.style + WindowStyle.FLAG_APP_NAV_BAR_VISIBILITY_AUTO
            }
        }
    }
}
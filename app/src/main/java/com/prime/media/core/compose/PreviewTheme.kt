/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 20-01-2024.
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

package com.prime.media.core.compose

import android.content.Intent
import android.os.Bundle
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationConstants
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material.Colors
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Typography
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import androidx.navigation.compose.rememberNavController
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.prime.media.settings.Settings
import com.primex.core.BlueLilac
import com.primex.core.DahliaYellow
import com.primex.core.OrientRed
import com.primex.core.SignalWhite
import com.primex.core.Text
import com.primex.core.TrafficBlack
import com.primex.core.UmbraGrey
import com.primex.core.hsl
import com.primex.preferences.Key
import com.primex.preferences.Preferences
import com.primex.preferences.observeAsState
import kotlinx.coroutines.flow.MutableStateFlow

private class FakeSystemFacade(private val prefs: Preferences) : SystemFacade {
    override val inAppUpdateProgress: Float = Float.NaN

    override fun showAd(force: Boolean) {
        error("showAd Not Supported")
    }

    override fun launch(intent: Intent, options: Bundle?) {
        error("showAd Not Supported")
    }

    override val inAppProductDetails: MutableStateFlow<Map<String, ProductDetails>>
        = MutableStateFlow(emptyMap())

    override fun show(
        message: Text,
        title: Text?,
        action: Text?,
        icon: Any?,
        accent: Color,
        duration: Channel.Duration,
        onAction: (() -> Unit)?
    ) {
        error("show Not Supported")
    }

    override fun launchUpdateFlow(report: Boolean) {
        error("launchUpdateFlow Not Supported")
    }

    override fun launchReviewFlow() {
        error("launchReviewFlow Not Supported")
    }

    override fun launchAppStore() {
        error("launchAppStore Not Supported")
    }

    @Composable
    override fun <S, O> observeAsState(key: Key.Key1<S, O>): State<O?> =
        prefs.observeAsState(key = key)

    @Composable
    override fun <S, O> observeAsState(key: Key.Key2<S, O>): State<O> =
        prefs.observeAsState(key = key)

    @Composable
    override fun observeAsState(product: String): State<Purchase?> =
        remember {
            derivedStateOf { Purchase("Empty String", "") }
        }

    override fun launchBillingFlow(id: String) {
        error("launchBillingFlow Not Supported")
    }

    override fun launchEqualizer(id: Int) {
        error("launchEqualizer Not Supported")
    }

    override fun shareApp() {
        error("shareApp Not Supported")
    }
}

private val LightPrimaryColor = Color.Black
private val LightPrimaryVariantColor = LightPrimaryColor.hsl(lightness = 0.25f)
private val LightSecondaryColor = Color.BlueLilac
private val LightSecondaryVariantColor = LightSecondaryColor.hsl(lightness = 0.2f)
private val DarkPrimaryColor = /*Color(0xFFff8f00)*/ Color(0xFFFF3D00)
private val DarkPrimaryVariantColor = /*Color.Amber*/ DarkPrimaryColor.hsl(lightness = 0.6f)
private val DarkSecondaryColor = Color.DahliaYellow
private val DarkSecondaryVariantColor = Color(0xFFf57d00)


/**
 * Applies a preview-specific theme for debugging and testing purposes.
 *
 * This composable provides a controlled environment for previewing components by injecting
 * fake composition locals, such as [FakeSystemFacade], that simulate different system states.
 *
 * @param darkTheme Whether to apply a dark theme for previews.
 * @param content The composable content to be rendered within the preview theme.
 */
@Composable
fun PreviewTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    BoxWithConstraints() {
        val size = DpSize(this.maxWidth, this.maxHeight)
        val windowSizeClass = WindowSize(size)
        val nav = rememberNavController()
        val context = LocalContext.current
        val prefs = remember {
            Preferences(context)
        }
        CompositionLocalProvider(
            LocalSystemFacade provides FakeSystemFacade(prefs),
            LocalWindowSize provides windowSizeClass,
            LocalNavController provides nav
        ) {
            val background by animateColorAsState(
                targetValue = if (darkTheme) Color(0xFF0E0E0F) else Color(0xFFF5F5FA),
                animationSpec = tween(AnimationConstants.DefaultDurationMillis)
            )
            val surface by animateColorAsState(
                targetValue = if (darkTheme) Color.TrafficBlack else Color.White,
                animationSpec = tween(AnimationConstants.DefaultDurationMillis)
            )
            val primary = if (darkTheme) DarkPrimaryColor else LightPrimaryColor
            val primaryVariant =
                if (darkTheme) DarkPrimaryVariantColor else LightPrimaryVariantColor
            val secondary = if (darkTheme) DarkSecondaryColor else LightSecondaryColor
            val secondaryVariant =
                if (darkTheme) DarkSecondaryVariantColor else LightSecondaryVariantColor
            val colors = Colors(
                primary = primary,
                secondary = secondary,
                background = background,
                surface = surface,
                primaryVariant = primaryVariant,
                secondaryVariant = secondaryVariant,
                onPrimary = Color.SignalWhite,
                onSurface = if (darkTheme) Color.SignalWhite else Color.UmbraGrey,
                onBackground = if (darkTheme) Color.SignalWhite else Color.UmbraGrey,
                error = Color.OrientRed,
                onSecondary = Color.SignalWhite,
                onError = Color.SignalWhite,
                isLight = !darkTheme
            )
            // Actual theme compose; in future handle fonts etc.
            MaterialTheme(
                colors = colors,
                content = content,
                typography = Typography(Settings.DefaultFontFamily)
            )
        }
    }
}
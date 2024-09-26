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

package com.prime.media.common

import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.DpSize
import androidx.navigation.compose.rememberNavController
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.zs.core_ui.LocalWindowSize
import com.zs.core_ui.WindowSize
import com.primex.core.BlueLilac
import com.primex.core.DahliaYellow
import com.primex.core.hsl
import com.primex.preferences.Key
import com.primex.preferences.Preferences
import com.primex.preferences.observeAsState
import com.zs.ads.AdSize
import com.zs.core_ui.AppTheme
import kotlinx.coroutines.flow.MutableStateFlow

private class FakeSystemFacade(private val prefs: Preferences) : SystemFacade {
    override val inAppUpdateProgress: Float = Float.NaN
    override val adFreePeriodEndTimeMillis: Long
        get() = TODO("Not yet implemented")
    override val isAdFree: Boolean
        get() = TODO("Not yet implemented")
    override val isAdFreeRewarded: Boolean
        get() = TODO("Not yet implemented")
    override val isAdFreeVersion: Boolean
        get() = TODO("Not yet implemented")
    override val isRewardedVideoAvailable: Boolean
        get() = TODO("Not yet implemented")
    override fun showRewardedVideo() {
        TODO("Not yet implemented")
    }

    override fun showAd(force: Boolean) {
        error("showAd Not Supported")
    }

    override fun launch(intent: Intent, options: Bundle?) {
        error("showAd Not Supported")
    }

    override val inAppProductDetails: MutableStateFlow<Map<String, ProductDetails>>
        = MutableStateFlow(emptyMap())

    override fun show(message: CharSequence, icon: ImageVector?, accent: Color, duration: Int) {
        TODO("Not yet implemented")
    }

    override fun show(message: Int, icon: ImageVector?, accent: Color, duration: Int) {
        TODO("Not yet implemented")
    }

    override fun launchUpdateFlow(report: Boolean) {
        error("launchUpdateFlow Not Supported")
    }

    override fun launchReviewFlow() {
        error("launchReviewFlow Not Supported")
    }

    override fun launchAppStore(id: String) {
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

    override fun loadBannerAd(size: AdSize) {
        error("loadBannerAd Not Supported")
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
    BoxWithConstraints {
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
            val primary = if (darkTheme) DarkPrimaryColor else LightPrimaryColor
            // Actual theme compose; in future handle fonts etc.
            AppTheme(
                accent = primary,
                isLight = !darkTheme,
                content = content,
            )
        }
    }
}
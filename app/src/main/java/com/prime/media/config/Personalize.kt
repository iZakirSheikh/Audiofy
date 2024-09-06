/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 26-08-2024.
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

@file:Suppress("NOTHING_TO_INLINE")

package com.prime.media.config

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.RadioButton
import androidx.compose.material.Scaffold
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ReplyAll
import androidx.compose.material.icons.twotone.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.android.billingclient.api.ProductDetails
import com.google.firebase.crashlytics.internal.model.CrashlyticsReport.Session.Event.Application.ProcessDetails
import com.prime.media.BuildConfig
import com.prime.media.Material
import com.prime.media.R
import com.prime.media.backgroundColorAtElevation
import com.prime.media.core.ContentPadding
import com.prime.media.core.billing.purchased
import com.prime.media.core.compose.Channel
import com.prime.media.core.compose.Header
import com.prime.media.core.compose.LocalNavController
import com.prime.media.core.compose.LocalSharedTransitionScope
import com.prime.media.core.compose.LocalSystemFacade
import com.prime.media.core.compose.preference
import com.prime.media.core.compose.purchase
import com.prime.media.core.playback.MediaItem
import com.prime.media.settings.Settings
import com.prime.media.small2
import com.prime.media.widget.Iphone
import com.prime.media.widget.RedVelvetCake
import com.prime.media.widget.SnowCone
import com.prime.media.widget.Tiramisu
import com.primex.core.textResource
import com.primex.core.withParagraphStyle
import com.primex.core.withSpanStyle
import com.primex.material2.Button
import com.primex.material2.IconButton
import com.primex.material2.Label
import com.primex.material2.OutlinedButton
import com.primex.material2.dialog.TextInputDialog

private val WIDGET_MAX_WIDTH = 400.dp

/**
 * A sample media item used for testing and demonstration purposes.
 */
private val SampleMediaItem = MediaItem(
    Uri.EMPTY,
    "Sample Title",
    "Sample Artist",
    artwork = Uri.parse("https://upload.wikimedia.org/wikipedia/en/c/c0/LetitbleedRS.jpg")
)

@Composable
@NonRestartableComposable
private fun AppBarTop(modifier: Modifier = Modifier) {
    TopAppBar(
        title = {
            Label(
                text = textResource(R.string.scr_personalize_title_desc),
                maxLines = 2
            )
        },
        windowInsets = WindowInsets.statusBars,
        navigationIcon = {
            val navController = LocalNavController.current
            IconButton(
                imageVector = Icons.Outlined.ReplyAll,
                onClick = navController::navigateUp
            )
        },
        actions = {
            val facade = LocalSystemFacade.current
            IconButton(
                Icons.TwoTone.Info,
                onClick = {
                    facade.show(
                        R.string.msg_scr_personalize_customize_everywhere,
                        duration = Channel.Duration.Indefinite
                    )
                }
            )
        },
        backgroundColor = Material.colors.backgroundColorAtElevation(1.dp),
        contentColor = Material.colors.onBackground,
        elevation = 0.dp,
        modifier = modifier
    )
}

private val Skins = mapOf(
    BuildConfig.IAP_WIDGETS_PLATFORM to listOf(
        BuildConfig.IAP_PLATFORM_WIDGET_IPHONE,
        BuildConfig.IAP_PLATFORM_WIDGET_RED_VIOLET_CAKE,
        BuildConfig.IAP_PLATFORM_WIDGET_SNOW_CONE,
        BuildConfig.IAP_PLATFORM_WIDGET_TIRAMISU
    )
)

@Composable
private inline fun ColumnScope.Retrofit(
    current: String,
    isGroupUnlocked: Boolean,
    values: List<ProductDetails>,
    crossinline onRequestApply: (id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val facade = LocalSystemFacade.current
    values.forEach { detail ->
        val id = detail.productId
        val purchase by purchase(id)
        val isApplied = id == current
        val isDebugable = BuildConfig.DEBUG
        when (id) {
            BuildConfig.IAP_PLATFORM_WIDGET_IPHONE -> Iphone(item = SampleMediaItem, modifier)
            BuildConfig.IAP_PLATFORM_WIDGET_RED_VIOLET_CAKE ->
                RedVelvetCake(item = SampleMediaItem, modifier)

            BuildConfig.IAP_PLATFORM_WIDGET_SNOW_CONE -> SnowCone(item = SampleMediaItem, modifier)
            BuildConfig.IAP_PLATFORM_WIDGET_TIRAMISU -> Tiramisu(item = SampleMediaItem, modifier)
        }

        // buttons
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = ContentPadding.large),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            content = {
                IconButton(
                    Icons.Outlined.Info,
                    onClick = {
                        facade.show(
                            message = buildAnnotatedString {
                                withSpanStyle(fontWeight = FontWeight.Bold) {
                                    appendLine(detail.name)
                                }
                                withStyle(SpanStyle(color = Color.Gray, fontSize = 13.sp)) {
                                    append(detail.description)
                                }
                            }
                        )
                    }
                )

                if (purchase.purchased || isGroupUnlocked || isApplied || isDebugable)
                    RadioButton(isApplied, onClick = { onRequestApply(id) }, enabled = !isApplied)
                else
                // get this for below price.
                    Button(
                        label = "Get - ${detail.oneTimePurchaseOfferDetails?.formattedPrice}",
                        onClick = { facade.launchBillingFlow(id) },
                    )
            }
        )
    }
}


@Composable
fun Personalize(viewState: PersonalizeViewState) {
    Scaffold(
        topBar = { AppBarTop() },
        content = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(ContentPadding.medium),
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = ContentPadding.large, vertical = ContentPadding.normal)
                    .padding(it)
                    .fillMaxSize()
            ) {
                val facade = LocalSystemFacade.current
                val details by facade.inAppProductDetails.collectAsState()
                val purchase by purchase(BuildConfig.IAP_WIDGETS_PLATFORM)
                val current by preference(Settings.GLANCE)
                Skins.forEach { (bundle, list) ->
                    val detail = details[bundle] ?: return@forEach
                    val children = list.mapNotNull { details[it] }
                    Header(
                        text = textResource(R.string.scr_personalize_title_legacy_widgets),
                        contentPadding = PaddingValues(vertical = ContentPadding.medium),
                        style = Material.typography.h6,
                        leading = {
                            IconButton(
                                Icons.Outlined.Info,
                                onClick = {
                                    facade.show(
                                        message = buildAnnotatedString {
                                            withSpanStyle(fontWeight = FontWeight.Bold) {
                                                appendLine(detail.name)
                                            }
                                            withStyle(SpanStyle(color = Color.Gray, fontSize = 13.sp)) {
                                                append(detail.description)
                                            }
                                        }
                                    )
                                }
                            )
                        }
                    ) {
                        if (!purchase.purchased)
                            OutlinedButton(
                                label = "Get - ${detail.oneTimePurchaseOfferDetails?.formattedPrice}",
                                onClick = { facade.launchBillingFlow(bundle) },
                                colors = ButtonDefaults.outlinedButtonColors(
                                    backgroundColor = Color.Transparent,
                                ),
                                shape = Material.shapes.small2,
                                border = ButtonDefaults.outlinedBorder
                            )
                    }

                    Retrofit(
                        current = current,
                        isGroupUnlocked = purchase.purchased,
                        values = children,
                        modifier = Modifier.widthIn(max = WIDGET_MAX_WIDTH),
                        onRequestApply = viewState::setInAppWidget
                    )
                }
            }
        }
    )
}
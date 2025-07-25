/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 10-05-2025.
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

package com.zs.audiofy.common

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import com.zs.core.billing.Paymaster
import com.zs.core.billing.Product

// Here extension methods to Paymaster are defined that are used within the app.
/**
 * Represents a array of valid product ids for a valid set of products.
 *
 * @property products An [Array] of valid product IDs that can be purchased through Google Play Billing.
 *                   These IDs correspond to product listings configured in the Google Play Console.
 *                   These product Ids are used to query for [Product] and to initiate purchases.
 */
val Paymaster.Companion.products get() = _products

/**
 * Returns a formatted [AnnotatedString] representation of the product description.
 *
 * This property formats the product information by displaying the title in bold
 * followed by the description on a new line. It uses an [AnnotatedString] for
 * richer text representation.
 *
 * @return An [AnnotatedString] containing the formatted product description.
 */
val Product.richDesc
    get() = buildAnnotatedString {
        withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { appendLine(title.ellipsize(22)) }
        withStyle(SpanStyle(Color.Gray)) { append(description) }
    }

val Paymaster.Companion.IAP_TAG_EDITOR_PRO get() = "tag_editor_pro"
val Paymaster.Companion.IAP_BUY_ME_COFFEE get() = "buy_me_a_coffee"
val Paymaster.Companion.IAP_CODEX get() = "buy_codex"
val Paymaster.Companion.IAP_NO_ADS get() = "disable_ads"

val Paymaster.Companion.IAP_WIDGETS_PLATFORM get() = "widgets_platform"
val Paymaster.Companion.IAP_PLATFORM_WIDGET_IPHONE get() = "platform_widget_iphone"
val Paymaster.Companion.IAP_PLATFORM_WIDGET_TIRAMISU get() = "platform_widget_tiramisu"
val Paymaster.Companion.IAP_PLATFORM_WIDGET_SNOW_CONE get() = "platform_widget_snow_cone"
val Paymaster.Companion.IAP_PLATFORM_WIDGET_RED_VIOLET_CAKE get() = "platform_widget_red_violet_cake"
val Paymaster.Companion.IAP_PLATFORM_WIDGET_DISK_DYNAMO get() = "platform_widget_disk_dynamo"
val Paymaster.Companion.IAP_PLATFORM_WIDGET_ELONGATE_BEAT get() = "platform_widget_elongate_beat"
val Paymaster.Companion.IAP_PLATFORM_WIDGET_SKEWED_DYNAMIC get() = "platform_widget_skewed_dynamic"

val Paymaster.Companion.IAP_COLOR_CROFT_WIDGET_BUNDLE get() = "aurora_widget_bundle"
val Paymaster.Companion.IAP_COLOR_CROFT_GRADIENT_GROVES get() = "color_craft_gradient_groves"
val Paymaster.Companion.IAP_COLOR_CROFT_GOLDEN_DUST get() = "color_craft_golden_dust"
val Paymaster.Companion.IAP_COLOR_CROFT_ROTATING_GRADIENT get() = "color_craft_rotating_gradient"
val Paymaster.Companion.IAP_COLOR_CROFT_WAVY_GRADIENT_DOTS get() = "color_croft_wavy_gradient_dots"
val Paymaster.Companion.IAP_COLOR_CROFT_MISTY_DREAM get() = "color_craft_misty_dream"

val Paymaster.Companion.IAP_ARTWORK_SHAPE_HEART get() = "artwork_shape_heart"
val Paymaster.Companion.IAP_ARTWORK_SHAPE_ROUNDED_RECT get() = "artwork_shape_rounded_rect"
val Paymaster.Companion.IAP_ARTWORK_SHAPE_CIRCLE get() = "artwork_shape_circle"
val Paymaster.Companion.IAP_ARTWORK_SHAPE_CUT_CORNERED_RECT get() = "artwork_shape_cut_cornored_rect"
val Paymaster.Companion.IAP_ARTWORK_SHAPE_SCOPED_RECT get() = "artwork_shape_scoped"
val Paymaster.Companion.IAP_ARTWORK_SHAPE_SKEWED_RECT get() = "artwork_shape_skewed_rect"
val Paymaster.Companion.IAP_ARTWORK_SHAPE_SQUIRCLE get() = "artwork_shape_squircle"
val Paymaster.Companion.IAP_ARTWORK_SHAPE_WAVY_CIRCLE get() = "artwork_shape_wavy_circle"
val Paymaster.Companion.IAP_ARTWORK_SHAPE_LEAF get() = "artwork_shape_leaf"
val Paymaster.Companion.IAP_ARTWORK_SHAPE_DISK get() = "artwork_shape_disk"
val Paymaster.Companion.IAP_ARTWORK_SHAPE_PENTAGON get() = "artwork_shape_pentagon"

private val _products = arrayOf(
    // General & utilities
    Paymaster.IAP_TAG_EDITOR_PRO,
    Paymaster.IAP_BUY_ME_COFFEE,
    Paymaster.IAP_CODEX,
    Paymaster.IAP_NO_ADS,

    // Widgets Platform Group
    Paymaster.IAP_WIDGETS_PLATFORM,
    Paymaster.IAP_PLATFORM_WIDGET_IPHONE,
    Paymaster.IAP_PLATFORM_WIDGET_TIRAMISU,
    Paymaster.IAP_PLATFORM_WIDGET_SNOW_CONE,
    Paymaster.IAP_PLATFORM_WIDGET_RED_VIOLET_CAKE,
    Paymaster.IAP_PLATFORM_WIDGET_DISK_DYNAMO,
    Paymaster.IAP_PLATFORM_WIDGET_ELONGATE_BEAT,
    Paymaster.IAP_PLATFORM_WIDGET_SKEWED_DYNAMIC,

    // ColorCroft Widget Bundle
    Paymaster.IAP_COLOR_CROFT_WIDGET_BUNDLE,
    Paymaster.IAP_COLOR_CROFT_GRADIENT_GROVES,
    Paymaster.IAP_COLOR_CROFT_GOLDEN_DUST,
    Paymaster.IAP_COLOR_CROFT_ROTATING_GRADIENT,
    Paymaster.IAP_COLOR_CROFT_WAVY_GRADIENT_DOTS,
    Paymaster.IAP_COLOR_CROFT_MISTY_DREAM,

    // Artwork Shapes
    Paymaster.IAP_ARTWORK_SHAPE_HEART,
    Paymaster.IAP_ARTWORK_SHAPE_ROUNDED_RECT,
    Paymaster.IAP_ARTWORK_SHAPE_CIRCLE,
    Paymaster.IAP_ARTWORK_SHAPE_CUT_CORNERED_RECT,
    Paymaster.IAP_ARTWORK_SHAPE_SCOPED_RECT,
    Paymaster.IAP_ARTWORK_SHAPE_SKEWED_RECT,
    Paymaster.IAP_ARTWORK_SHAPE_SQUIRCLE,
    Paymaster.IAP_ARTWORK_SHAPE_WAVY_CIRCLE,
    Paymaster.IAP_ARTWORK_SHAPE_LEAF,
    Paymaster.IAP_ARTWORK_SHAPE_DISK,
    Paymaster.IAP_ARTWORK_SHAPE_PENTAGON
)
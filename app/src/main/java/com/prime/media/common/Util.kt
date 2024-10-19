/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 10-10-2024.
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

package com.prime.media.common

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridItemSpanScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.prime.media.BuildConfig
import com.prime.media.old.common.util.PathUtils
import com.primex.core.withSpanStyle
import com.zs.core.paymaster.ProductInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.onEach
import kotlin.text.appendLine

/**
 * Creates a split install request for the module with [name]
 */
inline fun SplitInstallRequest(name: String) =
    SplitInstallRequest.newBuilder().addModule(name).build()

/**
 * Checks if the product represents a dynamic feature.
 */
val ProductInfo.isDynamicFeature
    inline get() = this.id == BuildConfig.IAP_CODEX

/**
 * Checks if a dynamic module with the given name is installed.
 *
 * @param id The name of the dynamic module.
 * @return True if the module is installed, false otherwise.
 */
fun SplitInstallManager.isInstalled(id: String): Boolean =
    installedModules.contains(id)

/**
 * Returns the name of the dynamic feature module associated with this product.
 *
 * This function maps product IDs to dynamic feature module names.
 * It throws an [IllegalArgumentException] if the product is not associated
 * with a dynamic feature module.
 *
 * @return The name of the dynamic feature module.
 * @throws IllegalArgumentException if the product is not a dynamic feature module.
 * @see isDynamicFeature
 */
val ProductInfo.dynamicModuleName
    inline get() = when (id) {
        BuildConfig.IAP_CODEX -> "codex"
        else -> error("$id is not a dynamic module.")
    }

/**
 * Creates a SplitInstallRequest for the dynamic feature associated with the product.
 */
val ProductInfo.dynamicFeatureRequest
    inline get() = SplitInstallRequest(dynamicModuleName)

/**
 * Utility function for [Flow] that processes each item in the emitted collections.
 *
 * This function iterates through each collection emitted by the Flow and applies the provided
 * `action` to each individual item within those collections.
 *
 * It essentially flattens the emitted collections of items into a stream of individual items
 * and then applies the `action` to each of them sequentially.
 *
 * @param T The type of the items in the collections emitted by the Flow.
 * @param action A suspending function that is executed for each item in the collections.
 *
 * @return A Flow that emits the original collections after the `action` has been applied
 *         to each item within them.
 */
inline fun <T> Flow<Iterable<T>>.onEachItem(crossinline action: suspend (T) -> Unit) = onEach {
    for (item in it)
        action(item)
}

/**
 * @see com.prime.media.old.core.playback.MediaItem
 */
fun MediaFile(context: Context, uri: Uri) =
    com.prime.media.old.core.playback.MediaItem(context, uri)

/**
 * Returns a formatted [AnnotatedString] representation of the product description.
 *
 * This property formats the product information by displaying the title in bold
 * followed by the description on a new line. It uses an [AnnotatedString] for
 * richer text representation.
 *
 * @return An [AnnotatedString] containing the formatted product description.
 */
val ProductInfo.richDesc
    get() = buildAnnotatedString {
        withSpanStyle(fontWeight = FontWeight.Bold) { appendLine(title.ellipsize(22)) }
        withSpanStyle(Color.Gray) { append(description) }
    }

private const val ELLIPSIS_NORMAL = "\u2026"; // HORIZONTAL ELLIPSIS (…)
/**
 * Ellipsizes this CharSequence, adding a horizontal ellipsis (…) if it is longer than [after] characters.
 *
 * @param after The maximum length of the CharSequence before it is ellipsized.
 * @return The ellipsized CharSequence.
 */
fun CharSequence.ellipsize(after: Int): CharSequence =
    if (this.length > after) this.substring(0, after) + ELLIPSIS_NORMAL else this

/**
 * @return - Indicates if the widget is free in the Play Console.
 * Some widgets are listed for providing localized names and descriptions,
 * but their price cannot be set to 0.
 *
 * **Note**: Developer need register any product for freemium
 */
val ProductInfo.isFreemium: Boolean
    get() {
        return when (id) {
            BuildConfig.IAP_PLATFORM_WIDGET_IPHONE, BuildConfig.IAP_COLOR_CROFT_GRADIENT_GROVES -> true
            else -> false
        }
    }

/**
 * Controls whether this item should be showcased for purchase.
 * Sometimes the group is not fully prepared and needs further improvements.
 * This variable can be used to control whether to showcase this item for purchase.
 */
val ProductInfo.isPurchasable: Boolean
    get() {
        return when (id) {
            BuildConfig.IAP_COLOR_CROFT_WIDGET_BUNDLE -> false
            else -> true
        }
    }

private val fullLineSpan: (LazyGridItemSpanScope.() -> GridItemSpan) = { GridItemSpan(maxLineSpan) }
val LazyGridScope.fullLineSpan
    get() = com.prime.media.common.fullLineSpan



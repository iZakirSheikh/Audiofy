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
import androidx.media3.common.MediaItem
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.prime.media.BuildConfig
import com.zs.core.paymaster.ProductInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach

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
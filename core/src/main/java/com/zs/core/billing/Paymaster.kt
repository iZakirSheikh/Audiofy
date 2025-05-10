
/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 01-10-2024.
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

package com.zs.core.billing

import android.app.Activity
import android.content.Context
import kotlinx.coroutines.flow.StateFlow

/**
 * Manages in-app purchases and provides access to product and purchase information.
 *
 * This interface defines methods for:
 *  * Initiating purchase flows.
 *  * Refreshing purchase data.
 *  * Releasing resources.
 *  * Accessing product and purchase details.
 *
 * @property purchases A [StateFlow] emitting a list of purchases the user has made.
 * @property details A [StateFlow] emitting a list of available products. This includes details for each product, such as whether it's an in-app purchase or subscription.
 */
interface Paymaster {

    companion object {
        /** Creates an instance of [Paymaster]. */
        operator fun invoke(context: Context, securityKey: String, products: Array<String>): Paymaster =
            PaymasterImpl(context, securityKey, products)
    }

    val purchases: StateFlow<List<Purchase>>
    val details: StateFlow<List<Product>>

    /**
     * Refreshes the product and purchase data.
     *
     * This method ensures the `Paymaster` has the latest information about products and purchases.
     * It may involve:
     *  * Fetching updated product details.
     *  * Retrieving the latest purchase statuses.
     *  * Synchronizing data with a backend server.
     */
    fun sync()

    /**
     * Initiates the purchase flow for a specific product.
     *
     * @param activity The [Activity] used to launch the purchase flow.
     * @param productId The ID of the product to purchase.
     *
     * @return `true` if the purchase flow was launched successfully, `false` otherwise.
     */
    fun initiatePurchaseFlow(activity: Activity, productId: String): Boolean

    /**
     * Releases resources held by this `Paymaster`.
     *
     * Call this method when the `Paymaster` is no longer needed to prevent memory leaks
     * and ensure proper cleanup. This might involve:
     *  * Unregistering listeners.
     *  * Closing connections.
     *  * Releasing any other acquired resources.
     */
    fun release()

    /**
     * Retrieves information about a specific product, including its details
     * and purchase information.
     *
     * @param id The ID of the product to retrieve.
     * @return A [Pair] containing the [ProductInfo] and an optional [Purchase]
     *   object if the product has been purchased, or `null` if the product
     *   is not found.
     */
    operator fun get(id: String): Pair<Product, Purchase?>?{
        val info = details.value.find { it.id == id } ?: return null
        val purchase = purchases.value.find { it.id == id }
        return info to purchase
    }
}

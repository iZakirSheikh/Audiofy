/*
 * Copyright 2025 sheik
 *
 * Created by sheik on 01-05-2025.
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

import com.android.billingclient.api.ProductDetails

/**
 * Represents a product.
 *
 * @property value The underlying [ProductDetails] object.
 * @property title The title of the product.
 * @property description The description of the product.
 * @property formattedPrice The formatted price of the product, or null if not available.
 * @property id The ID of the product.
 */
@JvmInline
value class Product internal constructor(internal val value: ProductDetails) {
    val title get() = value.title.trim()
    val description get() = value.description.trim()
    val formattedPrice get() = value.oneTimePurchaseOfferDetails?.formattedPrice?.trim()
    val id get() = value.productId
}
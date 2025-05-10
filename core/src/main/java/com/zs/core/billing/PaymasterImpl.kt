/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 02-10-2024.
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
import android.util.Log
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase.PurchaseState
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.acknowledgePurchase
import com.android.billingclient.api.queryProductDetails
import com.android.billingclient.api.queryPurchasesAsync
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlin.math.min
import com.android.billingclient.api.AcknowledgePurchaseParams.newBuilder as AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient.BillingResponseCode as R
import com.android.billingclient.api.BillingClient.newBuilder as BillingClient
import com.android.billingclient.api.BillingFlowParams.ProductDetailsParams.newBuilder as ProductDetailsParams
import com.android.billingclient.api.BillingFlowParams.newBuilder as BillingFlowParams
import com.android.billingclient.api.Purchase as ApiPurchase
import com.android.billingclient.api.QueryProductDetailsParams.Product.newBuilder as Product
import com.android.billingclient.api.QueryProductDetailsParams.newBuilder as QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams.newBuilder as QueryPurchasesParams

private const val RECONNECT_TIMER_START_MILLISECONDS = 1L * 1000L
private const val RECONNECT_TIMER_MAX_TIME_MILLISECONDS = 1000L * 60L * 15L // 15 minutes

private const val TAG = "PaymasterImpl"

internal class PaymasterImpl(
    context: Context,
    securityKey: String,
    val products: Array<String>,
) : Paymaster, PurchasesUpdatedListener, BillingClientStateListener {
    private val scope = CoroutineScope(Dispatchers.IO)
    private var delayReconnectMills = RECONNECT_TIMER_START_MILLISECONDS
    private val inAppBilling =
        BillingClient(context).setListener(this).enablePendingPurchases().build()

    override val purchases = MutableStateFlow(emptyList<Purchase>())
    override val details = MutableStateFlow(emptyList<Product>())

    init {
        Security.BASE_64_ENCODED_PUBLIC_KEY = securityKey
        inAppBilling.startConnection(this)
    }

    /**
     * Retries the billing service connection with exponential backoff, maxing out at the time
     * specified by [RECONNECT_TIMER_MAX_TIME_MILLISECONDS].
     */
    private fun reconnect() {
        scope.launch(Dispatchers.Main) {
            val delay = delayReconnectMills
            // compute next delay
            delayReconnectMills =
                min(delayReconnectMills * 2, RECONNECT_TIMER_MAX_TIME_MILLISECONDS)
            delay(delay)
            inAppBilling.startConnection(this@PaymasterImpl)
        }
    }

    override fun onBillingServiceDisconnected() = reconnect()
    override fun sync() {
        scope.launch { purchases.value = sync(null) }
    }

    override fun release() {
        Log.i(TAG, "Terminating connection")
        inAppBilling.endConnection()
        scope.cancel("releasing Paymaster and hence cancelling scope.")
    }

    /**
     * Synchronizes purchases and acknowledges any unacknowledged purchases.
     *
     * This method retrieves the user's purchase history and updates the [Paymaster.purchases]
     * state flow. If a list of `ApiPurchase` objects is provided, it uses that list instead of
     * querying the billing client.
     *
     * For each purchase, it verifies the signature and acknowledges any unacknowledged purchases
     * that are in the `PURCHASED` state.
     *
     * **Note:** Acknowledging purchases is crucial to prevent automatic refunds after three days.
     *
     * @param purchases An optional list of [ApiPurchase] objects. If `null`, the method will query
     * the billing client for the purchase history.
     * @return the list of processed purchases.
     */
    private suspend fun sync(value: List<ApiPurchase>? = null): List<Purchase> {
        // Check if the billing client is ready.
        if (!inAppBilling.isReady) {
            Log.e(TAG, "syncPurchases: BillingClient is not ready")
            reconnect()
            return emptyList()
        }
        // If purchases are not provided
        // Get the list of purchases from the billing client.
        val purchases = value ?: let {
            // for-now we only support in-App
            val (result, response) = inAppBilling.queryPurchasesAsync(
                QueryPurchasesParams().setProductType(ProductType.INAPP).build()
            )
            if (result.responseCode != R.OK) {
                Log.i(TAG, "query: ${result}}")
                return emptyList()
            }
            response
        }
        // Process each purchase.
        for (purchase in purchases) {
            // Note: You should acknowledge a purchase only when the state is PURCHASED, i.e. Do not
            // acknowledge it while a purchase is in PENDING state. The three day acknowledgement
            // window begins only when the purchase state transitions from 'PENDING' to 'PURCHASED'.
            if (purchase.purchaseState != PurchaseState.PURCHASED) continue
            // Acknowledge the purchase if it hasn't been acknowledged yet.
            // Unacknowledged purchases are automatically refunded after 3 days and revoked by Google Play.
            if (purchase.isAcknowledged) continue
            // **Security Check (Ideally performed on a secure server):**
            // Verify the purchase signature to prevent fraudulent transactions.
            // Ensure your public key is correctly configured.
            if (!Security.verifyPurchase(purchase.originalJson, purchase.signature)) {
                Log.e(TAG, "Invalid signature. Make sure your public key is correct.")
                continue
            }
            // Acknowledge the purchase with Google Play.
            val result = inAppBilling.acknowledgePurchase(
                AcknowledgePurchaseParams().setPurchaseToken(purchase.purchaseToken).build()
            )
            if (result.responseCode != R.OK)
                Log.e(TAG, "acknowledgePurchase: $result")
        }
        // Return the list of processed purchases.
        return purchases.map(::Purchase)
    }

    override fun onBillingSetupFinished(p0: BillingResult) {
        // Check if the billing setup was successful.
        // If setup failed, try to reconnect to the billing client.
        if (p0.responseCode != R.OK) {
            Log.i(TAG, p0.debugMessage)
            reconnect()
            return
        }
        //
        scope.launch {
            // Update purchases async.
            purchases.value = async { sync(null) }.await()
            // Build the query parameters for the products.
            val ofProducts = products.map {
                Product().setProductId(it)
                    .setProductType(ProductType.INAPP)
                    .build()
            }
            // Query the billing client for product details.
            val (result, list) = inAppBilling.queryProductDetails(
                QueryProductDetailsParams().setProductList(ofProducts).build()
            )
            // Check if the query was successful.
            if (result.responseCode != R.OK) {
                Log.i(TAG, "query: ${result}}")
                return@launch
            }
            // Check if the product details list is empty.
            // This might indicate that the products are not correctly configured
            // in the Google Play Console.
            if (list.isNullOrEmpty()) {
                Log.e(TAG, "onProductDetailsResponse: Found null or empty ProductDetails.")
                return@launch
            }
            // Update the product details state flow with the retrieved product information.
            details.value = list.map(::Product)
        }
    }

    override fun onPurchasesUpdated(p0: BillingResult, p1: MutableList<ApiPurchase>?) {
        when (p0.responseCode) {
            R.OK -> {
                if (p1.isNullOrEmpty()) {
                    Log.d(TAG, "Null Purchase List Returned from OK response!")
                    return
                }
                // process the purchases.
                scope.launch { purchases.value = sync(p1) }
            }

            R.USER_CANCELED -> Log.i(TAG, "onPurchasesUpdated: User canceled the purchase")
            R.ITEM_ALREADY_OWNED -> Log.i(
                TAG,
                "onPurchasesUpdated: The user already owns this item"
            )
            // Developer error means that Google Play
            // does not recognize the configuration. If you are just getting started
            // make sure you have configured the application correctly in the
            // Google Play Console. The SKU product ID must match and the APK you
            // are using must be signed with release keys.
            R.DEVELOPER_ERROR -> Log.e(TAG, "onPurchasesUpdated: Developer error")
            else -> Log.d(TAG, "BillingResult [" + p0.responseCode + "]: " + p0.debugMessage)
        }
    }

    override fun initiatePurchaseFlow(activity: Activity, productId: String): Boolean {
        // Find the product details for the given product ID.
        val details = details.value.firstOrNull { it.id == productId } ?: return false
        // Build the billing flow parameters.
        val ofProducts = listOf(
            ProductDetailsParams().setProductDetails(details.value)
                .build()
        )
        val params = BillingFlowParams().setProductDetailsParamsList(ofProducts).build()
        // Launch the billing flow.
        val result = inAppBilling.launchBillingFlow(activity, params)
        // Check the result of the billing flow launch.
        return when (result.responseCode) {
            // Billing flow launched successfully.
            R.OK -> true
            else -> {
                // Billing flow launch failed.
                Log.e(TAG, "Billing failed: + " + result.debugMessage)
                false
            }
        }
    }
}
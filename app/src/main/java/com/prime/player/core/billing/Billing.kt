package com.prime.player.core.billing

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.*
import com.android.billingclient.api.*
import com.prime.player.BuildConfig
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.lang.Long.min
import kotlin.coroutines.EmptyCoroutineContext

private const val TAG = "BillingManager"


private const val RECONNECT_TIMER_START_MILLISECONDS = 1L * 1000L
private const val RECONNECT_TIMER_MAX_TIME_MILLISECONDS = 1000L * 60L * 15L // 15 minutes
private const val SKU_DETAILS_REQUERY_TIME = 1000L * 60L * 60L * 4L // 4 hours

interface BillingManager {

    /**
     * The list of items that the user have purchased.
     */
    val purchases: MutableStateFlow<List<Purchase>>

    /**
     * The [ProductDetails] mapped with their product Ids.
     */
    val details: MutableStateFlow<Map<String, ProductDetails>>

    /**
     * Launch the billing flow. This will launch an external Activity for a result, so it requires
     * an Activity reference. For subscriptions, it supports upgrading from one SKU type to another
     * by passing in SKUs to be upgraded.
     *
     * @param activity active activity to launch our billing flow from
     * @param sku SKU (Product ID) to be purchased
     * @param uSKUS SKUs that the subscription can be upgraded from
     * @return true if launch is successful
     */
    fun launchBillingFlow(
        activity: Activity, product: String, vararg uSKUS: String
    ): Boolean

    /**
     * Needs to be called by the parent to run and refresh the code.
     */
    fun refresh()

    /**
     * Releases resources associated with this object.
     *
     * You must call this method once the instance is no longer required.
     */
    fun release()
}

/**
 * A one stop solution for monetization and Billing.
 * @param products The list of in-app product ids.
 * @param subscriptions The list of in-app subscription Ids.
 *
 * @author Zakir Ahmad Sheikh
 * @since 18-08-2022
 */

fun BillingManager(
    context: Context,
    products: Array<String>? = null,
    // TODO: Implement in future version of BillingManger
    subscriptions: Array<String>? = null,
): BillingManager = object : BillingManager, PurchasesUpdatedListener, BillingClientStateListener {

    private val ref = this

    // how long before the data source tries to reconnect to Google play
    private var delayReconnectMills = RECONNECT_TIMER_START_MILLISECONDS

    private val products = products?.map { product ->
        QueryProductDetailsParams.Product.newBuilder().setProductId(product)
            .setProductType(BillingClient.ProductType.INAPP).build()
    }

    private val subscriptions = products?.map { product ->
        QueryProductDetailsParams.Product.newBuilder().setProductId(product)
            .setProductType(BillingClient.ProductType.SUBS).build()
    }

    /**
     * The list of items that the user have purchased.
     */
    private val _purchases = MutableStateFlow<List<Purchase>>(emptyList())

    /**
     * @see _purchases
     */
    override val purchases get() = _purchases


    /**
     * The [ProductDetails] mapped with their product Ids.
     */
    private val _details = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())

    /**
     * @see _details
     */
    override val details get() = _details

    /**
     * A [CoroutineScope] to handle async tasks.
     */
    private val billingManagerScope = CoroutineScope(Dispatchers.IO)

    private val mBillingClient =
        BillingClient.newBuilder(context).setListener(this).enablePendingPurchases().build()

    init {
        // requires not everything to be null or empty.
        require(!products.isNullOrEmpty() || !subscriptions.isNullOrEmpty())
        mBillingClient.startConnection(this)

    }

    override fun onPurchasesUpdated(
        result: BillingResult, purchases: MutableList<Purchase>?
    ) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (purchases.isNullOrEmpty()) {
                    Log.d(TAG, "Null Purchase List Returned from OK response!")
                    return
                }
                // process the purchases.
                process(purchases)
                _purchases.value = purchases
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> Log.i(
                TAG,
                "onPurchasesUpdated: User canceled the purchase"
            )
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> Log.i(
                TAG,
                "onPurchasesUpdated: The user already owns this item"
            )
            BillingClient.BillingResponseCode.DEVELOPER_ERROR -> Log.e(
                TAG,
                "onPurchasesUpdated: Developer error means that Google Play " + "does not recognize the configuration. If you are just getting started, " + "make sure you have configured the application correctly in the " + "Google Play Console. The SKU product ID must match and the APK you " + "are using must be signed with release keys."
            )
            else -> Log.d(
                TAG, "BillingResult [" + result.responseCode + "]: " + result.debugMessage
            )
        }
    }

    /**
     * This is a pretty unusual occurrence. It happens primarily if the Google Play Store
     * self-upgrades or is force closed.
     */
    override fun onBillingServiceDisconnected() = reconnect()

    override fun onBillingSetupFinished(result: BillingResult) {
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.i(TAG, result.debugMessage)
            reconnect()
            return
        }
        // The billing client is ready. You can query purchases here.
        // This doesn't mean that your app is set up correctly in the console -- it just
        // means that you have a connection to the Billing service.
        billingManagerScope.launch {
                val purchases = async { query(BillingClient.ProductType.INAPP) }
                val response = purchases.await()
                // process them.
                process(response)
                _purchases.value = response

                // details
                val details = async { query(ref.products) }
                _details.value = details.await().associateBy { it.productId }
            }
    }


    override fun refresh() {
        Log.d(TAG, "refresh: ")
        // this just avoids an extra purchase refresh after we finish a billing flow
        if (mBillingClient.isReady) {
            billingManagerScope.launch {
                // refresh.
                val purchases = query(BillingClient.ProductType.INAPP)
                process(purchases)
                _purchases.value = purchases
            }
        }
    }

    override fun release() {
        Log.i(TAG, "Terminating connection")
        mBillingClient.endConnection()
        billingManagerScope.cancel("destroying BillingManager")
    }


    /**
     * Simple Utility function does is process the [purchases] and acknowledges them
     */
    private fun process(purchases: List<Purchase>) {
        billingManagerScope.launch {
            for (purchase in purchases) {
                // Global check to make sure all purchases are signed correctly.
                // This check is best performed on your server.
                val state = purchase.purchaseState
                if (state == Purchase.PurchaseState.PURCHASED) {
                    if (!isSignatureValid(purchase)) {
                        Log.e(TAG, "Invalid signature. Make sure your public key is correct.")
                        continue
                    }
                }
                if (!purchase.isAcknowledged) {
                    acknowledge(purchase)
                }
            }
        }
    }

    /**
     * Returns purchases details for currently owned items bought within your app.
     * Only active subscriptions and non-consumed one-time purchases are returned.
     * This method uses a cache of Google Play Store app without initiating a network request.
     *
     * Note:
     * * It's recommended for security purposes to go through purchases verification on
     * your backend (if you have one) by calling one of the following APIs:
     * [https://developers.google.com/android-publisher/api-ref/purchases/products/get https://developers.google.com/android-publisher/api-ref/purchases/subscriptions/get]
     *
     * * Handles all error cases.
     * @param : The list of products to query.
     * @return empty in case error else the products.
     */
    private suspend fun query(
        products: List<QueryProductDetailsParams.Product>?
    ): List<ProductDetails> {

        //leave early if null or empty
        Log.i(TAG, "query: ")
        if (products.isNullOrEmpty()) {
            Log.i(TAG, "query: Null $products id list passed.")
            return emptyList()
        }

        // construct params
        val prams = QueryProductDetailsParams.newBuilder().setProductList(products).build()

        val (result, list) = mBillingClient.queryProductDetails(prams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            Log.i(TAG, "query: ${result}}")
            return emptyList()
        }
        if (list.isNullOrEmpty()) {
            Log.e(
                TAG,
                "onProductDetailsResponse: " + "Found null or empty ProductDetails. " + "Check to see if the Products you requested are correctly " + "published in the Google Play Console."
            )
            return emptyList()
        }
        return list
    }

    /**
     * Performs a network query the details of products available for sale in your app.
     * @param type the type of purchases to fetch E.g., [BillingClient.ProductType.INAPP]]
     * @return empty list in case of error else purchases.
     */
    private suspend fun query(
        type: String = BillingClient.ProductType.INAPP
    ): List<Purchase> {
        if (!mBillingClient.isReady) {
            Log.e(TAG, "queryPurchases: BillingClient is not ready")
            reconnect()
        }

        val params = QueryPurchasesParams.newBuilder().setProductType(type).build()

        val response = mBillingClient.queryPurchasesAsync(params = params)
        val result = response.billingResult

        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                if (response.purchasesList.isEmpty()) {
                    Log.e(TAG, "Null|Empty Purchase List Returned from OK response!")
                    return emptyList()
                }
                return response.purchasesList
            }
            else -> Log.d(TAG, "query [" + result.responseCode + "]: " + result.debugMessage)
        }
        return emptyList()
    }


    /**
     * Ideally your implementation will comprise a secure server, rendering this check
     * unnecessary. @see [Security]
     */
    private fun isSignatureValid(purchase: Purchase): Boolean =
        Security.verifyPurchase(purchase.originalJson, purchase.signature)

    /**
     * Acknowledges the [purchase] in case it is not.
     * @param purchase the purchase to acknowledge.
     * @return true if acknowledged else false. Note if [purchase] is already acknowledged it will return false.
     */
    private suspend fun acknowledge(
        purchase: Purchase
    ): Boolean {
        // don't acknowledge in debug app.
        if (BuildConfig.DEBUG) return true
        if (purchase.isAcknowledged) return false
        val params =
            AcknowledgePurchaseParams.newBuilder().setPurchaseToken(purchase.purchaseToken).build()
        val result = mBillingClient.acknowledgePurchase(params)
        Log.i(TAG, "acknowledge: $result")
        return result.responseCode == BillingClient.BillingResponseCode.OK && purchase.purchaseState == Purchase.PurchaseState.PURCHASED

    }

    /**
     * Retries the billing service connection with exponential backoff, maxing out at the time
     * specified by [RECONNECT_TIMER_MAX_TIME_MILLISECONDS].
     */
    private fun reconnect() {
        billingManagerScope.launch(Dispatchers.Main) {
            val delay = delayReconnectMills
            //next delay
            delayReconnectMills =
                min(delayReconnectMills * 2, RECONNECT_TIMER_MAX_TIME_MILLISECONDS)
            delay(delay)
            mBillingClient.startConnection(ref)
        }
    }

    /**
     * Launch the billing flow. This will launch an external Activity for a result, so it requires
     * an Activity reference. For subscriptions, it supports upgrading from one SKU type to another
     * by passing in SKUs to be upgraded.
     *
     * @param activity active activity to launch our billing flow from
     * @param sku SKU (Product ID) to be purchased
     * @param uSKUS SKUs that the subscription can be upgraded from
     * @return true if launch is successful
     */
    override fun launchBillingFlow(
        activity: Activity, product: String, vararg uSKUS: String
    ): Boolean {
        val details = _details.value[product] ?: return false
        val params = BillingFlowParams.newBuilder().setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(details)
                        .build()
                )
            ).build()
        // val upgradeSkus = arrayOf(*upgradeSkusVarargs)
        val result = mBillingClient.launchBillingFlow(activity, params)
        return when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> true
            else -> {
                Log.e(TAG, "Billing failed: + " + result.debugMessage)
                false
            }
        }
    }
}


/**
 * Returns the purchase associated with this [id].
 */
operator fun BillingManager.get(id: String) = purchases.value.find { it.products.contains(id) }

/**
 * Observe the purchase as [State] associated with the [id].
 *   ``@Composable
 * @NonRestartableComposable
 * fun BillingManager.observeAsState(id: String) =
 * purchases.map { it.find { it.products.contains(id) } }
 * .collectAsState(initial = this@BillingManager[id]) ``
 *
 **/
@Composable
@NonRestartableComposable
fun BillingManager.observeAsState(id: String) =
    produceState(initialValue = this@BillingManager[id], key1 = purchases, EmptyCoroutineContext) {
        purchases.map { it.find { it.products.contains(id) } }.collect {
            value = it
        }
    }

/**
 * Check weather the purchase's state is acknowledged.
 * An item is purchased if its state [Purchase.PurchaseState.PURCHASED] && [Purchase.isAcknowledged]
 */
val Purchase?.purchased
    get() = if (this == null) false else purchaseState == Purchase.PurchaseState.PURCHASED && if (BuildConfig.DEBUG) true else isAcknowledged

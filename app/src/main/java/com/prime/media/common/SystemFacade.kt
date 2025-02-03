@file:Suppress("NOTHING_TO_INLINE")

package com.prime.media.common

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.prime.media.BuildConfig
import com.prime.media.MainActivity
import com.primex.preferences.Key
import com.zs.core.paymaster.ProductInfo
import com.zs.core.paymaster.Purchase
import com.zs.core_ui.WindowStyle
import com.zs.core_ui.toast.Priority
import com.zs.core_ui.toast.Toast
import kotlinx.coroutines.flow.map

private const val TAG = "SystemFacade"

private const val PREFIX_MARKET_URL = "market://details?id="
private const val PREFIX_MARKET_FALLBACK = "http://play.google.com/store/apps/details?id="
private const val PKG_MARKET_ID = "com.android.vending"

/**
 * An interface defining the methods and properties needed for common app functionality,
 * such as in-app updates, showing ads, and launching the app store.
 *
 * This interface is intended to be implemented by a class that is scoped to the entire app,
 * and is accessible from all parts of the app hierarchy.
 *
 * @property style The current [WindowStyle] as requested by the current screen. Defaults to `Automatic`
 * @property inAppTaskProgress  Represents the progress of an ongoing task within the app. The progress value is a float between 0.0 and 1.0, indicating the percentage of the task
 * that has been completed. The following special values are used:
 *  - `Float.NaN`: Default value when no update is occurring.
 *  - `-1`: Indicates that the task is about to start and represents no definite value.
 *
 * **Note**: This property represents the overall progress. If there are multiple tasks running
 * simultaneously, consider implementing a mechanism to handle each task's progress individually.
 * @property adFreePeriodEndTimeMillis Represents the time in `milliseconds` since the `epoch` when the `ad-free` period ends.
 *  - A value less than or equal to `0` indicates that the reward has expired.
 *  @property isAdFree Indicates whether the app is currently in an `ad-free` state, either due to a
 *  `rewarded` ad or a `purchased` ad-free version.
 *  @property isAdFreeRewarded Indicates whether the app is currently in an ad-free state due to
 *  watching a rewarded video.
 *  @property isAdFreeVersion Indicates whether the app is currently in an ad-free state due to a
 *  `one-time purchase` of the ad-free version.
 *  @property isRewardedVideoAvailable Indicates whether a rewarded video ad is currently available to be shown.
 */
interface SystemFacade {
    var style: WindowStyle
    val inAppTaskProgress: Float
    val adFreePeriodEndTimeMillis: Long
    val isAdFree: Boolean
    val isAdFreeRewarded: Boolean
    val isAdFreeVersion: Boolean
    val isRewardedVideoAvailable: Boolean

    /**
     * Shows a rewarded video ad to the user.
     */
    fun showRewardedVideo()

    /**
     * A utility extension function for showing interstitial ads.
     *
     * **Note**: The ad will not be shown if the app is [isAdFree].
     *
     * @param force If `true`, the ad will be shown regardless of the AdFree status.
     */
    fun showAd(force: Boolean = false)

    /**
     * Launches the provided [intent] with the specified [options].
     */
    fun launch(intent: Intent, options: Bundle? = null)

    /**
     * Launches the App Store to open the app details page for a given package.
     *
     * This function first attempts to open the App Store directly using [AppStoreIntent].
     * If this fails, it falls back to using [FallbackAppStoreIntent] as an alternative.
     *
     * @param pkg the package name of the app to open on the App Store.
     */
    fun launchAppStore(pkg: String = BuildConfig.APPLICATION_ID) {
        val url = "$PREFIX_MARKET_URL$pkg"
        // Create an Intent to open the Play Store app.
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            // Set the package to explicitly target the Play Store app.
            // Don't add this activity to the history stack.
            // Open in a new document (tab or window).
            // Allow multiple instances of the task.
            setPackage(PKG_MARKET_ID)
            addFlags(
                Intent.FLAG_ACTIVITY_NO_HISTORY
                        or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                        or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            )
        }
        // Try launching the Play Store app.
        val res = kotlin.runCatching { launch(intent) }
        // If launching the app fails, use the fallback URL to open in a web browser.
        if (res.isFailure) {
            val fallback = "${PREFIX_MARKET_FALLBACK}$pkg"
            launch(Intent(Intent.ACTION_VIEW, Uri.parse(fallback)))
        }
    }

    /**
     * @see com.zs.core_ui.toast.ToastHostState.showToast
     */
    fun showToast(
        message: CharSequence,
        icon: ImageVector? = null,
        accent: Color = Color.Unspecified,
        @Priority priority: Int = Toast.PRIORITY_LOW,
    )

    /**
     * @see com.zs.core_ui.toast.ToastHostState.showToast
     */
    fun showToast(
        @StringRes message: Int,
        icon: ImageVector? = null,
        accent: Color = Color.Unspecified,
        @Priority priority: Int = Toast.PRIORITY_LOW,
    )

    /**
     * A utility method to launch the in-app update flow, with an option to [report] low-priority
     * issues to the user via a Toast.
     *
     * @param report If `true`, low-priority issues will be reported to the user using the
     *               ToastHostState channel.
     */
    fun initiateUpdateFlow(report: Boolean = false)

    /**
     * Launches an in-app review process if appropriate.
     */
    fun initiateReviewFlow()

    /**
     * Launches billing flow for the provided product [id].
     */
    fun initiatePurchaseFlow(id: String): Boolean

    /**
     * @see com.zs.core_ui.showPlatformToast
     */
    fun showPlatformToast(message: String, @Priority priority: Int = Toast.PRIORITY_LOW)

    /**
     * @see com.zs.core_ui.showPlatformToast
     */
    fun showPlatformToast(@StringRes message: Int, @Priority priority: Int = Toast.PRIORITY_LOW)

    /**
     * Returns the handle to a system-level service by name.
     *
     * @param name The name of the desired service.
     * @return The service object, or null if the name does not exist.
     * @throws ClassCastException if the service is not of the expected type.
     * @see Context.BIOMETRIC_SERVICE
     * @see android.app.Activity.getSystemService
     */
    fun <T> getDeviceService(name: String): T

    /**
     * @see com.primex.preferences.Preferences.observeAsState
     */
    @Composable
    @NonRestartableComposable
    fun <S, O> observeAsState(key: Key.Key1<S, O>): State<O?>

    @Composable
    @NonRestartableComposable
    fun <S, O> observeAsState(key: Key.Key2<S, O>): State<O>

    /**
     * Restarts either the entire application or just the current activity, based on the specified mode.
     *
     * If [global] is set to `true`, this method will restart the entire application by launching the main activity
     * and terminating the current process. This results in a full app relaunch as if the user manually reopened the app.
     *
     * If `global` is set to `false`, only the current activity will be restarted. The activity is relaunched
     * with a fresh instance, mimicking an activity lifecycle reset (similar to what happens after a configuration change).
     *
     * @param global Set to `true` to restart the entire application (default is `false`).
     *               - `true`: Restarts the whole app by relaunching the main activity and terminating the current process.
     *               - `false`: Restarts only the current activity, clearing the current instance and relaunching it.
     *
     *
     * Example Usage:
     * ```
     * // Restart only the current activity
     * restart()
     *
     * // Restart the entire app
     * restart(global = true)
     * ```
     */
    fun restart(global: Boolean = false)

    /**
     * Requests to install the dynamic feature module identified by the given [id].
     *
     * This function initiates the installation process for the specified dynamic feature.
     * The actual installation behavior may vary depending on the device and platform.
     * The installation process might happen asynchronously, and there is no guarantee of completion.
     *
     * @param id The unique identifier of the dynamic feature module to install.
     *           This ID should match the one declared in your app's manifest.
     */
    fun initiateFeatureInstall(request: SplitInstallRequest)

    /**
     * Checks if the dynamic feature module identified by [id] is currently installed.
     *
     * This function determines whether the specified dynamic feature is already installed and
     * available for use within the application.
     *
     * @param id The unique identifier of the dynamic feature module to check.
     *           This ID should match the one declared in your app's manifest.
     * @return `true` if the dynamic feature is installed, `false` otherwise.
     */
    fun isInstalled(id: String): Boolean
}

/**
 * A [staticCompositionLocalOf] variable that provides access to the [SystemFacade] interface.
 *
 * The [SystemFacade] interface defines common methods that can be implemented by an activity that
 * uses a single view with child views.
 * This local composition allows child views to access the implementation of the [SystemFacade]
 * interface provided by their parent activity.
 *
 * If the [SystemFacade] interface is not defined, an error message will be thrown.
 */
val LocalSystemFacade =
    staticCompositionLocalOf<SystemFacade> { error("LocalSystemFacade not initialized") }

/**
 * A composable function that retrieves the purchase state of a product using the [LocalSystemFacade].
 *
 * This function leverages the `LocalSystemFacade` to access the purchase information for a given product ID.
 * In preview mode, it returns a `null` purchase state as the activity context is unavailable.
 *
 * @param id The ID of the product to check the purchase state for.
 * @return A [State] object representing the current purchase state of the product.
 * The state value can be `null` if there is no purchase associated with the given product ID or if the function
 * is called in preview mode.
 */
@Composable
@Stable
fun purchase(id: String): State<Purchase?> {
    val activity = LocalSystemFacade.current as? MainActivity
    if (activity == null) {
        Log.i(
            "SystemFacade", "Purchase operation returned null in preview mode because " +
                    "the activity context is null. This is expected behavior in preview mode."
        )
        return remember { mutableStateOf(null) }
    }
    val manager = activity.paymaster
    return produceState(remember { manager.purchases.value.find { it.id == id } }) {
        manager.purchases.map { it.find { it.id == id } }.collect {
            // updating purchase
            value = it
        }
    }
}


@Composable
@Stable
fun SystemFacade.observeProductInfoAsState(id: String): State<ProductInfo?> {
    val activity = LocalSystemFacade.current as? MainActivity
    if (activity == null) {
        Log.i(
            "SystemFacade", "Purchase operation returned null in preview mode because " +
                    "the activity context is null. This is expected behavior in preview mode."
        )
        return remember { mutableStateOf(null) }
    }
    val manager = activity.paymaster
    return produceState(remember { manager.details.value.find { it.id == id } }, id) {
        manager.details.map { it.find { it.id == id } }.collect {
            // updating
            value = it
        }
    }
}

/**
 * A composable function that uses the [LocalSystemFacade] to fetch [Preference] as state.
 * @param key A key to identify the preference value.
 * @return A [State] object that represents the current value of the preference identified by the provided key.
 * The value can be null if no preference value has been set for the given key.
 */
@Composable
inline fun <S, O> preference(key: Key.Key1<S, O>): State<O?> {
    val provider = LocalSystemFacade.current
    return provider.observeAsState(key = key)
}

/**
 * @see [preference]
 */
@Composable
inline fun <S, O> preference(key: Key.Key2<S, O>): State<O> {
    val provider = LocalSystemFacade.current
    return provider.observeAsState(key = key)
}
package com.zs.audiofy.common

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.State
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.net.toUri
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.zs.audiofy.BuildConfig
import com.zs.audiofy.settings.Settings.PKG_MARKET_ID
import com.zs.audiofy.settings.Settings.PREFIX_MARKET_FALLBACK
import com.zs.audiofy.settings.Settings.PREFIX_MARKET_URL
import com.zs.compose.theme.snackbar.SnackbarDuration
import com.zs.core.billing.Product
import com.zs.core.billing.Purchase
import com.zs.preferences.Key

/**
 * An interface defining the methods and properties needed for common app functionality,
 * such as in-app updates, showing ads, and launching the app store.
 *
 * This interface is intended to be implemented by a class that is scoped to the entire app,
 * and is accessible from all parts of the app hierarchy.
 *
 * @property style The current [WindowStyle] as requested by the current screen. Defaults to `Automatic`
 */
interface SystemFacade {

    var style: WindowStyle

    /**
     * @see Context.showToast
     */
    fun showToast(message: String, duration: Int = Toast.LENGTH_SHORT)

    /**
     * @see Context.showToast
     */
    fun showToast(@StringRes message: Int, duration: Int = Toast.LENGTH_SHORT)

    /**
     * @see com.zs.compose.theme.snackbar.SnackbarHostState.showSnackbar
     */
    fun showSnackbar(
        message: CharSequence,
        icon: ImageVector? = null,
        accent: Color = Color.Unspecified,
        duration: SnackbarDuration = SnackbarDuration.Short,
    )

    /**
     * @see com.zs.compose.theme.snackbar.SnackbarHostState.showSnackbar
     */
    fun showSnackbar(
        @StringRes message: Int,
        icon: ImageVector? = null,
        accent: Color = Color.Unspecified,
        duration: SnackbarDuration = SnackbarDuration.Short,
    )

    @Composable
    @NonRestartableComposable
    fun <S, O> observeAsState(key: Key.Key1<S, O>): State<O?>

    @Composable
    @NonRestartableComposable
    fun <S, O> observeAsState(key: Key.Key2<S, O>): State<O>

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
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
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
            launch(Intent(Intent.ACTION_VIEW, fallback.toUri()))
        }
    }

    /**
     * A utility method to launch the in-app update flow, with an option to report low-priority
     * issues to the user via a Toast.
     *
     * @param report If `true`, low-priority issues will be reported to the user using the
     *               ToastHostState channel.
     */
    fun initiateUpdateFlow(report: Boolean = false)

    /**
     * Launches an in-app review process if appropriate.
     *
     * This method ensures the review dialog is shown only at suitable intervals based on launch count and time since last prompt.
     * It considers [MIN_LAUNCH_COUNT], [MAX_DAYS_BEFORE_FIRST_REVIEW], and [MAX_DAYS_AFTER_FIRST_REVIEW] to prevent excessive prompting.
     */
    fun initiateReviewFlow()

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

    /** Launches billing flow for the provided product [id]. */
    fun initiatePurchaseFlow(id: String): Boolean

    /**/
    @Composable
    @NonRestartableComposable
    fun observePurchaseAsState(id: String): State<Purchase?>

    /**
     * Retrieves information about a product with the given [id].
     */
    // FixMe - Using Product as nullable make it lost the performance that might have been gained
    //          through value class; instead make unspecified case of these.
    // This will be null when the lib has not been refreshed otherwise this must not be null.
    fun getProductInfo(id: String): Product?

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

    fun isFeatureInstalled(id: String): Boolean

    fun initiateFeatureInstall(request: SplitInstallRequest)
}
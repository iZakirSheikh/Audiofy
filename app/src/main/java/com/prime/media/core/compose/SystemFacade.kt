package com.prime.media.core.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.android.billingclient.api.Purchase
import com.prime.media.core.compose.channel.Channel
import com.primex.core.Text
import com.primex.preferences.Key

/**
 * An interface defining the methods and properties needed for common app functionality,
 * such as in-app updates, showing ads, and launching the app store.
 *
 * This interface is intended to be implemented by a class that is scoped to the entire app,
 * and is accessible from all parts of the app hierarchy.
 *
 * @see DefaultProvider
 */
@Stable
interface SystemFacade {

    /**
     * A simple property that represents the progress of the in-app update.
     *
     * The progress is represented as a [State] object, which allows you to observe changes to the
     * progress value.
     *
     * The progress value is a float between 0.0 and 1.0, indicating the percentage of the update
     * that has been completed. The Float.NaN represents a default value when no update is going on.
     *
     */
    val inAppUpdateProgress: State<Float>

    /**
     * @see Channel
     */
    val channel: Channel

    /**
     * A utility extension function for showing interstitial ads.
     * * Note: The ad will not be shown if the app is adFree Version.
     *
     * @param force If `true`, the ad will be shown regardless of the AdFree status.
     * @param action A callback to be executed after the ad is shown.
     */
    fun showAd(force: Boolean = false, action: (() -> Unit)? = null)

    /**
     * This uses the provider to submit message to [Channel]
     *
     * @see Channel.show
     */
    fun show(
        title: Text,
        text: Text = Text(""),
        action: Text? = null,
        icon: Any? = null,
        accent: Color = Color.Unspecified,
        duration: Channel.Duration = Channel.Duration.Short
    )


    /**
     * @see show
     */
    fun show(
        title: String,
        text: String = "",
        action: String? = null,
        icon: Any? = null,
        accent: Color = Color.Unspecified,
        duration: Channel.Duration = Channel.Duration.Short
    )


    /**
     * A utility method to launch the in-app update flow, with an option to report low-priority
     * issues to the user via a Toast.
     *
     * @param report If `true`, low-priority issues will be reported to the user using the
     *               ToastHostState channel.
     */
    fun launchUpdateFlow(report: Boolean = false)

    /**
     * This is a convenient method for launching an in-app review process, with some built-in
     * conditions and guardrails.
     * Specifically, this method will only launch the review dialog if certain criteria are met,
     * as follows:
     *
     * - The app has been launched at least [MIN_LAUNCH_COUNT] times.
     * - At least [MAX_DAYS_BEFORE_FIRST_REVIEW] days have passed since the first launch.
     * - If a review has already been prompted, at least [MAX_DAYS_AFTER_FIRST_REVIEW] days have
     * passed since the last review prompt.
     *
     * These criteria are designed to ensure that the review prompt is only shown at appropriate
     * intervals, and that users are not repeatedly prompted to leave a review.
     *
     * Note that this method should not be used to prompt for a review after every cold boot or launch of the app.
     */
    fun launchReviewFlow()

    /**
     * Launches the Google Play Store app for this app's package.
     *
     * This function creates an intent to open the Google Play Store app for this app's package.
     * If the Google Play Store app is not installed, the intent will open the Play Store website instead.
     *
     * Note: This function requires the `android.permission.INTERNET` permission to be declared in your app's manifest file.
     */
    fun launchAppStore()

    @Composable
    @NonRestartableComposable
    fun <S, O> observeAsState(key: Key.Key1<S, O>): State<O?>

    @Composable
    @NonRestartableComposable
    fun <S, O> observeAsState(key: Key.Key2<S, O>): State<O>

    @Composable
    @NonRestartableComposable
    fun observeAsState(product: String): State<Purchase?>

    fun launchBillingFlow(id: String)
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
    staticCompositionLocalOf<SystemFacade> {
        error("Provider not defined.")
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

/**
 * A composable function that uses the [LocalSystemFacade] to fetch the purchase state of a product.
 * @param id The product ID to identify the purchase state.
 * @return A [State] object that represents the current purchase state of the provided product ID.
 * The value can be null if there is no purchase associated with the given product ID.
 */
@Composable
inline fun purchase(id: String): State<Purchase?> {
    val provider = LocalSystemFacade.current
    return provider.observeAsState(product = id)
}
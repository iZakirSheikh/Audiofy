package com.prime.media

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.size
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.LocalElevationOverlay
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.Purchase
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.ktx.AppUpdateResult
import com.google.android.play.core.ktx.requestAppUpdateInfo
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.ktx.requestUpdateFlow
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.analytics.FirebaseAnalytics
import com.prime.media.core.NightMode
import com.prime.media.core.billing.*
import com.prime.media.core.compose.Placeholder
import com.prime.media.core.compose.ToastHostState
import com.prime.media.core.compose.ToastHostState.Duration
import com.prime.media.core.compose.show
import com.prime.media.core.playback.Remote
import com.primex.core.MetroGreen
import com.primex.core.Text
import com.primex.material2.OutlinedButton
import com.primex.preferences.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

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
interface Provider {

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
    
    val toastHostState: ToastHostState

    /**
     * A utility extension function for showing interstitial ads.
     * * Note: The ad will not be shown if the app is adFree Version.
     *
     * @param force If `true`, the ad will be shown regardless of the AdFree status.
     * @param action A callback to be executed after the ad is shown.
     */
    fun showAd(force: Boolean = false, action: (() -> Unit)? = null)

    /**
     * This uses the provider to submit message to [ToastHostState]
     *
     * @see ToastHostState.show
     */
    fun show(
        title: Text,
        text: Text = Text(""),
        action: Text? = null,
        icon: Any? = null,
        accent: Color = Color.Unspecified,
        duration: Duration = Duration.Short
    )


    /**
     * @see show
     */
    suspend fun show(
        title: String,
        text: String = "",
        action: String? = null,
        icon: Any? = null,
        accent: Color = Color.Unspecified,
        duration: Duration = Duration.Short
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
 * A [staticCompositionLocalOf] variable that provides access to the [Provider] interface.
 *
 * The [Provider] interface defines common methods that can be implemented by an activity that
 * uses a single view with child views.
 * This local composition allows child views to access the implementation of the [Provider]
 * interface provided by their parent activity.
 *
 * If the [Provider] interface is not defined, an error message will be thrown.
 */
val LocalsProvider =
    staticCompositionLocalOf<Provider> {
        error("Provider not defined.")
    }

private const val TAG = "MainActivity"

private const val MIN_LAUNCH_COUNT = 20
private val MAX_DAYS_BEFORE_FIRST_REVIEW = TimeUnit.DAYS.toMillis(7)
private val MAX_DAY_AFTER_FIRST_REVIEW = TimeUnit.DAYS.toMillis(10)

private val KEY_LAST_REVIEW_TIME =
    longPreferenceKey(TAG + "_last_review_time")

private const val FLEXIBLE_UPDATE_MAX_STALENESS_DAYS = 2
private const val RESULT_CODE_APP_UPDATE = 1000

@Composable
private fun PermissionRationale(onRequestPermission: () -> Unit) {
    Placeholder(
        iconResId = R.raw.lt_permission,
        title = stringResource(R.string.storage_permission),
        message = stringResource(R.string.storage_permission_message),
    ) {
        OutlinedButton(
            onClick = onRequestPermission,
            modifier = Modifier.size(width = 200.dp, height = 46.dp),
            elevation = null,
            label = "ALLOW",
            border = ButtonDefaults.outlinedBorder
        )
    }
}


/**
 * Manages SplashScreen
 */
private fun ComponentActivity.initSplashScreen(isColdStart: Boolean) {
    // Install Splash Screen and Play animation when cold start.
    installSplashScreen().let { splashScreen ->
        // Animate entry of content
        // if cold start
        if (isColdStart) splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
            val splashScreenView = splashScreenViewProvider.view
            // Create your custom animation.
            val alpha = ObjectAnimator.ofFloat(
                splashScreenView, View.ALPHA, 1f, 0f
            )
            alpha.interpolator = AnticipateInterpolator()
            alpha.duration = 700L

            // Call SplashScreenView.remove at the end of your custom animation.
            alpha.doOnEnd { splashScreenViewProvider.remove() }

            // Run your animation.
            alpha.start()
        }
    }
}

/**
 * A composable function that uses the [LocalsProvider] to fetch [Preference] as state.
 * @param key A key to identify the preference value.
 * @return A [State] object that represents the current value of the preference identified by the provided key.
 * The value can be null if no preference value has been set for the given key.
 */
@Composable
inline fun <S, O> preference(key: Key.Key1<S, O>): State<O?> {
    val provider = LocalsProvider.current
    return provider.observeAsState(key = key)
}

/**
 * @see [preference]
 */
@Composable
inline fun <S, O> preference(key: Key.Key2<S, O>): State<O> {
    val provider = LocalsProvider.current
    return provider.observeAsState(key = key)
}

/**
 * A composable function that uses the [LocalsProvider] to fetch the purchase state of a product.
 * @param id The product ID to identify the purchase state.
 * @return A [State] object that represents the current purchase state of the provided product ID.
 * The value can be null if there is no purchase associated with the given product ID.
 */
@Composable
inline fun purchase(id: String): State<Purchase?> {
    val provider = LocalsProvider.current
    return provider.observeAsState(product = id)
}

@Composable
@NonRestartableComposable
private fun resolveAppThemeState(): Boolean {
    val mode by preference(key = Audiofy.NIGHT_MODE)
    return when (mode) {
        NightMode.YES -> true
        NightMode.FOLLOW_SYSTEM -> isSystemInDarkTheme()
        else -> false
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity(), Provider {
    private val fAnalytics by lazy { FirebaseAnalytics.getInstance(this) }
    private val advertiser by lazy { Advertiser(this) }
    private val billingManager by lazy { BillingManager(this, arrayOf(Product.DISABLE_ADS)) }


    override val inAppUpdateProgress: State<Float> =
        mutableStateOf(Float.NaN)

    // injectable code.
    @Inject
    lateinit var preferences: Preferences

    @Inject
    override lateinit var toastHostState: ToastHostState

    @Inject
    lateinit var remote: Remote


    override fun onResume() {
        super.onResume()
        billingManager.refresh()
    }

    override fun onDestroy() {
        billingManager.release()
        super.onDestroy()
    }

    override fun showAd(force: Boolean, action: (() -> Unit)?) {
        val isAdFree = billingManager[Product.DISABLE_ADS].purchased
        if (isAdFree) return // don't do anything
        advertiser.show(this, force, action)
    }

    override fun show(
        title: Text,
        text: Text,
        action: Text?,
        icon: Any?,
        accent: Color,
        duration: Duration
    ) {
        lifecycleScope.launch {
            toastHostState.show(title, text, action, icon, accent, duration)
        }
    }

    override suspend fun show(
        title: String,
        text: String,
        action: String?,
        icon: Any?,
        accent: Color,
        duration: Duration
    ) {
        lifecycleScope.launch {
            toastHostState.show(title, text, action, icon, accent, duration)
        }
    }

    override fun launchAppStore() {
        val res =
            kotlin.runCatching {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(Audiofy.GOOGLE_STORE))
                    .apply {
                        setPackage(Audiofy.PKG_GOOGLE_PLAY_STORE)
                        addFlags(
                            Intent.FLAG_ACTIVITY_NO_HISTORY
                                    or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                                    or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
                        )
                    }
                startActivity(intent)
            }
        // if failed start in webview.
        if (res.isFailure)
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(Audiofy.FALLBACK_GOOGLE_STORE)))
    }


    override fun launchBillingFlow(id: String) {
        billingManager.launchBillingFlow(this, id)
    }

    @Composable
    @NonRestartableComposable
    override fun <S, O> observeAsState(key: Key.Key1<S, O>): State<O?> =
        preferences.observeAsState(key = key)

    @Composable
    @NonRestartableComposable
    override fun <S, O> observeAsState(key: Key.Key2<S, O>): State<O> =
        preferences.observeAsState(key = key)

    @Composable
    @NonRestartableComposable
    override fun observeAsState(product: String): State<Purchase?> =
        billingManager.observeAsState(id = product)

    override fun launchReviewFlow() {
        lifecycleScope.launch {
            val count = preferences.value(Audiofy.KEY_LAUNCH_COUNTER) ?: 0
            // the time when lastly asked for review
            val lastAskedTime = preferences.value(KEY_LAST_REVIEW_TIME)
            // obtain teh first install time.
            val firstInstallTime =
                com.primex.core.runCatching(TAG + "_review") {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        packageManager.getPackageInfo(
                            packageName,
                            PackageManager.PackageInfoFlags.of(0)
                        )
                    else
                        packageManager.getPackageInfo(packageName, 0)
                }?.firstInstallTime
            // obtain the current time.
            val currentTime = System.currentTimeMillis()
            // Only first time we should not ask immediately
            // however other than this whenever we do some thing of appreciation.
            // we should ask for review.
            var ask =
                (lastAskedTime == null && firstInstallTime != null
                        && count >= MIN_LAUNCH_COUNT
                        && currentTime - firstInstallTime >= MAX_DAYS_BEFORE_FIRST_REVIEW)
            // check for other condition as well
            // if this is not the first review; ask only if after time passed.
            ask =
                ask || (lastAskedTime != null
                        && count >= MIN_LAUNCH_COUNT
                        && currentTime - lastAskedTime >= MAX_DAY_AFTER_FIRST_REVIEW)
            // return from here if not required to ask
            if (!ask) return@launch
            // The flow has finished. The API does not indicate whether the user
            // reviewed or not, or even whether the review dialog was shown. Thus, no
            // matter the result, we continue our app flow.
            com.primex.core.runCatching(TAG) {
                val reviewManager = ReviewManagerFactory.create(this@MainActivity)
                // update the last asking
                preferences[KEY_LAST_REVIEW_TIME] = System.currentTimeMillis()
                val info = reviewManager.requestReview()
                reviewManager.launchReviewFlow(this@MainActivity, info)
                //host.fAnalytics.
            }
        }
    }

    override fun launchUpdateFlow(report: Boolean) {
        lifecycleScope.launch {
            com.primex.core.runCatching(TAG) {
                val manager = AppUpdateManagerFactory.create(this@MainActivity)
                val channel = toastHostState
                manager.requestUpdateFlow().collect { result ->
                    when (result) {
                        AppUpdateResult.NotAvailable ->
                            if (report) channel.show("The app is already updated to the latest version.")
                        is AppUpdateResult.InProgress -> {
                            val state = result.installState

                            val total = state.totalBytesToDownload()
                            val downloaded = state.bytesDownloaded()

                            val progress = when {
                                total <= 0 -> -1f
                                total == downloaded -> Float.NaN
                                else -> downloaded / total.toFloat()
                            }
                            (inAppUpdateProgress as MutableState).value = progress
                            Log.i(TAG, "check: $progress")
                        }
                        is AppUpdateResult.Downloaded -> {
                            val info = manager.requestAppUpdateInfo()
                            //when update first becomes available
                            //don't force it.
                            // make it required when staleness days overcome allowed limit
                            val isFlexible = (info.clientVersionStalenessDays()
                                ?: -1) <= FLEXIBLE_UPDATE_MAX_STALENESS_DAYS

                            // forcefully update; if it's flexible
                            if (!isFlexible) {
                                manager.completeUpdate()
                                return@collect
                            }
                            // else show the toast.
                            val res = channel.show(
                                title = "Update",
                                message = "An update has just been downloaded.",
                                label = "RESTART",
                                duration = Duration.Indefinite,
                                accent = Color.MetroGreen
                            )
                            // complete update when ever user clicks on action.
                            if (res == ToastHostState.Result.ActionPerformed) manager.completeUpdate()
                        }
                        is AppUpdateResult.Available -> {
                            // if user choose to skip the update handle that case also.
                            val isFlexible = (result.updateInfo.clientVersionStalenessDays()
                                ?: -1) <= FLEXIBLE_UPDATE_MAX_STALENESS_DAYS
                            if (isFlexible) result.startFlexibleUpdate(
                                activity = this@MainActivity, RESULT_CODE_APP_UPDATE
                            )
                            else result.startImmediateUpdate(
                                activity = this@MainActivity, RESULT_CODE_APP_UPDATE
                            )
                            // no message needs to be shown
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app has started from scratch if savedInstanceState is null.
        val isColdStart = savedInstanceState == null //why?
        // show splash screen
        initSplashScreen(isColdStart)
        // only run this piece of code if cold start.
        if (isColdStart) {
            val counter = preferences.value(Audiofy.KEY_LAUNCH_COUNTER) ?: 0
            // update launch counter if
            // cold start.
            preferences[Audiofy.KEY_LAUNCH_COUNTER] = counter + 1
            // check for updates on startup
            // don't report
            // check silently
            launchUpdateFlow()
            // TODO: Try to reconcile if it is any good to ask for reviews here.
            // launchReviewFlow()
        }
        //manually handle decor.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // set content to the app.
        setContent {
            // observe the change to density
            val density = LocalDensity.current
            val fontScale by observeAsState(Audiofy.FONT_SCALE)
            val modified = Density(density = density.density, fontScale = fontScale)
            // ask different permission as per api level.
            val permission =
                rememberPermissionState(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                        android.Manifest.permission.READ_MEDIA_AUDIO
                    else
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            CompositionLocalProvider(
                LocalElevationOverlay provides null,
                //LocalWindowSizeClass provides sWindow,
                LocalDensity provides modified,
                LocalsProvider provides this@MainActivity,
                // content
                content = {
                    Theme(isDark = resolveAppThemeState()) {
                        // Maybe add support for intro.
                        Crossfade(
                            targetState = permission.status.isGranted
                        ) { has ->
                            when (has) {
                                false -> PermissionRationale { permission.launchPermissionRequest() }
                                else -> {
                                    val show by remote.loaded.collectAsState(initial = false)
                                    Home(show)
                                }
                            }
                        }
                    }
                }
            )
        }
    }
}
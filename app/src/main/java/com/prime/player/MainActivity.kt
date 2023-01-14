package com.prime.player

import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.database.ContentObserver
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.material.LocalElevationOverlay
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
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
import com.prime.player.billing.*
import com.prime.player.common.NightMode
import com.prime.player.common.compose.*
import com.prime.player.common.compose.ToastHostState.Duration
import com.prime.player.common.compose.ToastHostState.Result
import com.prime.player.core.Remote
import com.primex.core.activity
import com.primex.preferences.*
import com.primex.ui.ColoredOutlineButton
import com.primex.ui.Label
import com.primex.ui.MetroGreen
import com.primex.ui.activity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.components.ActivityRetainedComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ActivityRetainedScoped
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val TAG = "MainActivity"

private const val RESULT_CODE_APP_UPDATE = 1000

/**
 * A simple fun that uses [MainActivity] to fetch [Preference] as state.
 */
@Composable
inline fun <S, O> preference(key: Key.Key1<S, O>): State<O?> {
    val activity = LocalContext.activity
    require(activity is MainActivity)
    return activity.preferences.observeAsState(key = key)
}

/**
 * @see [preference]
 */
@Composable
inline fun <S, O> preference(key: Key.Key2<S, O>): State<O> {
    val activity = LocalContext.activity
    require(activity is MainActivity)
    return activity.preferences.observeAsState(key = key)
}


@Composable
private fun PermissionRationale(
    onRequestPermission: () -> Unit
) {
    Placeholder(
        iconResId = R.raw.lt_permission,
        title = stringResource(R.string.storage_permission),
        message = stringResource(R.string.storage_permission_message),
    ) {
        ColoredOutlineButton(
            onClick = onRequestPermission,
            modifier = Modifier.size(width = 200.dp, height = 46.dp),
            elevation = null,
        ) {
            Label(text = "ALLOW", style = Material.typography.button)
        }
    }
}

@Composable
private fun resolveAppThemeState(): Boolean {
    val mode by preference(key = Audiofy.NIGHT_MODE)
    return when (mode) {
        NightMode.YES -> true
        else -> false
    }
}


/**
 * Manages SplashScreen
 */
fun MainActivity.initSplashScreen(
    isColdStart: Boolean
) {
    // Install Splash Screen and Play animation when cold start.
    installSplashScreen().let { splashScreen ->
        // Animate entry of content
        // if cold start
        if (isColdStart)
            splashScreen.setOnExitAnimationListener { splashScreenViewProvider ->
                val splashScreenView = splashScreenViewProvider.view
                // Create your custom animation.
                val alpha = ObjectAnimator.ofFloat(
                    splashScreenView,
                    View.ALPHA,
                    1f,
                    0f
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

private const val MIN_LAUNCH_COUNT = 20
private val MAX_DAYS_BEFORE_FIRST_REVIEW = TimeUnit.DAYS.toMillis(7)
private val MAX_DAY_AFTER_FIRST_REVIEW = TimeUnit.DAYS.toMillis(10)

private val KEY_LAST_REVIEW_TIME =
    longPreferenceKey(
        TAG + "_last_review_time"
    )

/**
 * A convince method for launching an in-app review.
 * The review API is guarded by some conditions which are
 * * The first review will be asked when launchCount is > [MIN_LAUNCH_COUNT] and daysPassed >=[MAX_DAYS_BEFORE_FIRST_REVIEW]
 * * After asking first review then after each [MAX_DAY_AFTER_FIRST_REVIEW] a review dialog will be showed.
 * Note: The review should not be asked after every coldBoot.
 */
fun Activity.launchReviewFlow() {
    require(this is MainActivity)
    lifecycleScope.launch {
        val count =
            preferences.value(Audiofy.KEY_LAUNCH_COUNTER) ?: 0

        // the time when lastly asked for review
        val lastAskedTime =
            preferences.value(KEY_LAST_REVIEW_TIME)

        val firstInstallTime =
            com.primex.core.runCatching(TAG + "_review") {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                    packageManager.getPackageInfo(
                        packageName,
                        PackageManager.PackageInfoFlags.of(0)
                    ).firstInstallTime
                else
                    packageManager.getPackageInfo(packageName, 0).firstInstallTime
            }

        val currentTime = System.currentTimeMillis()
        // Only first time we should not ask immediately
        // however other than this whenever we do some thing of appreciation.
        // we should ask for review.
        var ask =
            (lastAskedTime == null &&
                    firstInstallTime != null &&
                    count >= MIN_LAUNCH_COUNT &&
                    currentTime - firstInstallTime >= MAX_DAYS_BEFORE_FIRST_REVIEW)

        // check for other condition as well
        ask = ask ||
                // if this is not the first review; ask only if after time passed.
                (lastAskedTime != null &&
                        count >= MIN_LAUNCH_COUNT &&
                        currentTime - lastAskedTime >= MAX_DAY_AFTER_FIRST_REVIEW)
        // return from here if not required to ask
        if (!ask) return@launch
        // The flow has finished. The API does not indicate whether the user
        // reviewed or not, or even whether the review dialog was shown. Thus, no
        // matter the result, we continue our app flow.
        com.primex.core.runCatching(TAG) {
            val reviewManager = ReviewManagerFactory.create(this@launchReviewFlow)
            // update the last asking
            preferences[KEY_LAST_REVIEW_TIME] = System.currentTimeMillis()
            val info = reviewManager.requestReview()
            reviewManager.launchReviewFlow(this@launchReviewFlow, info)
            //host.fAnalytics.
        }
    }
}

private const val FLEXIBLE_UPDATE_MAX_STALENESS_DAYS = 2

/**
 * A utility method to check for updates.
 * @param channel [SnackDataChannel] to report errors, inform users about the availability of update.
 * @param report simple messages.
 */
fun Activity.launchUpdateFlow(
    report: Boolean = false,
) {
    require(this is MainActivity)
    lifecycleScope.launch {
        com.primex.core.runCatching(TAG) {
            val manager = AppUpdateManagerFactory.create(this@launchUpdateFlow)
            val channel = toastHostState
            manager.requestUpdateFlow().collect { result ->
                when (result) {
                    AppUpdateResult.NotAvailable -> if (report)
                        channel.show("The app is already updated to the latest version.")
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
                        val isFlexible =
                            (info.clientVersionStalenessDays() ?: -1) <=
                                    FLEXIBLE_UPDATE_MAX_STALENESS_DAYS

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
                        if (res == Result.ActionPerformed)
                            manager.completeUpdate()
                    }
                    is AppUpdateResult.Available -> {
                        // if user choose to skip the update handle that case also.
                        val isFlexible =
                            (result.updateInfo.clientVersionStalenessDays()
                                ?: -1) <=
                                    FLEXIBLE_UPDATE_MAX_STALENESS_DAYS
                        if (isFlexible)
                            result.startFlexibleUpdate(
                                activity = this@launchUpdateFlow,
                                RESULT_CODE_APP_UPDATE
                            )
                        else
                            result.startImmediateUpdate(
                                activity = this@launchUpdateFlow,
                                RESULT_CODE_APP_UPDATE
                            )
                        // no message needs to be shown
                    }
                }
            }
        }
    }
}

/**
 * A simple extension property on [LocalContext] that returns the [Advertiser] of the [MainActivity].
 * *Requirements*
 * * The activity requires to be [MainActivity]
 */
val ProvidableCompositionLocal<Context>.advertiser: Advertiser
    @Composable
    @ReadOnlyComposable
    inline get() {
        val activity = current.activity
        require(activity is MainActivity)
        return activity.advertiser
    }


/**
 * A utility extension fun for showing interstitially ads.
 *
 * *Requirements*
 * * The activity requires to be [MainActivity]
 * * checks if the app version is AdFree; then proceeds.
 */
fun Activity.showAd(
    force: Boolean = false,
    action: (() -> Unit)? = null
) {
    require(this is MainActivity)
    val isAdFree = billingManager[Product.DISABLE_ADS].purchased
    if (isAdFree) return // don't do anything
    advertiser.show(this, force, action)
}

/**
 * A simple extension property on [LocalContext] that returns the activity the context is attached to.
 */
val ProvidableCompositionLocal<Context>.billingManager: BillingManager
    @ReadOnlyComposable
    @Composable
    inline get() {
        val activity = current.activity
        require(activity is MainActivity)
        return activity.billingManager
    }

/**
 * A simple extension property on [LocalContext] that returns the [FirebaseAnalytics].
 * *Requirements*
 * * The context must contain the [Activity] as [MainActivity]
 */
val ProvidableCompositionLocal<Context>.fAnalytics: FirebaseAnalytics
    @ReadOnlyComposable
    @Composable
    inline get() {
        val activity = current.activity
        require(activity is MainActivity)
        return activity.fAnalytics
    }

/**
 * A simple extension property on [LocalContext] that returns the [ToastHostState].
 * *Requirements*
 * * The context must contain the [Activity] as [MainActivity]
 */
val ProvidableCompositionLocal<Context>.toastHostState: ToastHostState
    @ReadOnlyComposable
    @Composable
    inline get() {
        val activity = current.activity
        require(activity is MainActivity)
        return activity.toastHostState
    }

val ProvidableCompositionLocal<Context>.inAppUpdateProgress: State<Float>
    @ReadOnlyComposable
    @Composable
    inline get() {
        val activity = current.activity
        require(activity is MainActivity)
        return activity.inAppUpdateProgress
    }

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    val fAnalytics by lazy { FirebaseAnalytics.getInstance(this) }
    val advertiser by lazy { Advertiser(this) }
    val billingManager by lazy { BillingManager(this, arrayOf(Product.DISABLE_ADS)) }
    val toastHostState by lazy { ToastHostState() }

    /**
     * The progress of the in-App update.
     */
    val inAppUpdateProgress: State<Float> = mutableStateOf(Float.NaN)


    @Inject
    lateinit var preferences: Preferences
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

    @SuppressLint("WrongThread")
    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app has started from scratch if savedInstanceState is null.
        val isColdStart = savedInstanceState == null //why?
        // show splash screen
        initSplashScreen(
            isColdStart
        )
        if (isColdStart) {
            val counter =
                preferences.value(Audiofy.KEY_LAUNCH_COUNTER) ?: 0
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

        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            val sWindow = calculateWindowSizeClass(activity = this)
            // observe the change to density
            val density = LocalDensity.current
            val fontScale by preference(key = Audiofy.FONT_SCALE)
            val modified = Density(density = density.density, fontScale = fontScale)

            val permission =
                rememberPermissionState(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
            CompositionLocalProvider(
                LocalElevationOverlay provides null,
                LocalWindowSizeClass provides sWindow,
                LocalDensity provides modified
            ) {
                Material(isDark = resolveAppThemeState()) {
                    // scaffold
                    // FixMe: Re-design the SnackBar Api.
                    // Introduce: SideBar and Bottom Bar.
                    Crossfade(
                        targetState = permission.status.isGranted,
                        modifier = Modifier.navigationBarsPadding()
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
        }
    }
}


@Module
@InstallIn(ActivityRetainedComponent::class)
object Activity {
    @ActivityRetainedScoped
    @Provides
    fun remote(@ApplicationContext context: Context) = Remote(context)
}
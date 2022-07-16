package com.prime.player


import android.animation.ObjectAnimator
import android.app.Activity
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
import androidx.compose.material.SnackbarDuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.AppUpdateResult
import com.google.android.play.core.ktx.requestAppUpdateInfo
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.ktx.requestUpdateFlow
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.prime.player.audio.Home
import com.prime.player.common.compose.*
import com.prime.player.core.SyncWorker
import com.prime.player.settings.GlobalKeys
import com.prime.player.settings.NightMode
import com.primex.preferences.LocalPreferenceStore
import com.primex.preferences.Preferences
import com.primex.preferences.intPreferenceKey
import com.primex.preferences.longPreferenceKey
import com.primex.ui.ColoredOutlineButton
import com.primex.ui.Label
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val TAG = "MainActivity"

private val KEY_LAUNCH_COUNTER = intPreferenceKey(TAG + "_launch_counter")
private val KEY_LAST_REVIEW_TIME = longPreferenceKey(TAG + "_last_review_time")

private const val RESULT_CODE_APP_UPDATE = 1000

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    lateinit var fAnalytics: FirebaseAnalytics
    lateinit var mAppUpdateManager: AppUpdateManager
    private lateinit var observer: ContentObserver

    @Inject
    lateinit var preferences: Preferences

    @OptIn(ExperimentalPermissionsApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // The app has started from scratch if savedInstanceState is null.
        val isColdStart = savedInstanceState == null //why?
        // Obtain the FirebaseAnalytics instance.
        fAnalytics = Firebase.analytics
        // show splash screen
        initSplashScreen(
            isColdStart
        )

        //init
        mAppUpdateManager = AppUpdateManagerFactory.create(this)
        val reviewManager = ReviewManagerFactory.create(this)
        val channel = SnackDataChannel()


        //schedule sync
        // trigger sync worker once change in MediaStore is detected.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) SyncWorker.schedule(this)
        else {
            observer = object : ContentObserver(null) {
                override fun onChange(selfChange: Boolean) {
                    // run worker when change is detected.
                    if (!selfChange) SyncWorker.run(this@MainActivity)
                }
            }

            // observe Images in MediaStore.
            contentResolver.registerContentObserver(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                true,
                observer,
            )
        }


        // launch
        if (isColdStart && !BuildConfig.DEBUG)
            lifecycleScope.launch {
                // increment launch counter
                val counter = with(preferences) { preferences[KEY_LAUNCH_COUNTER].obtain() } ?: 0
                // update launch counter if
                // cold start.
                preferences[KEY_LAUNCH_COUNTER] = counter + 1

                // check for updates on startup
                // don't report
                // check silently
                mAppUpdateManager.check(
                    channel = channel,
                    activity = this@MainActivity
                )

                val isAvailable = mAppUpdateManager.requestAppUpdateInfo().updateAvailability() ==
                        UpdateAvailability.UPDATE_AVAILABLE
                // don't ask
                // if update is available
                // because maybe the user wishes to update the app
                // this might interrupt the reviewFlow.
                if (!isAvailable)
                    reviewManager.review(this@MainActivity, preferences)
            }

        WindowCompat.setDecorFitsSystemWindows(window, false)

        setContent {
            val sWindow = rememberWindowSizeClass()

            // observe the change to density
            val density = LocalDensity.current
            val fontScale by with(preferences) { get(GlobalKeys.FONT_SCALE).observeAsState() }
            val modified = Density(density = density.density, fontScale = fontScale)

            val permission =
                rememberPermissionState(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)

            CompositionLocalProvider(
                LocalElevationOverlay provides null,
                LocalWindowSizeClass provides sWindow,
                LocalPreferenceStore provides preferences,
                LocalDensity provides modified,
                LocalAppUpdateManager provides mAppUpdateManager,
                LocalAppReviewManager provides reviewManager,
                LocalSnackDataChannel provides channel,
                LocalSystemUiController provides rememberSystemUiController()
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
                            true -> Home()
                            else -> PermissionRationale { permission.launchPermissionRequest() }
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        // unregister content Observer.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N)
            contentResolver.unregisterContentObserver(observer)
        super.onDestroy()
    }

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


@Composable
private fun resolveAppThemeState(): Boolean {
    val preferences = LocalPreferenceStore.current
    val mode by with(preferences) {
        preferences[GlobalKeys.NIGHT_MODE].observeAsState()
    }
    return when (mode) {
        NightMode.YES -> true
        else -> false
    }
}


private const val FLEXIBLE_UPDATE_MAX_STALENESS_DAYS = 2

/**
 * @param report simple messages.
 */
suspend fun AppUpdateManager.check(
    channel: SnackDataChannel,
    activity: Activity,
    report: Boolean = false,
) {
    requestUpdateFlow()
        // catch any error like the install exception occuring
        // in the release version of the app.
        .catch { Log.i(TAG, "check: ${it.message}") }
        // collect the flow and publish/request/install the update.
        .collect { result ->
            when (result) {
                AppUpdateResult.NotAvailable -> if (report)
                    channel.send("The app is already updated to the latest version.")

                is AppUpdateResult.InProgress -> {
                    //FixMe: Publish progress
                    val state = result.installState
                    val progress =
                        state.bytesDownloaded() / (state.totalBytesToDownload() + 0.1) * 100
                    Log.i(TAG, "check: $progress")
                    // currently don't show any message
                    // future version find ways to show progress.
                }
                is AppUpdateResult.Downloaded -> {
                    val info = requestAppUpdateInfo()
                    //when update first becomes available
                    //don't force it.
                    // make it required when staleness days overcome allowed limit
                    val isFlexible =
                        (info.clientVersionStalenessDays() ?: -1) <=
                                FLEXIBLE_UPDATE_MAX_STALENESS_DAYS

                    // forcefully update; if it's flexible
                    if (!isFlexible)
                        completeUpdate()
                    else
                    // ask gracefully
                        channel.send(
                            message = "An update has just been downloaded.",
                            label = "RESTART",
                            action = this::completeUpdate,
                            duration = SnackbarDuration.Indefinite
                        )
                    // no message needs to be shown
                }
                is AppUpdateResult.Available -> {
                    val isFlexible =
                        (result.updateInfo.clientVersionStalenessDays() ?: -1) <=
                                FLEXIBLE_UPDATE_MAX_STALENESS_DAYS
                    val result2 =
                        com.primex.core.runCatching(TAG) {
                            if (isFlexible)
                                result.startFlexibleUpdate(
                                    activity = activity,
                                    RESULT_CODE_APP_UPDATE
                                )
                            else
                                result.startImmediateUpdate(
                                    activity = activity,
                                    RESULT_CODE_APP_UPDATE
                                )
                        }
                    Log.i(TAG, "check: starting $result2")
                    // no message needs to be shown
                }
            }
        }
}


private const val MIN_LAUNCH_COUNT = 20
private val MAX_DAYS_BEFORE_FIRST_REVIEW = TimeUnit.DAYS.toMillis(7)
private val MAX_DAY_AFTER_FIRST_REVIEW = TimeUnit.DAYS.toMillis(20)

/**
 * Launch app review subject with condition.
 * @param activity The activity to launch on.
 * @param preferences The [Preferences] used to retrieve and stored values.
 * @param force if force all the manually applied conditions will be forgotten and
 */
suspend fun ReviewManager.review(
    activity: Activity,
    preferences: Preferences,
    force: Boolean = false
) {
    val count = with(preferences) { preferences[KEY_LAUNCH_COUNTER].obtain() } ?: 0
    // the time when lastly asked for review
    val lastAskedTime =
        with(preferences) { preferences[KEY_LAST_REVIEW_TIME].obtain() }

    val firstInstallTime =
        com.primex.core.runCatching(TAG + "_review") {
            activity.packageManager.getPackageInfo(activity.packageName, 0).firstInstallTime
        }
    val currentTime = System.currentTimeMillis()

    val askFirstReview =
        lastAskedTime == null &&
                firstInstallTime != null &&
                count >= MIN_LAUNCH_COUNT &&
                currentTime - firstInstallTime >= MAX_DAYS_BEFORE_FIRST_REVIEW

    val askBiggerOne =
        lastAskedTime != null &&
                count >= MIN_LAUNCH_COUNT &&
                currentTime - lastAskedTime >= MAX_DAY_AFTER_FIRST_REVIEW

    // if any one is true ask
    if (force || askFirstReview || askBiggerOne)
        ask(activity, preferences)
    /*val fAnalystics = Firebase.analytics
    fAnalystics.("Review Forced: $force, First: $askFirstReview, lastAsked: $askBiggerOne")*/
}

private suspend inline fun ReviewManager.ask(
    activity: Activity,
    preferences: Preferences
) {
    // The flow has finished. The API does not indicate whether the user
    // reviewed or not, or even whether the review dialog was shown. Thus, no
    // matter the result, we continue our app flow.
    com.primex.core.runCatching(TAG) {
        // update the last asking
        preferences[KEY_LAST_REVIEW_TIME] = System.currentTimeMillis()
        val info = requestReview()
        launchReviewFlow(activity, info)
    }
}
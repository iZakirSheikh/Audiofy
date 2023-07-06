package com.prime.media

import android.animation.ObjectAnimator
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.LocalElevationOverlay
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.twotone.Memory
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.core.animation.doOnEnd
import androidx.core.content.ContextCompat
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
import com.prime.media.core.compose.Channel
import com.prime.media.core.compose.Channel.Duration
import com.prime.media.core.compose.LocalsSystemFacade
import com.prime.media.core.compose.SystemFacade
import com.prime.media.core.compose.preference
import com.prime.media.core.db.findAudio
import com.prime.media.core.playback.Remote
import com.prime.media.core.util.toMediaItem
import com.primex.core.MetroGreen
import com.primex.core.Text
import com.primex.material2.OutlinedButton
import com.primex.preferences.*
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

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
    Surface(color = Material.colors.background, modifier = Modifier.fillMaxSize()) {
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
                border = ButtonDefaults.outlinedBorder,
                shape = CircleShape,
                colors = ButtonDefaults.outlinedButtonColors(backgroundColor = Color.Transparent)
            )
        }
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


private val STORAGE_PERMISSION =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
        android.Manifest.permission.READ_MEDIA_AUDIO
    else
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE

@AndroidEntryPoint
class MainActivity : ComponentActivity(), SystemFacade {
    private val fAnalytics by lazy { FirebaseAnalytics.getInstance(this) }
    private val advertiser by lazy { Advertiser(this) }
    private val billingManager by lazy { BillingManager(this, arrayOf(Product.DISABLE_ADS)) }


    override val inAppUpdateProgress: State<Float> =
        mutableFloatStateOf(Float.NaN)

    // injectable code.
    @Inject
    lateinit var preferences: Preferences

    @Inject
    override lateinit var channel: Channel

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
            channel.show(title, text, action, icon, accent, duration)
        }
    }

    override fun show(
        title: String,
        text: String,
        action: String?,
        icon: Any?,
        accent: Color,
        duration: Duration
    ) {
        lifecycleScope.launch {
            channel.show(title, text, action, icon, accent, duration)
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
                val channel = channel
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
                            if (res == Channel.Result.ActionPerformed) manager.completeUpdate()
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
        // pass intetn to onNewIntent
        onNewIntent(intent)
        // set content to the app.
        setContent {
            // observe the change to density
            val density = LocalDensity.current
            val fontScale by observeAsState(Audiofy.FONT_SCALE)
            val modified = Density(density = density.density, fontScale = fontScale)
            // ask different permission as per api level.
            val permission =
                rememberPermissionState(
                    STORAGE_PERMISSION
                )
            CompositionLocalProvider(
                LocalElevationOverlay provides null,
                //LocalWindowSizeClass provides sWindow,
                LocalDensity provides modified,
                LocalsSystemFacade provides this@MainActivity,
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        if (intent == null || intent.action != Intent.ACTION_VIEW)
            return
        // This message will not be shown, since home is not loaded yet.
        val isPermitted =
            ContextCompat.checkSelfPermission(this, STORAGE_PERMISSION) == PackageManager.PERMISSION_GRANTED
        if (!isPermitted) {
            show(
                "Storage Permission",
                "This app requires access to your device's storage to function properly. Please grant permission to continue.",
                icon = Icons.TwoTone.Memory
            )
            return
        }
        val data = intent?.data ?: return
        lifecycleScope.launch {
            val audio = runCatching { findAudio(data) }.getOrNull()
            if (audio == null) {
                show(
                    "Error",
                    "An error has occurred. We apologize for the inconvenience. Please wait while we address this issue."
                )
                return@launch
            }
            remote.set(listOf(audio.toMediaItem))
            remote.play()
        }
    }
}
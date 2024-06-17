package com.prime.media

import android.animation.ObjectAnimator
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.animation.doOnEnd
import androidx.core.app.ShareCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.ktx.AppUpdateResult
import com.google.android.play.core.ktx.requestAppUpdateInfo
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.ktx.requestUpdateFlow
import com.google.android.play.core.review.ReviewManagerFactory
import com.prime.media.core.billing.Advertiser
import com.prime.media.core.billing.BillingManager
import com.prime.media.core.billing.get
import com.prime.media.core.billing.observeAsState
import com.prime.media.core.billing.purchased
import com.prime.media.core.compose.Channel
import com.prime.media.core.compose.Channel.Duration
import com.prime.media.core.compose.LocalSystemFacade
import com.prime.media.core.compose.LocalWindowSize
import com.prime.media.core.compose.SystemFacade
import com.prime.media.core.compose.calculateWindowSizeClass
import com.prime.media.core.compose.preference
import com.prime.media.core.playback.Remote
import com.prime.media.settings.Settings
import com.primex.core.MetroGreen
import com.primex.core.OrientRed
import com.primex.core.Text
import com.primex.preferences.Key
import com.primex.preferences.Preferences
import com.primex.preferences.longPreferenceKey
import com.primex.preferences.observeAsState
import com.primex.preferences.value
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject

private const val TAG = "MainActivity"

private const val MIN_LAUNCH_COUNT = 5
private val MAX_DAYS_BEFORE_FIRST_REVIEW = TimeUnit.DAYS.toMillis(3)
private val MAX_DAY_AFTER_FIRST_REVIEW = TimeUnit.DAYS.toMillis(5)

private val KEY_LAST_REVIEW_TIME =
    longPreferenceKey(TAG + "_last_review_time")

private const val FLEXIBLE_UPDATE_MAX_STALENESS_DAYS = 2
private const val RESULT_CODE_APP_UPDATE = 1000

/**
 * Manages SplashScreen
 */
context(ComponentActivity)
private fun initSplashScreen(isColdStart: Boolean) {
    // Install Splash Screen and Play animation when cold start.
    installSplashScreen().let { screen ->
        // Animate entry of content
        if (!isColdStart)
            return@let
        screen.setOnExitAnimationListener { provider ->
            val splashScreenView = provider.view
            // Create your custom animation.
            val alpha = ObjectAnimator.ofFloat(
                splashScreenView, View.ALPHA, 1f, 0f
            )
            alpha.interpolator = AnticipateInterpolator()
            alpha.duration = 700L
            // Call SplashScreenView.remove at the end of your custom animation.
            alpha.doOnEnd { provider.remove() }
            // Run your animation.
            alpha.start()
        }
    }
}

@AndroidEntryPoint
class MainActivity : ComponentActivity(), SystemFacade {

    private val advertiser by lazy { Advertiser(this) }
    private val billingManager by lazy {
        BillingManager(
            this,
            arrayOf(
                BuildConfig.IAP_NO_ADS,
                BuildConfig.IAP_TAG_EDITOR_PRO,
                BuildConfig.IAP_BUY_ME_COFFEE,
                BuildConfig.IAP_CODEX
            )
        )
    }

    private val _inAppUpdateProgress = mutableFloatStateOf(Float.NaN)
    override val inAppUpdateProgress: Float
        get() = _inAppUpdateProgress.floatValue

    override val inAppProductDetails get() = billingManager.details

    // injectable code.
    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var channel: Channel

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

    override fun launch(intent: Intent, options: Bundle?) = startActivity(intent, options)

    override fun showAd(force: Boolean, action: (() -> Unit)?) {
        val isAdFree = billingManager[BuildConfig.IAP_NO_ADS].purchased
        if (isAdFree) return // don't do anything
        advertiser.show(this, force, action)
    }

    override fun show(
        message: Text,
        title: Text?,
        action: Text?,
        icon: Any?,
        accent: Color,
        duration: Duration,
        onAction: (() -> Unit)?
    ) {
        lifecycleScope.launch {
            val res = channel.show(message, title, action, icon, accent, duration)
            if (onAction == null)
                return@launch
            // invoke on action.
            if (res == Channel.Result.ActionPerformed)
                onAction()
        }
    }

    override fun launchAppStore() {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(Audiofy.GOOGLE_STORE)).apply {
            setPackage(Audiofy.PKG_GOOGLE_PLAY_STORE)
            addFlags(
                Intent.FLAG_ACTIVITY_NO_HISTORY
                        or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                        or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            )
        }
        val res = kotlin.runCatching { startActivity(intent) }
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
            val firstInstallTime = (application as Audiofy).packageInfo?.firstInstallTime
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
                manager.requestUpdateFlow().collect { result ->
                    when (result) {
                        AppUpdateResult.NotAvailable ->
                            if (report) channel.show(R.string.msg_no_new_update_available)

                        is AppUpdateResult.InProgress -> {
                            val state = result.installState
                            val total = state.totalBytesToDownload()
                            val downloaded = state.bytesDownloaded()
                            val progress = when {
                                total <= 0 -> -1f
                                total == downloaded -> Float.NaN
                                else -> downloaded / total.toFloat()
                            }
                            _inAppUpdateProgress.floatValue = progress
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
                                message = R.string.msg_new_update_downloaded,
                                action = R.string.update,
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

    override fun shareApp() {
        ShareCompat.IntentBuilder(this).setType("text/plain")
            .setChooserTitle(getString(R.string.app_name))
            .setText(getString(R.string.share_app_desc_s, Audiofy.GOOGLE_STORE)).startChooser()
    }

    override fun launchEqualizer(id: Int) {
        lifecycleScope.launch {
            if (id == AudioEffect.ERROR_BAD_VALUE)
                return@launch show(R.string.msg_unknown_error, R.string.error)
            val result = kotlin.runCatching {
                startActivity(
                    Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                        putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                        putExtra(AudioEffect.EXTRA_AUDIO_SESSION, id)
                        putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC);
                    }
                )
            }
            if (!result.isFailure)
                return@launch
            val res = channel.show(
                message = R.string.msg_3rd_party_equalizer_not_found,
                action = R.string.launch,
                accent = Color.OrientRed,
                duration = Duration.Short
            )
            if (res != Channel.Result.ActionPerformed)
                return@launch
            runCatching {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = Uri.parse("market://search?q=equalizer")
                startActivity(intent)
            }
        }
    }


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

            // pass intent to onNewIntent; but only when cold start; so that it is not called
            // multiple times
            lifecycleScope.launch {
                // Introducing a delay of 1000 milliseconds (1 second) here is essential
                // to ensure that the UI is fully prepared to receive the intent.
                // This delay gives the UI components time to initialize and be ready
                // to handle the incoming intent without any potential issues.
                delay(1000)
                onNewIntent(intent)
            }
        }
        // Manually handle decor.
        // I think I am handling this in AppTheme Already.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Set the content.
        setContent {
            val windowSizeClass = calculateWindowSizeClass(activity = this)

            // Observe font_scale
            val fontScale by observeAsState(key = Settings.FONT_SCALE)
            val density = LocalDensity.current
            val modified = if (fontScale == -1f) density else Density(density.density, fontScale)

            CompositionLocalProvider(
                LocalSystemFacade provides this,
                LocalWindowSize provides windowSizeClass,
                LocalDensity provides modified,
                content = { Home(channel = channel) }
            )
        }
    }
}
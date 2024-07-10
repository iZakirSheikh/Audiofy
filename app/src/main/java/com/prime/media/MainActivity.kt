package com.prime.media

import android.animation.ObjectAnimator
import android.content.Intent
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnticipateInterpolator
import android.widget.FrameLayout
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
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
import com.google.android.play.core.splitinstall.SplitInstallManager
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.ktx.Firebase
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
import com.prime.media.core.playback.Remote
import com.prime.media.settings.Settings
import com.primex.core.MetroGreen
import com.primex.core.OrientRed
import com.primex.core.Text
import com.primex.core.getText2
import com.primex.preferences.Key
import com.primex.preferences.Preferences
import com.primex.preferences.longPreferenceKey
import com.primex.preferences.observeAsState
import com.primex.preferences.value
import com.zs.ads.AdError
import com.zs.ads.AdInfo
import com.zs.ads.AdListener
import com.zs.ads.AdManager
import com.zs.ads.AdView
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
 * @return the purchase associated with the [id]
 */
// TODO - Why Purchase has list of products.
private fun BillingManager.getPurchase(id: String) =
    purchases.value.find { it.products.contains(id) }

/**
 * Checks if the product represents a dynamic feature.
 */
private val ProductDetails.isDynamicFeature
    inline get() = this.productId == BuildConfig.IAP_CODEX

/**
 * The name of the on-demand module for the Codex feature.
 */
private const val ON_DEMAND_MODULE_CODEX = "codex"

/**
 * Returns the name of the dynamic module associated with the product.
 *
 * @throws IllegalStateException if the product is not a dynamic module.
 */
private val ProductDetails.dynamicModuleName
    inline get() = when (productId) {
        BuildConfig.IAP_CODEX -> ON_DEMAND_MODULE_CODEX
        else -> error("$productId is not a dynamic module.")
    }

/**
 * Checks if a dynamic module with the given name is installed.
 *
 * @param id The name of the dynamic module.
 * @return True if the module is installed, false otherwise.
 */
private fun SplitInstallManager.isInstalled(id: String): Boolean =
    installedModules.contains(id)

/**
 * Creates a SplitInstallRequest for the dynamic feature associated with the product.
 */
private val ProductDetails.dynamicFeatureRequest
    inline get() = SplitInstallRequest.newBuilder().addModule(dynamicModuleName).build()

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

/**
 * The number of messages available to be displayed to the user.
 *
 * Each number from 0 until [MESSAGE_COUNT] represents a unique message ID. This can be used
 * to randomly select a message after a fresh start (or a multiple of 3 fresh starts)
 * and display an indefinite message to the user, such as prompting them to purchase
 * a feature like an ad-free experience.
 */
private const val MESSAGE_COUNT = 4

@AndroidEntryPoint
class MainActivity : ComponentActivity(), SystemFacade {

    private val advertiser = AdManager().apply {
        iListener = { info ->
            // Log ad impression event to Firebase Analytics
            Firebase.analytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION) {
                param(FirebaseAnalytics.Param.AD_PLATFORM, "IronSource")
                param(FirebaseAnalytics.Param.AD_UNIT_NAME, info.country)
                param(FirebaseAnalytics.Param.AD_FORMAT, info.format)
                param(FirebaseAnalytics.Param.AD_SOURCE, info.network)
                param(FirebaseAnalytics.Param.VALUE, info.revenue)
                // All IronSource revenue is sent in USD
                param(FirebaseAnalytics.Param.CURRENCY, "USD")
            }
        }
    }
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

    // Cache the banner in main activity.
    private val _cachedBannerView by lazy {
        AdView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
            )
            // Initially hide the ad view
            visibility = View.GONE

            // Set up the AdListener to handle ad events
            val fAnalytics = Firebase.analytics
            listener = object : AdListener {
                override fun onAdLoaded(info: AdInfo?) {
                    Log.d(TAG, "onAdLoaded: $info")
                    visibility = View.VISIBLE // Show the ad view when loaded
                    // Log ad impression event to Firebase Analytics
                    fAnalytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION){
                        param(FirebaseAnalytics.Param.AD_PLATFORM, "IronSource")
                        param(FirebaseAnalytics.Param.AD_UNIT_NAME, info?.country ?: "")
                        param(FirebaseAnalytics.Param.AD_FORMAT, info?.format ?: "")
                        param(FirebaseAnalytics.Param.AD_SOURCE, info?.network ?: "")
                        param(FirebaseAnalytics.Param.VALUE, info?.revenue ?: 0.0)
                        // All IronSource revenue is sent in USD
                        param(FirebaseAnalytics.Param.CURRENCY, "USD")
                    }
                }

                override fun onAdFailedToLoad(error: AdError?) {
                    Log.d(TAG, "onAdFailedToLoad: $error")
                    visibility = View.GONE // Hide the ad view if loading failed
                    loadAd() //Retry loading the ad with the provided key
                }
            }
        }
    }
    private var _bannerViewBackingField: View? by mutableStateOf(null)

    override var bannerAd: View?
        get() {
            // If the BannerView has not been attached to a parent yet...
            return if (_cachedBannerView.parent == null) {
                _bannerViewBackingField = _cachedBannerView
                _bannerViewBackingField
            }
            else _bannerViewBackingField
        }
        set(value) {
            if (value != null)
                error("Setting a non-null ($value) to bannerAd is not supported. Use null to release the banner.")
            // Release the cached bannerView and detach it from its parent
            _bannerViewBackingField = null
            (_cachedBannerView.parent as? ViewGroup)?.removeView(_cachedBannerView)
            // Reset the backing field to trigger recomposition and indicate banner availability
            _bannerViewBackingField = _cachedBannerView
        }


    override fun onPause() {
        super.onPause()
        advertiser.onPause(this)
        Log.d(TAG, "onPause: ")
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
        advertiser.onResume(this)
        Log.d(TAG, "onResume: ")
    }

    override fun onDestroy() {
        billingManager.release()
        super.onDestroy()
        _cachedBannerView.release()
        Log.d(TAG, "onDestroy: ")
    }

    override fun launch(intent: Intent, options: Bundle?) = startActivity(intent, options)

    override fun showAd(force: Boolean) {
        val isAdFree = billingManager[BuildConfig.IAP_NO_ADS].purchased
        if (isAdFree) return // don't do anything
        advertiser.show(force)
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

    private suspend fun showPromotionalMessage(id: Int) {
        when (id) {
            0 -> {
                val productId = BuildConfig.IAP_NO_ADS
                val purchase = billingManager.getPurchase(productId)
                // skip this and move to next.
                if (purchase?.purchased == true) return showPromotionalMessage(id + 1)
                val product = billingManager.details.value[productId]
                val price = product?.oneTimePurchaseOfferDetails?.formattedPrice ?: "0.0$"
                val result = channel.show(
                    resources.getText2(id = R.string.msg_ad_free_experience_ss, price),
                    action = resources.getText2(id = R.string.unlock),
                    duration = Duration.Indefinite
                )
                if (result == Channel.Result.ActionPerformed)
                    launchBillingFlow(productId)
            }

            1 -> {
                val productId = BuildConfig.IAP_CODEX
                val purchase = billingManager.getPurchase(productId)
                if (purchase?.purchased == true) return showPromotionalMessage(id + 1)
                val product = billingManager.details.value[productId]
                val price = product?.oneTimePurchaseOfferDetails?.formattedPrice ?: "0.0$"
                val result = channel.show(
                    resources.getText2(id = R.string.msg_unlock_codex_ss, price),
                    action = resources.getText2(id = R.string.unlock),
                    duration = Duration.Indefinite
                )
                if (result == Channel.Result.ActionPerformed)
                    launchBillingFlow(productId)
            }

            2 -> {
                val productId = BuildConfig.IAP_BUY_ME_COFFEE
                val purchase = billingManager.getPurchase(productId)
                if (purchase?.purchased == true) return showPromotionalMessage(id + 1)
                //val product = billingManager.details.value[productId]
                //val price = product?.oneTimePurchaseOfferDetails?.formattedPrice ?: "0.0$"
                val result = channel.show(
                    R.string.msg_library_buy_me_a_coffee,
                    action = R.string.thanks,
                    duration = Duration.Indefinite
                )
                if (result == Channel.Result.ActionPerformed)
                    launchBillingFlow(productId)
            }

            3 -> {
                val productId = BuildConfig.IAP_TAG_EDITOR_PRO
                val purchase = billingManager.getPurchase(productId)
                if (purchase?.purchased == true) return showPromotionalMessage(id + 1)
                val product = billingManager.details.value[productId]
                val price = product?.oneTimePurchaseOfferDetails?.formattedPrice ?: "0.0$"
                val result = channel.show(
                    resources.getText2(id = R.string.msg_unlock_tag_editor_pro_ss, price),
                    action = resources.getText2(id = R.string.unlock),
                    duration = Duration.Indefinite
                )
                if (result == Channel.Result.ActionPerformed)
                    launchBillingFlow(productId)
            }
        }
    }

    /**
     * Initializes the app after a cold start. This includes incrementing the launch counter,
     * checking for updates, and handling any pending intents. It also triggers the display
     * of promotional messages at specific intervals.
     */
    private fun initialize() {
        val counter = preferences.value(Audiofy.KEY_LAUNCH_COUNTER) ?: 0
        // Increment launch counter for cold starts
        preferences[Audiofy.KEY_LAUNCH_COUNTER] = counter + 1
        // Check for updates silently on startup
        launchUpdateFlow()
        // Handle pending intents after a brief delay to ensure UI readiness
        lifecycleScope.launch {
            // Introducing a delay of 1000 milliseconds (1 second) here is essential
            // to ensure that the UI is fully prepared to receive the intent.
            // This delay gives the UI components time to initialize and be ready
            // to handle the incoming intent without any potential issues.
            delay(1000)
            onNewIntent(intent)
        }
        // Display promotional messages on every third cold start
        lifecycleScope.launch {
            delay(3000)
            // Select and display a promotional message based on launch count
            val id = counter % MESSAGE_COUNT
            // Display the selected promotional message.
            Log.d(TAG, "onCreate: id: $id counter: $counter")
            showPromotionalMessage(id)
        }
        // Observe active purchases and prompt the user to install any purchased dynamic features.
        val manager = SplitInstallManagerFactory.create(this@MainActivity)
        // Observe active purchases and prompt the user to install any purchased dynamic features.
        lifecycleScope.launch {
            billingManager.purchases.collect() { purchases ->
                purchases.forEach { purchase ->
                    // Skip if the purchase is not completed
                    if (!purchase.purchased) return@forEach

                    val productId = purchase.products.first()
                    val details = billingManager.details.value[productId]
                    // Skip if product details are unavailable or the
                    // product is not a dynamic feature
                    if (details == null || !details.isDynamicFeature)
                        return@forEach
                    val dynamicModuleName = details.dynamicModuleName
                    // Skip if the dynamic feature is already installed
                    if (manager.isInstalled(dynamicModuleName))
                        return@forEach
                    // Prompt the user to install the dynamic feature
                    val response = channel.show(
                        resources.getText2(
                            id = R.string.msg_install_dynamic_module_ss,
                            details.name
                        ),
                        duration = Duration.Indefinite,
                        action = resources.getText2(R.string.install)
                    )
                    // Initiate the installation if the user chooses to install
                    if (response != Channel.Result.ActionPerformed)
                        return@forEach
                    manager.startInstall(details.dynamicFeatureRequest)
                }
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
        if (isColdStart) initialize()
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
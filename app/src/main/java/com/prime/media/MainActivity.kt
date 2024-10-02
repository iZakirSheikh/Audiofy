package com.prime.media

import android.content.Intent
import android.media.audiofx.AudioEffect
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdsClick
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.core.app.ShareCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.zs.core.paymaster.ProductInfo as ProductDetails
import com.zs.core.paymaster.Purchase
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
import com.zs.core.paymaster.Paymaster as BillingManager
import com.zs.core.paymaster.purchased
import com.prime.media.common.LocalSystemFacade
import com.zs.core_ui.LocalWindowSize
import com.prime.media.common.SystemFacade
import com.zs.core_ui.calculateWindowSizeClass
import com.prime.media.core.playback.Remote
import com.prime.media.settings.Settings
import com.primex.core.Amber
import com.primex.core.MetroGreen
import com.primex.core.MetroGreen2
import com.primex.core.OrientRed
import com.primex.core.getText2
import com.primex.core.runCatching
import com.primex.preferences.Key
import com.primex.preferences.Preferences
import com.primex.preferences.longPreferenceKey
import com.primex.preferences.observeAsState
import com.primex.preferences.value
import com.zs.ads.AdData
import com.zs.ads.AdEventListener
import com.zs.ads.AdManager
import com.zs.ads.AdSize
import com.zs.ads.Reward
import com.zs.core_ui.toast.Toast
import com.zs.core_ui.toast.ToastHostState
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.SharingStarted.Companion.WhileSubscribed
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

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
    purchases.value.find { it.id == id }

/**
 * Checks if the product represents a dynamic feature.
 */
private val ProductDetails.isDynamicFeature
    inline get() = this.id == BuildConfig.IAP_CODEX

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
    inline get() = when (id) {
        BuildConfig.IAP_CODEX -> ON_DEMAND_MODULE_CODEX
        else -> error("$id is not a dynamic module.")
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
private const val MESSAGE_COUNT = 5

private val IAP_ARRAY = arrayOf(
    BuildConfig.IAP_NO_ADS,
    BuildConfig.IAP_TAG_EDITOR_PRO,
    BuildConfig.IAP_BUY_ME_COFFEE,
    BuildConfig.IAP_CODEX,
    BuildConfig.IAP_WIDGETS_PLATFORM,
    BuildConfig.IAP_PLATFORM_WIDGET_IPHONE,
    BuildConfig.IAP_PLATFORM_WIDGET_SNOW_CONE,
    BuildConfig.IAP_PLATFORM_WIDGET_RED_VIOLET_CAKE,
    BuildConfig.IAP_PLATFORM_WIDGET_TIRAMISU,
    /*Color Croft Widget bundle*/
    BuildConfig.IAP_COLOR_CROFT_WIDGET_BUNDLE,
    BuildConfig.IAP_COLOR_CROFT_GRADIENT_GROVES,
    BuildConfig.IAP_COLOR_CROFT_GOLDEN_DUST,
)

/**
 * The amount of mills that are rewarded to user once they watch an ad.
 */
private val KEY_AD_FREE_REWARD_MILLS =
    longPreferenceKey("ad_free_reward_millis", 0L)
private val AD_FREE_TIME_REWARD = 1.days

@AndroidEntryPoint
class MainActivity : ComponentActivity(), SystemFacade, AdEventListener {

    private val advertiser by lazy {
        AdManager().apply { listener = this@MainActivity }
    }
    private val billingManager by lazy {
        BillingManager(this, BuildConfig.PLAY_CONSOLE_APP_RSA_KEY, IAP_ARRAY)
    }

    // Cache the banner in main activity.
    private var _bannerViewBackingField: View? by mutableStateOf(null)

    // The states the reflect the change in the dependent variables
    override var isRewardedVideoAvailable: Boolean by mutableStateOf(false)
    override var bannerAd: View?
        get() {
            // If the BannerView has not been attached to a parent yet...
            val _cachedBannerView = advertiser.banner(this)
            return if (_cachedBannerView.parent == null) {
                _bannerViewBackingField = _cachedBannerView
                _bannerViewBackingField
            } else _bannerViewBackingField
        }
        set(value) {
            if (value != null)
                error("Setting a non-null ($value) to bannerAd is not supported. Use null to release the banner.")
            // Release the cached bannerView and detach it from its parent
            _bannerViewBackingField = null
            val _cachedBannerView = advertiser.banner(this)
            (_cachedBannerView.parent as? ViewGroup)?.removeView(_cachedBannerView)
            // Reset the backing field to trigger recomposition and indicate banner availability
            _bannerViewBackingField = _cachedBannerView
        }

    override var adFreePeriodEndTimeMillis: Long by mutableLongStateOf(0)
    override var isAdFreeVersion: Boolean by mutableStateOf(false)

    override val isAdFree: Boolean by derivedStateOf {
        isAdFreeVersion || isAdFreeRewarded
    }
    override val isAdFreeRewarded: Boolean by derivedStateOf {
        adFreePeriodEndTimeMillis - System.currentTimeMillis() > 0f
    }

    override fun showRewardedVideo() {
        lifecycleScope.launch {
            val timeTobeAdded = AD_FREE_TIME_REWARD.inWholeDays
            // Store the new end time
            val saved = preferences.value(KEY_AD_FREE_REWARD_MILLS)
                .coerceAtLeast(System.currentTimeMillis())
            // Calculate remaining ad-free days for display
            val remaining = (saved - System.currentTimeMillis()).milliseconds.toString()
            // log to firebase
            Firebase.analytics.logEvent("click_claim_reward") {
                param(FirebaseAnalytics.Param.ITEM_NAME, "claim_reward")
                param("reward_type", "24hrs_ad_free")
            }
            val result = channel.showToast(
                message = resources.getText2(
                    R.string.msg_claim_ad_free_reward_ds,
                    timeTobeAdded,
                    remaining
                ),
                icon = Icons.Outlined.AdsClick,
                action = getString(R.string.claim).uppercase(),
                duration = Toast.DURATION_INDEFINITE,
                accent = Color.MetroGreen2
            )
            if (result == Toast.ACTION_PERFORMED) {
                advertiser.showRewardedAd()
                // Log the ad_claiming_reward event to Firebase
                Firebase.analytics.logEvent("ad_claiming_reward") {
                    param("remaining_days", remaining)
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        advertiser.onPause(this)
        Log.d(TAG, "onPause: ")
    }

    private val _inAppUpdateProgress =
        mutableFloatStateOf(Float.NaN)
    override val inAppUpdateProgress: Float
        get() = _inAppUpdateProgress.floatValue

    override val inAppProductDetails by lazy {
        billingManager.details.map { it.associateBy { it.id } }
            .stateIn(lifecycleScope, WhileSubscribed(1000), emptyMap())
    }

    // injectable code.
    @Inject
    lateinit var preferences: Preferences

    @Inject
    lateinit var channel: ToastHostState

    @Inject
    lateinit var remote: Remote

    override fun loadBannerAd(size: AdSize) =
        advertiser.load(size)

    override fun onAdImpression(value: AdData.AdImpression?) {
        Log.d(TAG, "onAdImpression: $value")
        // Safely cast to AdImpression, return if null// Log ad impression event to Firebase Analytics
        val data = value ?: return
        // Log ad impression event to Firebase Analytics
        Firebase.analytics.logEvent(FirebaseAnalytics.Event.AD_IMPRESSION) {
            param(FirebaseAnalytics.Param.AD_PLATFORM, "IronSource")
            param(FirebaseAnalytics.Param.AD_UNIT_NAME, data.name)
            param(FirebaseAnalytics.Param.AD_FORMAT, data.format)
            param(FirebaseAnalytics.Param.AD_SOURCE, data.network)
            param(FirebaseAnalytics.Param.VALUE, data.revenue)
            // All IronSource revenue is sent in USD
            param(FirebaseAnalytics.Param.CURRENCY, "USD")
        }
    }

    override fun onAdRewarded(reward: Reward?, info: AdData.AdInfo?) {
        // TODO - Maybe use the [reward] to reward the users.
        // User has watched a rewarded ad, grant ad-free time
        // Calculate the new end time of the ad-free period
        val old = preferences.value(KEY_AD_FREE_REWARD_MILLS)
            .coerceAtLeast(System.currentTimeMillis())

        val new = old + AD_FREE_TIME_REWARD.inWholeMilliseconds
        // Store the new end time
        preferences[KEY_AD_FREE_REWARD_MILLS] = new
        // Calculate remaining ad-free days for display
        val remaining = TimeUnit.MILLISECONDS.toDays(new - System.currentTimeMillis())
        // Show a celebratory message to the user
        // Exit the function as the reward has been processed
        show(
            message = resources.getText2(
                R.string.msg_ad_free_time_rewarded_dd,
                AD_FREE_TIME_REWARD.inWholeDays,
                remaining
            ),
            icon = Icons.Outlined.AdsClick,
            duration = Toast.DURATION_INDEFINITE,
            accent = Color.MetroGreen,
        )
    }

    override fun onAdEvent(event: String, data: AdData?) {
        Log.d(TAG, "onAdEvent: $event, $data")
        // Update if rewarded video is available
        if (event == AdManager.AD_EVENT_LOADED) {
            isRewardedVideoAvailable = advertiser.isRewardedVideoAvailable
            return
        }
    }

    override fun onResume() {
        super.onResume()
        billingManager.sync()
        advertiser.onResume(this)
        Log.d(TAG, "onResume: ")
    }

    override fun onDestroy() {
        billingManager.release()
        // FIXME - What if no-one called get on advertiser; in this case releaseing it actually
        //  caused it to load ads. but since app is closing this might not be the issue
        advertiser.release()
        super.onDestroy()
        Log.d(TAG, "onDestroy: ")
    }

    override fun launch(
        intent: Intent,
        options: Bundle?
    ) = startActivity(intent, options)

    override fun showAd(force: Boolean) {
        if (isAdFree) return // don't do anything
        advertiser.show(force)
    }

    override fun show(message: CharSequence, icon: ImageVector?, accent: Color, duration: Int) {
        lifecycleScope.launch {
            channel.showToast(message, null, icon, accent, duration)
        }
    }

    override fun show(message: Int, icon: ImageVector?, accent: Color, duration: Int) {
        lifecycleScope.launch {
            channel.showToast(resources.getText2(message), null, icon, accent, duration)
        }
    }

    override fun launchAppStore(id: String) {
        val url = "market://details?id=$id"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            setPackage(Audiofy.PKG_GOOGLE_PLAY_STORE)
            addFlags(
                Intent.FLAG_ACTIVITY_NO_HISTORY
                        or Intent.FLAG_ACTIVITY_NEW_DOCUMENT
                        or Intent.FLAG_ACTIVITY_MULTIPLE_TASK
            )
        }
        val res = kotlin.runCatching { startActivity(intent) }
        if (res.isFailure) {
            val fallback = "http://play.google.com/store/apps/details?id=$id"
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(fallback)))
        }
    }

    override fun launchBillingFlow(id: String) {
        billingManager.initiatePurchaseFlow(this, id)
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
        produceState(remember { billingManager.purchases.value.find { it.id == product } }) {
            billingManager.purchases.map { it.find { it.id == product } }.collect {
                value = it
            }
        }

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
            runCatching(TAG) {
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
            runCatching(TAG) {
                val manager = AppUpdateManagerFactory.create(this@MainActivity)
                manager.requestUpdateFlow().collect { result ->
                    when (result) {
                        AppUpdateResult.NotAvailable ->
                            if (report) channel.showToast(resources.getText2(R.string.msg_no_new_update_available))

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
                            val res = channel.showToast(
                                message = resources.getText2(R.string.msg_new_update_downloaded),
                                action = resources.getText2(R.string.update),
                                duration = Toast.DURATION_INDEFINITE,
                                accent = Color.MetroGreen
                            )
                            // complete update when ever user clicks on action.
                            if (res == Toast.ACTION_PERFORMED) manager.completeUpdate()
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
            .setText(getString(R.string.desc_share_app_s, Audiofy.GOOGLE_STORE)).startChooser()
    }

    override fun launchEqualizer(id: Int) {
        lifecycleScope.launch {
            if (id == AudioEffect.ERROR_BAD_VALUE)
                return@launch show(R.string.msg_unknown_error)
            val result = kotlin.runCatching {
                startActivity(
                    Intent(AudioEffect.ACTION_DISPLAY_AUDIO_EFFECT_CONTROL_PANEL).apply {
                        putExtra(AudioEffect.EXTRA_PACKAGE_NAME, packageName)
                        putExtra(AudioEffect.EXTRA_AUDIO_SESSION, id)
                        putExtra(AudioEffect.EXTRA_CONTENT_TYPE, AudioEffect.CONTENT_TYPE_MUSIC)
                    }
                )
            }
            if (!result.isFailure)
                return@launch
            val res = channel.showToast(
                message = resources.getText2(R.string.msg_3rd_party_equalizer_not_found),
                action = resources.getText2(R.string.launch),
                accent = Color.OrientRed,
                duration = Toast.DURATION_SHORT
            )
            if (res != Toast.ACTION_PERFORMED)
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
                val product = billingManager.details.value.find { it.id == productId }
                val price = product?.formattedPrice ?: "0.0$"
                val result = channel.showToast(
                    resources.getText2(id = R.string.msg_ad_free_experience_ss, price),
                    action = resources.getText2(id = R.string.unlock),
                    duration = Toast.DURATION_INDEFINITE
                )
                if (result == Toast.ACTION_PERFORMED)
                    launchBillingFlow(productId)
            }

            1 -> {
                val productId = BuildConfig.IAP_CODEX
                val purchase = billingManager.getPurchase(productId)
                if (purchase?.purchased == true) return showPromotionalMessage(id + 1)
                val product = billingManager.details.value.find { it.id == productId }
                val price = product?.formattedPrice ?: "0.0$"
                val result = channel.showToast(
                    resources.getText2(id = R.string.msg_unlock_codex_ss, price),
                    action = resources.getText2(id = R.string.unlock),
                    duration = Toast.DURATION_INDEFINITE
                )
                if (result == Toast.ACTION_PERFORMED)
                    launchBillingFlow(productId)
            }

            2 -> {
                val productId = BuildConfig.IAP_BUY_ME_COFFEE
                val purchase = billingManager.getPurchase(productId)
                if (purchase?.purchased == true) return showPromotionalMessage(id + 1)
                //val product = billingManager.details.value[productId]
                //val price = product?.oneTimePurchaseOfferDetails?.formattedPrice ?: "0.0$"
                val result = channel.showToast(
                    resources.getText2(R.string.msg_library_buy_me_a_coffee),
                    action = resources.getText2(R.string.thanks),
                    duration = Toast.DURATION_INDEFINITE
                )
                if (result == Toast.ACTION_PERFORMED)
                    launchBillingFlow(productId)
            }

            3 -> {
                val productId = BuildConfig.IAP_TAG_EDITOR_PRO
                val purchase = billingManager.getPurchase(productId)
                if (purchase?.purchased == true) return showPromotionalMessage(id + 1)
                val product = billingManager.details.value.find { it.id == productId }
                val price = product?.formattedPrice ?: "0.0$"
                val result = channel.showToast(
                    resources.getText2(id = R.string.msg_unlock_tag_editor_pro_ss, price),
                    action = resources.getText2(id = R.string.unlock),
                    duration = Toast.DURATION_INDEFINITE
                )
                if (result == Toast.ACTION_PERFORMED)
                    launchBillingFlow(productId)
            }

            4 -> {
                val pkg = "com.googol.android.apps.photos"
                // Check if the Gallery app is already installed
                val isInstalled = runCatching(TAG) { packageManager.getPackageInfo(pkg, 0) } != null
                // If the app is installed, show the next promotional message
                if (isInstalled) return showPromotionalMessage(id + 1)
                val result = channel.showToast(
                    resources.getText2(R.string.msg_promotion_gallery_app),
                    action = resources.getText2(R.string.dive_in),
                    duration = Toast.DURATION_INDEFINITE,
                    accent = Color.Amber,
                    icon = Icons.Outlined.GetApp
                )
                // If the user clicked the action button, launch the app store listing
                if (result == Toast.ACTION_PERFORMED)
                    launchAppStore(pkg)
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
            delay(10.seconds.inWholeMilliseconds)
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
            billingManager.purchases.collect { purchases ->
                purchases.forEach { purchase ->
                    // Skip if the purchase is not completed
                    val productId = purchase.id
                    // Update the isAdFreeVersion flag
                    if (productId == BuildConfig.IAP_NO_ADS)
                        isAdFreeVersion = purchase.purchased
                    // Skip if the purchase is not purchased
                    if (!purchase.purchased) return@forEach
                    val details = billingManager.details.value.find { it.id == productId }
                    // Skip if product details are unavailable or the
                    // product is not a dynamic feature
                    if (details == null || !details.isDynamicFeature)
                        return@forEach
                    val dynamicModuleName = details.dynamicModuleName
                    // Skip if the dynamic feature is already installed
                    if (manager.isInstalled(dynamicModuleName))
                        return@forEach
                    // Prompt the user to install the dynamic feature
                    val response = channel.showToast(
                        resources.getText2(
                            id = R.string.msg_install_dynamic_module_ss,
                            details.title
                        ),
                        duration = Toast.DURATION_INDEFINITE,
                        action = resources.getText2(R.string.install)
                    )
                    // Initiate the installation if the user chooses to install
                    if (response != Toast.ACTION_PERFORMED)
                        return@forEach
                    manager.startInstall(details.dynamicFeatureRequest)
                }
            }
        }
        // Observe Reward adFree
        preferences[KEY_AD_FREE_REWARD_MILLS].onEach {
            adFreePeriodEndTimeMillis = it
        }.launchIn(lifecycleScope)
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
            // Set the content.
            CompositionLocalProvider(
                LocalSystemFacade provides this,
                LocalWindowSize provides windowSizeClass,
                LocalDensity provides modified,
                content = { Home(channel = channel) }
            )
        }
    }
}
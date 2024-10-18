package com.prime.media


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdsClick
import androidx.compose.material.icons.outlined.ColorLens
import androidx.compose.material.icons.outlined.Downloading
import androidx.compose.material.icons.outlined.GetApp
import androidx.compose.material.icons.outlined.Whatshot
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavController.OnDestinationChangedListener
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.ktx.AppUpdateResult
import com.google.android.play.core.ktx.requestAppUpdateInfo
import com.google.android.play.core.ktx.requestProgressFlow
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.ktx.requestUpdateFlow
import com.google.android.play.core.ktx.status
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallManagerFactory
import com.google.android.play.core.splitinstall.SplitInstallRequest
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.analytics.logEvent
import com.google.firebase.crashlytics.ktx.crashlytics
import com.google.firebase.ktx.Firebase
import com.prime.media.common.MediaFile
import com.prime.media.common.SystemFacade
import com.prime.media.common.domain
import com.prime.media.common.dynamicFeatureRequest
import com.prime.media.common.dynamicModuleName
import com.prime.media.common.isDynamicFeature
import com.prime.media.common.onEachItem
import com.prime.media.common.richDesc
import com.prime.media.old.console.Console
import com.prime.media.old.core.playback.Remote
import com.prime.media.personalize.RoutePersonalize
import com.prime.media.settings.Settings
import com.primex.core.Amber
import com.primex.core.MetroGreen
import com.primex.core.MetroGreen2
import com.primex.core.getText2
import com.primex.core.runCatching
import com.primex.preferences.Key
import com.primex.preferences.Preferences
import com.primex.preferences.intPreferenceKey
import com.primex.preferences.invoke
import com.primex.preferences.longPreferenceKey
import com.primex.preferences.observeAsState
import com.primex.preferences.value
import com.zs.ads.AdData
import com.zs.ads.AdData.AdImpression
import com.zs.ads.AdEventListener
import com.zs.ads.AdManager
import com.zs.ads.AdSize
import com.zs.ads.Reward
import com.zs.core.paymaster.Paymaster
import com.zs.core.paymaster.purchased
import com.zs.core_ui.WindowStyle
import com.zs.core_ui.getPackageInfoCompat
import com.zs.core_ui.toast.Toast
import com.zs.core_ui.toast.ToastHostState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.util.concurrent.TimeUnit
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen as initSplashScreen
import com.google.android.play.core.splitinstall.model.SplitInstallSessionStatus as Flag
import com.zs.core_ui.showPlatformToast as showAndroidToast

private const val TAG = "MainActivity"

// In-app update and review settings

// Maximum staleness days allowed for a flexible update.
// If the app is older than this, an immediate update will be enforced.
private const val FLEXIBLE_UPDATE_MAX_STALENESS_DAYS = 2

// Minimum number of app launches before prompting for a review.
private const val MIN_LAUNCHES_BEFORE_REVIEW = 5

// Number of days to wait before showing the first review prompt.
private val INITIAL_REVIEW_DELAY = 3.days

// Minimum number of days between subsequent review prompts.
// Since we cannot confirm if the user actually left a review, we use this interval
// to avoid prompting too frequently.
private val STANDARD_REVIEW_DELAY = 5.days

private val KEY_LAST_REVIEW_TIME =
    longPreferenceKey(TAG + "_last_review_time", 0)

/**
 * The version code saved in app preferences, required to check if this is an update.
 */
private val KEY_APP_VERSION_CODE =
    intPreferenceKey(TAG + "_app_version_code", -1)

/**
 * The epoch time in milliseconds when the ad-free reward period ends.
 */
private val KEY_AD_FREE_REWARD_MILLS =
    longPreferenceKey("ad_free_reward_millis", 0L)

// The duration of ad-free time rewarded to the user after watching a promo ad.
private val AD_FREE_TIME_REWARD = 1.days

private val IAPs = arrayOf(
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
 * The number of messages available to be displayed to the user.
 *
 * Each number from 0 until [MESSAGE_COUNT] represents a unique message ID. This can be used
 * to randomly select a message after a fresh start (or a multiple of 3 fresh starts)
 * and display an indefinite message to the user, such as prompting them to purchase
 * a feature like an ad-free experience.
 */
private const val MESSAGE_COUNT = 6

class MainActivity : ComponentActivity(), SystemFacade, OnDestinationChangedListener,
    AdEventListener {
    // injectables
    private val toastHostState: ToastHostState by inject()
    private val preferences: Preferences by inject()

    // late-init
    private var navController: NavHostController? = null
    private val advertiser by lazy { AdManager().apply { listener = this@MainActivity } }

    @Deprecated("Will be removed soon.")
    val remote: Remote by inject()
    val paymaster by lazy {
        Paymaster(this, BuildConfig.PLAY_CONSOLE_APP_RSA_KEY, IAPs)
    }
    val splitInstallManager by lazy {
        val manager = SplitInstallManagerFactory.create(this@MainActivity)
        // Request progress updates for dynamic feature installation
        manager.requestProgressFlow()
            .onEach {state ->
                when(state.status){
                    Flag.DOWNLOADING -> {
                        // Calculate the download progress as a percentage
                        val percent = state.bytesDownloaded().toFloat() / state.totalBytesToDownload()
                        Log.d("SplitInstall", "Download progress: $percent%")
                        // Update the progress indicator
                        inAppTaskProgress = percent
                    }
                    Flag.INSTALLING, Flag.PENDING -> {
                        // Set the progress to an indeterminate state
                        inAppTaskProgress = -1f
                        Log.d("SplitInstall", "Installing...")
                    }
                    Flag.INSTALLED -> {
                        // There is a known issue when observing the state of dynamic module installations.
                        // If the user has requested the installation of the dynamic module during this session,
                        // the inAppTaskProgress flag will not be NaN once the state is reached.
                        // However, if inAppTaskProgress is NaN, it indicates that this callback was triggered due to an app restart,
                        // and no installation request was made in the current session. Therefore, we can safely ignore this state.
                        if (inAppTaskProgress.isNaN()) return@onEach
                        // Hide the progress bar
                        inAppTaskProgress = Float.NaN
                        Log.d("SplitInstall", "Module installed successfully!")
                        // Show a toast message requesting the app restart
                        val res = toastHostState.showToast(
                            "Restart the app for changes to take effect.",
                            "Restart",
                            duration = Toast.DURATION_INDEFINITE
                        )
                        // Restart the app if the user chooses to
                        if (res == Toast.ACTION_PERFORMED)
                            restart(true)
                        // The dynamic feature module can now be accessed
                    }
                    else -> {
                        // Hide the progress bar for unknown statuses
                        inAppTaskProgress = Float.NaN
                        Log.d("SplitInstall", "Unknown status: ${state.status()}")
                    }
                }
            }
            .launchIn(lifecycleScope)
        manager
    }

    // Cache the banner in main activity.
    private var _bannerViewBackingField: View? by mutableStateOf(null)
    var bannerAd: View?
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

    // some state variables
    // The states the reflect the change in the dependent variables
    override var isRewardedVideoAvailable: Boolean by mutableStateOf(false)
    override var adFreePeriodEndTimeMillis: Long by mutableLongStateOf(0)
    override var isAdFreeVersion: Boolean by mutableStateOf(false)
    override val isAdFree: Boolean by derivedStateOf { isAdFreeVersion || isAdFreeRewarded }
    override val isAdFreeRewarded: Boolean by derivedStateOf { adFreePeriodEndTimeMillis - System.currentTimeMillis() > 0f }
    override var style: WindowStyle by mutableStateOf(WindowStyle())
    override var inAppTaskProgress: Float by mutableFloatStateOf(Float.NaN)

    override fun showPlatformToast(message: String, duration: Int) =
        showAndroidToast(message, duration)

    override fun showPlatformToast(message: Int, duration: Int) =
        showAndroidToast(message, duration)

    fun loadBannerAd(size: AdSize) = advertiser.load(size)

    @Suppress("UNCHECKED_CAST")
    override fun <T> getDeviceService(name: String): T = getSystemService(name) as T
    override fun showToast(
        message: CharSequence,
        icon: ImageVector?,
        accent: Color,
        duration: Int
    ) {
        lifecycleScope.launch {
            toastHostState.showToast(message, null, icon, accent, duration)
        }
    }

    override fun showToast(message: Int, icon: ImageVector?, accent: Color, duration: Int) {
        lifecycleScope.launch {
            toastHostState.showToast(resources.getText2(id = message), null, icon, accent, duration)
        }
    }

    override fun launch(intent: Intent, options: Bundle?) = startActivity(intent, options)
    override fun initiatePurchaseFlow(id: String) = paymaster.initiatePurchaseFlow(this, id)

    override fun restart(global: Boolean) {
        // Get the launch intent for the app's main activity
        val packageManager = packageManager
        val intent = packageManager.getLaunchIntentForPackage(packageName)

        // Ensure the intent is not null
        if (intent == null) {
            Log.e("AppRestart", "Unable to restart: Launch intent is null")
            return
        }
        // restart just the current activity
        if (!global) {
            // Restart just the current activity
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()  // Finish the current activity to ensure it is restarted
            return
        }
        // Get the main component for the restart task
        val componentName = intent.component
        if (componentName == null) {
            Log.e("AppRestart", "Unable to restart: Component name is null")
            return
        }
        // Create the main restart intent and start the activity
        val mainIntent = Intent.makeRestartActivityTask(componentName)
        startActivity(mainIntent)
        // Terminate the current process to complete the restart
        Runtime.getRuntime().exit(0)
    }

    @Composable
    override fun <S, O> observeAsState(key: Key.Key1<S, O>) = preferences.observeAsState(key)

    @Composable
    override fun <S, O> observeAsState(key: Key.Key2<S, O>) = preferences.observeAsState(key)

    override fun initiateUpdateFlow(report: Boolean) {
        val manager = AppUpdateManagerFactory.create(this@MainActivity)
        manager.requestUpdateFlow().onEach { result ->
            when (result) {
                is AppUpdateResult.NotAvailable -> if (report) showToast(R.string.msg_no_new_update_available)
                is AppUpdateResult.InProgress -> {
                    val state = result.installState
                    val total = state.totalBytesToDownload()
                    val downloaded = state.bytesDownloaded()
                    val progress = when {
                        total <= 0 -> -1f
                        total == downloaded -> Float.NaN
                        else -> downloaded / total.toFloat()
                    }
                    inAppTaskProgress = progress
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
                        return@onEach
                    }
                    // else show the toast.
                    val res = toastHostState.showToast(
                        message = resources.getText2(R.string.msg_new_update_downloaded),
                        action = resources.getText2(R.string.install),
                        duration = Toast.DURATION_INDEFINITE,
                        accent = Color.MetroGreen,
                        icon = Icons.Outlined.Downloading
                    )
                    // complete update when ever user clicks on action.
                    if (res == Toast.ACTION_PERFORMED) manager.completeUpdate()
                }

                is AppUpdateResult.Available -> {
                    // if user choose to skip the update handle that case also.
                    val isFlexible = (result.updateInfo.clientVersionStalenessDays()
                        ?: -1) <= FLEXIBLE_UPDATE_MAX_STALENESS_DAYS
                    if (isFlexible) result.startFlexibleUpdate(
                        activity = this@MainActivity, 1000
                    )
                    else result.startImmediateUpdate(
                        activity = this@MainActivity, 1000
                    )
                    // no message needs to be shown
                }
            }
        }.catch {
            Firebase.crashlytics.recordException(it)
            if (!report) return@catch
            showToast(R.string.msg_update_check_error)
        }.launchIn(lifecycleScope)
    }

    override fun initiateReviewFlow() {
        lifecycleScope.launch {
            // Get the app launch count from preferences.
            val count = preferences.value(Settings.KEY_LAUNCH_COUNTER) ?: 0
            // Check if the minimum launch count has been reached.
            if (count < MIN_LAUNCHES_BEFORE_REVIEW)
                return@launch
            // Get the first install time of the app.
            // Check if enough time has passed since the first install.
            val firstInstallTime =
                packageManager.getPackageInfoCompat(BuildConfig.APPLICATION_ID)?.firstInstallTime
                    ?: 0
            val currentTime = System.currentTimeMillis()
            if (currentTime - firstInstallTime < INITIAL_REVIEW_DELAY.inWholeMilliseconds)
                return@launch
            // Get the last time the review prompt was shown.
            // Check if enough time has passed since the last review prompt.
            val lastAskedTime = preferences.value(KEY_LAST_REVIEW_TIME)
            if (currentTime - lastAskedTime <= STANDARD_REVIEW_DELAY.inWholeMilliseconds)
                return@launch

            // Request and launch the review flow.
            runCatching(TAG) {
                val reviewManager = ReviewManagerFactory.create(this@MainActivity)
                // Update the last asked time in preferences
                preferences[KEY_LAST_REVIEW_TIME] = System.currentTimeMillis()
                val info = reviewManager.requestReview()
                reviewManager.launchReviewFlow(this@MainActivity, info)
                // Optionally log an event to Firebase Analytics.
                // host.fAnalytics.logReviewPromptShown()
            }
        }
    }

    override fun onDestinationChanged(
        controller: NavController,
        destination: NavDestination,
        arguments: Bundle?
    ) {
        // Log the event.
        Firebase.analytics.logEvent(FirebaseAnalytics.Event.SCREEN_VIEW) {
            // create params for the event.
            val domain = destination.domain ?: "unknown"
            Log.d(TAG, "onNavDestChanged: $domain")
            param(FirebaseAnalytics.Param.SCREEN_NAME, domain)
        }
    }

    override fun showAd(force: Boolean) {
        if (isAdFree) return // don't do anything
        advertiser.show(force)
    }

    override fun onAdEvent(event: String, data: AdData?) {
        Log.d(TAG, "onAdEvent: $event, $data")
        // Update if rewarded video is available
        if (event == AdManager.AD_EVENT_LOADED) {
            isRewardedVideoAvailable = advertiser.isRewardedVideoAvailable
            return
        }
    }

    override fun onAdImpression(value: AdImpression?) {
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
            val result = toastHostState.showToast(
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
        showToast(
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

    override fun onPause() {
        super.onPause()
        advertiser.onPause(this)
        Log.d(TAG, "onPause: ")
    }

    override fun onResume() {
        super.onResume()
        paymaster.sync()
        advertiser.onResume(this)
        Log.d(TAG, "onResume: ")
    }

    override fun onDestroy() {
        paymaster.release()
        // FIXME - What if no-one called get on advertiser; in this case releasing it actually
        //  caused it to load ads. but since app is closing this might not be the issue
        advertiser.release()
        super.onDestroy()
        Log.d(TAG, "onDestroy: ")
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent: $intent")
        // Check if the intent action is not ACTION_VIEW; if so, return.
        if (intent.action != Intent.ACTION_VIEW)
            return
        // Obtain the URI from the incoming intent data.
        val data = intent.data ?: return
        // Use a coroutine to handle the media item construction and playback.
        lifecycleScope.launch {
            // Construct a MediaItem using the obtained parameters.
            // (Currently, details about playback queue setup are missing.)
            val item = MediaFile(this@MainActivity, data)
            // Play the media item by replacing the existing queue.
            remote.set(listOf(item))
            remote.play()
        }
        // If the intent is related to video content, navigate to the video player screen.
        navController?.navigate(Console.direction())
    }

    override fun isInstalled(id: String): Boolean {
        return splitInstallManager.installedModules.contains(id)
    }

    override fun initiateFeatureInstall(request: SplitInstallRequest) {
        splitInstallManager.startInstall(request)
    }

    /**
     *
     */
    private suspend fun showPromotionalMessage(id: Int) {
        when (id) {
            // promo message for ad-free version
            0 -> {
                val (info, purchase) = paymaster[BuildConfig.IAP_NO_ADS]
                    ?: return showPromotionalMessage(id + 1)
                // skip this and move to next.
                if (purchase.purchased) return showPromotionalMessage(id + 1)
                val result = toastHostState.showToast(
                    info.richDesc,
                    action = info.formattedPrice ?: "N/A",
                    duration = Toast.DURATION_INDEFINITE
                )
                if (result == Toast.ACTION_PERFORMED)
                    initiatePurchaseFlow(BuildConfig.IAP_NO_ADS)
            }
            // promo message for codex.
            1 -> {
                val (info, purchase) = paymaster[BuildConfig.IAP_CODEX]
                    ?: return showPromotionalMessage(id + 1)
                // skip this and move to next.
                if (purchase.purchased) return showPromotionalMessage(id + 1)
                val result = toastHostState.showToast(
                    info.richDesc,
                    action = info.formattedPrice ?: "N/A",
                    duration = Toast.DURATION_INDEFINITE
                )
                if (result == Toast.ACTION_PERFORMED)
                    initiatePurchaseFlow(BuildConfig.IAP_CODEX)
            }
            // promo message for buy me a coffee.
            2 -> {
                val (info, purchase) = paymaster[BuildConfig.IAP_BUY_ME_COFFEE]
                    ?: return showPromotionalMessage(id + 1)
                // skip this and move to next.
                if (purchase.purchased) return showPromotionalMessage(id + 1)
                val result = toastHostState.showToast(
                    info.richDesc,
                    action = info.formattedPrice ?: getText(R.string.abbr_not_available),
                    duration = Toast.DURATION_INDEFINITE
                )
                if (result == Toast.ACTION_PERFORMED)
                    initiatePurchaseFlow(BuildConfig.IAP_BUY_ME_COFFEE)
            }
            // promo message for tag editor
            3 -> {
                val (info, purchase) = paymaster[BuildConfig.IAP_TAG_EDITOR_PRO]
                    ?: return showPromotionalMessage(id + 1)
                // skip this and move to next.
                if (purchase.purchased) return showPromotionalMessage(id + 1)
                val result = toastHostState.showToast(
                    info.richDesc,
                    action = info.formattedPrice ?: getText(R.string.abbr_not_available),
                    duration = Toast.DURATION_INDEFINITE
                )
                if (result == Toast.ACTION_PERFORMED)
                    initiatePurchaseFlow(BuildConfig.IAP_TAG_EDITOR_PRO)
            }
            // promo message for app.
            4 -> {
                val pkg = "com.googol.android.apps.photos"
                // Check if the Gallery app is already installed
                val isInstalled = runCatching(TAG) { packageManager.getPackageInfo(pkg, 0) } != null
                // If the app is installed, show the next promotional message
                if (isInstalled) return showPromotionalMessage(id + 1)
                val result = toastHostState.showToast(
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
            // promo message for widgets.
            5 -> {
                val result = toastHostState.showToast(
                    "Personalize your app now with in-app widgets!",
                    duration = Toast.DURATION_INDEFINITE,
                    accent = Color.MetroGreen,
                    icon = Icons.Outlined.ColorLens,
                    action = "View"
                )
                // If the user clicked the action button, launch the Personalization screen.
                if (result == Toast.ACTION_PERFORMED)
                    navController?.navigate(RoutePersonalize())
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The app has started from scratch if savedInstanceState is null.
        // This is determined by checking if savedInstanceState is null.
        val isColdStart =
            savedInstanceState == null // A cold start occurs when there is no saved instance state.
        // Set up the splash screen
        initSplashScreen()
        if (isColdStart) {
            // check for updates
            initiateUpdateFlow()
            // Handle pending intents after a brief delay to ensure UI readiness
            // TODO: Replace this with new approach
            lifecycleScope.launch {
                // Introducing a delay of 1000 milliseconds (1 second) here is essential
                // to ensure that the UI is fully prepared to receive the intent.
                // This delay gives the UI components time to initialize and be ready
                // to handle the incoming intent without any potential issues.
                delay(1000)
                onNewIntent(intent)
            }
            // show promo message
            // update the state of variables dependent on payment master.
            // Observe active purchases and prompt the user to install any purchased dynamic features.
            paymaster.purchases.onEachItem { purchase ->
                // Skip if the purchase is not purchased
                if (!purchase.purchased) return@onEachItem
                // Update the isAdFreeVersion flag
                if (purchase.id == BuildConfig.IAP_NO_ADS) {
                    isAdFreeVersion = purchase.purchased
                    return@onEachItem
                }
                val details = paymaster.details.value.find { it.id == purchase.id }
                // Skip if product details are unavailable or the product is not a dynamic feature
                if (details == null || !details.isDynamicFeature) return@onEachItem
                // Skip if the dynamic feature is already installed
                if (isInstalled(details.dynamicModuleName)) return@onEachItem
                // Prompt the user to install the dynamic feature
                val response = toastHostState.showToast(
                    resources.getText2(
                        id = R.string.msg_install_dynamic_module_ss,
                        details.title
                    ),
                    duration = Toast.DURATION_INDEFINITE,
                    action = resources.getText2(R.string.install)
                )
                if (response == Toast.ACTION_PERFORMED)
                    initiateFeatureInstall(details.dynamicFeatureRequest)
            }.launchIn(lifecycleScope)
            // Display promotional messages on every third cold start
            // show what's new message on click.
            val savedVersionCode = preferences(KEY_APP_VERSION_CODE)
            lifecycleScope.launch {
                val counter = preferences.value(Settings.KEY_LAUNCH_COUNTER) ?: 0
                if (savedVersionCode == -1) // Don't show promo message on first launch
                    return@launch
                delay(10.seconds)
                // Select and display a promotional message based on launch count
                val id = counter % MESSAGE_COUNT
                // Display the selected promotional message.
                Log.d(TAG, "onCreate: id: $id counter: $counter")
                showPromotionalMessage(id)
            }
            val versionCode = BuildConfig.VERSION_CODE
            if (savedVersionCode != versionCode) {
                preferences[KEY_APP_VERSION_CODE] = versionCode
                showToast(
                    R.string.what_s_new_latest,
                    duration = Toast.DURATION_INDEFINITE,
                    icon = Icons.Outlined.Whatshot
                )
            }
        }
        // Set up the window
        // Window settings are likely handled in AppTheme already, but we ensure it here.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        // Set the content view
        setContent {
            val navController = rememberNavController()
            // Set the content to Home screen with toastHostState and navController
            Home(toastHostState, navController)
            // Observe the destination change and initialize navController.
            DisposableEffect(Unit) {
                Log.d(TAG, "onCreate - DisposableEffect")
                // Add this activity as a listener to navController's destination changes
                navController.addOnDestinationChangedListener(this@MainActivity)
                this@MainActivity.navController = navController
                // Cleanup the listener when the DisposableEffect is disposed
                onDispose {
                    navController.removeOnDestinationChangedListener(this@MainActivity)
                    this@MainActivity.navController = null
                }
            }
        }
    }
}
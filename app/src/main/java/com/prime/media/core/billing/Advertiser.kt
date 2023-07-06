package com.prime.media.core.billing

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.prime.media.BuildConfig
import com.unity3d.ads.IUnityAdsInitializationListener
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize
import java.util.concurrent.TimeUnit

/**
 * A interface for implementing Advertisement in app.
 * This is useful for implementing Interstitial and Rewarded video ads.
 * since both are shown one at a time hence there will be no issue of sync.
 *
 * @author Zakir Ahmad Sheikh
 * @since 2021
 */
interface Advertiser {
    /**
     * Show interstitial ad.
     * @param activity Current Android activity of calling app.
     * @param force a user tag to signify the importance of showing of ad.
     * @param action -> the code to trigger after completing of action.
     */
    fun show(activity: Activity, force: Boolean = false, action: (() -> Unit)? = null)
}

private val DEFAULT_DELAY =
    TimeUnit.MINUTES.toMillis(20)

private val DEFAULT_INITIAL_DELAY =
    TimeUnit.MINUTES.toMillis(5)

private val DEFAULT_FORCE_DELAY =
    TimeUnit.SECONDS.toMillis(45)

/**
 * **An Utility [Advertiser] class required to show advertisements in app.**
 * @param iDelay : The initial delay before an ad is showed to user.
 * @param delay: The delay between two intestinal ads.
 * @param forceDelay: minimum delay between interstitial ads if forced.
 *
 * @author Zakir Ahmad Sheikh
 * @since 5-01-2021
 */
fun Advertiser(
    context: Context,
    iDelay: Long = DEFAULT_INITIAL_DELAY,
    delay: Long = DEFAULT_DELAY,
    forceDelay: Long = DEFAULT_FORCE_DELAY,
) = object : Advertiser {

    private val fAnalytics = Firebase.analytics

    init {
        // init ads sdk
        UnityAds.initialize(
            context,
            Private.UNITY_APP_ID,
            BuildConfig.DEBUG,
            object : IUnityAdsInitializationListener {
                override fun onInitializationComplete() {
                    fAnalytics.logEvent("advertiser_initialized_successfully", null)
                    // load placements.
                    UnityAds.load(placementID, loadListener)
                }

                override fun onInitializationFailed(
                    error: UnityAds.UnityAdsInitializationError,
                    message: String
                ) {
                    fAnalytics.logEvent("advertiser_initialization_error:${error.name}", null)
                }
            }
        )
    }


    /**
     * The time in *mills* when interstitial ad was showed.
     * *null* value represents, the ad was never showed.
     */
    private var lastWhenShowed: Long? = null

    /**
     * The time this object was created; since this a global variable and hence this represents the
     * time of app first loaded.
     */
    private val creationTimeMills =
        System.currentTimeMillis()

    /**
     * The placement id of the interstitial ad.
     */
    private val placementID =
        Placement.INTERSTITIAL

    /**
     * This represents the loading state of the interstitial
     */
    private var isReady = false

    private val loadListener =
        object : IUnityAdsLoadListener {
            override fun onUnityAdsAdLoaded(placementId: String) {
                fAnalytics.logEvent("IAd loaded for $placementId", null)
                isReady = true
            }

            override fun onUnityAdsFailedToLoad(
                placementId: String,
                error: UnityAds.UnityAdsLoadError,
                message: String
            ) {
                fAnalytics.logEvent("IAd error: $placementId ${error.name}", null)
            }
        }

    private val displayListener =
        object : IUnityAdsShowListener {
            override fun onUnityAdsShowFailure(
                placementId: String,
                error: UnityAds.UnityAdsShowError,
                message: String
            ) {
                fAnalytics.logEvent("IAd error: $placementId ${error.name}", null)
                isReady = false
            }

            override fun onUnityAdsShowStart(placementId: String?) {
                fAnalytics.logEvent("IAd started: $placementId", null)
                isReady = false
            }

            override fun onUnityAdsShowClick(placementId: String?) {
                fAnalytics.logEvent("IAd: $placementId clicked", null)
            }

            override fun onUnityAdsShowComplete(
                placementId: String?,
                state: UnityAds.UnityAdsShowCompletionState?
            ) {
                fAnalytics.logEvent("IAd show completed: $placementId", null)
                lastWhenShowed = System.currentTimeMillis()
                isReady = false
            }
        }

    override fun show(
        activity: Activity,
        force: Boolean,
        action: (() -> Unit)?
    ) {
        if (!UnityAds.isInitialized()) return
        if (!isReady) {
            UnityAds.load(placementID, loadListener); return
        }
        val elapsed = System.currentTimeMillis() - (lastWhenShowed ?: creationTimeMills)

        val show =
            when {
                force -> elapsed > forceDelay
                lastWhenShowed == null -> elapsed > iDelay
                else -> elapsed > delay
            }
        if (!show) return
        UnityAds.show(activity, placementID, displayListener)
    }
}


private val iBannerListener =
    object : BannerView.IListener {

        val fAnalytics = Firebase.analytics

        override fun onBannerShown(bannerAdView: BannerView?) {
            // Do Nothing!!
        }

        override fun onBannerLoaded(bannerView: BannerView) {
            fAnalytics.logEvent("Banner loaded: id = ${bannerView.placementId}", null)
            bannerView.layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT)
            bannerView.visibility = View.VISIBLE
        }

        override fun onBannerClick(bannerView: BannerView) {
            fAnalytics.logEvent("Banner clicked: id = ${bannerView.placementId}", null)
        }

        override fun onBannerFailedToLoad(bannerView: BannerView, errorInfo: BannerErrorInfo) {
            fAnalytics.logEvent(
                "Banner failed load: id = ${bannerView.placementId} error: ${errorInfo.errorCode}",
                null
            )
            //bannerView.load()
        }

        override fun onBannerLeftApplication(bannerView: BannerView) {
            fAnalytics.logEvent("Banner left: id = ${bannerView.placementId}", null)
        }
    }

/**
 * By default, banner ads display anchored on the bottom-center of the screen, supporting 320 x 50
 * or 728 x 90 pixel resolution. To specify the banner anchor, use the Banner.SetPosition API. For example:
 */
@Composable
fun Banner(
    modifier: Modifier = Modifier,
    size: UnityBannerSize = UnityBannerSize(320, 50),
    placementID: String,
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            BannerView(context as Activity, placementID, size).apply {
                // Set the listener for banner lifecycle events:
                listener = iBannerListener
                // Request a banner ad:
                load()
                layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, 0)
                visibility = View.GONE
            }
        }
    )
}


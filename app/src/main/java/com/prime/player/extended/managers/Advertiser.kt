package com.prime.player.extended.managers

import android.app.Activity
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.LinearLayout
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import com.prime.player.R
import com.unity3d.ads.IUnityAdsLoadListener
import com.unity3d.ads.IUnityAdsShowListener
import com.unity3d.ads.UnityAds
import com.unity3d.services.banners.BannerErrorInfo
import com.unity3d.services.banners.BannerView
import com.unity3d.services.banners.UnityBannerSize
import java.util.concurrent.TimeUnit


private const val TAG = "Advertiser"

/**
 * **An Utility [Advertiser] class required to show advertisements in app.**
 * @param iDelay : The initial delay before an ad is showed to user.
 * @param delay: The delay between two intestinal ads.
 * @param forceDelay: minimum delay between interstitial ads if forced.
 */
class Advertiser(
    val iDelay: Long = DEFAULT_INITIAL_DELAY,
    val delay: Long = DEFAULT_DELAY,
    val forceDelay: Long = DEFAULT_FORCE_DELAY,
    val context: Activity,
) {

    private val fAnalytics = Firebase.analytics

    private val placementID = context.getString(R.string.interstitial_ad_id)
    private var isReady = false

    private val loadListener = object : IUnityAdsLoadListener {
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

    private val displayListener = object : IUnityAdsShowListener {
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

    private var lastWhenShowed: Long? = null
    private val creationTimeMills = System.currentTimeMillis()

    init {
        // load ad on init
        UnityAds.load(
            placementID,
            loadListener
        )
    }

    fun show(force: Boolean = false) {
        if (!isReady) {
            UnityAds.load(placementID, loadListener)
            return
        }
        val elapsed = lastWhenShowed?.let { System.currentTimeMillis() - it }
            ?: System.currentTimeMillis() - creationTimeMills
        val show = when {
            force -> elapsed > forceDelay
            else -> {
                if (lastWhenShowed == null)
                    elapsed >= iDelay
                else
                    elapsed >= delay
            }
        }
        if (show)
            UnityAds.show(context, placementID, displayListener)
    }


    companion object {


        private val DEFAULT_DELAY = TimeUnit.MINUTES.toMillis(20)
        private val DEFAULT_INITIAL_DELAY = TimeUnit.MINUTES.toMillis(5)
        private val DEFAULT_FORCE_DELAY = TimeUnit.SECONDS.toMillis(45)
    }
}


private val iBannerListener = object : BannerView.IListener {

    val fAnalytics = Firebase.analytics

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
    AndroidView(factory = { context ->
        BannerView(context as Activity, placementID, size).apply {
            // Set the listener for banner lifcycle events:
            listener = iBannerListener
            // Request a banner ad:
            load()
            layoutParams = LinearLayout.LayoutParams(WRAP_CONTENT, 0)
            visibility = View.GONE
        }
    }, modifier = modifier)
}


val LocalAdvertiser = staticCompositionLocalOf<Advertiser> {
    error("No local advertiser defined!!")
}
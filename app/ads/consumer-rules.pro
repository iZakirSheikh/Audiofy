-keepclassmembers class * implements android.os.Parcelable {
    public static final android.os.Parcelable$Creator *;
}
#noinspection ShrinkerUnresolvedReference
#unity
-keep class com.google.android.gms.ads.** {public *;}
-keep class com.google.android.gms.appset.** { *; }
-keep class com.google.android.gms.tasks.** { *; }
#adapters
-keep class com.ironsource.adapters.** { *; }
#sdk
-dontwarn com.ironsource.**
-dontwarn com.ironsource.adapters.**
-keepclassmembers class com.ironsource.** { public *; }
-keep public class com.ironsource.**
-keep class com.ironsource.adapters.** { *;
}
#omid
-dontwarn com.iab.omid.**
-keep class com.iab.omid.** {*;}
#javascript
-keepattributes JavascriptInterface
-keepclassmembers class * { @android.webkit.JavascriptInterface <methods>; }
#For AmazonAps integration
-keep class com.amazon.device.ads.DtbThreadService {
    static *;
}
-keep public interface com.amazon.device.ads** {*; }
#For AppLovin integration
-keepclassmembers class com.applovin.sdk.AppLovinSdk {
    static *;
}
-keep public interface com.applovin.sdk** {*; }
-keep public interface com.applovin.adview** {*; }
-keep public interface com.applovin.mediation** {*; }
-keep public interface com.applovin.communicator** {*; }
#For Bytedance integration
-keep public interface com.bytedance.sdk.openadsdk** {*; }
#For Facebook integration
-keepclassmembers class com.facebook.ads.internal.AdSdkVersion {
    static *;
}
-keepclassmembers class com.facebook.ads.internal.settings.AdSdkVersion {
    static *;
 }
-keepclassmembers class com.facebook.ads.BuildConfig {
    static *;
 }
-keep public interface com.facebook.ads** {*; }
#For Fairbid
-keep public interface com.fyber.fairbid.ads.interstitial** {*; }
-keep public interface com.fyber.fairbid.ads.rewarded** {*; }
-keep class com.fyber.offerwall.*
#For Fivead
-keep public interface com.five_corp.ad** {*; }
#For Fyber(Inneractive) integration
-keep public interface com.fyber.inneractive.sdk.external** {*; }
-keep public interface com.fyber.inneractive.sdk.activities** {*; }
-keep public interface com.fyber.inneractive.sdk.ui** {*; }
#For HyprMX integration
-keepclassmembers class com.hyprmx.android.sdk.utility.HyprMXProperties {
    static *;
}
-keepclassmembers class com.hyprmx.android.BuildConfig {
    static *;
}
-keep public interface com.hyprmx.android.sdk.activity** {*; }
-keep public interface com.hyprmx.android.sdk.graphics** {*; }
# For Inmobi integration
-keep class com.inmobi.*
-keep public interface com.inmobi.ads.listeners** {*; }
-keep public interface com.inmobi.ads.InMobiInterstitial** {*; }
-keep public interface com.inmobi.ads.InMobiBanner** {*; }
# For ironSource integration
-keep public interface com.ironsource.mediationsdk.sdk** {*; }
-keep public interface com.ironsource.mediationsdk.impressionData.ImpressionDataListener {*; }
#For Maio integration
-keep public interface jp.maio.sdk.android.MaioAdsListenerInterface {*; }
# For Mintergral integration
-keep public interface com.mbridge.msdk.out** {*; }
-keep public interface com.mbridge.msdk.videocommon.listener** {*; }
-keep public interface com.mbridge.msdk.interstitialvideo.out** {*; }
-keep public interface com.mintegral.msdk.out** {*; }
-keep public interface com.mintegral.msdk.videocommon.listener** {*; }
-keep public interface com.mintegral.msdk.interstitialvideo.out** {*; }
#For MyTarget integration
-keep class com.my.target.** {*;}
#For Ogury integration
-keep public interface io.presage.interstitial** {*; }
-keep public interface io.presage.interstitial.PresageInterstitialCallback {*; }
#For Pubnative integration
-keep public interface net.pubnative.lite.sdk.interstitial.HyBidInterstitialAd** {*; }
-keep public interface net.pubnative.lite.sdk.rewarded.HyBidRewardedAd** {*; }
-keep public interface net.pubnative.lite.sdk.views.HyBidAdView** {*; }
#For Smaato integration
-keep public interface com.smaato.sdk.interstitial** {*; }
-keep public interface com.smaato.sdk.video.vast** {*; }
-keep public interface com.smaato.sdk.banner.widget** {*; }
-keep public interface com.smaato.sdk.core.util** {*; }
# For Tapjoy integration
-keep public interface com.tapjoy.** {*; }
# For Tencent integration
-keep public interface com.qq.e.ads.interstitial2** {*; }
-keep public interface com.qq.e.ads.interstitial3** {*; }
-keep public interface com.qq.e.ads.rewardvideo** {*; }
-keep public interface com.qq.e.ads.rewardvideo2** {*; }
-keep public interface com.qq.e.ads.banner2** {*; }
-keep public interface com.qq.e.comm.adevent** {*; }
#For Verizon integration
-keepclassmembers class com.verizon.ads.edition.BuildConfig {
    static *;
}
-keep public interface com.verizon.ads.interstitialplacement** {*; }
-keep public interface com.verizon.ads.inlineplacement** {*; }
-keep public interface com.verizon.ads.vastcontroller** {*; }
-keep public interface com.verizon.ads.webcontroller** {*; }
#For Vungle integration
-keep public interface com.vungle.warren.PlayAdCallback {*; }
-keep public interface com.vungle.warren.ui.contract** {*; }
-keep public interface com.vungle.warren.ui.view** {*; }
#For AndroidX
-keep class androidx.localbroadcastmanager.content.LocalBroadcastManager { *;}
-keep class androidx.recyclerview.widget.RecyclerView { *;}
-keep class androidx.recyclerview.widget.RecyclerView$OnScrollListener { *;}
#For Android
-keep class * extends android.app.Activity
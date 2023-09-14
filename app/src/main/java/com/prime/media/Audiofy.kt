package com.prime.media

import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.primex.preferences.intPreferenceKey
import dagger.hilt.android.HiltAndroidApp

private const val TAG = "Audiofy"

@HiltAndroidApp
class Audiofy : Application(), ImageLoaderFactory {

    companion object {
        /**
         * The counter counts the number of times this app was launched.
         */
        val KEY_LAUNCH_COUNTER =
            intPreferenceKey(TAG + "_launch_counter")

        /**
         * The link to PlayStore Market.
         */
        const val GOOGLE_STORE = "market://details?id=" + BuildConfig.APPLICATION_ID

        /**
         * If PlayStore is not available in Users Phone. This will be used to redirect to the
         * WebPage of the app.
         */
        const val FALLBACK_GOOGLE_STORE =
            "http://play.google.com/store/apps/details?id=" + BuildConfig.APPLICATION_ID

        /**
         * Package name for the Google Play Store. Value can be verified here:
         * https://developers.google.com/android/reference/com/google/android/gms/common/GooglePlayServicesUtil.html#GOOGLE_PLAY_STORE_PACKAGE
         */
        const val PKG_GOOGLE_PLAY_STORE = "com.android.vending"

        val STORAGE_PERMISSION =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                android.Manifest.permission.READ_MEDIA_AUDIO
            else
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
    }

    /**
     * Gets the package info of this app using the package manager.
     * @return a PackageInfo object containing information about the app, or null if an exception occurs.
     * @see android.content.pm.PackageManager.getPackageInfo
     */
    val packageInfo
        get() = com.primex.core.runCatching(TAG + "_review") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0)
                )
            else
                packageManager.getPackageInfo(packageName, 0)
        }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .components {
//                add(VideoFrameDecoder.Factory())
//                add(SvgDecoder.Factory())
//                add(GifDecoder.Factory())
            }
            .memoryCache {
                MemoryCache.Builder(this)
                    .strongReferencesEnabled(true)
                    .build()
            }
            .memoryCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache/coil"))
                    //.maxSizePercent(0.02)
                    .maxSizeBytes(20_000)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }
}


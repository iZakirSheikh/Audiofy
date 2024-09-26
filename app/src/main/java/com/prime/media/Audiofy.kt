package com.prime.media

import android.app.Application
import android.content.ContentResolver
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.fetch.Fetcher
import coil.fetch.Fetcher.Factory
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.request.Options
import coil.util.DebugLogger
import com.prime.media.common.coil.MediaMetaDataArtFetcher
import com.prime.media.settings.Settings
import com.primex.preferences.Preferences
import com.primex.preferences.intPreferenceKey
import com.primex.preferences.value
import com.zs.ads.AdManager
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

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

    @Inject
    lateinit var preferences: Preferences
    private fun MediaMetaDataArtFactory() = object : Factory<Uri> {
        override fun create(data: Uri, options: Options, imageLoader: ImageLoader): Fetcher? {
            // Check if the provided Uri corresponds to an album art Uri within the MediaStore.
            val isAlbumUri = let {
                if(data.scheme != ContentResolver.SCHEME_CONTENT) return null
                if (data.authority != MediaStore.AUTHORITY) return@let false
                val segments = data.pathSegments
                val size = segments.size
                return@let size >= 3 && segments[size - 3] == "audio" && segments[size - 2] == "albums"
            }

            // If the preferences indicate the use of the legacy artwork method and it's not an album Uri,
            // return null to indicate that this factory should not be used.
            if (preferences.value(Settings.USE_LEGACY_ARTWORK_METHOD) && !isAlbumUri) return null

            // Otherwise, create and return a MediaMetaDataArtFetcher for handling the artwork retrieval.
            return MediaMetaDataArtFetcher(data, options)
        }
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            // Add the MediaMetaDataArtFactory
            .components { add(MediaMetaDataArtFactory()) }
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

    override fun onCreate() {
        super.onCreate()
        // Init the AdManager;
        // TODO - Pass through BuildConfig
        AdManager.initialize(this, BuildConfig.ADS_APP_ID)
    }
}


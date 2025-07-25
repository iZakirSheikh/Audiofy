/*
 * Copyright 2025 sheik
 *
 * Created by sheik on 09-05-2025.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.zs.audiofy.common.impl

import android.content.Context
import android.util.Log
import androidx.compose.animation.core.AnimationConstants
import androidx.startup.Initializer
import coil3.annotation.DelicateCoilApi
import coil3.asImage
import coil3.request.crossfade
import com.zs.audiofy.R
import com.zs.audiofy.settings.Settings
import com.zs.compose.theme.snackbar.SnackbarHostState
import com.zs.core.coil.VideoThumbnailFetcher
import com.zs.core.db.playlists.Playlists
import com.zs.core.playback.Remote
import com.zs.core.store.MediaProvider
import com.zs.core.telemetry.Analytics
import com.zs.preferences.Preferences
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import androidx.appcompat.content.res.AppCompatResources.getDrawable as Drawable
import coil3.ImageLoader.Builder as ImageLoader
import coil3.SingletonImageLoader.setUnsafe as Coil

private const val TAG = "Initializers"

/**
 * Initializes Koin for dependency injection.
 */
class KoinInitializer : Initializer<KoinApplication> {
    override fun create(context: Context): KoinApplication {
        Log.d(TAG, "Initializer: starting koin")
        return startKoin {
            androidContext(context)
            modules(KoinAppModules)
        }
    }

    // No dependencies on other libraries.
    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}

/**
 * Initializes Analytics for logging and crash reporting.
 */
class AnalyticsInitializer : Initializer<Unit> {
    override fun create(context: Context): Unit {
        Log.d(TAG, "Initializer: starting firebase")
        Analytics(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}

/**Initialize Coil and its components. */
class CoilInitializer : Initializer<Unit> {
    @OptIn(DelicateCoilApi::class)
    override fun create(context: Context) {
        val error = Drawable(context, R.drawable.ic_error_image_placeholder)!!.asImage()
        // Construct imageLoader
        val loader = ImageLoader(context)
            .error(error)
            .crossfade(AnimationConstants.DefaultDurationMillis)
            .components { add(VideoThumbnailFetcher.Factory()) }
            .build()
        // set global image loader
        Coil(loader)
    }

    override fun dependencies(): MutableList<Class<out Initializer<*>>> =
        mutableListOf()
}

/**
 * Initializes AdNetwork.
 */
class AdNetworkInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        // AdManager(context, BuildConfig.ADS_APP_ID).then {
        Log.d(TAG, "Initializer: starting AdNetworkInitializer")
    }

    override fun dependencies(): List<Class<out Initializer<*>?>?> = emptyList()
}

// Define dependencies here.
private val KoinAppModules = module {
    // Define Koin modules for dependency injection.
    // Declare a singleton instance of Preferences.
    single {
        // Initialize Preferences
        val preferences = Preferences(get(), "Shared_Preferences")
        // Retrieve the current launch counter value, defaulting to 0 if not set
        val counter = preferences[Settings.KEY_LAUNCH_COUNTER]
        // Increment the launch counter for cold starts
        preferences[Settings.KEY_LAUNCH_COUNTER] = counter + 1
        Log.d(TAG, "Cold start counter: ${preferences[Settings.KEY_LAUNCH_COUNTER]}")
        // Return the preferences instance
        preferences
    }
    // Declare a ViewModel dependency (lifecycle managed by Koin).
    // viewModel { BatteryViewModel(get()) }
    singleOf(::SnackbarHostState)
    single { Analytics(get()) }
    single { Playlists(get()) }
    single { Remote(get()) }

    //
    factory { MediaProvider(get()) }
    factory { androidContext().resources }

    viewModelOf(::LibraryViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::AlbumsViewModel)
    viewModelOf(::ArtistsViewModel)
    viewModelOf(::GenresViewModel)
    viewModelOf(::FoldersViewModel)
    viewModelOf(::PlaylistsViewModel)
    viewModelOf(::AudiosViewModel)
    viewModelOf(::VideosViewModel)
    viewModelOf(::MembersViewModel)
    viewModelOf(::PropertiesViewModel)
}
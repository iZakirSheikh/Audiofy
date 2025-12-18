/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 02-10-2024.
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

package com.prime.media.impl

import android.content.Context
import android.util.Log
import androidx.startup.Initializer
import coil.Coil
import com.prime.media.BuildConfig
import com.prime.media.R
import com.prime.media.old.core.playback.Remote
import com.prime.media.old.directory.playlists.MembersViewModel
import com.prime.media.old.directory.store.AudiosViewModel
import com.prime.media.old.impl.AudioFxViewModel
import com.prime.media.old.impl.ConsoleViewModel
import com.prime.media.old.impl.Remote
import com.prime.media.old.impl.Repository
import com.prime.media.old.impl.SystemDelegate
import com.prime.media.old.impl.TagEditorViewModel
import com.prime.media.common.AppConfig
import com.prime.media.common.Registry
import com.primex.preferences.Preferences
import com.primex.preferences.invoke
import com.zs.core.db.Playlists
import com.zs.core.db.Playlists2
import com.zs.core.store.MediaProvider
import com.zs.core_ui.Anim
import com.zs.core_ui.coil.MediaMetaDataArtFetcher
import com.zs.core_ui.toast.ToastHostState
import org.koin.android.ext.koin.androidContext
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module
import androidx.lifecycle.SavedStateHandle as Handle
import coil.ImageLoader.Builder as ImageLoader
import com.google.firebase.FirebaseApp.initializeApp as FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance as Crashlytics
import com.zs.ads.AdManager.Companion.initialize as AdManager
import com.zs.core.playback.PlaybackController.Companion.invoke as PlaybackController
import com.zs.core_ui.coil.VideoThumbnailFetcher as ThumbnailFetcher
import kotlin.also as then

private const val TAG = "Initializers"

private val KoinAppModules = module {
    // Define Koin modules for dependency injection.
    // Declare a singleton instance of Preferences.
    single {
        // Initialize Preferences
        val preferences = Preferences(get(), "Shared_Preferences")
        // Retrieve the app configuration from preferences and update config only if not null
        val config = preferences(Registry.KEY_APP_CONFIG)
        if (config != null) {
            val result = runCatching { AppConfig.update(config) }
            if (result.isFailure) {
                Log.e(TAG, "Error updating app config", result.exceptionOrNull())
            }
        }
        // Retrieve the current launch counter value, defaulting to 0 if not set
        val counter = preferences(Registry.KEY_LAUNCH_COUNTER) ?: 0
        // Increment the launch counter for cold starts
        preferences[Registry.KEY_LAUNCH_COUNTER] = counter + 1
        Log.d(TAG, "Cold start counter: ${preferences(Registry.KEY_LAUNCH_COUNTER)}")
        // Return the preferences instance
        preferences
    }
    single { Playlists2(get()) }
    single { Repository(get(), get(), get()) }
    single<Remote> { Remote(get()) }
    singleOf(::ToastHostState)
    single { Playlists(get()) }
    singleOf(::MediaProvider)
    singleOf(::PlaybackController)

    factory { androidContext().resources }
    factory() { SystemDelegate(get(), get()) }
    factory { androidContext().contentResolver }
    // ViewModels
    viewModelOf(::SettingsViewModel)
    viewModelOf(::VideosViewModel)
    viewModelOf(::MembersViewModel) // remove this later
    viewModelOf(::GenresViewModel)
    viewModelOf(::ArtistsViewModel)
    viewModelOf( ::FoldersViewModel )
    viewModel { AudioFxViewModel(get()) }
    viewModelOf(::LibraryViewModel)
    viewModel() { PersonalizeViewModel() }
    viewModel() { ConsoleViewModel(get(), get(), get()) }
    viewModel { (h: Handle) -> TagEditorViewModel(h, get(), get(), get()) }
    viewModelOf(::PlaylistViewModel)
    viewModel { (h: Handle) -> PlaylistsViewModel(get()) }
    viewModelOf(::AlbumsViewModel)
    viewModel { (h: Handle) -> AudiosViewModel(h, get(), get(), get(), get()) }
}

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
 * Initializes Firebase for crash reporting.
 */
class FirebaseInitializer : Initializer<Unit> {
    override fun create(context: Context): Unit {
        Log.d(TAG, "Initializer: starting firebase")
        FirebaseApp(context)
        Crashlytics().then { it.isCrashlyticsCollectionEnabled = true }
    }

    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}

/**
 * Initializes Coil for loading images.
 */
class CoilInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        Log.d(TAG, "Initializer: starting Coil")
        // Get the shared preferences.
        Log.d(TAG, "Initializer: use MediaMetaDataArtFetcher; ${AppConfig.isLoadThumbnailFromCache}")
        // Build the ImageLoader with the MediaMetaDataArtFetcher.Factory if the user hasn't opted for the legacy method.
        val imageLoader =
            ImageLoader(context)
                .error(R.drawable.default_art)
                .crossfade(Anim.DefaultDurationMillis)
                .components {
                    if (!AppConfig.isLoadThumbnailFromCache)
                        add(MediaMetaDataArtFetcher.Factory())
                    add(ThumbnailFetcher.Factory())
                }
        // Set the created ImageLoader as the default for Coil.
        Coil.setImageLoader(imageLoader.build())
    }

    override fun dependencies(): List<Class<out Initializer<*>?>?> =
        listOf(KoinInitializer::class.java)
}

/**
 * Initializes AdNetwork.
 */
class AdNetworkInitializer : Initializer<Unit> {
    override fun create(context: Context) = AdManager(context, BuildConfig.ADS_APP_ID).then {
        Log.d(TAG, "Initializer: starting AdNetworkInitializer")
    }
    override fun dependencies(): List<Class<out Initializer<*>?>?> = emptyList()
}
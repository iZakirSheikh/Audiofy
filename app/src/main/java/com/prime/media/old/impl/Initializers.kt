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

package com.prime.media.old.impl

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.lifecycle.SavedStateHandle
import androidx.startup.Initializer
import com.google.firebase.FirebaseApp
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.prime.media.old.directory.playlists.MembersViewModel
import com.prime.media.old.directory.playlists.PlaylistsViewModel
import com.prime.media.old.directory.store.AlbumsViewModel
import com.prime.media.old.directory.store.ArtistsViewModel
import com.prime.media.old.directory.store.AudiosViewModel
import com.prime.media.old.directory.store.FoldersViewModel
import com.prime.media.old.directory.store.GenresViewModel
import com.zs.core.db.Playlists2
import com.zs.core.db.Playlists2.Companion.invoke
import com.zs.core_ui.toast.ToastHostState
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

private const val TAG = "Initializers"

private val appModules = module {
    // Define Koin modules for dependency injection.
    // Declare a singleton instance of Preferences.
    single {
        // Initialize Preferences
        val preferences = com.primex.preferences.Preferences(get(), "Shared_Preferences")
        preferences
    }
    single(){ Playlists2(get()) }
    single(){ Repository(get(),get(),get()) }
    single{ Remote (get()) }

    /*scope<ComponentActivity> {
       scoped {
           ToastHostState()
       }
    }*/

    singleOf(::ToastHostState)

    factory { androidContext().resources }
    factory(){SystemDelegate(get(),get())}
    factory { androidContext().contentResolver }
    // ViewModels
    viewModel { SettingsViewModel(get()) }
    viewModel { AudioFxViewModel(get()) }
    viewModel { FeedbackViewModel(get()) }
    viewModel(){ LibraryViewModel(get(), get(), get()) }
    viewModel(){ PersonalizeViewModel(get()) }
    viewModel(){ ConsoleViewModel(get(), get(), get()) }
    viewModel { (handle: SavedStateHandle) -> TagEditorViewModel(handle, get(), get(), get()) }
    viewModel { (handle: SavedStateHandle) -> MembersViewModel(handle, get(), get(), get()) }
    viewModel { (handle: SavedStateHandle) -> PlaylistsViewModel(handle, get(), get(), get()) }
    viewModel { (handle: SavedStateHandle) -> AlbumsViewModel(handle, get(), get(), get()) }
    viewModel { (handle: SavedStateHandle) -> ArtistsViewModel(handle, get(), get(), get()) }
    viewModel { (handle: SavedStateHandle) -> AudiosViewModel(handle, get(), get(), get(), get()) }
    viewModel { (handle: SavedStateHandle) -> GenresViewModel(handle, get(), get(), get()) }
    viewModel {  (handle: SavedStateHandle) -> FoldersViewModel(handle, get(), get(), get()) }
}

class KoinInitializer : Initializer<Unit> {
    override fun create(context: Context) {
        startKoin {
            androidContext(context)
            modules(appModules)
        }
    }

    // No dependencies on other libraries.
    override fun dependencies(): List<Class<out Initializer<*>>> = emptyList()
}

class FirebaseInitializer : Initializer<Unit> {
    override fun create(context: Context): Unit {
        // Initialize Firebase
        FirebaseApp.initializeApp(context)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return emptyList()
    }
}

class CrashlyticsInitializer : Initializer<Unit> {
    override fun create(context: Context): Unit {
        // Initialize Crashlytics
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(true)
    }

    override fun dependencies(): List<Class<out Initializer<*>>> {
        return listOf(FirebaseInitializer::class.java)
    }
}

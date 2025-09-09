/*
 *  Copyright (c) 2025 Zakir Sheikh
 *
 *  Created by Zakir Sheikh on $today.date.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */

package com.zs.audiofy.settings

import android.os.Build

/**
 * `AppConfig` is a singleton object that holds various configuration values for the application.
 *
 * This object serves as a centralized place to manage settings that can be modified
 * during runtime, some of which may require an application restart to take effect.
 *
 * It is designed to be lightweight and is initialized at application startup via the
 * [update] method. Values within `AppConfig` can be altered through the application's
 * [Settings] interface.
 *
 * The primary purpose of `AppConfig` is to provide default values for certain settings
 * and to manage configurations that are not directly handled by Jetpack Compose,
 * offering a clear separation of concerns for application-wide settings.
 *
 * @author Zakir Sheikh
 * @since 1.0.0
 *
 */
object AppConfig {

    /**
     * Get/Set strategy to load media thumbnails from cache.
     */
    var isLoadThumbnailFromCache: Boolean = false

    /**
     * Get/Set strategy enable background blur.
     */
    var isBackgroundBlurEnabled: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

    fun update(value: String) {

    }

    fun stringfy(): String {
        TODO()
    }
}
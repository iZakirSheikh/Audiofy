/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 10-05-2025.
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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.zs.audiofy.R
import com.zs.audiofy.common.SystemFacade
import com.zs.audiofy.settings.AppConfig
import com.zs.audiofy.settings.Settings
import com.zs.audiofy.settings.SettingsViewState
import com.zs.compose.theme.snackbar.SnackbarResult
import com.zs.preferences.Key
import kotlinx.coroutines.launch

class SettingsViewModel : KoinViewModel(), SettingsViewState {

    // By default save is false
    override var save: Boolean by mutableStateOf(false)

    var _trashCanEnabled: Boolean by mutableStateOf(AppConfig.isTrashCanEnabled)
    var _preferCachedThumbnails: Boolean by mutableStateOf(AppConfig.isLoadThumbnailFromCache)
    var _enabledBackgroundBlur: Boolean by mutableStateOf(AppConfig.isBackgroundBlurEnabled)
    var _fontScale: Float by mutableFloatStateOf(AppConfig.fontScale)
    var _minTrackLengthSecs: Int by mutableIntStateOf(AppConfig.minTrackLengthSecs)
    var _shouldUseSystemAudioEffects: Boolean by mutableStateOf(AppConfig.shouldUseSystemAudioEffects)
    var _gridItemSizeMultiplier: Float by mutableFloatStateOf(AppConfig.gridItemSizeMultiplier)

    override var trashCanEnabled: Boolean
        get() = _trashCanEnabled
        set(value) {
            _trashCanEnabled = value
            save = true
        }

    override var preferCachedThumbnails: Boolean
        get() = _preferCachedThumbnails
        set(value) {
            _preferCachedThumbnails = value
            save = true
        }
    override var enabledBackgroundBlur: Boolean
        get() = _enabledBackgroundBlur
        set(value) {
            _enabledBackgroundBlur = value
            save = true
        }
    override var fontScale: Float
        get() = _fontScale
        set(value) {
            _fontScale = value
            save = true
        }

    override var minTrackLengthSecs: Int
        get() = _minTrackLengthSecs
        set(value) {
            _minTrackLengthSecs = value
            save = true
        }

    override var shouldUseSystemAudioEffects: Boolean
        get() = _shouldUseSystemAudioEffects
        set(value) {
            _shouldUseSystemAudioEffects = value
            save = true
        }

    override var gridItemSizeMultiplier: Float
        get() = _gridItemSizeMultiplier
        set(value) {
            _gridItemSizeMultiplier = value
            save = true
        }

    override fun commit(facade: SystemFacade) {
        viewModelScope.launch {
            // [CORE_SETTING_CHANGE] Update AppConfig with new settings values
            // This directly modifies the global AppConfig object, which is used throughout the application
            // to determine runtime behavior based on user preferences.
            AppConfig.isLoadThumbnailFromCache = preferCachedThumbnails
            AppConfig.isBackgroundBlurEnabled = enabledBackgroundBlur

            // [PERSISTENCE] Serialize and save the updated AppConfig to preferences
            // The `stringify()` method likely converts the AppConfig object into a JSON or similar string format
            // for storage. `preferences` is an abstraction over SharedPreferences or DataStore.
            preferences[Settings.KEY_APP_CONFIG] = AppConfig.stringify()

            // [USER_FEEDBACK] Inform the user that a restart is required for some changes to take effect
            // Display a snackbar with a "Restart" action.
            val result = showSnackbar(
                R.string.msg_apply_changes_restart,
                R.string.restart,
                icon = Icons.Outlined.RestartAlt
            )
            save = false
            // [APP_LIFECYCLE] If the user confirms, trigger an application restart via the SystemFacade
            if (result == SnackbarResult.ActionPerformed)
                facade.restart(true)
        }
    }

    override fun <S, O> set(key: Key<S, O>, value: O) {
        preferences[key] = value
    }
}
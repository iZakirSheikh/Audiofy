/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by 2024 on 14-10-2024.
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

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PowerSettingsNew
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import com.prime.media.R
import com.prime.media.common.SystemFacade
import com.prime.media.settings.AppConfig
import com.prime.media.settings.Settings
import com.prime.media.settings.SettingsViewState
import com.primex.preferences.Key
import com.zs.core_ui.toast.Toast
import kotlinx.coroutines.launch

class SettingsViewModel() : KoinViewModel(), SettingsViewState {
    override var trashCanEnabled: Boolean by mutableStateOf(AppConfig.isTrashCanEnabled)
    override var preferCachedThumbnails: Boolean by mutableStateOf(AppConfig.isLoadThumbnailFromCache)
    override var enabledBackgroundBlur: Boolean by mutableStateOf(AppConfig.isBackgroundBlurEnabled)
    override var fontScale: Float by mutableFloatStateOf(AppConfig.fontScale)
    override var minTrackLengthSecs: Int by mutableIntStateOf(AppConfig.minTrackLengthSecs)
    override var inAppAudioEffectsEnabled: Boolean by mutableStateOf(AppConfig.inAppAudioEffectsEnabled)
    override var gridItemSizeMultiplier: Float by mutableFloatStateOf(AppConfig.gridItemSizeMultiplier)
    override var fabLongPressLaunchConsole: Boolean by mutableStateOf(AppConfig.fabLongPressLaunchConsole)
    override var isFileGroupingEnabled: Boolean by mutableStateOf(AppConfig.isFileGroupingEnabled)
    override var isSplashAnimWaitEnabled: Boolean by mutableStateOf(AppConfig.isSplashAnimWaitEnabled)


    override val save: Boolean by derivedStateOf {
        trashCanEnabled != AppConfig.isTrashCanEnabled ||
                preferCachedThumbnails != AppConfig.isLoadThumbnailFromCache ||
                enabledBackgroundBlur != AppConfig.isBackgroundBlurEnabled ||
                fontScale != AppConfig.fontScale ||
                minTrackLengthSecs != AppConfig.minTrackLengthSecs ||
                inAppAudioEffectsEnabled != AppConfig.inAppAudioEffectsEnabled ||
                gridItemSizeMultiplier != AppConfig.gridItemSizeMultiplier ||
                fabLongPressLaunchConsole != AppConfig.fabLongPressLaunchConsole ||
                isFileGroupingEnabled != AppConfig.isFileGroupingEnabled ||
                isSplashAnimWaitEnabled != AppConfig.isSplashAnimWaitEnabled
    }

    override fun commit(facade: SystemFacade) {
        viewModelScope.launch {
            // [CORE_SETTING_CHANGE] Update AppConfig with new settings values
            // This directly modifies the global AppConfig object, which is used throughout the application
            // to determine runtime behavior based on user preferences.
            val global = AppConfig.isLoadThumbnailFromCache != preferCachedThumbnails
            AppConfig.isLoadThumbnailFromCache = preferCachedThumbnails
            AppConfig.isBackgroundBlurEnabled = enabledBackgroundBlur
            AppConfig.isTrashCanEnabled = trashCanEnabled
            AppConfig.fontScale = fontScale
            AppConfig.minTrackLengthSecs = minTrackLengthSecs
            AppConfig.inAppAudioEffectsEnabled = inAppAudioEffectsEnabled
            AppConfig.gridItemSizeMultiplier = gridItemSizeMultiplier
            AppConfig.fabLongPressLaunchConsole = fabLongPressLaunchConsole
            AppConfig.isFileGroupingEnabled = isFileGroupingEnabled
            AppConfig.isSplashAnimWaitEnabled = isSplashAnimWaitEnabled

            // [PERSISTENCE] Serialize and save the updated AppConfig to preferences
            // The `stringify()` method likely converts the AppConfig object into a JSON or similar string format
            // for storage. `preferences` is an abstraction over SharedPreferences or DataStore.
            preferences[Settings.KEY_APP_CONFIG] = AppConfig.stringify()
            // trigger save
            val enabled = trashCanEnabled
            trashCanEnabled = !enabled
            trashCanEnabled = enabled
            // [USER_FEEDBACK] Inform the user that a restart is required for some changes to take effect
            // Display a snackbar with a "Restart" action.
            val result = showToast(
                R.string.msg_apply_changes_restart,
                R.string.restart,
                icon = Icons.Outlined.PowerSettingsNew
            )
            // [APP_LIFECYCLE] If the user confirms, trigger an application restart via the SystemFacade
            if (result == Toast.ACTION_PERFORMED)
                facade.restart(global)
        }
    }

    override fun discard() {
        viewModelScope.launch {
            val res = showToast("Discard unsaved changes?", "Discard")
            if (res == Toast.ACTION_PERFORMED) {
                fontScale = AppConfig.fontScale
                minTrackLengthSecs = AppConfig.minTrackLengthSecs
                inAppAudioEffectsEnabled = AppConfig.inAppAudioEffectsEnabled
                gridItemSizeMultiplier = AppConfig.gridItemSizeMultiplier
                preferCachedThumbnails = AppConfig.isLoadThumbnailFromCache
                enabledBackgroundBlur = AppConfig.isBackgroundBlurEnabled
                trashCanEnabled = AppConfig.isTrashCanEnabled
                fabLongPressLaunchConsole = AppConfig.fabLongPressLaunchConsole
                isFileGroupingEnabled = AppConfig.isFileGroupingEnabled
                isSplashAnimWaitEnabled = AppConfig.isSplashAnimWaitEnabled
            }
        }
    }

    override fun <S, O> set(key: Key<S, O>, value: O) {
        preferences[key] = value
    }
}
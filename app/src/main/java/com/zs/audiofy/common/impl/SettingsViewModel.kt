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

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.zs.audiofy.settings.SettingsViewState
import com.zs.preferences.Key

class SettingsViewModel : KoinViewModel(), SettingsViewState{


    override var trashCanEnabled: Boolean by mutableStateOf(false)
    override var preferCachedThumbnails: Boolean by mutableStateOf(false)
    override var enabledBackgroundBlur: Boolean by mutableStateOf(false)
    override var fontScale: Float by mutableFloatStateOf(-1f)
    override var minTrackLengthSecs: Int by mutableIntStateOf(30)
    override var shouldUseSystemAudioEffects: Boolean by mutableStateOf(true)
    override var gridItemSizeMultiplier: Float by mutableFloatStateOf(1.0f)

    // if there is change in above; this must be enabled;
    override val save: Boolean by derivedStateOf {
        trashCanEnabled != false ||
                preferCachedThumbnails != false ||
                enabledBackgroundBlur != false ||
                fontScale != -1f ||
                minTrackLengthSecs != 30 ||
                shouldUseSystemAudioEffects != true ||
                gridItemSizeMultiplier != 1.0f
    }


    override fun apply() {
        // causes the app to resatrt.
        TODO("Not yet implemented")
    }

    override fun <S, O> set(key: Key<S, O>, value: O) {
        preferences[key] = value
    }
}
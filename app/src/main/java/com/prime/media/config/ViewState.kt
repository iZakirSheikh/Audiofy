/*
 * Copyright 2024 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 26-08-2024.
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

package com.prime.media.config

import androidx.compose.ui.graphics.Color
import com.prime.media.core.Route
import com.primex.preferences.Key

object RoutePersonalize: Route

interface PersonalizeViewState {

    /**
     * Sets the current widget to [id]
     */
    fun setInAppWidget(id: String)

}
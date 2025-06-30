/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 30-06-2025.
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

package com.prime.media.widget

import android.util.Log
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.prime.media.MainActivity
import com.prime.media.common.compose.LocalSystemFacade

private const val TAG = "MiniPlayer"

@Composable
fun MiniPlayer(
    modifier: Modifier = Modifier
) {
    val remote = (LocalSystemFacade.current as? MainActivity)?.relay
    if (remote == null)
        return Spacer(modifier)
    val current by remote.state.collectAsState(null)
    Log.d(TAG, "MiniPlayer: $current")
}
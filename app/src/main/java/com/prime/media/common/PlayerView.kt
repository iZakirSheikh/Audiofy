/*
 * Copyright (c)  2026 Zakir Sheikh
 *
 * Created by sheik on 2 of Feb 2026
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at:
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Last Modified by sheik on 2 of Feb 2026
 *
 */

package com.prime.media.common

import android.annotation.SuppressLint
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView

/**
 * A wrapper around Media3 [PlayerView]
 */
@SuppressLint("UnsafeOptInUsageError")
@Composable
fun PlayerView(
    player: Player?,
    modifier: Modifier = Modifier,
    resizeMode: Int = AspectRatioFrameLayout.RESIZE_MODE_FIT,
    keepScreenOn: Boolean = true,
) {
    AndroidView(
        modifier = modifier,
        factory = {
            PlayerView(it).apply {
                hideController()
                useController = false
                this.player = player
                this.resizeMode = resizeMode
                layoutParams = FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                )
                clipToOutline = true
                // Set the Background Color of the player as Solid Black Color.
                setBackgroundColor(Color.Black.toArgb())
                this.keepScreenOn = keepScreenOn
            }
        },
        update = { it.resizeMode = resizeMode; it.player = player;   it.keepScreenOn = keepScreenOn },
        onRelease = {it.player = null; it.keepScreenOn = false}
    )
}
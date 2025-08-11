/*
 * Copyright 2025 Zakir Sheikh
 *
 * Created by Zakir Sheikh on 07-08-2025.
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

@file:Suppress("FunctionName")

package com.zs.audiofy.console

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstrainedLayoutReference
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import com.zs.audiofy.common.compose.horizontal
import com.zs.audiofy.common.compose.linkTo
import com.zs.compose.theme.WindowSize
import com.zs.audiofy.common.compose.ContentPadding as CP

private val TITLE_TEXT_SIZE_LARGE = 44.sp
private val TITLE_TEXT_SIZE_NORMAL = 16.sp

/** Represents the constraints of the PLayerView */
@Stable
interface Constraints {
    @Stable
    val value: ConstraintSet
    @Stable
    val titleTextSize: TextUnit
}

///
private val REF_CLOSE_BTN = ConstrainedLayoutReference(Console.ID_BTN_COLLAPSE)
private val REF_ARTWORK = ConstrainedLayoutReference(Console.ID_ARTWORK)
private val REF_TITLE = ConstrainedLayoutReference(Console.ID_TITLE)
private val REF_SUBTITLE = ConstrainedLayoutReference(Console.ID_SUBTITLE)
private val REF_POSITION = ConstrainedLayoutReference(Console.ID_POSITION)
private val REF_SHUFFLE = ConstrainedLayoutReference(Console.ID_SHUFFLE)
private val REF_REPEAT_MODE = ConstrainedLayoutReference(Console.ID_BTN_REPEAT_MODE)
private val REF_SKIP_PREVIOUS = ConstrainedLayoutReference(Console.ID_BTN_SKIP_PREVIOUS)
private val REF_PLAY_PAUSE = ConstrainedLayoutReference(Console.ID_BTN_PLAY_PAUSE)
private val REF_SKIP_TO_NEXT = ConstrainedLayoutReference(Console.ID_BTN_SKIP_TO_NEXT)
private val REF_SEEK_BAR = ConstrainedLayoutReference(Console.ID_SEEK_BAR)
private val REF_VIDEO_SURFACE = ConstrainedLayoutReference(Console.ID_VIDEO_SURFACE)
private val REF_TOAST = ConstrainedLayoutReference(Console.ID_TOAST)
private val REF_BACKGROUND = ConstrainedLayoutReference(Console.ID_BACKGROUND)
private val REF_SCRIM = ConstrainedLayoutReference(Console.ID_SCRIM)
private val REF_RESIZE_MODE = ConstrainedLayoutReference(Console.ID_BTN_RESIZE_MODE)
private val REF_ROTATION_LOCK = ConstrainedLayoutReference(Console.ID_BTN_ROTATION_LOCK)
private val REF_QUEUE = ConstrainedLayoutReference(Console.ID_BTN_QUEUE)
private val REF_SLEEP_TIMER = ConstrainedLayoutReference(Console.ID_BTN_SLEEP_TIMER)
private val REF_SPEED = ConstrainedLayoutReference(Console.ID_BTN_PLAYBACK_SPEED)
private val REF_LIKED = ConstrainedLayoutReference(Console.ID_BTN_LIKED)
private val REF_MORE = ConstrainedLayoutReference(Console.ID_BTN_MORE)


/**
 * Computes the appropriate [Constraints] configuration for the media console layout
 * based on the current window size, window insets, and content type.
 *
 * This function is used to determine how the player console should be arranged —
 * whether to show the full video with controls, only the controller UI, or adapt
 * based on screen size and available space.
 *
 * @param forceOnlyVideoSurface If true, forces the layout to display only the Video Surface UI, ignoring typical window size or content type decisions.
 *
 * @return A [Constraints] object defining how the console should be laid out based on the provided parameters.
 */
fun calculateConstraintSet(
    windowSize: WindowSize,
    insets: DpRect,
    isVideo: Boolean,
    forceOnlyVideoSurface: Boolean,
): Constraints {
    val (width, height) = windowSize.value
    return when {
        width <= 400.dp && height < 500.dp  -> Compact(insets)
        width > height -> MobileLandscape(insets)
        else -> MobilePortrait(insets)
    }
}

private fun MobileLandscape(insets: DpRect) = object : Constraints {
    override val titleTextSize: TextUnit = TITLE_TEXT_SIZE_LARGE
    override val value: ConstraintSet = ConstraintSet {
        // Background
        constrain(REF_BACKGROUND) {
            linkTo(parent.start, parent.top, parent.end, parent.bottom)
            width = Dimension.fillToConstraints
            height = Dimension.fillToConstraints
        }
        // Artwork
        val (left, up, right, down) = insets
        val split = this.createGuidelineFromStart(0.48f)
        constrain(REF_ARTWORK) {
            linkTo(parent.start, split, left + CP.large, endMargin = CP.large)
            linkTo(parent.top, parent.bottom, up + CP.medium, down + CP.large)
            width = Dimension.fillToConstraints
            height = Dimension.ratio("1:1")
        }

        // Collapse
        constrain(REF_CLOSE_BTN) {
            end.linkTo(parent.end, right + CP.large)
            top.linkTo(REF_ARTWORK.top)
        }

        val options = horizontal(
            REF_QUEUE, REF_SPEED, REF_SLEEP_TIMER, REF_LIKED, REF_MORE,
            chainStyle = ChainStyle.Packed(1f),
            constrainBlock = {
                end.linkTo(REF_CLOSE_BTN.start, CP.normal)
                start.linkTo(split)
            }
        )
        constrain(options) {
            linkTo(REF_CLOSE_BTN.top, REF_CLOSE_BTN.bottom)
        }

        // Subtitle
        constrain(REF_SUBTITLE) {
            linkTo(split, REF_TITLE.end, endMargin = CP.xLarge)
            top.linkTo(REF_CLOSE_BTN.bottom, CP.large)
            width = Dimension.fillToConstraints
            horizontalBias = 0f
        }

        // Title
        constrain(REF_TITLE) {
            linkTo(split, REF_CLOSE_BTN.end)
            top.linkTo(REF_SUBTITLE.bottom)
            width = Dimension.fillToConstraints
        }

        // SeekBar
        val timeBar = horizontal(
            REF_RESIZE_MODE, REF_SEEK_BAR, REF_ROTATION_LOCK,
            alignBy = REF_SEEK_BAR,
            constrainBlock = {
                start.linkTo(split)
                end.linkTo(REF_CLOSE_BTN.end)
            }
        )
        constrain(timeBar) {
            linkTo(REF_TITLE.bottom, parent.bottom, CP.normal, down)
            width = Dimension.fillToConstraints
            verticalBias = 0f
        }
        constrain(REF_POSITION) {
            start.linkTo(timeBar.start)
            bottom.linkTo(timeBar.top, -CP.medium)
        }

        // Controls
        val controls = horizontal(
            REF_SHUFFLE, REF_SKIP_PREVIOUS, REF_PLAY_PAUSE, REF_SKIP_TO_NEXT, REF_REPEAT_MODE,
            alignBy = REF_PLAY_PAUSE,
            constrainBlock = {
                start.linkTo(split)
                end.linkTo(REF_CLOSE_BTN.end)
            }
        )
        constrain(controls) {
            top.linkTo(timeBar.bottom, CP.normal)
        }
    }
}

/**
 * Represents constraints for Audio Player in Mobile Phone setting
 */
private fun MobilePortrait(insets: DpRect) = object : Constraints {
    override val titleTextSize: TextUnit = TITLE_TEXT_SIZE_LARGE
    override val value: ConstraintSet = ConstraintSet {
        // Background
        constrain(REF_BACKGROUND) {
            linkTo(parent.start, parent.top, parent.end, parent.bottom)
            width = Dimension.fillToConstraints
            height = Dimension.fillToConstraints
        }
        val (left, up, right, down) = insets

        // Collapse
        constrain(REF_CLOSE_BTN) {
            top.linkTo(parent.top, margin = up + CP.large)
            end.linkTo(parent.end, margin = right + CP.large)
        }

        // Artwork
        constrain(REF_ARTWORK) {
            linkTo(parent.start, parent.end, CP.xLarge + left, CP.xLarge + right)
            linkTo(REF_CLOSE_BTN.bottom, REF_SUBTITLE.top, CP.normal, CP.normal)
            width = Dimension.fillToConstraints
            height = Dimension.ratio("1:1")
        }

        // Options
        val options = horizontal(
            REF_QUEUE, REF_SPEED, REF_SLEEP_TIMER, REF_LIKED, REF_MORE,
            constrainBlock = {
                start.linkTo(parent.start, left + CP.large)
                end.linkTo(parent.end, right + CP.large)
            }
        )
        constrain(options) {
            bottom.linkTo(parent.bottom, down + CP.normal)
        }

        // Controls
        val controls = horizontal(
            REF_SHUFFLE, REF_SKIP_PREVIOUS, REF_PLAY_PAUSE, REF_SKIP_TO_NEXT, REF_REPEAT_MODE,
            alignBy = REF_PLAY_PAUSE,
            constrainBlock = {
                start.linkTo(parent.start, left + CP.large)
                end.linkTo(parent.end, right + CP.large)
            }
        )
        constrain(controls) {
            bottom.linkTo(options.top, CP.normal)
        }

        val timeBar = horizontal(
            REF_RESIZE_MODE, REF_SEEK_BAR, REF_ROTATION_LOCK,
            alignBy = REF_SEEK_BAR,
            constrainBlock = {
                start.linkTo(parent.start, left + CP.large)
                end.linkTo(parent.end, right + CP.large)
            }
        )
        constrain(timeBar) {
            bottom.linkTo(controls.top, CP.normal)
            width = Dimension.fillToConstraints
        }

        // Position
        constrain(REF_POSITION) {
            start.linkTo(REF_SEEK_BAR.start)
            bottom.linkTo(REF_SEEK_BAR.top, -CP.medium)
        }
        // Title
        constrain(REF_TITLE) {
            linkTo(parent.start, parent.end, left + CP.xLarge, right + CP.xLarge)
            bottom.linkTo(REF_POSITION.top, CP.normal)
            width = Dimension.fillToConstraints
        }
        // Subtitle
        constrain(REF_SUBTITLE) {
            linkTo(REF_TITLE.start, REF_TITLE.end, endMargin = CP.large)
            bottom.linkTo(REF_TITLE.top)
            width = Dimension.fillToConstraints
            horizontalBias = 0f
        }
    }
}

/**
 * Represents constraints for Audio Player when in Compact mode. 320-360 x 320 x 500
 */
private fun Compact(insets: DpRect) = object : Constraints {
    override val titleTextSize: TextUnit = TITLE_TEXT_SIZE_LARGE
    override val value: ConstraintSet = ConstraintSet {
        // Background
        constrain(REF_BACKGROUND) {
            linkTo(parent.start, parent.top, parent.end, parent.bottom)
            width = Dimension.fillToConstraints
            height = Dimension.fillToConstraints
        }

        val (left, up, right, down) = insets
        // Collapse
        constrain(REF_CLOSE_BTN) {
            end.linkTo(parent.end, right + CP.normal)
            top.linkTo(parent.top, up + CP.normal)
        }
        // Options
        val options = horizontal(
            REF_QUEUE, REF_SPEED, REF_SLEEP_TIMER, REF_LIKED, REF_MORE,
            chainStyle = ChainStyle.Packed(1f),
            constrainBlock = {
                end.linkTo(REF_CLOSE_BTN.start, CP.normal)
                start.linkTo(parent.start, CP.normal)
            }
        )
        constrain(options) {
            linkTo(REF_CLOSE_BTN.top, REF_CLOSE_BTN.bottom)
        }
        // Artwork
        val split = this.createGuidelineFromStart(0.35f)
        constrain(REF_ARTWORK) {
            linkTo(parent.start, split, left + CP.normal, endMargin = CP.normal)
            top.linkTo(options.bottom,  CP.medium)
            width = Dimension.fillToConstraints
            height = Dimension.ratio("1:1")
        }
        // Title
        constrain(REF_TITLE) {
            linkTo(split, REF_CLOSE_BTN.end)
            linkTo(REF_ARTWORK.top, REF_ARTWORK.bottom, CP.medium)
            width = Dimension.fillToConstraints
        }

        // Subtitle
        constrain(REF_SUBTITLE) {
            linkTo(split, REF_TITLE.end, endMargin = CP.xLarge)
            bottom.linkTo(REF_TITLE.top)
            width = Dimension.fillToConstraints
            horizontalBias = 0f
        }
        // SeekBar
        val timeBar = horizontal(
            REF_RESIZE_MODE, REF_SEEK_BAR, REF_ROTATION_LOCK,
            alignBy = REF_SEEK_BAR,
            constrainBlock = {
                linkTo(REF_ARTWORK.start, REF_CLOSE_BTN.end)
            }
        )
        constrain(timeBar) {
            top.linkTo(REF_ARTWORK.bottom, CP.large)
            width = Dimension.fillToConstraints
        }
        // Controls
        val controls = horizontal(
            REF_SHUFFLE, REF_SKIP_PREVIOUS, REF_PLAY_PAUSE, REF_SKIP_TO_NEXT, REF_REPEAT_MODE,
            alignBy = REF_PLAY_PAUSE,
            constrainBlock = {
                linkTo(REF_ARTWORK.start, REF_CLOSE_BTN.end)
            }
        )
        constrain(controls) {
            linkTo(timeBar.bottom, parent.bottom, CP.medium, down + CP.medium)
            verticalBias = 0f
        }

        constrain(REF_POSITION) {
            start.linkTo(timeBar.start)
            bottom.linkTo(timeBar.top, -CP.medium)
        }
    }
}
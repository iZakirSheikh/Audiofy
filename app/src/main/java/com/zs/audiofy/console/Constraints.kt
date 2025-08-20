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

import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import androidx.constraintlayout.compose.atMost
import com.zs.audiofy.common.compose.dimensions
import com.zs.audiofy.common.compose.hide
import com.zs.audiofy.common.compose.horizontal
import com.zs.audiofy.common.compose.linkTo
import com.zs.compose.theme.WindowSize
import com.zs.audiofy.common.compose.ContentPadding as CP

// Represents constraints in Compact audio form
private fun CompactAudio(insets: DpRect) = object : Constraints(34) {
    override val constraints = ConstraintSet {
        // Background
        constrain(BACKGROUND) {
            linkTo(parent.start, parent.top, parent.end, parent.bottom)
            dimensions = Dimension.fillToConstraints
        }

        // Collapse
        val (left, up, right, down) = insets
        constrain(COLLAPSE) {
            end.linkTo(parent.end, CP.normal + right)
            top.linkTo(parent.top, CP.normal + up)
        }

        // Options
        val options = horizontal(
            REPEAT_MODE, INFO, SHUFFLE, QUEUE, SPEED, SLEEP_TIMER, LIKED, MORE,
            chainStyle = ChainStyle.Packed(1f),
            constrainBlock = {
                linkTo(parent.start, COLLAPSE.start, left + CP.normal, CP.medium)
            }
        )
        constrain(options) {
            linkTo(COLLAPSE.top, COLLAPSE.bottom)
        }

        // Artwork
        constrain(ARTWORK) {
            start.linkTo(parent.start, left + CP.normal)
            top.linkTo(COLLAPSE.bottom, CP.medium)
            dimensions = Dimension.value(86.dp)
        }

        // Play Button
        constrain(PLAY_PAUSE) {
            end.linkTo(COLLAPSE.end)
            top.linkTo(ARTWORK.top)
        }

        // Subtitle
        constrain(SUBTITLE) {
            bottom.linkTo(TITLE.top)
            linkTo(TITLE.start, TITLE.end)
            width = Dimension.fillToConstraints.atMost(160.dp)
        }

        // TITLE
        constrain(TITLE) {
            linkTo(ARTWORK.end, PLAY_PAUSE.start, CP.normal, CP.medium)
            linkTo(ARTWORK.top, ARTWORK.bottom)
            width = Dimension.fillToConstraints
        }

        // SeekBar
        val chainStyle = ChainStyle.Packed(0f)
        val timeBar = horizontal(
            INDICATOR, SKIP_PREVIOUS, SEEK_BAR, SKIP_TO_NEXT, EQUALIZER,
            alignBy = SEEK_BAR,
            chainStyle = chainStyle,
            constrainBlock = {
                linkTo(ARTWORK.start, COLLAPSE.end)
            }
        )
        constrain(timeBar) {
            linkTo(ARTWORK.bottom, parent.bottom, CP.medium, down)
            width = Dimension.fillToConstraints.atMost(230.dp)
            verticalBias = 0f
        }

        // Extra info
        constrain(EXTRA_INFO) {
            start.linkTo(timeBar.start)
            bottom.linkTo(timeBar.top, -CP.medium)
        }

        // Make these invisible
        hide(INFO, ROTATION_LOCK, REPEAT_MODE, SHUFFLE)
    }
}

// Represents constraints in Small audio form
private fun SmallAudio(insets: DpRect) = object : Constraints(44) {
    override val constraints: ConstraintSet = ConstraintSet {
        // Background
        constrain(BACKGROUND) {
            linkTo(parent.start, parent.top, parent.end, parent.bottom)
            dimensions = Dimension.fillToConstraints
        }

        val (left, up, right, down) = insets
        // Collapse
        constrain(COLLAPSE) {
            end.linkTo(parent.end, right + CP.normal)
            top.linkTo(parent.top, up + CP.normal)
        }

        // Options
        val options = horizontal(
            QUEUE, SPEED, SLEEP_TIMER, LIKED, MORE,
            chainStyle = ChainStyle.Packed(1f),
            constrainBlock = {
                end.linkTo(COLLAPSE.start, CP.normal)
                start.linkTo(parent.start, CP.normal)
            }
        )
        constrain(options) {
            linkTo(COLLAPSE.top, COLLAPSE.bottom)
        }

        // Artwork
        constrain(ARTWORK) {
            start.linkTo(parent.start, left + CP.normal)
            top.linkTo(options.bottom, CP.medium)
            width = Dimension.percent(0.3f)
            height = Dimension.ratio("1:1")
        }

        constrain(INFO) {
            end.linkTo(COLLAPSE.end)
            top.linkTo(TITLE.top)
        }

        // Title
        constrain(TITLE) {
            linkTo(ARTWORK.end, INFO.start, CP.normal, CP.normal)
            top.linkTo(ARTWORK.top)
            width = Dimension.fillToConstraints
        }
        // Subtitle
        constrain(SUBTITLE) {
            linkTo(TITLE.start, TITLE.end)
            top.linkTo(TITLE.bottom)
            width = Dimension.fillToConstraints.atMost(160.dp)
            horizontalBias = 0f
        }

        // TimeBar
        val timeBar = horizontal(
            INDICATOR, SEEK_BAR, ROTATION_LOCK, EQUALIZER,
            alignBy = SEEK_BAR,
            constrainBlock = {
                linkTo(ARTWORK.start, COLLAPSE.end)
            }
        )
        constrain(timeBar) {
            top.linkTo(ARTWORK.bottom, CP.large)
            width = Dimension.fillToConstraints
        }

        // Extra-Info
        constrain(EXTRA_INFO) {
            start.linkTo(SEEK_BAR.start)
            bottom.linkTo(SEEK_BAR.top, -CP.medium)
        }

        // Controls
        val controls = horizontal(
            SHUFFLE, SKIP_PREVIOUS, PLAY_PAUSE, SKIP_TO_NEXT, REPEAT_MODE,
            alignBy = PLAY_PAUSE,
            constrainBlock = {
                linkTo(ARTWORK.start, COLLAPSE.end)
            }
        )
        constrain(controls) {
            linkTo(timeBar.bottom, parent.bottom, CP.medium, down + CP.medium)
            verticalBias = 0f
        }
    }
}

// Represents constraints in Portrait audio form
private fun PortraitAudio(insets: DpRect) = object : Constraints(44) {
    override val constraints: ConstraintSet = ConstraintSet {
        // Background
        constrain(BACKGROUND) {
            linkTo(parent.start, parent.top, parent.end, parent.bottom)
            dimensions = Dimension.fillToConstraints
        }

        val (left, up, right, down) = insets
        // Collapse
        constrain(COLLAPSE) {
            top.linkTo(parent.top, margin = up + CP.large)
            end.linkTo(parent.end, margin = right + CP.large)
        }

        // Artwork
        constrain(ARTWORK) {
            linkTo(parent.start, parent.end, CP.xLarge + left, CP.xLarge + right)
            linkTo(COLLAPSE.bottom, SUBTITLE.top, CP.normal, CP.normal)
            width = Dimension.fillToConstraints
            height = Dimension.ratio("1:1")
        }

        // Options
        val options = horizontal(
            QUEUE, SPEED, SLEEP_TIMER, LIKED, MORE,
            constrainBlock = {
                linkTo(TITLE.start, COLLAPSE.end)
            }
        )
        constrain(options) {
            bottom.linkTo(parent.bottom, down + CP.normal)
        }

        // Controls
        val controls = horizontal(
            SHUFFLE, SKIP_PREVIOUS, PLAY_PAUSE, SKIP_TO_NEXT, REPEAT_MODE,
            alignBy = PLAY_PAUSE,
            constrainBlock = {
                linkTo(TITLE.start, COLLAPSE.end)
            }
        )
        constrain(controls) {
            bottom.linkTo(options.top, CP.normal)
        }

        // TimeBar
        val timeBar = horizontal(
            INDICATOR, SEEK_BAR, ROTATION_LOCK, EQUALIZER,
            alignBy = SEEK_BAR,
            constrainBlock = {
                linkTo(TITLE.start, COLLAPSE.end)
            }
        )
        constrain(timeBar) {
            bottom.linkTo(controls.top, CP.normal)
            width = Dimension.fillToConstraints
        }

        // Extra-Info
        constrain(EXTRA_INFO) {
            start.linkTo(SEEK_BAR.start)
            bottom.linkTo(SEEK_BAR.top, -CP.medium)
        }

        // Info
        constrain(INFO){
            end.linkTo(COLLAPSE.end)
            linkTo(SUBTITLE.top, TITLE.bottom)
        }

        // Title
        constrain(TITLE) {
            linkTo(parent.start, INFO.start, left + CP.xLarge, CP.normal)
            bottom.linkTo(EXTRA_INFO.top, CP.normal)
            width = Dimension.fillToConstraints
        }

        // Subtitle
        constrain(SUBTITLE) {
            linkTo(TITLE.start, TITLE.end, endMargin = CP.large)
            bottom.linkTo(TITLE.top)
            width = Dimension.fillToConstraints
            horizontalBias = 0f
        }
    }
}

//
private fun LandscapeAudio(insets: DpRect) = object : Constraints(44) {
    override val constraints: ConstraintSet = ConstraintSet {
        // Background
        constrain(BACKGROUND) {
            linkTo(parent.start, parent.top, parent.end, parent.bottom)
            dimensions = Dimension.fillToConstraints
        }
        // Artwork
        val (left, up, right, down) = insets
        val split = this.createGuidelineFromStart(0.45f)
        constrain(ARTWORK) {
            linkTo(parent.start, split, left + CP.large, endMargin = CP.large)
            linkTo(parent.top, parent.bottom, up + CP.medium, down + CP.large)
            width = Dimension.fillToConstraints
            height = Dimension.ratio("1:1")
        }

        // Collapse
        constrain(COLLAPSE) {
            end.linkTo(parent.end, right + CP.large)
            top.linkTo(ARTWORK.top)
        }

        // Options
        val options = horizontal(
            QUEUE, SPEED, SLEEP_TIMER, LIKED, MORE,
            chainStyle = ChainStyle.Packed(1f),
            constrainBlock = {
                linkTo(split, COLLAPSE.start, endMargin = CP.normal)
            }
        )
        constrain(options) {
            linkTo(COLLAPSE.top, COLLAPSE.bottom)
        }

        // Subtitle
        constrain(SUBTITLE) {
            linkTo(split, TITLE.end)
            top.linkTo(COLLAPSE.bottom, CP.large)
            width = Dimension.fillToConstraints.atMost(160.dp)
            horizontalBias = 0f
        }

        // Title
        constrain(TITLE) {
            linkTo(split, INFO.start, endMargin = CP.normal)
            top.linkTo(SUBTITLE.bottom)
            width = Dimension.fillToConstraints
        }

        // Info
        constrain(INFO){
            end.linkTo(COLLAPSE.end)
            linkTo(SUBTITLE.top, TITLE.bottom)
        }

        // SeekBar
        val chainStyle = ChainStyle.Packed(0f)
        val timeBar = horizontal(
            INDICATOR, SEEK_BAR, ROTATION_LOCK, EQUALIZER,
            alignBy = SEEK_BAR,
            chainStyle = chainStyle,
            constrainBlock = {
                linkTo(split, COLLAPSE.end)
            }
        )
        constrain(timeBar) {
            linkTo(TITLE.bottom, parent.bottom, CP.normal, down)
            width = Dimension.fillToConstraints.atMost(230.dp)
            verticalBias = 0f
        }
        // Extra info
        constrain(EXTRA_INFO) {
            start.linkTo(timeBar.start)
            bottom.linkTo(timeBar.top, -CP.medium)
        }

        // Controls
        val controls = horizontal(
            SHUFFLE, SKIP_PREVIOUS, PLAY_PAUSE, SKIP_TO_NEXT, REPEAT_MODE,
            alignBy = PLAY_PAUSE,
            chainStyle = ChainStyle.Packed(0f),
            constrainBlock = {
                linkTo(split, COLLAPSE.end)
            }
        )
        constrain(controls) {
            top.linkTo(timeBar.bottom, CP.normal)
        }
    }
}
//
private fun LargeAudio(insets: DpRect) = object : Constraints(44) {
    override val constraints: ConstraintSet = ConstraintSet {
        // Background
        constrain(BACKGROUND) {
            linkTo(parent.start, parent.top, parent.end, parent.bottom)
            dimensions = Dimension.fillToConstraints
        }
        // Collapse
        val (left, up, right, down) = insets
        constrain(COLLAPSE) {
            end.linkTo(parent.end, right + CP.large)
            top.linkTo(parent.top, up + CP.large)
        }
        // Options
        val options = horizontal(
            QUEUE, SPEED, SLEEP_TIMER, LIKED, MORE,
            chainStyle = ChainStyle.Packed(1f),
            constrainBlock = {
                linkTo(parent.start, COLLAPSE.start, endMargin = CP.large)
            }
        )
        constrain(options) {
            linkTo(COLLAPSE.top, COLLAPSE.bottom)
        }

        // Controls
        val controls = horizontal(
            SHUFFLE, SKIP_PREVIOUS, PLAY_PAUSE, SKIP_TO_NEXT, REPEAT_MODE,
            alignBy = PLAY_PAUSE,
            constrainBlock = {
                linkTo(TITLE.end, COLLAPSE.end, startMargin = CP.normal)
            }
        )
        constrain(controls) {
            bottom.linkTo(TITLE.bottom)
        }

        // Title
        constrain(TITLE) {
            linkTo(parent.start, parent.end, startMargin = left + CP.xLarge)
            bottom.linkTo(parent.bottom, down + CP.normal)
            top.linkTo(controls.top, CP.normal)
            width = Dimension.percent(0.38f)
            horizontalBias = 0f
        }
        // Subtitle
        constrain(SUBTITLE) {
            linkTo(TITLE.start, TITLE.end)
            bottom.linkTo(TITLE.top)
            width = Dimension.fillToConstraints.atMost(160.dp)
            horizontalBias = 0f
        }

        // SeekBar
        val chainStyle = ChainStyle.Packed(0f)
        val timeBar = horizontal(
            INDICATOR, SEEK_BAR, ROTATION_LOCK, EQUALIZER, INFO,
            alignBy = SEEK_BAR,
            chainStyle = chainStyle,
            constrainBlock = {
                linkTo(TITLE.start, COLLAPSE.end)
            }
        )
        constrain(timeBar) {
            bottom.linkTo(controls.top, CP.normal)
            width = Dimension.fillToConstraints
            verticalBias = 0f
        }

        // Extra info
        constrain(EXTRA_INFO) {
            start.linkTo(timeBar.start)
            bottom.linkTo(timeBar.top, -CP.medium)
        }
        // Artwork
        constrain(ARTWORK) {
            linkTo(parent.start, COLLAPSE.end, left + CP.large, )
            linkTo(COLLAPSE.bottom, timeBar.top,  CP.large, CP.large)
            width = Dimension.fillToConstraints.atMost(260.dp)
            height = Dimension.ratio("1:1")
        }
    }
}
//
private fun PortraitVideo(insets: DpRect,   only: Array<String>?,) = object : Constraints(20) {
    override val constraints: ConstraintSet = ConstraintSet {
        // Background
        constrain(BACKGROUND) {
            linkTo(parent.start, parent.top, parent.end, parent.bottom)
            dimensions = Dimension.fillToConstraints
        }

        // skrim
        constrain(SCRIM){
            linkTo(parent.start, parent.top, parent.end, parent.bottom)
            dimensions = Dimension.fillToConstraints
        }

        // artwork
        constrain(ARTWORK){
            linkTo(parent.start, parent.top, parent.end, parent.bottom)
            visibility = Visibility.Gone
        }
        // Video Surface
        constrain(VIDEO_SURFACE) {
            linkTo(parent.start, parent.top, parent.end, parent.bottom)
            // dimensions = Dimension.fillToConstraints
        }


        // Options
        COLLAPSE.withChainParams(startMargin = CP.normal)
        val options = horizontal(
            EQUALIZER, QUEUE, SPEED, SLEEP_TIMER, LIKED, MORE, COLLAPSE,
            constrainBlock = {
                linkTo(parent.start, parent.end, endMargin = CP.normal)
            }
        )
        val (left, up, right, down) = insets
        constrain(options) {
            top.linkTo(parent.top, up + CP.normal)
            horizontalBias = 1f
        }
        // Title
        TITLE.withChainParams(startMargin = CP.medium, endMargin = CP.medium)
        val title = horizontal(
            INDICATOR, TITLE, INFO,
            alignBy = TITLE,
            constrainBlock = {
                linkTo(parent.start, COLLAPSE.end, CP.normal + left)
            }
        )
        constrain(title) {
            bottom.linkTo(parent.bottom, CP.normal + down)
            width = Dimension.preferredWrapContent
            horizontalChainWeight = 1f
        }

        // Subtitle
        constrain(SUBTITLE) {
            bottom.linkTo(TITLE.top)
            linkTo(TITLE.start, TITLE.end)
            horizontalBias = 0f
            height = Dimension.preferredValue(0.dp)
            width = Dimension.fillToConstraints.atMost(180.dp)
        }

        // TimeBar
        val timeBar = horizontal(
            PLAY_PAUSE, SKIP_PREVIOUS, SEEK_BAR, SKIP_TO_NEXT, ROTATION_LOCK,
            alignBy = SEEK_BAR,
            constrainBlock = {
                linkTo(parent.start, parent.end, left + CP.normal, right + CP.normal)
            }
        )
        constrain(timeBar) {
            bottom.linkTo(title.top, CP.normal)
            width = Dimension.fillToConstraints
        }
        // Extra-Info
        constrain(EXTRA_INFO) {
            start.linkTo(SEEK_BAR.start, 6.dp)
            bottom.linkTo(SEEK_BAR.top, -CP.medium)
        }
        // more
        val more = horizontal(
            SHUFFLE, REPEAT_MODE, RESIZE_MODE,
            chainStyle = ChainStyle.Packed(1f),
            constrainBlock =  {
                linkTo(timeBar.start, COLLAPSE.end)
            }
        )
        constrain(more) {
            bottom.linkTo(EXTRA_INFO.top)
        }
        if (only == null)
            return@ConstraintSet
        hideController(only)
    }
}

//
private fun LargeVideo(insets: DpRect,   only: Array<String>?,) = object : Constraints(16) {
    override val constraints: ConstraintSet = ConstraintSet {
        // Background
        constrain(BACKGROUND) {
            linkTo(parent.start, parent.top, parent.end, parent.bottom)
            dimensions = Dimension.fillToConstraints
        }

        // skrim
        constrain(SCRIM){
            linkTo(parent.start, parent.top, parent.end, parent.bottom)
            dimensions = Dimension.fillToConstraints
        }

        // artwork
        constrain(ARTWORK){
            linkTo(parent.start, parent.top, parent.end, parent.bottom)
            visibility = Visibility.Gone
        }

        // Video Surface
        constrain(VIDEO_SURFACE) {
            linkTo(parent.start, parent.top, parent.end, parent.bottom)
            dimensions = Dimension.wrapContent
        }

        // Collapse
        val (left, up, right, down) = insets
        TITLE.withChainParams(startMargin = CP.medium, endMargin = CP.xLarge)
        COLLAPSE.withChainParams(startMargin = CP.normal)
        val options = horizontal(
            INDICATOR, TITLE, INFO, SHUFFLE, REPEAT_MODE, EQUALIZER, QUEUE, SPEED, SLEEP_TIMER, LIKED, MORE, COLLAPSE,
            alignBy = TITLE,
            constrainBlock = {
                linkTo(parent.start, parent.end, CP.normal + left, CP.normal + right)
            }
        )
        constrain(options) {
            top.linkTo(parent.top, CP.normal + up)
            width = Dimension.preferredWrapContent
            horizontalChainWeight = 1f
        }

        // Subtitle
        constrain(SUBTITLE){
            top.linkTo(TITLE.bottom)
            linkTo(TITLE.start, TITLE.end)
            horizontalBias = 0f
            height = Dimension.preferredValue(0.dp)
            width = Dimension.fillToConstraints.atMost(180.dp)
        }

        // TimeBar
        val timeBar = horizontal(
            SKIP_PREVIOUS, PLAY_PAUSE, SKIP_TO_NEXT, SEEK_BAR, ROTATION_LOCK, RESIZE_MODE,
            alignBy = SEEK_BAR,
            constrainBlock = {
                linkTo(parent.start, parent.end, left + CP.normal, right + CP.normal)
            }
        )
        constrain(timeBar) {
            bottom.linkTo(parent.bottom, CP.small + down)
            width = Dimension.fillToConstraints
        }
        // Extra-Info
        constrain(EXTRA_INFO) {
            start.linkTo(SEEK_BAR.start, 6.dp)
            bottom.linkTo(SEEK_BAR.top, -CP.medium)
        }
        if (only == null)
            return@ConstraintSet
        hideController(only)
    }
}

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
    only: Array<String>?,
): Constraints {
    val (width, height) = windowSize.value
    return when {
        isVideo && height > width -> PortraitVideo(insets, only)
        isVideo -> LargeVideo(insets, only)

        // Compact
        width <= 400.dp && height < 350.dp -> CompactAudio(insets)

        // Small
        width <= 500.dp && height <= 500.dp -> SmallAudio(insets)

        // Phones in landscape
        height < 500.dp && width > height -> LandscapeAudio(insets)

        // Large landscape screens (tablets/desktops)
        width > height && height >= 500.dp -> LargeAudio(insets)

        // Portrait screens // height > width
        else -> PortraitAudio(insets)
    }
}


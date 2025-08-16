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
import androidx.constraintlayout.compose.atMost
import com.zs.audiofy.common.compose.dimensions
import com.zs.audiofy.common.compose.hide
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
private val REF_COLLAPSE = ConstrainedLayoutReference(Console.ID_BTN_COLLAPSE)
private val REF_ARTWORK = ConstrainedLayoutReference(Console.ID_ARTWORK)
private val REF_TITLE = ConstrainedLayoutReference(Console.ID_TITLE)
private val REF_SUBTITLE = ConstrainedLayoutReference(Console.ID_SUBTITLE)
private val REF_EXTRA_INFO = ConstrainedLayoutReference(Console.ID_POSITION)
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
private val REF_EQUALIZER = ConstrainedLayoutReference(Console.ID_BTN_EQUALIZER)
private val REF_INFO = ConstrainedLayoutReference(Console.ID_BTN_MEDIA_INFO)
private val REF_INDICATOR = ConstrainedLayoutReference(Console.ID_PLAYING_INDICATOR)

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
    only: Array<String>,
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

//
private fun CompactAudio(insets: DpRect) = object : Constraints {
    override val titleTextSize: TextUnit = 34.sp
    override val value: ConstraintSet = ConstraintSet {
        // Background
        constrain(REF_BACKGROUND) {
            linkTo(parent.start, parent.top, parent.end, parent.bottom)
            dimensions = Dimension.fillToConstraints
        }

        // Collapse
        val (left, up, right, down) = insets
        constrain(REF_COLLAPSE){
            end.linkTo(parent.end, CP.normal + right)
            top.linkTo(parent.top, CP.normal + up)
        }

        // Options
        val options = horizontal(
            REF_REPEAT_MODE, REF_INFO, REF_SHUFFLE, REF_QUEUE,  REF_SPEED, REF_SLEEP_TIMER, REF_LIKED, REF_MORE,
            chainStyle = ChainStyle.Packed(1f),
            constrainBlock = {
                linkTo(parent.start, REF_COLLAPSE.start, left + CP.normal, CP.medium)
            }
        )
        constrain(options) {
            linkTo(REF_COLLAPSE.top, REF_COLLAPSE.bottom)
        }

        // Artwork
        constrain(REF_ARTWORK) {
            start.linkTo(parent.start, left + CP.normal)
            top.linkTo(REF_COLLAPSE.bottom, CP.medium)
            dimensions = Dimension.value(86.dp)
        }

        // Play Button
        constrain(REF_PLAY_PAUSE){
            end.linkTo(REF_COLLAPSE.end)
            top.linkTo(REF_ARTWORK.top)
        }

        // Subtitle
        constrain(REF_SUBTITLE){
            bottom.linkTo(REF_TITLE.top)
            linkTo(REF_TITLE.start, REF_TITLE.end)
            width = Dimension.fillToConstraints.atMost(160.dp)
        }

        // TITLE
        constrain(REF_TITLE){
            linkTo(REF_ARTWORK.end, REF_PLAY_PAUSE.start, CP.normal, CP.medium)
            linkTo(REF_ARTWORK.top, REF_ARTWORK.bottom)
            width = Dimension.fillToConstraints
        }

        // SeekBar
        val chainStyle = ChainStyle.Packed(0f)
        val timeBar = horizontal(
            REF_INDICATOR, REF_SKIP_PREVIOUS, REF_SEEK_BAR, REF_SKIP_TO_NEXT, REF_EQUALIZER,
            alignBy = REF_SEEK_BAR,
            chainStyle = chainStyle,
            constrainBlock = {
                linkTo(REF_ARTWORK.start, REF_COLLAPSE.end)
            }
        )
        constrain(timeBar) {
            linkTo(REF_ARTWORK.bottom, parent.bottom, CP.medium, down)
            width = Dimension.fillToConstraints.atMost(230.dp)
            verticalBias = 0f
        }

        // Extra info
        constrain(REF_EXTRA_INFO) {
            start.linkTo(timeBar.start)
            bottom.linkTo(timeBar.top, -CP.medium)
        }

        // Make these invisible
        hide(REF_INFO, REF_ROTATION_LOCK, REF_REPEAT_MODE, REF_SHUFFLE)
    }
}
//
private fun SmallAudio(insets: DpRect) = object : Constraints {
    override val titleTextSize: TextUnit = TITLE_TEXT_SIZE_LARGE
    override val value: ConstraintSet = ConstraintSet {
        // Background
        constrain(REF_BACKGROUND) {
            linkTo(parent.start, parent.top, parent.end, parent.bottom)
            dimensions = Dimension.fillToConstraints
        }

        val (left, up, right, down) = insets
        // Collapse
        constrain(REF_COLLAPSE) {
            end.linkTo(parent.end, right + CP.normal)
            top.linkTo(parent.top, up + CP.normal)
        }

        // Options
        val options = horizontal(
            REF_QUEUE, REF_SPEED, REF_SLEEP_TIMER, REF_LIKED, REF_MORE,
            chainStyle = ChainStyle.Packed(1f),
            constrainBlock = {
                end.linkTo(REF_COLLAPSE.start, CP.normal)
                start.linkTo(parent.start, CP.normal)
            }
        )
        constrain(options) {
            linkTo(REF_COLLAPSE.top, REF_COLLAPSE.bottom)
        }

        // Artwork
        constrain(REF_ARTWORK) {
            start.linkTo(parent.start, left + CP.normal)
            top.linkTo(options.bottom,  CP.medium)
            width = Dimension.percent(0.3f)
            height = Dimension.ratio("1:1")
        }

        constrain(REF_INFO){
            end.linkTo(REF_COLLAPSE.end)
            top.linkTo(REF_TITLE.top)
        }

        // Title
        constrain(REF_TITLE) {
            linkTo(REF_ARTWORK.end, REF_INFO.start, CP.normal, CP.normal)
            top.linkTo(REF_ARTWORK.top)
            width = Dimension.fillToConstraints
        }
        // Subtitle
        constrain(REF_SUBTITLE) {
            linkTo(REF_TITLE.start, REF_TITLE.end)
            top.linkTo(REF_TITLE.bottom)
            width = Dimension.fillToConstraints.atMost(160.dp)
            horizontalBias = 0f
        }

        // TimeBar
        val timeBar = horizontal(
            REF_INDICATOR, REF_SEEK_BAR, REF_ROTATION_LOCK, REF_EQUALIZER,
            alignBy = REF_SEEK_BAR,
            constrainBlock = {
                linkTo(REF_ARTWORK.start, REF_COLLAPSE.end)
            }
        )
        constrain(timeBar) {
            top.linkTo(REF_ARTWORK.bottom, CP.large)
            width = Dimension.fillToConstraints
        }

        // Extra-Info
        constrain(REF_EXTRA_INFO) {
            start.linkTo(REF_SEEK_BAR.start)
            bottom.linkTo(REF_SEEK_BAR.top, -CP.medium)
        }

        // Controls
        val controls = horizontal(
            REF_SHUFFLE, REF_SKIP_PREVIOUS, REF_PLAY_PAUSE, REF_SKIP_TO_NEXT, REF_REPEAT_MODE,
            alignBy = REF_PLAY_PAUSE,
            constrainBlock = {
                linkTo(REF_ARTWORK.start, REF_COLLAPSE.end)
            }
        )
        constrain(controls) {
            linkTo(timeBar.bottom, parent.bottom, CP.medium, down + CP.medium)
            verticalBias = 0f
        }
    }
}
//
private fun PortraitAudio(insets: DpRect) = object : Constraints {
    override val titleTextSize: TextUnit = TITLE_TEXT_SIZE_LARGE
    override val value: ConstraintSet = ConstraintSet {
        // Background
        constrain(REF_BACKGROUND) {
            linkTo(parent.start, parent.top, parent.end, parent.bottom)
            dimensions = Dimension.fillToConstraints
        }

        val (left, up, right, down) = insets
        // Collapse
        constrain(REF_COLLAPSE) {
            top.linkTo(parent.top, margin = up + CP.large)
            end.linkTo(parent.end, margin = right + CP.large)
        }

        // Artwork
        constrain(REF_ARTWORK) {
            linkTo(parent.start, parent.end, CP.xLarge + left, CP.xLarge + right)
            linkTo(REF_COLLAPSE.bottom, REF_SUBTITLE.top, CP.normal, CP.normal)
            width = Dimension.fillToConstraints
            height = Dimension.ratio("1:1")
        }

        // Options
        val options = horizontal(
            REF_QUEUE, REF_SPEED, REF_SLEEP_TIMER, REF_LIKED, REF_MORE,
            constrainBlock = {
                linkTo(REF_TITLE.start, REF_COLLAPSE.end)
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
                linkTo(REF_TITLE.start, REF_COLLAPSE.end)
            }
        )
        constrain(controls) {
            bottom.linkTo(options.top, CP.normal)
        }

        // TimeBar
        val timeBar = horizontal(
            REF_INDICATOR, REF_SEEK_BAR, REF_ROTATION_LOCK, REF_EQUALIZER,
            alignBy = REF_SEEK_BAR,
            constrainBlock = {
                linkTo(REF_TITLE.start, REF_COLLAPSE.end)
            }
        )
        constrain(timeBar) {
            bottom.linkTo(controls.top, CP.normal)
            width = Dimension.fillToConstraints
        }

        // Extra-Info
        constrain(REF_EXTRA_INFO) {
            start.linkTo(REF_SEEK_BAR.start)
            bottom.linkTo(REF_SEEK_BAR.top, -CP.medium)
        }

        // Info
        constrain(REF_INFO){
            end.linkTo(REF_COLLAPSE.end)
            linkTo(REF_SUBTITLE.top, REF_TITLE.bottom)
        }

        // Title
        constrain(REF_TITLE) {
            linkTo(parent.start, REF_INFO.start, left + CP.xLarge, CP.normal)
            bottom.linkTo(REF_EXTRA_INFO.top, CP.normal)
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
//
private fun LandscapeAudio(insets: DpRect) = object : Constraints {
    override val titleTextSize: TextUnit = TITLE_TEXT_SIZE_LARGE
    override val value: ConstraintSet = ConstraintSet {
        // Background
        constrain(REF_BACKGROUND) {
            linkTo(parent.start, parent.top, parent.end, parent.bottom)
            dimensions = Dimension.fillToConstraints
        }
        // Artwork
        val (left, up, right, down) = insets
        val split = this.createGuidelineFromStart(0.45f)
        constrain(REF_ARTWORK) {
            linkTo(parent.start, split, left + CP.large, endMargin = CP.large)
            linkTo(parent.top, parent.bottom, up + CP.medium, down + CP.large)
            width = Dimension.fillToConstraints
            height = Dimension.ratio("1:1")
        }

        // Collapse
        constrain(REF_COLLAPSE) {
            end.linkTo(parent.end, right + CP.large)
            top.linkTo(REF_ARTWORK.top)
        }

        // Options
        val options = horizontal(
            REF_QUEUE, REF_SPEED, REF_SLEEP_TIMER, REF_LIKED, REF_MORE,
            chainStyle = ChainStyle.Packed(1f),
            constrainBlock = {
                linkTo(split, REF_COLLAPSE.start, endMargin = CP.normal)
            }
        )
        constrain(options) {
            linkTo(REF_COLLAPSE.top, REF_COLLAPSE.bottom)
        }

        // Subtitle
        constrain(REF_SUBTITLE) {
            linkTo(split, REF_TITLE.end)
            top.linkTo(REF_COLLAPSE.bottom, CP.large)
            width = Dimension.fillToConstraints.atMost(160.dp)
            horizontalBias = 0f
        }

        // Title
        constrain(REF_TITLE) {
            linkTo(split, REF_INFO.start, endMargin = CP.normal)
            top.linkTo(REF_SUBTITLE.bottom)
            width = Dimension.fillToConstraints
        }

        // Info
        constrain(REF_INFO){
            end.linkTo(REF_COLLAPSE.end)
            linkTo(REF_SUBTITLE.top, REF_TITLE.bottom)
        }

        // SeekBar
        val chainStyle = ChainStyle.Packed(0f)
        val timeBar = horizontal(
            REF_INDICATOR, REF_SEEK_BAR, REF_ROTATION_LOCK, REF_EQUALIZER,
            alignBy = REF_SEEK_BAR,
            chainStyle = chainStyle,
            constrainBlock = {
                linkTo(split, REF_COLLAPSE.end)
            }
        )
        constrain(timeBar) {
            linkTo(REF_TITLE.bottom, parent.bottom, CP.normal, down)
            width = Dimension.fillToConstraints.atMost(230.dp)
            verticalBias = 0f
        }
        // Extra info
        constrain(REF_EXTRA_INFO) {
            start.linkTo(timeBar.start)
            bottom.linkTo(timeBar.top, -CP.medium)
        }

        // Controls
        val controls = horizontal(
            REF_SHUFFLE, REF_SKIP_PREVIOUS, REF_PLAY_PAUSE, REF_SKIP_TO_NEXT, REF_REPEAT_MODE,
            alignBy = REF_PLAY_PAUSE,
            chainStyle = ChainStyle.Packed(0f),
            constrainBlock = {
                linkTo(split, REF_COLLAPSE.end)
            }
        )
        constrain(controls) {
            top.linkTo(timeBar.bottom, CP.normal)
        }
    }
}
//
private fun LargeAudio(insets: DpRect) = object : Constraints {
    override val titleTextSize: TextUnit = TITLE_TEXT_SIZE_LARGE
    override val value: ConstraintSet = ConstraintSet {
        // Background
        constrain(REF_BACKGROUND) {
            linkTo(parent.start, parent.top, parent.end, parent.bottom)
            dimensions = Dimension.fillToConstraints
        }
        // Collapse
        val (left, up, right, down) = insets
        constrain(REF_COLLAPSE) {
            end.linkTo(parent.end, right + CP.large)
            top.linkTo(parent.top, up + CP.large)
        }
        // Options
        val options = horizontal(
            REF_QUEUE, REF_SPEED, REF_SLEEP_TIMER, REF_LIKED, REF_MORE,
            chainStyle = ChainStyle.Packed(1f),
            constrainBlock = {
                linkTo(parent.start, REF_COLLAPSE.start, endMargin = CP.large)
            }
        )
        constrain(options) {
            linkTo(REF_COLLAPSE.top, REF_COLLAPSE.bottom)
        }

        // Controls
        val controls = horizontal(
            REF_SHUFFLE, REF_SKIP_PREVIOUS, REF_PLAY_PAUSE, REF_SKIP_TO_NEXT, REF_REPEAT_MODE,
            alignBy = REF_PLAY_PAUSE,
            constrainBlock = {
                linkTo(REF_TITLE.end, REF_COLLAPSE.end, startMargin = CP.normal)
            }
        )
        constrain(controls) {
            bottom.linkTo(REF_TITLE.bottom)
        }

        // Title
        constrain(REF_TITLE) {
            linkTo(parent.start, parent.end, startMargin = left + CP.xLarge)
            bottom.linkTo(parent.bottom, down + CP.normal)
            top.linkTo(controls.top, CP.normal)
            width = Dimension.percent(0.38f)
            horizontalBias = 0f
        }
        // Subtitle
        constrain(REF_SUBTITLE) {
            linkTo(REF_TITLE.start, REF_TITLE.end)
            bottom.linkTo(REF_TITLE.top)
            width = Dimension.fillToConstraints.atMost(160.dp)
            horizontalBias = 0f
        }

        // SeekBar
        val chainStyle = ChainStyle.Packed(0f)
        val timeBar = horizontal(
            REF_INDICATOR, REF_SEEK_BAR, REF_ROTATION_LOCK, REF_EQUALIZER, REF_INFO,
            alignBy = REF_SEEK_BAR,
            chainStyle = chainStyle,
            constrainBlock = {
                linkTo(REF_TITLE.start, REF_COLLAPSE.end)
            }
        )
        constrain(timeBar) {
            bottom.linkTo(controls.top, CP.normal)
            width = Dimension.fillToConstraints
            verticalBias = 0f
        }

        // Extra info
        constrain(REF_EXTRA_INFO) {
            start.linkTo(timeBar.start)
            bottom.linkTo(timeBar.top, -CP.medium)
        }
        // Artwork
        constrain(REF_ARTWORK) {
            linkTo(parent.start, REF_COLLAPSE.end, left + CP.large, )
            linkTo(REF_COLLAPSE.bottom, timeBar.top,  CP.large, CP.large)
            width = Dimension.fillToConstraints.atMost(260.dp)
            height = Dimension.ratio("1:1")
        }
    }
}
//
private fun PortraitVideo(insets: DpRect,   only: Array<String>,) = object : Constraints {
    override val titleTextSize: TextUnit = 20.sp
    override val value: ConstraintSet = ConstraintSet {
        // Background
        constrain(REF_BACKGROUND) {
            linkTo(parent.start, parent.top, parent.end, parent.bottom)
            dimensions = Dimension.fillToConstraints
        }

        // Video Surface
        constrain(REF_VIDEO_SURFACE) {
            linkTo(parent.start, parent.top, parent.end, parent.bottom)
           // dimensions = Dimension.fillToConstraints
        }


        // Options
        REF_COLLAPSE.withChainParams(startMargin = CP.normal)
        val options = horizontal(
            REF_EQUALIZER, REF_QUEUE, REF_SPEED, REF_SLEEP_TIMER, REF_LIKED, REF_MORE, REF_COLLAPSE,
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
        REF_TITLE.withChainParams(startMargin = CP.medium, endMargin = CP.medium)
        val title = horizontal(
            REF_INDICATOR, REF_TITLE, REF_INFO,
            alignBy = REF_TITLE,
            constrainBlock = {
                linkTo(parent.start, REF_COLLAPSE.end, CP.normal + left)
            }
        )
        constrain(title) {
            bottom.linkTo(parent.bottom, CP.normal + down)
            width = Dimension.preferredWrapContent
            horizontalChainWeight = 1f
        }

        // Subtitle
        constrain(REF_SUBTITLE) {
            bottom.linkTo(REF_TITLE.top)
            linkTo(REF_TITLE.start, REF_TITLE.end)
            horizontalBias = 0f
            height = Dimension.preferredValue(0.dp)
            width = Dimension.fillToConstraints.atMost(180.dp)
        }

        // TimeBar
        val timeBar = horizontal(
            REF_PLAY_PAUSE, REF_SKIP_PREVIOUS, REF_SEEK_BAR, REF_SKIP_TO_NEXT, REF_ROTATION_LOCK,
            alignBy = REF_SEEK_BAR,
            constrainBlock = {
                linkTo(parent.start, parent.end, left + CP.normal, right + CP.normal)
            }
        )
        constrain(timeBar) {
            bottom.linkTo(title.top, CP.normal)
            width = Dimension.fillToConstraints
        }
        // Extra-Info
        constrain(REF_EXTRA_INFO) {
            start.linkTo(REF_SEEK_BAR.start)
            bottom.linkTo(REF_SEEK_BAR.top, -CP.medium)
        }
        // more
        val more = horizontal(
            REF_SHUFFLE, REF_REPEAT_MODE, REF_RESIZE_MODE,
            chainStyle = ChainStyle.Packed(1f),
            constrainBlock =  {
                linkTo(timeBar.start, REF_COLLAPSE.end)
            }
        )
        constrain(more) {
            bottom.linkTo(REF_EXTRA_INFO.top)
        }
    }
}
//
private fun LargeVideo(insets: DpRect,   only: Array<String>,) = object : Constraints {
    override val titleTextSize: TextUnit = TITLE_TEXT_SIZE_NORMAL
    override val value: ConstraintSet = ConstraintSet {
        // Background
        constrain(REF_BACKGROUND) {
            linkTo(parent.start, parent.top, parent.end, parent.bottom)
            dimensions = Dimension.fillToConstraints
        }

        // Video Surface
        constrain(REF_VIDEO_SURFACE) {
            linkTo(parent.start, parent.top, parent.end, parent.bottom)
            dimensions = Dimension.wrapContent
        }

        // Collapse
        val (left, up, right, down) = insets
        REF_TITLE.withChainParams(startMargin = CP.medium, endMargin = CP.xLarge)
        REF_COLLAPSE.withChainParams(startMargin = CP.normal)
        val options = horizontal(
            REF_INDICATOR, REF_TITLE, REF_INFO, REF_SHUFFLE, REF_REPEAT_MODE, REF_EQUALIZER, REF_QUEUE, REF_SPEED, REF_SLEEP_TIMER, REF_LIKED, REF_MORE, REF_COLLAPSE,
            alignBy = REF_TITLE,
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
        constrain(REF_SUBTITLE){
            top.linkTo(REF_TITLE.bottom)
            linkTo(REF_TITLE.start, REF_TITLE.end)
            horizontalBias = 0f
            height = Dimension.preferredValue(0.dp)
            width = Dimension.fillToConstraints.atMost(180.dp)
        }

        // TimeBar
        val timeBar = horizontal(
            REF_SKIP_PREVIOUS, REF_PLAY_PAUSE, REF_SKIP_TO_NEXT, REF_SEEK_BAR, REF_ROTATION_LOCK, REF_RESIZE_MODE,
            alignBy = REF_SEEK_BAR,
            constrainBlock = {
                linkTo(parent.start, parent.end, left + CP.normal, right + CP.normal)
            }
        )
        constrain(timeBar) {
            bottom.linkTo(parent.bottom, CP.small + down)
            width = Dimension.fillToConstraints
        }
        // Extra-Info
        constrain(REF_EXTRA_INFO) {
            start.linkTo(REF_SEEK_BAR.start)
            bottom.linkTo(REF_SEEK_BAR.top, -CP.medium)
        }
    }
}
package com.zs.audiofy.console

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstrainedLayoutReference
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.ConstraintSetScope
import androidx.constraintlayout.compose.Dimension
import com.zs.audiofy.common.compose.horizontal
import com.zs.compose.theme.WindowSize
import com.zs.audiofy.common.compose.ContentPadding as CP

private const val TAG = "ConstraintSets"

private val TITLE_TEXT_SIZE_LARGE = 44.sp
private val TITLE_TEXT_SIZE_NORMAL = 16.sp

/**
 * Represents a new ConstraintSet until Compose ConstraintLayout adds the ability to support custom properties
 * directly in [ConstraintSet]. Some properties are private to the constraint configuration and
 * need to be handled in a custom set.
 *
 * This interface defines custom properties for use in layout constraints.
 *
 * @property titleTextSize A custom property representing the size of the title in the layout.
 * @property constraints The original constraints for the layout.
 */
// TODO - Remove [NewConstraintSet] and return only [ConstraintSet] once custom property support is added.
@Stable
class Constraints private constructor(
    val titleTextSize: TextUnit,
    val value: ConstraintSet
) {
    companion object {
        const val ID_CLOSE_BTN = "_close"
        const val ID_ARTWORK = "_artwork"
        const val ID_TITLE = "_title"
        const val ID_SUBTITLE = "_subtitle"
        const val ID_POSITION = "_position"
        const val ID_SHUFFLE = "_shuffle"
        const val ID_REPEAT_MODE = "_repeat_mode"
        const val ID_SKIP_PREVIOUS = "_skip_previous"
        const val ID_PLAY_PAUSE = "_play_pause"
        const val ID_SKIP_TO_NEXT = "_skip_next"
        const val ID_SEEK_BAR = "_seek_bar"
        const val ID_VIDEO_SURFACE = "_video_surface"
        const val ID_MESSAGE = "_message"
        const val ID_BACKGROUND = "_background"
        const val ID_SCRIM = "_scrim"
        const val ID_RESIZE_MODE = "_resize_mode"
        const val ID_ROTATION_LOCK = "_rotation_lock"
        const val ID_QUEUE = "_queue"
        const val ID_SLEEP_TIMER = "_sleep_timer"
        const val ID_SPEED = "_speed"
        const val ID_LIKED = "_liked"
        const val ID_MORE = "_more"

        /**
         * A shorthand for creating instance of [ConstraintSet]
         */
        operator fun invoke(titleTextSize: TextUnit, desc: ConstraintSetScope.() -> Unit) =
            Constraints(titleTextSize, ConstraintSet(desc))

        /**
         * Calculates the appropriate constraint set for the console layout based on window size, content type, and insets.
         *
         * @param windowSize The current size of the window.
         * @param insets The insets of the window (e.g., for status bar or navigation bar).
         * @param isVideo True if the content is video, false otherwise.
         * @param forceOnlyController True to force only the controller to be displayed, regardless of window size.
         * @return The calculated constraint set.
         */
        operator fun invoke(
            windowSize: WindowSize,
            insets: DpRect,
            isVideo: Boolean,
            forceOnlyController: Boolean
        ): Constraints {
            return Portrait(insets)
        }
    }
}

private val REF_CLOSE_BTN = ConstrainedLayoutReference(Constraints.ID_CLOSE_BTN)
private val REF_ARTWORK = ConstrainedLayoutReference(Constraints.ID_ARTWORK)
private val REF_TITLE = ConstrainedLayoutReference(Constraints.ID_TITLE)
private val REF_SUBTITLE = ConstrainedLayoutReference(Constraints.ID_SUBTITLE)
private val REF_POSITION = ConstrainedLayoutReference(Constraints.ID_POSITION)
private val REF_SHUFFLE = ConstrainedLayoutReference(Constraints.ID_SHUFFLE)
private val REF_REPEAT_MODE = ConstrainedLayoutReference(Constraints.ID_REPEAT_MODE)
private val REF_SKIP_PREVIOUS = ConstrainedLayoutReference(Constraints.ID_SKIP_PREVIOUS)
private val REF_PLAY_PAUSE = ConstrainedLayoutReference(Constraints.ID_PLAY_PAUSE)
private val REF_SKIP_TO_NEXT = ConstrainedLayoutReference(Constraints.ID_SKIP_TO_NEXT)
private val REF_SEEK_BAR = ConstrainedLayoutReference(Constraints.ID_SEEK_BAR)
private val REF_VIDEO_SURFACE = ConstrainedLayoutReference(Constraints.ID_VIDEO_SURFACE)
private val REF_MESSAGE = ConstrainedLayoutReference(Constraints.ID_MESSAGE)
private val REF_BACKGROUND = ConstrainedLayoutReference(Constraints.ID_BACKGROUND)
private val REF_SCRIM = ConstrainedLayoutReference(Constraints.ID_SCRIM)
private val REF_RESIZE_MODE = ConstrainedLayoutReference(Constraints.ID_RESIZE_MODE)
private val REF_ROTATION_LOCK = ConstrainedLayoutReference(Constraints.ID_ROTATION_LOCK)
private val REF_QUEUE = ConstrainedLayoutReference(Constraints.ID_QUEUE)
private val REF_SLEEP_TIMER = ConstrainedLayoutReference(Constraints.ID_SLEEP_TIMER)
private val REF_SPEED = ConstrainedLayoutReference(Constraints.ID_SPEED)
private val REF_LIKED = ConstrainedLayoutReference(Constraints.ID_LIKED)
private val REF_MORE = ConstrainedLayoutReference(Constraints.ID_MORE)

// Constraints
private fun Portrait(insets: DpRect) = Constraints(TITLE_TEXT_SIZE_LARGE) {
    // close button
    val (left, up, right, down) = insets

    // Background
    constrain(REF_BACKGROUND) {
        linkTo(parent.start, parent.end)
        linkTo(parent.top, parent.bottom)
        width = Dimension.fillToConstraints
        height = Dimension.fillToConstraints
    }

    // CloseBtn
    constrain(REF_CLOSE_BTN) {
        top.linkTo(parent.top, margin = up + CP.normal)
        end.linkTo(parent.end, margin = CP.normal + right)
    }

    // Artwork
    constrain(REF_ARTWORK) {
        linkTo(parent.start, parent.end, CP.normal + left)
        linkTo(REF_CLOSE_BTN.bottom, REF_SUBTITLE.top, CP.normal)
        width = Dimension.percent(0.8f)
        height = Dimension.ratio("1:1")
    }

    // Now start building from bottom
    // Options
    constrain(REF_QUEUE) {
        bottom.linkTo(parent.bottom, down + CP.normal)
    }
    horizontal(
        REF_QUEUE, REF_SPEED, REF_SLEEP_TIMER, REF_LIKED, REF_MORE,
        constrainBlock = {
            start.linkTo(parent.start, left + CP.xLarge)
            end.linkTo(parent.end, right + CP.xLarge)
        }
    )

    // Controls
    constrain(REF_PLAY_PAUSE) {
        bottom.linkTo(REF_QUEUE.top, CP.normal)
    }
    horizontal(
        REF_SHUFFLE, REF_SKIP_PREVIOUS, REF_PLAY_PAUSE, REF_SKIP_TO_NEXT, REF_REPEAT_MODE,
        alignBy = REF_PLAY_PAUSE,
        constrainBlock = {
            start.linkTo(parent.start, left + CP.normal)
            end.linkTo(parent.end, right + CP.normal)
        }
    )

    // TimeBar
    constrain(REF_SEEK_BAR) {
        bottom.linkTo(REF_PLAY_PAUSE.top, CP.normal)
        width = Dimension.fillToConstraints
        //  height = Dimension.wrapContent
    }
    horizontal(
        REF_RESIZE_MODE, REF_SEEK_BAR, REF_ROTATION_LOCK,
        alignBy = REF_SEEK_BAR,
        constrainBlock = {
            start.linkTo(parent.start, left + CP.xLarge)
            end.linkTo(parent.end, right + CP.xLarge)
        }
    )
    constrain(REF_POSITION) {
        start.linkTo(REF_SEEK_BAR.start)
        bottom.linkTo(REF_SEEK_BAR.top, -CP.medium)
    }
    //
    constrain(REF_TITLE) {
        start.linkTo(parent.start, left + CP.xLarge)
        end.linkTo(parent.end, right + CP.xLarge)
        bottom.linkTo(REF_POSITION.top, CP.normal)
        width = Dimension.fillToConstraints
    }
    constrain(REF_SUBTITLE) {
        start.linkTo(REF_TITLE.start)
        bottom.linkTo(REF_TITLE.top)
        width = Dimension.percent(0.65f)
    }
}
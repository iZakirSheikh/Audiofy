package com.prime.media.console

import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstrainedLayoutReference
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.ConstraintSetScope
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.Visibility
import com.prime.media.core.ContentPadding

// Constraint reference of components.
// Signature Row
private val SIGNATURE = ConstrainedLayoutReference("_signature")
private val CLOSE = ConstrainedLayoutReference("_close")

// Artwork Row
private val ARTWORK = ConstrainedLayoutReference("_artwork")
private val PROGRESS_MILLS = ConstrainedLayoutReference("_progress_mills")
private val DURATION = ConstrainedLayoutReference("_id_duration")

// Title
private val SUBTITLE = ConstrainedLayoutReference("_subtitle")
private val TITLE = ConstrainedLayoutReference("_title")

// Slider
private val HEART = ConstrainedLayoutReference("_heart")
private val SLIDER = ConstrainedLayoutReference("_slider")
private val EQUALIZER = ConstrainedLayoutReference("_equalizer")

// Controls
private val SKIP_TO_PREVIOUS = ConstrainedLayoutReference("_previous")
private val SKIP_BACK_10 = ConstrainedLayoutReference("_skip_back_10")
private val TOGGLE = ConstrainedLayoutReference("_toggle")
private val SKIP_FORWARD_30 = ConstrainedLayoutReference("_skip_forward_30")
private val SKIP_TO_NEXT = ConstrainedLayoutReference("_next")

// Buttons
private val SHUFFLE = ConstrainedLayoutReference("_shuffle")
private val REPEAT = ConstrainedLayoutReference("_repeat")
private val QUEUE = ConstrainedLayoutReference("_queue")
private val SPEED = ConstrainedLayoutReference("_speed")
private val SLEEP = ConstrainedLayoutReference("_sleep")

// Getters on Console.companion
val Console.Companion.SIGNATURE get() = com.prime.media.console.SIGNATURE
val Console.Companion.CLOSE get() = com.prime.media.console.CLOSE
val Console.Companion.ARTWORK get() = com.prime.media.console.ARTWORK
val Console.Companion.PROGRESS_MILLS get() = com.prime.media.console.PROGRESS_MILLS
val Console.Companion.DURATION get() = com.prime.media.console.DURATION
val Console.Companion.SUBTITLE get() = com.prime.media.console.SUBTITLE
val Console.Companion.TITLE get() = com.prime.media.console.TITLE
val Console.Companion.SLIDER get() = com.prime.media.console.SLIDER
val Console.Companion.EQUALIZER get() = com.prime.media.console.EQUALIZER
val Console.Companion.SKIP_TO_PREVIOUS get() = com.prime.media.console.SKIP_TO_PREVIOUS
val Console.Companion.SKIP_BACK_10 get() = com.prime.media.console.SKIP_BACK_10
val Console.Companion.TOGGLE get() = com.prime.media.console.TOGGLE
val Console.Companion.SKIP_FORWARD_30 get() = com.prime.media.console.SKIP_FORWARD_30
val Console.Companion.SKIP_TO_NEXT get() = com.prime.media.console.SKIP_TO_NEXT
val Console.Companion.SHUFFLE get() = com.prime.media.console.SHUFFLE
val Console.Companion.REPEAT get() = com.prime.media.console.REPEAT
val Console.Companion.QUEUE get() = com.prime.media.console.QUEUE
val Console.Companion.SPEED get() = com.prime.media.console.SPEED
val Console.Companion.SLEEP get() = com.prime.media.console.SLEEP
val Console.Companion.HEART get() = com.prime.media.console.HEART

/**
 * A utility fun to hide refs.
 */
context(ConstraintSetScope)
private inline fun hide(
    vararg ref: ConstrainedLayoutReference
) {
    ref.forEach {
        constrain(it) {
            //start.linkTo(parent.start)
            end.linkTo(parent.start)
            top.linkTo(parent.top)
            bottom.linkTo(parent.bottom)
            visibility = Visibility.Gone
        }
    }
}

val Console.Companion.Collapsed get() = collapsed

private val collapsed = ConstraintSet {
    // hide extra
    hide(SIGNATURE, CLOSE)
    hide(SLIDER, EQUALIZER, PROGRESS_MILLS, DURATION)
    hide(SKIP_FORWARD_30, SKIP_TO_NEXT, SKIP_BACK_10, SKIP_TO_PREVIOUS)
    hide(QUEUE, SPEED, SLEEP, SHUFFLE, REPEAT)
    // Artwork
    constrain(ARTWORK) {
        top.linkTo(parent.top)
        bottom.linkTo(parent.bottom)
        start.linkTo(parent.start, ContentPadding.medium)
        height = Dimension.value(56.dp)
        width = Dimension.ratio("1:1")
    }

    // chain vertically title and subtitle.
    createVerticalChain(TITLE, SUBTITLE, chainStyle = ChainStyle.Packed)
    constrain(TITLE) {
        start.linkTo(ARTWORK.end, ContentPadding.medium)
        end.linkTo(HEART.start, ContentPadding.medium)
        width = Dimension.fillToConstraints
    }

    constrain(SUBTITLE) {
        start.linkTo(TITLE.start)
        end.linkTo(TITLE.end)
        width = Dimension.fillToConstraints
        visibility = Visibility.Visible
    }

    constrain(HEART) {
        start.linkTo(TITLE.end)
        top.linkTo(ARTWORK.top)
        bottom.linkTo(ARTWORK.bottom)
    }
    // toggles
    constrain(TOGGLE) {
        start.linkTo(HEART.end)
        end.linkTo(parent.end)
        top.linkTo(ARTWORK.top)
        bottom.linkTo(ARTWORK.bottom)
    }
}

val Console.Companion.Expanded get() = com.prime.media.console.Expanded

private val Expanded = ConstraintSet {
    // signature
    constrain(SIGNATURE) {
        start.linkTo(parent.start, ContentPadding.normal)
        top.linkTo(parent.top, ContentPadding.medium)
    }

    constrain(CLOSE) {
        end.linkTo(parent.end, ContentPadding.normal)
        top.linkTo(SIGNATURE.top)
        bottom.linkTo(SIGNATURE.bottom)
    }

    // artwork
    constrain(ARTWORK) {
        linkTo(parent.start, SIGNATURE.bottom, parent.end, SUBTITLE.top)
        height = Dimension.fillToConstraints
        width = Dimension.ratio("1:1")
    }

    constrain(PROGRESS_MILLS) {
        end.linkTo(ARTWORK.end, 50.dp)
        top.linkTo(ARTWORK.top)
        bottom.linkTo(ARTWORK.bottom)
    }

    //Title
    constrain(SUBTITLE) {
        start.linkTo(TITLE.start)
        bottom.linkTo(TITLE.top)
    }

    constrain(TITLE) {
        bottom.linkTo(SLIDER.top, ContentPadding.normal)
        start.linkTo(parent.start, ContentPadding.xLarge)
        end.linkTo(parent.end, ContentPadding.xLarge)
        width = Dimension.fillToConstraints
    }

    //progressbar
    constrain(SLIDER) {
        bottom.linkTo(TOGGLE.top, ContentPadding.normal)
        start.linkTo(HEART.end, ContentPadding.medium)
        end.linkTo(EQUALIZER.start, ContentPadding.medium)
        width = Dimension.fillToConstraints
    }

    constrain(HEART) {
        top.linkTo(SLIDER.top)
        bottom.linkTo(SLIDER.bottom)
        start.linkTo(TITLE.start)
    }

    constrain(EQUALIZER) {
        top.linkTo(SLIDER.top)
        bottom.linkTo(SLIDER.bottom)
        end.linkTo(TITLE.end)
    }

    // play controls row
    constrain(TOGGLE) {
        start.linkTo(parent.start)
        end.linkTo(parent.end)
        bottom.linkTo(QUEUE.top, ContentPadding.xLarge)
    }

    constrain(SKIP_TO_PREVIOUS) {
        end.linkTo(TOGGLE.start, ContentPadding.normal)
        top.linkTo(TOGGLE.top)
        bottom.linkTo(TOGGLE.bottom)
    }

    constrain(SKIP_BACK_10) {
        end.linkTo(SKIP_TO_PREVIOUS.start, ContentPadding.medium)
        top.linkTo(TOGGLE.top)
        bottom.linkTo(TOGGLE.bottom)
    }

    constrain(SKIP_TO_NEXT) {
        start.linkTo(TOGGLE.end, ContentPadding.normal)
        top.linkTo(TOGGLE.top)
        bottom.linkTo(TOGGLE.bottom)
    }

    constrain(SKIP_FORWARD_30) {
        start.linkTo(SKIP_TO_NEXT.end, ContentPadding.medium)
        top.linkTo(TOGGLE.top)
        bottom.linkTo(TOGGLE.bottom)
    }

    val ref =
        createHorizontalChain(QUEUE, SPEED, SLEEP, SHUFFLE, REPEAT, chainStyle = ChainStyle.Packed)
    constrain(ref) {
        start.linkTo(parent.start, ContentPadding.xLarge)
        end.linkTo(parent.end, ContentPadding.xLarge)
    }

    constrain(QUEUE) {
        bottom.linkTo(parent.bottom, ContentPadding.xLarge)
    }

    constrain(SPEED) {
        bottom.linkTo(QUEUE.bottom)
    }

    constrain(SLEEP) {
        bottom.linkTo(QUEUE.bottom)
    }

    constrain(SHUFFLE) {
        bottom.linkTo(QUEUE.bottom)
    }

    constrain(REPEAT) {
        bottom.linkTo(QUEUE.bottom)
    }
}
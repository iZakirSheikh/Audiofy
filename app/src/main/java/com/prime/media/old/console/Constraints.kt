package com.prime.media.old.console

import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstrainedLayoutReference
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.ConstraintSetScope
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.HorizontalChainReference
import androidx.constraintlayout.compose.HorizontalChainScope
import androidx.constraintlayout.compose.VerticalChainReference
import androidx.constraintlayout.compose.VerticalChainScope
import androidx.constraintlayout.compose.Visibility
import androidx.constraintlayout.compose.atMost
import com.zs.core_ui.ContentPadding
import com.zs.core_ui.Range
import com.zs.core_ui.WindowSize

private const val TAG = "ConstraintSets"

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
interface Constraints {
    val titleTextSize: TextUnit
    val value: ConstraintSet

    companion object {
        const val ID_SIGNATURE = "_signature"
        const val ID_CLOSE_BTN = "_close"
        const val ID_ARTWORK = "_artwork"
        const val ID_TITLE = "_title"
        const val ID_POSITION = "_position"
        const val ID_SUBTITLE = "_subtitle"
        const val ID_CONTROLS = "_controls"
        const val ID_TIME_BAR = "_time_bar"
        const val ID_OPTIONS = "_options"
        const val ID_VIDEO_SURFACE = "_video_surface"
        const val ID_MESSAGE = "_message"
        const val ID_BACKGROUND = "_background"
        const val ID_SCRIM = "_scrim"
    }
}

private val REF_SIGNATURE = ConstrainedLayoutReference(Constraints.ID_SIGNATURE)
private val REF_CLOSE_BTN = ConstrainedLayoutReference(Constraints.ID_CLOSE_BTN)
private val REF_ARTWORK = ConstrainedLayoutReference(Constraints.ID_ARTWORK)
private val REF_TITLE = ConstrainedLayoutReference(Constraints.ID_TITLE)
private val REF_POSITION = ConstrainedLayoutReference(Constraints.ID_POSITION)
private val REF_SUBTITLE = ConstrainedLayoutReference(Constraints.ID_SUBTITLE)
private val REF_CONTROLS = ConstrainedLayoutReference(Constraints.ID_CONTROLS)
private val REF_TIME_BAR = ConstrainedLayoutReference(Constraints.ID_TIME_BAR)
private val REF_OPTIONS = ConstrainedLayoutReference(Constraints.ID_OPTIONS)
private val REF_MESSAGE = ConstrainedLayoutReference(Constraints.ID_MESSAGE)
private val REF_VIDEO_SURFACE = ConstrainedLayoutReference(Constraints.ID_VIDEO_SURFACE)
private val REF_BACKGROUND = ConstrainedLayoutReference(Constraints.ID_BACKGROUND)
private val REF_SCRIM = ConstrainedLayoutReference(Constraints.ID_SCRIM)


/**
 * A shorthand method to create a horizontal chain.
 */
private fun ConstraintSetScope.horizontal(
    vararg elements: ConstrainedLayoutReference,
    chainStyle: ChainStyle = ChainStyle.Spread,
    constrainBlock: HorizontalChainScope.() -> Unit
): HorizontalChainReference {
    val chain = createHorizontalChain(*elements, chainStyle = chainStyle)
    constrain(chain, constrainBlock)
    return chain
}

/**
 * @see horizontal
 */
private fun ConstraintSetScope.vertical(
    vararg elements: ConstrainedLayoutReference,
    chainStyle: ChainStyle = ChainStyle.Spread,
    constrainBlock: VerticalChainScope.() -> Unit
): VerticalChainReference {
    val chain = createVerticalChain(*elements, chainStyle = chainStyle)
    constrain(chain, constrainBlock)
    return chain
}

private fun ConstraintSetScope.hide(
    vararg elements: ConstrainedLayoutReference,
) {
    for (element in elements)
        constrain(element) {
            visibility = Visibility.Gone
            // TODO - Position these so as to get a nice animation when shown again.
        }
}

/**
 * Creates a new [Constraints] with custom properties, including the title text size,
 * and original constraints defined in the [description] block.
 *
 * @param titleTextSize The size of the title in the layout.
 * @param description A lambda block to define constraints using [ConstraintSetScope].
 *
 * @return A new [Constraints] instance.
 */
private fun ConstraintSet(
    titleTextSize: TextUnit,
    description: ConstraintSetScope.() -> Unit
) = object : Constraints {
    override val titleTextSize: TextUnit
        get() = titleTextSize
    override val value: ConstraintSet = ConstraintSet(description)
}

private val TITLE_TEXT_SIZE_LARGE = 44.sp
private val TITLE_TEXT_SIZE_NORMAL = 16.sp

/**
 * A constraint set that configures the layout to display only the video surface,
 * excluding other UI elements.
 */
private val onlySurface =
    ConstraintSet(TITLE_TEXT_SIZE_NORMAL) {
        // Hide the controllers and just show the video surface.
        hide(
            REF_SCRIM,
            REF_CLOSE_BTN,
            REF_SIGNATURE,
            REF_ARTWORK,
            REF_TITLE,
            REF_SUBTITLE,
            REF_POSITION,
            REF_CONTROLS,
            REF_OPTIONS,
            REF_TIME_BAR
        )

        constrain(REF_MESSAGE){
            linkTo(parent.start, parent.end)
            top.linkTo(parent.top, ContentPadding.xLarge)
        }

        constrain(REF_BACKGROUND) {
            linkTo(parent.start, parent.top, parent.end, parent.bottom)
            width = Dimension.fillToConstraints
            height = Dimension.fillToConstraints
        }

        constrain(REF_VIDEO_SURFACE) {
            linkTo(parent.start, parent.top, parent.end, parent.bottom)
            width = Dimension.fillToConstraints
            height = Dimension.fillToConstraints
        }
    }

/**
 * Defines the layout for the compact window mode.
 * The compact layout is designed for smaller window dimensions, such as when the window height
 * is between 320dp and 500dp, and the width is between 450dp and 500dp.
 *
 * @param insets The system window insets to account for padding around the compact layout.
 */
private fun Compact(
    insets: DpRect,
    compact: Boolean,
) = ConstraintSet(if (compact) TITLE_TEXT_SIZE_NORMAL else TITLE_TEXT_SIZE_LARGE) {
    // De-structure insets
    val (left, up, right, down) = insets

    constrain(REF_MESSAGE){
        linkTo(parent.start, parent.end)
        bottom.linkTo(parent.bottom, ContentPadding.medium)
    }

    // Set the visibility of Sg=ignature to gone.
    constrain(REF_SIGNATURE) {
        visibility = Visibility.Gone
    }

    constrain(REF_SCRIM) {
        linkTo(parent.start, parent.top, parent.end, parent.bottom)
        width = Dimension.fillToConstraints
        height = Dimension.fillToConstraints
        visibility = Visibility.Gone
    }

    // The background
    constrain(REF_BACKGROUND) {
        linkTo(parent.start, parent.top, parent.end, parent.bottom)
        width = Dimension.fillToConstraints
        height = Dimension.fillToConstraints
    }

    // Video Surface
    constrain(REF_VIDEO_SURFACE) {
        linkTo(parent.start, parent.top, parent.end, parent.bottom)
        width = Dimension.fillToConstraints
        height = Dimension.fillToConstraints
        visibility = Visibility.Gone
    }

    // TopRow Constitutes Close button and options
    constrain(REF_CLOSE_BTN) {
        start.linkTo(parent.start, ContentPadding.medium + left)
        top.linkTo(parent.top, ContentPadding.medium + up)
    }

    constrain(REF_OPTIONS) {
        end.linkTo(parent.end, ContentPadding.medium + right)
        top.linkTo(REF_CLOSE_BTN.top)
        bottom.linkTo(REF_CLOSE_BTN.bottom)
    }

    horizontal(
        REF_ARTWORK,
        REF_TITLE,
        chainStyle = ChainStyle.Packed(0f),
        constrainBlock = {
            start.linkTo(parent.start, ContentPadding.medium + left)
            end.linkTo(parent.end, ContentPadding.medium + right)
        }
    )

    constrain(REF_ARTWORK) {
        top.linkTo(REF_CLOSE_BTN.bottom, ContentPadding.medium)
        bottom.linkTo(REF_CONTROLS.top, ContentPadding.medium)
        width = Dimension.percent(0.3f)
        height = Dimension.ratio(if (compact) "1:1" else "0.8")
        val scale = if (compact) 0.6f else 0.7f
        scaleY = scale
        scaleX = scale
    }

    vertical(
        REF_POSITION,
        REF_TITLE,
        REF_SUBTITLE,
        chainStyle = ChainStyle.Packed(if (compact) 0.5f else 0.4f),
        constrainBlock = {
            top.linkTo(REF_ARTWORK.top)
            bottom.linkTo(REF_ARTWORK.bottom)
        }
    )

    constrain(REF_TITLE) {
        width = Dimension.fillToConstraints
    }

    constrain(REF_SUBTITLE) {
        start.linkTo(REF_TITLE.start)
    }

    constrain(REF_POSITION) {
        start.linkTo(REF_TITLE.start)
    }

    constrain(REF_CONTROLS) {
        start.linkTo(parent.start, ContentPadding.medium + left)
        end.linkTo(parent.end, ContentPadding.medium + right)
        bottom.linkTo(
            if (!compact) REF_TIME_BAR.top else parent.bottom,
            if (!compact) ContentPadding.medium else down + ContentPadding.medium
        )
    }

    constrain(REF_TIME_BAR) {
        start.linkTo(parent.start, ContentPadding.medium + left)
        end.linkTo(parent.end, ContentPadding.medium + right)
        bottom.linkTo(parent.bottom, ContentPadding.normal + down)
        width = Dimension.fillToConstraints
        visibility = if (compact) Visibility.Gone else Visibility.Visible
    }
}

/**
 * Defines the portrait layout for windows, primarily targeting mobile phones.
 * The portrait layout is designed for windows where the width is greater than the compact threshold,
 * and the height is at least 1.5 times the width.
 *
 * @param insets System window insets to account for padding around the portrait layout.
 * @param compact If true, non-essential elements like signature are dropped to fit in smaller screens.
 *
 * TODO: Maybe implement scaling up for larger screens.
 *
 * @return A [Constraints] instance representing the portrait layout constraints.
 */
private fun Portrait(
    insets: DpRect,
    compact: Boolean
) = ConstraintSet(TITLE_TEXT_SIZE_LARGE) {
    val (left, up, right, down) = insets

    constrain(REF_MESSAGE){
        linkTo(parent.start, parent.end)
        top.linkTo(REF_TIME_BAR.bottom, ContentPadding.medium)
    }

    constrain(REF_SCRIM) {
        linkTo(parent.start, parent.top, parent.end, parent.bottom)
        width = Dimension.fillToConstraints
        height = Dimension.fillToConstraints
        visibility = Visibility.Gone
    }

    horizontal(
        REF_SIGNATURE,
        REF_CLOSE_BTN,
        chainStyle = ChainStyle.SpreadInside,
        constrainBlock = {
            start.linkTo(parent.start, ContentPadding.normal + left)
            end.linkTo(parent.end, ContentPadding.normal + right)
        }
    )
    constrain(REF_SIGNATURE) {
        top.linkTo(parent.top, ContentPadding.normal + up)
        visibility = if (compact) Visibility.Gone else Visibility.Visible
    }

    constrain(REF_CLOSE_BTN) {
        top.linkTo(REF_SIGNATURE.top)
        bottom.linkTo(REF_SIGNATURE.bottom)
        visibility = if (compact) Visibility.Gone else Visibility.Visible
    }

    // Artwork Row
    constrain(REF_ARTWORK) {
        start.linkTo(parent.start, ContentPadding.normal + left)
        end.linkTo(parent.end, ContentPadding.normal + right)
        top.linkTo(REF_SIGNATURE.bottom, up)
        bottom.linkTo(REF_POSITION.top)
        width = Dimension.percent(0.8f)
        height = Dimension.ratio("1:1")
    }

    vertical(
        REF_TITLE,
        REF_TIME_BAR,
        REF_CONTROLS,
        REF_OPTIONS,
        chainStyle = ChainStyle.Spread,
        constrainBlock = {
            top.linkTo(REF_ARTWORK.bottom, ContentPadding.normal)
            bottom.linkTo(parent.bottom, down + ContentPadding.normal)
        }
    )


    // Title
    constrain(REF_TITLE) {
        start.linkTo(parent.start, left + ContentPadding.xLarge)
        end.linkTo(parent.end, right + ContentPadding.xLarge)
        width = Dimension.fillToConstraints

    }
    // Position + Subtitle
    constrain(REF_POSITION) {
        start.linkTo(REF_TITLE.start)
        bottom.linkTo(REF_TITLE.top)
        width = Dimension.wrapContent
    }

    constrain(REF_SUBTITLE) {
        start.linkTo(REF_POSITION.end, 4.dp)
        bottom.linkTo(REF_POSITION.bottom)
        end.linkTo(REF_TITLE.end)
        width = Dimension.fillToConstraints
    }

    constrain(REF_TIME_BAR) {
        start.linkTo(parent.start, left + ContentPadding.xLarge)
        end.linkTo(parent.end, right + ContentPadding.xLarge)
        width = Dimension.fillToConstraints
        top.linkTo(REF_ARTWORK.bottom, ContentPadding.normal)
    }

    // Controls
    constrain(REF_CONTROLS) {
        start.linkTo(parent.start, left + ContentPadding.normal)
        end.linkTo(parent.end, right + ContentPadding.normal)
        top.linkTo(REF_TIME_BAR.bottom, ContentPadding.normal)
    }

    // Options
    constrain(REF_OPTIONS) {
        start.linkTo(parent.start, left + ContentPadding.xLarge)
        end.linkTo(parent.end, right + ContentPadding.xLarge)
    }

    constrain(REF_BACKGROUND) {
        linkTo(parent.start, parent.top, parent.end, parent.bottom)
        width = Dimension.fillToConstraints
        height = Dimension.fillToConstraints
    }

    constrain(REF_VIDEO_SURFACE) {
        linkTo(parent.start, parent.top, parent.end, parent.bottom)
        width = Dimension.fillToConstraints
        height = Dimension.fillToConstraints
        visibility = Visibility.Gone
    }
}

/**
 * @see portrait
 */
private fun Landscape(
    insets: DpRect,
    compact: Boolean
) = ConstraintSet(TITLE_TEXT_SIZE_LARGE) {

    val (left, up, right, down) = insets

    constrain(REF_MESSAGE){
        linkTo(parent.start, parent.end)
        top.linkTo(REF_TIME_BAR.bottom, ContentPadding.medium)
    }

    constrain(REF_SCRIM) {
        linkTo(parent.start, parent.top, parent.end, parent.bottom)
        width = Dimension.fillToConstraints
        height = Dimension.fillToConstraints
        visibility = Visibility.Gone
    }

    constrain(REF_SIGNATURE) {
        // Will be set to visible in future releases.
        visibility = if (compact) Visibility.Gone else Visibility.Invisible
        end.linkTo(REF_CLOSE_BTN.end)
    }

    vertical(
        REF_CLOSE_BTN,
        REF_SIGNATURE,
        chainStyle = ChainStyle.SpreadInside,
        constrainBlock = {
            top.linkTo(parent.top, ContentPadding.normal + up)
            bottom.linkTo(parent.bottom, ContentPadding.normal + down)
        }
    )

    constrain(REF_CLOSE_BTN) {
        start.linkTo(parent.start, ContentPadding.normal + left)
        visibility = if (compact) Visibility.Gone else Visibility.Visible
    }

    horizontal(
        REF_ARTWORK,
        REF_TITLE,
        chainStyle = ChainStyle.Spread,
        constrainBlock = {
            start.linkTo(REF_CLOSE_BTN.end, ContentPadding.small)
            end.linkTo(parent.end, ContentPadding.xLarge + right)
        }
    )

    // Artwork Row
    constrain(REF_ARTWORK) {
        top.linkTo(parent.top, ContentPadding.normal + up)
        // Make artwork middle only if not compact
        if (!compact)
            bottom.linkTo(parent.bottom, ContentPadding.normal + down)
        width = Dimension.percent((if (compact) 0.7f else 0.9f) * 0.35f)
        height = Dimension.ratio("1:1")
       // scaleY =
      //  scaleX = if (compact) 0.7f else 0.9f
    }

    constrain(REF_TITLE) {
        start.linkTo(REF_ARTWORK.end, ContentPadding.large)
        end.linkTo(REF_CLOSE_BTN.start, ContentPadding.normal)
        top.linkTo(REF_ARTWORK.top, ContentPadding.xLarge)
        width = Dimension.fillToConstraints
    }

    constrain(REF_POSITION) {
        start.linkTo(REF_TITLE.start)
        bottom.linkTo(REF_TITLE.top)
    }

    constrain(REF_SUBTITLE) {
        start.linkTo(REF_POSITION.end, 2.dp)
        bottom.linkTo(REF_POSITION.bottom)
        end.linkTo(REF_TITLE.end, ContentPadding.xLarge)
        width = Dimension.fillToConstraints
    }

    constrain(REF_CONTROLS) {
        end.linkTo(REF_TITLE.end)
        start.linkTo(REF_TITLE.start)
        top.linkTo(REF_TITLE.bottom, ContentPadding.normal)
    }
    constrain(REF_TIME_BAR) {
        end.linkTo(REF_TITLE.end)
        start.linkTo(REF_TITLE.start)
        top.linkTo(REF_CONTROLS.bottom, ContentPadding.normal)
        width = Dimension.fillToConstraints
    }

    constrain(REF_OPTIONS) {
        end.linkTo(REF_TIME_BAR.end)
        top.linkTo(REF_TIME_BAR.bottom)
    }

    constrain(REF_BACKGROUND) {
        linkTo(parent.start, parent.top, parent.end, parent.bottom)
        width = Dimension.fillToConstraints
        height = Dimension.fillToConstraints
    }

    constrain(REF_VIDEO_SURFACE) {
        linkTo(parent.start, parent.top, parent.end, parent.bottom)
        width = Dimension.fillToConstraints
        height = Dimension.fillToConstraints
        visibility = Visibility.Gone
    }
}

/**
 * Defines the layout for a medium-sized window, typically falling between the medium and large size classes.
 * This layout is suitable for windows with dimensions greater than the medium threshold.
 *
 * @param insets System window insets to account for padding around the medium layout.
 *
 * @return A [Constraints] instance representing the constraints for the medium-sized window layout.
 */
private fun Medium(
    insets: DpRect
) = ConstraintSet(TITLE_TEXT_SIZE_NORMAL) {
    val (left, up, right, down) = insets

    constrain(REF_MESSAGE){
        linkTo(parent.start, parent.end)
        top.linkTo(REF_TIME_BAR.bottom, ContentPadding.medium)
    }

    constrain(REF_SCRIM) {
        linkTo(parent.start, parent.top, parent.end, parent.bottom)
        width = Dimension.fillToConstraints
        height = Dimension.fillToConstraints
        visibility = Visibility.Gone
    }

    // This Config of controller doesnt support signature
    constrain(REF_SIGNATURE) {
        visibility = Visibility.Gone
    }

    constrain(REF_CLOSE_BTN) {
        end.linkTo(parent.end, ContentPadding.normal + right)
        top.linkTo(parent.top, ContentPadding.large + up)
    }

    // Artwork Row
    constrain(REF_ARTWORK) {
        start.linkTo(parent.start, ContentPadding.large)
        end.linkTo(parent.end, ContentPadding.large)
        top.linkTo(REF_CLOSE_BTN.bottom, ContentPadding.normal)
        bottom.linkTo(REF_OPTIONS.top, ContentPadding.normal)
        width = Dimension.fillToConstraints.atMost(420.dp * 0.8f)
        height = Dimension.ratio("1:1")
       // scaleY = 0.8f
      //  scaleX = 0.8f
    }

    constrain(REF_TIME_BAR) {
        start.linkTo(parent.start, left + ContentPadding.xLarge)
        end.linkTo(parent.end, right + ContentPadding.xLarge)
        bottom.linkTo(REF_CONTROLS.top, ContentPadding.medium)
        width = Dimension.fillToConstraints
    }

    constrain(REF_CONTROLS) {
        bottom.linkTo(parent.bottom, down + ContentPadding.normal)
        horizontalChainWeight = 0.3f
    }

    constrain(REF_POSITION) {
        start.linkTo(REF_TIME_BAR.start, ContentPadding.normal)
        bottom.linkTo(REF_TIME_BAR.top)
    }

    horizontal(
        REF_TITLE,
        REF_CONTROLS,
        chainStyle = ChainStyle.Packed,
        constrainBlock = {
            start.linkTo(REF_TIME_BAR.start)
            end.linkTo(REF_TIME_BAR.end)
        }
    )

    constrain(REF_TITLE) {
        width = Dimension.fillToConstraints
        top.linkTo(REF_CONTROLS.top)
        bottom.linkTo(REF_CONTROLS.bottom)
        horizontalChainWeight = 0.3f
    }

    constrain(REF_SUBTITLE) {
        bottom.linkTo(REF_TITLE.top)
        start.linkTo(REF_TITLE.start)
    }

    constrain(REF_OPTIONS) {
        end.linkTo(parent.end, right + ContentPadding.xLarge)
        bottom.linkTo(REF_TIME_BAR.top)
    }

    constrain(REF_BACKGROUND) {
        linkTo(parent.start, parent.top, parent.end, parent.bottom)
        width = Dimension.fillToConstraints
        height = Dimension.fillToConstraints
    }

    constrain(REF_VIDEO_SURFACE) {
        linkTo(parent.start, parent.top, parent.end, parent.bottom)
        width = Dimension.fillToConstraints
        height = Dimension.fillToConstraints
        visibility = Visibility.Gone
    }
}

/**
 * @see Medium
 */
private fun Large(
    insets: DpRect
) = ConstraintSet(TITLE_TEXT_SIZE_NORMAL) {
    val (left, up, right, down) = insets

    constrain(REF_MESSAGE){
        linkTo(parent.start, parent.end)
        top.linkTo(REF_TIME_BAR.bottom, ContentPadding.medium)
    }

    constrain(REF_SCRIM) {
        linkTo(parent.start, parent.top, parent.end, parent.bottom)
        width = Dimension.fillToConstraints
        height = Dimension.fillToConstraints
        visibility = Visibility.Gone
    }

    // This Config of controller doesnt support signature
    constrain(REF_SIGNATURE) {
        visibility = Visibility.Gone
    }

    constrain(REF_CLOSE_BTN) {
        end.linkTo(parent.end, ContentPadding.normal + right)
        top.linkTo(parent.top, ContentPadding.large + up)
    }

    // Artwork Row
    constrain(REF_ARTWORK) {
        start.linkTo(parent.start, ContentPadding.normal)
        //end.linkTo(parent.end, ContentPadding.normal)
        // top.linkTo(REF_CLOSE_BTN.bottom)
        bottom.linkTo(REF_TIME_BAR.top)
        width = Dimension.fillToConstraints.atMost(420.dp * 0.8f)
        height = Dimension.ratio("1:1")
//        scaleY = 0.8f
//        scaleX = 0.8f
    }

    constrain(REF_TIME_BAR) {
        start.linkTo(parent.start, left + ContentPadding.xLarge)
        end.linkTo(parent.end, right + ContentPadding.xLarge)
        bottom.linkTo(REF_CONTROLS.top, ContentPadding.medium)
        width = Dimension.fillToConstraints
    }


    constrain(REF_POSITION) {
        start.linkTo(REF_TIME_BAR.start, ContentPadding.normal)
        bottom.linkTo(REF_TIME_BAR.top)
    }

    horizontal(
        REF_TITLE,
        REF_CONTROLS,
        REF_OPTIONS,
        chainStyle = ChainStyle.SpreadInside,
        constrainBlock = {
            start.linkTo(REF_TIME_BAR.start, ContentPadding.xLarge)
            end.linkTo(REF_TIME_BAR.end)
        }
    )

    constrain(REF_CONTROLS) {
        bottom.linkTo(parent.bottom, down + ContentPadding.normal)
    }

    constrain(REF_TITLE) {
        top.linkTo(REF_CONTROLS.top)
        bottom.linkTo(REF_CONTROLS.bottom)
    }

    constrain(REF_SUBTITLE) {
        bottom.linkTo(REF_TITLE.top)
        start.linkTo(REF_TITLE.start)
    }

    constrain(REF_OPTIONS) {
        top.linkTo(REF_CONTROLS.top)
        bottom.linkTo(REF_CONTROLS.bottom)
    }

    constrain(REF_BACKGROUND) {
        linkTo(parent.start, parent.top, parent.end, parent.bottom)
        width = Dimension.fillToConstraints
        height = Dimension.fillToConstraints
    }

    constrain(REF_VIDEO_SURFACE) {
        linkTo(parent.start, parent.top, parent.end, parent.bottom)
        width = Dimension.fillToConstraints
        height = Dimension.fillToConstraints
        visibility = Visibility.Gone
    }
}

/**
 * Defines a special layout (for videos) for portrait mode when the window width is considered compact.
 * This layout is suitable for scenarios where the window is in portrait mode and has a compact width.
 * No specific height requirement is imposed in this layout.
 *
 * @param insets System window insets to account for padding around the video portrait layout.
 *
 * @return A [Constraints] instance representing the constraints for the video portrait layout.
 */
private fun VideoPortrait(
    insets: DpRect
): Constraints = ConstraintSet(TITLE_TEXT_SIZE_NORMAL) {
    val (left, up, right, down) = insets

    constrain(REF_SCRIM) {
        linkTo(parent.start, parent.top, parent.end, parent.bottom)
        width = Dimension.fillToConstraints
        height = Dimension.fillToConstraints
    }

    constrain(REF_SIGNATURE) {
        visibility = Visibility.Gone
    }
    constrain(REF_ARTWORK) {
        visibility = Visibility.Gone
    }

    horizontal(
        REF_TITLE,
        REF_CLOSE_BTN,
        chainStyle = ChainStyle.SpreadInside,
        constrainBlock = {
            start.linkTo(parent.start, ContentPadding.large + left)
            end.linkTo(parent.end, ContentPadding.large + right)

        }
    )

    constrain(REF_TITLE) {
        top.linkTo(REF_CLOSE_BTN.top)
        bottom.linkTo(REF_CLOSE_BTN.bottom)
        horizontalChainWeight = 1f
        width = Dimension.fillToConstraints
    }

    constrain(REF_SUBTITLE) {
        top.linkTo(REF_TITLE.bottom)
        start.linkTo(REF_TITLE.start)
    }

    constrain(REF_CLOSE_BTN){
        top.linkTo(parent.top, up + ContentPadding.normal)
    }

    constrain(REF_CONTROLS) {
        linkTo(REF_TIME_BAR.start, parent.top, REF_TIME_BAR.end, parent.bottom)
    }

    constrain(REF_OPTIONS) {
        end.linkTo(REF_TIME_BAR.end)
        bottom.linkTo(REF_TIME_BAR.top)
    }

    constrain(REF_TIME_BAR) {
        start.linkTo(parent.start, ContentPadding.normal + left)
        end.linkTo(parent.end, ContentPadding.normal + right)
        bottom.linkTo(REF_POSITION.top)
        width = Dimension.fillToConstraints
    }
    constrain(REF_POSITION) {
        start.linkTo(REF_TIME_BAR.start, ContentPadding.normal)
        bottom.linkTo(parent.bottom, ContentPadding.normal + down)
    }

    constrain(REF_MESSAGE){
        linkTo(parent.start, parent.end)
        top.linkTo(parent.top, 150.dp)
    }
}

/**
 * @see VideoPortrait
 */
private fun VideoLandscape(
    insets: DpRect
) = ConstraintSet(TITLE_TEXT_SIZE_NORMAL) {

    constrain(REF_MESSAGE){
        linkTo(parent.start, parent.top, parent.end, parent.bottom, topMargin = -130.dp)
    }

    constrain(REF_SCRIM) {
        linkTo(parent.start, parent.top, parent.end, parent.bottom)
        width = Dimension.fillToConstraints
        height = Dimension.fillToConstraints
    }

    constrain(REF_SIGNATURE) {
        visibility = Visibility.Gone
    }
    constrain(REF_SUBTITLE) {
        visibility = Visibility.Gone
    }
    constrain(REF_ARTWORK) {
        visibility = Visibility.Gone
    }

    val (left, up, right, down) = insets
   horizontal(
        REF_CLOSE_BTN,
        REF_TITLE,
        REF_OPTIONS,
        chainStyle = ChainStyle.SpreadInside,
        constrainBlock = {
            start.linkTo(parent.start, ContentPadding.medium + right)
            end.linkTo(parent.end, ContentPadding.medium + left)
        }
    )

    constrain(REF_TITLE) {
        top.linkTo(REF_CLOSE_BTN.top)
        bottom.linkTo(REF_CLOSE_BTN.bottom)
        horizontalChainWeight = 1f
        width = Dimension.fillToConstraints
    }

    constrain(REF_OPTIONS) {
        top.linkTo(REF_CLOSE_BTN.top)
        bottom.linkTo(REF_CLOSE_BTN.bottom)
    }

    vertical(
        REF_CLOSE_BTN,
        REF_CONTROLS,
        REF_POSITION,
        REF_TIME_BAR,
        chainStyle = ChainStyle.SpreadInside,
        constrainBlock = {
            top.linkTo(parent.top, ContentPadding.medium + up)
            bottom.linkTo(parent.bottom, ContentPadding.medium + down)
        }
    )

    constrain(REF_CONTROLS) {
        start.linkTo(REF_TIME_BAR.start)
        end.linkTo(REF_TIME_BAR.end)
        verticalChainWeight = 1f
        height = Dimension.fillToConstraints
    }

    constrain(REF_POSITION) {
        start.linkTo(REF_TIME_BAR.start, ContentPadding.normal)
    }
    constrain(REF_TIME_BAR) {
        start.linkTo(parent.start, ContentPadding.xLarge + left)
        end.linkTo(parent.end, ContentPadding.xLarge + right)
        width = Dimension.fillToConstraints
    }

    constrain(REF_BACKGROUND) {
        linkTo(parent.start, parent.top, parent.end, parent.bottom)
        width = Dimension.fillToConstraints
        height = Dimension.fillToConstraints
    }

    constrain(REF_VIDEO_SURFACE) {
        linkTo(parent.start, parent.top, parent.end, parent.bottom)
        width = Dimension.fillToConstraints
        height = Dimension.fillToConstraints
    }
}


/**
 * Calculates the appropriate constraint set for the console layout based on window size, content type, and insets.
 *
 * @param windowSize The current size of the window.
 * @param insets The insets of the window (e.g., for status bar or navigation bar).
 * @param isVideo True if the content is video, false otherwise.
 * @param forceOnlyController True to force only the controller to be displayed, regardless of window size.
 * @return The calculated constraint set.
 */
fun calculateConstraintSet(
    windowSize: WindowSize,
    insets: DpRect,
    isVideo: Boolean,
    forceOnlyController: Boolean,
): Constraints {

    // Destructure the width and height from the WindowSize object
    val (wReach, hReach) = windowSize
    val (width, height) = windowSize.value

    return when {
        // Force only the controller to be displayed
        forceOnlyController -> onlySurface

        // Handle simple video layouts
        isVideo -> {
            // Use VideoPortrait layout if height is at least 30% more than width
            if (height - width > height * 0.3f) VideoPortrait(insets)
            else VideoLandscape(insets) // Otherwise, use VideoLandscape layout
        }

        // Handle audio layouts based on window size
        wReach == Range.Compact && hReach == Range.Compact -> Compact(
            insets,
            height < 300.dp
        )    // Compact audio layout
        hReach > Range.Medium && wReach > Range.Medium -> Large(insets)          // Large audio layout
        hReach > Range.Compact && wReach > Range.Compact -> Medium(insets)        // Medium audio layout

        // Handle portrait and landscape layouts
        wReach < hReach -> Portrait(
            insets,
            height < 600.dp
        )   // Portrait layout (considering height constraint)
        else -> Landscape(
            insets,
            width < 650.dp
        )               // Landscape layout (considering width constraint)
    }
}

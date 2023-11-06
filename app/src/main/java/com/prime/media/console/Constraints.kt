package com.prime.media.console

import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ChainStyle
import androidx.constraintlayout.compose.ConstrainedLayoutReference
import androidx.constraintlayout.compose.ConstraintSet
import androidx.constraintlayout.compose.ConstraintSetScope
import androidx.constraintlayout.compose.Dimension
import androidx.constraintlayout.compose.HorizontalChainScope
import androidx.constraintlayout.compose.Visibility
import com.prime.media.R
import com.prime.media.core.ContentPadding

private val SIGNATURE = ConstrainedLayoutReference(R.id.np_signature)
private val CLOSE = ConstrainedLayoutReference(R.id.np_close)
private val ARTWORK = ConstrainedLayoutReference(R.id.np_artwork)
private val TIMER = ConstrainedLayoutReference(R.id.np_timer)
private val SUBTITLE = ConstrainedLayoutReference(R.id.np_subtitle)
private val TITLE = ConstrainedLayoutReference(R.id.np_title)
private val OPTION_0 = ConstrainedLayoutReference(R.id.np_option_0)
private val OPTION_1 = ConstrainedLayoutReference(R.id.np_option_1)
private val OPTION_2 = ConstrainedLayoutReference(R.id.np_option_2)
private val OPTION_3 = ConstrainedLayoutReference(R.id.np_option_3)
private val OPTION_4 = ConstrainedLayoutReference(R.id.np_option_4)
private val OPTION_5 = ConstrainedLayoutReference(R.id.np_option_5)
private val OPTION_6 = ConstrainedLayoutReference(R.id.np_option_6)
private val SEEK_BACK_10 =ConstrainedLayoutReference(R.id.np_seek_back_10)
private val SEEK_FORWARD_30 =ConstrainedLayoutReference(R.id.np_seek_forward_30)
private val SKIP_TO_PREV =ConstrainedLayoutReference(R.id.np_skip_to_prev)
private val SKIP_TO_NEXT =ConstrainedLayoutReference(R.id.np_skip_to_next)
private val PLAY_TOGGLE =ConstrainedLayoutReference(R.id.np_play_toggle)
private val SLIDER = ConstrainedLayoutReference(R.id.np_slider)


/**
 * A shorthand method to create a horizontal chain.
 */
private fun ConstraintSetScope.constrain(
    vararg elements: ConstrainedLayoutReference,
    chainStyle: ChainStyle = ChainStyle.Spread,
    constrainBlock: HorizontalChainScope.() -> Unit
) = constrain(createHorizontalChain(*elements, chainStyle = chainStyle), constrainBlock)

val Console.Companion.AudioPortraitConstraintSet get() = com.prime.media.console.AudioPortraitConstraintSet
private val AudioPortraitConstraintSet = ConstraintSet {
    // Signature row
    constrain(SIGNATURE, CLOSE, chainStyle = ChainStyle.SpreadInside){
        start.linkTo(parent.start, ContentPadding.normal)
        end.linkTo(parent.end, ContentPadding.normal)
    }

    constrain(SIGNATURE){
        top.linkTo(parent.top)
    }

    constrain(CLOSE){
        top.linkTo(SIGNATURE.top)
        bottom.linkTo(SIGNATURE.bottom)
    }

    // Artwork Row
    constrain(ARTWORK){
        start.linkTo(parent.start, ContentPadding.normal)
        end.linkTo(parent.end, ContentPadding.normal)
        top.linkTo(SIGNATURE.bottom)
        bottom.linkTo(TIMER.top)
        width = Dimension.fillToConstraints
        height = Dimension.ratio("1:1")
    }

    //options
    constrain(OPTION_2, OPTION_3, OPTION_4, OPTION_5, OPTION_6, chainStyle = ChainStyle.Packed){
        start.linkTo(parent.start, ContentPadding.normal)
        end.linkTo(parent.end, ContentPadding.normal)
    }
    constrain(OPTION_2){
        bottom.linkTo(parent.bottom)
    }
    constrain(OPTION_3){
        top.linkTo(OPTION_2.top)
        bottom.linkTo(OPTION_2.bottom)
    }
    constrain(OPTION_4){
        top.linkTo(OPTION_2.top)
        bottom.linkTo(OPTION_2.bottom)
    }
    constrain(OPTION_5){
        top.linkTo(OPTION_2.top)
        bottom.linkTo(OPTION_2.bottom)
    }
    constrain(OPTION_6){
        top.linkTo(OPTION_2.top)
        bottom.linkTo(OPTION_2.bottom)
    }

    // Playback Controls
    constrain(SEEK_BACK_10, SKIP_TO_PREV, PLAY_TOGGLE, SKIP_TO_NEXT, SEEK_FORWARD_30, chainStyle = ChainStyle.Packed){
        start.linkTo(parent.start, ContentPadding.normal)
        end.linkTo(parent.end, ContentPadding.normal)
    }
    constrain(PLAY_TOGGLE){
        bottom.linkTo(OPTION_2.top, ContentPadding.medium)
    }
    constrain(SEEK_BACK_10){
        top.linkTo(PLAY_TOGGLE.top)
        bottom.linkTo(PLAY_TOGGLE.bottom)
    }
    constrain(SKIP_TO_PREV){
        top.linkTo(PLAY_TOGGLE.top)
        bottom.linkTo(PLAY_TOGGLE.bottom)
    }
    constrain(SKIP_TO_NEXT){
        top.linkTo(PLAY_TOGGLE.top)
        bottom.linkTo(PLAY_TOGGLE.bottom)
    }
    constrain(SEEK_FORWARD_30){
        top.linkTo(PLAY_TOGGLE.top)
        bottom.linkTo(PLAY_TOGGLE.bottom)
    }

    // slider
    constrain(OPTION_0, SLIDER, OPTION_1, chainStyle = ChainStyle.Packed){
        start.linkTo(parent.start, ContentPadding.xLarge)
        end.linkTo(parent.end, ContentPadding.xLarge)
    }

    constrain(SLIDER){
        bottom.linkTo(PLAY_TOGGLE.top, ContentPadding.medium)
        width = Dimension.fillToConstraints
    }
    constrain(OPTION_0){
        top.linkTo(SLIDER.top)
        bottom.linkTo(SLIDER.bottom)
    }

    constrain(OPTION_1){
        top.linkTo(SLIDER.top)
        bottom.linkTo(SLIDER.bottom)
    }

    // title
    constrain(TITLE){
        start.linkTo(parent.start, ContentPadding.xLarge)
        end.linkTo(parent.end, ContentPadding.xLarge)
        bottom.linkTo(SLIDER.top, ContentPadding.medium)
        width = Dimension.fillToConstraints
    }

    constrain(TIMER){
        start.linkTo(TITLE.start)
        bottom.linkTo(TITLE.top)
    }
    constrain(SUBTITLE){
        start.linkTo(TIMER.end, ContentPadding.small)
        bottom.linkTo(TIMER.bottom)
        end.linkTo(TITLE.end)
        width = Dimension.percent(0.6f)
    }
}

val Console.Companion.AudioLandscapeConstraintSet get() = com.prime.media.console.AudioLandscapeConstraintSet
private val AudioLandscapeConstraintSet = ConstraintSet {
    constrain(CLOSE){
        top.linkTo(parent.top)
        start.linkTo(parent.start, ContentPadding.normal)
    }

    constrain(SIGNATURE){
        start.linkTo(parent.start)
        bottom.linkTo(parent.bottom)
    }

    constrain(ARTWORK){
        start.linkTo(CLOSE.end)
        top.linkTo(parent.top)
        bottom.linkTo(parent.bottom)
        end.linkTo(TITLE.start)
        height = Dimension.fillToConstraints
        width = Dimension.ratio("1:1")
    }

    val ref = createVerticalChain(TIMER, TITLE, SLIDER, PLAY_TOGGLE, OPTION_2, chainStyle = ChainStyle.Packed)
    constrain(ref){
        top.linkTo(parent.top)
        bottom.linkTo(parent.bottom)
    }

    constrain(TITLE){
        start.linkTo(ARTWORK.end)
        end.linkTo(parent.end, ContentPadding.normal)
        width = Dimension.fillToConstraints
    }

    constrain(TIMER){
        start.linkTo(TITLE.start)
        width = Dimension.wrapContent
    }

    constrain(SUBTITLE){
        start.linkTo(TITLE.end, 1.dp)
        bottom.linkTo(TIMER.bottom)
        end.linkTo(TITLE.end)
        width = Dimension.percent(0.6f)
    }

    // slider
    constrain(OPTION_0, SLIDER, OPTION_1, chainStyle = ChainStyle.Packed){
        start.linkTo(TITLE.start)
        end.linkTo(TITLE.end)
    }

    constrain(SLIDER){
       // bottom.linkTo(PLAY_TOGGLE.top, ContentPadding.medium)
        width = Dimension.fillToConstraints
      //  height = Dimension.wrapContent
    }
    constrain(OPTION_0){
        top.linkTo(SLIDER.top)
        bottom.linkTo(SLIDER.bottom)
    }

    constrain(OPTION_1){
        top.linkTo(SLIDER.top)
        bottom.linkTo(SLIDER.bottom)
    }

    // Playback Controls
    constrain(SEEK_BACK_10, SKIP_TO_PREV, PLAY_TOGGLE, SKIP_TO_NEXT, SEEK_FORWARD_30, chainStyle = ChainStyle.Packed){
        start.linkTo(TITLE.start)
        end.linkTo(TITLE.end)
    }

    constrain(PLAY_TOGGLE){
        bottom.linkTo(OPTION_2.top, ContentPadding.medium)
        height = Dimension.wrapContent
        top.linkTo(SLIDER.bottom, ContentPadding.medium)
    }

    constrain(SEEK_BACK_10){
        top.linkTo(PLAY_TOGGLE.top)
        bottom.linkTo(PLAY_TOGGLE.bottom)
    }

    constrain(SKIP_TO_PREV){
        top.linkTo(PLAY_TOGGLE.top)
        bottom.linkTo(PLAY_TOGGLE.bottom)
    }

    constrain(SKIP_TO_NEXT){
        top.linkTo(PLAY_TOGGLE.top)
        bottom.linkTo(PLAY_TOGGLE.bottom)
    }

    constrain(SEEK_FORWARD_30){
        top.linkTo(PLAY_TOGGLE.top)
        bottom.linkTo(PLAY_TOGGLE.bottom)
    }

    // Options
    constrain(OPTION_2, OPTION_3, OPTION_4, OPTION_5, OPTION_6, chainStyle = ChainStyle.Packed){
        start.linkTo(TITLE.start)
        end.linkTo(TITLE.end)
    }

    constrain(OPTION_2){
       // top.linkTo(PLAY_TOGGLE.bottom, ContentPadding.medium)
    }

    constrain(OPTION_3){
        top.linkTo(OPTION_2.top)
        bottom.linkTo(OPTION_2.bottom)
    }
    constrain(OPTION_4){
        top.linkTo(OPTION_2.top)
        bottom.linkTo(OPTION_2.bottom)
    }
    constrain(OPTION_5){
        top.linkTo(OPTION_2.top)
        bottom.linkTo(OPTION_2.bottom)
    }
    constrain(OPTION_6){
        top.linkTo(OPTION_2.top)
        bottom.linkTo(OPTION_2.bottom)
    }
}

val Console.Companion.VideoPortraitConstraintSet get() = com.prime.media.console.VideoPortraitConstraintSet
private val VideoPortraitConstraintSet = ConstraintSet {
    // Title row
    constrain(TITLE, CLOSE, chainStyle = ChainStyle.Packed){
        start.linkTo(parent.start, ContentPadding.normal)
        end.linkTo(parent.end, ContentPadding.normal)
    }

    // title
    constrain(TITLE){
        width = Dimension.fillToConstraints
        top.linkTo(CLOSE.top)
        bottom.linkTo(CLOSE.bottom)
        translationX = -5.dp
    }

    // Artwork Row
    constrain(ARTWORK){
        start.linkTo(parent.start, ContentPadding.normal)
        end.linkTo(parent.end, ContentPadding.normal)
        top.linkTo(SIGNATURE.bottom)
        bottom.linkTo(TIMER.top)
        width = Dimension.fillToConstraints
        height = Dimension.ratio("1:1")
        visibility = Visibility.Gone
    }


    constrain(SIGNATURE){
        visibility = Visibility.Gone
    }

    constrain(TIMER){
        start.linkTo(TITLE.start)
        top.linkTo(TITLE.bottom)
    }
    constrain(SUBTITLE){
        start.linkTo(TIMER.end, ContentPadding.small)
        bottom.linkTo(TIMER.bottom)
        end.linkTo(TITLE.end)
        width = Dimension.percent(0.6f)
    }

    // Playback Controls
    constrain(SEEK_BACK_10, SKIP_TO_PREV, PLAY_TOGGLE, SKIP_TO_NEXT, SEEK_FORWARD_30, chainStyle = ChainStyle.Packed){
        start.linkTo(parent.start, ContentPadding.normal)
        end.linkTo(parent.end, ContentPadding.normal)
    }
    constrain(PLAY_TOGGLE){
        top.linkTo(parent.top)
        bottom.linkTo(parent.bottom)
    }
    constrain(SEEK_BACK_10){
        top.linkTo(PLAY_TOGGLE.top)
        bottom.linkTo(PLAY_TOGGLE.bottom)
    }
    constrain(SKIP_TO_PREV){
        top.linkTo(PLAY_TOGGLE.top)
        bottom.linkTo(PLAY_TOGGLE.bottom)
    }
    constrain(SKIP_TO_NEXT){
        top.linkTo(PLAY_TOGGLE.top)
        bottom.linkTo(PLAY_TOGGLE.bottom)
    }
    constrain(SEEK_FORWARD_30){
        top.linkTo(PLAY_TOGGLE.top)
        bottom.linkTo(PLAY_TOGGLE.bottom)
    }


    // slider
    constrain(OPTION_0, SLIDER, OPTION_1, chainStyle = ChainStyle.Packed){
        start.linkTo(parent.start, ContentPadding.xLarge)
        end.linkTo(parent.end, ContentPadding.xLarge)
    }

    // Slider Row
    constrain(SLIDER){
        bottom.linkTo(parent.bottom)
        width = Dimension.fillToConstraints
    }
    constrain(OPTION_0){
        top.linkTo(SLIDER.top)
        bottom.linkTo(SLIDER.bottom)
    }

    constrain(OPTION_1){
        top.linkTo(SLIDER.top)
        bottom.linkTo(SLIDER.bottom)
    }

    //options
    constrain(OPTION_2, OPTION_3, OPTION_4, OPTION_5, OPTION_6, chainStyle = ChainStyle.Packed(1.0f)){
        start.linkTo(parent.start, ContentPadding.normal)
        end.linkTo(OPTION_1.end)
    }
    constrain(OPTION_2){
        bottom.linkTo(SLIDER.top)
    }
    constrain(OPTION_3){
        top.linkTo(OPTION_2.top)
        bottom.linkTo(OPTION_2.bottom)
    }
    constrain(OPTION_4){
        top.linkTo(OPTION_2.top)
        bottom.linkTo(OPTION_2.bottom)
    }
    constrain(OPTION_5){
        top.linkTo(OPTION_2.top)
        bottom.linkTo(OPTION_2.bottom)
    }
    constrain(OPTION_6){
        top.linkTo(OPTION_2.top)
        bottom.linkTo(OPTION_2.bottom)
    }
}

val Console.Companion.VideoLandscapeConstraintSet get() = com.prime.media.console.VideoLandscapeConstraintSet
private val VideoLandscapeConstraintSet = ConstraintSet {

    constrain(CLOSE, TITLE, OPTION_2, OPTION_3, OPTION_4, OPTION_5, OPTION_6, chainStyle = ChainStyle.Packed){
        start.linkTo(parent.start, ContentPadding.normal)
        end.linkTo(parent.end, ContentPadding.normal)
    }

    constrain(CLOSE){
        top.linkTo(parent.top)
    }

    constrain(TITLE){
        // top.linkTo(CLOSE.top)
        // bottom.linkTo(CLOSE.bottom)
        width = Dimension.fillToConstraints
    }

    val ref = createVerticalChain(TITLE, SUBTITLE, chainStyle = ChainStyle.Packed)
    constrain(ref){
        top.linkTo(CLOSE.top)
        bottom.linkTo(CLOSE.bottom)
    }


    constrain(SUBTITLE){
        start.linkTo(TITLE.start)
        top.linkTo(TITLE.bottom)
        end.linkTo(TITLE.end)
        width = Dimension.percent(0.6f)
    }

    constrain(OPTION_2){
        top.linkTo(CLOSE.top)
        bottom.linkTo(CLOSE.bottom)
    }

    constrain(OPTION_3){
        top.linkTo(OPTION_2.top)
        bottom.linkTo(OPTION_2.bottom)
    }
    constrain(OPTION_4){
        top.linkTo(OPTION_2.top)
        bottom.linkTo(OPTION_2.bottom)
    }
    constrain(OPTION_5){
        top.linkTo(OPTION_2.top)
        bottom.linkTo(OPTION_2.bottom)
    }
    constrain(OPTION_6){
        top.linkTo(OPTION_2.top)
        bottom.linkTo(OPTION_2.bottom)
    }

    constrain(SIGNATURE){
        start.linkTo(parent.start)
        bottom.linkTo(parent.bottom)
        visibility = Visibility.Gone
    }

    // Artwork Row
    constrain(ARTWORK){
        start.linkTo(parent.start, ContentPadding.normal)
        end.linkTo(parent.end, ContentPadding.normal)
        top.linkTo(SIGNATURE.bottom)
        bottom.linkTo(TIMER.top)
        width = Dimension.fillToConstraints
        height = Dimension.ratio("1:1")
        visibility = Visibility.Gone
    }


    // Playback Controls
    constrain(SEEK_BACK_10, SKIP_TO_PREV, PLAY_TOGGLE, SKIP_TO_NEXT, SEEK_FORWARD_30, chainStyle = ChainStyle.Packed){
        start.linkTo(parent.start, ContentPadding.normal)
        end.linkTo(parent.end, ContentPadding.normal)
    }
    constrain(PLAY_TOGGLE){
        top.linkTo(parent.top)
        bottom.linkTo(parent.bottom)
    }
    constrain(SEEK_BACK_10){
        top.linkTo(PLAY_TOGGLE.top)
        bottom.linkTo(PLAY_TOGGLE.bottom)
    }
    constrain(SKIP_TO_PREV){
        top.linkTo(PLAY_TOGGLE.top)
        bottom.linkTo(PLAY_TOGGLE.bottom)
    }
    constrain(SKIP_TO_NEXT){
        top.linkTo(PLAY_TOGGLE.top)
        bottom.linkTo(PLAY_TOGGLE.bottom)
    }
    constrain(SEEK_FORWARD_30){
        top.linkTo(PLAY_TOGGLE.top)
        bottom.linkTo(PLAY_TOGGLE.bottom)
    }



    // slider
    constrain(OPTION_0, SLIDER, OPTION_1, chainStyle = ChainStyle.Packed){
        start.linkTo(parent.start, ContentPadding.xLarge)
        end.linkTo(parent.end, ContentPadding.xLarge)
    }

    constrain(TIMER){
        start.linkTo(SLIDER.start)
        bottom.linkTo(SLIDER.top)
    }

    // Slider Row
    constrain(SLIDER){
        bottom.linkTo(parent.bottom)
        width = Dimension.fillToConstraints
    }
    constrain(OPTION_0){
        top.linkTo(SLIDER.top)
        bottom.linkTo(SLIDER.bottom)
    }

    constrain(OPTION_1){
        top.linkTo(SLIDER.top)
        bottom.linkTo(SLIDER.bottom)
    }
}
package com.prime.media.core.compose.scaffold

import androidx.annotation.FloatRange
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.*
import com.airbnb.lottie.utils.MiscUtils.lerp
import com.prime.media.core.compose.channel.Channel
import kotlin.math.roundToInt


/**
 * This houses the logic to show [Toast]s, animates [sheet] and displays update progress.
 * @param progress progress for the linear progress bar. pass [Float.NaN] to hide and -1 to show
 * indeterminate and value between 0 and 1 to show progress
 */
@Composable
fun Scaffold2(
    sheet: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    sheetPeekHeight: Dp = 56.dp,
    state: ScaffoldState2 = rememberScaffoldState2(initial = SheetState.COLLAPSED),
    channel: Channel = remember(::Channel),
    @FloatRange(0.0, 1.0) progress: Float = Float.NaN,
    content: @Composable () -> Unit
) {
    // How am I going to build it.
    // * Firstly the content occupies the whole of the screen.
    // * The toast shows below sheet if not expanded other wise over it. Keep in mind the animation.
    // * Third The progress bar can be null shows at the extreme bottom of the screen.
    // * Lastly if sheet is closed don't measure it.
    Layout(
        modifier = modifier.fillMaxSize(),
        content = {
            // stack each part over the player.
            content()
            Channel(state = channel)
            // don't draw sheet when closed.
            sheet()
            // don't draw progressBar.
            when {
                progress == -1f -> LinearProgressIndicator()
                !progress.isNaN() -> LinearProgressIndicator(progress = progress)
            }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val height = constraints.maxHeight

        // create duplicate constants to measure the contents as per their wishes.
        val duplicate = constraints.copy(minWidth = 0, minHeight = 0)

        // measure original content with original constrains
        val contentPlaceable = measurables[0].measure(constraints)
        val toastPlaceable = measurables[1].measure(duplicate)
        val progressPlaceable = measurables.getOrNull(3)?.measure(duplicate)

        val progress by state.progress
        val sheetPeekHeightPx = sheetPeekHeight.toPx().roundToInt()
        // animate sheet with only upto open.
        val sheetH = lerp(sheetPeekHeightPx, height, progress)
        val sheetW = lerp(0, width, progress)
        val sheetPlaceable = measurables[2].measure(
            constraints.copy(0, width, 0, sheetH)
        )

        layout(width, height) {
            contentPlaceable.placeRelative(0, 0)
            // place at the bottom centre
            val sheetY = height - sheetH
            if (sheetY != height)  // draw only if visible
                sheetPlaceable.placeRelative(0, sheetY)
            //Log.d(TAG, "Player: ${height}")
            val adjusted = if (state.current == SheetState.COLLAPSED) sheetPeekHeightPx else 0
            // draw a bottom centre.
            toastPlaceable.placeRelative(
                width / 2 - toastPlaceable.width / 2, height - toastPlaceable.height - adjusted
            )

            progressPlaceable?.placeRelative(
                width / 2 - progressPlaceable.width / 2, height - progressPlaceable.height
            )
        }
    }
}

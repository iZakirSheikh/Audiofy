package com.zs.core_ui.shape

import androidx.compose.foundation.shape.GenericShape

private const val TAG = "Folder"

/**
 * Defines a [GenericShape] simpler to [Icons.Rounded.Folder]
 */
val FolderShape = GenericShape { (x, y), _ ->
    val radius = 0.18f * x
    val stepAt = 0.40f * x
    moveTo(radius, 0f)
    lineTo(stepAt, 0f)
    lineTo(stepAt + radius, radius)
    lineTo(x - radius, radius)
    quadraticTo(x, radius, x, 2 * radius)
    lineTo(x, y - radius)
    quadraticTo(x, y, x - radius, y)
    lineTo(radius, y)
    quadraticTo(0f, y, 0f, y - radius)
    lineTo(0f, radius)
    quadraticTo(0f, 0f, radius, 0f)
    close()
}

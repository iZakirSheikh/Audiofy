package com.prime.media.core.compose.shape

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection

val FolderShape = object : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): Outline {
        val path = Path().apply {
            val cornerSize = size.maxDimension * 0.2f // 20%
            val flapSize = size.width * 0.2f
            moveTo(0f, cornerSize)
            relativeQuadraticBezierTo(0f, -cornerSize, cornerSize, -cornerSize)
            relativeLineTo(flapSize - cornerSize, 0f)
            relativeLineTo(0f, cornerSize)
            relativeLineTo(size.width - flapSize, 0f)
            relativeQuadraticBezierTo(cornerSize, 0f, cornerSize, cornerSize)
            relativeLineTo(0f, size.height - 2 * cornerSize)
            relativeQuadraticBezierTo(0f, cornerSize, -cornerSize, cornerSize)
            relativeLineTo(-size.width + 2 * cornerSize, 0f)
            relativeQuadraticBezierTo(-cornerSize, 0f, -cornerSize, -cornerSize)
            close()
        }
        return Outline.Generic(path)
    }
}

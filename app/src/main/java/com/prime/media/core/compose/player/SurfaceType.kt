package com.prime.media.core.compose.player

import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import androidx.media3.common.Player

/**
 * The type of surface view used for video playbacks.
 * A surface view is a view that displays video or OpenGL content on a dedicated surface layer.
 * There are two types of surface views: SurfaceView and TextureView.
 * @see [SurfaceView]
 * @see [TextureView]
 */
enum class SurfaceType {
    /**
     * A surface view that uses a SurfaceView as the underlying implementation.
     * A SurfaceView provides a high-performance surface for drawing,
     * but has some limitations, such as being always on top of the view hierarchy
     * and not being able to transform or animate.
     */
    SurfaceView,

    /**
     * A surface view that uses a TextureView as the underlying implementation.
     * A TextureView provides a flexible and transformable surface for drawing,
     * but has some trade-offs, such as increased memory usage and reduced performance.
     */
    TextureView;
}


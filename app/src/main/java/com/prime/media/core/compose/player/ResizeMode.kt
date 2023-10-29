package com.prime.media.core.compose.player

/**
 * Specifies how video and album art should be resized when displayed within an `AspectRatioFrameLayout`.
 * These resizing options control how the content is adjusted to maintain the specified aspect ratio.
 *
 * @see androidx.media3.ui.AspectRatioFrameLayout
 */
enum class ResizeMode {
    /**
     * [Fit] mode resizes the content, maintaining its aspect ratio, to fit within the available space.
     * It calculates the maximum dimensions that fit within the view's bounds while preserving the content's aspect ratio.
     */
    Fit,

    /**
     * [FixedWidth] mode resizes the content to a fixed width, maintaining its aspect ratio.
     * The height is adjusted accordingly, ensuring that the content fits within the specified width.
     */
    FixedWidth,

    /**
     * [FixedHeight] mode resizes the content to a fixed height, maintaining its aspect ratio.
     * The width is adjusted accordingly, ensuring that the content fits within the specified height.
     */
    FixedHeight,

    /**
     * [Fill] mode resizes the content, ignoring the aspect ratio.
     * It stretches or compresses the content to completely fill the available space within the view.
     */
    Fill,

    /**
     * [Zoom] mode resizes the content, maintaining its aspect ratio, to completely fill the available space.
     * Either the width or height is increased to obtain the desired aspect ratio, ensuring the content
     * covers the entire view while preserving its proportions.
     */
    Zoom,
}
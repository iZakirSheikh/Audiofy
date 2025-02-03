package com.zs.widget

/**
 * Represents the different sizes for a widget.
 *
 * This enum is used to categorize the size of a widget based on its width and height.
 * It provides three distinct categories:
 *   - COMPACT: A small widget with width less than 300 and height less than 100.
 *   - SQUARE: A widget with approximately equal width and height, or a size that doesn't fall into COMPACT or NORMAL.
 *   - NORMAL: A larger widget with width greater than 300 and height greater than 100.
 */
internal enum class ViewType {
    COMPACT,
    SQUARE,
    NORMAL
}
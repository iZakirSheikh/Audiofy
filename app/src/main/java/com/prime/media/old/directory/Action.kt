package com.prime.media.old.directory

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
import androidx.compose.material.icons.automirrored.outlined.PlaylistAdd
import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.vector.ImageVector
import com.prime.media.R
import com.primex.core.Text

/**
 * Class to represent a generic action in the app.
 * @property id: The unique identifier for the action, used to differentiate it from other actions.
 * @property title: The human-readable title for the action, displayed to the user.
 * @property icon: The icon representing the action, displayed to the user to help with visual recognition.
 * @author Zakir Sheikh.
 * @since 2023
 */
@Stable
sealed class Action(val id: String, val title: Text, val icon: ImageVector) {
    /**
     * Constructor to create an `Action` instance from a string title.
     *
     * @param id The unique identifier for this action.
     * @param title The title of this action as a string.
     * @param icon The icon associated with the action.
     */
    constructor(id: String, title: String, icon: ImageVector) : this(id, Text(title), icon)

    constructor(id: String, @StringRes title: Int, icon: ImageVector) : this(id, Text(title), icon)

    object Play : Action("action_play", R.string.play, Icons.Outlined.PlayArrow)
    object Shuffle : Action("action_shuffle", R.string.shuffle, Icons.Outlined.Shuffle)
    object Share : Action("action_share", R.string.share, Icons.Outlined.Share)
    object Delete : Action("action_delete", R.string.delete, Icons.Outlined.Delete)
    object PlaylistAdd :
        Action(
            "action_add_to_playlist", R.string.add_to_playlist,
            Icons.AutoMirrored.Outlined.PlaylistAdd
        )

    object Make : Action("action_create", R.string.create, Icons.Outlined.AddCircle)
    object Edit : Action("action_edit", R.string.edit, Icons.Outlined.Edit)
    object GoToArtist : Action("action_go_to_artist", R.string.go_to_artist, Icons.Outlined.Person)
    object GoToAlbum : Action("action_go_to_album", R.string.go_to_album, Icons.Outlined.Album)
    object Properties : Action("action_properties", R.string.properties, Icons.Outlined.Info)
    object SelectAll : Action("action_select_all", R.string.select_all, Icons.Outlined.SelectAll)
    object AddToQueue :
        Action("action_add_to_queue", R.string.add_to_queue, Icons.Outlined.AddToQueue)

    object PlayNext : Action("action_play_next", R.string.play_next, Icons.Outlined.QueuePlayNext)

    /**
     * Check if this `Action` instance is equal to another object.
     *
     * @param other The object to compare to this `Action` instance.
     * @return `true` if the objects are equal, `false` otherwise.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}

/**
 * Class to represent a "group by" action in the app.
 *
 * This class is marked as stable, indicating that it's expected to have a long lifespan
 * and should not change significantly.
 *
 * This class extends the [Action] class and represents a specific type of action, a "group by"
 * action. It's marked as a sealed class, meaning that it cannot have subclasses except for the
 * ones defined within the same file.
 *
 * An example of using a `DropDownItem` of `Compose` could look like this:
 * ```
 *     DropDownItem(
 *          value = action,
 *          onClick = {
 *              groupBy(GroupBy.Date)
 *          }
 *     )
 * ```
 * @author Zakir Sheikh
 * @since 2023-02-12
 * @see [Action]
 */
@Stable
sealed class GroupBy(id: String, title: Text, icon: ImageVector) : Action(id, title, icon) {
    private constructor(id: String, title: String, icon: ImageVector) : this(id, Text(title), icon)
    private constructor(id: String, @StringRes title: Int, icon: ImageVector) : this(
        id,
        Text(title),
        icon
    )

    /**
     * GroupBy the title/Name of the item.
     */
    object None : GroupBy(ORDER_BY_NONE, R.string.none, Icons.Outlined.FilterNone)
    object Name : GroupBy(ORDER_BY_NAME, R.string.name, Icons.Outlined.Title)
    object DateModified :
        GroupBy(ORDER_BY_DATE_MODIFIED, R.string.date_modified, Icons.Outlined.AccessTime)

    object DateAdded :
        GroupBy(ORDER_BY_DATE_ADDED, R.string.date_added, Icons.Outlined.CalendarMonth)

    object Artist : GroupBy(ORDER_BY_ARTIST, R.string.artist, Icons.Outlined.Person)
    object Album : GroupBy(ORDER_BY_ALBUM, R.string.album, Icons.Outlined.Album)
    object Folder : GroupBy(ORDER_BY_FOLDER, R.string.folder, Icons.Outlined.Folder)
    object Length : GroupBy(ORDER_BY_LENGTH, R.string.length, Icons.Outlined.AvTimer)

    companion object {
        private const val ORDER_BY_NONE = "order_by_none"
        private const val ORDER_BY_NAME = "order_by_name"
        private const val ORDER_BY_DATE_MODIFIED = "order_by_date_modified"
        private const val ORDER_BY_DATE_ADDED = "order_by_date_added"
        private const val ORDER_BY_ARTIST = "order_by_artist"
        private const val ORDER_BY_ALBUM = "order_by_album"
        private const val ORDER_BY_FOLDER = "order_by_folder"
        private const val ORDER_BY_LENGTH = "order_by_length"

        fun map(id: String): GroupBy {
            return when (id) {
                ORDER_BY_NONE -> None
                ORDER_BY_ALBUM -> Album
                ORDER_BY_ARTIST -> Artist
                ORDER_BY_DATE_ADDED -> DateAdded
                ORDER_BY_DATE_MODIFIED -> DateModified
                ORDER_BY_FOLDER -> Folder
                ORDER_BY_LENGTH -> Length
                ORDER_BY_NAME -> Name
                else -> error("No such GroupBy algo. $id")
            }
        }
    }
}

/**
 * Class to represent a "view type" action in the app.
 *
 * This class extends the [Action] class and represents a specific type of action, a "view type"
 * action. It's marked as a sealed class, meaning that it cannot have subclasses except for the ones
 * defined within the same file.
 *
 * An example of using this class could look like:
 *
 * ```
 *     val viewType = ViewType("view_type_id", Text("List view"), ImageVector(R.drawable.ic_list_view))
 *     changeViewType(viewType)
 * ```
 * @author: Zakir Sheikh
 * @since: 2023-02-12
 * @see: [Action]
 */
@Stable
sealed class ViewType(id: String, title: Text, icon: ImageVector) : Action(id, title, icon) {
    private constructor(id: String, title: String, icon: ImageVector) : this(id, Text(title), icon)
    private constructor(id: String, @StringRes title: Int, icon: ImageVector) : this(
        id,
        Text(title),
        icon
    )

    object List : ViewType(VIEW_TYPE_LIST, R.string.list, Icons.AutoMirrored.Outlined.List)
    object Grid : ViewType(VIEW_TYPE_GRID, R.string.grid, Icons.Outlined.GridView)

    companion object {
        val VIEW_TYPE_LIST = "view_type_list"
        val VIEW_TYPE_GRID = "view_type_grid"

        /**
         * Maps the given `id` to a `ViewType` instance.
         *
         * This function takes a string `id` as input and returns a `ViewType` instance that
         * corresponds to that `id`. The mapping between `id` and `ViewType` instances is determined
         * by the implementation of this function. The `id` can be passed to the function using a
         * navigator, such as the `handle` method.
         *
         * @author: [Author Name]
         * @since: [Date]
         *
         * An example of using this function could look like:
         *
         * ```
         *     val viewTypeId = "list_view"
         *     val viewType = map(viewTypeId)
         *     changeViewType(viewType)
         * ```
         *
         * @param id The string `id` to map to a `ViewType` instance.
         * @return A `ViewType` instance that corresponds to the given `id`.
         */
        fun map(id: String): ViewType {
            return when (id) {
                VIEW_TYPE_LIST -> List
                VIEW_TYPE_GRID -> Grid
                else -> error("No such view type id: $id")
            }
        }
    }
}
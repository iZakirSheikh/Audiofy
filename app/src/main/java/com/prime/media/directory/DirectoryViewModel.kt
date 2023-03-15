package com.prime.media.directory

import androidx.compose.material.icons.outlined.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.primex.core.Text
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

private const val TAG = "Directory"

/**
 * A type alias for a `Map` of `Text` to a list of objects of type `T`.
 * @param T the type of the objects in the list values of the map
 */
typealias Mapped<T> = Map<Text, List<T>>

/**
 * Represents metadata of [Directory].
 *
 * This data class represents metadata for a [Directory].
 * The metadata provides additional information about the list and its elements, such as a
 * title, cardinality, and last modified.
 *
 * @param title A human-readable title for the [Directory].
 * @param cardinality The number of elements in the [Directory]. A value of `-1` indicates that the cardinality is unknown.
 * @param dateModified An optional last modified time for the [Directory]. A value of `-1L` indicates that the dateModified is unknown.
 * @param artwork The uri to the representational image of this data item.
 *
 * @author Zakir Sheikh
 * @since 2.0.0
 *
 * An example usage of this class could look like:
 *
 * ```
 *     val listData = listOf(1, 2, 3)
 *     val metadata = MetaData("My List", listData.size, 10001561321)
 *     processData(listData, metadata)
 * ```
 */
data class MetaData(
    val title: Text,
    val artwork: String? = null,
    val cardinality: Int = -1,
    val dateModified: Long = -1L
)

/**
 * Author: Zakir Sheikh
 * Since: 2.0
 *
 * Introduction:
 * This property is used to check if the [MetaData] requires a simple view or an advanced one.
 *
 * Usage Example:
 * ```
 * val metaData = MetaData(item = null, cardinality = -1)
 * val result = metaData.isSimple
 * ```
 * The above code will return `true` if the `item` property [MetaData.artwork] object is `null`.
 */
val MetaData.isSimple get() = artwork == null

/**
 * Represents the filter for the directory.
 *
 * @author Zakir Sheikh
 * @since 1.0
 *
 * @property first The `GroupBy` parameter to group the result.
 * @property second The query string used to filter the result.
 * @property third Specifies the sort order for the resulting list, either ascending or descending.
 *
 * Example usage:
 * ```
 * val filter = Filter(GroupBy.NAME, "John", true)
 * ```
 */
typealias Filter = Triple<GroupBy, String?, Boolean>

/**
 * DirectoryViewModel represents the common logic among different directories in the application.
 * It is an abstract class and provides the basic structure for managing the visibility, selection,
 * and focus of the items displayed in a directory. It also manages common parameters such as the
 * [ViewType], `query`, `key`, `ascending`, and [MetaData].
 *
 * **Functionality:**
 * 1. Visibility management of the items in the directory.
 * 2. Selection logic of the items.
 * 3. Current focusable item management.
 * 4. Management of common parameters such as `ViewType`, `query`, `key`, `ascending`, and `ListMeta`.
 * 5. Filter and `GroupBy` logic handling.
 * 6. Management of the main `Actions`, selected `Action`, and supported `GroupBy`s and `ViewType`s.
 *
 * @param handle - A [SavedStateHandle] instance that is used to manage the state of the view model.
 *
 * Usage example:
 * In order to use this class, you need to extend it and provide implementations for the abstract methods.
 *
 * ```
 * class MyDirectoryViewModel(handle: SavedStateHandle) : DirectoryViewModel<MyDirectoryItem>(handle) {
 *
 *      // Implement abstract methods
 *      ...
 * }
 * ```
 * @see ViewModel
 *
 * @author Zakir Sheikh
 * @since 2.0
 */
abstract class DirectoryViewModel<T : Any>(handle: SavedStateHandle) : ViewModel() {
    /**
     * Holds some of the default values and params use in the directory.
     */
    companion object {
        /**
         * The constant `NULL_STRING` represents the default value of a parameter.
         *
         * @author: Zakir Sheikh
         * @since 2.0
         */
        const val NULL_STRING = "@null"

        /**
         * Author: Zakir Sheikh
         *
         * @since 2.0
         *
         * Property:
         * `PARAM_KEY` - A constant representing the optional key parameter used to identify the directory.
         */
        private const val PARAM_KEY = "_key"

        /**
         * The optional search query.
         */
        private const val PARAM_QUERY = "_query"

        /**
         * The optional [GroupBy] action.
         */
        private const val PARAM_GROUP_BY = "_group_by"

        /**
         * The optional [GroupBy]/OrderBy ascending param.
         */
        private const val PARAM_ASCENDING = "_ascending"

        /**
         * The ViewType requested by the launcher. Must be any of [ViewType]
         */
        private const val PARAM_VIEW_TYPE = "_view_type"

        /**
         * Composes a route for the given host.
         *
         * @param host The host name.
         * @return The constructed route.
         */
        fun compose(host: String) =
            "$host/{${PARAM_KEY}}/{${PARAM_QUERY}}/{${PARAM_GROUP_BY}}/{${PARAM_ASCENDING}}/{${PARAM_VIEW_TYPE}}"

        /**
         * Constructs a route for the given host.
         *
         * @param host The host name.
         * @param key The optional key to identify the directory.
         * @param query The optional search query.
         * @param order The optional grouping criteria.
         * @param ascending The optional sorting order, ascending if true.
         * @param viewType The optional view type.
         * @return The constructed direction.
         */
        fun compose(
            host: String,
            key: String = NULL_STRING,
            query: String = NULL_STRING,
            order: GroupBy = GroupBy.Name,
            ascending: Boolean = true,
            viewType: ViewType = ViewType.List
        ) = "$host/$key/$query/${order.id}/${if (ascending) 1 else 0}/${viewType.id}"
    }

    /**
     * Extract all the params and use in child/sub-classes.
     * The non-null key representing this directory with default value
     */
    val key = handle.get<String>(PARAM_KEY)!!

    /**
     * Represents the current filter applied to the directory.
     *
     * @property groupBy The current grouping criteria.
     * @property query The current search query.
     * @property ascending The current sorting order, ascending if true.
     */
    val filter = MutableStateFlow(
        Filter(
            handle.get<String>(PARAM_GROUP_BY)!!.let { GroupBy.map(it) },
            handle.get<String>(PARAM_QUERY)!!.let { if (it == NULL_STRING) null else it },
            handle.get<String>(PARAM_ASCENDING).let { it != "0" }
        )
    )

    /**
     * Filters the source based on the provided [value], [order], and [ascending].
     *
     * @param value The new search query to filter against.
     * @param order The new grouping criteria.
     * @param ascending The new sorting order, ascending if true.
     */
    fun filter(
        value: String? = filter.value.second,
        order: GroupBy = filter.value.first,
        ascending: Boolean = filter.value.third
    ) {
        filter.value = filter.value.copy(order, value, ascending)
    }


    /**
     * The viewType of this directory.
     *
     * @property viewType a [MutableState] object that represents the view type of the directory
     *
     * The initial value of the [viewType] is obtained by calling [handle.get] with [PARAM_VIEW_TYPE] as the key.
     * The returned string is then mapped to its corresponding [ViewType] object using the [ViewType.map] method.
     */
    var viewType: ViewType by mutableStateOf(
        handle.get<String>(PARAM_VIEW_TYPE)!!.let {
            ViewType.map(it)
        }
    )

    /**
     * Toggles the viewType.
     *
     * This function is used to switch between different view types for the directory.
     * The exact implementation of this function is left abstract and should be defined
     * in a subclass.
     */
    abstract fun toggleViewType()

    /**
     * Meta information for the [DirectoryViewModel], includes the title, etc.
     *
     * Initial value is `null`.
     */
    var meta: MetaData? by mutableStateOf(null)

    /**
     * Keys of items that are selected.
     *
     * An observable [List] of [String] representing the keys of selected items.
     */
    val selected: List<String> = mutableStateListOf()

    /**
     * Toggles the selection of an item.
     *
     * @param key The key of the item to toggle the selection for.
     *
     * This function toggles the selection state of the item with the specified key. If the item is already
     * selected, it is deselected. If it is not selected, it is selected.
     */
    open fun select(key: String) {
        if (selected.contains(key))
            (selected as SnapshotStateList).remove(key)
        else
            (selected as SnapshotStateList).add(key)
        // clear focus if selected contains items
        if (selected.size == 1)
            focused = ""
    }

    /**
     * Clears the selection of all items.
     *
     * This function deselects all items by clearing the [selected].
     */
    fun clear() {
        viewModelScope.launch {
            val list = ArrayList(selected)
            list.forEach {
                // remove each.
                select(it)
            }
        }
    }

    /**
     * The key of the item that currently has focus.
     *
     * Mutable. Can be directly used to update the focused item.
     */
    var focused: String by mutableStateOf("")

    /**
     * A list of supported actions.
     *
     * Visible only when the value of [selected] is greater than 0.
     */
    abstract val actions: List<Action>

    /**
     * A list of supported orders.
     */
    abstract val orders: List<GroupBy>

    /**
     * The primary action of the directory.
     *
     * Used to display the floating action button (FAB), similar to playlists.
     *
     * The index of the items in the list determines their priority, with `0` being the FAB and `1`
     * and `2` being header buttons 1 and 2, respectively, from the right.
     */
    abstract val mActions: List<Action?>

    /**
     * The data of the directory. Might represent both paged flow and normal flow.
     */
    abstract val data: Flow<Any>
}
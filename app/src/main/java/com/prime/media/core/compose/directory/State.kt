package com.prime.media.core.compose.directory

import com.prime.media.impl.Filter
import com.prime.media.impl.MetaData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

interface Directory<T : Any> {
    /**
     * Extract all the params and use in child/sub-classes.
     * The non-null key representing this directory with default value
     */
    val key: String

    /**
     * Represents the current filter applied to the directory.
     *
     * @property groupBy The current grouping criteria.
     * @property query The current search query.
     * @property ascending The current sorting order, ascending if true.
     */
    val filter: MutableStateFlow<Filter>

    /**
     * The viewType of this directory.
     *
     * @property viewType a [MutableState] object that represents the view type of the directory
     *
     * The initial value of the [viewType] is obtained by calling [handle.get] with [PARAM_VIEW_TYPE] as the key.
     * The returned string is then mapped to its corresponding [ViewType] object using the [ViewType.map] method.
     */
    var viewType: ViewType

    /**
     * Meta information for the [DirectoryViewModel], includes the title, etc.
     *
     * Initial value is `null`.
     */
    var meta: MetaData?

    /**
     * Keys of items that are selected.
     *
     * An observable [List] of [String] representing the keys of selected items.
     */
    val selected: List<String>

    /**
     * The key of the item that currently has focus.
     *
     * Mutable. Can be directly used to update the focused item.
     */
    var focused: String

    /**
     * A list of supported actions.
     *
     * Visible only when the value of [selected] is greater than 0.
     */
    val actions: List<Action>

    /**
     * A list of supported orders.
     */
    val orders: List<GroupBy>

    /**
     * The primary action of the directory.
     *
     * Used to display the floating action button (FAB), similar to playlists.
     *
     * The index of the items in the list determines their priority, with `0` being the FAB and `1`
     * and `2` being header buttons 1 and 2, respectively, from the right.
     */
    val mActions: List<Action?>

    /**
     * The data of the directory. Might represent both paged flow and normal flow.
     */
    val data: Flow<Any>

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
    )

    /**
     * Toggles the viewType.
     *
     * This function is used to switch between different view types for the directory.
     * The exact implementation of this function is left abstract and should be defined
     * in a subclass.
     */
    fun toggleViewType()

    /**
     * Toggles the selection of an item.
     *
     * @param key The key of the item to toggle the selection for.
     *
     * This function toggles the selection state of the item with the specified key. If the item is already
     * selected, it is deselected. If it is not selected, it is selected.
     */
    open fun select(key: String)

    /**
     * Clears the selection of all items.
     *
     * This function deselects all items by clearing the [selected].
     */
    fun clear()
}
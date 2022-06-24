package com.prime.player.extended

import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

private class Holder(surface: View) : RecyclerView.ViewHolder(surface)

private class LocalAdapter<T>(
    callback: DiffUtil.ItemCallback<T>,
    val content: @Composable (T) -> Unit
) : ListAdapter<T, Holder>(callback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        return Holder(ComposeView(parent.context))
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        (holder.itemView as ComposeView).apply {
            setContent {
                val item = getItem(position)
                content(item)
            }
        }
    }
}





@Composable
fun <T> Recycler(
    modifier: Modifier = Modifier,
    reversed: Boolean = false,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    observer: ((RecyclerView) -> RecyclerView.AdapterDataObserver?)? = null,
    orientation: Int = RecyclerView.VERTICAL,
    list: List<T>,
    callback: DiffUtil.ItemCallback<T>,
    content: @Composable (T) -> Unit
) {
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    AndroidView(
        modifier = modifier,
        factory = { context ->
            RecyclerView(context).apply {
                layoutManager = LinearLayoutManager(context, orientation, reversed)
                adapter = LocalAdapter(callback, content).also { adapter ->
                    observer?.invoke(this)?.let {
                        adapter.registerAdapterDataObserver(it)
                    }
                }
            }
        }
    ) {
        (it.adapter as LocalAdapter<T>).submitList(list)
        with(density) {
            it.setPadding(
                contentPadding.calculateLeftPadding(direction).toPx().toInt(),
                contentPadding.calculateTopPadding().toPx().toInt(),
                contentPadding.calculateRightPadding(direction).toPx().toInt(),
                contentPadding.calculateBottomPadding().toPx().toInt()
            )
            it.clipToPadding = false
        }
    }
}

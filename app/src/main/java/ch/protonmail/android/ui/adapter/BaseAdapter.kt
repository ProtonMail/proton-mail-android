/*
 * Copyright (c) 2020 Proton Technologies AG
 *
 * This file is part of ProtonMail.
 *
 * ProtonMail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ProtonMail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with ProtonMail. If not, see https://www.gnu.org/licenses/.
 */

@file:Suppress("unused", "MemberVisibilityCanBePrivate") // Public APIs

package ch.protonmail.android.ui.adapter

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.calculateDiff
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.ui.adapter.BaseAdapter.DiffCallback
import ch.protonmail.android.ui.adapter.BaseAdapter.ItemsComparator

/**
 * A [RecyclerView.Adapter] that contains a [List] of [UiModel] items.
 * Implements [ClickableAdapter]
 *
 * A basic implementations expects
 * * an implementation of [ItemsComparator] that will be passed to the constructor
 * * an implementation of [ClickableAdapter.ViewHolder] that will be passes ad generic [UiModel] and
 * will be created by overriding [onCreateViewHolder]
 *
 * A basic usage only require [items] to be set and [DiffUtil] will handle everything.
 * [ClickableAdapter.onItemClick] and [ClickableAdapter.onItemLongClick] can be set.
 *
 *
 * @param itemsComparator a REQUIRED [ItemsComparator] of [UiModel] that will be used from
 * [DiffCallback] for compare the items.
 *
 * @param onItemClick A lambda with [UiModel] as receiver that will be triggered when an item is clicked
 *
 * @param onItemLongClick A lambda with [UiModel] as receiver that will be triggered when an item is long clicked
 *
 *
 * @author Davide Farella
 */
abstract class BaseAdapter<UiModel, ViewHolder : ClickableAdapter.ViewHolder<UiModel>>(
    private val itemsComparator: ItemsComparator<UiModel>,
    @set:Deprecated("This should not be mutable. It will be immutable from 0.3.x. Move it in the " +
        "constructor if didn't do yet")
    override var onItemClick: (UiModel) -> Unit = {},
    @set:Deprecated("This should not be mutable. It will be immutable from 0.3.x. Move it in the " +
        "constructor if didn't do yet")
    override var onItemLongClick: (UiModel) -> Unit = {}
) : RecyclerView.Adapter<ViewHolder>(), ClickableAdapter<UiModel, ViewHolder> {

    /**
     * A [List] of items [UiModel].
     * We overridden the `set` method for dispatch updates through a [DiffCallback].
     */
    var items = listOf<UiModel>()
        set(value) {
            val oldValue = field
            field = value

            val diffResult = calculateDiff(DiffCallback(oldValue, value, itemsComparator))
            diffResult.dispatchUpdatesTo(this)
        }

    /**
     * @return the size of all the Items in the Adapter.
     * @see items
     */
    override fun getItemCount(): Int = items.size

    /**
     * Get the Item for the requested [position], call [ClickableAdapter.ViewHolder.onBind] and set
     * [clickListenerInvoker] and [longClickListenerInvoker] for the [holder].
     */
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = items[position]
        holder.onBind(item)
        prepareClickListeners(holder)
    }

    /**
     * A [DiffUtil.Callback] for [BaseAdapter].
     *
     * @param itemsComparator an [ItemsComparator] of [T] for compare old items to new items.
     */
    class DiffCallback<T>(
        private val oldList: List<T>, private val newList: List<T>,
        private val itemsComparator: ItemsComparator<T>
    ) : DiffUtil.Callback() {

        /** @return the size of [oldList] */
        override fun getOldListSize() = oldList.size

        /** @return the size of [newList] */
        override fun getNewListSize(): Int = newList.size

        /** @return [ItemsComparator.areItemsTheSame] */
        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            itemsComparator.areItemsTheSame(oldList[oldItemPosition], newList[newItemPosition])

        /** @return [ItemsComparator.areContentsTheSame] */
        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
            itemsComparator.areContentsTheSame(oldList[oldItemPosition], newList[newItemPosition])

        /** @return [ItemsComparator.getChangePayload] */
        override fun getChangePayload(oldItemPosition: Int, newItemPosition: Int): Any? =
            itemsComparator.getChangePayload(oldList[oldItemPosition], newList[newItemPosition])
    }

    /**
     * An abstract class for compare two items [T] for declare if they are the same items and
     * if the have the same contents.
     *
     * Used by [DiffCallback].
     */
    abstract class ItemsComparator<T> {

        /**
         * Called by the [DiffCallback.areItemsTheSame] to decide whether two object represent
         * the same Item.
         * <p>
         * For example, if your items have unique ids, this method should check their id equality.
         *
         * @return True if the two items represent the same object or false if they are different.
         */
        abstract fun areItemsTheSame(oldItem: T, newItem: T): Boolean

        /**
         * Called by the [DiffCallback.areContentsTheSame] when it wants to check whether two
         * items have the same data.
         * [DiffUtil] uses this information to detect if the contents of an item has changed.
         * <p>
         * DiffUtil uses this method to check equality instead of [Object.equals] so that you
         * can change its behavior depending on your UI.
         * For example, if you are using DiffUtil with a [RecyclerView.Adapter], you should
         * return whether the items' visual representations are the same.
         * <p>
         * This method is called only if [areItemsTheSame] returns `true` for these items.
         *
         * @return True if the contents of the items are the same or false if they are different.
         */
        open fun areContentsTheSame(oldItem: T, newItem: T): Boolean =
            oldItem == newItem

        /**
         * When [areItemsTheSame] returns `true` for two items and [areContentsTheSame] returns
         * false for them, DiffUtil
         * calls this method to get a payload about the change.
         *
         *
         * For example, if you are using DiffUtil with [RecyclerView], you can return the
         * particular field that changed in the item and your [RecyclerView.ItemAnimator] can
         * use that information to run the correct animation.
         *
         *
         * Default implementation returns `null`.
         *
         * @return A payload object that represents the change between the two items.
         */
        open fun getChangePayload(oldItem: T, newItem: T): Any? = null
    }
}

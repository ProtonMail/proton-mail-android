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
package ch.protonmail.android.drawer.presentation.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.R
import ch.protonmail.android.databinding.DrawerFooterBinding
import ch.protonmail.android.databinding.DrawerListItemBinding
import ch.protonmail.android.databinding.DrawerSectionNameItemBinding
import ch.protonmail.android.drawer.presentation.model.DrawerItemUiModel
import ch.protonmail.android.drawer.presentation.model.DrawerItemUiModel.Footer
import ch.protonmail.android.drawer.presentation.model.DrawerItemUiModel.Primary
import ch.protonmail.android.drawer.presentation.model.DrawerItemUiModel.SectionName
import ch.protonmail.android.utils.extensions.setNotificationIndicatorSize
import ch.protonmail.libs.core.ui.adapter.BaseAdapter
import ch.protonmail.libs.core.ui.adapter.ClickableAdapter
import kotlinx.android.synthetic.main.drawer_list_item.view.*

private const val VIEW_TYPE_SECTION_NAME = 0
private const val VIEW_TYPE_STATIC = 1
private const val VIEW_TYPE_LABEL = 2
private const val VIEW_TYPE_FOOTER = 3

/**
 * Adapter for Drawer Items that support different View types
 *
 * Inherit from [BaseAdapter]
 */
internal class DrawerAdapter :
    BaseAdapter<DrawerItemUiModel, DrawerAdapter.ViewHolder<DrawerItemUiModel>>(ModelsComparator) {

    /** Select the given [item] and un-select all the others */
    fun setSelected(item: Primary) {
        items = items.map {
            if (it is Primary)
            // Select if this item is same as given item
                it.copyWithSelected(it == item)
            else it
        }
    }

    /** @return a [ViewHolder] for the given [viewType] */
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<DrawerItemUiModel> =
        parent.viewHolderForViewType(viewType)

    /** @return [Int] that identifies the View type for the Item at the given [position] */
    override fun getItemViewType(position: Int) = items[position].viewType

    /** A [BaseAdapter.ItemsComparator] for the Adapter */
    private object ModelsComparator : BaseAdapter.ItemsComparator<DrawerItemUiModel>() {

        /** Check if old [DrawerItemUiModel] and new [DrawerItemUiModel] are the same element */
        override fun areItemsTheSame(oldItem: DrawerItemUiModel, newItem: DrawerItemUiModel): Boolean {
            val newItemAsStatic = newItem as? Primary.Static
            val newItemAsLabel = newItem as? Primary.Label
            return when (oldItem) {
                is SectionName -> oldItem == newItem
                is Primary -> when (oldItem) {
                    is Primary.Static -> oldItem.type == newItemAsStatic?.type
                    is Primary.Label -> oldItem.uiModel.labelId == newItemAsLabel?.uiModel?.labelId
                }
                is Footer -> true // We only have one footer
            }
        }
    }

    /** @return a [ViewHolder] for the given [viewType] */
    private fun <Model : DrawerItemUiModel> ViewGroup.viewHolderForViewType(viewType: Int): ViewHolder<Model> {
        val inflater = LayoutInflater.from(context)
        return when (viewType) {
            VIEW_TYPE_SECTION_NAME -> {
                val binding = DrawerSectionNameItemBinding.inflate(inflater, this, false)
                SectionNameViewHolder(binding)
            }
            VIEW_TYPE_STATIC -> {
                val binding = DrawerListItemBinding.inflate(inflater, this, false)
                StaticViewHolder(binding)
            }
            VIEW_TYPE_LABEL -> {
                val binding = DrawerListItemBinding.inflate(inflater, this, false)
                LabelViewHolder(binding)
            }
            VIEW_TYPE_FOOTER -> {
                val binding = DrawerFooterBinding.inflate(inflater, this, false)
                FooterViewHolder(binding)
            }
            else -> throw IllegalArgumentException("View type not found: '$viewType'")
        } as ViewHolder<Model>
    }

    /** Abstract ViewHolder for the Adapter */
    abstract class ViewHolder<Model : DrawerItemUiModel>(itemView: View) :
        ClickableAdapter.ViewHolder<Model>(itemView)

    /** [ViewHolder] for [Primary] Item */
    private abstract class PrimaryViewHolder<P : Primary>(itemView: View) : ViewHolder<P>(itemView) {

        override fun onBind(item: P) = with(itemView) {
            super.onBind(item)
            drawer_item_selection_view.isVisible = item.selected
            drawer_item_notifications_text_view.isVisible = item.hasNotifications()
            drawer_item_notifications_text_view.text = item.notificationCount.formatToMax4chars()
            drawer_item_notifications_text_view.setNotificationIndicatorSize(item.notificationCount)
        }

        private fun Int.formatToMax4chars(): String {
            val coerced = coerceAtMost(9999)
            return if (coerced == this) "$coerced"
            else "$coerced+"
        }
    }

    /** [ViewHolder] for [SectionName] */
    private class SectionNameViewHolder(
        private val binding: DrawerSectionNameItemBinding
    ) : ViewHolder<SectionName>(binding.root) {

        override fun onBind(item: SectionName) {
            super.onBind(item)
            binding.drawerSectionNameTextView.text = item.text
            binding.drawerSectionNameCreateButton.isVisible = item.shouldShowCreateButton
        }
    }

    /**
     * [ViewHolder] for [Primary.Label] Item
     * Inherit from [PrimaryViewHolder]
     */
    private class LabelViewHolder(
        private val binding: DrawerListItemBinding
    ) : PrimaryViewHolder<Primary.Label>(binding.root) {

        override fun onBind(item: Primary.Label) {
            super.onBind(item)
            val model = item.uiModel

            (itemView.layoutParams as RecyclerView.LayoutParams).marginStart =
                model.folderLevel * itemView.context.resources.getDimensionPixelSize(R.dimen.gap_large)
            binding.drawerItemLabelTextView.apply {
                text = model.name
                tag = model.name
            }
            binding.drawerItemIconView.apply {
                setColorFilter(item.uiModel.icon.colorInt)
                setImageResource(item.uiModel.icon.drawableRes)
            }
        }
    }

    /**
     * [ViewHolder] for [Primary.Static] Item
     * Inherit from [PrimaryViewHolder]
     */
    private class StaticViewHolder(
        private val binding: DrawerListItemBinding
    ) : PrimaryViewHolder<Primary.Static>(binding.root) {

        override fun onBind(item: Primary.Static) {
            super.onBind(item)
            binding.apply {
                drawerItemLabelTextView.setText(item.labelRes)
                drawerItemIconView.setImageResource(item.iconRes)
                menuItem.tag = context.getString(item.labelRes)
            }
        }
    }

    /** [ViewHolder] for [Footer] */
    private class FooterViewHolder(
        private val binding: DrawerFooterBinding
    ) : ViewHolder<Footer>(binding.root) {

        override fun onBind(item: Footer) {
            super.onBind(item)
            binding.root.text = item.text
        }
    }
}

/** @return [Int] view type for the receiver [DrawerItemUiModel] */
private val DrawerItemUiModel.viewType: Int
    get() {
        return when (this) {
            is SectionName -> VIEW_TYPE_SECTION_NAME
            is Primary -> when (this) {
                is Primary.Static -> VIEW_TYPE_STATIC
                is Primary.Label -> VIEW_TYPE_LABEL
            }
            is Footer -> VIEW_TYPE_FOOTER
        }
    }

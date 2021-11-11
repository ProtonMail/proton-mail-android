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
import ch.protonmail.android.drawer.presentation.model.DrawerItemUiModel.CreateItem
import ch.protonmail.android.drawer.presentation.model.DrawerItemUiModel.Footer
import ch.protonmail.android.drawer.presentation.model.DrawerItemUiModel.Primary
import ch.protonmail.android.drawer.presentation.model.DrawerItemUiModel.SectionName
import ch.protonmail.android.utils.extensions.setNotificationIndicatorSize
import ch.protonmail.libs.core.ui.adapter.BaseAdapter
import ch.protonmail.libs.core.ui.adapter.ClickableAdapter
import ch.protonmail.libs.core.utils.onClick
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
internal class DrawerAdapter(
    onItemClick: (DrawerItemUiModel) -> Unit,
    private val onCreateLabel: () -> Unit,
    private val onCreateFolder: () -> Unit
) : BaseAdapter<DrawerItemUiModel, DrawerAdapter.ViewHolder<DrawerItemUiModel>>(
    itemsComparator = ModelsComparator,
    onItemClick = onItemClick
) {

    /** Select the given [item] and un-select all the others */
    fun setSelected(item: Primary) {
        items = items.map {
            if (it is Primary)
            // Select if this item is same as given item
                it.copyWithSelected(it == item)
            else it
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<DrawerItemUiModel> =
        parent.viewHolderForViewType(ViewType.fromInt(viewType))

    override fun getItemViewType(position: Int) = ViewType.fromModel(items[position]).intValue

    private object ModelsComparator : BaseAdapter.ItemsComparator<DrawerItemUiModel>() {

        /** Check if old [DrawerItemUiModel] and new [DrawerItemUiModel] are the same element */
        override fun areItemsTheSame(oldItem: DrawerItemUiModel, newItem: DrawerItemUiModel): Boolean {
            val newItemAsStatic = newItem as? Primary.Static
            val newItemAsLabel = newItem as? Primary.Label
            return when (oldItem) {
                is SectionName, is CreateItem -> oldItem == newItem
                is Primary -> when (oldItem) {
                    is Primary.Static -> oldItem.type == newItemAsStatic?.type
                    is Primary.Label -> oldItem.uiModel.labelId == newItemAsLabel?.uiModel?.labelId
                }
                is Footer -> true // We only have one footer
            }
        }
    }

    private fun <Model : DrawerItemUiModel> ViewGroup.viewHolderForViewType(viewType: ViewType): ViewHolder<Model> {
        val inflater = LayoutInflater.from(context)
        return when (viewType) {
            ViewType.SECTION_NAME -> {
                val binding = DrawerSectionNameItemBinding.inflate(inflater, this, false)
                SectionNameViewHolder(
                    binding = binding,
                    onCreateLabel = onCreateLabel,
                    onCreateFolder = onCreateFolder
                )
            }
            ViewType.STATIC -> {
                val binding = DrawerListItemBinding.inflate(inflater, this, false)
                StaticViewHolder(binding)
            }
            ViewType.LABEL -> {
                val binding = DrawerListItemBinding.inflate(inflater, this, false)
                LabelViewHolder(binding)
            }
            ViewType.CREATE_ITEM -> {
                val binding = DrawerListItemBinding.inflate(inflater, this, false)
                CreateItemViewHolder(binding)
            }
            ViewType.FOOTER -> {
                val binding = DrawerFooterBinding.inflate(inflater, this, false)
                FooterViewHolder(binding)
            }
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
        private val binding: DrawerSectionNameItemBinding,
        private val onCreateLabel: () -> Unit,
        private val onCreateFolder: () -> Unit,
    ) : ViewHolder<SectionName>(binding.root) {

        override fun onBind(item: SectionName) {
            super.onBind(item)
            binding.drawerSectionNameTextView.text = item.text
            binding.drawerSectionNameCreateButton.apply {
                isVisible = item.shouldShowCreateButton
                onClick {
                    when (item.type) {
                        SectionName.Type.FOLDER -> onCreateFolder()
                        SectionName.Type.LABEL -> onCreateLabel()
                        else -> {}
                    }
                }
            }
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

    /**
     * [ViewHolder] for [CreateItem] button
     * @see CreateItem.Folder
     * @see CreateItem.Label
     */
    private class CreateItemViewHolder(
        private val binding: DrawerListItemBinding
    ) : ViewHolder<CreateItem>(binding.root) {

        override fun onBind(item: CreateItem) {
            super.onBind(item)
            binding.drawerItemIconView.setImageResource(R.drawable.ic_plus)
            binding.drawerItemLabelTextView.apply {
                setText(item.textRes)
                setTextColor(getColor(R.color.text_weak))
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

    enum class ViewType(val intValue: Int) {
        SECTION_NAME(0),
        STATIC(1),
        LABEL(2),
        CREATE_ITEM(3),
        FOOTER(4);

        companion object {

            fun fromInt(intValue: Int): ViewType =
                values().first { it.intValue == intValue }

            fun fromModel(model: DrawerItemUiModel): ViewType =
                when (model) {
                    is SectionName -> SECTION_NAME
                    is Primary.Static -> STATIC
                    is Primary.Label -> LABEL
                    is CreateItem -> CREATE_ITEM
                    is Footer -> FOOTER
                }
        }
    }
}

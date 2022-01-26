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
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
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
import ch.protonmail.android.drawer.presentation.model.DrawerItemUiModel.SectionName.CreateButtonState
import ch.protonmail.android.utils.extensions.setNotificationIndicatorSize
import kotlinx.android.synthetic.main.drawer_list_item.view.*
import me.proton.core.presentation.ui.adapter.ClickableAdapter
import me.proton.core.presentation.ui.adapter.ProtonAdapter
import me.proton.core.presentation.utils.onClick

/**
 * Adapter for Drawer Items that support different View types
 */
internal class DrawerAdapter(
    onItemClick: (DrawerItemUiModel) -> Unit,
    private val onCreateLabel: () -> Unit,
    private val onCreateFolder: () -> Unit
) : ProtonAdapter<DrawerItemUiModel, Any, DrawerAdapter.ViewHolder<DrawerItemUiModel, Any>>(
    diffCallback = ModelsComparator,
    onItemClick = onItemClick
) {

    /** Select the given [item] and un-select all the others */
    fun setSelected(item: Primary) {
        val newItems = currentList.map {
            if (it is Primary)
            // Select if this item is same as given item
                it.copyWithSelected(it == item)
            else it
        }
        submitList(newItems)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder<DrawerItemUiModel, Any> =
        parent.viewHolderForViewType(ViewType.fromInt(viewType))

    override fun getItemViewType(position: Int) = ViewType.fromModel(getItem(position)).intValue

    private object ModelsComparator : DiffUtil.ItemCallback<DrawerItemUiModel>() {

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

        override fun areContentsTheSame(oldItem: DrawerItemUiModel, newItem: DrawerItemUiModel) =
            oldItem == newItem
    }

    private fun <Model : DrawerItemUiModel> ViewGroup.viewHolderForViewType(viewType: ViewType): ViewHolder<Model, Any> {
        val inflater = LayoutInflater.from(context)
        return when (viewType) {
            ViewType.SECTION_NAME -> {
                val binding = DrawerSectionNameItemBinding.inflate(inflater, this, false)
                SectionNameViewHolder(
                    binding = binding,
                    onItemClick,
                    onCreateLabel = onCreateLabel,
                    onCreateFolder = onCreateFolder
                )
            }
            ViewType.STATIC -> {
                val binding = DrawerListItemBinding.inflate(inflater, this, false)
                StaticViewHolder(binding, onItemClick)
            }
            ViewType.LABEL -> {
                val binding = DrawerListItemBinding.inflate(inflater, this, false)
                LabelViewHolder(binding, onItemClick)
            }
            ViewType.CREATE_ITEM -> {
                val binding = DrawerListItemBinding.inflate(inflater, this, false)
                CreateItemViewHolder(binding, onItemClick)
            }
            ViewType.FOOTER -> {
                val binding = DrawerFooterBinding.inflate(inflater, this, false)
                FooterViewHolder(binding, onItemClick)
            }
        } as ViewHolder<Model, Any>
    }

    /** Abstract ViewHolder for the Adapter */
    abstract class ViewHolder<Model : DrawerItemUiModel, ViewRef : Any>(
        view: ViewRef,
        onItemClick: (DrawerItemUiModel) -> Unit
    ) : ClickableAdapter.ViewHolder<Model, ViewRef>(view, onItemClick)

    private abstract class PrimaryViewHolder<P : Primary, ViewRef : Any>(
        view: ViewRef,
        onItemClick: (DrawerItemUiModel) -> Unit
    ): ViewHolder<P, ViewRef>(view, onItemClick) {

        override fun onBind(item: P, position: Int) {
            super.onBind(item, position)
            with(itemView) {
                drawer_item_selection_view.isVisible = item.selected
                drawer_item_notifications_text_view.isVisible = item.hasNotifications()
                drawer_item_notifications_text_view.text = item.notificationCount.formatToMax4chars()
                drawer_item_notifications_text_view.setNotificationIndicatorSize(item.notificationCount)
            }
        }

        private fun Int.formatToMax4chars(): String {
            val coerced = coerceAtMost(9999)
            return if (coerced == this) "$coerced"
            else "$coerced+"
        }
    }

    private class SectionNameViewHolder(
        private val binding: DrawerSectionNameItemBinding,
        onItemClick: (DrawerItemUiModel) -> Unit,
        private val onCreateLabel: () -> Unit,
        private val onCreateFolder: () -> Unit,
    ) : ViewHolder<SectionName, DrawerSectionNameItemBinding>(binding, onItemClick) {

        override fun onBind(item: SectionName, position: Int) {
            super.onBind(item, position)
            binding.drawerSectionNameTextView.setText(item.text)
            binding.drawerSectionNameCreateButton.apply {
                if (item.createButtonState is CreateButtonState.Shown) {
                    isVisible = true
                    contentDescription = getString(item.createButtonState.contentDescriptionRes)
                    onClick {
                        when (item.type) {
                            SectionName.Type.FOLDER -> onCreateFolder()
                            SectionName.Type.LABEL -> onCreateLabel()
                            else -> {}
                        }
                    }
                } else {
                    isVisible = false
                }
            }
        }
    }

    private class LabelViewHolder(
        private val binding: DrawerListItemBinding,
        onItemClick: (DrawerItemUiModel) -> Unit
    ) : PrimaryViewHolder<Primary.Label, DrawerListItemBinding>(binding, onItemClick) {

        override fun onBind(item: Primary.Label, position: Int) {
            super.onBind(item, position)
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

    private class StaticViewHolder(
        private val binding: DrawerListItemBinding,
        onItemClick: (DrawerItemUiModel) -> Unit
    ) : PrimaryViewHolder<Primary.Static, DrawerListItemBinding>(binding, onItemClick) {

        override fun onBind(item: Primary.Static, position: Int) {
            super.onBind(item, position)
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
        private val binding: DrawerListItemBinding,
        onItemClick: (DrawerItemUiModel) -> Unit
    ) : ViewHolder<CreateItem, DrawerListItemBinding>(binding, onItemClick) {

        override fun onBind(item: CreateItem, position: Int) {
            super.onBind(item, position)
            binding.drawerItemIconView.setImageResource(R.drawable.ic_plus)
            binding.drawerItemLabelTextView.apply {
                setText(item.textRes)
                setTextColor(getColor(R.color.text_weak))
            }
        }
    }

    private class FooterViewHolder(
        private val binding: DrawerFooterBinding,
        onItemClick: (DrawerItemUiModel) -> Unit
    ) : ViewHolder<Footer, DrawerFooterBinding>(binding, onItemClick) {

        override fun onBind(item: Footer, position: Int) {
            super.onBind(item, position)
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

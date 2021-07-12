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
package ch.protonmail.android.adapters

import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import ch.protonmail.android.R
import ch.protonmail.android.activities.navigation.LabelWithUnreadCounter
import ch.protonmail.android.drawer.presentation.model.DrawerItemUiModel
import ch.protonmail.android.drawer.presentation.model.DrawerItemUiModel.Footer
import ch.protonmail.android.drawer.presentation.model.DrawerItemUiModel.Primary
import ch.protonmail.android.drawer.presentation.model.DrawerItemUiModel.SectionName
import ch.protonmail.android.mapper.LabelUiModelMapper
import ch.protonmail.android.utils.extensions.setNotificationIndicatorSize
import ch.protonmail.libs.core.ui.adapter.BaseAdapter
import ch.protonmail.libs.core.ui.adapter.ClickableAdapter
import kotlinx.android.synthetic.main.drawer_list_item.view.*
import kotlinx.android.synthetic.main.drawer_section_name_item.view.*
import me.proton.core.presentation.utils.inflate
import me.proton.core.util.kotlin.invoke

// region constants
/** View types for Adapter */
private const val VIEW_TYPE_SECTION_NAME = 0
private const val VIEW_TYPE_STATIC = 1
private const val VIEW_TYPE_LABEL = 2
private const val VIEW_TYPE_FOOTER = 3
// endregion

/**
 * Adapter for Drawer Items that support different View types
 *
 * Inherit from [BaseAdapter]
 *
 * @author Davide Farella.
 */

internal class DrawerAdapter : BaseAdapter<
    DrawerItemUiModel, DrawerAdapter.ViewHolder<DrawerItemUiModel>
    >(ModelsComparator) {

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

    /** Abstract ViewHolder for the Adapter */
    abstract class ViewHolder<Model : DrawerItemUiModel>(itemView: View) :
        ClickableAdapter.ViewHolder<Model>(itemView)

    /** [ViewHolder] for [Primary] Item */
    private abstract class PrimaryViewHolder<P : Primary>(itemView: View) : ViewHolder<P>(itemView) {

        override fun onBind(item: P) = with(itemView) {
            super.onBind(item)
            drawer_item_selection_view.isVisible = item.selected
            drawer_item_notifications_text_view.isVisible = item.hasNotifications()
            drawer_item_notifications_text_view.text = item.notificationCount.toString()
            drawer_item_notifications_text_view.setNotificationIndicatorSize(item.notificationCount)
        }
    }

    /**
     * [ViewHolder] for [Primary.Static] Item
     * Inherit from [PrimaryViewHolder]
     */
    private class StaticViewHolder(itemView: View) : PrimaryViewHolder<Primary.Static>(itemView) {

        override fun onBind(item: Primary.Static) = with(itemView) {
            super.onBind(item)
            drawer_item_label_text_view.setText(item.labelRes)
            drawer_item_icon_view.setImageResource(item.iconRes)
            menuItem.tag = resources.getString(item.labelRes)
        }
    }

    /**
     * [ViewHolder] for [Primary.Label] Item
     * Inherit from [PrimaryViewHolder]
     */
    private class LabelViewHolder(itemView: View) : PrimaryViewHolder<Primary.Label>(itemView) {

        override fun onBind(item: Primary.Label) = with(itemView) {
            super.onBind(item)
            drawer_item_label_text_view.text = item.uiModel.name
            drawer_item_icon_view.setColorFilter(item.uiModel.color)
            drawer_item_icon_view.setImageResource(item.uiModel.image)
            drawer_item_label_text_view.tag = item.uiModel.name
        }
    }

    /** [ViewHolder] for [Footer] */
    private class FooterViewHolder(itemView: View) : ViewHolder<Footer>(itemView) {

        override fun onBind(item: Footer) {
            super.onBind(item)
            val textView = itemView as TextView
            textView.text = item.text
        }
    }

    /** [ViewHolder] for [SectionName] */
    private class SectionNameViewHolder(itemView: View) : ViewHolder<SectionName>(itemView) {

        override fun onBind(item: SectionName) {
            super.onBind(item)
            itemView.text.text = item.text
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

    /** @return [LayoutRes] for the given [viewType] */
    private fun layoutForViewType(viewType: Int) = when (viewType) {
        VIEW_TYPE_SECTION_NAME -> R.layout.drawer_section_name_item
        VIEW_TYPE_STATIC, VIEW_TYPE_LABEL -> R.layout.drawer_list_item
        VIEW_TYPE_FOOTER -> R.layout.drawer_footer
        else -> throw IllegalArgumentException("View type not found: '$viewType'")
    }

    /** @return a [ViewHolder] for the given [viewType] */
    private fun <Model : DrawerItemUiModel> ViewGroup.viewHolderForViewType(viewType: Int): ViewHolder<Model> {
        val view = inflate(layoutForViewType(viewType))
        @Suppress("UNCHECKED_CAST") // Type cannot be checked since is in invariant position
        return when (viewType) {
            VIEW_TYPE_SECTION_NAME -> SectionNameViewHolder(view)
            VIEW_TYPE_STATIC -> StaticViewHolder(view)
            VIEW_TYPE_LABEL -> LabelViewHolder(view)
            VIEW_TYPE_FOOTER -> FooterViewHolder(view)
            else -> throw IllegalArgumentException("View type not found: '$viewType'")
        } as ViewHolder<Model>
    }
}

/**
 * @return [List] of [DrawerItemUiModel.Primary.Label] created from a [List] of
 * [LabelWithUnreadCounter]
 *
 * This function is an helper for Java, since it would be annoying to implement it in Java.
 */
internal fun mapLabelsToDrawerLabels(
    mapper: LabelUiModelMapper,
    labels: List<LabelWithUnreadCounter>
): List<Primary.Label> = labels.map { labelWithUnread ->
    val uiModel = mapper { labelWithUnread.label.toUiModel() }
    Primary.Label(uiModel, labelWithUnread.unreadCount)
}


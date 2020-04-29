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
@file:JvmName("DrawerAdapterHelper") // Set name of the file use functions in this file from Java

package ch.protonmail.android.adapters

import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.core.view.isVisible
import ch.protonmail.android.R
import ch.protonmail.android.activities.navigation.LabelWithUnreadCounter
import ch.protonmail.android.mapper.LabelUiModelMapper
import ch.protonmail.android.uiModel.DrawerItemUiModel
import ch.protonmail.android.uiModel.DrawerItemUiModel.Divider
import ch.protonmail.android.uiModel.DrawerItemUiModel.Header
import ch.protonmail.android.uiModel.DrawerItemUiModel.Primary
import ch.protonmail.android.utils.extensions.inflate
import ch.protonmail.android.utils.extensions.setNotificationIndicatorSize
import ch.protonmail.android.views.DrawerHeaderView
import ch.protonmail.libs.core.arch.invoke
import ch.protonmail.libs.core.ui.adapter.BaseAdapter
import ch.protonmail.libs.core.ui.adapter.ClickableAdapter
import kotlinx.android.synthetic.main.drawer_list_item.view.*

// region constants
/** View types for Adapter */
private const val VIEW_TYPE_HEADER = 0
private const val VIEW_TYPE_DIVIDER = 1
private const val VIEW_TYPE_STATIC = 2
private const val VIEW_TYPE_LABEL = 3
// endregion

/**
 * Adapter for Drawer Items that support different View types
 * Inherit from [BaseAdapter]
 *
 * @author Davide Farella.
 */
internal class DrawerAdapter(onItemClick: (DrawerItemUiModel) -> Unit) : BaseAdapter<
        DrawerItemUiModel, DrawerAdapter.ViewHolder<DrawerItemUiModel>
    >(ModelsComparator, onItemClick) {

    /** Select the given [item] and un-select all the others */
    fun setSelected( item: Primary ) {
        items = items.map {
            if ( it is Primary )
                // Select if this item is same as given item
                it.copyWithSelected( it == item )
            else it
        }
    }

    /** @return a [ViewHolder] for the given [viewType] */
    override fun onCreateViewHolder( parent: ViewGroup, viewType: Int ): ViewHolder<DrawerItemUiModel> =
            parent.viewHolderForViewType( viewType )

    /** @return [Int] that identifies the View type for the Item at the given [position] */
    override fun getItemViewType( position: Int ) = items[position].viewType

    /** A [BaseAdapter.ItemsComparator] for the Adapter */
    private object ModelsComparator : BaseAdapter.ItemsComparator<DrawerItemUiModel>() {

        /** Check if old [DrawerItemUiModel] and new [DrawerItemUiModel] are the same element */
        override fun areItemsTheSame( oldItem: DrawerItemUiModel, newItem: DrawerItemUiModel ): Boolean {
            val newItemAsStatic = newItem as? Primary.Static
            val newItemAsLabel = newItem as? Primary.Label
            return when ( oldItem ) {
                is Header -> true // We only have one header
                Divider -> true // Singleton, always animate the object when possible
                is Primary -> when( oldItem ){
                    is Primary.Static -> oldItem.type == newItemAsStatic?.type
                    is Primary.Label -> oldItem.uiModel.labelId == newItemAsLabel?.uiModel?.labelId
                }
            }
        }
    }

    /** Abstract ViewHolder for the Adapter */
    abstract class ViewHolder<Model: DrawerItemUiModel>( itemView: View ) :
            ClickableAdapter.ViewHolder<Model>( itemView )

    /** [ViewHolder] for [Header] */
    private class HeaderViewHolder( itemView: View ) : ViewHolder<Header>( itemView ) {
        override fun onBind( item: Header ) = with( itemView as DrawerHeaderView ) {
            super.onBind( item )
            setUser( item.name, item.email )
            refresh( item.snoozeEnabled )
        }
    }

    /** [ViewHolder] for [Primary] Item */
    private abstract class PrimaryViewHolder<P : Primary>( itemView: View ) : ViewHolder<P>( itemView ) {
        override fun onBind( item: P ) = with( itemView ) {
            super.onBind( item )
            selection.isVisible = item.selected
            notifications.isVisible = item.hasNotifications()
            notifications.text = item.notificationCount.toString()
            notifications.setNotificationIndicatorSize(item.notificationCount)
        }
    }

    /**
     * [ViewHolder] for [Primary.Static] Item
     * Inherit from [PrimaryViewHolder]
     */
    private class StaticViewHolder( itemView: View ) : PrimaryViewHolder<Primary.Static>( itemView ) {
        override fun onBind( item: Primary.Static ) = with( itemView ) {
            super.onBind( item )
            label.setText( item.labelRes )
            icon.setImageResource( item.iconRes )
        }
    }

    /**
     * [ViewHolder] for [Primary.Label] Item
     * Inherit from [PrimaryViewHolder]
     */
    private class LabelViewHolder( itemView: View ) : PrimaryViewHolder<Primary.Label>( itemView ) {
        override fun onBind( item: Primary.Label ) = with( itemView ) {
            super.onBind( item )
            label.text = item.uiModel.name
            icon.setColorFilter( item.uiModel.color )
            icon.setImageResource( item.uiModel.image )
        }
    }

    /** [ViewHolder] for [Divider] */
    private class DividerViewHolder( itemView: View ) : ViewHolder<Divider>( itemView )

    /** @return [Int] view type for the receiver [DrawerItemUiModel] */
    private val DrawerItemUiModel.viewType: Int get() {
        return when ( this ) {
            is Header -> VIEW_TYPE_HEADER
            Divider -> VIEW_TYPE_DIVIDER
            is Primary -> when ( this ) {
                is Primary.Static -> VIEW_TYPE_STATIC
                is Primary.Label -> VIEW_TYPE_LABEL
            }
        }
    }

    /** @return [LayoutRes] for the given [viewType] */
    private fun layoutForViewType( viewType: Int ) = when ( viewType ) {
//        VIEW_TYPE_HEADER -> R.layout.drawer_header
        VIEW_TYPE_DIVIDER -> R.layout.drawer_list_item_divider
        VIEW_TYPE_STATIC, VIEW_TYPE_LABEL -> R.layout.drawer_list_item
        else -> throw IllegalArgumentException( "View type not found: '$viewType'" )
    }

    /** @return a [ViewHolder] for the given [viewType] */
    private fun <Model: DrawerItemUiModel> ViewGroup.viewHolderForViewType( viewType: Int ): ViewHolder<Model> {
        val view = inflate( layoutForViewType( viewType ) )
        @Suppress("UNCHECKED_CAST") // Type cannot be checked since is in invariant position
        return when ( viewType ) {
            VIEW_TYPE_HEADER -> HeaderViewHolder(view)
            VIEW_TYPE_DIVIDER -> DividerViewHolder(view)
            VIEW_TYPE_STATIC -> StaticViewHolder(view)
            VIEW_TYPE_LABEL -> LabelViewHolder(view)
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
) : List<Primary.Label> {
    return labels.map { labelWithUnread ->
        val uiModel = mapper { labelWithUnread.label.toUiModel() }
        Primary.Label( uiModel, labelWithUnread.unreadCount )
    }
}

/**
 * @return [List] of [DrawerItemUiModel] injecting [unread] as
 * [DrawerItemUiModel.Primary.notificationCount]
 *
 * @receiver [List] of [DrawerItemUiModel] to edit
 *
 * @param unread [Map] associating type's id to the count of unread Messages
 * @see DrawerItemUiModel.Primary.Static.Type.itemId
 */
internal fun List<DrawerItemUiModel>.setUnreadLocations(
        unread: Map<Int, Int>
) : List<DrawerItemUiModel> {
    return map { item ->
        if (item is Primary.Static) {
            // Get unread count by id of item's type from unread
            val unreadCount = unread.getOrElse(item.type.itemId) { 0 }
            // Update the notificationCount for the item
            item.copyWithNotificationCount(unreadCount)
        }
        else item
    }
}

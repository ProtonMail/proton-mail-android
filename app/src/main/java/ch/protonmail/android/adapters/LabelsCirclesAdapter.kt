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
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.R
import ch.protonmail.android.uiModel.LabelUiModel
import ch.protonmail.android.utils.extensions.inflate
import ch.protonmail.android.utils.extensions.truncateToLength
import ch.protonmail.libs.core.ui.adapter.BasePagedAdapter
import ch.protonmail.libs.core.ui.adapter.SelectableAdapter
import kotlinx.android.synthetic.main.labels_circle_list_item.view.*

/**
 * A [RecyclerView.Adapter] for show `Labels` or `Folders`
 * Inherit from [BasePagedAdapter]
 * Implement [SelectableAdapter]
 *
 * @author Davide Farella
 */
internal class LabelsCirclesAdapter :
        BasePagedAdapter<LabelUiModel, LabelsCirclesAdapter.ViewHolder>( LabelsComparator ),
        SelectableAdapter<LabelUiModel, LabelsCirclesAdapter.ViewHolder> {

    /** A lambda that will be triggered when an item is clicked */
    override var onItemSelect: (LabelUiModel, isSelected: Boolean) -> Unit = { _, _ -> }

    /** @return new instance of [LabelsCirclesAdapter.ViewHolder] */
    override fun onCreateViewHolder( parent: ViewGroup, viewType: Int ) =
            ViewHolder( parent.inflate( R.layout.labels_circle_list_item ) )

    /** A [DiffUtil.ItemCallback] for [LabelsCirclesAdapter] */
    private object LabelsComparator : DiffUtil.ItemCallback<LabelUiModel>() {

        /** @see DiffUtil.ItemCallback.areItemsTheSame */
        override fun areItemsTheSame( oldItem: LabelUiModel, newItem: LabelUiModel ) =
                oldItem.labelId == newItem.labelId

        /** @see DiffUtil.ItemCallback.areContentsTheSame */
        override fun areContentsTheSame( oldItem: LabelUiModel, newItem: LabelUiModel ) =
                oldItem == newItem
    }

    /**
     * A [RecyclerView.ViewHolder] for [LabelsCirclesAdapter]
     * Inherit from [SelectableAdapter.ViewHolder]
     */
    internal class ViewHolder( itemView: View ) :
            SelectableAdapter.ViewHolder<LabelUiModel>( itemView ) {

        /** Setup [LabelUiModel] into [itemView] */
        @Suppress("RemoveExplicitTypeArguments") // `with` needs to always return `Unit`
        override fun onBind( item: LabelUiModel ) = with<View, Unit>( itemView ) {
            super.onBind( item )

            // Selection listener
            label_check.setOnClickListener {
                toggleCheck()
                setSelected( item, !item.isChecked )
            }

            // Set View
            label_name.text = item.name.truncateToLength( 15 )
            label_edit.setImageResource( item.image )
            label_color.setColorFilter( item.color )
            label_check.isChecked = item.isChecked
        }

        /** Toggle the check state of [View.label_check] */
        private fun toggleCheck() {
            itemView.label_check.toggle()
        }
    }
}

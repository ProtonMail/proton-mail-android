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
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.R
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.uiModel.LabelUiModel
import ch.protonmail.android.utils.extensions.truncateToLength
import ch.protonmail.libs.core.ui.adapter.BasePagedAdapter
import ch.protonmail.libs.core.ui.adapter.SelectableAdapter
import com.google.android.material.checkbox.MaterialCheckBox
import kotlinx.android.synthetic.main.labels_list_item.view.*
import me.proton.core.presentation.utils.inflate

/**
 * A [RecyclerView.Adapter] for show `Labels` or `Folders`
 * Inherit from [BasePagedAdapter]
 * Implement [SelectableAdapter]
 *
 * @author Davide Farella
 */
internal class LabelsAdapter :
    BasePagedAdapter<LabelUiModel, LabelsAdapter.ViewHolder>(LabelsComparator),
    SelectableAdapter<LabelUiModel, LabelsAdapter.ViewHolder> {

    override var onItemSelect: (LabelUiModel, isSelected: Boolean) -> Unit = { _, _ -> }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ViewHolder(parent.inflate(R.layout.labels_list_item))

    private object LabelsComparator : DiffUtil.ItemCallback<LabelUiModel>() {

        override fun areItemsTheSame(oldItem: LabelUiModel, newItem: LabelUiModel) =
            oldItem.labelId == newItem.labelId

        override fun areContentsTheSame(oldItem: LabelUiModel, newItem: LabelUiModel) =
            oldItem == newItem
    }

    /**
     * A [RecyclerView.ViewHolder] for [LabelsAdapter]
     * Inherit from [SelectableAdapter.ViewHolder]
     */
    internal class ViewHolder(itemView: View) :
        SelectableAdapter.ViewHolder<LabelUiModel>(itemView) {

        @Suppress("RemoveExplicitTypeArguments") // `with` needs to always return `Unit`
        override fun onBind(item: LabelUiModel) = with<View, Unit>(itemView) {
            super.onBind(item)

            val check = itemView.findViewById<MaterialCheckBox>(R.id.label_check)
            val color = itemView.findViewById<ImageView>(R.id.label_color)
            val name = itemView.findViewById<TextView>(R.id.label_name)

            // Selection listener
            check.setOnClickListener {
                itemView.label_check.toggle()
                setSelected(item, !item.isChecked)
            }

            // Set View
            if (item.type == LabelType.MESSAGE_LABEL) {
                color.layoutParams.height = resources.getDimensionPixelSize(R.dimen.padding_l)
                color.layoutParams.width = resources.getDimensionPixelSize(R.dimen.padding_l)
                color.setImageDrawable(getDrawable(R.drawable.circle_labels_selection))
            } else if (item.type == LabelType.FOLDER) {
                color.layoutParams.height = resources.getDimensionPixelSize(R.dimen.padding_xl)
                color.layoutParams.width = resources.getDimensionPixelSize(R.dimen.padding_xl)
                color.setImageDrawable(getDrawable(R.drawable.ic_folder_filled))
            }
            color.setColorFilter(item.color)
            name.text = item.name.truncateToLength(15)
            check.isChecked = item.isChecked
        }
    }
}

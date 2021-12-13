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

package ch.protonmail.android.labels.presentation.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import ch.protonmail.android.R
import ch.protonmail.android.databinding.LabelsListItemBinding
import ch.protonmail.android.labels.presentation.model.LabelsManagerItemUiModel
import ch.protonmail.android.labels.presentation.model.LabelsManagerItemUiModel.Folder
import me.proton.core.presentation.ui.adapter.ClickableAdapter
import me.proton.core.presentation.ui.adapter.ProtonAdapter
import me.proton.core.presentation.utils.onClick

class LabelsManagerAdapter(
    override val onItemClick: (LabelsManagerItemUiModel) -> Unit,
    private val onItemCheck: (LabelsManagerItemUiModel, isChecked: Boolean) -> Unit,
    private val onItemEditClick: (LabelsManagerItemUiModel) -> Unit
) : ProtonAdapter<LabelsManagerItemUiModel, LabelsListItemBinding, LabelsManagerAdapter.ViewHolder>(
    diffCallback = LabelsManagerItemUiModel.DiffCallback
) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = LabelsListItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(
            binding = binding,
            onItemClick = onItemClick,
            onItemCheck = onItemCheck,
            onItemEditClick = onItemEditClick
        )
    }

    class ViewHolder(
        binding: LabelsListItemBinding,
        onItemClick: (LabelsManagerItemUiModel) -> Unit,
        private val onItemCheck: (LabelsManagerItemUiModel, isChecked: Boolean) -> Unit,
        private val onItemEditClick: (LabelsManagerItemUiModel) -> Unit
    ) : ClickableAdapter.ViewHolder<LabelsManagerItemUiModel, LabelsListItemBinding>(binding, onItemClick) {

        override fun onBind(item: LabelsManagerItemUiModel, position: Int) {
            super.onBind(item, position)

            viewRef.apply {
                labelIconImageView.apply {
                    if (item is Folder) applyMarginForSubFolders(item.folderLevel)
                    setImageResource(item.icon.drawableRes)
                    setColorFilter(item.icon.colorInt)
                    contentDescription = getString(item.icon.contentDescriptionRes)
                }
                labelNameTextView.text = item.name
                labelCheckBox.isChecked = item.isChecked
                labelCheckBox.onClick { onItemCheck(item, labelCheckBox.isChecked) }
                labelEditImageButton.onClick { onItemEditClick(item) }
            }
        }

        private fun View.applyMarginForSubFolders(folderLevel: Int) {
            (layoutParams as ConstraintLayout.LayoutParams).marginStart =
                folderLevel * context.resources.getDimensionPixelSize(R.dimen.gap_large)
        }
    }
}

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

import android.os.Bundle
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.R
import ch.protonmail.android.databinding.ActivityParentFolderPickerBinding
import ch.protonmail.android.databinding.ItemParentPickerFolderBinding
import ch.protonmail.android.labels.presentation.model.ParentFolderPickerAction
import ch.protonmail.android.labels.presentation.model.ParentFolderPickerItemUiModel
import ch.protonmail.android.labels.presentation.model.ParentFolderPickerState
import ch.protonmail.android.labels.presentation.viewmodel.ParentFolderPickerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import me.proton.core.presentation.ui.adapter.ProtonAdapter

@AndroidEntryPoint
class ParentFolderPickerActivity : AppCompatActivity() {

    private val viewModel: ParentFolderPickerViewModel by viewModels()
    private lateinit var binding: ActivityParentFolderPickerBinding

    private val adapter = ProtonAdapter(
        diffCallback = ParentFolderPickerItemUiModel.DiffCallback,
        getView = { parent, inflater ->
            ItemParentPickerFolderBinding.inflate(inflater, parent, false)
        },
        onBind = { model ->
            when (model) {
                is ParentFolderPickerItemUiModel.Folder -> {
                    setMarginFor(folderLevel = model.folderLevel)
                    parentPickerFolderIconImageView.apply {
                        isVisible = true
                        setColorFilter(model.colorInt)
                        setImageResource(model.icon.drawableRes)
                        contentDescription = getString(model.icon.contentDescriptionRes)
                    }
                    parentPickerFolderNameTextView.text = model.name
                }
                is ParentFolderPickerItemUiModel.None -> {
                    parentPickerFolderIconImageView.isVisible = false
                    parentPickerFolderNameTextView.setText(R.string.x_none)
                }
            }
            parentPickerFolderNameTextView.setCheckmark(model.isSelected)
        },
        onItemClick = { model ->
            viewModel.process(ParentFolderPickerAction.SetSelected(model.id))
        }
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParentFolderPickerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.parentPickerRecyclerView.apply {
            layoutManager = LinearLayoutManager(context)
            this.adapter = this@ParentFolderPickerActivity.adapter
        }

        viewModel.state.onEach { state ->
            when (state) {
                is ParentFolderPickerState.Editing -> onEditingState(state)
                is ParentFolderPickerState.SavingAndClose -> onSavingAndCloseState(state)
            }
        }.launchIn(lifecycleScope)
    }

    private fun onEditingState(state: ParentFolderPickerState.Editing) {
        adapter.submitList(state.items)
    }

    private fun onSavingAndCloseState(state: ParentFolderPickerState.SavingAndClose) {
        TODO("set result and finish")
    }
}

private fun ItemParentPickerFolderBinding.setMarginFor(folderLevel: Int) {
    (root.layoutParams as RecyclerView.LayoutParams).marginStart =
        folderLevel * root.context.resources.getDimensionPixelSize(R.dimen.gap_large)
}

private fun TextView.setCheckmark(isSelected: Boolean) {
    val drawable = R.drawable.ic_check.takeIf { isSelected } ?: 0
    setCompoundDrawablesRelativeWithIntrinsicBounds(0, 0, drawable, 0)
}

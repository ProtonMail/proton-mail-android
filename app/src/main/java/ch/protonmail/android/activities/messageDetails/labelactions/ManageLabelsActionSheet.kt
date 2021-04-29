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

package ch.protonmail.android.activities.messageDetails.labelactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import ch.protonmail.android.R
import ch.protonmail.android.databinding.FragmentManageLabelsActionSheetBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

/**
 * Actions sheet used to manage labels and folders (a folder is a type of label with id=1)
 */
@AndroidEntryPoint
class ManageLabelsActionSheet : BottomSheetDialogFragment() {

    private val viewModel: ManageLabelsActionSheetViewModel by viewModels()

    private var _binding: FragmentManageLabelsActionSheetBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentManageLabelsActionSheetBinding.inflate(inflater)

        val actionSheetType = arguments?.getSerializable(EXTRA_ARG_ACTION_SHEET_TYPE) as Type

        with(binding.actionSheetHeader) {
            val title = if (actionSheetType == Type.FOLDER) {
                getString(R.string.move_to)
            } else {
                getString(R.string.label_as)
            }
            setTitle(title)

            setOnCloseClickListener {
                dismiss()
            }

            if (actionSheetType == Type.LABEL) {
                setRightActionClickListener {
                    viewModel.onDoneClicked(binding.switchLabelsSheetArchive.isChecked)
                }
            }
        }
        val manageLabelsActionAdapter = ManageLabelsActionAdapter(::onLabelClicked)
        with(binding.recyclerviewLabelsSheet) {
            layoutManager = LinearLayoutManager(context)
            adapter = manageLabelsActionAdapter
        }

        with (binding.layoutLabelsSheetArchiveSwitch) {
            isVisible = actionSheetType == Type.LABEL
            setOnClickListener {
                binding.switchLabelsSheetArchive.toggle()
            }

            binding.textViewLabelsSheetNewLabel.apply {
                isVisible = actionSheetType == Type.LABEL
                setOnClickListener {
                    // TODO: Link it to appropriate setting section for adding new Label
                }
            }

            binding.textViewLabelsSheetNewFolder.apply {
                isVisible = actionSheetType == Type.FOLDER
                setOnClickListener {
                    // TODO: Link it to appropriate setting section for adding new Folder
                }
            }

            viewModel.labels
                .onEach { manageLabelsActionAdapter.submitList(it) }
                .launchIn(lifecycleScope)

            viewModel.actionsResult
                .onEach { processActionResult(it) }
                .launchIn(lifecycleScope)

            return binding.root
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun processActionResult(result: ManageLabelActionResult) {
        Timber.v("Result received $result")
        when (result) {
            is ManageLabelActionResult.LabelsSuccessfullySaved,
            is ManageLabelActionResult.MessageSuccessfullyMoved -> dismiss()
            is ManageLabelActionResult.ErrorLabelsThresholdReached ->
                showApplicableLabelsThresholdError(result.maxAllowedCount)
            else -> {
                Timber.v("Result $result")
            }
        }
    }

    private fun onLabelClicked(model: ManageLabelItemUiModel) {
        viewModel.onLabelClicked(model)
    }

    private fun showApplicableLabelsThresholdError(maxLabelsAllowed: Int) {
        Toast.makeText(
            context, getString(R.string.max_labels_selected, maxLabelsAllowed),
            Toast.LENGTH_SHORT
        ).show()
    }

    companion object {

        const val EXTRA_ARG_MESSAGE_CHECKED_LABELS = "extra_arg_message_checked_labels"
        const val EXTRA_ARG_MESSAGES_IDS = "extra_arg_messages_ids"
        const val EXTRA_ARG_ACTION_SHEET_TYPE = "extra_arg_action_sheet_type"
        const val EXTRA_ARG_CURRENT_FOLDER_LOCATION_ID = "extra_arg_current_folder_location_id"

        fun newInstance(
            checkedLabels: List<String>,
            messageIds: List<String>,
            labelActionSheetType: Type = Type.LABEL,
            currentFolderLocationId: Int
        ): ManageLabelsActionSheet {

            return ManageLabelsActionSheet().apply {
                arguments = bundleOf(
                    EXTRA_ARG_MESSAGE_CHECKED_LABELS to checkedLabels,
                    EXTRA_ARG_MESSAGES_IDS to messageIds,
                    EXTRA_ARG_ACTION_SHEET_TYPE to labelActionSheetType,
                    EXTRA_ARG_CURRENT_FOLDER_LOCATION_ID to currentFolderLocationId
                )
            }
        }
    }

    enum class Type(val typeInt: Int) {
        LABEL(0), // default
        FOLDER(1)
    }
}

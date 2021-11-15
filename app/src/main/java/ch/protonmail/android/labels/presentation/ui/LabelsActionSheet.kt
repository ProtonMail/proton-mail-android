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

import android.content.Intent
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
import ch.protonmail.android.databinding.FragmentLabelsActionSheetBinding
import ch.protonmail.android.details.presentation.MessageDetailsActivity
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.labels.domain.model.ManageLabelActionResult
import ch.protonmail.android.labels.presentation.EXTRA_MANAGE_FOLDERS
import ch.protonmail.android.labels.presentation.LabelsActionAdapter
import ch.protonmail.android.labels.presentation.LabelsActionSheetViewModel
import ch.protonmail.android.labels.presentation.LabelsManagerActivity
import ch.protonmail.android.labels.presentation.model.LabelActonItemUiModel
import ch.protonmail.android.ui.actionsheet.model.ActionSheetTarget
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.ui.dialogs.DialogUtils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

/**
 * Actions sheet used to manage labels and folders.
 */
@AndroidEntryPoint
class LabelsActionSheet : BottomSheetDialogFragment() {

    private val viewModel: LabelsActionSheetViewModel by viewModels()

    private var _binding: FragmentLabelsActionSheetBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLabelsActionSheetBinding.inflate(inflater)

        val actionSheetType = arguments?.getSerializable(EXTRA_ARG_ACTION_SHEET_TYPE) as LabelType

        with(binding.actionSheetHeader) {
            val title = if (actionSheetType == LabelType.FOLDER) {
                getString(R.string.move_to)
            } else {
                getString(R.string.label_as)
            }
            setTitle(title)

            setOnCloseClickListener {
                dismiss()
            }

            if (actionSheetType == LabelType.MESSAGE_LABEL) {
                setRightActionClickListener {
                    viewModel.onDoneClicked(binding.labelsSheetArchiveSwitch.isChecked)
                }
            }
        }
        val manageLabelsActionAdapter = LabelsActionAdapter(::onLabelClicked)
        with(binding.labelsSheetRecyclerview) {
            layoutManager = LinearLayoutManager(context)
            adapter = manageLabelsActionAdapter
        }

        with(binding.labelsSheetArchiveSwitchLayout) {
            isVisible = actionSheetType == LabelType.MESSAGE_LABEL
            setOnClickListener {
                binding.labelsSheetArchiveSwitch.toggle()
            }
        }

        binding.labelsSheetNewLabelTextView.apply {
            isVisible = actionSheetType == LabelType.MESSAGE_LABEL
            setOnClickListener {
                val createLabelIntent = AppUtil.decorInAppIntent(
                    Intent(requireContext(), LabelsManagerActivity::class.java)
                )
                startActivity(createLabelIntent)
            }
        }

        binding.labelsSheetNewFolderTextView.apply {
            isVisible = actionSheetType == LabelType.FOLDER
            setOnClickListener {
                val createFolderIntent = AppUtil.decorInAppIntent(
                    Intent(requireContext(), LabelsManagerActivity::class.java)
                ).putExtra(EXTRA_MANAGE_FOLDERS, true)
                startActivity(createFolderIntent)
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

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun processActionResult(result: ManageLabelActionResult) {
        Timber.v("Result received $result")
        when (result) {
            is ManageLabelActionResult.LabelsSuccessfullySaved -> dismiss()
            is ManageLabelActionResult.MessageSuccessfullyMoved ->
                handleDismissBehavior(result.shouldDismissBackingActivity)
            is ManageLabelActionResult.ErrorLabelsThresholdReached ->
                showApplicableLabelsThresholdError(result.maxAllowedCount)
            is ManageLabelActionResult.ErrorUpdatingLabels ->
                showCouldNotCompleteActionError()
            is ManageLabelActionResult.ErrorMovingToFolder ->
                showCouldNotCompleteActionError()
            else -> {
                Timber.v("Result $result")
            }
        }
    }

    private fun onLabelClicked(model: LabelActonItemUiModel) {
        viewModel.onLabelClicked(model)
    }

    private fun showApplicableLabelsThresholdError(maxLabelsAllowed: Int) {
        DialogUtils.showInfoDialog(
            context = requireNotNull(context),
            title = requireNotNull(context?.getString(R.string.labels_selected_limit_reached)),
            message = requireNotNull(context?.getString(R.string.max_labels_selected, maxLabelsAllowed)),
            okListener = { }
        )
    }

    private fun showCouldNotCompleteActionError() {
        Toast.makeText(
            context,
            context?.getString(R.string.could_not_complete_action),
            Toast.LENGTH_SHORT
        ).show()
    }

    private fun handleDismissBehavior(dismissBackingActivity: Boolean) {
        dismiss()
        if (dismissBackingActivity) {
            popBackDetailsActivity()
        }
    }

    private fun popBackDetailsActivity() = (activity as? MessageDetailsActivity)?.onBackPressed()

    companion object {

        const val EXTRA_ARG_MESSAGES_IDS = "extra_arg_messages_ids"
        const val EXTRA_ARG_ACTION_SHEET_TYPE = "extra_arg_action_sheet_type"
        const val EXTRA_ARG_CURRENT_FOLDER_LOCATION_ID = "extra_arg_current_folder_location_id"
        const val EXTRA_ARG_ACTION_TARGET = "extra_arg_labels_action_sheet_actions_target"

        fun newInstance(
            messageIds: List<String>,
            currentFolderLocationId: Int,
            labelType: LabelType = LabelType.MESSAGE_LABEL,
            actionSheetTarget: ActionSheetTarget
        ): LabelsActionSheet {

            return LabelsActionSheet().apply {
                arguments = bundleOf(
                    EXTRA_ARG_MESSAGES_IDS to messageIds,
                    EXTRA_ARG_ACTION_SHEET_TYPE to labelType,
                    EXTRA_ARG_CURRENT_FOLDER_LOCATION_ID to currentFolderLocationId,
                    EXTRA_ARG_ACTION_TARGET to actionSheetTarget
                )
            }
        }
    }

}

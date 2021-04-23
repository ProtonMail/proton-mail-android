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

@AndroidEntryPoint
class ManageLabelsActionSheet : BottomSheetDialogFragment() {

    private val viewModel: ManageLabelsActionSheetViewModel by viewModels()

    private var _binding: FragmentManageLabelsActionSheetBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentManageLabelsActionSheetBinding.inflate(inflater)

        with(binding.actionSheetHeader) {
            setTitle(getString(R.string.label_as))
            setRightActionClickListener {
                viewModel.onDoneClicked()
            }
            setOnCloseClickListener {
                dismiss()
            }
        }
        val manageLabelsActionAdapter = ManageLabelsActionAdapter(::onLabelClicked)
        with(binding.recyclerviewLabelsSheet) {
            layoutManager = LinearLayoutManager(context)
            adapter = manageLabelsActionAdapter
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
        const val EXTRA_ARG_MESSAGE_ID = "extra_arg_message_id"

        fun newInstance(checkedLabels: List<String>, messageId: String): ManageLabelsActionSheet {

            return ManageLabelsActionSheet().apply {
                arguments = bundleOf(
                    EXTRA_ARG_MESSAGE_CHECKED_LABELS to checkedLabels,
                    EXTRA_ARG_MESSAGE_ID to messageId
                )
            }
        }
    }
}

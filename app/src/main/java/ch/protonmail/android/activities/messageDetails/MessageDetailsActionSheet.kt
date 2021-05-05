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

package ch.protonmail.android.activities.messageDetails

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import ch.protonmail.android.activities.messageDetails.viewmodel.MessageDetailsViewModel
import ch.protonmail.android.databinding.FragmentMessageDetailsActionSheetBinding
import ch.protonmail.android.databinding.LayoutMessageDetailsActionsSheetButtonsBinding
import ch.protonmail.android.databinding.LayoutMessageDetailsActionsSheetHeaderBinding
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber

/**
 * Fragment popping up with actions for message details screen.
 */
@AndroidEntryPoint
class MessageDetailsActionSheet : BottomSheetDialogFragment() {

    private val viewModel: MessageDetailsViewModel by activityViewModels()

    private var _binding: FragmentMessageDetailsActionSheetBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentMessageDetailsActionSheetBinding.inflate(inflater)

        setupHeaderBindings(binding.includeLayoutActionSheetHeader, arguments)
        setupMainButtonsBindings(binding.includeLayoutActionSheetButtons, viewModel)
        setupOtherButtonsBindings(binding, viewModel)

        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = super.onCreateDialog(savedInstanceState)
        bottomSheetDialog.setOnShowListener { dialogInterface ->
            val dialog = dialogInterface as BottomSheetDialog
            val bottomSheet: View? = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet)
            if (bottomSheet != null) {
                val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
                bottomSheetBehavior.addBottomSheetCallback(
                    object : BottomSheetBehavior.BottomSheetCallback() {
                        override fun onStateChanged(bottomSheet: View, newState: Int) {
                            Timber.v("State changed to $newState")
                            if (newState == STATE_EXPANDED) {
                                setCloseIconVisibility(true)
                            } else {
                                setCloseIconVisibility(false)
                            }
                        }

                        override fun onSlide(bottomSheet: View, slideOffset: Float) {
                            Timber.v("onSlide to offset $slideOffset")
                        }
                    }
                )
            }
        }
        return bottomSheetDialog
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupHeaderBindings(
        binding: LayoutMessageDetailsActionsSheetHeaderBinding,
        arguments: Bundle?
    ) = with(binding) {
        val title = arguments?.getString(EXTRA_ARG_TITLE)
        if (!title.isNullOrEmpty()) {
            detailsActionsTitleTextView.text = title
        }
        val subtitle = arguments?.getString(EXTRA_ARG_SUBTITLE)
        if (!subtitle.isNullOrEmpty()) {
            detailsActionsSubTitleTextView.text = subtitle
        }
        detailsActionsCloseView.setOnClickListener {
            dismiss()
        }
    }

    private fun setupMainButtonsBindings(
        binding: LayoutMessageDetailsActionsSheetButtonsBinding,
        detailsViewModel: MessageDetailsViewModel
    ) = with(binding) {
        detailsActionsReplyTextView.setOnClickListener {
            detailsViewModel.handleAction(MessageDetailsAction.REPLY)
            dismiss()
        }
        detailsActionsReplyAllTextView.setOnClickListener {
            detailsViewModel.handleAction(MessageDetailsAction.REPLY_ALL)
            dismiss()
        }
        detailsActionsForwardTextView.setOnClickListener {
            detailsViewModel.handleAction(MessageDetailsAction.FORWARD)
            dismiss()
        }
        detailsActionsMarkAsUnreadTextView.setOnClickListener {
            detailsViewModel.handleAction(MessageDetailsAction.MARK_UNREAD)
            dismiss()
        }
    }

    private fun setupOtherButtonsBindings(
        binding: FragmentMessageDetailsActionSheetBinding,
        detailsViewModel: MessageDetailsViewModel
    ) = with(binding) {
        detailsActionsStarUnstarTextView.setOnClickListener {
            detailsViewModel.handleAction(MessageDetailsAction.STAR_UNSTAR)
            dismiss()
        }
        detailsActionsTrashTextView.setOnClickListener {
            detailsViewModel.handleAction(MessageDetailsAction.MOVE_TO_TRASH)
            dismiss()
        }
        detailsActionsMoveToArchiveTextView.setOnClickListener {
            detailsViewModel.handleAction(MessageDetailsAction.MOVE_TO_ARCHIVE)
            dismiss()
        }
        detailsActionsMoveToSpamTextView.setOnClickListener {
            detailsViewModel.handleAction(MessageDetailsAction.MOVE_TO_SPAM)
            dismiss()
        }
        detailsActionsLabelAsTextView.setOnClickListener {
            (activity as MessageDetailsActivity).showLabelsManagerDialog()
            dismiss()
        }
        detailsActionsMoveToTextView.setOnClickListener {
            (activity as MessageDetailsActivity).showFoldersManagerDialog()
            dismiss()
        }
        detailsActionsReportPhishingTextView.setOnClickListener {
            (activity as MessageDetailsActivity).showReportPhishingDialog()
            dismiss()
        }
        detailsActionsPrintTextView.setOnClickListener {
            // we call it this way as it requires "special" context from the Activity
            (activity as MessageDetailsActivity).printMessage()
            dismiss()
        }
    }

    private fun setCloseIconVisibility(shouldBeVisible: Boolean) {
        binding.includeLayoutActionSheetHeader.detailsActionsCloseView.isVisible = shouldBeVisible
    }

    companion object {

        const val EXTRA_ARG_TITLE = "arg_message_details_actions_title"
        const val EXTRA_ARG_SUBTITLE = "arg_message_details_actions_sub_title"
    }
}

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
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import ch.protonmail.android.R
import ch.protonmail.android.activities.messageDetails.viewmodel.MessageDetailsViewModel
import ch.protonmail.android.core.Constants
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
            val headerView = binding.includeLayoutActionSheetHeader.actionsSheetTitleTextView
            val subHeaderView = binding.includeLayoutActionSheetHeader.actionsSheetSubTitleTextView
            val targetOffsetSize = resources.getDimensionPixelSize(R.dimen.padding_xxl)
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
                            if (slideOffset > HEADER_SLIDE_THRESHOLD) {
                                val intermediateShift =
                                    targetOffsetSize *
                                        ((slideOffset - HEADER_SLIDE_THRESHOLD) * (1 / (1 - HEADER_SLIDE_THRESHOLD)))
                                headerView.translationX = intermediateShift
                                subHeaderView.translationX = intermediateShift
                            } else if (headerView.translationX != 0f) {
                                headerView.translationX = 0f
                                subHeaderView.translationX = 0f
                            }
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
            actionsSheetTitleTextView.text = title
        }
        val subtitle = arguments?.getString(EXTRA_ARG_SUBTITLE)
        if (!subtitle.isNullOrEmpty()) {
            actionsSheetSubTitleTextView.text = subtitle
        }
        actionsSheetCloseView.setOnClickListener {
            dismiss()
        }
    }

    private fun setupMainButtonsBindings(
        binding: LayoutMessageDetailsActionsSheetButtonsBinding,
        detailsViewModel: MessageDetailsViewModel
    ) = with(binding) {
        detailsActionsReplyTextView.setOnClickListener {
            (activity as MessageDetailsActivity).executeMessageAction(Constants.MessageActionType.REPLY)
            dismiss()
        }
        detailsActionsReplyAllTextView.setOnClickListener {
            (activity as MessageDetailsActivity).executeMessageAction(Constants.MessageActionType.REPLY_ALL)
            dismiss()
        }
        detailsActionsForwardTextView.setOnClickListener {
            (activity as MessageDetailsActivity).executeMessageAction(Constants.MessageActionType.FORWARD)
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
            // This is a bit crazy requirement but designers do not know what to do about it
            // so we have to dismiss 2 screens at the time and go to the main list here
            // this should be thought through and improved
            (activity as MessageDetailsActivity).onBackPressed()
        }
        detailsActionsMoveToArchiveTextView.setOnClickListener {
            detailsViewModel.handleAction(MessageDetailsAction.MOVE_TO_ARCHIVE)
            dismiss()
            // This is a bit crazy requirement but designers do not know what to do about it
            // so we have to dismiss 2 screens at the time and go to the main list here
            // this should be thought through and improved
            (activity as MessageDetailsActivity).onBackPressed()
        }
        detailsActionsMoveToSpamTextView.setOnClickListener {
            detailsViewModel.handleAction(MessageDetailsAction.MOVE_TO_SPAM)
            dismiss()
            // This is a bit crazy requirement but designers do not know what to do about it
            // so we have to dismiss 2 screens at the time and go to the main list here
            // this should be thought through and improved
            (activity as MessageDetailsActivity).onBackPressed()
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
        binding.includeLayoutActionSheetHeader.actionsSheetCloseView.isVisible = shouldBeVisible
    }

    companion object {

        private const val EXTRA_ARG_TITLE = "arg_message_details_actions_title"
        private const val EXTRA_ARG_SUBTITLE = "arg_message_details_actions_sub_title"
        private const val HEADER_SLIDE_THRESHOLD = 0.8f

        fun newInstance(
            title: CharSequence,
            subTitle: String
        ): MessageDetailsActionSheet {
            return MessageDetailsActionSheet().apply {
                arguments = bundleOf(
                    EXTRA_ARG_TITLE to title,
                    EXTRA_ARG_SUBTITLE to subTitle
                )
            }
        }
    }
}

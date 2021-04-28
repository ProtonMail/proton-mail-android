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
import android.view.WindowManager
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import ch.protonmail.android.R
import ch.protonmail.android.activities.messageDetails.viewmodel.MessageDetailsViewModel
import ch.protonmail.android.core.Constants
import ch.protonmail.android.databinding.FragmentMessageDetailsActionSheetBinding
import ch.protonmail.android.databinding.LayoutMessageDetailsActionsSheetButtonsBinding
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

    private var actionSheetHeader: ActionSheetHeader? = null
    private val viewModel: MessageDetailsViewModel by activityViewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val binding = FragmentMessageDetailsActionSheetBinding.inflate(inflater)

        setupHeaderBindings(binding.actionSheetHeaderDetailsActions, arguments)
        setupMainMessageActionsBindings(binding.includeLayoutActionSheetButtons)
        setupManageSectionBindings(binding, viewModel)
        setupMoveSectionBindings(binding, viewModel)
        setupMoreSectionBindings(binding)

        actionSheetHeader = binding.actionSheetHeaderDetailsActions
        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = super.onCreateDialog(savedInstanceState)
        bottomSheetDialog.setOnShowListener { dialogInterface ->
            val dialog = dialogInterface as BottomSheetDialog
            val bottomSheet: View? = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet)
            val targetOffsetSize = resources.getDimensionPixelSize(R.dimen.padding_xxl)
            if (bottomSheet != null) {
                val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
                bottomSheetBehavior.addBottomSheetCallback(
                    object : BottomSheetBehavior.BottomSheetCallback() {
                        override fun onStateChanged(bottomSheet: View, newState: Int) {
                            Timber.v("State changed to $newState")
                            if (newState == STATE_EXPANDED) {
                                setCloseIconVisibility(true)
                                dialog.window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                            } else {
                                setCloseIconVisibility(false)
                                dialog.window?.addFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                            }
                        }

                        override fun onSlide(bottomSheet: View, slideOffset: Float) {
                            if (slideOffset > HEADER_SLIDE_THRESHOLD) {
                                val intermediateShift =
                                    targetOffsetSize *
                                        ((slideOffset - HEADER_SLIDE_THRESHOLD) * (1 / (1 - HEADER_SLIDE_THRESHOLD)))
                                actionSheetHeader?.shiftTitleToRightBy(intermediateShift)
                            } else {
                                actionSheetHeader?.shiftTitleToRightBy(0f)
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
        actionSheetHeader = null
        super.onDestroyView()
    }

    private fun setupHeaderBindings(
        actionSheetHeader: ActionSheetHeader,
        arguments: Bundle?
    ) = with(actionSheetHeader) {
        val title = arguments?.getString(EXTRA_ARG_TITLE)
        if (!title.isNullOrEmpty()) {
            setTitle(title)
        }
        val subtitle = arguments?.getString(EXTRA_ARG_SUBTITLE)
        if (!subtitle.isNullOrEmpty()) {
            setSubTitle(subtitle)
        }
        setOnCloseClickListener {
            dismiss()
        }
    }

    private fun setupMainMessageActionsBindings(
        binding: LayoutMessageDetailsActionsSheetButtonsBinding
    ) = with(binding) {
        textViewDetailsActionsReply.setOnClickListener {
            (activity as MessageDetailsActivity).executeMessageAction(Constants.MessageActionType.REPLY)
            dismiss()
        }
        textViewDetailsActionsReplyAll.setOnClickListener {
            (activity as MessageDetailsActivity).executeMessageAction(Constants.MessageActionType.REPLY_ALL)
            dismiss()
        }
        textViewDetailsActionsForward.setOnClickListener {
            (activity as MessageDetailsActivity).executeMessageAction(Constants.MessageActionType.FORWARD)
            dismiss()
        }

    }

    private fun setupManageSectionBindings(
        binding: FragmentMessageDetailsActionSheetBinding,
        viewModel: MessageDetailsViewModel
    ) = with(binding) {
        val isStarred = arguments?.getBoolean(EXTRA_ARG_IS_STARED) ?: false
        if (isStarred) {
            textViewDetailsActionsUnstar.apply {
                isVisible = true
                setOnClickListener {
                    viewModel.handleAction(MessageDetailsAction.STAR_UNSTAR)
                    dismiss()
                }
            }
            textViewDetailsActionsStar.isVisible = false
        } else {
            textViewDetailsActionsStar.apply {
                isVisible = true
                setOnClickListener {
                    viewModel.handleAction(MessageDetailsAction.STAR_UNSTAR)
                    dismiss()
                }
            }
            textViewDetailsActionsUnstar.isVisible = false
        }

        textViewDetailsActionsMarkRead.setOnClickListener {
            viewModel.handleAction(MessageDetailsAction.MARK_READ)
            dismiss()
        }
        textViewDetailsActionsMarkUnread.setOnClickListener {
            viewModel.handleAction(MessageDetailsAction.MARK_UNREAD)
            dismiss()
        }
        textViewDetailsActionsLabelAs.setOnClickListener {
            (activity as MessageDetailsActivity).showLabelsManagerDialog()
            dismiss()
        }
    }

    private fun setupMoveSectionBindings(
        binding: FragmentMessageDetailsActionSheetBinding,
        detailsViewModel: MessageDetailsViewModel
    ) = with(binding) {
        val messageLocation =
            Constants.MessageLocationType.fromInt(
                arguments?.getInt(EXTRA_ARG_CURRENT_LOCATION_ID) ?: 0
            )

        textViewDetailsActionsMoveToInbox.apply {
            isVisible = messageLocation in Constants.MessageLocationType.values()
                .filter { it != Constants.MessageLocationType.INBOX }
            setOnClickListener {
                detailsViewModel.handleAction(MessageDetailsAction.MOVE_TO_INBOX)
                dismiss()
                // This is a bit crazy requirement but designers do not know what to do about it
                // so we have to dismiss 2 screens at the time and go to the main list here
                // this should be thought through and improved
                (activity as MessageDetailsActivity).onBackPressed()
            }
        }
        textViewDetailsActionsTrash.apply {
            isVisible = messageLocation in Constants.MessageLocationType.values()
                .filter { it != Constants.MessageLocationType.TRASH }
            setOnClickListener {
                detailsViewModel.handleAction(MessageDetailsAction.MOVE_TO_TRASH)
                dismiss()
                // This is a bit crazy requirement but designers do not know what to do about it
                // so we have to dismiss 2 screens at the time and go to the main list here
                // this should be thought through and improved
                (activity as MessageDetailsActivity).onBackPressed()
            }
        }
        textViewDetailsActionsMoveToArchive.apply {
            isVisible = messageLocation in Constants.MessageLocationType.values()
                .filter { it != Constants.MessageLocationType.ARCHIVE }
            setOnClickListener {
                detailsViewModel.handleAction(MessageDetailsAction.MOVE_TO_ARCHIVE)
                dismiss()
                // This is a bit crazy requirement but designers do not know what to do about it
                // so we have to dismiss 2 screens at the time and go to the main list here
                // this should be thought through and improved
                (activity as MessageDetailsActivity).onBackPressed()
            }
        }
        textViewDetailsActionsMoveToSpam.apply {
            isVisible = messageLocation in Constants.MessageLocationType.values()
                .filter { it != Constants.MessageLocationType.SPAM }
            setOnClickListener {
                detailsViewModel.handleAction(MessageDetailsAction.MOVE_TO_SPAM)
                dismiss()
                // This is a bit crazy requirement but designers do not know what to do about it
                // so we have to dismiss 2 screens at the time and go to the main list here
                // this should be thought through and improved
                (activity as MessageDetailsActivity).onBackPressed()
            }
        }
        textViewDetailsActionsDelete.apply {
            isVisible = messageLocation in Constants.MessageLocationType.values()
                .filter { type ->
                    type != Constants.MessageLocationType.INBOX &&
                        type != Constants.MessageLocationType.ARCHIVE &&
                        type != Constants.MessageLocationType.STARRED &&
                        type != Constants.MessageLocationType.ALL_MAIL
                }
            setOnClickListener {
                detailsViewModel.handleAction(MessageDetailsAction.DELETE_MESSAGE)
                dismiss()
            }
        }
        textViewDetailsActionsMoveTo.setOnClickListener {
            (activity as MessageDetailsActivity).showFoldersManagerDialog()
            dismiss()
        }
    }

    private fun setupMoreSectionBindings(
        binding: FragmentMessageDetailsActionSheetBinding
    ) = with(binding) {
        textViewDetailsActionsPrint.setOnClickListener {
            // we call it this way as it requires "special" context from the Activity
            (activity as MessageDetailsActivity).printMessage()
            dismiss()
        }
        textViewDetailsActionsViewHeaders.setOnClickListener {
            (activity as MessageDetailsActivity).showViewHeaders()
            dismiss()
        }
        textViewDetailsActionsReportPhishing.setOnClickListener {
            (activity as MessageDetailsActivity).showReportPhishingDialog()
            dismiss()
        }
    }

    private fun setCloseIconVisibility(shouldBeVisible: Boolean) =
        actionSheetHeader?.setCloseIconVisibility(shouldBeVisible)

    companion object {

        private const val EXTRA_ARG_TITLE = "arg_message_details_actions_title"
        private const val EXTRA_ARG_SUBTITLE = "arg_message_details_actions_sub_title"
        private const val EXTRA_ARG_IS_STARED = "arg_extra_is_stared"
        private const val EXTRA_ARG_CURRENT_LOCATION_ID = "extra_arg_current_location_id"
        private const val HEADER_SLIDE_THRESHOLD = 0.8f

        fun newInstance(
            title: CharSequence,
            subTitle: String,
            isStarred: Boolean,
            currentLocationId: Int
        ): MessageDetailsActionSheet {
            return MessageDetailsActionSheet().apply {
                arguments = bundleOf(
                    EXTRA_ARG_TITLE to title,
                    EXTRA_ARG_SUBTITLE to subTitle,
                    EXTRA_ARG_IS_STARED to isStarred,
                    EXTRA_ARG_CURRENT_LOCATION_ID to currentLocationId
                )
            }
        }
    }
}

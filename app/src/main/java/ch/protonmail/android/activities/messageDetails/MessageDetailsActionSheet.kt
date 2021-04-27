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

    private val viewModel: MessageDetailsViewModel by activityViewModels()

    private var _binding: FragmentMessageDetailsActionSheetBinding? = null
    private val binding get() = requireNotNull(_binding)

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentMessageDetailsActionSheetBinding.inflate(inflater)

        setupHeaderBindings(binding.actionSheetHeaderDetailsActions, arguments)
        setupMainButtonsBindings(binding.includeLayoutActionSheetButtons, viewModel)
        setupOtherButtonsBindings(binding, viewModel)

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
                                binding.actionSheetHeaderDetailsActions.shiftTitleToRightBy(intermediateShift)
                            } else {
                                binding.actionSheetHeaderDetailsActions.shiftTitleToRightBy(0f)
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
        _binding = null
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

    private fun setupMainButtonsBindings(
        binding: LayoutMessageDetailsActionsSheetButtonsBinding,
        detailsViewModel: MessageDetailsViewModel
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
        textViewDetailsActionsMarkAsUnread.setOnClickListener {
            detailsViewModel.handleAction(MessageDetailsAction.MARK_UNREAD)
            dismiss()
        }
    }

    private fun setupOtherButtonsBindings(
        binding: FragmentMessageDetailsActionSheetBinding,
        detailsViewModel: MessageDetailsViewModel
    ) = with(binding) {
        textViewDetailsActionsStarUnstar.apply {
            setOnClickListener {
                detailsViewModel.handleAction(MessageDetailsAction.STAR_UNSTAR)
                dismiss()
            }
            // message aware states
            val message = viewModel.message.value
            if (message != null) {
                if (message.isStarred == true) {
                    setText(R.string.unstar)
                    setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_star_remove, 0, 0, 0)
                } else {
                    setText(R.string.star)
                    setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.ic_star_24dp, 0, 0, 0)
                }
            }
        }
        textViewDetailsActionsTrash.setOnClickListener {
            detailsViewModel.handleAction(MessageDetailsAction.MOVE_TO_TRASH)
            dismiss()
            // This is a bit crazy requirement but designers do not know what to do about it
            // so we have to dismiss 2 screens at the time and go to the main list here
            // this should be thought through and improved
            (activity as MessageDetailsActivity).onBackPressed()
        }
        textViewDetailsActionsMoveToArchive.setOnClickListener {
            detailsViewModel.handleAction(MessageDetailsAction.MOVE_TO_ARCHIVE)
            dismiss()
            // This is a bit crazy requirement but designers do not know what to do about it
            // so we have to dismiss 2 screens at the time and go to the main list here
            // this should be thought through and improved
            (activity as MessageDetailsActivity).onBackPressed()
        }
        textViewDetailsActionsMoveToSpam.setOnClickListener {
            detailsViewModel.handleAction(MessageDetailsAction.MOVE_TO_SPAM)
            dismiss()
            // This is a bit crazy requirement but designers do not know what to do about it
            // so we have to dismiss 2 screens at the time and go to the main list here
            // this should be thought through and improved
            (activity as MessageDetailsActivity).onBackPressed()
        }
        textViewDetailsActionsLabelAs.setOnClickListener {
            (activity as MessageDetailsActivity).showLabelsManagerDialog()
            dismiss()
        }
        textViewDetailsActionsMoveTo.setOnClickListener {
            (activity as MessageDetailsActivity).showFoldersManagerDialog()
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
        textViewDetailsActionsPrint.setOnClickListener {
            // we call it this way as it requires "special" context from the Activity
            (activity as MessageDetailsActivity).printMessage()
            dismiss()
        }
    }

    private fun setCloseIconVisibility(shouldBeVisible: Boolean) {
        binding.actionSheetHeaderDetailsActions.setCloseIconVisibility(shouldBeVisible)
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

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

package ch.protonmail.android.ui.actionsheet

import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import ch.protonmail.android.R
import ch.protonmail.android.activities.messageDetails.EXTRA_VIEW_HEADERS
import ch.protonmail.android.activities.messageDetails.MessageViewHeadersActivity
import ch.protonmail.android.core.Constants
import ch.protonmail.android.databinding.FragmentMessageActionSheetBinding
import ch.protonmail.android.databinding.LayoutMessageDetailsActionsSheetButtonsBinding
import ch.protonmail.android.details.presentation.MessageDetailsActivity
import ch.protonmail.android.labels.presentation.ui.LabelsActionSheet
import ch.protonmail.android.utils.AppUtil
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import timber.log.Timber

/**
 * Fragment popping up with actions for messages.
 */
@AndroidEntryPoint
class MessageActionSheet : BottomSheetDialogFragment() {

    private var actionSheetHeader: ActionSheetHeader? = null
    private val viewModel: MessageActionSheetViewModel by viewModels()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val originatorId = arguments?.getInt(EXTRA_ARG_ORIGINATOR_SCREEN_ID) ?: ARG_ORIGINATOR_SCREEN_MESSAGE_DETAILS_ID
        val messageIds: List<String> = arguments?.getStringArray(EXTRA_ARG_MESSAGE_IDS)?.toList()
            ?: throw IllegalStateException("messageIds in MessageActionSheet are Empty!")
        val messageLocation =
            Constants.MessageLocationType.fromInt(
                arguments?.getInt(EXTRA_ARG_CURRENT_FOLDER_LOCATION_ID) ?: 0
            )

        val binding = FragmentMessageActionSheetBinding.inflate(inflater)

        setupHeaderBindings(binding.actionSheetHeaderDetailsActions, arguments)
        setupReplyActionsBindings(binding.includeLayoutActionSheetButtons, originatorId)
        setupManageSectionBindings(binding, viewModel, originatorId, messageIds, messageLocation)
        setupMoveSectionBindings(binding, viewModel, messageIds, messageLocation)
        setupMoreSectionBindings(binding, originatorId, messageIds)
        actionSheetHeader = binding.actionSheetHeaderDetailsActions

        viewModel.actionsFlow
            .onEach { processAction(it) }
            .launchIn(lifecycleScope)

        return binding.root
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val bottomSheetDialog = super.onCreateDialog(savedInstanceState)
        bottomSheetDialog.setOnShowListener { dialogInterface ->
            val dialog = dialogInterface as BottomSheetDialog
            val bottomSheet: View? = dialog.findViewById(com.google.android.material.R.id.design_bottom_sheet)
            val targetOffsetSize = resources.getDimensionPixelSize(R.dimen.padding_3xl)
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
    ) {
        with(actionSheetHeader) {
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
    }

    private fun setupReplyActionsBindings(
        binding: LayoutMessageDetailsActionsSheetButtonsBinding,
        originatorId: Int
    ) {
        with(binding) {
            layoutDetailsActions.isVisible = originatorId == ARG_ORIGINATOR_SCREEN_MESSAGE_DETAILS_ID

            textViewDetailsActionsReply.setOnClickListener {
                (activity as? MessageDetailsActivity)?.executeMessageAction(Constants.MessageActionType.REPLY)
                dismiss()
            }
            textViewDetailsActionsReplyAll.setOnClickListener {
                (activity as? MessageDetailsActivity)?.executeMessageAction(Constants.MessageActionType.REPLY_ALL)
                dismiss()
            }
            textViewDetailsActionsForward.setOnClickListener {
                (activity as? MessageDetailsActivity)?.executeMessageAction(Constants.MessageActionType.FORWARD)
                dismiss()
            }
        }
    }

    private fun setupManageSectionBindings(
        binding: FragmentMessageActionSheetBinding,
        viewModel: MessageActionSheetViewModel,
        originatorId: Int,
        messageIds: List<String>,
        messageLocation: Constants.MessageLocationType
    ) {
        with(binding) {
            val isStarred = arguments?.getBoolean(EXTRA_ARG_IS_STARED) ?: false

            textViewDetailsActionsUnstar.apply {
                isVisible = originatorId != ARG_ORIGINATOR_SCREEN_MESSAGE_DETAILS_ID || isStarred
                setOnClickListener {
                    viewModel.unStarMessage(messageIds)
                    dismiss()
                }
            }

            textViewDetailsActionsStar.apply {
                isVisible = originatorId != ARG_ORIGINATOR_SCREEN_MESSAGE_DETAILS_ID || !isStarred
                setOnClickListener {
                    viewModel.starMessage(messageIds)
                    dismiss()
                }
            }

            textViewDetailsActionsMarkRead.apply {
                isVisible = originatorId != ARG_ORIGINATOR_SCREEN_MESSAGE_DETAILS_ID
                setOnClickListener {
                    viewModel.markRead(messageIds, messageLocation)
                }
            }
            textViewDetailsActionsMarkUnread.setOnClickListener {
                viewModel.markUnread(messageIds, messageLocation)
            }
            textViewDetailsActionsLabelAs.setOnClickListener {
                viewModel.showLabelsManager(messageIds, messageLocation)
                dismiss()
            }
        }
    }

    private fun setupMoveSectionBindings(
        binding: FragmentMessageActionSheetBinding,
        viewModel: MessageActionSheetViewModel,
        messageIds: List<String>,
        messageLocation: Constants.MessageLocationType
    ) {
        with(binding) {

            textViewDetailsActionsMoveToInbox.apply {
                isVisible = messageLocation in Constants.MessageLocationType.values()
                    .filter { it != Constants.MessageLocationType.INBOX }
                if (messageLocation == Constants.MessageLocationType.SPAM) {
                    setText(R.string.not_spam_move_to_inbox)
                }
                setOnClickListener {
                    viewModel.moveToInbox(messageIds, messageLocation)
                    dismissActionSheetAndGoToMailbox()
                }
            }
            textViewDetailsActionsTrash.apply {
                isVisible = messageLocation in Constants.MessageLocationType.values()
                    .filter { it != Constants.MessageLocationType.TRASH }
                setOnClickListener {
                    viewModel.moveToTrash(messageIds, messageLocation)
                    dismissActionSheetAndGoToMailbox()
                }
            }
            textViewDetailsActionsMoveToArchive.apply {
                isVisible = messageLocation in Constants.MessageLocationType.values()
                    .filter { it != Constants.MessageLocationType.ARCHIVE }
                setOnClickListener {
                    viewModel.moveToArchive(messageIds, messageLocation)
                    dismissActionSheetAndGoToMailbox()
                }
            }
            textViewDetailsActionsMoveToSpam.apply {
                isVisible = messageLocation in Constants.MessageLocationType.values()
                    .filter { it != Constants.MessageLocationType.SPAM }
                setOnClickListener {
                    viewModel.moveToSpam(messageIds, messageLocation)
                    dismissActionSheetAndGoToMailbox()
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
                    viewModel.deleteMessage(messageIds)
                    dismiss()
                }
            }
            textViewDetailsActionsMoveTo.setOnClickListener {
                viewModel.showLabelsManager(messageIds, messageLocation, LabelsActionSheet.Type.FOLDER)
                dismiss()
            }
        }
    }

    private fun setupMoreSectionBindings(
        binding: FragmentMessageActionSheetBinding,
        originatorId: Int,
        messageIds: List<String>
    ) {
        with(binding) {

            viewActionSheetSeparator.isVisible = originatorId == ARG_ORIGINATOR_SCREEN_MESSAGE_DETAILS_ID
            textViewActionSheetMoreTitle.isVisible = originatorId == ARG_ORIGINATOR_SCREEN_MESSAGE_DETAILS_ID

            textViewDetailsActionsPrint.apply {
                isVisible = originatorId == ARG_ORIGINATOR_SCREEN_MESSAGE_DETAILS_ID
                setOnClickListener {
                    // we call it this way as it requires "special" context from the Activity
                    (activity as? MessageDetailsActivity)?.printMessage()
                    dismiss()
                }
            }
            textViewDetailsActionsViewHeaders.apply {
                isVisible = originatorId == ARG_ORIGINATOR_SCREEN_MESSAGE_DETAILS_ID
                setOnClickListener {
                    viewModel.showMessageHeaders(messageIds.first())
                }
            }
            textViewDetailsActionsReportPhishing.apply {
                isVisible = originatorId == ARG_ORIGINATOR_SCREEN_MESSAGE_DETAILS_ID
                setOnClickListener {
                    (activity as? MessageDetailsActivity)?.showReportPhishingDialog()
                    dismiss()
                }
            }
        }
    }

    /**
     * This is a bit crazy requirement but designers currently do not know what to do about it,
     * so we have to dismiss the action sheet and the Details activity at the time and go to the main list.
     * This should be improved.
     */
    private fun popBackDetailsActivity() = (activity as? MessageDetailsActivity)?.onBackPressed()

    private fun setCloseIconVisibility(shouldBeVisible: Boolean) =
        actionSheetHeader?.setCloseIconVisibility(shouldBeVisible)

    private fun processAction(sheetAction: MessageActionSheetAction) {
        Timber.v("Action received $sheetAction")
        when (sheetAction) {
            is MessageActionSheetAction.ShowLabelsManager -> showManageLabelsActionSheet(
                sheetAction.messageIds,
                sheetAction.labelActionSheetType,
                sheetAction.currentFolderLocationId
            )
            is MessageActionSheetAction.ShowMessageHeaders -> showMessageHeaders(sheetAction.messageHeaders)
            is MessageActionSheetAction.ChangeReadStatus -> dismissActionSheetAndGoToMailbox()
            else -> Timber.v("unhandled action $sheetAction")
        }
    }

    private fun showManageLabelsActionSheet(
        messageIds: List<String>,
        labelActionSheetType: LabelsActionSheet.Type,
        currentFolderLocationId: Int
    ) {
        LabelsActionSheet.newInstance(
            messageIds,
            currentFolderLocationId,
            labelActionSheetType
        )
            .show(parentFragmentManager, LabelsActionSheet::class.qualifiedName)
        dismiss()
    }

    private fun showMessageHeaders(messageHeader: String) {
        startActivity(
            AppUtil.decorInAppIntent(
                Intent(
                    context,
                    MessageViewHeadersActivity::class.java
                ).putExtra(EXTRA_VIEW_HEADERS, messageHeader)
            )
        )
        dismiss()
    }

    private fun dismissActionSheetAndGoToMailbox() {
        dismiss()
        popBackDetailsActivity()
    }

    companion object {

        private const val EXTRA_ARG_MESSAGE_IDS = "arg_message_ids"
        private const val EXTRA_ARG_CURRENT_FOLDER_LOCATION_ID = "extra_arg_current_folder_location_id"
        private const val EXTRA_ARG_TITLE = "arg_message_details_actions_title"
        private const val EXTRA_ARG_SUBTITLE = "arg_message_details_actions_sub_title"
        private const val EXTRA_ARG_IS_STARED = "arg_extra_is_stared"
        private const val EXTRA_ARG_ORIGINATOR_SCREEN_ID = "extra_arg_originator_screen_id"
        private const val HEADER_SLIDE_THRESHOLD = 0.8f
        const val ARG_ORIGINATOR_SCREEN_MESSAGE_DETAILS_ID = 0 // e.g. [MessageDetailsActivity]
        const val ARG_ORIGINATOR_SCREEN_MESSAGES_LIST_ID = 1 // e.g [MailboxActivity]

        /**
         * Creates new action sheet instance.
         *
         * @param originatorLocationId defines starting activity/location
         *  0 = [ARG_ORIGINATOR_SCREEN_MESSAGE_DETAILS_ID]
         *  1 = [ARG_ORIGINATOR_SCREEN_MESSAGES_LIST_ID]
         * @param messagesIds current message id/ or selected messages Ids
         * @param currentFolderLocationId defines current message folder location based on values from
         * [Constants.MessageLocationType] e.g. 3 = trash
         * @param title title part that will be displayed in the top header
         * @param subTitle small sub title part that will be displayed in the top header, null/empty if not needed
         * @param isStarred defines if message is currently marked as starred
         */
        fun newInstance(
            originatorLocationId: Int,
            messagesIds: List<String>,
            currentFolderLocationId: Int,
            title: CharSequence,
            subTitle: String? = null,
            isStarred: Boolean = false
        ): MessageActionSheet {
            return MessageActionSheet().apply {
                arguments = bundleOf(
                    EXTRA_ARG_MESSAGE_IDS to messagesIds.toTypedArray(),
                    EXTRA_ARG_TITLE to title,
                    EXTRA_ARG_SUBTITLE to subTitle,
                    EXTRA_ARG_IS_STARED to isStarred,
                    EXTRA_ARG_CURRENT_FOLDER_LOCATION_ID to currentFolderLocationId,
                    EXTRA_ARG_ORIGINATOR_SCREEN_ID to originatorLocationId
                )
            }
        }
    }
}

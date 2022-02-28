/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */

package ch.protonmail.android.ui.actionsheet

import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.ui.actionsheet.model.ActionSheetTarget

/**
 * Contains types of actions executed from message action sheet.
 */
sealed class MessageActionSheetAction {

    object Default : MessageActionSheetAction()
    data class ShowLabelsManager(
        val messageIds: List<String>,
        val currentFolderLocation: Int,
        val currentLocationId: String,
        val labelActionSheetType: LabelType = LabelType.MESSAGE_LABEL,
        val actionSheetTarget: ActionSheetTarget
    ) : MessageActionSheetAction()

    data class ShowMessageHeaders(val messageHeaders: String) : MessageActionSheetAction()

    data class ChangeStarredStatus(
        val starredStatus: Boolean,
        val isSuccessful: Boolean,
        val areMailboxItemsMovedFromLocation: Boolean
    ) : MessageActionSheetAction()

    data class DismissActionSheet(
        val shallDismissBackingActivity: Boolean,
        val areMailboxItemsMovedFromLocation: Boolean
    ) : MessageActionSheetAction()

    data class ViewMessageInLightDarkMode(
        val messageId: String
    ) : MessageActionSheetAction()

    object CouldNotCompleteActionError : MessageActionSheetAction()
}

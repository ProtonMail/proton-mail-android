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

import ch.protonmail.android.core.Constants
import ch.protonmail.android.ui.actionsheet.model.ActionSheetTarget

sealed class MessageActionSheetState {
    object Initial : MessageActionSheetState()

    data class Data(
        val moveSectionState: MoveSectionState
    ) : MessageActionSheetState()

    data class MoveSectionState(
        val mailboxItemIds: List<String>,
        val messageLocation: Constants.MessageLocationType,
        val actionsTarget: ActionSheetTarget,
        val showMoveToInboxAction: Boolean,
        val showMoveToTrashAction: Boolean,
        val showMoveToArchiveAction: Boolean,
        val showMoveToSpamAction: Boolean,
        val showDeleteAction: Boolean
    )

}

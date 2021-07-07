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

package ch.protonmail.android.ui.actionsheet.model

enum class ActionSheetTarget {
    /**
     * ActionSheet called from the MailBox Screen (ie. MailboxActivity)
     * to act on a number of Mailbox items which can be either messages or conversations
     */
    MAILBOX_ITEMS_IN_MAILBOX_SCREEN,

    /**
     * ActionSheet called from the Detail Screen (ie. MessageDetailsActivity)
     * to act on the one "main" message entity being displayed
     */
    MAILBOX_ITEM_IN_DETAIL_SCREEN,

    /**
     * ActionSheet called from the Detail Screen (ie. MessageDetailsActivity)
     * to act on one specific message within a conversation (that has more than one message)
     */
    MESSAGE_ITEM_WITHIN_CONVERSATION_DETAIL_SCREEN,

    /**
     * ActionSheet called from the Detail Screen (ie. MessageDetailsActivity)
     * to act on the one "main" conversation entity being displayed
     */
    CONVERSATION_ITEM_IN_DETAIL_SCREEN
}

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

package ch.protonmail.android.mailbox.data.local.model

import androidx.room.ColumnInfo

data class LabelContextDatabaseModel(

    @ColumnInfo(name = COLUMN_ID)
    val id: String,

    @ColumnInfo(name = COLUMN_CONTEXT_NUM_UNREAD)
    val contextNumUnread: Int,

    @ColumnInfo(name = COLUMN_CONTEXT_NUM_MESSAGES)
    val contextNumMessages: Int,

    @ColumnInfo(name = COLUMN_CONTEXT_TIME)
    val contextTime: Long,

    @ColumnInfo(name = COLUMN_CONTEXT_SIZE)
    val contextSize: Int,

    @ColumnInfo(name = COLUMN_CONTEXT_NUM_ATTACHMENTS)
    val contextNumAttachments: Int
) {

    companion object {

        private const val COLUMN_ID = "ID"
        private const val COLUMN_CONTEXT_NUM_UNREAD = "ContextNumUnread"
        private const val COLUMN_CONTEXT_NUM_MESSAGES = "ContextNumMessages"
        private const val COLUMN_CONTEXT_TIME = "ContextTime"
        private const val COLUMN_CONTEXT_SIZE = "ContextSize"
        private const val COLUMN_CONTEXT_NUM_ATTACHMENTS = "ContextNumAttachments"
    }
}

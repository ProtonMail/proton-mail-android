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
package ch.protonmail.android.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ch.protonmail.android.data.local.model.Attachment
import ch.protonmail.android.data.local.model.AttachmentTypesConverter
import ch.protonmail.android.data.local.model.Label
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.MessagesTypesConverter
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.mailbox.data.local.ConversationDao
import ch.protonmail.android.mailbox.data.local.model.ConversationEntity
import ch.protonmail.android.mailbox.data.local.model.ConversationTypesConverter

@Database(
    entities = [
        Attachment::class,
        Message::class,
        Label::class,
        ConversationEntity::class
    ],
    version = 9
)
@TypeConverters(
    value = [
        MessagesTypesConverter::class,
        AttachmentTypesConverter::class,
        ConversationTypesConverter::class
    ]
)
abstract class MessageDatabase : RoomDatabase() {

    abstract fun getDao(): MessageDao
    abstract fun getConversationDao(): ConversationDao

    companion object : DatabaseFactory<MessageDatabase>(
        MessageDatabase::class,
        "MessagesDatabase.db"
    ) {

        private val searchCache = mutableMapOf<Id, MessageDatabase>()

        @Synchronized
        fun getSearchDatabase(context: Context, userId: Id): MessageDatabase =
            searchCache.getOrPut(userId) { buildInMemoryDatabase(context) }
    }
}

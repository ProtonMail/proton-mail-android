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
package ch.protonmail.android.data.local

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import ch.protonmail.android.data.ProtonMailConverters
import ch.protonmail.android.data.local.model.Attachment
import ch.protonmail.android.data.local.model.AttachmentTypesConverter
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.data.local.model.MessagePreferenceEntity
import ch.protonmail.android.data.local.model.MessagesTypesConverter
import ch.protonmail.android.mailbox.data.local.ConversationDao
import ch.protonmail.android.mailbox.data.local.ConversationTypesConverter
import ch.protonmail.android.mailbox.data.local.UnreadCounterDao
import ch.protonmail.android.mailbox.data.local.model.ConversationDatabaseModel
import ch.protonmail.android.mailbox.data.local.model.UnreadCounterEntity
import me.proton.core.data.room.db.CommonConverters

@Database(
    entities = [
        Attachment::class,
        ConversationDatabaseModel::class,
        Message::class,
        MessagePreferenceEntity::class,
        UnreadCounterEntity::class
    ],
    autoMigrations = [
        AutoMigration(from = 15, to = 16),
        AutoMigration(from = 16, to = 17),
        AutoMigration(from = 17, to = 19),
        AutoMigration(from = 18, to = 19),
        AutoMigration(from = 19, to = 20)
    ],
    version = 20
)
@TypeConverters(
    value = [
        CommonConverters::class,
        AttachmentTypesConverter::class,
        MessagesTypesConverter::class,
        ConversationTypesConverter::class,
        ProtonMailConverters::class
    ]
)
internal abstract class MessageDatabase : RoomDatabase() {

    @Deprecated("Use getMessageDao", ReplaceWith("getMessageDao()"))
    fun getDao(): MessageDao =
        getMessageDao()

    abstract fun getConversationDao(): ConversationDao
    abstract fun getMessageDao(): MessageDao
    abstract fun getMessagePreferenceDao(): MessagePreferenceDao
    abstract fun getUnreadCounterDao(): UnreadCounterDao

    companion object Factory : DatabaseFactory<MessageDatabase>(
        MessageDatabase::class,
        "MessagesDatabase.db"
    )
}

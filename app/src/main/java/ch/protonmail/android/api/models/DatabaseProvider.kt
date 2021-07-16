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
package ch.protonmail.android.api.models

import android.content.Context
import ch.protonmail.android.data.local.AttachmentMetadataDao
import ch.protonmail.android.data.local.AttachmentMetadataDatabase
import ch.protonmail.android.data.local.ContactDao
import ch.protonmail.android.data.local.ContactDatabase
import ch.protonmail.android.data.local.CounterDao
import ch.protonmail.android.data.local.CounterDatabase
import ch.protonmail.android.data.local.MessageDao
import ch.protonmail.android.data.local.MessageDatabase
import ch.protonmail.android.data.local.NotificationDao
import ch.protonmail.android.data.local.NotificationDatabase
import ch.protonmail.android.data.local.PendingActionDao
import ch.protonmail.android.data.local.PendingActionDatabase
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.mailbox.data.local.ConversationDao
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provider of DAOs and Databases, use this instead of old *Factory classes.
 */
@Singleton
class DatabaseProvider @Inject constructor(
    private val context: Context
) {

    // Attachment metadata
    fun provideAttachmentMetadataDao(userId: Id): AttachmentMetadataDao =
        AttachmentMetadataDatabase.getInstance(context, userId).getDao()

    // Contact
    fun provideContactDao(userId: Id): ContactDao =
        ContactDatabase.getInstance(context, userId).getDao()

    // Counter
    fun provideCounterDao(userId: Id): CounterDao =
        CounterDatabase.getInstance(context, userId).getDao()

    // Message
    fun provideMessageDao(userId: Id): MessageDao =
        MessageDatabase.getInstance(context, userId).getDao()

    // TODO remove once the usage in ClearUserData use-case is removed
    // Conversation
    fun provideConversationDao(userId: Id): ConversationDao =
        MessageDatabase.getInstance(context, userId).getConversationDao()

    // Notification
    fun provideNotificationDao(userId: Id): NotificationDao =
        NotificationDatabase.getInstance(context, userId).getDao()

    // Pending action
    fun providePendingActionDao(userId: Id): PendingActionDao =
        PendingActionDatabase.getInstance(context, userId).getDao()

}

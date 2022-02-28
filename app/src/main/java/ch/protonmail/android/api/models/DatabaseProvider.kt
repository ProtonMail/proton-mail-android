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
package ch.protonmail.android.api.models

import android.content.Context
import ch.protonmail.android.data.local.AttachmentMetadataDao
import ch.protonmail.android.data.local.AttachmentMetadataDatabase
import ch.protonmail.android.data.local.ContactDao
import ch.protonmail.android.data.local.ContactDatabase
import ch.protonmail.android.data.local.CounterDatabase
import ch.protonmail.android.data.local.MessageDao
import ch.protonmail.android.data.local.MessageDatabase
import ch.protonmail.android.data.local.MessagePreferenceDao
import ch.protonmail.android.pendingaction.data.PendingActionDao
import ch.protonmail.android.pendingaction.data.PendingActionDatabase
import ch.protonmail.android.mailbox.data.local.ConversationDao
import me.proton.core.domain.entity.UserId
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
    fun provideAttachmentMetadataDao(userId: UserId): AttachmentMetadataDao =
        AttachmentMetadataDatabase.getInstance(context, userId).getDao()

    // Contact
    fun provideContactDatabase(userId: UserId): ContactDatabase =
        ContactDatabase.getInstance(context, userId)

    fun provideContactDao(userId: UserId): ContactDao =
        ContactDatabase.getInstance(context, userId).getDao()

    // TODO remove once the usage in ClearUserData use-case is removed
    // Counter
    internal fun provideUnreadCounterDao(userId: UserId) =
        MessageDatabase.getInstance(context, userId).getUnreadCounterDao()

    fun provideCounterDao(userId: UserId) =
        CounterDatabase.getInstance(context, userId).getDao()

    // Message
    fun provideMessageDao(userId: UserId): MessageDao =
        MessageDatabase.getInstance(context, userId).getMessageDao()

    // TODO remove once the usage in ClearUserData use-case is removed
    // Conversation
    fun provideConversationDao(userId: UserId): ConversationDao =
        MessageDatabase.getInstance(context, userId).getConversationDao()

    // Pending action
    fun providePendingActionDao(userId: UserId): PendingActionDao =
        PendingActionDatabase.getInstance(context, userId).getDao()

    fun provideMessagePreferenceDao(userId: UserId): MessagePreferenceDao =
        MessageDatabase.getInstance(context, userId).getMessagePreferenceDao()
}

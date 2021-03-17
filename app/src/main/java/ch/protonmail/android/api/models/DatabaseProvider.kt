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
import ch.protonmail.android.api.models.room.messages.MessagesDao
import ch.protonmail.android.api.models.room.notifications.NotificationsDatabaseFactory
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDao
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDatabaseFactory
import ch.protonmail.android.api.models.room.sendingFailedNotifications.SendingFailedNotificationsDatabaseFactory
import ch.protonmail.android.data.local.ContactsDao
import ch.protonmail.android.data.local.ContactsDatabase
import ch.protonmail.android.data.local.CounterDao
import ch.protonmail.android.data.local.CounterDatabase
import ch.protonmail.android.data.local.MessageDatabase
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.usecase.FindUsernameForUserId
import me.proton.core.util.kotlin.unsupported
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provider of DAOs and Databases, use this instead of old *Factory classes.
 */
@Singleton
class DatabaseProvider @Inject constructor(
    private val context: Context,
    private val findUsernameForUserId: FindUsernameForUserId
) {

    fun provideContactsDatabase(userId: Id): ContactsDatabase =
        ContactsDatabase.getInstance(context, userId)

    fun provideContactsDao(userId: Id): ContactsDao =
        provideContactsDatabase(userId).getDao()

    @Deprecated(
        "Get by user Id",
        ReplaceWith("provideContactsDao(userId)"),
        DeprecationLevel.ERROR
    )
    fun provideContactsDao(username: String?): ContactsDao =
        unsupported

    @Deprecated(
        "Get by user Id",
        ReplaceWith("provideContactsDatabase(userId)"),
        DeprecationLevel.ERROR
    )
    fun provideContactsDatabase(username: String?): ContactsDatabase =
        unsupported

    fun provideMessagesDao(userId: Id? = null): MessagesDao =
        provideMessagesDao(userId?.let { findUsernameForUserId.blocking(it) }?.s)

    @Deprecated("Get by user Id", ReplaceWith("provideMessagesDao(userId)"))
    fun provideMessagesDao(username: String?): MessagesDao =
        MessageDatabase.getInstance(context, username).getDao()

    fun provideMessagesDatabaseFactory(userId: Id? = null) =
        provideMessagesDatabaseFactory(userId?.let { findUsernameForUserId.blocking(it) }?.s)

    @Deprecated("Get by user Id", ReplaceWith("provideMessagesDatabaseFactory(userId)"))
    fun provideMessagesDatabaseFactory(username: String?) =
        MessageDatabase.getInstance(context, username)

    fun provideCountersDao(userId: Id): CounterDao =
        CounterDatabase.getInstance(context, userId).getDao()

    fun providePendingActionsDao(userId: Id? = null): PendingActionsDao =
        providePendingActionsDao(userId?.let { findUsernameForUserId.blocking(it) }?.s)

    @Deprecated("Get by user Id", ReplaceWith("providePendingActionsDao(userId)"))
    fun providePendingActionsDao(username: String?): PendingActionsDao =
        PendingActionsDatabaseFactory.getInstance(context, username).getDatabase()

    fun providePendingActionsDatabase(userId: Id? = null) =
        providePendingActionsDatabase(userId?.let { findUsernameForUserId.blocking(it) }?.s)

    @Deprecated("Get by user Id", ReplaceWith("providePendingActionsDatabase(userId)"))
    fun providePendingActionsDatabase(username: String?) =
        PendingActionsDatabaseFactory.getInstance(context, username)

    fun provideNotificationsDao(userId: Id? = null) =
        provideNotificationsDao(userId?.let { findUsernameForUserId.blocking(it) }?.s)

    @Deprecated("Get by user Id", ReplaceWith("provideNotificationsDao(userId)"))
    fun provideNotificationsDao(username: String?) =
        NotificationsDatabaseFactory.getInstance(context, username).getDatabase()

    fun provideNotificationsDatabase(userId: Id? = null) =
        provideNotificationsDatabase(userId?.let { findUsernameForUserId.blocking(it) }?.s)

    @Deprecated("Get by user Id", ReplaceWith("provideNotificationsDatabase(userId)"))
    fun provideNotificationsDatabase(username: String?) =
        NotificationsDatabaseFactory.getInstance(context, username)

    fun provideSendingFailedNotificationsDao(userId: Id? = null) =
        provideSendingFailedNotificationsDao(userId?.let { findUsernameForUserId.blocking(it) }?.s)

    @Deprecated("Get by user Id", ReplaceWith("provideSendingFailedNotificationsDao(userId)"))
    fun provideSendingFailedNotificationsDao(username: String?) =
        SendingFailedNotificationsDatabaseFactory.getInstance(context, username).getDatabase()

    fun provideSendingFailedNotificationsDatabase(userId: Id? = null) =
        provideSendingFailedNotificationsDatabase(userId?.let { findUsernameForUserId.blocking(it) }?.s)

    @Deprecated("Get by user Id", ReplaceWith("provideSendingFailedNotificationsDatabase(userId)"))
    fun provideSendingFailedNotificationsDatabase(username: String?) =
        SendingFailedNotificationsDatabaseFactory.getInstance(context, username)
}

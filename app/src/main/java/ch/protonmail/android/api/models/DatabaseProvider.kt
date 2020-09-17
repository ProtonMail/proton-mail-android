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
import ch.protonmail.android.api.models.room.contacts.ContactsDao
import ch.protonmail.android.api.models.room.contacts.ContactsDatabaseFactory
import ch.protonmail.android.api.models.room.counters.CountersDao
import ch.protonmail.android.api.models.room.counters.CountersDatabaseFactory
import ch.protonmail.android.api.models.room.messages.MessagesDao
import ch.protonmail.android.api.models.room.messages.MessagesDatabaseFactory
import ch.protonmail.android.api.models.room.notifications.NotificationsDatabaseFactory
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDao
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDatabaseFactory
import ch.protonmail.android.api.models.room.sendingFailedNotifications.SendingFailedNotificationsDatabaseFactory
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provider of DAOs and Databases, use this instead of old *Factory classes.
 *
 * This class and its functions are "open" only because Mockito needs it.
 */
@Singleton
class DatabaseProvider @Inject constructor(private val context: Context) {

    fun provideContactsDao(username: String? = null): ContactsDao =
        ContactsDatabaseFactory.getInstance(context, username).getDatabase()

    fun provideContactsDatabase(username: String? = null) =
        ContactsDatabaseFactory.getInstance(context, username)

    fun provideMessagesDao(username: String? = null): MessagesDao =
        MessagesDatabaseFactory.getInstance(context, username).getDatabase()

    fun provideMessagesDatabaseFactory(username: String? = null) =
        MessagesDatabaseFactory.getInstance(context, username)

    fun provideCountersDao(username: String? = null): CountersDao =
        CountersDatabaseFactory.getInstance(context, username).getDatabase()

    fun providePendingActionsDao(username: String? = null): PendingActionsDao =
        PendingActionsDatabaseFactory.getInstance(context, username).getDatabase()

    fun providePendingActionsDatabase(username: String? = null) =
        PendingActionsDatabaseFactory.getInstance(context, username)

    fun provideNotificationsDao(username: String? = null) =
        NotificationsDatabaseFactory.getInstance(context, username).getDatabase()

    fun provideNotificationsDatabase(username: String? = null) =
        NotificationsDatabaseFactory.getInstance(context, username)

    fun provideSendingFailedNotificationsDao(username: String? = null) =
        SendingFailedNotificationsDatabaseFactory.getInstance(context, username).getDatabase()

    fun provideSendingFailedNotificationsDatabase(username: String? = null) =
        SendingFailedNotificationsDatabaseFactory.getInstance(context, username)
}

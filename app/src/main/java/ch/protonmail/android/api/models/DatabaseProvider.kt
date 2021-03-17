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
import ch.protonmail.android.data.local.SendingFailedNotificationDao
import ch.protonmail.android.data.local.SendingFailedNotificationDatabase
import ch.protonmail.android.domain.entity.Id
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Provider of DAOs and Databases, use this instead of old *Factory classes.
 */
@Singleton
class DatabaseProvider @Inject constructor(
    private val context: Context
) {

    // Contact
    fun provideContactDatabase(userId: Id): ContactDatabase =
        ContactDatabase.getInstance(context, userId)

    fun provideContactDao(userId: Id): ContactDao =
        provideContactDatabase(userId).getDao()

    // Counter
    fun provideCounterDao(userId: Id): CounterDao =
        CounterDatabase.getInstance(context, userId).getDao()

    // Message
    fun provideMessageDatabase(userId: Id): MessageDatabase =
        MessageDatabase.getInstance(context, userId)

    fun provideMessageDao(userId: Id): MessageDao =
        provideMessageDatabase(userId).getDao()

    // Notification
    fun provideNotificationDatabase(userId: Id): NotificationDatabase =
        NotificationDatabase.getInstance(context, userId)

    fun provideNotificationsDao(userId: Id): NotificationDao =
        provideNotificationDatabase(userId).getDao()

    // Pending Action
    fun providePendingActionDatabase(userId: Id): PendingActionDatabase =
        PendingActionDatabase.getInstance(context, userId)

    fun providePendingActionDao(userId: Id): PendingActionDao =
        providePendingActionDatabase(userId).getDao()

    // Sending failed notification
    fun provideSendingFailedNotificationsDatabase(userId: Id): SendingFailedNotificationDatabase =
        SendingFailedNotificationDatabase.getInstance(context, userId)

    fun provideSendingFailedNotificationsDao(userId: Id): SendingFailedNotificationDao =
        provideSendingFailedNotificationsDatabase(userId).getDao()
}

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

package ch.protonmail.android.di

import android.content.Context
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.AppDatabase
import ch.protonmail.android.data.local.AttachmentMetadataDao
import ch.protonmail.android.data.local.AttachmentMetadataDatabase
import ch.protonmail.android.data.local.ContactDao
import ch.protonmail.android.data.local.ContactDatabase
import ch.protonmail.android.data.local.CounterDao
import ch.protonmail.android.data.local.CounterDatabase
import ch.protonmail.android.data.local.MessageDao
import ch.protonmail.android.data.local.MessageDatabase
import ch.protonmail.android.pendingaction.data.PendingActionDao
import ch.protonmail.android.pendingaction.data.PendingActionDatabase
import ch.protonmail.android.labels.data.local.LabelDao
import ch.protonmail.android.mailbox.data.local.ConversationDao
import ch.protonmail.android.mailbox.data.local.UnreadCounterDao
import ch.protonmail.android.notifications.data.local.NotificationDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import me.proton.core.domain.entity.UserId
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
internal object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(
        @ApplicationContext context: Context
    ): AppDatabase = AppDatabase.buildDatabase(context)

    @Provides
    fun provideLabelDao(
        appDatabase: AppDatabase
    ): LabelDao = appDatabase.labelDao()

    @Provides
    fun provideNotificationDao(
        appDatabase: AppDatabase
    ): NotificationDao = appDatabase.notificationDao()

    @Provides
    fun provideAttachmentMetadataDao(
        context: Context,
        @CurrentUserId userId: UserId
    ): AttachmentMetadataDao = AttachmentMetadataDatabase.getInstance(context, userId).getDao()

    @Provides
    fun provideContactDatabase(
        context: Context,
        @CurrentUserId userId: UserId
    ): ContactDatabase = ContactDatabase.getInstance(context, userId)

    @Provides
    fun provideContactDao(factory: ContactDatabase): ContactDao =
        factory.getDao()

    @Provides
    fun provideCounterDatabase(
        context: Context,
        @CurrentUserId userId: UserId
    ): CounterDatabase = CounterDatabase.getInstance(context, userId)

    @Provides
    fun provideCounterDao(database: CounterDatabase): CounterDao =
        database.getDao()

    @Provides
    fun provideMessageDatabaseFactory(): MessageDatabase.Factory =
        MessageDatabase.Factory

    @Provides
    fun provideMessageDatabase(
        context: Context,
        @CurrentUserId userId: UserId
    ): MessageDatabase = MessageDatabase.getInstance(context, userId)

    @Provides
    fun provideMessageDao(messageDatabase: MessageDatabase): MessageDao =
        messageDatabase.getMessageDao()

    @Provides
    fun provideConversationDao(messageDatabase: MessageDatabase): ConversationDao =
        messageDatabase.getConversationDao()


    @Provides
    fun providePendingActionDatabase(context: Context, userManager: UserManager) =
        PendingActionDatabase.getInstance(context, userManager.requireCurrentUserId())

    @Provides
    fun providePendingActionDao(
        pendingActionDatabase: PendingActionDatabase
    ): PendingActionDao = pendingActionDatabase.getDao()

    @Provides
    fun provideUnreadCounterDao(messageDatabase: MessageDatabase): UnreadCounterDao =
        messageDatabase.getUnreadCounterDao()
}

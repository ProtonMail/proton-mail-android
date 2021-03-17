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
import ch.protonmail.android.api.models.room.counters.CounterDao
import ch.protonmail.android.api.models.room.counters.CounterDatabase
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDatabase
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDatabaseFactory
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.AttachmentMetadataDao
import ch.protonmail.android.data.local.AttachmentMetadataDatabase
import ch.protonmail.android.data.local.ContactsDao
import ch.protonmail.android.data.local.ContactsDatabase
import ch.protonmail.android.data.local.MessageDao
import ch.protonmail.android.data.local.MessageDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    fun provideAttachmentMetadataDatabase(context: Context, userManager: UserManager): AttachmentMetadataDao =
        AttachmentMetadataDatabase.getInstance(context, userManager.username).getDao()


    @Provides
    fun provideContactsDatabaseFactory(context: Context, userManager: UserManager): ContactsDatabase =
        ContactsDatabase.getInstance(context, userManager.username)

    @Provides
    fun provideContactsDatabase(factory: ContactsDatabase): ContactsDao =
        factory.getDao()


    @Provides
    fun provideCounterDatabase(context: Context, userManager: UserManager): CounterDatabase =
        CounterDatabase.getInstance(context, userManager.requireCurrentUserId())

    @Provides
    fun provideCounterDao(database: CounterDatabase): CounterDao =
        database.getDao()


    @Provides
    @Named("messages_factory")
    fun provideMessagesDatabaseFactory(context: Context, userManager: UserManager): MessageDatabase =
        MessageDatabase.getInstance(context, userManager.username)

    @Provides
    @Named("messages")
    fun provideMessagesDatabase(
        @Named("messages_factory") messageDatabase: MessageDatabase
    ): MessageDao = messageDatabase.getDao()


    @Provides
    fun providePendingActionsDatabaseFactory(context: Context, userManager: UserManager) =
        PendingActionsDatabaseFactory.getInstance(context, userManager.username)

    @Provides
    fun providePendingActionsDatabase(
        pendingActionsDatabaseFactory: PendingActionsDatabaseFactory
    ): PendingActionsDatabase = pendingActionsDatabaseFactory.getDatabase()


    @Provides
    @Named("messages_search_factory")
    fun provideSearchMessagesDatabaseFactory(context: Context): MessageDatabase =
        MessageDatabase.getSearchDatabase(context)

    @Provides
    @Named("messages_search")
    fun provideSearchMessagesDatabase(
        @Named("messages_search_factory") messageDatabase: MessageDatabase
    ): MessageDao = messageDatabase.getDao()

}

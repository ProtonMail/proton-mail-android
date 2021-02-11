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
import ch.protonmail.android.api.models.room.attachmentMetadata.AttachmentMetadataDatabase
import ch.protonmail.android.api.models.room.attachmentMetadata.AttachmentMetadataDatabaseFactory
import ch.protonmail.android.api.models.room.contacts.ContactsDatabase
import ch.protonmail.android.api.models.room.contacts.ContactsDatabaseFactory
import ch.protonmail.android.api.models.room.counters.CounterDao
import ch.protonmail.android.api.models.room.counters.CounterDatabase
import ch.protonmail.android.api.models.room.messages.MessagesDatabase
import ch.protonmail.android.api.models.room.messages.MessagesDatabaseFactory
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDatabase
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDatabaseFactory
import ch.protonmail.android.core.UserManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    fun provideAttachmentMetadataDatabase(context: Context, userManager: UserManager): AttachmentMetadataDatabase =
        AttachmentMetadataDatabaseFactory.getInstance(context, userManager.username).getDatabase()


    @Provides
    fun provideContactsDatabaseFactory(context: Context, userManager: UserManager): ContactsDatabaseFactory =
        ContactsDatabaseFactory.getInstance(context, userManager.username)

    @Provides
    fun provideContactsDatabase(factory: ContactsDatabaseFactory): ContactsDatabase =
        factory.getDatabase()


    @Provides
    fun provideCounterDatabase(context: Context, userManager: UserManager): CounterDatabase =
        CounterDatabase.getInstance(context, userManager.requireCurrentUserId())

    @Provides
    fun provideCounterDao(database: CounterDatabase): CounterDao =
        database.getDao()


    @Provides
    @Named("messages_factory")
    fun provideMessagesDatabaseFactory(context: Context, userManager: UserManager): MessagesDatabaseFactory =
        MessagesDatabaseFactory.getInstance(context, userManager.username)

    @Provides
    @Named("messages")
    fun provideMessagesDatabase(
        @Named("messages_factory") messagesDatabaseFactory: MessagesDatabaseFactory
    ): MessagesDatabase = messagesDatabaseFactory.getDatabase()


    @Provides
    fun providePendingActionsDatabaseFactory(context: Context, userManager: UserManager) =
        PendingActionsDatabaseFactory.getInstance(context, userManager.username)

    @Provides
    fun providePendingActionsDatabase(
        pendingActionsDatabaseFactory: PendingActionsDatabaseFactory
    ): PendingActionsDatabase = pendingActionsDatabaseFactory.getDatabase()


    @Provides
    @Named("messages_search_factory")
    fun provideSearchMessagesDatabaseFactory(context: Context): MessagesDatabaseFactory =
        MessagesDatabaseFactory.getSearchDatabase(context)

    @Provides
    @Named("messages_search")
    fun provideSearchMessagesDatabase(
        @Named("messages_search_factory") messagesDatabaseFactory: MessagesDatabaseFactory
    ): MessagesDatabase = messagesDatabaseFactory.getDatabase()

}

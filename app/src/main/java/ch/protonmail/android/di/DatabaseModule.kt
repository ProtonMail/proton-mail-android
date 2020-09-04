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
import ch.protonmail.android.api.models.room.messages.MessagesDatabase
import ch.protonmail.android.api.models.room.messages.MessagesDatabaseFactory
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDatabase
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDatabaseFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    @Named("messages")
    fun provideMessagesDatabase(
        @Named("messages_factory") messagesDatabaseFactory: MessagesDatabaseFactory
    ): MessagesDatabase = messagesDatabaseFactory.getDatabase()

    @Provides
    @Singleton
    @Named("messages_factory")
    fun provideMessagesDatabaseFactory(context: Context): MessagesDatabaseFactory =
        MessagesDatabaseFactory.getInstance(context)

    @Provides
    @Singleton
    @Named("messages_search")
    fun provideSearchMessagesDatabase(
        @Named("messages_search_factory") messagesDatabaseFactory: MessagesDatabaseFactory
    ): MessagesDatabase = messagesDatabaseFactory.getDatabase()

    @Provides
    @Singleton
    @Named("messages_search_factory")
    fun provideSearchMessagesDatabaseFactory(context: Context): MessagesDatabaseFactory =
        MessagesDatabaseFactory.getSearchDatabase(context)

    @Provides
    @Singleton
    fun providePendingActionsDatabase(
        pendingActionsDatabaseFactory: PendingActionsDatabaseFactory
    ): PendingActionsDatabase = pendingActionsDatabaseFactory.getDatabase()

    @Provides
    @Singleton
    fun providePendingActionsDatabaseFactory(context: Context): PendingActionsDatabaseFactory =
        PendingActionsDatabaseFactory.getInstance(context)

    @Provides
    @Singleton
    fun provideAttachmentMetadataDatabase(context: Context): AttachmentMetadataDatabase =
        AttachmentMetadataDatabaseFactory.getInstance(context).getDatabase()
}

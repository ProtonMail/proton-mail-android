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

import ch.protonmail.android.activities.messageDetails.DefaultDocumentParser
import ch.protonmail.android.activities.messageDetails.DefaultImageDecoder
import ch.protonmail.android.activities.messageDetails.DocumentParser
import ch.protonmail.android.activities.messageDetails.ImageDecoder
import ch.protonmail.android.data.local.CounterRepository
import ch.protonmail.android.data.local.CounterRepositoryImpl
import ch.protonmail.android.labels.data.LabelRepositoryImpl
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.mailbox.data.ConversationsRepositoryImpl
import ch.protonmail.android.mailbox.domain.ConversationsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface ApplicationBindsModule {

    @Binds
    fun DefaultDocumentParser.documentParser(): DocumentParser

    @Binds
    fun DefaultImageDecoder.imageDecoder(): ImageDecoder

    @Binds
    fun provideLabelRepository(repo: LabelRepositoryImpl): LabelRepository

    @Binds
    fun provideConversationRepository(repo: ConversationsRepositoryImpl): ConversationsRepository

    @Binds
    fun provideCounterRepository(repo: CounterRepositoryImpl): CounterRepository

}

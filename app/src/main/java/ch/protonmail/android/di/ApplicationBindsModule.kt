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
import ch.protonmail.android.notifications.data.NotificationRepositoryImpl
import ch.protonmail.android.notifications.domain.NotificationRepository
import ch.protonmail.android.settings.data.AccountSettingsRepository
import ch.protonmail.android.settings.data.SharedPreferencesAccountSettingsRepository
import ch.protonmail.android.settings.data.SharedPreferencesDeviceSettingsRepository
import ch.protonmail.android.settings.domain.DeviceSettingsRepository
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
    fun SharedPreferencesDeviceSettingsRepository.deviceSettingsRepository(): DeviceSettingsRepository

    @Binds
    fun SharedPreferencesAccountSettingsRepository.accountSettingsRepository(): AccountSettingsRepository

    @Binds
    fun provideLabelRepository(repo: LabelRepositoryImpl): LabelRepository

    @Binds
    fun provideConversationRepository(repo: ConversationsRepositoryImpl): ConversationsRepository

    @Binds
    fun provideCounterRepository(repo: CounterRepositoryImpl): CounterRepository

    @Binds
    fun provideNotificationRepository(repo: NotificationRepositoryImpl): NotificationRepository

}

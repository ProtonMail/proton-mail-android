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
@file:Suppress("unused")

package ch.protonmail.android.di

import androidx.lifecycle.ViewModelProvider
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.activities.settings.NotificationSettingsViewModel
import ch.protonmail.android.api.AccountManager
import ch.protonmail.android.api.NetworkConfigurator
import ch.protonmail.android.api.segments.event.FetchEventsAndReschedule
import ch.protonmail.android.contacts.groups.edit.ContactGroupEditCreateViewModelFactory
import ch.protonmail.android.contacts.groups.edit.chooser.AddressChooserViewModelFactory
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.drawer.presentation.mapper.DrawerFoldersAndLabelsSectionUiModelMapper
import ch.protonmail.android.feature.rating.usecase.StartRateAppFlowIfNeeded
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.usecase.ObserveLabels
import ch.protonmail.android.labels.domain.usecase.ObserveLabelsAndFoldersWithChildren
import ch.protonmail.android.mailbox.domain.ChangeConversationsReadStatus
import ch.protonmail.android.mailbox.domain.ChangeConversationsStarredStatus
import ch.protonmail.android.mailbox.domain.DeleteConversations
import ch.protonmail.android.mailbox.domain.MoveConversationsToFolder
import ch.protonmail.android.mailbox.domain.usecase.MoveMessagesToFolder
import ch.protonmail.android.mailbox.domain.usecase.ObserveAllUnreadCounters
import ch.protonmail.android.mailbox.domain.usecase.ObserveConversationsByLocation
import ch.protonmail.android.mailbox.domain.usecase.ObserveMessagesByLocation
import ch.protonmail.android.mailbox.presentation.mapper.MailboxItemUiModelMapper
import ch.protonmail.android.mailbox.presentation.util.ConversationModeEnabled
import ch.protonmail.android.mailbox.presentation.viewmodel.MailboxViewModel
import ch.protonmail.android.notifications.presentation.usecase.ClearNotificationsForUser
import ch.protonmail.android.settings.domain.usecase.GetMailSettings
import ch.protonmail.android.settings.pin.viewmodel.PinFragmentViewModelFactory
import ch.protonmail.android.usecase.VerifyConnection
import ch.protonmail.android.usecase.delete.DeleteMessage
import ch.protonmail.android.usecase.delete.EmptyFolder
import ch.protonmail.android.usecase.message.ChangeMessagesReadStatus
import ch.protonmail.android.usecase.message.ChangeMessagesStarredStatus
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import me.proton.core.util.kotlin.DispatcherProvider

@Module
@InstallIn(SingletonComponent::class)
internal class ViewModelModule {

    @Provides
    fun provideAddressChooserViewModelFactory(
        addressChooserViewModelFactory: AddressChooserViewModelFactory
    ): ViewModelProvider.NewInstanceFactory = addressChooserViewModelFactory

    @Provides
    fun provideContactGroupEditCreateViewModelFactory(
        contactGroupEditCreateViewModelFactory: ContactGroupEditCreateViewModelFactory
    ): ViewModelProvider.NewInstanceFactory = contactGroupEditCreateViewModelFactory

    @Provides
    fun provideNotificationSettingsViewModelFactory(
        application: ProtonMailApplication,
        userManager: UserManager
    ) = NotificationSettingsViewModel.Factory(application, userManager)

    @Provides
    internal fun provideAccountManager(
        application: ProtonMailApplication,
        dispatcherProvider: DispatcherProvider
    ) = AccountManager.getInstance(application, dispatcherProvider)

    @Provides
    fun providePinFragmentViewModelFactory(
        pinFragmentViewModelFactory: PinFragmentViewModelFactory
    ): ViewModelProvider.NewInstanceFactory = pinFragmentViewModelFactory

    @Suppress("LongParameterList") // Every new parameter adds a new issue and breaks the build
    @Provides
    fun provideMailboxViewModel(
        messageDetailsRepositoryFactory: MessageDetailsRepository.AssistedFactory,
        userManager: UserManager,
        deleteMessage: DeleteMessage,
        labelRepository: LabelRepository,
        verifyConnection: VerifyConnection,
        networkConfigurator: NetworkConfigurator,
        conversationModeEnabled: ConversationModeEnabled,
        observeConversationsByLocation: ObserveConversationsByLocation,
        changeMessagesReadStatus: ChangeMessagesReadStatus,
        changeConversationsReadStatus: ChangeConversationsReadStatus,
        changeMessagesStarredStatus: ChangeMessagesStarredStatus,
        changeConversationsStarredStatus: ChangeConversationsStarredStatus,
        observeMessagesByLocation: ObserveMessagesByLocation,
        observeAllUnreadCounters: ObserveAllUnreadCounters,
        moveConversationsToFolder: MoveConversationsToFolder,
        moveMessagesToFolder: MoveMessagesToFolder,
        deleteConversations: DeleteConversations,
        emptyFolder: EmptyFolder,
        observeLabels: ObserveLabels,
        observeLabelsAndFoldersWithChildren: ObserveLabelsAndFoldersWithChildren,
        drawerFoldersAndLabelsSectionUiModelMapper: DrawerFoldersAndLabelsSectionUiModelMapper,
        getMailSettings: GetMailSettings,
        mailboxItemUiModelMapper: MailboxItemUiModelMapper,
        fetchEventsAndReschedule: FetchEventsAndReschedule,
        clearNotificationsForUser: ClearNotificationsForUser,
        startRateAppFlowIfNeeded: StartRateAppFlowIfNeeded
    ) = MailboxViewModel(
        messageDetailsRepositoryFactory = messageDetailsRepositoryFactory,
        userManager = userManager,
        deleteMessage = deleteMessage,
        labelRepository = labelRepository,
        verifyConnection = verifyConnection,
        networkConfigurator = networkConfigurator,
        conversationModeEnabled = conversationModeEnabled,
        observeMessagesByLocation = observeMessagesByLocation,
        observeConversationsByLocation = observeConversationsByLocation,
        changeMessagesReadStatus = changeMessagesReadStatus,
        changeConversationsReadStatus = changeConversationsReadStatus,
        changeMessagesStarredStatus = changeMessagesStarredStatus,
        changeConversationsStarredStatus = changeConversationsStarredStatus,
        observeAllUnreadCounters = observeAllUnreadCounters,
        moveConversationsToFolder = moveConversationsToFolder,
        moveMessagesToFolder = moveMessagesToFolder,
        deleteConversations = deleteConversations,
        emptyFolder = emptyFolder,
        observeLabels = observeLabels,
        observeLabelsAndFoldersWithChildren = observeLabelsAndFoldersWithChildren,
        drawerFoldersAndLabelsSectionUiModelMapper = drawerFoldersAndLabelsSectionUiModelMapper,
        getMailSettings = getMailSettings,
        mailboxItemUiModelMapper = mailboxItemUiModelMapper,
        fetchEventsAndReschedule = fetchEventsAndReschedule,
        clearNotificationsForUser = clearNotificationsForUser,
        startRateAppFlowIfNeeded = startRateAppFlowIfNeeded
    )
}

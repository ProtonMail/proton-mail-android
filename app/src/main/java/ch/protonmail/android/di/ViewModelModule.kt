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
@file:Suppress("unused")

package ch.protonmail.android.di

import androidx.lifecycle.ViewModelProvider
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.activities.settings.NotificationSettingsViewModel
import ch.protonmail.android.api.AccountManager
import ch.protonmail.android.api.NetworkConfigurator
import ch.protonmail.android.contacts.groups.edit.ContactGroupEditCreateViewModelFactory
import ch.protonmail.android.contacts.groups.edit.chooser.AddressChooserViewModelFactory
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.ContactsRepository
import ch.protonmail.android.drawer.presentation.mapper.DrawerFoldersAndLabelsSectionUiModelMapper
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.mailbox.domain.usecase.MoveMessagesToFolder
import ch.protonmail.android.labels.domain.usecase.ObserveLabels
import ch.protonmail.android.mailbox.data.mapper.MessageRecipientToCorrespondentMapper
import ch.protonmail.android.mailbox.domain.ChangeConversationsReadStatus
import ch.protonmail.android.mailbox.domain.ChangeConversationsStarredStatus
import ch.protonmail.android.mailbox.domain.DeleteConversations
import ch.protonmail.android.mailbox.domain.MoveConversationsToFolder
import ch.protonmail.android.mailbox.domain.usecase.ObserveAllUnreadCounters
import ch.protonmail.android.mailbox.domain.usecase.ObserveConversationModeEnabled
import ch.protonmail.android.mailbox.domain.usecase.ObserveConversationsByLocation
import ch.protonmail.android.mailbox.domain.usecase.ObserveMessagesByLocation
import ch.protonmail.android.mailbox.presentation.ConversationModeEnabled
import ch.protonmail.android.mailbox.presentation.MailboxViewModel
import ch.protonmail.android.settings.domain.GetMailSettings
import ch.protonmail.android.settings.pin.viewmodel.PinFragmentViewModelFactory
import ch.protonmail.android.usecase.VerifyConnection
import ch.protonmail.android.usecase.delete.DeleteMessage
import ch.protonmail.android.viewmodel.ManageLabelsDialogViewModel
import com.birbit.android.jobqueue.JobManager
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
    internal fun provideAccountManager(application: ProtonMailApplication) = AccountManager.getInstance(application)

    @Provides
    fun providePinFragmentViewModelFactory(
        pinFragmentViewModelFactory: PinFragmentViewModelFactory
    ): ViewModelProvider.NewInstanceFactory = pinFragmentViewModelFactory

    @Provides
    fun provideManageLabelsDialogViewModelFactory(
        factory: ManageLabelsDialogViewModel.ManageLabelsDialogViewModelFactory
    ): ViewModelProvider.NewInstanceFactory = factory

    @Provides
    fun provideMailboxViewModel(
        messageDetailsRepository: MessageDetailsRepository,
        userManager: UserManager,
        jobManager: JobManager,
        deleteMessage: DeleteMessage,
        dispatchers: DispatcherProvider,
        contactsRepository: ContactsRepository,
        labelRepository: LabelRepository,
        verifyConnection: VerifyConnection,
        networkConfigurator: NetworkConfigurator,
        conversationModeEnabled: ConversationModeEnabled,
        observeConversationsByLocation: ObserveConversationsByLocation,
        observeConversationModeEnabled: ObserveConversationModeEnabled,
        changeConversationsReadStatus: ChangeConversationsReadStatus,
        changeConversationsStarredStatus: ChangeConversationsStarredStatus,
        observeMessagesByLocation: ObserveMessagesByLocation,
        observeAllUnreadCounters: ObserveAllUnreadCounters,
        moveConversationsToFolder: MoveConversationsToFolder,
        moveMessagesToFolder: MoveMessagesToFolder,
        deleteConversations: DeleteConversations,
        observeLabels: ObserveLabels,
        drawerFoldersAndLabelsSectionUiModelMapper: DrawerFoldersAndLabelsSectionUiModelMapper,
        getMailSettings: GetMailSettings,
        messageRecipientToCorrespondentMapper: MessageRecipientToCorrespondentMapper
    ) = MailboxViewModel(
        messageDetailsRepository = messageDetailsRepository,
        userManager = userManager,
        jobManager = jobManager,
        deleteMessage = deleteMessage,
        dispatchers = dispatchers,
        contactsRepository = contactsRepository,
        labelRepository = labelRepository,
        verifyConnection = verifyConnection,
        networkConfigurator = networkConfigurator,
        conversationModeEnabled = conversationModeEnabled,
        observeMessagesByLocation = observeMessagesByLocation,
        observeConversationsByLocation = observeConversationsByLocation,
        observeConversationModeEnabled = observeConversationModeEnabled,
        changeConversationsReadStatus = changeConversationsReadStatus,
        changeConversationsStarredStatus = changeConversationsStarredStatus,
        observeAllUnreadCounters = observeAllUnreadCounters,
        moveConversationsToFolder = moveConversationsToFolder,
        moveMessagesToFolder = moveMessagesToFolder,
        deleteConversations = deleteConversations,
        observeLabels = observeLabels,
        drawerFoldersAndLabelsSectionUiModelMapper = drawerFoldersAndLabelsSectionUiModelMapper,
        getMailSettings = getMailSettings,
        messageRecipientToCorrespondentMapper = messageRecipientToCorrespondentMapper
    )
}

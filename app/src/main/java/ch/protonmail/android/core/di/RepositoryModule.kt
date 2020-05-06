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

package ch.protonmail.android.core.di

import android.content.Context
import androidx.lifecycle.ViewModelProvider
import ch.protonmail.android.activities.messageDetails.MessageRenderer
import ch.protonmail.android.activities.messageDetails.repository.MessageDetailsRepository
import ch.protonmail.android.activities.messageDetails.viewmodel.MessageDetailsViewModel
import ch.protonmail.android.activities.multiuser.viewModel.AccountManagerViewModel
import ch.protonmail.android.activities.multiuser.viewModel.ConnectAccountMailboxLoginViewModel
import ch.protonmail.android.activities.multiuser.viewModel.ConnectAccountViewModel
import ch.protonmail.android.activities.settings.NotificationSettingsViewModel
import ch.protonmail.android.api.AccountManager
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.room.attachmentMetadata.AttachmentMetadataDatabase
import ch.protonmail.android.api.models.room.contacts.ContactsDatabase
import ch.protonmail.android.api.models.room.messages.MessagesDatabase
import ch.protonmail.android.api.models.room.pendingActions.PendingActionsDatabase
import ch.protonmail.android.compose.ComposeMessageRepository
import ch.protonmail.android.compose.ComposeMessageViewModelFactory
import ch.protonmail.android.compose.recipients.GroupRecipientsViewModelFactory
import ch.protonmail.android.contacts.ContactsViewModelFactory
import ch.protonmail.android.contacts.details.ContactDetailsRepository
import ch.protonmail.android.contacts.details.ContactDetailsViewModelFactory
import ch.protonmail.android.contacts.details.edit.EditContactDetailsRepository
import ch.protonmail.android.contacts.details.edit.EditContactDetailsViewModelFactory
import ch.protonmail.android.contacts.groups.details.ContactGroupDetailsRepository
import ch.protonmail.android.contacts.groups.details.ContactGroupDetailsViewModelFactory
import ch.protonmail.android.contacts.groups.edit.ContactGroupEditCreateRepository
import ch.protonmail.android.contacts.groups.edit.ContactGroupEditCreateViewModelFactory
import ch.protonmail.android.contacts.groups.edit.chooser.AddressChooserRepository
import ch.protonmail.android.contacts.groups.edit.chooser.AddressChooserViewModelFactory
import ch.protonmail.android.contacts.groups.list.ContactGroupsRepository
import ch.protonmail.android.contacts.groups.list.ContactGroupsViewModelFactory
import ch.protonmail.android.contacts.list.viewModel.ContactListRepository
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.ContactsRepository
import ch.protonmail.android.settings.pin.viewmodel.PinFragmentViewModelFactory
import com.birbit.android.jobqueue.JobManager
import dagger.Module
import dagger.Provides
import javax.inject.Named
import javax.inject.Singleton

/**
 * Created by kadrikj on 8/28/18. */
@Module
class RepositoryModule {

    // region view models factories
    @Provides
    fun provideEditContactDetailsViewModelFactory(editContactDetailsViewModelFactory: EditContactDetailsViewModelFactory):
            ViewModelProvider.NewInstanceFactory {
        return editContactDetailsViewModelFactory
    }

    @Provides
    fun provideContactsViewModelFactory(contactsViewModelFactory: ContactsViewModelFactory):
            ViewModelProvider.NewInstanceFactory {
        return contactsViewModelFactory
    }

    @Provides
    fun provideAddressChooserViewModelFactory(addressChooserViewModelFactory: AddressChooserViewModelFactory):
            ViewModelProvider.NewInstanceFactory {
        return addressChooserViewModelFactory
    }

    @Provides
    fun provideContactGroupsViewModelFactory(contactGroupsViewModelFactory: ContactGroupsViewModelFactory):
            ViewModelProvider.NewInstanceFactory {
        return contactGroupsViewModelFactory
    }

    @Provides
    fun provideContactGroupDetailsViewModelFactory(contactGroupDetailsViewModelFactory: ContactGroupDetailsViewModelFactory): ViewModelProvider.NewInstanceFactory {
        return contactGroupDetailsViewModelFactory
    }

    @Provides
    fun provideContactDetailsViewModelFactory(contactDetailsViewModelFactory: ContactDetailsViewModelFactory): ViewModelProvider.NewInstanceFactory {
        return contactDetailsViewModelFactory
    }

    @Provides
    fun provideContactGroupEditCreateViewModelFactory(contactGroupEditCreateViewModelFactory: ContactGroupEditCreateViewModelFactory): ViewModelProvider.NewInstanceFactory {
        return contactGroupEditCreateViewModelFactory
    }

    @Provides
    fun provideComposeMessageViewModelFactory(composeMessageViewModelFactory: ComposeMessageViewModelFactory): ViewModelProvider.NewInstanceFactory {
        return composeMessageViewModelFactory
    }

    @Provides
    fun provideGroupRecipientsViewModelFactory(groupRecipientsViewModelFactory: GroupRecipientsViewModelFactory): ViewModelProvider.NewInstanceFactory {
        return groupRecipientsViewModelFactory
    }

    @Provides
    fun provideMessageDetailsViewModelFactory(
            messageDetailsRepository: MessageDetailsRepository,
            userManager: UserManager,
            contactsRepository: ContactsRepository,
            attachmentMetadataDatabase: AttachmentMetadataDatabase,
            messageRendererFactory: MessageRenderer.Factory
    ) = MessageDetailsViewModel.Factory(
            messageDetailsRepository,
            userManager,
            contactsRepository,
            attachmentMetadataDatabase,
            messageRendererFactory
    )

    @Provides
    internal fun provideNotificationSettingsViewModelFactory(
            application: ProtonMailApplication,
            userManager: UserManager
    ) = NotificationSettingsViewModel.Factory( application, userManager )

    @Provides
    internal fun provideConnectAccountViewModelFactory(
            application: ProtonMailApplication,
            userManager: UserManager
    ) = ConnectAccountViewModel.Factory(application, userManager)

    @Provides
    internal fun provideConnectAccountMailboxLoginViewModelFactory(
            application: ProtonMailApplication,
            userManager: UserManager
    ) = ConnectAccountMailboxLoginViewModel.Factory(application, userManager)

    @Provides
    internal fun provideAccountManager(application: ProtonMailApplication) = AccountManager.getInstance(application)

    @Provides
    internal fun provideAccountManagerViewModelFactory(
            application: ProtonMailApplication,
            userManager: UserManager,
            accountManager: AccountManager
    ) = AccountManagerViewModel.Factory(application, userManager, accountManager)

    @Provides
    fun providePinFragmentViewModelFactory(pinFragmentViewModelFactory: PinFragmentViewModelFactory):
            ViewModelProvider.NewInstanceFactory {
        return pinFragmentViewModelFactory
    }
    // endregion

    // region repositories
    @Provides
    @Singleton
    fun provideEditContactDetailsRepository(jobManager: JobManager, protonMailApi: ProtonMailApiManager, databaseProvider: DatabaseProvider)
            : EditContactDetailsRepository = EditContactDetailsRepository(jobManager, protonMailApi, databaseProvider)

    @Provides
    @Singleton
    fun provideAddressChooserRepository(databaseProvider: DatabaseProvider): AddressChooserRepository = AddressChooserRepository(databaseProvider)

    @Provides
    @Singleton
    fun provideContactGroupsRepository(jobManager: JobManager, protonMailApi: ProtonMailApiManager, databaseProvider: DatabaseProvider):
            ContactGroupsRepository = ContactGroupsRepository(jobManager, protonMailApi, databaseProvider)

    @Provides
    @Singleton
    fun provideContactGroupDetailsRepository(jobManager: JobManager, protonMailApi: ProtonMailApiManager, databaseProvider: DatabaseProvider)
            : ContactGroupDetailsRepository = ContactGroupDetailsRepository(jobManager, protonMailApi, databaseProvider)

    @Provides
    @Singleton
    fun provideContactGroupEditCreateRepository(jobManager: JobManager, protonMailApi: ProtonMailApiManager, databaseProvider: DatabaseProvider)
            : ContactGroupEditCreateRepository = ContactGroupEditCreateRepository(jobManager, protonMailApi, databaseProvider)

    @Provides
    @Singleton
    fun provideContactDetailsRepository(jobManager: JobManager, protonMailApi: ProtonMailApiManager, databaseProvider: DatabaseProvider)
            : ContactDetailsRepository = ContactDetailsRepository(jobManager, protonMailApi, databaseProvider)

    @Provides
    fun provideComposeMessageRepository(jobManager: JobManager, protonMailApi: ProtonMailApiManager, databaseProvider: DatabaseProvider, @Named("messages") messagesDatabase: MessagesDatabase, @Named("messages_search") searchDatabase: MessagesDatabase, messageDetailsRepository: MessageDetailsRepository)
            : ComposeMessageRepository = ComposeMessageRepository(jobManager, protonMailApi, databaseProvider, messagesDatabase, searchDatabase, messageDetailsRepository)

    @Provides
    @Singleton
    fun provideContactListRepository(jobManager: JobManager, protonMailApi: ProtonMailApiManager, contactsDatabase: ContactsDatabase):
            ContactListRepository = ContactListRepository(jobManager, protonMailApi, contactsDatabase)

    @Provides
    fun provideMessageDetailsRepository(jobManager: JobManager, protonMailApi: ProtonMailApiManager, @Named("messages_search") searchDatabase: MessagesDatabase, pendingActionsDatabase: PendingActionsDatabase, applicationContext: Context, databaseProvider: DatabaseProvider):
        MessageDetailsRepository = MessageDetailsRepository(jobManager, protonMailApi, searchDatabase, pendingActionsDatabase, applicationContext, databaseProvider)
    // endregion
}

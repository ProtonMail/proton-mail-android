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
import ch.protonmail.android.activities.multiuser.viewModel.AccountManagerViewModel
import ch.protonmail.android.activities.multiuser.viewModel.ConnectAccountMailboxLoginViewModel
import ch.protonmail.android.activities.multiuser.viewModel.ConnectAccountViewModel
import ch.protonmail.android.activities.settings.NotificationSettingsViewModel
import ch.protonmail.android.api.AccountManager
import ch.protonmail.android.compose.ComposeMessageViewModelFactory
import ch.protonmail.android.compose.recipients.GroupRecipientsViewModelFactory
import ch.protonmail.android.contacts.groups.details.ContactGroupDetailsViewModelFactory
import ch.protonmail.android.contacts.groups.edit.ContactGroupEditCreateViewModelFactory
import ch.protonmail.android.contacts.groups.edit.chooser.AddressChooserViewModelFactory
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.settings.pin.viewmodel.PinFragmentViewModelFactory
import ch.protonmail.android.viewmodel.ManageLabelsDialogViewModel
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/*
 * Created by kadrikj on 8/28/18. */

// TODO here we're providing factories by injecting the ViewModel, this is wrong, we should get rid of it!
//  With Hilt + Assisted Inject we can avoid the Factory - see `val myViewModel by viewModels<MyViewModel>()`
@Module
@InstallIn(SingletonComponent::class)
internal class ViewModelModule {

    @Provides
    fun provideAddressChooserViewModelFactory(addressChooserViewModelFactory: AddressChooserViewModelFactory):
            ViewModelProvider.NewInstanceFactory {
        return addressChooserViewModelFactory
    }

    @Provides
    fun provideContactGroupDetailsViewModelFactory(contactGroupDetailsViewModelFactory: ContactGroupDetailsViewModelFactory): ViewModelProvider.NewInstanceFactory {
        return contactGroupDetailsViewModelFactory
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

    @Provides
    fun provideManageLabelsDialogViewModelFactory(factory: ManageLabelsDialogViewModel.ManageLabelsDialogViewModelFactory):
        ViewModelProvider.NewInstanceFactory {
        return factory
    }

}

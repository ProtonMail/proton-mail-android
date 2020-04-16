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
package ch.protonmail.android.core.di

import ch.protonmail.android.activities.composeMessage.ComposeMessageActivity
import ch.protonmail.android.activities.messageDetails.MessageDetailsActivity
import ch.protonmail.android.activities.multiuser.AccountManagerActivity
import ch.protonmail.android.activities.multiuser.ConnectAccountActivity
import ch.protonmail.android.activities.multiuser.ConnectAccountMailboxLoginActivity
import ch.protonmail.android.activities.settings.NotificationSettingsActivity
import ch.protonmail.android.contacts.ContactsActivity
import ch.protonmail.android.contacts.details.ContactDetailsActivity
import ch.protonmail.android.contacts.details.edit.EditContactDetailsActivity
import ch.protonmail.android.contacts.groups.details.ContactGroupDetailsActivity
import ch.protonmail.android.contacts.groups.edit.ContactGroupEditCreateActivity
import ch.protonmail.android.contacts.groups.edit.chooser.AddressChooserActivity
import ch.protonmail.android.settings.pin.ChangePinActivity
import ch.protonmail.android.settings.pin.CreatePinActivity
import ch.protonmail.android.settings.pin.ValidatePinActivity
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * Created by kadrikj on 8/22/18. */
@Module
abstract class ActivityModule {

    @ContributesAndroidInjector
    abstract fun contributeAddressChooserActivity(): AddressChooserActivity
    @ContributesAndroidInjector(modules = [(FragmentModule::class)])
    abstract fun contributeChangePinActivity(): ChangePinActivity
    @ContributesAndroidInjector(modules = [(FragmentModule::class)])
    abstract fun contributeComposeMessageActivity(): ComposeMessageActivity
    @ContributesAndroidInjector(modules = [(FragmentModule::class)])
    abstract fun contributeContactGroupsActivity(): ContactsActivity
    @ContributesAndroidInjector
    abstract fun contributeContactGroupDetailsActivity(): ContactGroupDetailsActivity
    @ContributesAndroidInjector
    abstract fun contributeContactGroupEditCreateActivity(): ContactGroupEditCreateActivity
    @ContributesAndroidInjector
    abstract fun contributeContactDetailsActivity(): ContactDetailsActivity
    @ContributesAndroidInjector(modules = [(FragmentModule::class)])
    abstract fun contributeCreatePinActivity(): CreatePinActivity
    @ContributesAndroidInjector
    abstract fun contributeEditContactDetailsActivity(): EditContactDetailsActivity
    @ContributesAndroidInjector
    abstract fun contributeMessageDetailsActivity(): MessageDetailsActivity
    @ContributesAndroidInjector
    internal abstract fun contributeNotificationSettingsActivity() : NotificationSettingsActivity
    @ContributesAndroidInjector(modules = [(FragmentModule::class)])
    abstract fun contributeValidatePinActivity() : ValidatePinActivity
    @ContributesAndroidInjector
    internal abstract fun contributeConnectAccountActivity() : ConnectAccountActivity
    @ContributesAndroidInjector
    internal abstract fun contributeConnectAccountMailboxLoginActivity() : ConnectAccountMailboxLoginActivity
    @ContributesAndroidInjector
    internal abstract fun contributeAccountManagerActivity() : AccountManagerActivity
}
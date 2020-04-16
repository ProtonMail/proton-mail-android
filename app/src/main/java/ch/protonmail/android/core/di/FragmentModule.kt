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

import ch.protonmail.android.compose.recipients.GroupRecipientsDialogFragment
import ch.protonmail.android.contacts.groups.list.ContactGroupsFragment
import ch.protonmail.android.contacts.list.ContactsListFragment
import ch.protonmail.android.settings.pin.PinFragment
import dagger.Module
import dagger.android.ContributesAndroidInjector

/**
 * Created by kadrikj on 8/24/18. */
@Module
abstract class FragmentModule {

    @ContributesAndroidInjector
    abstract fun contributeContactGroupsFragment(): ContactGroupsFragment

    @ContributesAndroidInjector
    abstract fun contributeContactListFragment(): ContactsListFragment

    @ContributesAndroidInjector
    abstract fun contributeGroupRecipientsFragment(): GroupRecipientsDialogFragment

    @ContributesAndroidInjector
    abstract fun contributePinFragment(): PinFragment
}
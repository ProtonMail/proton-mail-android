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
package ch.protonmail.android.contacts

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.switchMap
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.usecase.fetch.FetchContactsData
import timber.log.Timber

class ContactsViewModel @ViewModelInject constructor(
    private val userManager: UserManager,
    private val fetchContactsData: FetchContactsData
) : ViewModel() {

    private val fetchContactsTrigger: MutableLiveData<Unit> = MutableLiveData()

    val fetchContactsResult: LiveData<Boolean> =
        fetchContactsTrigger.switchMap { fetchContactsData() }

    fun fetchContacts() {
        Timber.v("fetchContacts")
        fetchContactsTrigger.value = Unit
    }

    fun isPaidUser(): Boolean = userManager.user.isPaidUser

}

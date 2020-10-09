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
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.usecase.NETWORK_CHECK_DELAY
import ch.protonmail.android.usecase.SendPing
import ch.protonmail.android.usecase.fetch.FetchContactsData
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber

class ContactsViewModel @ViewModelInject constructor(
    private val userManager: UserManager,
    private val fetchContactsData: FetchContactsData,
    private val sendPing: SendPing
) : ViewModel() {

    private val fetchContactsTrigger: MutableLiveData<Unit> = MutableLiveData()
    private val _pingTrigger: MutableLiveData<Unit> = MutableLiveData()

    val fetchContactsResult: LiveData<Boolean> =
        fetchContactsTrigger.switchMap { fetchContactsData() }

    val hasConnection: LiveData<Boolean> = _pingTrigger
        .switchMap { sendPing() }

    fun isPaidUser(): Boolean = userManager.user.isPaidUser

    fun fetchContacts() {
        Timber.v("fetchContacts")
        fetchContactsTrigger.value = Unit
    }

    fun checkConnectivity() {
        Timber.v("checkConnectivity launch ping")
        _pingTrigger.value = Unit
    }

    /**
     * Check connectivity with a delay allowing snack bar to be displayed.
     */
    fun checkConnectivityDelayed() {
        viewModelScope.launch {
            delay(NETWORK_CHECK_DELAY)
            checkConnectivity()
        }
    }
}

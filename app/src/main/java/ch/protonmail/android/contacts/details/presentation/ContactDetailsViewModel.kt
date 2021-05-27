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

package ch.protonmail.android.contacts.details.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import ch.protonmail.android.contacts.details.domain.FetchContactDetails
import ch.protonmail.android.contacts.details.domain.model.FetchContactDetailsResult
import ch.protonmail.android.worker.DeleteContactWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import me.proton.core.user.domain.UserManager
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ContactDetailsViewModel @Inject constructor(
    private val fetchContactDetails: FetchContactDetails,
    private val userManager: UserManager,
    private val workManager: WorkManager
) : ViewModel() {

    private val mutableContactsResultFlow =
        MutableStateFlow<FetchContactDetailsResult>(FetchContactDetailsResult.Loading)
    val contactsResultFlow: StateFlow<FetchContactDetailsResult>
        get() = mutableContactsResultFlow

    fun getContactDetails(contactId: String) {
        Timber.v("getContactDetails for $contactId")
        viewModelScope.launch {
            fetchContactDetails(contactId)
                .catch { FetchContactDetailsResult.Error(it) }
                .collect { mutableContactsResultFlow.value = it }
        }
    }

    fun deleteContact(contactId: String) = DeleteContactWorker.Enqueuer(workManager).enqueue(listOf(contactId))

}

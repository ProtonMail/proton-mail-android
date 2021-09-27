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
package ch.protonmail.android.contacts.groups.list

import android.database.SQLException
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.api.models.room.contacts.ContactLabel
import ch.protonmail.android.contacts.ErrorEnum
import ch.protonmail.android.contacts.list.search.ISearchListenerViewModel
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.usecase.delete.DeleteLabel
import ch.protonmail.android.utils.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber

class ContactGroupsViewModel @ViewModelInject constructor(
    private val contactGroupsRepository: ContactGroupsRepository,
    private val userManager: UserManager,
    private val deleteLabel: DeleteLabel
) : ViewModel(), ISearchListenerViewModel {

    private val _contactGroupsResult: MutableLiveData<List<ContactLabel>> = MutableLiveData()
    private val _contactGroupsError: MutableLiveData<Event<String>> = MutableLiveData()
    private var searchPhraseFlow = MutableStateFlow("")
    private val _contactGroupEmailsResult: MutableLiveData<Event<List<ContactEmail>>> = MutableLiveData()
    private val _contactGroupEmailsError: MutableLiveData<Event<String>> = MutableLiveData()

    val contactGroupsResult: LiveData<List<ContactLabel>>
        get() = _contactGroupsResult

    val contactGroupsError: LiveData<Event<String>>
        get() = _contactGroupsError

    val contactGroupEmailsResult: LiveData<Event<List<ContactEmail>>>
        get() = _contactGroupEmailsResult

    val contactGroupEmailsError: LiveData<Event<String>>
        get() = _contactGroupEmailsError

    fun observeContactGroups() {
        // observe db changes
        contactGroupsRepository.getJoins()
            .combine(searchPhraseFlow) { _, searchPhrase -> searchPhrase }
            .onEach { Timber.v("Search term: $it") }
            .flatMapLatest { searchPhrase ->
                contactGroupsRepository.observeContactGroups(searchPhrase)
            }
            .catch { _contactGroupsError.value = Event(it.message ?: ErrorEnum.INVALID_EMAIL_LIST.name) }
            .onEach { labels ->
                Timber.d("Contacts groups labels received size: ${labels.size}")
                _contactGroupsResult.value = labels
            }
            .launchIn(viewModelScope)
    }

    override fun setSearchPhrase(searchPhrase: String?) {
        if (searchPhrase != null) {
            searchPhraseFlow.value = searchPhrase
        }
    }

    fun deleteSelected(contactGroups: List<ContactLabel>) {
        val labelIds = contactGroups.map { it.ID }
        Timber.v("Delete labelIds $labelIds")
        viewModelScope.launch {
            deleteLabel(labelIds)
            // reply with empty success status
            _contactGroupsResult.value = emptyList()
        }
    }

    fun getContactGroupEmails(contactLabel: ContactLabel) {
        viewModelScope.launch {
            runCatching { contactGroupsRepository.getContactGroupEmails(contactLabel.ID) }
                .fold(
                    onSuccess = { list ->
                        Timber.v("Contacts groups emails list received size: ${list.size}")
                        _contactGroupEmailsResult.value = Event(list)
                    },
                    onFailure = { throwable ->
                        if (throwable is SQLException) {
                            _contactGroupEmailsError.value = Event(
                                throwable.message ?: ErrorEnum.INVALID_EMAIL_LIST.name
                            )
                        } else
                            throw throwable
                    }
                )
        }
    }

    fun isPaidUser(): Boolean = userManager.user.isPaidUser
}

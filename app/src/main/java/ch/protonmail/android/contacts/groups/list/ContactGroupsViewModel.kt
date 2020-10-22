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

import android.annotation.SuppressLint
import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.api.models.room.contacts.ContactLabel
import ch.protonmail.android.api.rx.ThreadSchedulers
import ch.protonmail.android.contacts.ErrorEnum
import ch.protonmail.android.contacts.list.search.ISearchListenerViewModel
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.usecase.delete.DeleteLabel
import ch.protonmail.android.utils.Event
import io.reactivex.Scheduler
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

class ContactGroupsViewModel @Inject constructor(
    private val contactGroupsRepository: ContactGroupsRepository,
    private val userManager: UserManager,
    private val deleteLabel: DeleteLabel
) : ViewModel(), ISearchListenerViewModel {

    private val _contactGroupsResult: MutableLiveData<List<ContactLabel>> = MutableLiveData()
    private val _contactGroupsError: MutableLiveData<Event<String>> = MutableLiveData()
    private var _searchPhrase: String = ""
    private val _contactGroupEmailsResult: MutableLiveData<Event<List<ContactEmail>>> = MutableLiveData()
    private val _contactGroupEmailsError: MutableLiveData<Event<String>> = MutableLiveData()

    private lateinit var _contactGroups: List<ContactLabel>

    val contactGroupsResult: LiveData<List<ContactLabel>>
        get() = _contactGroupsResult

    val contactGroupsError: LiveData<Event<String>>
        get() = _contactGroupsError

    val contactGroupEmailsResult: LiveData<Event<List<ContactEmail>>>
        get() = _contactGroupEmailsResult

    val contactGroupEmailsError: LiveData<Event<String>>
        get() = _contactGroupEmailsError

    @SuppressLint("CheckResult")
    fun watchForJoins(schedulers: Scheduler) {
        contactGroupsRepository.getJoins()
            .subscribeOn(ThreadSchedulers.io())
            .observeOn(schedulers)
            .subscribe {
                fetchContactGroups(schedulers)
            }
    }

    @SuppressLint("CheckResult")
    fun fetchContactGroups(schedulers: Scheduler) {
        if (_searchPhrase.isEmpty()) {
            contactGroupsRepository.getContactGroups().subscribeOn(ThreadSchedulers.io())
                .observeOn(schedulers).subscribe(
                    {
                        _contactGroups = it
                        _contactGroupsResult.postValue(it)
                    },
                    {
                        _contactGroupsError.value = Event(it.message ?: ErrorEnum.INVALID_EMAIL_LIST.name)
                    }
                )
        } else {
            setSearchPhrase(_searchPhrase)
        }
    }

    @SuppressLint("CheckResult")
    override fun setSearchPhrase(searchPhrase: String?) {
        if (searchPhrase != null) {
            _searchPhrase = searchPhrase
            if (TextUtils.isEmpty(searchPhrase)) {
                fetchContactGroups(ThreadSchedulers.main()) // todo move this out of a depedency
            }
            val searchPhraseQuery = "%$searchPhrase%"
            contactGroupsRepository.getContactGroups(searchPhraseQuery).subscribeOn(ThreadSchedulers.io())
                .observeOn(ThreadSchedulers.main()).subscribe(
                    {
                        _contactGroupsResult.postValue(it)
                    },
                    {
                        _contactGroupsError.value = Event(it.message ?: ErrorEnum.INVALID_GROUP_LIST.name)
                    }
                )
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

    @SuppressLint("CheckResult")
    fun getContactGroupEmails(contactLabel: ContactLabel) {
        contactGroupsRepository.getContactGroupEmails(contactLabel.ID)
            .subscribeOn(ThreadSchedulers.io())
            .observeOn(ThreadSchedulers.main()).subscribe(
                {
                    _contactGroupEmailsResult.postValue(Event(it))
                },
                {
                    _contactGroupEmailsError.value = Event(it.message ?: ErrorEnum.INVALID_EMAIL_LIST.name)
                }
            )
    }

    fun isPaidUser(): Boolean = userManager.user?.isPaidUser ?: false
}

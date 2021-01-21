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
package ch.protonmail.android.contacts.groups.details

import android.annotation.SuppressLint
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.api.models.room.contacts.ContactLabel
import ch.protonmail.android.api.rx.ThreadSchedulers
import ch.protonmail.android.contacts.ErrorEnum
import ch.protonmail.android.usecase.delete.DeleteLabel
import ch.protonmail.android.utils.Event
import com.jakewharton.rxrelay2.PublishRelay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.concurrent.TimeUnit

class ContactGroupDetailsViewModel @ViewModelInject constructor(
    private val contactGroupDetailsRepository: ContactGroupDetailsRepository,
    private val deleteLabel: DeleteLabel
) : ViewModel() {

    private lateinit var _contactLabel: ContactLabel
    private lateinit var _data: List<ContactEmail>
    private val _contactGroupEmailsResult: MutableLiveData<List<ContactEmail>> = MutableLiveData()
    private val _filteringPublishSubject = PublishRelay.create<String>()
    private val _contactGroupEmailsEmpty: MutableLiveData<Event<String>> = MutableLiveData()
    private val _setupUIData = MutableLiveData<ContactLabel>()
    private val _deleteLabelIds: MutableLiveData<List<String>> = MutableLiveData()

    init {
        initFiltering()
    }

    val contactGroupEmailsResult: LiveData<List<ContactEmail>>
        get() = _contactGroupEmailsResult

    val contactGroupEmailsEmpty: LiveData<Event<String>>
        get() = _contactGroupEmailsEmpty

    val setupUIData: LiveData<ContactLabel>
        get() = _setupUIData

    val deleteGroupStatus: LiveData<Event<Status>>
        get() = _deleteLabelIds.switchMap {
            processDeleteLiveData(it)
        }

    private fun processDeleteLiveData(contactsToDelete: List<String>): LiveData<Event<Status>> {
        return liveData {
            emitSource(
                deleteLabel(contactsToDelete)
                    .map { isSuccess ->
                        if (isSuccess) {
                            Event(Status.SUCCESS)
                        } else {
                            Event(Status.ERROR)
                        }
                    }
            )
        }
    }

    fun setData(contactLabel: ContactLabel?) {
        contactLabel?.let { newContact ->
            _contactLabel = newContact
            getContactGroupEmails(newContact)
            watchForContactGroup()
            _setupUIData.postValue(newContact)
        }
    }

    fun getData(): ContactLabel = _contactLabel

    private fun watchForContactGroup() {
        viewModelScope.launch {
            runCatching { contactGroupDetailsRepository.findContactGroupDetails(_contactLabel.ID) }
                .fold(
                    onSuccess = { contactLabel ->
                        Timber.v("ContactLabel: $contactLabel retrieved")
                        contactLabel?.let {
                            _contactLabel = it
                        }
                    },
                    onFailure = {
                        _contactGroupEmailsEmpty.value = Event(it.message ?: ErrorEnum.DEFAULT_ERROR.name)
                    }
                )
        }

    }

    private fun getContactGroupEmails(contactLabel: ContactLabel) {
        viewModelScope.launch {
            runCatching { contactGroupDetailsRepository.getContactGroupEmails(contactLabel.ID) }
                .fold(
                    onSuccess = {
                        _data = it
                        watchForContactGroup()
                        _contactGroupEmailsResult.postValue(it)
                    },
                    onFailure = {
                        _contactGroupEmailsEmpty.value = Event(it.message ?: ErrorEnum.INVALID_EMAIL_LIST.name)
                    }
                )
        }
    }

    @SuppressLint("CheckResult")
    private fun initFiltering() {
        _filteringPublishSubject
            .debounce(300, TimeUnit.MILLISECONDS)
            .distinctUntilChanged()
            .switchMap { contactGroupDetailsRepository.filterContactGroupEmails(_contactLabel.ID, it) }
            .subscribeOn(ThreadSchedulers.io())
            .observeOn(ThreadSchedulers.main())
            .subscribe(
                {
                    _contactGroupEmailsResult.postValue(it)
                },
                {
                    _contactGroupEmailsEmpty.value = Event(it.message ?: ErrorEnum.DEFAULT_ERROR.name)
                }
            )
    }

    fun doFilter(filter: String) {
        _filteringPublishSubject.accept(filter.trim())
    }

    fun delete() {
        _deleteLabelIds.value = listOf(_contactLabel.ID)
    }

    enum class Status(var message: String?) {
        SUCCESS(""),
        ERROR("");
    }
}

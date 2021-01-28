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

import android.database.SQLException
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
import ch.protonmail.android.contacts.ErrorEnum
import ch.protonmail.android.usecase.delete.DeleteLabel
import ch.protonmail.android.utils.Event
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.time.milliseconds

class ContactGroupDetailsViewModel @ViewModelInject constructor(
    private val contactGroupDetailsRepository: ContactGroupDetailsRepository,
    private val deleteLabel: DeleteLabel
) : ViewModel() {

    private lateinit var _contactLabel: ContactLabel
    private val _contactGroupEmailsResult: MutableLiveData<List<ContactEmail>> = MutableLiveData()
    private val filteringChannel = BroadcastChannel<String>(1)
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
            _setupUIData.value = newContact
        }
    }

    fun getData(): ContactLabel = _contactLabel

    private fun getContactGroupEmails(contactLabel: ContactLabel) {
        contactGroupDetailsRepository.getContactGroupEmails(contactLabel.ID)
            .onEach { list ->
                updateContactGroup()
                _contactGroupEmailsResult.postValue(list)
            }
            .catch { throwable ->
                _contactGroupEmailsEmpty.value = Event(
                    throwable.message ?: ErrorEnum.INVALID_EMAIL_LIST.name
                )
            }
            .launchIn(viewModelScope)
    }

    private suspend fun updateContactGroup() {
        runCatching { contactGroupDetailsRepository.findContactGroupDetails(_contactLabel.ID) }
            .fold(
                onSuccess = { contactLabel ->
                    Timber.v("ContactLabel: $contactLabel retrieved")
                    contactLabel?.let {
                        _contactLabel = it
                    }
                },
                onFailure = { throwable ->
                    if (throwable is SQLException) {
                        _contactGroupEmailsEmpty.value = Event(throwable.message ?: ErrorEnum.DEFAULT_ERROR.name)
                    } else
                        throw throwable
                }
            )
    }

    private fun initFiltering() {
        filteringChannel
            .asFlow()
            .debounce(300.milliseconds)
            .distinctUntilChanged()
            .flatMapLatest { contactGroupDetailsRepository.filterContactGroupEmails(_contactLabel.ID, it) }
            .catch {
                _contactGroupEmailsEmpty.value = Event(it.message ?: ErrorEnum.DEFAULT_ERROR.name)
            }
            .onEach { list ->
                Timber.v("Filtered emails list size: ${list.size}")
                _contactGroupEmailsResult.postValue(list)
            }
            .launchIn(viewModelScope)
    }

    fun doFilter(filter: String) {
        viewModelScope.launch {
            filteringChannel.send(filter.trim())
        }
    }

    fun delete() {
        _deleteLabelIds.value = listOf(_contactLabel.ID)
    }

    enum class Status(var message: String?) {
        SUCCESS(""),
        ERROR("");
    }
}

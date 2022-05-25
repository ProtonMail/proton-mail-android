/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.contacts.groups.details

import android.database.SQLException
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.contacts.ErrorEnum
import ch.protonmail.android.contacts.groups.list.ContactGroupListItem
import ch.protonmail.android.contacts.list.viewModel.ContactsListMapper
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.ContactsRepository
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.usecase.DeleteLabels
import ch.protonmail.android.mailbox.domain.usecase.MoveMessagesToFolder
import ch.protonmail.android.utils.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@HiltViewModel
class ContactGroupDetailsViewModel @Inject constructor(
    private val contactGroupDetailsRepository: ContactGroupDetailsRepository,
    private val deleteLabels: DeleteLabels,
    private val contactsMapper: ContactsListMapper,
    private val contactRepository: ContactsRepository,
    private val moveMessagesToFolder: MoveMessagesToFolder,
    private val userManager: UserManager
) : ViewModel() {

    private lateinit var _contactLabel: ContactGroupListItem
    private val _contactGroupItemFlow = MutableStateFlow<ContactGroupListItem?>(null)
    private val _contactGroupEmailsResult: MutableLiveData<List<ContactEmail>> = MutableLiveData()
    private val filteringChannel = MutableSharedFlow<String>(replay = 1)
    private val _contactGroupEmailsEmpty: MutableLiveData<Event<String>> = MutableLiveData()
    private val _setupUIData = MutableLiveData<ContactGroupListItem>()
    private val _deleteLabelIds: MutableLiveData<List<String>> = MutableLiveData()

    init {
        initFiltering()
        initGroupsObserving()
        initGroupLabelObserving()
    }

    val contactGroupEmailsResult: LiveData<List<ContactEmail>>
        get() = _contactGroupEmailsResult

    val contactGroupEmailsEmpty: LiveData<Event<String>>
        get() = _contactGroupEmailsEmpty

    val setupUIData: LiveData<ContactGroupListItem>
        get() = _setupUIData

    val deleteGroupStatus: LiveData<Event<Status>>
        get() = _deleteLabelIds.switchMap {
            processDeleteLiveData(it)
        }

    private fun processDeleteLiveData(contactsToDelete: List<String>): LiveData<Event<Status>> {
        return liveData {
            emitSource(
                deleteLabels(contactsToDelete.map { LabelId(it) })
                    .map { isSuccess ->
                        if (isSuccess) {
                            Event(Status.SUCCESS)
                        } else {
                            Event(Status.ERROR)
                        }
                    }
                    .asLiveData()
            )
        }
    }

    fun setData(contactLabel: ContactGroupListItem?) {
        contactLabel?.let { newContact ->
            _contactLabel = newContact
            _contactGroupItemFlow.tryEmit(contactLabel)
            _setupUIData.value = newContact
        }
    }

    fun getData(): ContactGroupListItem = _contactLabel

    private fun initFiltering() {
        val userId = userManager.currentUserId
            ?: return
        filteringChannel
            .debounce(300.toDuration(DurationUnit.MILLISECONDS))
            .distinctUntilChanged()
            .flatMapLatest { contactGroupDetailsRepository.filterContactGroupEmails(userId, _contactLabel.contactId, it) }
            .catch {
                _contactGroupEmailsEmpty.value = Event(it.message ?: ErrorEnum.DEFAULT_ERROR.name)
            }
            .onEach { list ->
                Timber.v("Filtered emails list size: ${list.size}")
                _contactGroupEmailsResult.postValue(list)
            }
            .launchIn(viewModelScope)
    }

    private fun initGroupsObserving() {
        val userId = userManager.currentUserId
            ?: return
        _contactGroupItemFlow
            .filterNotNull()
            .flatMapLatest { contactGroupDetailsRepository.observeContactGroupEmails(userId, it.contactId) }
            .onEach { list ->
                _contactGroupEmailsResult.postValue(list)
            }
            .catch { throwable ->
                _contactGroupEmailsEmpty.value = Event(
                    throwable.message ?: ErrorEnum.INVALID_EMAIL_LIST.name
                )
            }
            .launchIn(viewModelScope)
    }

    private fun initGroupLabelObserving() {
        val userId = userManager.currentUserId
            ?: return
        _contactGroupItemFlow
            .filterNotNull()
            .flatMapLatest { contactGroupListItem ->
                contactGroupDetailsRepository.observeContactGroupDetails(contactGroupListItem.contactId)
                    .filterNotNull()
                    .map { label ->
                        contactsMapper.mapLabelEntityToContactGroup(
                            label,
                            contactRepository.countContactEmailsByLabelId(
                                userId,
                                LabelId(contactGroupListItem.contactId)
                            )
                        )
                    }
            }
            .onEach { groupListItem ->
                _contactLabel = groupListItem
                _setupUIData.value = groupListItem
            }
            .catch { throwable ->
                if (throwable is SQLException) {
                    _contactGroupEmailsEmpty.value = Event(throwable.message ?: ErrorEnum.DEFAULT_ERROR.name)
                }
            }
            .launchIn(viewModelScope)
    }

    fun doFilter(filter: String) {
        viewModelScope.launch {
            filteringChannel.emit(filter.trim())
        }
    }

    fun delete() {
        _deleteLabelIds.value = listOf(_contactLabel.contactId)
    }

    fun moveDraftToTrash(messageId: String) {
        viewModelScope.launch {
            moveMessagesToFolder(
                listOf(messageId),
                Constants.MessageLocationType.TRASH.asLabelIdString(),
                Constants.MessageLocationType.DRAFT.asLabelIdString(),
                userManager.requireCurrentUserId()
            )
        }
    }

    enum class Status(var message: String?) {
        SUCCESS(""),
        ERROR("");
    }
}

/*
 * Copyright (c) 2022 Proton Technologies AG
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
package ch.protonmail.android.contacts.groups.edit

import android.annotation.SuppressLint
import androidx.annotation.ColorInt
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.contacts.ErrorEnum
import ch.protonmail.android.contacts.PostResult
import ch.protonmail.android.contacts.groups.list.ContactGroupListItem
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.events.Status
import ch.protonmail.android.labels.domain.model.Label
import ch.protonmail.android.labels.domain.model.LabelId
import ch.protonmail.android.labels.domain.model.LabelType
import ch.protonmail.android.utils.Event
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.domain.getPrimaryAccount
import me.proton.core.network.domain.ApiResult
import me.proton.core.user.domain.UserManager
import me.proton.core.user.domain.extension.hasSubscriptionForMail
import me.proton.core.util.kotlin.EMPTY_STRING
import timber.log.Timber
import java.util.Locale
import javax.inject.Inject

class ContactGroupEditCreateViewModel @Inject constructor(
    private val userManager: UserManager,
    private val accountManager: AccountManager,
    private val contactGroupEditCreateRepository: ContactGroupEditCreateRepository
) : ViewModel() {

    private lateinit var mode: ContactGroupMode
    private var _changed: Boolean = false
    private var _data = listOf<ContactEmail>()
    private var _toBeDeleted = listOf<ContactEmail>()
    private var _toBeAdded = listOf<ContactEmail>()

    private val _contactGroupSetupLayout: MutableLiveData<Event<ContactGroupMode>> = MutableLiveData()
    private val _contactGroupEmailsResult: MutableLiveData<List<ContactEmail>> = MutableLiveData()
    private val _contactGroupEmailsError: MutableLiveData<Event<String>> = MutableLiveData()
    private val _contactGroupUpdateResult: MutableLiveData<Event<PostResult>> = MutableLiveData()
    private val _cleanUpComplete: MutableLiveData<Event<Boolean>> = MutableLiveData()
    private val _contactGroupItemFlow = MutableStateFlow<ContactGroupListItem?>(null)
    val contactGroupItemFlow = _contactGroupItemFlow.asStateFlow()

    val contactGroupEmailsResult: LiveData<List<ContactEmail>>
        get() = _contactGroupEmailsResult

    val contactGroupSetupLayout: LiveData<Event<ContactGroupMode>>
        get() = _contactGroupSetupLayout

    val contactGroupEmailsError: LiveData<Event<String>>
        get() = _contactGroupEmailsError

    val contactGroupUpdateResult: LiveData<Event<PostResult>>
        get() = _contactGroupUpdateResult

    val cleanUpComplete: LiveData<Event<Boolean>>
        get() = _cleanUpComplete

    init {
        combine(
            _contactGroupItemFlow.filterNotNull(),
            accountManager.getPrimaryUserId().filterNotNull()
        ) { group, userId -> group to userId }
            .flatMapLatest { (group, userId) ->
                contactGroupEditCreateRepository.observeContactGroupEmails(userId, group.contactId)
            }.onEach {
                _data = it
                _contactGroupEmailsResult.postValue(it)
            }
            .catch {
                _contactGroupEmailsError.value = Event(it.message ?: ErrorEnum.INVALID_EMAIL_LIST.name)
            }
            .launchIn(viewModelScope)
    }

    fun setData(contactLabel: ContactGroupListItem?) {
        _contactGroupItemFlow.tryEmit(contactLabel)
        mode = if (contactLabel == null) ContactGroupMode.CREATE else ContactGroupMode.EDIT
        _contactGroupSetupLayout.postValue(Event(mode))
    }

    fun setGroupColor(@ColorInt color: Int) {
        val contactGroupItem = _contactGroupItemFlow.value
        if (contactGroupItem == null) {
            _contactGroupItemFlow.tryEmit(ContactGroupListItem("", "", 0, color))
        } else {
            _contactGroupItemFlow.tryEmit(contactGroupItem.copy(color = color))
        }
    }

    fun save(name: String) {
        viewModelScope.launch {
            val userId = requireNotNull(accountManager.getPrimaryAccount().first()?.userId)
            val isPaidUser = userManager.getUser(userId).hasSubscriptionForMail()
            if (!isPaidUser) {
                _contactGroupUpdateResult.postValue(Event(PostResult(status = Status.UNAUTHORIZED)))
                return@launch
            }
            val contactGroupItem = _contactGroupItemFlow.value
            Timber.v("Save contact mode: $mode")
            when (mode) {
                ContactGroupMode.CREATE ->
                    createContactGroup(
                        name,
                        _toBeAdded.map {
                            it.contactEmailId
                        },
                        contactGroupItem
                    )
                ContactGroupMode.EDIT ->
                    editContactGroup(
                        name,
                        _toBeAdded.map {
                            it.contactEmailId
                        },
                        _toBeDeleted.map {
                            it.contactEmailId
                        },
                        contactGroupItem
                    )
            }
        }
    }

    @SuppressLint("CheckResult")
    private suspend fun editContactGroup(
        name: String,
        toBeAdded: List<String>,
        toBeDeleted: List<String>,
        contactGroupItem: ContactGroupListItem?
    ) {
        val userId = requireNotNull(accountManager.getPrimaryUserId().first())
        val contactLabel = Label(
            id = LabelId(requireNotNull(contactGroupItem?.contactId)),
            name = name,
            color = String.format(Locale.getDefault(), "#%06X", 0xFFFFFF and (contactGroupItem?.color ?: 0)),
            order = 0,
            type = LabelType.CONTACT_GROUP,
            path = "",
            parentId = ""
        )
        when (
            val editContactResult = contactGroupEditCreateRepository.editContactGroup(contactLabel, userId)
        ) {
            is ApiResult.Success -> {
                contactGroupEditCreateRepository.setMembersForContactGroup(
                    userId = userId,
                    contactGroupLabelId = contactLabel.id.id,
                    contactGroupName = name,
                    membersList = toBeAdded
                )
                contactGroupEditCreateRepository.removeMembersFromContactGroup(
                    userId = userId,
                    contactGroupLabelId = contactLabel.id.id,
                    contactGroupName = name,
                    membersList = toBeDeleted
                )
                _contactGroupUpdateResult.postValue(Event(PostResult(status = Status.SUCCESS)))
            }
            is ApiResult.Error.Http -> {
                _contactGroupUpdateResult.postValue(
                    Event(PostResult(editContactResult.proton?.error, Status.FAILED))
                )
            }
            else -> {
                Timber.d("editContactGroup failure $editContactResult")
            }
        }
    }

    @SuppressLint("CheckResult")
    private suspend fun createContactGroup(
        name: String,
        membersList: List<String>,
        contactGroupItem: ContactGroupListItem?
    ) {

        val userId = requireNotNull(accountManager.getPrimaryUserId().first())
        val contactLabel = Label(
            id = LabelId(EMPTY_STRING),
            name = name,
            color = String.format(Locale.getDefault(), "#%06X", 0xFFFFFF and (contactGroupItem?.color ?: 0)),
            order = 0,
            type = LabelType.CONTACT_GROUP,
            path = "",
            parentId = ""
        )
        if (!validate(contactLabel)) {
            _contactGroupUpdateResult.postValue(Event(PostResult(status = Status.VALIDATION_FAILED)))
            return
        }

        val createGroupResponse = contactGroupEditCreateRepository.createContactGroup(contactLabel, userId)
        when (createGroupResponse) {
            is ApiResult.Success -> {
                contactGroupEditCreateRepository.setMembersForContactGroup(
                    userId = userId,
                    contactGroupLabelId = createGroupResponse.value.label.id,
                    contactGroupName = name,
                    membersList = membersList
                )
                _contactGroupUpdateResult.postValue(Event(PostResult(status = Status.SUCCESS)))
            }
            is ApiResult.Error.Http -> {
                _contactGroupUpdateResult.postValue(
                    Event(PostResult(createGroupResponse.proton?.error, Status.FAILED))
                )
            }
            else -> {
                Timber.d("createGroup failure $createGroupResponse")
            }
        }
    }

    private fun validate(contactLabel: Label): Boolean {
        if (contactLabel.name.isEmpty()) {
            return false
        }
        return true
    }

    fun setChanged() {
        _changed = true
    }

    /**
     * Should inform the view that the user want's to leave the screen, so that the view is able to save
     * state or do whatever needed for cleanup.
     */
    fun onBackPressed() {
        _cleanUpComplete.postValue(Event(!_changed))
    }

    fun calculateDiffMembers(newMembers: List<ContactEmail>) {
        _toBeDeleted = _data.minus(newMembers) // to be deleted
        _toBeAdded = newMembers.minus(_data) // to be added
        _contactGroupEmailsResult.postValue(newMembers)
    }
}

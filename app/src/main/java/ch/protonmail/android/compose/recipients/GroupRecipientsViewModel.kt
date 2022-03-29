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
package ch.protonmail.android.compose.recipients

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.compose.ComposeMessageRepository
import ch.protonmail.android.contacts.ErrorEnum
import ch.protonmail.android.contacts.ErrorResponse
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.Label
import ch.protonmail.android.utils.Event
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class GroupRecipientsViewModel @Inject constructor(
    private val composeMessageRepository: ComposeMessageRepository,
    private val accountManager: AccountManager,
    private val labelRepository: LabelRepository
) : ViewModel() {

    private lateinit var _recipients: ArrayList<MessageRecipient>
    private var _location: Constants.RecipientLocationType = Constants.RecipientLocationType.TO
    private lateinit var _group: String
    private lateinit var _groupDetails: Label
    private lateinit var _groupAllEmails: List<ContactEmail>

    private val _contactGroupResult: MutableLiveData<List<ContactEmail>> = MutableLiveData()
    private val _contactGroupError: MutableLiveData<Event<ErrorResponse>> = MutableLiveData()

    val contactGroupResult: LiveData<List<ContactEmail>>
        get() = _contactGroupResult

    val contactGroupError: LiveData<Event<ErrorResponse>>
        get() = _contactGroupError

    fun setData(recipients: ArrayList<MessageRecipient>, location: Constants.RecipientLocationType) {
        _recipients = recipients
        _group = _recipients[0].group
        _location = location

        viewModelScope.launch {
            val userId = accountManager.getPrimaryUserId().filterNotNull().first()
            getContactGroupFromDB(userId)
        }
    }

    fun getLocation(): Constants.RecipientLocationType = _location

    fun getGroup(): String = _group

    private suspend fun getContactGroupFromDB(userId: UserId) {
        val groupName = _recipients[0].group
        val contactGroup = labelRepository.findLabelByName(groupName, userId)
        if (contactGroup != null) {
            _groupDetails = contactGroup
            val emails = composeMessageRepository.getContactGroupEmailsSync(userId, contactGroup.id.id)
            _groupAllEmails = emails
            _groupAllEmails.forEach { email ->
                email.selected = _recipients.find { selected ->
                    selected.emailAddress == email.email && selected.name == email.name
                } != null
                email.isPGP = _recipients.find { selected -> selected.emailAddress == email.email }?.isPGP ?: false
                email.pgpIcon = _recipients.find { selected -> selected.emailAddress == email.email }?.icon ?: 0
                email.pgpIconColor = _recipients.find { selected ->
                    selected.emailAddress == email.email
                }?.iconColor ?: 0
                email.pgpDescription =
                    _recipients.find { selected -> selected.emailAddress == email.email }?.description ?: 0
            }
            _contactGroupResult.postValue(emails)


        } else {
            val errorMessage = "Cannot find contact group $groupName for user: $userId"
            Timber.i(errorMessage)
            _contactGroupError.postValue(Event(ErrorResponse(errorMessage, ErrorEnum.DEFAULT_ERROR)))
        }

    }

    fun getData(): ArrayList<MessageRecipient> = _recipients

    fun getTitle(): String = _recipients[0].group

    fun getGroupColor(): Int = _recipients[0].groupColor

    fun getGroupIcon(): Int = _recipients[0].groupIcon
}

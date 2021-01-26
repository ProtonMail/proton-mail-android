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
package ch.protonmail.android.compose.recipients

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.api.models.room.contacts.ContactLabel
import ch.protonmail.android.api.rx.ThreadSchedulers
import ch.protonmail.android.compose.ComposeMessageRepository
import ch.protonmail.android.contacts.ErrorEnum
import ch.protonmail.android.contacts.ErrorResponse
import ch.protonmail.android.core.Constants
import ch.protonmail.android.utils.Event
import javax.inject.Inject

/**
 * Created by kadrikj on 9/18/18. */
class GroupRecipientsViewModel @Inject constructor(
    private val composeMessageRepository: ComposeMessageRepository) : ViewModel() {

    private lateinit var _recipients: ArrayList<MessageRecipient>
    private var _location: Constants.RecipientLocationType = Constants.RecipientLocationType.TO
    private lateinit var _group: String
    private lateinit var _groupDetails: ContactLabel
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
        getContactGroupFromDB()
    }

    fun getLocation(): Constants.RecipientLocationType = _location

    fun getGroup(): String = _group

    @SuppressLint("CheckResult")
    private fun getContactGroupFromDB() {
        val groupName = _recipients[0].group
        composeMessageRepository.getContactGroupFromDB(groupName).subscribeOn(ThreadSchedulers.io())
            .observeOn(ThreadSchedulers.main())
            .flatMapObservable {
                _groupDetails = it
                composeMessageRepository.getContactGroupEmails(it.ID)
            }
            .subscribe({ it ->
                           _groupAllEmails = it
                           _groupAllEmails.forEach {
                               it.selected = _recipients.find { selected -> selected.emailAddress == it.email && selected.name == it.name } != null
                               it.isPGP = _recipients.find { selected -> selected.emailAddress == it.email }?.isPGP ?: false
                               it.pgpIcon = _recipients.find { selected -> selected.emailAddress == it.email }?.icon ?: 0
                               it.pgpIconColor = _recipients.find { selected -> selected.emailAddress == it.email }?.iconColor ?: 0
                               it.pgpDescription = _recipients.find { selected -> selected.emailAddress == it.email }?.description ?: 0
                           }
                           _contactGroupResult.postValue(it)
                       }, {
                           _contactGroupError.postValue(Event(ErrorResponse(it.message ?: "", ErrorEnum.DEFAULT_ERROR)))

            })

    }

    fun getData(): ArrayList<MessageRecipient> {
        return _recipients
    }

    fun getTitle(): String = _recipients[0].group

    fun getGroupColor(): Int = _recipients[0].groupColor

    fun getGroupIcon(): Int = _recipients[0].groupIcon

}
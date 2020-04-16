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
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.api.models.room.contacts.ContactLabel
import ch.protonmail.android.api.rx.ThreadSchedulers
import ch.protonmail.android.contacts.ErrorEnum
import ch.protonmail.android.contacts.groups.ContactGroupsBaseViewModel
import ch.protonmail.android.utils.Event
import com.jakewharton.rxrelay2.PublishRelay
import retrofit2.HttpException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Created by kadrikj on 8/23/18. */
class ContactGroupDetailsViewModel @Inject constructor(private val contactGroupDetailsRepository: ContactGroupDetailsRepository) : ContactGroupsBaseViewModel() {

    private lateinit var _contactLabel: ContactLabel
    private lateinit var _data: List<ContactEmail>
    private val _contactGroupEmailsResult: MutableLiveData<List<ContactEmail>> = MutableLiveData()
    private val _filteringPublishSubject = PublishRelay.create<String>()
    private val _contactGroupEmailsEmpty: MutableLiveData<Event<String>> = MutableLiveData()
    private val _setupUIData = MutableLiveData<ContactLabel>()
    private val _deleteGroupStatus: MutableLiveData<Event<Status>> = MutableLiveData()

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
        get() = _deleteGroupStatus

    fun setData(contactLabel: ContactLabel?) {
        contactLabel?.let {
            this._contactLabel = contactLabel
            getContactGroupEmails(contactLabel)
            watchForContactGroup()
            _setupUIData.postValue(contactLabel)
        }
    }

    fun getData(): ContactLabel? = _contactLabel

    @SuppressLint("CheckResult")
    private fun watchForContactGroup() {
        contactGroupDetailsRepository.findContactGroupDetails(_contactLabel.ID)
                .subscribeOn(ThreadSchedulers.io())
                .observeOn(ThreadSchedulers.main())
                .subscribe({
                    _contactLabel = it
                    if (::_data.isInitialized) {
                        _contactGroupEmailsResult.postValue(_data)
                    }
                }, {
                    _contactGroupEmailsEmpty.value = Event(it.message ?: ErrorEnum.DEFAULT_ERROR.name)

                })
    }

    @SuppressLint("CheckResult")
    private fun getContactGroupEmails(contactLabel: ContactLabel) {
        contactGroupDetailsRepository.getContactGroupEmails(contactLabel.ID)
                .subscribeOn(ThreadSchedulers.io())
                .observeOn(ThreadSchedulers.main()).subscribe(
                        {
                            _data = it
                            watchForContactGroup()
                            _contactGroupEmailsResult.postValue(it)
                        },
                        {
                            _contactGroupEmailsEmpty.value = Event(it.message ?: ErrorEnum.INVALID_EMAIL_LIST.name)
                        }
                )
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

    @SuppressLint("CheckResult")
    fun delete() {
        contactGroupDetailsRepository.delete(_contactLabel)
            .subscribeOn(ThreadSchedulers.io())
            .observeOn(ThreadSchedulers.main())
            .subscribe({
                           val status = Status.SUCCESS
                           _deleteGroupStatus.postValue(Event(status))
                       }, {
                           val status = Status.ERROR
                           status.msg(
                               if (it is HttpException) {
                                   parseErrorApiResponse(it)
                               } else
                                   it.message
                           )
                           _deleteGroupStatus.postValue(Event(status))
                       })
    }

    enum class Status(var message: String?) {
        SUCCESS("") {
            override fun msg(msg: String?) {
                message = msg
            }
        },
        ERROR("") {
            override fun msg(msg: String?) {
                message = msg
            }
        };

        abstract fun msg(msg: String?)
    }
}
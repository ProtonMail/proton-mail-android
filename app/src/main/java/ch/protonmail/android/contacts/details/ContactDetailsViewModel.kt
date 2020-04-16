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
package ch.protonmail.android.contacts.details

import android.annotation.SuppressLint
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Patterns
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.api.models.room.contacts.ContactLabel
import ch.protonmail.android.api.rx.ThreadSchedulers
import ch.protonmail.android.contacts.ErrorEnum
import ch.protonmail.android.contacts.ErrorResponse
import ch.protonmail.android.contacts.PostResult
import ch.protonmail.android.events.Status
import ch.protonmail.android.utils.Event
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.functions.BiFunction
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject

open class ContactDetailsViewModel @Inject constructor(private val contactDetailsRepository: ContactDetailsRepository) :
    ViewModel() {

    //region data
    protected lateinit var allContactGroups: List<ContactLabel>
    protected lateinit var allContactEmails: List<ContactEmail>

    private val _mapEmailGroups: HashMap<String, List<ContactLabel>> = HashMap()
    //endregion
    private var _setupCompleteValue: Boolean = false
    private val _setupComplete: MutableLiveData<Event<Boolean>> = MutableLiveData()
    private val _setupError: MutableLiveData<Event<ErrorResponse>> = MutableLiveData()
    //region email groups result and error below
    private var _emailGroupsResult: MutableLiveData<ContactEmailsGroups> = MutableLiveData()
    private val _emailGroupsError: MutableLiveData<Event<ErrorResponse>> = MutableLiveData()
    //endregion
    //region contact groups result and error
    private val _contactEmailGroupsResult: MutableLiveData<Event<PostResult>> = MutableLiveData()
    //endregion
    //region merged result and errors
    private val _mergedContactEmailGroupsResult: MutableLiveData<List<ContactLabel>> =
        MutableLiveData()
    private val _mergedContactEmailGroupsError: MutableLiveData<Event<ErrorResponse>> =
        MutableLiveData()
    private val _photoFromUrl: MutableLiveData<Bitmap> = MutableLiveData()
    //endregion

    val setupComplete: LiveData<Event<Boolean>>
        get() = _setupComplete

    val setupError: LiveData<Event<ErrorResponse>>
        get() = _setupError

    val mergedContactEmailGroupsResult: LiveData<List<ContactLabel>>
        get() = _mergedContactEmailGroupsResult

    val mergedContactEmailGroupsError: LiveData<Event<ErrorResponse>>
        get() = _mergedContactEmailGroupsError

    val contactEmailsGroups: LiveData<ContactEmailsGroups>
        get() = _emailGroupsResult

    val contactEmailsError: LiveData<Event<ErrorResponse>>
        get() = _emailGroupsError

    val photoFromUrl: LiveData<Bitmap>
        get() = _photoFromUrl


    private val bgDispatcher: CoroutineDispatcher = Dispatchers.IO


    @SuppressLint("CheckResult")
    fun mergeContactEmailGroups(email: String) {
        Observable.just(allContactEmails)
            .flatMap { emailList ->
                val contactEmail =
                    emailList.find { contactEmail -> contactEmail.email == email }!!
                val list1 = allContactGroups
                val list2 = _mapEmailGroups[contactEmail.contactEmailId!!]
                list2?.let { _ ->
                    list1.forEach {
                        val selectedState =
                            list2.find { selected -> selected.ID == it.ID } != null
                        it.isSelected = if (selectedState) {
                            ContactEmailGroupSelectionState.SELECTED
                        } else {
                            ContactEmailGroupSelectionState.DEFAULT
                        }
                    }
                }
                Observable.just(list1)
            }
            .subscribe({
                _mergedContactEmailGroupsResult.postValue(it)
            }, {
                _mergedContactEmailGroupsError.postValue(
                    Event(
                        ErrorResponse(
                            it.message ?: "",
                            ErrorEnum.INVALID_GROUP_LIST
                        )
                    )
                )
            })
    }

    fun fetchContactEmailGroups(rowID: Int, email: String) {
        val emailId = allContactEmails.find {
            it.email == email
        }?.contactEmailId

        emailId?.let { _ ->
            contactDetailsRepository.getContactGroups(emailId)
                .subscribeOn(ThreadSchedulers.io())
                .observeOn(ThreadSchedulers.main())
                .subscribe({
                    _mapEmailGroups[emailId] = it
                    _emailGroupsResult.value = ContactEmailsGroups(it, emailId, rowID)
                }, {
                    _emailGroupsError.postValue(
                        Event(
                            ErrorResponse(
                                it.message ?: "",
                                ErrorEnum.SERVER_ERROR
                            )
                        )
                    )
                })
        }
    }

    @SuppressLint("CheckResult")
    fun fetchContactGroupsAndContactEmails(contactId: String) {
        Observable.zip(contactDetailsRepository.getContactGroups().subscribeOn(ThreadSchedulers.io())
            .doOnError {
                if (allContactGroups.isEmpty()) {
                    _setupError.postValue(
                        Event(
                            ErrorResponse(
                                "",
                                ErrorEnum.INVALID_GROUP_LIST
                            )
                        )
                    )
                } else {
                    _setupError.postValue(
                        Event(
                            ErrorResponse(
                                it.message ?: "",
                                ErrorEnum.SERVER_ERROR
                            )
                        )
                    )
                }
            },
            contactDetailsRepository.getContactEmails(contactId).subscribeOn(ThreadSchedulers.io())
                .doOnError {
                    if (allContactEmails.isEmpty()) {
                        _setupError.postValue(
                            Event(
                                ErrorResponse(
                                    "",
                                    ErrorEnum.INVALID_EMAIL_LIST
                                )
                            )
                        )
                    } else {
                        _setupError.postValue(
                            Event(
                                ErrorResponse(
                                    it.message ?: "",
                                    ErrorEnum.SERVER_ERROR
                                )
                            )
                        )
                    }
                },
            BiFunction { groups: List<ContactLabel>, emails: List<ContactEmail> ->
                allContactGroups = groups
                allContactEmails = emails
            }
        ).observeOn(ThreadSchedulers.main())
            .subscribe({
                if (!_setupCompleteValue) {
                    _setupCompleteValue = true
                    _setupComplete.value = Event(true)
                }
            }, {
                _setupError.postValue(
                    Event(
                        ErrorResponse(
                            it.message ?: "",
                            ErrorEnum.SERVER_ERROR
                        )
                    )
                )
            })
    }

    @SuppressLint("CheckResult")
    fun updateContactEmailGroup(contactLabel: ContactLabel, email: String) {
        val membersList: HashSet<String> = HashSet()
        Observable.just(allContactEmails)
            .flatMapCompletable {
                val contactEmail = it.find { contactEmail -> contactEmail.email == email }!!
                membersList.add(contactEmail.contactEmailId!!)
                Completable.complete().andThen(
                    contactDetailsRepository.editContactGroup(contactLabel).andThen(
                        when (contactLabel.isSelected) {
                            ContactEmailGroupSelectionState.SELECTED -> contactDetailsRepository.setMembersForContactGroup(
                                contactLabel.ID,
                                contactLabel.name,
                                membersList.toList()
                            )
                            ContactEmailGroupSelectionState.UNSELECTED -> contactDetailsRepository.removeMembersForContactGroup(
                                contactLabel.ID,
                                contactLabel.name,
                                membersList.toList()
                            )
                            ContactEmailGroupSelectionState.DEFAULT -> TODO()
                        }
                    )
                )

            }
            .subscribeOn(ThreadSchedulers.io())
            .observeOn(ThreadSchedulers.main())
            .subscribe({
                _contactEmailGroupsResult.postValue(Event(PostResult(status = Status.SUCCESS)))
            }, {
                _contactEmailGroupsResult.postValue(Event(PostResult(it.message, Status.FAILED)))
            })
    }

    fun getBitmapFromURL(src: String) {
        if (!Patterns.WEB_URL.matcher(src).matches()) {
            return
        }
        GlobalScope.launch(bgDispatcher) {
            val url = URL(src)
            val connection = url.openConnection() as HttpURLConnection
            val input = connection.inputStream
            _photoFromUrl.postValue(BitmapFactory.decodeStream(input))
        }
    }
}
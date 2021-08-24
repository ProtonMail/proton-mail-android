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
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.util.PatternsCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.api.rx.ThreadSchedulers
import ch.protonmail.android.contacts.ErrorEnum
import ch.protonmail.android.contacts.ErrorResponse
import ch.protonmail.android.contacts.details.ContactEmailGroupSelectionState.SELECTED
import ch.protonmail.android.contacts.details.data.ContactDetailsRepository
import ch.protonmail.android.contacts.details.presentation.model.ContactLabelUiModel
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.domain.usecase.DownloadFile
import ch.protonmail.android.exceptions.BadImageUrlException
import ch.protonmail.android.exceptions.ImageNotFoundException
import ch.protonmail.android.labels.data.db.LabelEntity
import ch.protonmail.android.utils.Event
import ch.protonmail.android.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import me.proton.core.util.kotlin.DispatcherProvider
import studio.forface.viewstatestore.ViewStateStore
import java.io.FileNotFoundException
import javax.inject.Inject

/**
 * A [ViewModel] for display a contact
 * It is open so it can be extended by EditContactViewModel
 *
 * Inherit from [BaseViewModel]
 *
 * TODO:
 *   [ ] Replace RxJava with Coroutines
 *   [ ] Fix unhandled concurrency
 *   [ ] Replace [LiveData] with [ViewStateStore] for avoid multiple [LiveData] for success and error and
 *      [ViewStateStore.lock] for avoid useless private [MutableLiveData]
 *   [ x] Inject dispatchers in the constructor
 *   [ ] Replace [ContactDetailsRepository] with a `ContactsRepository`
 */
@Deprecated("Use new ContactDetailsViewModel")
@HiltViewModel
open class ContactDetailsViewModelOld @Inject constructor(
    dispatchers: DispatcherProvider,
    private val downloadFile: DownloadFile,
    private val contactDetailsRepository: ContactDetailsRepository,
    private val userManager: UserManager
) : BaseViewModel(dispatchers) {

    protected lateinit var allContactGroups: List<ContactLabelUiModel>
    protected lateinit var allContactEmails: List<ContactEmail>

    private var _setupCompleteValue: Boolean = false
    private val _setupComplete: MutableLiveData<Event<Boolean>> = MutableLiveData()
    private val _setupError: MutableLiveData<Event<ErrorResponse>> = MutableLiveData()

    private var _emailGroupsResult: MutableLiveData<ContactEmailsGroups> = MutableLiveData()
    private val _emailGroupsError: MutableLiveData<Event<ErrorResponse>> = MutableLiveData()
    private val _mapEmailGroups: HashMap<String, List<LabelEntity>> = HashMap()

    private val _mergedContactEmailGroupsResult: MutableLiveData<List<ContactLabelUiModel>> = MutableLiveData()
    private val _mergedContactEmailGroupsError: MutableLiveData<Event<ErrorResponse>> = MutableLiveData()

    val setupComplete: LiveData<Event<Boolean>>
        get() = _setupComplete

    val profilePicture = ViewStateStore<Bitmap>().lock

    @SuppressLint("CheckResult")
    fun mergeContactEmailGroups(email: String) {
        Observable.just(allContactEmails)
            .flatMap { emailList ->
                val contactEmail =
                    emailList.find { contactEmail -> contactEmail.email == email }!!
                val list1 = allContactGroups
                val list2 = _mapEmailGroups[contactEmail.contactEmailId]
                list2?.let { _ ->
                    list1.forEach { contactLabel ->
                        val selectedState =
                            list2.find { selected -> selected.id == contactLabel.id } != null
                        contactLabel.copy(
                            isSelected = if (selectedState) {
                                SELECTED
                            } else {
                                ContactEmailGroupSelectionState.DEFAULT
                            }
                        )
                    }
                }
                Observable.just(list1)
            }
            .subscribe(
                {
                    _mergedContactEmailGroupsResult.postValue(it)
                },
                {
                    _mergedContactEmailGroupsError.postValue(
                        Event(
                            ErrorResponse(
                                it.message ?: "",
                                ErrorEnum.INVALID_GROUP_LIST
                            )
                        )
                    )
                }
            )
    }

    fun fetchContactEmailGroups(rowID: Int, email: String) {
        val emailId = allContactEmails.find {
            it.email == email
        }?.contactEmailId

        emailId?.let { _ ->
            contactDetailsRepository.getContactGroups(emailId)
                .subscribeOn(ThreadSchedulers.io())
                .observeOn(ThreadSchedulers.main())
                .subscribe(
                    {
                        _mapEmailGroups[emailId] = it
                        _emailGroupsResult.value = ContactEmailsGroups(it, emailId, rowID)
                    },
                    {
                        _emailGroupsError.postValue(
                            Event(
                                ErrorResponse(
                                    it.message ?: "",
                                    ErrorEnum.SERVER_ERROR
                                )
                            )
                        )
                    }
                )
        }
    }

    fun fetchContactGroupsAndContactEmails(contactId: String) {
        val userId = userManager.requireCurrentUserId()
        Observable.zip(
            contactDetailsRepository.getContactGroups(userId).subscribeOn(ThreadSchedulers.io())
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
            { groups: List<LabelEntity>,
              emails: List<ContactEmail> ->
                allContactGroups = groups.map { entity ->
                    ContactLabelUiModel(
                        id = entity.id,
                        name = entity.name,
                        color = entity.color,
                        type = entity.type,
                        path = entity.path,
                        parentId = entity.parentId,
                        expanded = entity.expanded,
                        sticky = entity.sticky,
                        contactEmailsCount = contactDetailsRepository.getContactEmailsCount(entity.id)
                    )
                }
                allContactEmails = emails
            }
        ).observeOn(ThreadSchedulers.main())
            .subscribe(
                {
                    if (!_setupCompleteValue) {
                        _setupCompleteValue = true
                        _setupComplete.value = Event(true)
                    }
                },
                {
                    _setupError.postValue(
                        Event(
                            ErrorResponse(
                                it.message ?: "",
                                ErrorEnum.SERVER_ERROR
                            )
                        )
                    )
                }
            )
    }

    fun getBitmapFromURL(src: String) {
        viewModelScope.launch(Comp) {

            runCatching {
                if (!PatternsCompat.WEB_URL.matcher(src).matches()) throw BadImageUrlException(src)
                BitmapFactory.decodeStream(downloadFile(src))
            }.fold(
                onSuccess = { profilePicture.post(it) },

                onFailure = { throwable ->

                    if (throwable is FileNotFoundException || throwable is TimeoutCancellationException) {
                        profilePicture.postError(ImageNotFoundException(throwable, src))
                    } else {
                        profilePicture.postError(throwable)
                    }
                }
            )
        }
    }

}

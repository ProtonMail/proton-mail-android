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
package ch.protonmail.android.contacts.details

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
import ch.protonmail.android.contacts.details.data.ContactDetailsRepository
import ch.protonmail.android.contacts.details.presentation.model.ContactLabelUiModel
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.domain.usecase.DownloadFile
import ch.protonmail.android.exceptions.BadImageUrlException
import ch.protonmail.android.exceptions.ImageNotFoundException
import ch.protonmail.android.labels.domain.model.Label
import ch.protonmail.android.utils.Event
import ch.protonmail.android.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.Observable
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
    private val _setupError: MutableLiveData<Event<ErrorResponse>> = MutableLiveData()

    val profilePicture = ViewStateStore<Bitmap>().lock

    fun fetchContactGroupsAndContactEmails(contactId: String) {
        val userId = userManager.requireCurrentUserId()
        Observable.zip(
            Observable.fromCallable {
                runBlocking {
                    contactDetailsRepository.getContactGroups(userId)
                }
            }
                .subscribeOn(ThreadSchedulers.io())
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
            {
                groups: List<Label>,
                emails: List<ContactEmail> ->
                allContactGroups = groups.map { entity ->
                    ContactLabelUiModel(
                        id = entity.id,
                        name = entity.name,
                        color = entity.color,
                        type = entity.type,
                        path = entity.path,
                        parentId = entity.parentId,
                        contactEmailsCount = runBlocking {
                            contactDetailsRepository.getContactEmailsCount(userId, entity.id)
                        }
                    )
                }
                allContactEmails = emails
            }
        ).observeOn(ThreadSchedulers.main())
            .subscribe(
                {
                    if (!_setupCompleteValue) {
                        _setupCompleteValue = true
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

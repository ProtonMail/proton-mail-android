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
package ch.protonmail.android.contacts.groups.edit.chooser

import android.annotation.SuppressLint
import android.text.TextUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import ch.protonmail.android.api.rx.ThreadSchedulers
import ch.protonmail.android.contacts.ErrorEnum
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.utils.Event
import com.jakewharton.rxrelay2.PublishRelay
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class AddressChooserViewModel @Inject constructor(
    private val addressChooserRepository: AddressChooserRepository
) : ViewModel() {

    private var _data: List<ContactEmail> = ArrayList()
    private var _selected: HashSet<ContactEmail> = HashSet()

    private val _contactGroupEmailsResult: MutableLiveData<List<ContactEmail>> = MutableLiveData()
    private val _contactGroupEmailsEmpty: MutableLiveData<Event<String>> = MutableLiveData()
    private val _filteringPublishSubject = PublishRelay.create<String>()

    val contactGroupEmailsResult: LiveData<List<ContactEmail>>
        get() = _contactGroupEmailsResult

    val contactGroupEmailsEmpty: LiveData<Event<String>>
        get() = _contactGroupEmailsEmpty

    init {
        initFiltering()
    }

    @SuppressLint("CheckResult")
    private fun initFiltering() {
        _filteringPublishSubject
            .debounce(300, TimeUnit.MILLISECONDS)
            .distinctUntilChanged()
            .switchMap {
                addressChooserRepository.filterContactGroupEmails(it)
            }
            .subscribeOn(ThreadSchedulers.io())
            .observeOn(ThreadSchedulers.main())
            .subscribe(
                { list ->
                    list.forEach { contactEmail ->
                        contactEmail.selected =
                            _selected.find { selected ->
                            selected.contactEmailId == contactEmail.contactEmailId
                        } != null
                    }
                    _contactGroupEmailsResult.postValue(list)
                },
                {
                    _contactGroupEmailsEmpty.value = Event(it.message ?: ErrorEnum.INVALID_EMAIL_LIST.name)
                }
            )
    }

    @SuppressLint("CheckResult")
    fun getAllEmails(selected: HashSet<ContactEmail>) {
        _selected = selected
        addressChooserRepository.getContactGroupEmails()
            .subscribeOn(ThreadSchedulers.io())
            .observeOn(ThreadSchedulers.main()).subscribe(
                { list ->
                    list.forEach { contactEmail ->
                        contactEmail.selected =
                            _selected.find { selected ->
                            selected.contactEmailId == contactEmail.contactEmailId
                        } != null
                    }
                    _data = list
                    _contactGroupEmailsResult.postValue(list)
                },
                {
                    _contactGroupEmailsEmpty.value = Event(it.message ?: ErrorEnum.INVALID_EMAIL_LIST.name)
                }
            )
    }

    fun doFilter(filter: String, selected: List<ContactEmail>) {
        if (TextUtils.isEmpty(filter)) {
            _data.forEach { contactEmail ->
                contactEmail.selected =
                    _selected.find { selected -> selected.contactEmailId == contactEmail.contactEmailId } != null
            }
            _contactGroupEmailsResult.postValue(_data)
        } else {
            _selected.addAll(selected)
            _filteringPublishSubject.accept(filter.trim())
        }
    }

    fun updateSelected(unselected: List<ContactEmail>, selected: List<ContactEmail>): ArrayList<ContactEmail> {
        _selected.removeAll(unselected)
        _selected.addAll(selected)
        return ArrayList(_selected)
    }
}

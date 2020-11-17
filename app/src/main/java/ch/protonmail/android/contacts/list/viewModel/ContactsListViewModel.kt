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
package ch.protonmail.android.contacts.list.viewModel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.work.Operation
import androidx.work.WorkManager
import ch.protonmail.android.api.models.room.contacts.ContactsDatabase
import ch.protonmail.android.contacts.list.listView.ContactItem
import ch.protonmail.android.contacts.list.listView.ContactsLiveData
import ch.protonmail.android.contacts.list.listView.ProtonMailContactsLiveData
import ch.protonmail.android.contacts.list.progress.ProgressLiveData
import ch.protonmail.android.contacts.list.search.ISearchListenerViewModel
import ch.protonmail.android.contacts.repositories.andorid.baseInfo.IAndroidContactsRepository
import ch.protonmail.android.contacts.repositories.andorid.details.AndroidContactDetailsRepository
import ch.protonmail.android.worker.DeleteContactWorker
import javax.inject.Inject

class ContactsListViewModel @Inject constructor(
    contactsDatabase: ContactsDatabase,
    private val workManager: WorkManager,
    private val androidContactsRepository: IAndroidContactsRepository<ContactItem>,
    private val androidContactsDetailsRepository: AndroidContactDetailsRepository
) : ViewModel(), IContactsListViewModel, ISearchListenerViewModel {

    private val progressMax = MutableLiveData<Int?>()
    private val progress = MutableLiveData<Int?>()

    private val searchPhrase = MutableLiveData<String?>()
    private val protonmailContactsData = contactsDatabase.findAllContactDataAsync()
    private val protonmailContactsEmails = contactsDatabase.findAllContactsEmailsAsync()
    private val protonmailContacts = ProtonMailContactsLiveData(protonmailContactsData, protonmailContactsEmails)

    override val androidContacts = androidContactsRepository.androidContacts
    override val contactItems = ContactsLiveData(searchPhrase, protonmailContacts, androidContacts)
    override val uploadProgress = ProgressLiveData(progress, progressMax)
    override val contactToConvert = androidContactsDetailsRepository.contactDetails

    var hasPermission: Boolean = false
        private set

    override fun startConvertDetails(contactId: String) =
        androidContactsDetailsRepository.makeQuery(contactId)

    override fun setHasContactsPermission(hasPermission: Boolean) {
        this.hasPermission = hasPermission
        androidContactsRepository.setContactsPermission(hasPermission)
    }

    override fun setSearchPhrase(searchPhrase: String?) {
        this.searchPhrase.value = searchPhrase
        androidContactsRepository.setSearchPhrase(searchPhrase ?: "")
    }

    override fun setProgressMax(max: Int?) {
        progressMax.value = max
    }

    override fun setProgress(progress: Int?) {
        this.progress.value = progress
    }

    fun deleteSelected(contacts: List<String>): LiveData<Operation.State> =
        DeleteContactWorker.Enqueuer(workManager).enqueue(contacts).state
}

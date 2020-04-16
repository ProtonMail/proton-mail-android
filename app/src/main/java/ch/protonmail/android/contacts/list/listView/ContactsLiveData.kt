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
package ch.protonmail.android.contacts.list.listView

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import java.util.*

class ContactsLiveData(searchPhraseLiveData:LiveData<String?>,
								protonmailContactsLiveData:LiveData<List<ContactItem>>,
								androidContactsLiveData:LiveData<List<ContactItem>>):MediatorLiveData<List<ContactItem>>() {

	private var searchPhrase: String? = null
	private var protonmailContacts: List<ContactItem>? = null
	private var androidContacts: List<ContactItem>? = null
	init {
		addSource(searchPhraseLiveData) {
			searchPhrase = it
			emit()
		}
		addSource(protonmailContactsLiveData) {
			protonmailContacts = it
			emit()
		}
		addSource(androidContactsLiveData) {
			androidContacts = it
			emit()
		}
	}
	private fun emit() {
		val searchPhrase = searchPhrase ?: ""
		val protonmailContacts = protonmailContacts ?: emptyList()
		val androidContacts = androidContacts ?: emptyList()
		val filteredProtonMailEmails = protonmailContacts.filter {
			searchPhrase.isEmpty() || it.getName().contains(searchPhrase, ignoreCase = true) || it.getEmail().contains(searchPhrase, ignoreCase = true)
		}
		val protonMailEmails = protonmailContacts.asSequence().map { it.getEmail().toLowerCase() }.toSet()

		val filteredAndroidContacts = androidContacts.filter {
			!protonMailEmails.contains(it.getEmail().toLowerCase())
		}

		val mergedContacts = ArrayList<ContactItem>()
		if (filteredProtonMailEmails.isNotEmpty()) {
			mergedContacts.add(ContactItem(contactId = "-1", isProtonMailContact =  true)) // adding this for serving as a header item
			mergedContacts.addAll(filteredProtonMailEmails)
		}
		if (filteredAndroidContacts.isNotEmpty()) {
			mergedContacts.add(ContactItem(contactId = "-1", isProtonMailContact = false)) // adding this for serving as a header item
			mergedContacts.addAll(filteredAndroidContacts)
		}
		value = mergedContacts
	}
}
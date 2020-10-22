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
package ch.protonmail.android.contacts.groups.list

import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.api.models.room.contacts.ContactEmailContactLabelJoin
import ch.protonmail.android.api.models.room.contacts.ContactLabel
import ch.protonmail.android.api.models.room.contacts.ContactsDao
import io.reactivex.Observable
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ContactGroupsRepository @Inject constructor(
    private val api: ProtonMailApiManager,
    private val contactsDao: ContactsDao
) {

    fun getJoins(): Observable<List<ContactEmailContactLabelJoin>> {
        return contactsDao.fetchJoinsObservable().toObservable()
    }

    fun getContactGroups(): Observable<List<ContactLabel>> {
        return Observable.concatArrayDelayError(
            getContactGroupsFromDB(),
            getContactGroupsFromApi().debounce(400, TimeUnit.MILLISECONDS)
        )
    }

    fun getContactGroups(filter: String): Observable<List<ContactLabel>> {
        return getContactGroupsFromDB(filter)
    }

    fun getContactGroupEmails(id: String): Observable<List<ContactEmail>> {
        return contactsDao.findAllContactsEmailsByContactGroupAsyncObservable(id)
            .toObservable()
    }

    fun saveContactGroup(contactLabel: ContactLabel) {
        contactsDao.saveContactGroupLabel(contactLabel)
    }

    private fun getContactGroupsFromApi(): Observable<List<ContactLabel>> {
        return api.fetchContactGroupsAsObservable().doOnNext {
            contactsDao.clearContactGroupsLabelsTable()
            contactsDao.saveContactGroupsList(it)
        }
    }

    private fun getContactGroupsFromDB(): Observable<List<ContactLabel>> {
        return contactsDao.findContactGroupsObservable()
            .flatMap { list ->
                Observable.fromIterable(list)
                    .map {
                        it.contactEmailsCount = contactsDao.countContactEmailsByLabelId(it.ID)
                        it
                    }
                    .toList()
                    .toFlowable()
            }
            .toObservable()
    }

    private fun getContactGroupsFromDB(filter: String): Observable<List<ContactLabel>> {
        return contactsDao.findContactGroupsObservable(filter)
            .flatMap { list ->
                Observable.fromIterable(list)
                    .map {
                        it.contactEmailsCount = contactsDao.countContactEmailsByLabelId(it.ID)
                        it
                    }
                    .toList()
                    .toFlowable()
            }
            .toObservable()
    }
}

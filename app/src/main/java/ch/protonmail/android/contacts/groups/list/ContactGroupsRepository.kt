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
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.api.models.room.contacts.ContactEmailContactLabelJoin
import ch.protonmail.android.api.models.room.contacts.ContactLabel
import io.reactivex.Observable
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ContactGroupsRepository @Inject constructor(
    private val api: ProtonMailApiManager,
    private val databaseProvider: DatabaseProvider
) {

    private var contactsDatabase = databaseProvider.provideContactsDao()

    fun reloadDependencies() {
        contactsDatabase = databaseProvider.provideContactsDao()
    }

    fun getJoins(): Observable<List<ContactEmailContactLabelJoin>> {
        return contactsDatabase.fetchJoinsObservable().toObservable()
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

    private fun getContactGroupsFromApi(): Observable<List<ContactLabel>> {
        return api.fetchContactGroupsAsObservable().doOnNext {
            contactsDatabase.clearContactGroupsLabelsTable()
            contactsDatabase.saveContactGroupsList(it)
        }
    }

    private fun getContactGroupsFromDB(): Observable<List<ContactLabel>> {
        return contactsDatabase.findContactGroupsObservable()
            .flatMap { list ->
                Observable.fromIterable(list)
                    .map {
                        it.contactEmailsCount = contactsDatabase.countContactEmailsByLabelId(it.ID)
                        it
                    }
                    .toList()
                    .toFlowable()
            }
            .toObservable()
    }

    private fun getContactGroupsFromDB(filter: String): Observable<List<ContactLabel>> {
        return contactsDatabase.findContactGroupsObservable(filter)
            .flatMap { list ->
                Observable.fromIterable(list)
                    .map {
                        it.contactEmailsCount = contactsDatabase.countContactEmailsByLabelId(it.ID)
                        it
                    }
                    .toList()
                    .toFlowable()
            }
            .toObservable()
    }

    fun getContactGroupEmails(id: String): Observable<List<ContactEmail>> {
        return contactsDatabase.findAllContactsEmailsByContactGroupAsyncObservable(id)
            .toObservable()
    }

    fun saveContactGroup(contactLabel: ContactLabel) {
        TODO("Not yet implemented")
    }
}

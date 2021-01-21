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

import android.util.Log
import androidx.work.WorkManager
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.contacts.receive.ContactLabelFactory
import ch.protonmail.android.api.models.factories.makeInt
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.api.models.room.contacts.ContactLabel
import ch.protonmail.android.worker.PostLabelWorker
import io.reactivex.Observable
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow
import java.io.IOException
import javax.inject.Inject

class ContactGroupDetailsRepository @Inject constructor(
    private val api: ProtonMailApiManager,
    private val databaseProvider: DatabaseProvider,
    private val workManager: WorkManager
) {

    private val contactsDatabase by lazy { /*TODO*/ Log.d("PMTAG", "instantiating contactsDatabase in ContactGroupDetailsRepository"); databaseProvider.provideContactsDao() }

    fun findContactGroupDetailsBlocking(id: String): Single<ContactLabel> =
        contactsDatabase.findContactGroupByIdAsync(id)

    suspend fun findContactGroupDetails(id: String): ContactLabel? =
        contactsDatabase.findContactGroupById(id)

    fun getContactGroupEmailsBlocking(id: String): Observable<List<ContactEmail>> {
        return contactsDatabase.findAllContactsEmailsByContactGroupAsyncObservable(id)
            .toObservable()
    }

    suspend fun getContactGroupEmails(id: String): List<ContactEmail> =
        contactsDatabase.findAllContactsEmailsByContactGroupId(id)

    fun filterContactGroupEmails(id: String, filter: String): Flow<List<ContactEmail>> =
        contactsDatabase.filterContactsEmailsByContactGroup(id, "%$filter%")

    fun createContactGroup(contactLabel: ContactLabel): Single<ContactLabel> {
        val contactLabelConverterFactory = ContactLabelFactory()
        val labelBody = contactLabelConverterFactory.createServerObjectFromDBObject(contactLabel)
        return api.createLabelCompletable(labelBody.labelBody)
            .doOnSuccess { label -> contactsDatabase.saveContactGroupLabel(label) }
            .doOnError { throwable ->
                if (throwable is IOException) {
                    PostLabelWorker.Enqueuer(workManager).enqueue(
                        contactLabel.name,
                        contactLabel.color,
                        contactLabel.display,
                        contactLabel.exclusive.makeInt(),
                        false,
                        contactLabel.ID
                    )
                }
            }
    }
}

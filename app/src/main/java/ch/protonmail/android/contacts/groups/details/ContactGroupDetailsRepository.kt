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
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.exceptions.ApiException
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.contacts.receive.ContactLabelFactory
import ch.protonmail.android.api.models.factories.makeInt
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.api.models.room.contacts.ContactLabel
import ch.protonmail.android.core.Constants
import ch.protonmail.android.jobs.DeleteLabelJob
import ch.protonmail.android.jobs.PostLabelJob
import com.birbit.android.jobqueue.JobManager
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/*
 * Created by kadrikj on 8/23/18. */

@Singleton
class ContactGroupDetailsRepository @Inject constructor(
    val jobManager: JobManager,
    val api: ProtonMailApiManager,
    private val databaseProvider: DatabaseProvider
) {

    private val contactsDatabase by lazy { /*TODO*/ Log.d("PMTAG", "instantiating contactsDatabase in ContactGroupDetailsRepository"); databaseProvider.provideContactsDao() }

    fun findContactGroupDetails(id: String): Single<ContactLabel> {
        return contactsDatabase.findContactGroupByIdAsync(id)
    }

    fun getContactGroupEmails(id: String): Observable<List<ContactEmail>> {
        return contactsDatabase.findAllContactsEmailsByContactGroupAsyncObservable(id)
                .toObservable()
    }

    fun filterContactGroupEmails(id: String, filter: String): Observable<List<ContactEmail>> {
        val filterString = "%$filter%"
        return contactsDatabase.filterContactsEmailsByContactGroupAsyncObservable(id, filterString)
                .toObservable()
    }

    fun createContactGroup(contactLabel: ContactLabel): Single<ContactLabel> {
        val contactLabelConverterFactory = ContactLabelFactory()
        val labelBody = contactLabelConverterFactory.createServerObjectFromDBObject(contactLabel)
        return api.createLabelCompletable(labelBody.labelBody)
                .doOnSuccess { label -> contactsDatabase.saveContactGroupLabel(label) }
                .doOnError { throwable ->
                    if (throwable is IOException) {
                        jobManager.addJobInBackground(PostLabelJob(contactLabel.name, contactLabel.color, contactLabel.display,
                                contactLabel.exclusive.makeInt(), false, contactLabel.ID))
                    }
                }
    }

    fun delete(contactLabel: ContactLabel): Completable {
        return api.deleteLabel(contactLabel.ID)
                .doOnSuccess {
                    it?.let {
                        if (it.code == Constants.RESPONSE_CODE_OK) {
                            contactsDatabase.deleteContactGroup(contactLabel)
                        } else {
                            throw ApiException(it, it.error)
                        }
                    }
                }
                .doOnError {
                    if (it is IOException) {
                        jobManager.addJobInBackground(DeleteLabelJob(contactLabel.ID))
                    }
                }.toCompletable()
    }
}

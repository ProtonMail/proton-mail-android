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

import androidx.work.WorkManager
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.contacts.receive.ContactLabelFactory
import ch.protonmail.android.api.models.contacts.send.LabelContactsBody
import ch.protonmail.android.api.models.room.contacts.ContactData
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.api.models.room.contacts.ContactEmailContactLabelJoin
import ch.protonmail.android.api.models.room.contacts.ContactLabel
import ch.protonmail.android.contacts.groups.jobs.SetMembersForContactGroupJob
import ch.protonmail.android.data.local.ContactsDao
import ch.protonmail.android.worker.PostLabelWorker
import ch.protonmail.android.worker.RemoveMembersFromContactGroupWorker
import com.birbit.android.jobqueue.JobManager
import io.reactivex.Completable
import io.reactivex.Observable
import kotlinx.coroutines.withContext
import me.proton.core.util.kotlin.DispatcherProvider
import java.io.IOException
import java.util.concurrent.TimeUnit
import javax.inject.Inject

open class ContactDetailsRepository @Inject constructor(
    private val workManager: WorkManager,
    protected val jobManager: JobManager,
    protected val api: ProtonMailApiManager,
    protected val contactsDao: ContactsDao,
    private val dispatcherProvider: DispatcherProvider
) {

    fun getContactGroups(id: String): Observable<List<ContactLabel>> {
        return contactsDao.findAllContactGroupsByContactEmailAsyncObservable(id)
            .toObservable()
    }

    fun getContactEmails(id: String): Observable<List<ContactEmail>> {
        return contactsDao.findContactEmailsByContactIdObservable(id)
            .toObservable()
    }

    fun getContactGroups(): Observable<List<ContactLabel>> {
        return Observable.concatArrayDelayError(
            getContactGroupsFromDB(),
            getContactGroupsFromApi()
                .debounce(400, TimeUnit.MILLISECONDS)
        )
    }

    private fun getContactGroupsFromApi(): Observable<List<ContactLabel>> {
        return api.fetchContactGroupsAsObservable().doOnNext {
            contactsDao.clearContactGroupsLabelsTableBlocking()
            contactsDao.saveContactGroupsListBlocking(it)
        }
    }

    private fun getContactGroupsFromDB(): Observable<List<ContactLabel>> {
        return contactsDao.findContactGroupsObservable()
            .flatMap { list ->
                Observable.fromIterable(list)
                    .map {
                        it.contactEmailsCount = contactsDao.countContactEmailsByLabelIdBlocking(it.ID)
                        it
                    }
                    .toList()
                    .toFlowable()
            }
            .toObservable()
    }

    fun editContactGroup(contactLabel: ContactLabel): Completable {
        val contactLabelConverterFactory = ContactLabelFactory()
        val labelBody = contactLabelConverterFactory.createServerObjectFromDBObject(contactLabel)
        return api.updateLabelCompletable(contactLabel.ID, labelBody.labelBody)
            .doOnComplete {
                val joins = contactsDao.fetchJoins(contactLabel.ID)
                contactsDao.saveContactGroupLabel(contactLabel)
                contactsDao.saveContactEmailContactLabelBlocking(joins)
            }
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

    fun setMembersForContactGroup(contactGroupId: String, contactGroupName: String, membersList: List<String>): Completable {
        val labelContactsBody = LabelContactsBody(contactGroupId, membersList)
        return api.labelContacts(labelContactsBody)
            .doOnComplete {
                val joins = contactsDao.fetchJoins(contactGroupId) as ArrayList
                for (contactEmail in membersList) {
                    joins.add(ContactEmailContactLabelJoin(contactEmail, contactGroupId))
                }
                contactsDao.saveContactEmailContactLabelBlocking(joins)
            }
            .doOnError { throwable ->
                if (throwable is IOException) {
                    jobManager.addJobInBackground(SetMembersForContactGroupJob(contactGroupId, contactGroupName, membersList))
                }
            }
    }

    fun removeMembersForContactGroup(
        contactGroupId: String, contactGroupName: String,
        membersList: List<String>
    ): Completable {
        if (membersList.isEmpty()) {
            return Completable.complete()
        }
        val labelContactsBody = LabelContactsBody(contactGroupId, membersList)
        return api.unlabelContactEmailsCompletable(labelContactsBody)
            .doOnComplete {
                contactsDao.deleteJoinByGroupIdAndEmailId(membersList, contactGroupId)
            }
            .doOnError { throwable ->
                if (throwable is IOException) {
                    RemoveMembersFromContactGroupWorker.Enqueuer(workManager).enqueue(
                        contactGroupId,
                        contactGroupName,
                        membersList
                    )
                }
            }
    }

    suspend fun saveContactEmails(emails: List<ContactEmail>) = withContext(dispatcherProvider.Io) {
        contactsDao.saveAllContactsEmails(emails)
    }

    suspend fun updateContactDataWithServerId(contactDataInDb: ContactData, contactServerId: String) {
        withContext(dispatcherProvider.Io) {
            contactsDao.findContactDataByDbId(contactDataInDb.dbId ?: -1)?.let {
                it.contactId = contactServerId
                contactsDao.saveContactData(it)
            }
        }
    }

    suspend fun updateAllContactEmails(contactId: String?, contactServerEmails: List<ContactEmail>) {
        withContext(dispatcherProvider.Io) {
            contactId?.let {
                val localContactEmails = contactsDao.findContactEmailsByContactId(it)
                contactsDao.deleteAllContactsEmails(localContactEmails)
                contactsDao.saveAllContactsEmailsBlocking(contactServerEmails)
            }
        }
    }

    suspend fun deleteContactData(contactData: ContactData) =
        withContext(dispatcherProvider.Io) {
            contactsDao.deleteContactData(contactData)
        }

    suspend fun saveContactData(contactData: ContactData): Long =
        withContext(dispatcherProvider.Io) {
            return@withContext contactsDao.saveContactData(contactData)
        }

}

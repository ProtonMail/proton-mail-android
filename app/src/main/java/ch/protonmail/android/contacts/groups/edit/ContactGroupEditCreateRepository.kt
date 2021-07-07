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
package ch.protonmail.android.contacts.groups.edit

import androidx.work.WorkManager
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.contacts.send.LabelContactsBody
import ch.protonmail.android.api.models.factories.IConverterFactory
import ch.protonmail.android.api.models.messages.receive.ServerLabel
import ch.protonmail.android.contacts.groups.jobs.SetMembersForContactGroupJob
import ch.protonmail.android.data.local.ContactDao
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.data.local.model.ContactEmailContactLabelJoin
import ch.protonmail.android.data.local.model.ContactLabel
import ch.protonmail.android.worker.CreateContactGroupWorker
import ch.protonmail.android.worker.RemoveMembersFromContactGroupWorker
import com.birbit.android.jobqueue.JobManager
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import me.proton.core.util.kotlin.toInt
import java.io.IOException
import javax.inject.Inject

class ContactGroupEditCreateRepository @Inject constructor(
    val jobManager: JobManager,
    val workManager: WorkManager,
    val apiManager: ProtonMailApiManager,
    private val contactDao: ContactDao,
    private val contactLabelFactory: IConverterFactory<ServerLabel, ContactLabel>,
    private val createContactGroupWorker: CreateContactGroupWorker.Enqueuer
) {

    fun editContactGroup(contactLabel: ContactLabel): Completable {
        val labelBody = contactLabelFactory.createServerObjectFromDBObject(contactLabel)
        return apiManager.updateLabelCompletable(contactLabel.ID, labelBody.labelBody)
            .doOnComplete {
                val joins = contactDao.fetchJoinsBlocking(contactLabel.ID)
                contactDao.saveContactGroupLabel(contactLabel)
                contactDao.saveContactEmailContactLabelBlocking(joins)
            }
            .doOnError { throwable ->
                if (throwable is IOException) {
                    enqueueCreateContactGroupWorker(contactLabel, true)
                }
            }
    }

    fun getContactGroupEmails(id: String): Observable<List<ContactEmail>> {
        return contactDao.findAllContactsEmailsByContactGroupAsyncObservable(id)
            .toObservable()
    }

    fun removeMembersFromContactGroup(
        contactGroupId: String,
        contactGroupName: String,
        membersList: List<String>
    ): Completable {
        if (membersList.isEmpty()) {
            return Completable.complete()
        }
        val labelContactsBody = LabelContactsBody(contactGroupId, membersList)
        return apiManager.unlabelContactEmailsCompletable(labelContactsBody)
            .doOnComplete {
                val list = ArrayList<ContactEmailContactLabelJoin>()
                for (contactEmail in membersList) {
                    list.add(ContactEmailContactLabelJoin(contactEmail, contactGroupId))
                }
                contactDao.deleteContactEmailContactLabel(list)
            }.doOnError { throwable ->
                if (throwable is IOException) {
                    RemoveMembersFromContactGroupWorker.Enqueuer(workManager).enqueue(
                        contactGroupId,
                        contactGroupName,
                        membersList
                    )
                }
            }
    }

    fun setMembersForContactGroup(
        contactGroupId: String,
        contactGroupName: String,
        membersList: List<String>
    ): Completable {
        if (membersList.isEmpty()) {
            return Completable.complete()
        }
        val labelContactsBody = LabelContactsBody(contactGroupId, membersList)
        return apiManager.labelContacts(labelContactsBody)
            .doOnComplete {
                val list = ArrayList<ContactEmailContactLabelJoin>()
                for (contactEmail in membersList) {
                    list.add(ContactEmailContactLabelJoin(contactEmail, contactGroupId))
                }
                getContactGroupEmails(contactGroupId).test().values()
                contactDao.saveContactEmailContactLabelBlocking(list)
            }.doOnError { throwable ->
                if (throwable is IOException) {
                    jobManager.addJobInBackground(
                        SetMembersForContactGroupJob(contactGroupId, contactGroupName, membersList)
                    )
                }
            }
    }

    fun createContactGroup(contactLabel: ContactLabel): Single<ContactLabel> {
        val labelBody = contactLabelFactory.createServerObjectFromDBObject(contactLabel)
        return apiManager.createLabelCompletable(labelBody.labelBody)
            .doOnSuccess { label -> contactDao.saveContactGroupLabel(label) }
            .doOnError { throwable ->
                if (throwable is IOException) {
                    enqueueCreateContactGroupWorker(contactLabel, false)
                }
            }
    }

    private fun enqueueCreateContactGroupWorker(contactLabel: ContactLabel, isUpdate: Boolean) {
        createContactGroupWorker.enqueue(
            contactLabel.name,
            contactLabel.color,
            contactLabel.display,
            contactLabel.exclusive.toInt(),
            isUpdate,
            contactLabel.ID
        )
    }
}

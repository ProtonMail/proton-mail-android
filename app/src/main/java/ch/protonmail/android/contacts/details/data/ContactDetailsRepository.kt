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
package ch.protonmail.android.contacts.details.data

import androidx.work.WorkManager
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.labels.domain.mapper.LabelsMapper
import ch.protonmail.android.api.models.contacts.send.LabelContactsBody
import ch.protonmail.android.labels.data.model.LabelResponse
import ch.protonmail.android.contacts.details.presentation.model.ContactLabelUiModel
import ch.protonmail.android.contacts.groups.jobs.SetMembersForContactGroupJob
import ch.protonmail.android.data.local.ContactDao
import ch.protonmail.android.data.local.model.ContactData
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.data.local.model.ContactEmailContactLabelJoin
import ch.protonmail.android.data.local.model.FullContactDetails
import ch.protonmail.android.labels.data.db.LabelEntity
import ch.protonmail.android.labels.data.model.LabelId
import ch.protonmail.android.worker.PostLabelWorker
import ch.protonmail.android.worker.RemoveMembersFromContactGroupWorker
import com.birbit.android.jobqueue.JobManager
import io.reactivex.Completable
import io.reactivex.Observable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.ApiResult
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

open class ContactDetailsRepository @Inject constructor(
    private val workManager: WorkManager,
    protected val jobManager: JobManager,
    protected val api: ProtonMailApiManager,
    protected val contactDao: ContactDao,
    private val dispatcherProvider: DispatcherProvider,
    private val labelsMapper: LabelsMapper
) {

    fun getContactGroups(id: String): Observable<List<LabelEntity>> {
        return contactDao.findAllContactGroupsByContactEmailAsyncObservable(id)
            .toObservable()
    }

    suspend fun getContactGroupsLabelForId(emailId: String): List<LabelEntity> =
        contactDao.getAllContactGroupsByContactEmail(emailId)

    fun getContactEmails(id: String): Observable<List<ContactEmail>> {
        return contactDao.findContactEmailsByContactIdObservable(id)
            .toObservable()
    }

    fun observeContactEmails(contactId: String): Flow<List<ContactEmail>> =
        contactDao.observeContactEmailsByContactId(contactId)

    fun getContactGroups(userId: UserId): Observable<List<LabelEntity>> {
        return Observable.fromCallable {
            runBlocking {
                val dbContacts = getContactGroupsFromDb()
                val apiContacts = getContactGroupsFromApi(userId)
                dbContacts.plus(apiContacts)
            }
        }
    }

    fun getContactEmailsCount(contactGroupId: LabelId) =
        contactDao.countContactEmailsByLabelIdBlocking(contactGroupId.id)

    private suspend fun getContactGroupsFromApi(userId: UserId): List<LabelEntity> {
        val contactGroupsResponse = api.fetchContactGroups(userId).valueOrNull?.labels

        return contactGroupsResponse?.let { labels ->
            labels.map { label ->
                labelsMapper.mapLabelToLabelEntity(label, userId)
            }
        }?.also {
            contactDao.clearContactGroupsLabelsTable()
            contactDao.saveContactGroupsList(it)
        } ?: emptyList()
    }

    private suspend fun getContactGroupsFromDb(): List<LabelEntity> {
        return contactDao.findContactGroups()
            .flatMapConcat { list ->
                list.map { entity ->
                    ContactLabelUiModel(
                        id = entity.id,
                        name = entity.name,
                        color = entity.color,
                        type = entity.type,
                        path = entity.path,
                        parentId = entity.parentId,
                        expanded = entity.expanded,
                        sticky = entity.sticky,
                        contactEmailsCount = getContactEmailsCount(entity.id)
                    )
                }
                list.asFlow()
            }.toList()
    }

    suspend fun editContactGroup(contactLabel: LabelEntity, userId: UserId): ApiResult<LabelResponse> {
        val labelBody = labelsMapper.mapLabelEntityToRequestLabel(contactLabel)
        val updateLabelResult = api.updateLabel(userId, contactLabel.id.id, labelBody)
        when (updateLabelResult) {
            is ApiResult.Success -> {
                val joins = contactDao.fetchJoins(contactLabel.id.id)
                contactDao.saveContactGroupLabel(contactLabel)
                contactDao.saveContactEmailContactLabel(joins)
            }
            is ApiResult.Error.Http -> {
                PostLabelWorker.Enqueuer(workManager).enqueue(
                    contactLabel.name,
                    contactLabel.color,
                    contactLabel.expanded,
                    contactLabel.type,
                    false,
                    contactLabel.id.id
                )
            }
            else -> {
                Timber.w("updateLabel error $updateLabelResult")
            }
        }
        return updateLabelResult
    }

    fun setMembersForContactGroup(
        contactGroupId: String, contactGroupName: String, membersList: List<String>
    ): Completable {
        val labelContactsBody = LabelContactsBody(contactGroupId, membersList)
        return api.labelContacts(labelContactsBody)
            .doOnComplete {
                val joins = contactDao.fetchJoinsBlocking(contactGroupId) as ArrayList
                for (contactEmail in membersList) {
                    joins.add(ContactEmailContactLabelJoin(contactEmail, contactGroupId))
                }
                contactDao.saveContactEmailContactLabelBlocking(joins)
            }
            .doOnError { throwable ->
                if (throwable is IOException) {
                    jobManager.addJobInBackground(
                        SetMembersForContactGroupJob(contactGroupId, contactGroupName, membersList)
                    )
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
                contactDao.deleteJoinByGroupIdAndEmailId(membersList, contactGroupId)
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
        contactDao.saveAllContactsEmails(emails)
    }

    suspend fun updateContactDataWithServerId(contactDataInDb: ContactData, contactServerId: String) {
        withContext(dispatcherProvider.Io) {
            contactDao.findContactDataByDbId(contactDataInDb.dbId ?: -1)?.let {
                it.contactId = contactServerId
                contactDao.saveContactData(it)
            }
        }
    }

    suspend fun updateAllContactEmails(contactId: String?, contactServerEmails: List<ContactEmail>) {
        withContext(dispatcherProvider.Io) {
            contactId?.let {
                val localContactEmails = contactDao.findContactEmailsByContactId(it)
                contactDao.deleteAllContactsEmails(localContactEmails)
                contactDao.saveAllContactsEmails(contactServerEmails)
            }
        }
    }

    suspend fun deleteContactData(contactData: ContactData) =
        withContext(dispatcherProvider.Io) {
            contactDao.deleteContactData(contactData)
        }

    suspend fun saveContactData(contactData: ContactData): Long =
        withContext(dispatcherProvider.Io) {
            return@withContext contactDao.saveContactData(contactData)
        }

    fun observeFullContactDetails(contactId: String): Flow<FullContactDetails> =
        contactDao.observeFullContactDetailsById(contactId)
            .distinctUntilChanged()
            .onEach { savedContacts ->
                Timber.v("Fetched saved Contact Details $savedContacts")
                if (savedContacts == null) {
                    val response = api.fetchContactDetails(contactId)
                    val fetchedContact = response.contact
                    Timber.d("Fetched new Contact Details $fetchedContact")
                    insertFullContactDetails(fetchedContact)
                }
            }
            .filterNotNull()

    suspend fun insertFullContactDetails(fullContactDetails: FullContactDetails) =
        contactDao.insertFullContactDetails(fullContactDetails)

}

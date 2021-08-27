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
import ch.protonmail.android.contacts.groups.jobs.SetMembersForContactGroupJob
import ch.protonmail.android.data.local.ContactDao
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.data.local.model.ContactEmailContactLabelJoin
import ch.protonmail.android.labels.data.LabelRepository
import ch.protonmail.android.labels.data.db.LabelEntity
import ch.protonmail.android.labels.data.mapper.LabelsMapper
import ch.protonmail.android.labels.data.model.LabelResponse
import ch.protonmail.android.worker.CreateContactGroupWorker
import ch.protonmail.android.worker.RemoveMembersFromContactGroupWorker
import com.birbit.android.jobqueue.JobManager
import io.reactivex.Completable
import io.reactivex.Observable
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.ApiResult
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class ContactGroupEditCreateRepository @Inject constructor(
    val jobManager: JobManager,
    val workManager: WorkManager,
    val apiManager: ProtonMailApiManager,
    private val contactDao: ContactDao,
    private val labelsMapper: LabelsMapper,
    private val createContactGroupWorker: CreateContactGroupWorker.Enqueuer,
    private val labelRepository: LabelRepository
) {

    suspend fun editContactGroup(contactLabel: LabelEntity, userId: UserId): ApiResult<LabelResponse> {
        val labelBody = labelsMapper.mapLabelEntityToRequestLabel(contactLabel)
        val updateLabelResult = apiManager.updateLabel(userId, contactLabel.id.id, labelBody)
        when (updateLabelResult) {
            is ApiResult.Success -> {
                val label = updateLabelResult.value.label
                val joins = contactDao.fetchJoins(contactLabel.id.id)
                labelRepository.saveLabel(
                    labelsMapper.mapLabelToLabelEntity(label, userId)
                )
                contactDao.saveContactEmailContactLabel(joins)
            }
            is ApiResult.Error.Http -> {
                enqueueCreateContactGroupWorker(contactLabel, true)
            }
            else -> {
                Timber.w("updateLabel failure $updateLabelResult")
            }
        }
        return updateLabelResult
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
                        SetMembersForContactGroupJob(contactGroupId, contactGroupName, membersList, labelRepository)
                    )
                }
            }
    }

    suspend fun createContactGroup(contactLabel: LabelEntity, userId: UserId): ApiResult<LabelResponse> {
        val labelRequestBody = labelsMapper.mapLabelEntityToRequestLabel(contactLabel)
        val createLabelResult = apiManager.createLabel(userId, labelRequestBody)
        when (createLabelResult) {
            is ApiResult.Success -> {
                val label = createLabelResult.value.label
                labelRepository.saveLabel(
                    labelsMapper.mapLabelToLabelEntity(label, userId)
                )
            }
            is ApiResult.Error.Http -> {
                enqueueCreateContactGroupWorker(contactLabel, false)
            }
            else -> {
                Timber.w("createContactGroup failure $createLabelResult")
            }
        }

        return createLabelResult
    }

    private fun enqueueCreateContactGroupWorker(contactLabel: LabelEntity, isUpdate: Boolean) {
        createContactGroupWorker.enqueue(
            contactLabel.name,
            contactLabel.color,
            contactLabel.expanded,
            contactLabel.sticky,
            isUpdate,
            contactLabel.id.id
        )
    }
}

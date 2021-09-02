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
import ch.protonmail.android.data.ContactsRepository
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.labels.data.LabelRepository
import ch.protonmail.android.labels.data.local.model.LabelEntity
import ch.protonmail.android.labels.data.mapper.LabelsMapper
import ch.protonmail.android.labels.data.remote.model.LabelResponse
import ch.protonmail.android.worker.CreateContactGroupWorker
import ch.protonmail.android.worker.RemoveMembersFromContactGroupWorker
import com.birbit.android.jobqueue.JobManager
import kotlinx.coroutines.flow.Flow
import me.proton.core.domain.entity.UserId
import me.proton.core.network.domain.ApiResult
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class ContactGroupEditCreateRepository @Inject constructor(
    val jobManager: JobManager,
    val workManager: WorkManager,
    val apiManager: ProtonMailApiManager,
    private val contactRepository: ContactsRepository,
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
                labelRepository.saveLabel(
                    labelsMapper.mapLabelToLabelEntity(label, userId)
                )
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

    fun observeContactGroupEmails(id: String): Flow<List<ContactEmail>> =
        contactRepository.observeAllContactEmailsByContactGroupId(id)

    suspend fun removeMembersFromContactGroup(
        contactGroupLabelId: String,
        contactGroupName: String,
        membersList: List<String>
    ) {
        if (membersList.isEmpty()) {
            Timber.v("No group members to remove")
            return
        }
        Timber.v("Remove contact Members contactLabelId: $contactGroupLabelId, contactGroupName: $contactGroupName")
        runCatching {
            val labelContactsBody = LabelContactsBody(contactGroupLabelId, membersList)
            apiManager.unlabelContactEmails(labelContactsBody)
        }.fold(
            onSuccess = {
                val contactEmails = contactRepository.findAllContactEmailsByContactGroupId(contactGroupLabelId)
                contactEmails.forEach { contactEmail ->
                    if (contactEmail.contactEmailId in membersList) {
                        val updatedList = contactEmail.labelIds?.toMutableList()
                        if (updatedList != null) {
                            updatedList.remove(contactGroupLabelId)
                            contactRepository.saveContactEmail(contactEmail.copy(labelIds = updatedList))
                        }
                    }
                }
            },
            onFailure = { throwable ->
                if (throwable is IOException) {
                    RemoveMembersFromContactGroupWorker.Enqueuer(workManager).enqueue(
                        contactGroupLabelId,
                        contactGroupName,
                        membersList
                    )
                }
            }
        )
    }

    suspend fun setMembersForContactGroup(
        contactGroupLabelId: String,
        contactGroupName: String,
        membersList: List<String>
    ) {
        if (membersList.isEmpty()) {
            Timber.v("No group members to update")
            return
        }
        Timber.v("Set contact Members contactGroupId: $contactGroupLabelId, contactGroupName: $contactGroupName")
        runCatching {
            val labelContactsBody = LabelContactsBody(contactGroupLabelId, membersList)
            apiManager.labelContacts(labelContactsBody)
        }.fold(
            onSuccess = {
                membersList.forEach { newMemberEmailId ->
                    val contactEmail = contactRepository.findAllContactEmailsById(newMemberEmailId)
                    if (contactEmail != null) {
                        val updatedList = contactEmail.labelIds?.toMutableSet() ?: mutableSetOf()
                        updatedList.add(contactGroupLabelId)
                        contactRepository.saveContactEmail(contactEmail.copy(labelIds = updatedList.toList()))
                    } else {
                        Timber.i("Cannot add email with ID: $newMemberEmailId to a group!")
                    }
                }
            },
            onFailure = { throwable ->
                if (throwable is IOException) {
                    jobManager.addJobInBackground(
                        SetMembersForContactGroupJob(
                            contactGroupLabelId,
                            contactGroupName,
                            membersList,
                            labelRepository
                        )
                    )
                }
            }
        )
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

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
package ch.protonmail.android.data

import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.labels.data.local.model.LabelEntity
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.LabelId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import javax.inject.Inject

class ContactsRepository @Inject constructor(
    private val databaseProvider: DatabaseProvider,
    private val userManager: UserManager,
    private val labelRepository: LabelRepository
) {

    private val contactDao by lazy {
        Timber.v("Instantiating contactDao in ContactsRepository")
        databaseProvider.provideContactDao(userManager.requireCurrentUserId())
    }

    suspend fun findContactEmailByEmail(email: String): ContactEmail? =
        contactDao.findContactEmailByEmail(email)

    fun findAllContactEmails(): Flow<List<ContactEmail>> = contactDao.findAllContactsEmails()

    fun findContactsByEmail(emails: List<String>): Flow<List<ContactEmail>> =
        contactDao.findContactsByEmail(emails)

    suspend fun countContactEmailsByLabelId(labelId: LabelId): Int =
        // we take only a part of the label as in the old DB they were Unicode escaped and has some brackets
        contactDao.countContactEmailsByGroupId(labelId.id.take(IMPORTANT_LABEL_CHARACTERS_COUNT))

    suspend fun getAllContactGroupsByContactEmail(emailId: String): List<LabelEntity> =
        observeAllContactGroupsByContactEmail(emailId).first()

    private fun observeAllContactGroupsByContactEmail(emailId: String): Flow<List<LabelEntity>> =
        contactDao.observeContactEmailById(emailId)
            .filterNotNull()
            .map { contactEmail ->
                val labelsIds = contactEmail.labelIds?.map { LabelId(it) }
                if (!labelsIds.isNullOrEmpty()) {
                    labelRepository.findLabels(labelsIds, userManager.requireCurrentUserId())
                } else {
                    emptyList()
                }
            }

    suspend fun findAllContactEmailsByContactGroupId(groupLabelId: String): List<ContactEmail> =
        contactDao.observeAllContactsEmailsByContactGroup(groupLabelId.take(IMPORTANT_LABEL_CHARACTERS_COUNT))
            .first()

    suspend fun findAllContactEmailsById(emailId: String): ContactEmail? =
        contactDao.findContactEmailById(emailId)

    fun observeAllContactEmailsByContactGroupId(groupLabelId: String): Flow<List<ContactEmail>> =
        contactDao.observeAllContactsEmailsByContactGroup(groupLabelId.take(IMPORTANT_LABEL_CHARACTERS_COUNT))

    fun observeFilterContactEmailsByContactGroup(groupLabelId: String, filter: String): Flow<List<ContactEmail>> =
        contactDao.observeFilterContactEmailsByContactGroup(groupLabelId.take(IMPORTANT_LABEL_CHARACTERS_COUNT), filter)

    suspend fun saveContactEmail(contactEmail: ContactEmail) =
        contactDao.saveContactEmail(contactEmail)

    companion object {

        private const val IMPORTANT_LABEL_CHARACTERS_COUNT = 80
    }
}

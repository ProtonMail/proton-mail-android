/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.data

import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.labels.domain.LabelRepository
import ch.protonmail.android.labels.domain.model.Label
import ch.protonmail.android.labels.domain.model.LabelId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import me.proton.core.domain.entity.UserId
import javax.inject.Inject

class ContactsRepository @Inject constructor(
    private val databaseProvider: DatabaseProvider,
    private val labelRepository: LabelRepository
) {

    suspend fun findContactEmailByEmail(userId: UserId, email: String): ContactEmail? =
        contactDao(userId).findContactEmailByEmail(email)

    fun findAllContactEmails(userId: UserId): Flow<List<ContactEmail>> =
        contactDao(userId).findAllContactsEmails()

    fun findContactsByEmail(userId: UserId, emails: List<String>): Flow<List<ContactEmail>> =
        contactDao(userId).findContactsByEmail(emails)

    suspend fun countContactEmailsByLabelId(userId: UserId, labelId: LabelId): Int =
        // we take only a part of the label as in the old DB they were Unicode escaped and has some brackets
        contactDao(userId).countContactEmailsByGroupId(labelId.id.take(IMPORTANT_LABEL_CHARACTERS_COUNT))

    suspend fun getAllContactGroupsByContactEmail(userId: UserId, emailId: String): List<Label> =
        contactDao(userId).observeContactEmailById(emailId)
            .filterNotNull()
            .map { contactEmail ->
                val labelsIds = contactEmail.labelIds?.map { LabelId(it) }
                if (!labelsIds.isNullOrEmpty()) {
                    labelRepository.findLabels(labelsIds)
                } else {
                    emptyList()
                }
            }.first()

    suspend fun findAllContactEmailsByContactGroupId(userId: UserId, groupLabelId: String): List<ContactEmail> =
        contactDao(userId).observeAllContactsEmailsByContactGroup(groupLabelId.take(IMPORTANT_LABEL_CHARACTERS_COUNT))
            .first()

    suspend fun findAllContactEmailsById(userId: UserId, emailId: String): ContactEmail? =
        contactDao(userId).findContactEmailById(emailId)

    fun observeAllContactEmailsByContactGroupId(userId: UserId, groupLabelId: String): Flow<List<ContactEmail>> =
        if (groupLabelId.isBlank()) {
            flowOf(emptyList())
        } else {
            contactDao(userId)
                .observeAllContactsEmailsByContactGroup(groupLabelId.take(IMPORTANT_LABEL_CHARACTERS_COUNT))
        }

    fun observeFilterContactEmailsByContactGroup(userId: UserId, groupLabelId: String, filter: String): Flow<List<ContactEmail>> =
        contactDao(userId)
            .observeFilterContactEmailsByContactGroup(groupLabelId.take(IMPORTANT_LABEL_CHARACTERS_COUNT), filter)

    suspend fun saveContactEmail(userId: UserId, contactEmail: ContactEmail) =
        contactDao(userId).saveContactEmail(contactEmail)

    private fun contactDao(userId: UserId) =
        databaseProvider.provideContactDao(userId)

    companion object {

        private const val IMPORTANT_LABEL_CHARACTERS_COUNT = 80
    }
}

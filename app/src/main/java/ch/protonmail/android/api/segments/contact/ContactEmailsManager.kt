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
package ch.protonmail.android.api.segments.contact

import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.ContactEmailsResponseV2
import ch.protonmail.android.api.models.DatabaseProvider
import ch.protonmail.android.api.models.contacts.receive.ContactGroupsResponse
import ch.protonmail.android.api.models.contacts.receive.LabelsMapper
import ch.protonmail.android.api.models.messages.receive.Label
import ch.protonmail.android.core.Constants
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.data.local.model.ContactEmailContactLabelJoin
import ch.protonmail.android.data.local.model.ContactLabelEntity
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import me.proton.core.accountmanager.domain.AccountManager
import timber.log.Timber
import javax.inject.Inject

class ContactEmailsManager @Inject constructor(
    private var api: ProtonMailApiManager,
    private val databaseProvider: DatabaseProvider,
    private val accountManager: AccountManager,
    private val labelsMapper: LabelsMapper
) {

    suspend fun refresh(pageSize: Int = Constants.CONTACTS_PAGE_SIZE) {
        val userId = accountManager.getPrimaryUserId().filterNotNull().first()
        val contactGroupsResponse: ContactGroupsResponse? =
            api.fetchContactGroups(userId).valueOrNull

        var currentPage = 0
        var hasMorePages = true
        val allResults = mutableListOf<ContactEmailsResponseV2>()
        while (hasMorePages) {
            val result = api.fetchContactEmails(currentPage, pageSize)
            allResults += result
            hasMorePages = currentPage < result.total / pageSize
            currentPage++
        }

        if (allResults.isNotEmpty()) {
            val allContactEmails = allResults.flatMap { it.contactEmails }
            val allJoins = getJoins(allContactEmails)
            val contactLabels = mapToContactLabelsEntity(contactGroupsResponse?.labels)
            Timber.v(
                "Refresh emails: ${allContactEmails.size}, labels: ${contactLabels.size}, allJoins: ${allJoins.size}"
            )
            val contactsDao = databaseProvider.provideContactDao(userId)
            contactsDao.insertNewContactsAndLabels(allContactEmails, contactLabels, allJoins)
        } else {
            Timber.v("contactEmails result list is empty")
        }
    }

    private fun getJoins(allContactEmails: List<ContactEmail>): List<ContactEmailContactLabelJoin> {
        val allJoins = mutableListOf<ContactEmailContactLabelJoin>()
        for (contactEmail in allContactEmails) {
            val labelIds = contactEmail.labelIds
            if (labelIds != null) {
                for (labelId in labelIds) {
                    allJoins.add(ContactEmailContactLabelJoin(contactEmail.contactEmailId, labelId))
                }
            }
        }
        return allJoins
    }

    private fun mapToContactLabelsEntity(labels: List<Label>?): List<ContactLabelEntity> {
        return labels?.map { label ->
            labelsMapper.mapLabelToContactLabelEntity(label)
        } ?: emptyList()
    }

}

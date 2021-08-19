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
package ch.protonmail.android.contacts.groups.list

import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.contacts.details.presentation.model.ContactLabelUiModel
import ch.protonmail.android.data.local.ContactDao
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.data.local.model.ContactEmailContactLabelJoin
import ch.protonmail.android.data.local.model.ContactLabelEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import me.proton.core.util.kotlin.DispatcherProvider
import javax.inject.Inject

class ContactGroupsRepository @Inject constructor(
    private val api: ProtonMailApiManager,
    private val contactDao: ContactDao,
    private val dispatchers: DispatcherProvider
) {

    fun getJoins(): Flow<List<ContactEmailContactLabelJoin>> = contactDao.observeJoins()

    fun observeContactGroups(filter: String): Flow<List<ContactLabelUiModel>> =
        contactDao.findContactGroups("$SEARCH_DELIMITER$filter$SEARCH_DELIMITER")
            .map { labels ->
                labels.map { entity ->
                    ContactLabelUiModel(
                        id = entity.id,
                        name = entity.name,
                        color = entity.color,
                        type = entity.type,
                        path = entity.path,
                        parentId = entity.parentId,
                        expanded = entity.expanded,
                        sticky = entity.sticky,
                        contactEmailsCount = contactDao.countContactEmailsByLabelIdBlocking(entity.id)
                    )
                }
            }
            .flowOn(dispatchers.Io)

    suspend fun getContactGroupEmails(id: String): List<ContactEmail> =
        contactDao.findAllContactsEmailsByContactGroup(id).first()

    fun saveContactGroup(contactLabel: ContactLabelEntity) {
        contactDao.saveContactGroupLabel(contactLabel)
    }

    private companion object {
        private const val SEARCH_DELIMITER = "%"
    }
}

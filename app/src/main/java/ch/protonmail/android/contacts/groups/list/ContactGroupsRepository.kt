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
import ch.protonmail.android.data.local.ContactDao
import ch.protonmail.android.data.local.model.ContactEmail
import ch.protonmail.android.data.local.model.ContactEmailContactLabelJoin
import ch.protonmail.android.data.local.model.ContactLabel
import io.reactivex.Observable
import kotlinx.coroutines.flow.Flow
import me.proton.core.util.kotlin.DispatcherProvider
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class ContactGroupsRepository @Inject constructor(
    private val api: ProtonMailApiManager,
    private val contactDao: ContactDao,
    private val dispatchers: DispatcherProvider
) {

    fun getJoins(): Flow<List<ContactEmailContactLabelJoin>> = contactDao.fetchJoins()

    fun observeContactGroups(filter: String): Flow<List<ContactLabel>> =
        contactDao.findContactGroupsFlow("$SEARCH_DELIMITER$filter$SEARCH_DELIMITER")
            .map { labels ->
                labels.map { label -> label.contactEmailsCount = contactDao.countContactEmailsByLabelId(label.ID) }
                labels
            }
            .flowOn(dispatchers.Io)

    suspend fun getContactGroupEmails(id: String): List<ContactEmail> =
        contactDao.findAllContactsEmailsByContactGroupId(id)

    fun saveContactGroup(contactLabel: ContactLabel) {
        contactDao.saveContactGroupLabel(contactLabel)
    }

    suspend fun getContactGroupsFromApi(): List<ContactLabel> {
        return api.fetchContactGroupsList().also { labels ->
            contactDao.clearContactGroupsLabelsTable()
            contactDao.saveContactGroupsList(labels)
        }
    }

    private companion object {
        private const val SEARCH_DELIMITER = "%"
    }
}

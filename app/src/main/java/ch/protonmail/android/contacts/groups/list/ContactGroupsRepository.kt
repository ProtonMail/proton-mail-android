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
import ch.protonmail.android.api.models.room.contacts.ContactEmail
import ch.protonmail.android.api.models.room.contacts.ContactEmailContactLabelJoin
import ch.protonmail.android.api.models.room.contacts.ContactLabel
import ch.protonmail.android.api.models.room.contacts.ContactsDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import me.proton.core.util.kotlin.DispatcherProvider
import me.proton.core.util.kotlin.EMPTY_STRING
import timber.log.Timber
import javax.inject.Inject

class ContactGroupsRepository @Inject constructor(
    private val api: ProtonMailApiManager,
    private val contactsDao: ContactsDao,
    private val dispatchers: DispatcherProvider
) {

    suspend fun getContactGroups(): Flow<List<ContactLabel>> =
        flow {
            emitAll(getContactGroupsFromDb())
            emit(getContactGroupsFromApi())
        }
            .catch { Timber.w(it, "Fetch contacts groups error") }
            .flowOn(dispatchers.Io)

    fun getJoins(): Flow<List<ContactEmailContactLabelJoin>> = contactsDao.fetchJoins()

    fun getContactGroupsFiltered(filter: String): Flow<List<ContactLabel>> = getContactGroupsFromDb(filter)
        .flowOn(dispatchers.Io)

    suspend fun getContactGroupEmails(id: String): List<ContactEmail> =
        contactsDao.findAllContactsEmailsByContactGroupId(id)

    fun saveContactGroup(contactLabel: ContactLabel) {
        contactsDao.saveContactGroupLabel(contactLabel)
    }

    private suspend fun getContactGroupsFromApi(): List<ContactLabel> {
        return api.fetchContactGroupsList().also {
            contactsDao.clearContactGroupsLabelsTable()
            contactsDao.saveContactGroupsList(it)
        }
    }

    private fun getContactGroupsFromDb(filter: String = EMPTY_STRING): Flow<List<ContactLabel>> {
        return if (filter.isEmpty()) {
            contactsDao.findContactGroups()
        } else {
            contactsDao.findContactGroups(filter)
        }.map { labels ->
            labels.map { label ->
                label.contactEmailsCount = contactsDao.countContactEmailsByLabelId(label.ID)
            }
            labels
        }
            .flowOn(dispatchers.Io)
    }
}

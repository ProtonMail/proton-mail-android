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

package ch.protonmail.android.contacts.details.domain

import android.database.sqlite.SQLiteBlobTooBigException
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.contacts.details.data.ContactDetailsRepository
import ch.protonmail.android.contacts.details.domain.model.FetchContactDetailsResult
import ch.protonmail.android.data.local.model.FullContactDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import me.proton.core.util.kotlin.DispatcherProvider
import timber.log.Timber
import javax.inject.Inject

class FetchContactDetails @Inject constructor(
    private val repository: ContactDetailsRepository,
    private val api: ProtonMailApiManager,
    private val mapper: FetchContactsMapper,
    private val dispatchers: DispatcherProvider
) {

    operator fun invoke(contactId: String): Flow<FetchContactDetailsResult> = flow {

        if (contactId.isBlank()) {
            throw IllegalArgumentException("Cannot fetch contact with an empty id")
        }

        Timber.v("Fetching contact data for $contactId")

        // fetch existing data from the DB
        val fullContact = try {
            repository.getFullContactDetails(contactId)
        } catch (tooBigException: SQLiteBlobTooBigException) {
            Timber.i(tooBigException, "Data too big to be fetched")
            null
        }
        fullContact?.let { fullDetailsFromDb ->
            val parsedContact = parseContactDetails(fullDetailsFromDb)
            Timber.v("Fetched existing Contacts Details $parsedContact")
            if (parsedContact != null) {
                emit(parsedContact)
            }
        }

        // fetch data from the server (refresh)
        val response = api.fetchContactDetails(contactId)
        val fetchedContact = response.contact
        repository.insertFullContactDetails(fetchedContact)
        val parsedContact = parseContactDetails(fetchedContact)
        Timber.v("Fetched new Contact Details $parsedContact")
        if (parsedContact != null) {
            emit(parsedContact)
        }
    }
        .flowOn(dispatchers.Io)

    private fun parseContactDetails(contact: FullContactDetails): FetchContactDetailsResult? =
        mapper.mapEncryptedDataToResult(contact.encryptedData)

}

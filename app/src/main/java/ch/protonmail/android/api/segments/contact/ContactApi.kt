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
package ch.protonmail.android.api.segments.contact

import androidx.annotation.WorkerThread
import ch.protonmail.android.api.models.ContactEmailsResponseV2
import ch.protonmail.android.api.models.ContactResponse
import ch.protonmail.android.api.models.ContactsDataResponse
import ch.protonmail.android.api.models.CreateContact
import ch.protonmail.android.api.models.CreateContactBody
import ch.protonmail.android.api.models.CreateContactV2BodyItem
import ch.protonmail.android.api.models.DeleteResponse
import ch.protonmail.android.api.models.IDList
import ch.protonmail.android.api.models.contacts.send.LabelContactsBody
import ch.protonmail.android.api.segments.BaseApi
import ch.protonmail.android.api.utils.ParseUtils
import ch.protonmail.android.data.local.model.FullContactDetailsResponse
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import java.io.IOException

class ContactApi(private val service: ContactService) : BaseApi(), ContactApiSpec {
    override suspend fun labelContacts(labelContactsBody: LabelContactsBody) =
        service.labelContacts(labelContactsBody)

    @Throws(IOException::class)
    override fun unlabelContactEmailsCompletable(labelContactsBody: LabelContactsBody): Completable =
        service.unlabelContactEmailsCompletable(labelContactsBody)

    override suspend fun unlabelContactEmails(labelContactsBody: LabelContactsBody) =
        service.unlabelContactEmails(labelContactsBody)

    override suspend fun fetchContacts(page: Int, pageSize: Int): ContactsDataResponse =
        service.contacts(page, pageSize)

    override suspend fun fetchContactEmails(page: Int, pageSize: Int): ContactEmailsResponseV2 =
        service.contactsEmails(page, pageSize)

    override fun fetchContactsEmailsByLabelId(page: Int, labelId: String): Observable<ContactEmailsResponseV2> =
        service.contactsEmailsByLabelId(page, labelId)

    @Throws(IOException::class)
    override fun fetchContactDetailsBlocking(contactId: String): FullContactDetailsResponse? =
        ParseUtils.parse(service.contactByIdBlocking(contactId).execute())

    override suspend fun fetchContactDetails(contactId: String): FullContactDetailsResponse =
        service.contactById(contactId)

    @WorkerThread
    @Throws(Exception::class)
    override fun fetchContactDetailsBlocking(contactIDs: Collection<String>): Map<String, FullContactDetailsResponse?> {
        if (contactIDs.isEmpty()) {
            return emptyMap()
        }
        val service = service
        val list = ArrayList(contactIDs)
        return executeAll(list.map { contactId -> service.contactByIdBlocking(contactId) })
            .mapIndexed { i, resp -> list[i] to resp }.toMap()
    }

    @Throws(IOException::class)
    override fun createContactBlocking(body: CreateContact): ContactResponse? {
        val createContactBody = CreateContactBody(listOf(body))
        return ParseUtils.parse(service.createContactBlocking(createContactBody).execute())
    }

    override suspend fun createContact(body: CreateContact): ContactResponse? {
        val createContactBody = CreateContactBody(listOf(body))
        return service.createContact(createContactBody)
    }

    @Throws(IOException::class)
    override fun updateContact(contactId: String, body: CreateContactV2BodyItem): FullContactDetailsResponse? =
        ParseUtils.parse(service.updateContact(contactId, body).execute())

    @Throws(IOException::class)
    override fun deleteContactSingle(contactIds: IDList): Single<DeleteResponse> =
        service.deleteContactSingle(contactIds)

    override suspend fun deleteContact(contactIds: IDList): DeleteResponse =
        service.deleteContact(contactIds)
}

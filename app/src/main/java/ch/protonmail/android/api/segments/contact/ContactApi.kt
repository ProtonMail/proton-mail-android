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

import androidx.annotation.WorkerThread
import ch.protonmail.android.api.models.ContactEmailsResponseV2
import ch.protonmail.android.api.models.ContactResponse
import ch.protonmail.android.api.models.ContactsDataResponse
import ch.protonmail.android.api.models.CreateContact
import ch.protonmail.android.api.models.CreateContactBody
import ch.protonmail.android.api.models.CreateContactV2BodyItem
import ch.protonmail.android.api.models.DeleteContactResponse
import ch.protonmail.android.api.models.IDList
import ch.protonmail.android.api.models.contacts.send.LabelContactsBody
import ch.protonmail.android.api.models.room.contacts.server.FullContactDetailsResponse
import ch.protonmail.android.api.segments.BaseApi
import ch.protonmail.android.api.utils.ParseUtils
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.Call
import java.io.IOException

class ContactApi(private val service: ContactService) : BaseApi(), ContactApiSpec {
    @Throws(IOException::class)
    override fun labelContacts(labelContactsBody: LabelContactsBody): Completable {
        return service.labelContacts(labelContactsBody)
    }

    @Throws(IOException::class)
    override fun unlabelContactEmailsCompletable(labelContactsBody: LabelContactsBody): Completable {
        return service.unlabelContactEmailsCompletable(labelContactsBody)
    }

    override suspend fun unlabelContactEmails(labelContactsBody: LabelContactsBody) =
        service.unlabelContactEmails(labelContactsBody)

    override suspend fun fetchContacts(page: Int, pageSize: Int): ContactsDataResponse =
        service.contacts(page, pageSize)

    @Throws(IOException::class)
    override fun fetchContactEmails(pageSize: Int): List<ContactEmailsResponseV2?> {
        val list = ArrayList<ContactEmailsResponseV2?>()
        val pendingRequests = ArrayList<Call<ContactEmailsResponseV2>>()
        val firstPage = service.contactsEmails(0, pageSize).execute().body()
        list.add(firstPage!!)
        for (i in 1..(firstPage.total + (pageSize - 1)) / pageSize) {
            pendingRequests.add(service.contactsEmails(i, pageSize))
        }
        list.addAll(executeAll(pendingRequests))
        return list
    }

    override fun fetchContactsEmailsByLabelId(page: Int, labelId: String): Observable<ContactEmailsResponseV2> {
        return service.contactsEmailsByLabelId(page, labelId)
    }

    @Throws(IOException::class)
    override fun fetchContactDetailsBlocking(contactId: String): FullContactDetailsResponse? {
        return ParseUtils.parse(service.contactByIdBlocking(contactId).execute())
    }

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
    override fun createContact(body: CreateContact): ContactResponse? {
        val contactList = ArrayList<CreateContact>()
        contactList.add(body)
        val createContactBody = CreateContactBody(contactList)
        return ParseUtils.parse(service.createContact(createContactBody).execute())
    }

    @Throws(IOException::class)
    override fun updateContact(contactId: String, body: CreateContactV2BodyItem): FullContactDetailsResponse? {
        return ParseUtils.parse(service.updateContact(contactId, body).execute())
    }

    @Throws(IOException::class)
    override fun deleteContactSingle(contactIds: IDList): Single<DeleteContactResponse> {
        return service.deleteContactSingle(contactIds)
    }

    override suspend fun deleteContact(contactIds: IDList): DeleteContactResponse =
        service.deleteContact(contactIds)
}

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

import ch.protonmail.android.api.models.ContactEmailsResponseV2
import ch.protonmail.android.api.models.ContactResponse
import ch.protonmail.android.api.models.ContactsDataResponse
import ch.protonmail.android.api.models.CreateContact
import ch.protonmail.android.api.models.CreateContactV2BodyItem
import ch.protonmail.android.api.models.DeleteContactResponse
import ch.protonmail.android.api.models.IDList
import ch.protonmail.android.api.models.contacts.send.LabelContactsBody
import ch.protonmail.android.api.models.room.contacts.server.FullContactDetailsResponse
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import java.io.IOException

interface ContactApiSpec {

    suspend fun fetchContacts(page: Int, pageSize: Int): ContactsDataResponse

    @Throws(IOException::class)
    fun fetchContactEmails(pageSize: Int): List<ContactEmailsResponseV2?>

    suspend fun fetchRawContactEmails(page: Int, pageSize: Int): ContactEmailsResponseV2

    @Throws(IOException::class)
    fun fetchContactsEmailsByLabelId(page: Int, labelId: String): Observable<ContactEmailsResponseV2>

    @Throws(IOException::class)
    fun fetchContactDetailsBlocking(contactId: String): FullContactDetailsResponse?

    suspend fun fetchContactDetails(contactId: String): FullContactDetailsResponse

    @Throws(Exception::class)
    fun fetchContactDetailsBlocking(contactIDs: Collection<String>): Map<String, FullContactDetailsResponse?>

    @Throws(IOException::class)
    fun createContactBlocking(body: CreateContact): ContactResponse?

    suspend fun createContact(body: CreateContact): ContactResponse?

    @Throws(IOException::class)
    fun updateContact(contactId: String, body: CreateContactV2BodyItem): FullContactDetailsResponse?

    @Throws(IOException::class)
    fun deleteContactSingle(contactIds: IDList): Single<DeleteContactResponse>

    suspend fun deleteContact(contactIds: IDList): DeleteContactResponse

    @Throws(IOException::class)
    fun labelContacts(labelContactsBody: LabelContactsBody): Completable

    @Throws(IOException::class)
    fun unlabelContactEmailsCompletable(labelContactsBody: LabelContactsBody): Completable

    suspend fun unlabelContactEmails(labelContactsBody: LabelContactsBody)
}

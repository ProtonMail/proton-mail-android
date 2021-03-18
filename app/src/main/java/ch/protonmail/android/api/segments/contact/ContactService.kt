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
import ch.protonmail.android.api.models.CreateContactBody
import ch.protonmail.android.api.models.CreateContactV2BodyItem
import ch.protonmail.android.api.models.DeleteContactResponse
import ch.protonmail.android.api.models.IDList
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.models.contacts.send.LabelContactsBody
import ch.protonmail.android.api.segments.RetrofitConstants.ACCEPT_HEADER_V1
import ch.protonmail.android.api.segments.RetrofitConstants.CONTENT_TYPE
import ch.protonmail.android.data.local.model.FullContactDetailsResponse
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ContactService {

    @GET("contacts")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    suspend fun contacts(@Query("Page") page: Int, @Query("PageSize") pageSize: Int): ContactsDataResponse

    @GET("contacts/emails")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun contactsEmailsCall(@Query("Page") page: Int, @Query("PageSize") pageSize: Int): Call<ContactEmailsResponseV2>

    @GET("contacts/emails")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    suspend fun contactsEmails(@Query("Page") page: Int, @Query("PageSize") pageSize: Int): ContactEmailsResponseV2

    @GET("contacts/emails")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun contactsEmailsByLabelId(@Query("Page") page: Int, @Query("LabelID") labelId: String): Observable<ContactEmailsResponseV2>

    @GET("contacts/{contact_id}")
    fun contactByIdBlocking(@Path("contact_id") contactId: String): Call<FullContactDetailsResponse>

    @GET("contacts/{contact_id}")
    suspend fun contactById(@Path("contact_id") contactId: String): FullContactDetailsResponse

    @POST("contacts")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun createContactBlocking(@Body contactsBody: CreateContactBody): Call<ContactResponse>

    @POST("contacts")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    suspend fun createContact(@Body contactsBody: CreateContactBody): ContactResponse

    @PUT("contacts/{contact_id}")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun updateContact(@Path("contact_id") contactId: String, @Body contact: CreateContactV2BodyItem): Call<FullContactDetailsResponse>

    @PUT("contacts/delete")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun deleteContactSingle(@Body contactId: IDList): Single<DeleteContactResponse>

    @PUT("contacts/delete")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    suspend fun deleteContact(@Body contactId: IDList): DeleteContactResponse

    @DELETE("contacts")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun clearContacts(): Call<ResponseBody>

    @PUT("contacts/emails/label")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun labelContacts(@Body labelContactsBody: LabelContactsBody): Completable

    @PUT("contacts/emails/unlabel")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun unlabelContactEmailsCompletable(@Body labelContactsBody: LabelContactsBody): Completable

    @PUT("contacts/emails/unlabel")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    suspend fun unlabelContactEmails(@Body labelContactsBody: LabelContactsBody)
}

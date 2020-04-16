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
package ch.protonmail.android.api.segments.label

import ch.protonmail.android.api.interceptors.RetrofitTag
import ch.protonmail.android.api.models.LabelBody
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.models.contacts.receive.ContactGroupsResponse
import ch.protonmail.android.api.models.messages.receive.LabelResponse
import ch.protonmail.android.api.models.messages.receive.LabelsResponse
import ch.protonmail.android.api.segments.RetrofitConstants.ACCEPT_HEADER_V1
import ch.protonmail.android.api.segments.RetrofitConstants.CONTENT_TYPE
import ch.protonmail.android.api.utils.Fields
import ch.protonmail.android.core.Constants
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import retrofit2.Call
import retrofit2.http.*

// region constants
private const val PATH_LABEL_ID = "label_id"
// endregion

interface LabelService {

    @GET("labels?" + Fields.Label.TYPE + "=" + Constants.LABEL_TYPE_MESSAGE)
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun fetchLabels(@Tag retrofitTag: RetrofitTag): Call<LabelsResponse>

    // this is coded here and not passed as a param because there is no point when it is a constant always
    @GET("labels?" + Fields.Label.TYPE + "=" + Constants.LABEL_TYPE_CONTACT_GROUPS)
    fun fetchContactGroups(): Single<ContactGroupsResponse>

    @GET("labels?" + Fields.Label.TYPE + "=" + Constants.LABEL_TYPE_CONTACT_GROUPS)
    fun fetchContactGroupsAsObservable(): Observable<ContactGroupsResponse>

    @POST("labels")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun createLabel(@Body label: LabelBody): Call<LabelResponse>

    @POST("labels")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun createLabelCompletable(@Body label: LabelBody): Single<LabelResponse>

    @PUT("labels/{$PATH_LABEL_ID}")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun updateLabel(@Path(PATH_LABEL_ID) labelId: String, @Body label: LabelBody): Call<LabelResponse>

    @PUT("labels/{$PATH_LABEL_ID}")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun updateLabelCompletable(@Path(PATH_LABEL_ID) labelId: String, @Body label: LabelBody): Completable

    @DELETE("labels/{$PATH_LABEL_ID}")
    @Headers(CONTENT_TYPE, ACCEPT_HEADER_V1)
    fun deleteLabel(@Path(PATH_LABEL_ID) labelId: String): Single<ResponseBody>


}

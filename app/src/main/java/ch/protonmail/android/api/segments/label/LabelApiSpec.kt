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
import ch.protonmail.android.api.models.room.contacts.ContactLabel
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import java.io.IOException

interface LabelApiSpec {

    @Throws(IOException::class)
    fun fetchLabels(retrofitTag: RetrofitTag): LabelsResponse

    @Throws(IOException::class)
    fun fetchContactGroups(): Single<ContactGroupsResponse>

    @Throws(IOException::class)
    fun fetchContactGroupsAsObservable(): Observable<List<ContactLabel>>

    @Throws(IOException::class)
    fun createLabel(label: LabelBody): LabelResponse

    @Throws(IOException::class)
    fun createLabelCompletable(label: LabelBody): Single<ContactLabel>

    @Throws(IOException::class)
    fun updateLabel(labelId: String, label: LabelBody): LabelResponse

    @Throws(IOException::class)
    fun updateLabelCompletable(labelId: String, label: LabelBody): Completable

    @Throws(IOException::class)
    fun deleteLabel(labelId: String): Single<ResponseBody>
}
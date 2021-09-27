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
import ch.protonmail.android.api.segments.BaseApi
import ch.protonmail.android.api.utils.ParseUtils
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import java.io.IOException

class LabelApi (private val service : LabelService) : BaseApi(), LabelApiSpec {

    @Throws(IOException::class)
    override fun fetchLabels(retrofitTag: RetrofitTag): LabelsResponse {
        return ParseUtils.parse(service.fetchLabels(retrofitTag).execute())
    }

    @Throws(IOException::class)
    override fun fetchContactGroups(): Single<ContactGroupsResponse> {
        return service.fetchContactGroups().doOnError {
            ParseUtils.doOnError(it)
        }
    }

    @Throws(IOException::class)
    override fun fetchContactGroupsAsObservable(): Observable<List<ContactLabel>> {
        return service.fetchContactGroupsAsObservable().map { t: ContactGroupsResponse -> t.contactGroups }
                .doOnError {
                    ParseUtils.doOnError(it)
                }
    }

    override suspend fun fetchContactGroupsList(): List<ContactLabel> {
        return service.fetchContactGroupsList().contactGroups
    }

    @Throws(IOException::class)
    override fun createLabel(label: LabelBody): LabelResponse {
        return ParseUtils.parse(service.createLabel(label).execute())
    }

    override fun createLabelCompletable(label: LabelBody): Single<ContactLabel> {
        return service.createLabelCompletable(label).map {
            t: LabelResponse -> t.contactGroup
        }.doOnError {
            ParseUtils.doOnError(it)
        }
    }

    @Throws(IOException::class)
    override fun updateLabel(labelId: String, label: LabelBody): LabelResponse {
        return ParseUtils.parse(service.updateLabel(labelId, label).execute())
    }

    override fun updateLabelCompletable(labelId: String, label: LabelBody): Completable {
        return service.updateLabelCompletable(labelId, label).doOnError {
            ParseUtils.doOnError(it)
        }
    }

    @Throws(IOException::class)
    override fun deleteLabelSingle(labelId: String): Single<ResponseBody> {
        return service.deleteLabelSingle(labelId).doOnError {
            ParseUtils.doOnError(it)
        }
    }

    override suspend fun deleteLabel(labelId: String): ResponseBody = service.deleteLabel(labelId)

}

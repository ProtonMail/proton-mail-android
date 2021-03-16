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
package ch.protonmail.android.api.segments.organization

import ch.protonmail.android.api.models.CreateOrganizationBody
import ch.protonmail.android.api.models.Keys
import ch.protonmail.android.api.models.OrganizationResponse
import ch.protonmail.android.api.segments.BaseApi
import ch.protonmail.android.api.utils.ParseUtils
import java.io.IOException

class OrganizationApi(private val service: OrganizationService) : BaseApi(), OrganizationApiSpec {

    @Throws(IOException::class)
    override fun fetchOrganization(): OrganizationResponse =
        ParseUtils.parse(service.fetchOrganization().execute())

    @Throws(IOException::class)
    override fun fetchOrganizationKeys(): Keys =
        ParseUtils.parse(service.fetchOrganizationsKeys().execute())

    @Throws(IOException::class)
    override fun createOrganization(body: CreateOrganizationBody): OrganizationResponse? =
        ParseUtils.parse(service.createOrganization(body).execute())
}

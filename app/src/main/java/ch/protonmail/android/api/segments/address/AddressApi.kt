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
package ch.protonmail.android.api.segments.address

import ch.protonmail.android.api.interceptors.RetrofitTag
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.models.address.*
import ch.protonmail.android.api.segments.BaseApi
import ch.protonmail.android.api.utils.ParseUtils
import java.io.IOException

class AddressApi (val service: AddressService) : BaseApi(), AddressApiSpec {

    @Throws(IOException::class)
    override fun fetchAddresses() : AddressesResponse =
        ParseUtils.parse(service.fetchAddresses().execute())

    @Throws(IOException::class)
    override fun fetchAddresses(username : String) : AddressesResponse =
        ParseUtils.parse(service.fetchAddresses(RetrofitTag(username)).execute())

    @Throws(IOException::class)
    override fun updateAlias(addressIds: List<String>): ResponseBody =
        ParseUtils.parse(service.updateAddressOrder(AddressOrder(addressIds)).execute())

    @Throws(IOException::class)
    override fun setupAddress(addressSetupBody: AddressSetupBody): AddressSetupResponse =
        ParseUtils.parse(service.setupAddress(addressSetupBody).execute())

    @Throws(IOException::class)
    override fun editAddress(addressId: String, displayName: String, signature: String): ResponseBody =
        ParseUtils.parse(service.editAddress(addressId, CondensedAddress(displayName, signature)).execute())
}
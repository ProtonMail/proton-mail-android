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

package ch.protonmail.android.testdata

import ch.protonmail.android.domain.entity.EmailAddress
import ch.protonmail.android.domain.entity.user.Address
import ch.protonmail.android.domain.entity.user.AddressKeys
import ch.protonmail.android.domain.entity.user.Addresses
import me.proton.core.user.domain.entity.AddressId

object AddressesTestData {

    val idsToRawAddresses = listOf(
        "addressId0" to "address0@proton.me",
        "addressId1" to "address1@proton.me",
        "addressId2" to "address2@proton.me",
        "addressId3" to "address3@proton.me"
    )

    fun from(idsToAddresses: List<Pair<String, String>>): Addresses {
        val addressMap = idsToAddresses.toOrderAddressMap()
        return Addresses(addressMap)
    }

    private fun List<Pair<String, String>>.toOrderAddressMap() = mapIndexed { index, idToAddress ->
        val address = Address(
            id = AddressId(idToAddress.first),
            email = EmailAddress(idToAddress.second),
            enabled = true,
            type = Address.Type.ORIGINAL,
            allowedToSend = true,
            allowedToReceive = true,
            keys = AddressKeys(primaryKey = null, keys = emptyList()),
            domainId = null,
            displayName = null,
            signature = null
        )
        index to address
    }.toMap()
}

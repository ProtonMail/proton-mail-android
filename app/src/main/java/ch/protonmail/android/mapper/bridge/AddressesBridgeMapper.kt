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
package ch.protonmail.android.mapper.bridge

import ch.protonmail.android.domain.entity.EmailAddress
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.domain.entity.user.Address
import ch.protonmail.android.domain.entity.user.Addresses
import me.proton.core.util.kotlin.invoke
import me.proton.core.util.kotlin.takeIfNotBlank
import me.proton.core.util.kotlin.toBoolean
import javax.inject.Inject
import ch.protonmail.android.api.models.address.Address as OldAddress

/**
 * Transforms a collection of [ch.protonmail.android.api.models.address.Address] to
 * [ch.protonmail.android.domain.entity.user.Addresses]
 * Inherit from [BridgeMapper]
 */
class AddressesBridgeMapper @Inject constructor(
    private val singleMapper: AddressBridgeMapper
) : BridgeMapper<Collection<OldAddress>, Addresses> {

    override fun Collection<OldAddress>.toNewModel() =
        Addresses(mapWithFixedOrder(singleMapper) { it.toNewModel() })

    @OptIn(ExperimentalStdlibApi::class)
    private fun <T> Collection<OldAddress>.mapWithFixedOrder(
        mapper: AddressBridgeMapper,
        map: AddressBridgeMapper.(OldAddress) -> T
    ): Map<Int, T> {
        val sortedOriginals = sortedBy { it.order }

        // Keys is new order
        val fixedOrders = buildMap<Int, OldAddress> {
            for (original in sortedOriginals) {
                var newOrder = original.order
                while (newOrder in keys) newOrder++
                put(newOrder, original)
            }
        }

        return fixedOrders.mapValues { (_, original) -> mapper { map(original) } }
    }
}

/**
 * Transforms a single of [ch.protonmail.android.api.models.address.Address] to
 * [ch.protonmail.android.domain.entity.user.Address]
 * Inherit from [BridgeMapper]
 */
class AddressBridgeMapper @Inject constructor(
    private val keysMapper: AddressKeysBridgeMapper
) : BridgeMapper<OldAddress, Address> {

    override fun OldAddress.toNewModel(): Address {

        return Address(
            id = Id(id),
            domainId = Id(domainId),
            email = EmailAddress(email),
            displayName = displayName?.takeIfNotBlank()?.let(::Name),
            enabled = status.toBoolean(),
            type = getType(type),
            allowedToSend = send.toBoolean(),
            allowedToReceive = receive.toBoolean(),
            keys = keysMapper { keys.toNewModel() }
        )
    }

    private fun getType(i: Int) =
        Address.Type.values().find { it.i == i }
            ?: throw IllegalArgumentException("Cannot crate Address Type from value: $i")
}

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
package ch.protonmail.android.domain.entity.user

import ch.protonmail.android.domain.entity.EmailAddress
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.domain.entity.NotBlankString
import ch.protonmail.android.domain.entity.Validated
import me.proton.core.user.domain.entity.AddressId

/**
 * Representation of an user's address
 * @author Davide Farella
 */
@Validated
data class Address(
    val id: AddressId,
    val domainId: String?,
    val email: EmailAddress,
    val displayName: Name?,
    val signature: NotBlankString?,
    val enabled: Boolean,
    val type: Type,
    val allowedToSend: Boolean,
    val allowedToReceive: Boolean,
    val keys: AddressKeys
) {

    enum class Type(val i: Int) {
        /**
         * First address the user created using a PM domain
         */
        ORIGINAL(1),

        /**
         * Subsequent addresses created using a PM domain
         */
        ALIAS(2),

        /**
         * Custom domain address
         */
        CUSTOM(3),

        /**
         * pm.me
         */
        PREMIUM(4),

        EXTERNAL(5)
    }
}

/**
 * An ordered set of [Address]s
 */
@Validated
data class Addresses(
    /**
     * [Map.Entry.key] is the 'Order', Address with lower Order is the primary one
     */
    val addresses: Map<Int, Address>
) {

    /**
     * @return [Address] with lower `Order` ( entry.key )
     *   `null` if there are no addresses
     */
    val primary get() = addresses.minByOrNull { it.key }?.value
    val hasAddresses get() = addresses.isNotEmpty()

    /**
     * @return [Address] matching the given [addressId]
     */
    fun findBy(addressId: AddressId): Address? =
        addresses.values.find { it.id == addressId }

    /**
     * @return [List] of [Address] sorted by their 'Order'
     */
    fun sorted() = addresses.toSortedMap().values.toList()
}

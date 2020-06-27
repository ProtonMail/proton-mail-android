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
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.domain.entity.Validable
import ch.protonmail.android.domain.entity.Validated
import ch.protonmail.android.domain.entity.Validator
import ch.protonmail.android.domain.entity.requireValid

/**
 * Representation of an user's address
 * @author Davide Farella
 */
@Validated
data class Address(
    val id: Id,
    val domainId: Id,
    val email: EmailAddress,
    val displayName: Name?,
    val keys: AddressKeys
)

/**
 * An ordered set of [Address]s with a primary one
 *
 * @param primaryAddress can be `null`, as is possible that the [User] didn't set up its email address ( VPN user )
 * @param addresses can be empty only if [primaryAddress] is `null`.
 *   first value must be [primaryAddress]
 */
@Validated
data class Addresses(
    val primaryAddress: Address?,
    val addresses: List<Address>
) : Validable by Validator<Addresses>({
    primaryAddress == null && addresses.isEmpty() ||
        primaryAddress == addresses.firstOrNull()
}) {
    init { requireValid() }

    val hasAddresses get() = addresses.isNotEmpty()
}

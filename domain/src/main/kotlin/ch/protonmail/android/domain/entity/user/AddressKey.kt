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

import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Validable
import ch.protonmail.android.domain.entity.Validated
import ch.protonmail.android.domain.entity.Validator
import ch.protonmail.android.domain.entity.requireValid

// It is possible for an address to not have any key

/**
 * Representation of an user's address' Key
 * @author Davide Farella
 */
@Validated
data class AddressKey(
    val id: Id
)

/**
 * A set of [AddressKey]s with a primary one
 * [Validable]: [keys] must contains [primaryKey], if not `null`
 *
 * @param primaryKey can be `null`, as an [Address] is not required to have keys
 * @param keys can be empty only if [primaryKey] is `null`
 */
@Validated
data class AddressKeys(
    val primaryKey: AddressKey?,
    val keys: Collection<AddressKey>
) : Validable by Validator<AddressKeys>({
    primaryKey == null && keys.isEmpty() ||
        primaryKey in keys
}) {
    init { requireValid() }

    val hasKeys get() = keys.isNotEmpty()
}

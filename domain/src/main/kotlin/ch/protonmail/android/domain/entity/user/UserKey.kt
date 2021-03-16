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
import ch.protonmail.android.domain.entity.PgpField
import ch.protonmail.android.domain.entity.Validable
import ch.protonmail.android.domain.entity.Validated
import ch.protonmail.android.domain.entity.Validator
import ch.protonmail.android.domain.entity.requireValid

/**
 * Representation of an user's Key
 * @author Davide Farella
 */
@Validated
data class UserKey(
    val id: Id,
    val version: UInt,
    val privateKey: PgpField.PrivateKey,
    val token: PgpField.Message?
)

/**
 * [User]s key ring, there can be zero if the users has not set up their mail address yet (i.e. VPN users).
 * There can be multiple if the user has done a password reset
 *
 * @param primaryKey can be `null`, as an [Address] is not required to have keys
 * @param keys can be empty only if [primaryKey] is `null`
 */
@Validated
data class UserKeys(
    val primaryKey: UserKey?,
    val keys: Collection<UserKey>
) : Validable by Validator<UserKeys>({
        require(
            primaryKey == null && keys.isEmpty() ||
                primaryKey in keys
        )
    }) {
    init { requireValid() }

    val hasKeys get() = keys.isNotEmpty()

    companion object {

        /**
         * Empty [UserKeys]
         */
        val Empty get() = UserKeys(null, emptySet())
    }
}

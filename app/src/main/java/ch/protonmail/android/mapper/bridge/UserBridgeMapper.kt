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

import ch.protonmail.android.domain.entity.Bytes
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.domain.entity.NotBlankString
import ch.protonmail.android.domain.entity.user.Addresses
import ch.protonmail.android.domain.entity.user.Delinquent
import ch.protonmail.android.domain.entity.user.Role
import ch.protonmail.android.domain.entity.user.User
import ch.protonmail.android.domain.entity.user.UserKeys
import ch.protonmail.android.domain.entity.user.UserSpace
import ch.protonmail.android.api.models.User as OldUser

/**
 * Transforms [ch.protonmail.android.api.models.User] to [ch.protonmail.android.domain.entity.user.User]
 */
class UserBridgeMapper : BridgeMapper<OldUser, User> {

    // STOPSHIP FIX DUMMY FIELDS!!!
    override fun OldUser.toNewModel(): User {

        return User(
            id = Id("id"), // TODO
            name = Name(name),
            addresses = Addresses(emptyMap()), // TODO
            keys = UserKeys(null, emptyList()), // TODO
            plans = emptySet(), // TODO
            private = false, // TODO
            role = Role.values().first { it.i == role },
            organizationPrivateKey = null, // TODO
            currency = NotBlankString("eur"), // TODO
            credits = 0, // TODO
            delinquent = Delinquent.None, //TODO
            totalUploadLimit = Bytes(0u), //TODO
            dedicatedSpace = UserSpace(Bytes(0u), Bytes(0u)) // TODO
        )
    }
}

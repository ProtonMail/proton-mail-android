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

package ch.protonmail.android.domain.testDoubles

import ch.protonmail.android.domain.entity.Credential
import ch.protonmail.android.domain.entity.EmailAddress
import ch.protonmail.android.domain.repository.CredentialRepository

class FakeCredentialsRepository : CredentialRepository {

    private val map = mutableMapOf<EmailAddress, Credential>()

    override fun getAll() = map

    override fun get(address: EmailAddress) = map.getOrDefault(address, Credential.NotFound)

    override fun set(address: EmailAddress, credential: Credential) {
        map[address] = credential
    }

    override fun minusAssign(address: EmailAddress) {
        map -= address
    }

}

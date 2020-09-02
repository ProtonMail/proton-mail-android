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

package ch.protonmail.android.credentials

import android.content.SharedPreferences
import ch.protonmail.android.domain.entity.Credential
import ch.protonmail.android.domain.entity.Credential.NotFound
import ch.protonmail.android.domain.entity.EmailAddress
import ch.protonmail.android.domain.repository.CredentialRepository
import me.proton.core.util.android.sharedpreferences.get
import me.proton.core.util.android.sharedpreferences.minusAssign
import me.proton.core.util.android.sharedpreferences.set
import me.proton.core.util.kotlin.deserialize

internal class SharedPreferencesCredentialRepository(
    private val preferences: SharedPreferences
) : CredentialRepository {

    override fun getAll(): Map<EmailAddress, Credential> =
        preferences.all.map { (k, v) ->
            EmailAddress(k) to (v as String).deserialize(Credential.serializer())
        }.toMap()

    override operator fun get(address: EmailAddress): Credential =
        preferences[address.s] ?: NotFound

    override operator fun set(address: EmailAddress, credential: Credential) {
        require(credential != NotFound) { "Cannot set 'NotFound' as credential" }
        preferences[address.s] = credential
    }

    override operator fun minusAssign(address: EmailAddress) {
        preferences -= address.s
    }
}

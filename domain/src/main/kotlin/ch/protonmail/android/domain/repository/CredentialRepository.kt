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

package ch.protonmail.android.domain.repository

import ch.protonmail.android.domain.entity.Credential
import ch.protonmail.android.domain.entity.EmailAddress

interface CredentialRepository {

    /**
     * @return all the [Credential]s stored, associated with its [EmailAddress]
     */
    fun getAll(): Map<EmailAddress, Credential>

    /**
     * @return [Credential] stored for a single [EmailAddress]
     */
    operator fun get(address: EmailAddress): Credential

    /**
     * Store [Credential] for given [EmailAddress]
     */
    operator fun set(address: EmailAddress, credential: Credential)

    /**
     * Remove a [Credential] stored for given [EmailAddress]
     */
    operator fun minusAssign(address: EmailAddress)
}

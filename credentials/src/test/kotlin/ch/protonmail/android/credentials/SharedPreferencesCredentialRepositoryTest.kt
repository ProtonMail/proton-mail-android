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

import assert4k.assert
import assert4k.equals
import assert4k.fails
import assert4k.that
import assert4k.with
import ch.protonmail.android.domain.entity.Credential.FullyLoggedIn
import ch.protonmail.android.domain.entity.Credential.LoggedOut
import ch.protonmail.android.domain.entity.Credential.MailboxPasswordRequired
import ch.protonmail.android.domain.entity.Credential.NotFound
import ch.protonmail.android.domain.entity.EmailAddress
import me.proton.core.test.android.mocks.newMockSharedPreferences
import kotlin.test.Test

class SharedPreferencesCredentialRepositoryTest {

    private val credentials = SharedPreferencesCredentialRepository(newMockSharedPreferences)

    @Test
    fun `returns empty map if no credentials are stored`() {
        assert that credentials.getAll() equals emptyMap()
    }

    @Test
    fun `returns correctly all the credentials stored`() {
        val first = EmailAddress("first@email.com")
        val second = EmailAddress("second@email.com")
        val third = EmailAddress("third@email.com")

        credentials[first] = FullyLoggedIn
        credentials[second] = MailboxPasswordRequired
        credentials[third] = LoggedOut

        assert that credentials.getAll() equals mapOf(
            first to FullyLoggedIn,
            second to MailboxPasswordRequired,
            third to LoggedOut
        )
    }

    @Test
    fun `returns NotFound if no credentials matching is found`() {
        assert that credentials[EmailAddress("something@email.com")] equals NotFound
    }

    @Test
    fun `throws exception if trying to store NotFound`() {
        assert that fails<IllegalArgumentException> {
            credentials[EmailAddress("something@email.com")] = NotFound
        } with "Cannot set 'NotFound' as credential"
    }

    @Test
    fun `correctly updates credentials`() {
        val address = EmailAddress("something@email.com")
        credentials[address] = LoggedOut
        assert that credentials[address] equals LoggedOut

        credentials[address] = MailboxPasswordRequired
        assert that credentials[address] equals MailboxPasswordRequired
    }

    @Test
    fun `correctly remove credentials`() {
        val address = EmailAddress("something@email.com")
        credentials[address] = LoggedOut
        assert that credentials[address] equals LoggedOut

        credentials -= address
        assert that credentials[address] equals NotFound
    }
}

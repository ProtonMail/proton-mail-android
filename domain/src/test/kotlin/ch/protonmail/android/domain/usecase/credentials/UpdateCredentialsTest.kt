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

package ch.protonmail.android.domain.usecase.credentials

import assert4k.assert
import assert4k.equals
import assert4k.that
import ch.protonmail.android.domain.entity.Credential
import ch.protonmail.android.domain.repository.CredentialRepository
import ch.protonmail.android.domain.testDoubles.FakeCredentialsRepository
import ch.protonmail.android.domain.testDoubles.SomeEmailAddress
import me.proton.core.test.kotlin.CoroutinesTest
import kotlin.test.Test

class UpdateCredentialsTest : CoroutinesTest {

    private val repo: CredentialRepository = FakeCredentialsRepository()
    private val updateCredentials = UpdateCredentials(dispatchers, repository = repo)

    @Test
    fun `can update existing credentials`() = coroutinesTest {

        // Given
        repo[SomeEmailAddress] = Credential.MailboxPasswordRequired

        // When
        updateCredentials(SomeEmailAddress, Credential.FullyLoggedIn)

        // Then
        assert that repo[SomeEmailAddress] equals Credential.FullyLoggedIn
    }

    @Test
    fun `can create credentials if none is found`() = coroutinesTest {

        // Given
        // * No saved credentials *

        // When
        updateCredentials(SomeEmailAddress, Credential.FullyLoggedIn)

        // Then
        assert that repo[SomeEmailAddress] equals Credential.FullyLoggedIn
    }

}

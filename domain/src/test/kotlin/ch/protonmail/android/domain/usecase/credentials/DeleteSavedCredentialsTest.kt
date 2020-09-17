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

class DeleteSavedCredentialsTest : CoroutinesTest {

    private val repo: CredentialRepository = FakeCredentialsRepository()
    private val deleteSavedCredentials = DeleteSavedCredentials(dispatchers, repository = repo)

    @Test
    fun `can delete existing credentials`() = coroutinesTest {

        // Given
        repo[SomeEmailAddress] = Credential.FullyLoggedIn

        // When
        deleteSavedCredentials(SomeEmailAddress)

        // Then
        assert that repo[SomeEmailAddress] equals Credential.NotFound
    }

    @Test
    fun `does nothing if credentials are not found`() = coroutinesTest {

        // Given
        // * No saved credentials *

        // When
        deleteSavedCredentials(SomeEmailAddress)

        // Then
        assert that repo[SomeEmailAddress] equals Credential.NotFound
    }

}

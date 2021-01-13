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

package ch.protonmail.android.usecase.fetch

import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.PublicKeyBody
import ch.protonmail.android.api.models.PublicKeyResponse
import ch.protonmail.android.api.models.enumerations.KeyFlag
import ch.protonmail.android.core.Constants
import ch.protonmail.android.usecase.model.FetchPublicKeysRequest
import ch.protonmail.android.usecase.model.FetchPublicKeysResult
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class FetchPublicKeysTest {

    @MockK
    private lateinit var api: ProtonMailApiManager

    private lateinit var fetchPublicKeys: FetchPublicKeys

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        fetchPublicKeys = FetchPublicKeys(api, TestDispatcherProvider)
    }

    @Test
    fun verifyThatEmailKeysAreFetchedCorrectlyWithAllKeysHavingEncryptionFlagSet() = runBlockingTest {
        // given
        val location = Constants.RecipientLocationType.TO
        val email = "email1@proto.com"
        val email2 = "email2@proto.com"
        val emailsList = listOf(email, email2)
        val publicKey = "publicKey1"
        val publicKey2 = "publicKey2"
        val publicKeyBody = PublicKeyBody(KeyFlag.ENCRYPTION_ENABLED.value, publicKey)
        val publicKeyBody2 = PublicKeyBody(KeyFlag.ENCRYPTION_ENABLED.value, publicKey2)
        val publicKeyResponse = PublicKeyResponse(
            0,
            "mimeType",
            arrayOf(publicKeyBody)
        )
        val publicKeyResponse2 = PublicKeyResponse(
            1,
            "mimeType",
            arrayOf(publicKeyBody2)
        )
        coEvery { api.getPublicKeys(email) } returns publicKeyResponse
        coEvery { api.getPublicKeys(email2) } returns publicKeyResponse2
        val expected = listOf(
            FetchPublicKeysResult(
                mapOf(email to publicKey, email2 to publicKey2),
                location
            )
        )

        // when
        val result = fetchPublicKeys.invoke(listOf(FetchPublicKeysRequest(emailsList, location)))

        // then
        assertNotNull(result)
        assertEquals(expected, result)
    }

    @Test
    fun verifyThatEmailKeysAreFetchedCorrectlyWithJustOneKeyHavingEncryptionFlagSet() = runBlockingTest {
        // given
        val location = Constants.RecipientLocationType.TO
        val email = "email1@proto.com"
        val email2 = "email2@proto.com"
        val emailsList = listOf(email, email2)
        val publicKey = "publicKey1"
        val publicKey2 = "publicKey2"
        val publicKeyBody = PublicKeyBody(KeyFlag.ENCRYPTION_ENABLED.value, publicKey)
        val publicKeyBody2 = PublicKeyBody(0, publicKey2)
        val publicKeyResponse = PublicKeyResponse(
            0,
            "mimeType",
            arrayOf(publicKeyBody)
        )
        val publicKeyResponse2 = PublicKeyResponse(
            1,
            "mimeType",
            arrayOf(publicKeyBody2)
        )
        coEvery { api.getPublicKeys(email) } returns publicKeyResponse
        coEvery { api.getPublicKeys(email2) } returns publicKeyResponse2
        val expected = listOf(
            FetchPublicKeysResult(
                mapOf(email to publicKey, email2 to ""),
                location
            )
        )

        // when
        val result = fetchPublicKeys.invoke(listOf(FetchPublicKeysRequest(emailsList, location)))

        // then
        assertNotNull(result)
        assertEquals(expected, result)
    }

    @Test
    fun verifyThatNoEmailKeysAreEmittedWhenConnectionErrorOccurs() = runBlockingTest {
        // given
        val location = Constants.RecipientLocationType.TO
        val email = "email1@proto.com"
        val email2 = "email2@proto.com"
        val emailsList = listOf(email, email2)
        val exception = Exception("An error occurred!")
        coEvery { api.getPublicKeys(email) } throws exception
        val expected = emptyList<FetchPublicKeysResult>()

        // when
        val result = fetchPublicKeys.invoke(listOf(FetchPublicKeysRequest(emailsList, location)))

        // then
        assertNotNull(result)
        assertEquals(expected, result)
    }
}

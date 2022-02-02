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
import ch.protonmail.android.core.Constants.RecipientLocationType
import ch.protonmail.android.usecase.model.FetchPublicKeysRequest
import ch.protonmail.android.usecase.model.FetchPublicKeysResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import me.proton.core.util.kotlin.EMPTY_STRING
import kotlin.test.Test
import kotlin.test.assertEquals

class FetchPublicKeysTest {

    private val api: ProtonMailApiManager = mockk()

    private val fetchPublicKeys = FetchPublicKeys(api, TestDispatcherProvider)

    @Test
    fun `email keys are fetched correctly with all keys having encryption flag set`() = runBlockingTest {
        // given
        val publicKeyResponse1 = buildKeyResponse(PUBLIC_KEY_1, hasEncryptionFlag = true)
        val publicKeyResponse2 = buildKeyResponse(PUBLIC_KEY_2, hasEncryptionFlag = true)

        coEvery { api.getPublicKeys(EMAIL_1) } returns publicKeyResponse1
        coEvery { api.getPublicKeys(EMAIL_2) } returns publicKeyResponse2

        val expected = listOf(
            buildKeySuccessResult(EMAIL_1, PUBLIC_KEY_1,),
            buildKeySuccessResult(EMAIL_2, PUBLIC_KEY_2)
        )

        // when
        val result = fetchPublicKeys(buildKeyRequests(listOf(EMAIL_1, EMAIL_2)))

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `email keys are fetched correctly with just one key having encryption flag set`() = runBlockingTest {
        // given
        val publicKeyResponse1 = buildKeyResponse(PUBLIC_KEY_1, hasEncryptionFlag = true)
        val publicKeyResponse2 = buildKeyResponse(PUBLIC_KEY_2, hasEncryptionFlag = false)

        coEvery { api.getPublicKeys(EMAIL_1) } returns publicKeyResponse1
        coEvery { api.getPublicKeys(EMAIL_2) } returns publicKeyResponse2

        val expected = listOf(
            buildKeySuccessResult(EMAIL_1, PUBLIC_KEY_1,),
            buildKeySuccessResult(EMAIL_2, EMPTY_STRING)
        )

        // when
        val result = fetchPublicKeys(buildKeyRequests(listOf(EMAIL_1, EMAIL_2)))

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `return correct results with Success and Error when connection error occurs for some keys`() = runBlockingTest {
        // given
        val publicKeyResponse1 = buildKeyResponse(PUBLIC_KEY_1, hasEncryptionFlag = true)

        coEvery { api.getPublicKeys(EMAIL_1) } returns publicKeyResponse1
        coEvery { api.getPublicKeys(EMAIL_2) } throws Exception("An error occurred!")

        val expected = listOf(
            buildKeySuccessResult(EMAIL_1, PUBLIC_KEY_1),
            buildKeyErrorResult(EMAIL_2)
        )

        // when
        val result = fetchPublicKeys(buildKeyRequests(listOf(EMAIL_1, EMAIL_2)))

        // then
        assertEquals(expected, result)
    }

    private companion object TestData {

        const val EMAIL_1 = "email1@pm.me"
        const val EMAIL_2 = "email2@pm.me"

        const val PUBLIC_KEY_1 = "publicKey1"
        const val PUBLIC_KEY_2 = "publicKey2"

        fun buildKeyRequests(
            emails: List<String>,
            location: RecipientLocationType = RecipientLocationType.TO
        ) = listOf(FetchPublicKeysRequest(emails, location))

        fun buildKeySuccessResult(
            email: String,
            key: String,
            location: RecipientLocationType = RecipientLocationType.TO
        ) = FetchPublicKeysResult.Success(email, key, location)

        fun buildKeyErrorResult(
            email: String,
            location: RecipientLocationType = RecipientLocationType.TO,
            error: String = "error"
        ) = FetchPublicKeysResult.Error(email, location, error)

        fun buildKeyBody(key: String, hasEncryptionFlag: Boolean) = PublicKeyBody(
            if (hasEncryptionFlag) KeyFlag.ENCRYPTION_ENABLED.value else 0,
            key
        )

        fun buildKeyResponse(key: String, hasEncryptionFlag: Boolean) = PublicKeyResponse(
            PublicKeyResponse.RecipientType.INTERNAL.value,
            "mimeType",
            arrayOf(buildKeyBody(key, hasEncryptionFlag))
        )
    }
}

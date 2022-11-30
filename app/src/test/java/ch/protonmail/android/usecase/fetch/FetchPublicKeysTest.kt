/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */

package ch.protonmail.android.usecase.fetch

import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.PublicKeyBody
import ch.protonmail.android.api.models.PublicKeyResponse
import ch.protonmail.android.api.models.enumerations.KeyFlag
import ch.protonmail.android.core.Constants.RESPONSE_CODE_OK
import ch.protonmail.android.core.Constants.RecipientLocationType
import ch.protonmail.android.usecase.model.FetchPublicKeysRequest
import ch.protonmail.android.usecase.model.FetchPublicKeysResult
import ch.protonmail.android.utils.extensions.toPmResponseBodyOrNull
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import me.proton.core.util.kotlin.EMPTY_STRING
import retrofit2.HttpException
import timber.log.Timber
import kotlin.test.Test
import kotlin.test.assertEquals

class FetchPublicKeysTest {

    private val api: ProtonMailApiManager = mockk()

    private val dispatchers = TestDispatcherProvider()

    private val fetchPublicKeys = FetchPublicKeys(api, dispatchers)

    @Test
    fun `email keys are fetched correctly with all keys having encryption flag set`() = runTest(dispatchers.Main) {
        // given
        val publicKeyResponse1 = buildKeyResponse(PUBLIC_KEY_1, hasEncryptionFlag = true)
        val publicKeyResponse2 = buildKeyResponse(PUBLIC_KEY_2, hasEncryptionFlag = true)

        coEvery { api.getPublicKeys(EMAIL_1) } returns publicKeyResponse1
        coEvery { api.getPublicKeys(EMAIL_2) } returns publicKeyResponse2

        val expected = listOf(
            buildKeySuccessResult(EMAIL_1, PUBLIC_KEY_1),
            buildKeySuccessResult(EMAIL_2, PUBLIC_KEY_2)
        )

        // when
        val result = fetchPublicKeys(buildKeyRequests(listOf(EMAIL_1, EMAIL_2)))

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `email keys are fetched correctly with just one key having encryption flag set`() = runTest(dispatchers.Main) {
        // given
        val publicKeyResponse1 = buildKeyResponse(PUBLIC_KEY_1, hasEncryptionFlag = true)
        val publicKeyResponse2 = buildKeyResponse(PUBLIC_KEY_2, hasEncryptionFlag = false)

        coEvery { api.getPublicKeys(EMAIL_1) } returns publicKeyResponse1
        coEvery { api.getPublicKeys(EMAIL_2) } returns publicKeyResponse2

        val expected = listOf(
            buildKeySuccessResult(EMAIL_1, PUBLIC_KEY_1),
            buildKeySuccessResult(EMAIL_2, EMPTY_STRING)
        )

        // when
        val result = fetchPublicKeys(buildKeyRequests(listOf(EMAIL_1, EMAIL_2)))

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `return correct results with Success and Error when connection error occurs for some keys`() =
        runTest(dispatchers.Main) {
            mockkStatic(Throwable::toPmResponseBodyOrNull) {
                // given
                val errorMessage = "An error occurred"
                val publicKeyResponse1 = buildKeyResponse(PUBLIC_KEY_1, hasEncryptionFlag = true)
                val publicKeyResponse3 = buildErrorResponse(message = errorMessage)
                val key3Exception: Throwable = mockk(relaxed = true) {
                    every { toPmResponseBodyOrNull() } returns publicKeyResponse3
                }

                coEvery { api.getPublicKeys(EMAIL_1) } returns publicKeyResponse1
            coEvery { api.getPublicKeys(EMAIL_2) } throws Exception()
            coEvery { api.getPublicKeys(EMAIL_3) } throws key3Exception

            val expected = listOf(
                buildKeySuccessResult(EMAIL_1, PUBLIC_KEY_1),
                buildKeyErrorResult(EMAIL_2, error = FetchPublicKeysResult.Failure.Error.Generic),
                buildKeyErrorResult(EMAIL_3, error = FetchPublicKeysResult.Failure.Error.WithMessage(errorMessage))
            )

            // when
            val result = fetchPublicKeys(buildKeyRequests(listOf(EMAIL_1, EMAIL_2, EMAIL_3)))

            // then
            assertEquals(expected, result)
        }
    }

    @Test
    fun `should log when the call fails and the error is not an http exception`() = runTest(dispatchers.Main) {
        mockkStatic(Timber::class)
        mockkStatic(Throwable::toPmResponseBodyOrNull)
        // given
        val publicKeyResponse = buildErrorResponse(message = "error")
        val keyException: IllegalStateException = mockk(relaxed = true) {
            every { toPmResponseBodyOrNull() } returns publicKeyResponse
        }

        coEvery { api.getPublicKeys(EMAIL_1) } throws keyException

        // when
        fetchPublicKeys(buildKeyRequests(listOf(EMAIL_1)))

        // then
        verify { Timber.w(keyException, any()) }

        unmockkStatic(Timber::class)
        unmockkStatic(Throwable::toPmResponseBodyOrNull)
    }

    @Test
    fun `should log when the call fails and the error is a non-422 http exception`() = runTest(dispatchers.Main) {
        mockkStatic(Timber::class)
        mockkStatic(Throwable::toPmResponseBodyOrNull)
        // given
        val publicKeyResponse = buildErrorResponse(message = "error")
        val keyException: HttpException = mockk(relaxed = true) {
            every { toPmResponseBodyOrNull() } returns publicKeyResponse
            every { code() } returns 401
        }

        coEvery { api.getPublicKeys(EMAIL_1) } throws keyException

        // when
        fetchPublicKeys(buildKeyRequests(listOf(EMAIL_1)))

        // then
        verify { Timber.w(keyException, any()) }

        unmockkStatic(Timber::class)
        unmockkStatic(Throwable::toPmResponseBodyOrNull)
    }

    @Test
    fun `should not log when the call fails and the error is a 422 http exception`() = runTest(dispatchers.Main) {
        mockkStatic(Timber::class)
        mockkStatic(Throwable::toPmResponseBodyOrNull)
        // given
        val publicKeyResponse = buildErrorResponse(message = "error")
        val keyException: HttpException = mockk(relaxed = true) {
            every { toPmResponseBodyOrNull() } returns publicKeyResponse
            every { code() } returns 422
        }

        coEvery { api.getPublicKeys(EMAIL_1) } throws keyException

        // when
        fetchPublicKeys(buildKeyRequests(listOf(EMAIL_1)))

        // then
        verify(exactly = 0) { Timber.w(keyException, any()) }

        unmockkStatic(Timber::class)
        unmockkStatic(Throwable::toPmResponseBodyOrNull)
    }

    private companion object TestData {

        const val EMAIL_1 = "email1@pm.me"
        const val EMAIL_2 = "email2@pm.me"
        const val EMAIL_3 = "email3@pm.me"

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
            error: FetchPublicKeysResult.Failure.Error = FetchPublicKeysResult.Failure.Error.Generic
        ) = FetchPublicKeysResult.Failure(email, location, error)

        fun buildKeyBody(key: String, hasEncryptionFlag: Boolean) = PublicKeyBody(
            if (hasEncryptionFlag) KeyFlag.ENCRYPTION_ENABLED.value else 0,
            key
        )

        fun buildKeyResponse(key: String, hasEncryptionFlag: Boolean): PublicKeyResponse = mockk {
            every { code } returns RESPONSE_CODE_OK
            every { recipientType } returns PublicKeyResponse.RecipientType.INTERNAL
            every { mimeType } returns "mimeType"
            every { keys } returns arrayOf(buildKeyBody(key, hasEncryptionFlag))
        }

        fun buildErrorResponse(message: String): PublicKeyResponse = mockk {
            every { code } returns 0
            every { error } returns message
        }
    }
}

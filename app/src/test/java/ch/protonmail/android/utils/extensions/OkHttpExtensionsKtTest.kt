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

package ch.protonmail.android.utils.extensions

import arrow.core.extensions.list.semigroup.plus
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import kotlin.test.assertEquals

@RunWith(Enclosed::class)
internal class OkHttpExtensionsKtTest {

    @RunWith(Parameterized::class)
    class IsRetryableErrorTest(
        private val response: Response,
        private val isRetryableError: Boolean
    ) {

        @Test
        fun `check if response is a retryable error`() {
            assertEquals(isRetryableError, response.isRetryableError())
        }

        companion object {

            private const val ERROR_MESSAGE = "error"
            private const val URL = "https://protonmail.com"
            private val retryableResponses = listOf(
                500.response(),
                503.response(),
                429.response(),
                408.response()
            )
            private val nonRetryableResponses = listOf(
                200.response(),
                204.response(),
                400.response(),
                401.response(),
                404.response(),
                418.response(),
                422.response()
            )

            private fun Int.response() = Response.Builder()
                .request(Request.Builder().url(URL).build())
                .code(this)
                .message(ERROR_MESSAGE)
                .protocol(Protocol.HTTP_2)
                .build()

            @JvmStatic
            @Parameterized.Parameters
            fun data(): Collection<Array<Any>> {
                return retryableResponses.map { response -> arrayOf(response, true) } +
                    nonRetryableResponses.map { response -> arrayOf(response, false) }
            }
        }
    }
}

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
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import retrofit2.HttpException
import retrofit2.Response
import java.io.InterruptedIOException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLHandshakeException
import javax.net.ssl.SSLPeerUnverifiedException
import kotlin.test.assertEquals

@RunWith(Enclosed::class)
internal class ExceptionExtensionsKtTest {

    @RunWith(Parameterized::class)
    class IsRetryableNetworkErrorTest(
        private val exception: Exception,
        private val isRetryableNetworkError: Boolean
    ) {

        @Test
        fun `check if exception is a retryable network error`() {
            assertEquals(isRetryableNetworkError, exception.isRetryableNetworkError())
        }

        companion object {

            private const val ERROR_MESSAGE = "error"
            private val retryableExceptions = listOf(
                500.httpException(),
                503.httpException(),
                429.httpException(),
                408.httpException(),
                InterruptedIOException(),
                SocketTimeoutException()
            )
            private val nonRetryableExceptions = listOf(
                400.httpException(),
                401.httpException(),
                404.httpException(),
                418.httpException(),
                422.httpException(),
                SSLPeerUnverifiedException(ERROR_MESSAGE),
                SSLHandshakeException(ERROR_MESSAGE),
                IllegalStateException(),
                InterruptedException()
            )

            private fun Int.httpException() = HttpException(
                Response.error<String>(this, ERROR_MESSAGE.toResponseBody())
            )

            @JvmStatic
            @Parameterized.Parameters
            fun data(): Collection<Array<Any>> {
                return retryableExceptions.map { exception -> arrayOf<Any>(exception, true) } +
                    nonRetryableExceptions.map { exception -> arrayOf(exception, false) }
            }
        }
    }
}

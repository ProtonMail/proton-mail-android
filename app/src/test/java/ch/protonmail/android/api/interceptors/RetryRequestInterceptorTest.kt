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

package ch.protonmail.android.api.interceptors

import arrow.core.left
import arrow.core.right
import ch.protonmail.android.utils.TryWithExponentialBackoff
import ch.protonmail.android.utils.extensions.isRetryableError
import ch.protonmail.android.utils.extensions.isRetryableNetworkError
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody
import okhttp3.internal.closeQuietly
import org.junit.Test
import kotlin.test.assertEquals

internal class RetryRequestInterceptorTest {

    private val interceptorChainMock = mockk<Interceptor.Chain> {
        every { request() } returns request
        every { proceed(request) } returns response
    }
    private val tryWithExponentialBackoffMock = mockk<TryWithExponentialBackoff>()
    private val retryRequestInterceptor = RetryRequestInterceptor(tryWithExponentialBackoffMock)

    @Test(expected = TestException::class)
    fun `should throw exception when exponential retry failed`() {
        // given
        coEvery {
            tryWithExponentialBackoffMock<Response>(any(), any(), any(), any(), any(), any(), any())
        } returns exception.left()

        // when
        retryRequestInterceptor.intercept(interceptorChainMock)
    }

    @Test
    fun `should return a response when exponential backoff returns it`() {
        // given
        coEvery {
            tryWithExponentialBackoffMock<Response>(any(), any(), any(), any(), any(), any(), any())
        } returns response.right()

        // when
        val actualResponse = retryRequestInterceptor.intercept(interceptorChainMock)

        // then
        assertEquals(response, actualResponse)
    }

    @Test
    fun `should try with backoff using correct params and close response body before retry`() = runBlockingTest {
        // given
        mockkStatic(Exception::isRetryableNetworkError, Response::isRetryableError, ResponseBody::closeQuietly)

        val shouldRetryOnErrorSlot = slot<(Exception) -> Boolean>()
        val shouldRetryOnResultSlot = slot<(Response) -> Boolean>()
        val beforeRetrySlot = slot<suspend (Int, Response?) -> Unit>()
        val blockSlot = slot<suspend () -> Response>()
        coEvery {
            tryWithExponentialBackoffMock(
                any(),
                any(),
                any(),
                capture(shouldRetryOnErrorSlot),
                capture(shouldRetryOnResultSlot),
                capture(beforeRetrySlot),
                capture(blockSlot)
            )
        } returns response.right()

        // when
        retryRequestInterceptor.intercept(interceptorChainMock)

        // then
        shouldRetryOnErrorSlot.captured(exception)
        verify { exception.isRetryableNetworkError() }

        shouldRetryOnResultSlot.captured(response)
        verify { response.isRetryableError() }

        beforeRetrySlot.captured(0, response)
        verify { response.body!!.closeQuietly() }

        blockSlot.captured()
        verify { interceptorChainMock.proceed(request) }

        unmockkStatic(Exception::isRetryableNetworkError, Response::isRetryableError, ResponseBody::closeQuietly)
    }

    private class TestException : Exception()

    private companion object TestData {

        val request = Request.Builder().url("https://protonmail.com").build()
        val responseBody = mockk<ResponseBody> {
            every { closeQuietly() } just runs
        }
        val response = Response.Builder()
            .request(request)
            .code(200)
            .message("OK")
            .body(responseBody)
            .protocol(Protocol.HTTP_2)
            .build()
        val exception = TestException()
    }
}

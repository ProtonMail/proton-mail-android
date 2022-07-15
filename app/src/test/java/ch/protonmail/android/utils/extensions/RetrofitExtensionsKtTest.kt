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
import kotlin.test.assertEquals

@RunWith(Enclosed::class)
internal class RetrofitExtensionsKtTest {

    @RunWith(Parameterized::class)
    class IsServerErrorTest(
        private val exception: HttpException,
        private val isServerError: Boolean
    ) {

        @Test
        fun `check if exception is a server error`() {
            assertEquals(isServerError, exception.isServerError())
        }

        companion object {

            private val serverErrorCodes = listOf(500, 501, 502, 503, 504, 505, 506, 507, 508, 510, 511)
            private val otherErrorCodes = listOf(401, 402, 404, 405, 418, 422)

            private fun Int.toHttpException() = HttpException(
                Response.error<String>(
                    this,
                    "error".toResponseBody()
                )
            )

            @JvmStatic
            @Parameterized.Parameters
            fun data(): Collection<Array<Any>> {
                return serverErrorCodes.map { serverErrorCode ->
                    arrayOf<Any>(serverErrorCode.toHttpException(), true)
                } + otherErrorCodes.map { otherErrorCode ->
                    arrayOf(otherErrorCode.toHttpException(), false)
                }
            }
        }
    }
}

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
import ch.protonmail.android.api.models.PaymentMethod
import ch.protonmail.android.api.models.PaymentMethodsResponse
import ch.protonmail.android.core.Constants
import ch.protonmail.android.usecase.model.FetchPaymentMethodsResult
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class FetchPaymentMethodsTest {

    @MockK
    private lateinit var api: ProtonMailApiManager

    @InjectMockKs
    private lateinit var useCase: FetchPaymentMethods

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun verifyRequestFailsWhenFetchingPayments() = runBlockingTest {
        // given
        val exceptionMessage = "Network failure!"
        val exception = Exception(exceptionMessage)
        coEvery { api.fetchPaymentMethods() } throws exception
        val expected = FetchPaymentMethodsResult.Error(exception)

        // when
        val actualResult = useCase.invoke()

        // then
        assertEquals(expected, actualResult)
    }

    @Test
    fun verifyRequestSucceedsWhenFetchPaymentsTerminatesNormally() = runBlockingTest {
        // given
        val paymentMethod1 = mockk<PaymentMethod>()
        val paymentMethodsResponse = mockk<PaymentMethodsResponse> {
            every { code } returns Constants.RESPONSE_CODE_OK
            every { paymentMethods } returns listOf(paymentMethod1)
        }
        coEvery { api.fetchPaymentMethods() } returns paymentMethodsResponse
        val expected = FetchPaymentMethodsResult.Success(paymentMethodsResponse.paymentMethods)

        // when
        val actualResult = useCase.invoke()

        // then
        assertEquals(expected, actualResult)
    }

    @Test
    fun verifyRequestFailsWhenFetchPaymentsApiReturnsError() = runBlockingTest {
        // given
        val paymentMethod1 = mockk<PaymentMethod>()
        val paymentMethodsResponse = mockk<PaymentMethodsResponse> {
            every { code } returns 121334
            every { paymentMethods } returns listOf(paymentMethod1)
        }
        coEvery { api.fetchPaymentMethods() } returns paymentMethodsResponse
        val expected = FetchPaymentMethodsResult.Error()

        // when
        val actualResult = useCase.invoke()

        // then
        assertEquals(expected, actualResult)
    }
}

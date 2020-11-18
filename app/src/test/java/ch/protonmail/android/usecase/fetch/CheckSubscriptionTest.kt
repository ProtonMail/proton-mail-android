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
import ch.protonmail.android.api.models.CheckSubscriptionResponse
import ch.protonmail.android.api.models.GetSubscriptionResponse
import ch.protonmail.android.api.models.Plan
import ch.protonmail.android.api.models.Subscription
import ch.protonmail.android.core.Constants
import ch.protonmail.android.usecase.model.CheckSubscriptionResult
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

class CheckSubscriptionTest {

    @MockK
    private lateinit var api: ProtonMailApiManager

    @InjectMockKs
    private lateinit var useCase:  CheckSubscription

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun verifyThatFetchSubscriptionErrorsAreIgnoredAndOnlyCheckSubscriptionFailuresAreTakenIntoAccount() {
        runBlockingTest {
            // given
            val coupon = "cupon1"
            val currency = Constants.CurrencyType.EUR
            val cycle = 0
            val planIds = mutableListOf<String>()
            val fetchExceptionMessage = "Fetch Subscription Network failure!"
            val checkExceptionMessage = "Check Subscription Network failure!"
            val fetchException = Exception(fetchExceptionMessage)
            val checkException = Exception(checkExceptionMessage)
            coEvery { api.fetchSubscription() } throws fetchException
            coEvery { api.checkSubscription(any()) } throws checkException
            val expected = CheckSubscriptionResult.Error(null, throwable = checkException)

            // when
            val actualResult = useCase.invoke(coupon, planIds, currency, cycle)

            // then
            assertEquals(expected, actualResult)
        }
    }

    @Test
    fun verifyRequestFailsWhenCheckingSubscriptions() {
        runBlockingTest {
            // given
            val coupon = "cupon1"
            val currency = Constants.CurrencyType.EUR
            val cycle = 0
            val planIds = mutableListOf<String>()
            val exceptionMessage = "Network failure!"
            val exception = Exception(exceptionMessage)
            val testPlan = mockk<Plan>(relaxed = true)
            val testSubscription = mockk<Subscription> {
                every { plans } returns listOf(testPlan)
            }
            val getSubscriptionResponse = mockk<GetSubscriptionResponse> {
                every { subscription } returns testSubscription
                every { code } returns Constants.RESPONSE_CODE_OK
            }
            coEvery { api.fetchSubscription() } returns getSubscriptionResponse
            coEvery { api.checkSubscription(any()) } throws exception
            val expected = CheckSubscriptionResult.Error(null, throwable = exception)

            // when
            val actualResult = useCase.invoke(coupon, planIds, currency, cycle)

            // then
            assertEquals(expected, actualResult)
        }
    }

    @Test
    fun verifyRequestSucceedsWhenCheckSubscriptionsTerminatesNormally() {
        runBlockingTest {
            // given
            val coupon = "cupon1"
            val currency = Constants.CurrencyType.EUR
            val cycle = 0
            val planIds = mutableListOf<String>()
            val testPlan = mockk<Plan>(relaxed = true)
            val testSubscription = mockk<Subscription> {
                every { plans } returns listOf(testPlan)
            }
            val getSubscriptionResponse = mockk<GetSubscriptionResponse> {
                every { subscription } returns testSubscription
                every { code } returns Constants.RESPONSE_CODE_OK
            }
            coEvery { api.fetchSubscription() } returns getSubscriptionResponse
            val checkSubscriptionResponse = mockk<CheckSubscriptionResponse> {
                every { code } returns Constants.RESPONSE_CODE_OK
            }
            coEvery { api.checkSubscription(any()) } returns checkSubscriptionResponse
            val expected = CheckSubscriptionResult.Success(checkSubscriptionResponse)

            // when
            val actualResult = useCase.invoke(coupon, planIds, currency, cycle)

            // then
            assertEquals(expected, actualResult)
        }
    }

    @Test
    fun verifyRequestSucceedsWhenCheckSubscriptionsTerminatesNormallyEvenWhenFetchSubscriptionFails() {
        runBlockingTest {
            // given
            val coupon = "cupon1"
            val currency = Constants.CurrencyType.EUR
            val cycle = 0
            val planIds = mutableListOf<String>()
            val fetchExceptionMessage = "Fetch Subscription Network failure!"
            val fetchException = Exception(fetchExceptionMessage)
            coEvery { api.fetchSubscription() } throws fetchException
            val checkSubscriptionResponse = mockk<CheckSubscriptionResponse> {
                every { code } returns Constants.RESPONSE_CODE_OK
            }
            coEvery { api.checkSubscription(any()) } returns checkSubscriptionResponse
            val expected = CheckSubscriptionResult.Success(checkSubscriptionResponse)

            // when
            val actualResult = useCase.invoke(coupon, planIds, currency, cycle)

            // then
            assertEquals(expected, actualResult)
        }
    }

    @Test
    fun verifyRequestFailsWhenCheckSubscriptionsTerminatesWithAnErrorCode() {
        runBlockingTest {
            // given
            val coupon = "cupon1"
            val currency = Constants.CurrencyType.EUR
            val cycle = 0
            val planIds = mutableListOf<String>()
            val testPlan = mockk<Plan>(relaxed = true)
            val testSubscription = mockk<Subscription> {
                every { plans } returns listOf(testPlan)
            }
            val getSubscriptionResponse = mockk<GetSubscriptionResponse> {
                every { subscription } returns testSubscription
                every { code } returns Constants.RESPONSE_CODE_OK
            }
            coEvery { api.fetchSubscription() } returns getSubscriptionResponse
            val checkSubscriptionResponse = mockk<CheckSubscriptionResponse> {
                every { code } returns 12323
            }
            coEvery { api.checkSubscription(any()) } returns checkSubscriptionResponse
            val expected = CheckSubscriptionResult.Error(checkSubscriptionResponse)

            // when
            val actualResult = useCase.invoke(coupon, planIds, currency, cycle)

            // then
            assertEquals(expected, actualResult)
        }
    }
}

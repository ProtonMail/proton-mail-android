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

package ch.protonmail.android.usecase.create

import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.CreateSubscriptionBody
import ch.protonmail.android.api.models.CreateUpdateSubscriptionResponse
import ch.protonmail.android.api.models.GetSubscriptionResponse
import ch.protonmail.android.api.models.Plan
import ch.protonmail.android.api.models.Subscription
import ch.protonmail.android.core.Constants
import ch.protonmail.android.usecase.model.CreateSubscriptionResult
import com.birbit.android.jobqueue.JobManager
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Ignore
import java.util.Collections
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class CreateSubscriptionTest {

    @MockK
    private lateinit var api: ProtonMailApiManager

    @RelaxedMockK
    private lateinit var jobManager: JobManager

    @InjectMockKs
    private lateinit var useCase: CreateSubscription

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun verifyRequestFailsWithNoInputDataProvided() {
        runBlockingTest {
            // given
            val amount = 1
            val currency = ""
            val cycle = 0
            val expected = CreateSubscriptionResult.Error("Incorrect input currency: is incorrect")

            // when
            val actualResult = useCase.invoke(amount, currency, cycle)

            // then
            assertEquals(expected, actualResult)
        }
    }

    @Test
    fun verifyRequestFailsWhenCreatingSubscriptions() {
        runBlockingTest {
            // given
            val amount = 1
            val currency = "EUR"
            val cycle = 0
            val exceptionMessage = "Network failure!"
            val exception = Exception(exceptionMessage)
            coEvery { api.fetchSubscription() } throws exception
            coEvery { api.createUpdateSubscription(any()) } throws exception
            val expected = CreateSubscriptionResult.Error("Error", throwable = exception)

            // when
            val actualResult = useCase.invoke(amount, currency, cycle)

            // then
            assertEquals(expected, actualResult)
        }
    }

    @Test
    fun verifyGivenPlanIdsListIsTransformedToMutableCollectionBeforeAddingPlansReturnedByFetchSubscriptionApi() {
        // This test is needed to verify we handle cases where the caller of CreateSubscription
        // passes `Collections.singletonList("planId")` or `List<String>` as planIds parameter
        runBlockingTest {
            // given
            val amount = 1
            val currency = "EUR"
            val cycle = 0

            // when
            val immutablePlanIds = mockk<MutableList<String>>(relaxed = true)
            useCase.invoke(amount, currency, cycle, immutablePlanIds)

            // then
            coVerifyOrder {
                immutablePlanIds.toMutableList()
                api.fetchSubscription()
            }
        }
    }

    @Test
    fun verifyCreateUpdateSubscriptionIsCalledWithInputPlansPlusAnyPlansReturnedByFetchSubscriptionsApiWhenAmountIsZero() {
        runBlockingTest {
            // given
            val amount = 0
            val currency = "EUR"
            val cycle = 0
            val testPlan = mockk<Plan>(relaxed = true) {
                every { id } returns "existingPlanId"
            }
            val testSubscription = mockk<Subscription> {
                every { plans } returns listOf(testPlan)
            }
            val getSubscriptionResponse = mockk<GetSubscriptionResponse> {
                every { subscription } returns testSubscription
                every { code } returns Constants.RESPONSE_CODE_OK
            }
            coEvery { api.fetchSubscription() } returns getSubscriptionResponse

            // when
            val immutablePlanIds = Collections.singletonList("inputPlanId")
            useCase.invoke(amount, currency, cycle, immutablePlanIds)

            // then
            val expectedPlanIds = listOf("inputPlanId", "existingPlanId")
            val expectedBody = CreateSubscriptionBody(amount, currency, null, null, expectedPlanIds, cycle)
            coVerify { api.createUpdateSubscription(expectedBody) }
        }
    }

    @Test
    fun verifyRequestFailsWhenCreateUpdateSubscriptionReturnsFailure() {
        runBlockingTest {
            // given
            val amount = 1
            val currency = "EUR"
            val cycle = 0
            val testPlan = mockk<Plan>(relaxed = true)
            val testSubscription = mockk<Subscription> {
                every { plans } returns listOf(testPlan)
            }
            val getSubscriptionResponse = mockk<GetSubscriptionResponse> {
                every { subscription } returns testSubscription
                every { code } returns Constants.RESPONSE_CODE_OK
            }
            coEvery { api.fetchSubscription() } returns getSubscriptionResponse

            val testError = "Test API Error"
            val testDetails = mapOf("Error1" to "Error1StringObject")
            val createSubscriptionResponse = mockk<CreateUpdateSubscriptionResponse> {
                every { subscription } returns testSubscription
                every { code } returns 123
                every { error } returns testError
                every { details } returns testDetails
            }
            coEvery { api.createUpdateSubscription(any()) } returns createSubscriptionResponse
            val expected = CreateSubscriptionResult.Error(testError, errorDescription = "Error1 : Error1StringObject\n")

            // when
            val actualResult = useCase.invoke(amount, currency, cycle)

            // then
            assertEquals(expected, actualResult)
        }
    }

    @Test
    fun verifyRequestFailsWhenCreateUpdateSubscriptionReturnsFailureAndInitialFetchSubscriptionErrorIsIgnored() {
        runBlockingTest {
            // given
            val amount = 1
            val currency = "EUR"
            val cycle = 0
            val testPlan = mockk<Plan>(relaxed = true)
            val testSubscription = mockk<Subscription> {
                every { plans } returns listOf(testPlan)
            }
            val fetchExceptionMessage = "Fetch Subscription Network failure!"
            val fetchException = Exception(fetchExceptionMessage)
            coEvery { api.fetchSubscription() } throws fetchException

            val testError = "Test API Error"
            val testDetails = mapOf("Error1" to "Error1StringObject")
            val createSubscriptionResponse = mockk<CreateUpdateSubscriptionResponse> {
                every { subscription } returns testSubscription
                every { code } returns 123
                every { error } returns testError
                every { details } returns testDetails
            }
            coEvery { api.createUpdateSubscription(any()) } returns createSubscriptionResponse
            val expected = CreateSubscriptionResult.Error(testError, errorDescription = "Error1 : Error1StringObject\n")

            // when
            val actualResult = useCase.invoke(amount, currency, cycle)

            // then
            assertEquals(expected, actualResult)
        }
    }

    @Ignore("Ignore until FetchUserSettingsJob & GetPaymentMethodsJob are also converted")
    @Test
    fun verifyRequestWorksProperlyWhenAmountIsZeroAndThereIsNoPaymentToken() {
        runBlockingTest {
            // given
            val amount = 1
            val currency = "EUR"
            val cycle = 0
            val testPlan = mockk<Plan>(relaxed = true)
            val testSubscription = mockk<Subscription> {
                every { plans } returns listOf(testPlan)
            }
            val getSubscriptionResponse = mockk<GetSubscriptionResponse> {
                every { subscription } returns testSubscription
                every { code } returns Constants.RESPONSE_CODE_OK
            }
            coEvery { api.fetchSubscription() } returns getSubscriptionResponse
            val createSubscriptionResponse = mockk<CreateUpdateSubscriptionResponse> {
                every { subscription } returns testSubscription
                every { code } returns Constants.RESPONSE_CODE_OK
            }
            coEvery { api.createUpdateSubscription(any()) } returns createSubscriptionResponse
            every { jobManager.addJobInBackground(any()) } returns Unit
            val expected = CreateSubscriptionResult.Success

            // when
            val actualResult = useCase.invoke(amount, currency, cycle)

            // then
            assertEquals(expected, actualResult)
        }
    }

}

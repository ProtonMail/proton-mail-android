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

package ch.protonmail.android.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.CreateUpdateSubscriptionResponse
import ch.protonmail.android.api.models.GetSubscriptionResponse
import ch.protonmail.android.api.models.Plan
import ch.protonmail.android.api.models.Subscription
import ch.protonmail.android.core.Constants
import com.birbit.android.jobqueue.JobManager
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Before
import org.junit.Test
import kotlin.test.Ignore
import kotlin.test.assertEquals

class CreateSubscriptionWorkerTest {

    @RelaxedMockK
    private lateinit var context: Context

    @RelaxedMockK
    private lateinit var parameters: WorkerParameters

    @MockK
    private lateinit var api: ProtonMailApiManager

    @RelaxedMockK
    private lateinit var jobManager: JobManager

    private var dispatcherProvider = TestDispatcherProvider

    @InjectMockKs
    private lateinit var worker: CreateSubscriptionWorker


    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun verifyWorkerFailsWithNoInputDataProvided() {
        runBlockingTest {
            // given
            val expected = ListenableWorker.Result.failure(
                workDataOf(KEY_WORKER_ERROR_DESCRIPTION to "Incorrect input parameters currency:, ids:[]")
            )

            // when
            val actualResult = worker.doWork()

            // then
            assertEquals(expected, actualResult)
        }
    }

    @Test
    fun verifyWorkerFailsWhenFetchingSubscriptions() {
        runBlockingTest {
            // given
            val currency = "EUR"
            every { parameters.inputData } returns
                workDataOf(
                    KEY_INPUT_CREATE_SUBSCRIPT_CURRENCY to currency,
                    KEY_INPUT_CREATE_SUBSCRIPT_PLAN_IDS to arrayOf<String>()
                )
            val exceptionMessage = "Network failure!"
            val exception = Exception(exceptionMessage)
            coEvery { api.fetchSubscription() } throws exception
            val expected = worker.failure(exception)

            // when
            val actualResult = worker.doWork()

            // then
            assertEquals(expected, actualResult)
        }
    }

    @Ignore("Ignore until FetchUserSettingsJob & GetPaymentMethodsJob are also converted")
    @Test
    fun verifyWorkerWorksProperlyWhenAmountIsZeroAndThereIsNoPaymentToken() {
        runBlockingTest {
            // given
            val currency = "EUR"
            every { parameters.inputData } returns
                workDataOf(
                    KEY_INPUT_CREATE_SUBSCRIPT_CURRENCY to currency,
                    KEY_INPUT_CREATE_SUBSCRIPT_AMOUNT to 0,
                    KEY_INPUT_CREATE_SUBSCRIPT_PLAN_IDS to arrayOf<String>()
                )
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
            val expected = ListenableWorker.Result.success()

            // when
            val actualResult = worker.doWork()

            // then
            assertEquals(expected, actualResult)
        }
    }

}

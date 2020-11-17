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

package ch.protonmail.android.viewmodel

import ch.protonmail.android.api.models.CheckSubscriptionResponse
import ch.protonmail.android.core.Constants
import ch.protonmail.android.testAndroid.lifecycle.testObserver
import ch.protonmail.android.usecase.create.CreateSubscription
import ch.protonmail.android.usecase.fetch.CheckSubscription
import ch.protonmail.android.usecase.model.CheckSubscriptionResult
import ch.protonmail.android.usecase.model.CreateSubscriptionResult
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class UpsellingViewModelTest : CoroutinesTest, ArchTest {

    @MockK
    private lateinit var createSubscriptionUseCase: CreateSubscription

    @MockK
    private lateinit var checkSubscriptionUseCase: CheckSubscription

    @InjectMockKs
    private lateinit var viewModel: UpsellingViewModel

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun verifyThatCreateSubscriptionCallsCreateSubscriptionUseCase() = runBlockingTest {
        // given
        val amount = 1
        val currency = "EUR"
        val cycle = 1
        val planIds = mutableListOf<String>()
        val couponCode = "ZeeCoupon"
        val token = "token"
        val createSubscriptionSuccess = CreateSubscriptionResult.Success
        coEvery { createSubscriptionUseCase(amount, currency, cycle, planIds, couponCode, token) } returns createSubscriptionSuccess

        // when
        viewModel.createSubscription(amount, currency, cycle, planIds, couponCode, token)
        val testObserver = viewModel.createSubscriptionResult.testObserver()

        // then
        coVerify { createSubscriptionUseCase(amount, currency, cycle, planIds, couponCode, token) }
        assertEquals(createSubscriptionSuccess, testObserver.observedValues[0])
    }

    @Test
    fun verifyThatCheckSubscriptionCallsCreateSubscriptionUseCase() = runBlockingTest {
        // given
        val currency = Constants.CurrencyType.EUR
        val cycle = 1
        val planIds = mutableListOf<String>()
        val couponCode = "ZeeCoupon"
        val response = mockk<CheckSubscriptionResponse>()
        val checkSubscriptionSuccess = CheckSubscriptionResult.Success(response)
        coEvery { checkSubscriptionUseCase(couponCode, planIds, currency, cycle) } returns checkSubscriptionSuccess

        // when
        viewModel.checkSubscription(couponCode, planIds, currency, cycle)
        val testObserver = viewModel.checkSubscriptionResult.testObserver()

        // then
        coVerify { checkSubscriptionUseCase(couponCode, planIds, currency, cycle) }
        assertEquals(checkSubscriptionSuccess, testObserver.observedValues[0])
    }

}

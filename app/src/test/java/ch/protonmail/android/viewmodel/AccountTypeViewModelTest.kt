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

import ch.protonmail.android.api.models.PaymentMethod
import ch.protonmail.android.testAndroid.lifecycle.testObserver
import ch.protonmail.android.usecase.fetch.FetchPaymentMethods
import ch.protonmail.android.usecase.model.FetchPaymentMethodsResult
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.account.domain.entity.Account
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.plan.presentation.PlansOrchestrator
import me.proton.core.plan.presentation.onUpgradeResult
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class AccountTypeViewModelTest : CoroutinesTest, ArchTest {

    @MockK
    private lateinit var fetchPaymentMethodsUseCase: FetchPaymentMethods
    @MockK
    private lateinit var plansOrchestrator: PlansOrchestrator
    @MockK
    private lateinit var accountManager: AccountManager

    @InjectMockKs
    private lateinit var viewModel: AccountTypeViewModel

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun verifyThatFetchPaymentMethodsCallsFetchPaymentMethodsUseCase() = runBlockingTest {
        // given
        val paymentMethod = mockk<PaymentMethod>()
        val fetchPaymentMethodSuccess = FetchPaymentMethodsResult.Success(listOf(paymentMethod))
        coEvery { fetchPaymentMethodsUseCase() } returns fetchPaymentMethodSuccess

        // when
        viewModel.fetchPaymentMethods()
        val testObserver = viewModel.fetchPaymentMethodsResult.testObserver()

        // then
        coVerify { fetchPaymentMethodsUseCase() }
        assertEquals(fetchPaymentMethodSuccess, testObserver.observedValues[0])
    }

    @Test
    fun verifyUpgradeCalledOnPrimaryUserNotNull() = runBlockingTest {
        // given
        val testUserId = UserId("test-user-id")
        val testAccount = mockk<Account>()
        every { testAccount.userId } returns testUserId
        coEvery { accountManager.getPrimaryUserId() } returns flowOf(testUserId)
        coEvery { accountManager.getAccount(testUserId) } returns flowOf(testAccount)
        coEvery { plansOrchestrator.setOnUpgradeResult(any()) } answers {
        }
        coEvery { plansOrchestrator.startUpgradeWorkflow(any()) } answers {
        }
        // when
        viewModel.onUpgradeClicked()

        // then
        coVerify(exactly = 1) { plansOrchestrator.startUpgradeWorkflow(testUserId) }
    }

    @Test
    fun verifyUpgradeNotCalledOnPrimaryUserNull() = runBlockingTest {
        // given
        val testUserId = UserId("test-user-id")
        val testAccount = mockk<Account>()
        every { testAccount.userId } returns testUserId
        coEvery { accountManager.getPrimaryUserId() } returns flowOf(null)
        coEvery { accountManager.getAccount(testUserId) } returns flowOf(testAccount)
        coEvery { plansOrchestrator.setOnUpgradeResult(any()) } answers {
        }
        coEvery { plansOrchestrator.startUpgradeWorkflow(any()) } answers {
        }
        // when
        viewModel.onUpgradeClicked()
        val testObserver = viewModel.upgradeStateResult.testObserver()

        // then
        coVerify(exactly = 0) { plansOrchestrator.startUpgradeWorkflow(testUserId) }
        assertEquals(AccountTypeViewModel.State.Error.PrimaryUser, testObserver.observedValues[0])
    }
}

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

package ch.protonmail.android.worker

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import ch.protonmail.android.usecase.fetch.FetchMailSettings
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.account.domain.entity.Account
import me.proton.core.account.domain.entity.AccountState
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.domain.getAccounts
import me.proton.core.domain.entity.UserId
import me.proton.core.test.kotlin.TestDispatcherProvider
import java.io.IOException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests the functionality of [FetchMailSettingsWorker].
 */

class FetchMailSettingsWorkerTest {

    @RelaxedMockK
    private lateinit var context: Context

    @RelaxedMockK
    private lateinit var parameters: WorkerParameters

    @MockK
    private lateinit var accountManager: AccountManager

    @MockK
    private lateinit var fetchMailSettings: FetchMailSettings

    private lateinit var fetchMailSettingsWorker: FetchMailSettingsWorker

    private var accounts = listOf(
        mockk<Account> {
            every { userId } returns UserId("user1")
            every { state } returns AccountState.Ready
        },
        mockk<Account> {
            every { userId } returns UserId("user2")
            every { state } returns AccountState.Ready
        }
    )
    private val dispatchers = TestDispatcherProvider()

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        fetchMailSettingsWorker = FetchMailSettingsWorker(
            context,
            parameters,
            fetchMailSettings,
            accountManager,
            dispatchers
        )
    }

    @Test
    fun `should return success when API call is successful`() = runTest(dispatchers.Main) {
        // given
        coEvery { accountManager.getAccounts(AccountState.Ready) } returns flowOf(accounts)
        coEvery { fetchMailSettings.invoke(any()) } returns mockk(relaxed = true)

        val expectedResult = ListenableWorker.Result.success()

        // when
        val workerResult = fetchMailSettingsWorker.doWork()

        // then
        assertEquals(expectedResult, workerResult)
    }

    @Test
    fun `should return retry when API call throws an exception`() {
        runTest(dispatchers.Main) {
            // given
            val mockException = IOException("exception")

            coEvery { fetchMailSettings.invoke(any()) } throws mockException

            val expectedResult = ListenableWorker.Result.retry()

            // when
            val workerResult = fetchMailSettingsWorker.doWork()

            // then
            assertEquals(expectedResult, workerResult)
        }
    }
}

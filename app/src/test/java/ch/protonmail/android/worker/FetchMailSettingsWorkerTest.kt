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
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.MailSettingsResponse
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import me.proton.core.test.kotlin.TestDispatcherProvider
import java.io.IOException
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests the functionality of [FetchMailSettingsWorker].
 *
 * @author Stefanija Boshkovska
 */

class FetchMailSettingsWorkerTest {

    @RelaxedMockK
    private lateinit var context: Context

    @RelaxedMockK
    private lateinit var parameters: WorkerParameters

    @MockK
    private lateinit var protonMailApiManager: ProtonMailApiManager

    @MockK
    private lateinit var userManager: UserManager

    private lateinit var fetchMailSettingsWorker: FetchMailSettingsWorker

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)

        fetchMailSettingsWorker = FetchMailSettingsWorker(
            context,
            parameters,
            protonMailApiManager,
            TestDispatcherProvider
        )
    }

    @Test
    fun `should return success when API call is successful`() {
        runBlocking {
            // given
            val mockMailSettingsResponse = mockk<MailSettingsResponse> {
                every { code } returns Constants.RESPONSE_CODE_OK
                every { mailSettings } returns mockk()
            }

            coEvery { protonMailApiManager.fetchMailSettings() } returns mockMailSettingsResponse

            val expectedResult = ListenableWorker.Result.success()

            // when
            val workerResult = fetchMailSettingsWorker.doWork()

            // then
            assertEquals(expectedResult, workerResult)
        }
    }

    @Test
    fun `should return retry when API call throws an exception`() {
        runBlocking {
            // given
            val mockException = IOException("exception")

            coEvery { protonMailApiManager.fetchMailSettings() } throws mockException

            val expectedResult = ListenableWorker.Result.retry()

            // when
            val workerResult = fetchMailSettingsWorker.doWork()

            // then
            assertEquals(expectedResult, workerResult)
        }
    }
}

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
import ch.protonmail.android.api.AccountManager
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class LogoutWorkerTest {

    @MockK
    private lateinit var userManager: UserManager

    @MockK
    private lateinit var accountManager: AccountManager

    @RelaxedMockK
    private lateinit var context: Context

    @RelaxedMockK
    private lateinit var parameters: WorkerParameters

    @MockK
    private lateinit var api: ProtonMailApiManager

    private lateinit var worker: LogoutWorker

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        worker = LogoutWorker(
            context,
            parameters,
            api,
            accountManager,
            userManager,
            TestDispatcherProvider
        )
    }

    @Test
    fun verifyThatWhenUserNameIsNotEmptyDeviceIsUnregisteredAndAccessIsRevoked() =
        runBlockingTest {
            // given
            val testUserName = "testUserName"
            val testLoggedInName = "testLoggedInName"
            every { userManager.username } returns testUserName
            every { userManager.nextLoggedInAccountOtherThanCurrent } returns null
            every { accountManager.getLoggedInUsers() } returns listOf(testLoggedInName)
            every { accountManager.clear() } returns Unit

            val revokeResponse = mockk<ResponseBody> {
                every { code } returns Constants.RESPONSE_CODE_OK
            }
            coEvery { api.revokeAccess(testUserName) } returns revokeResponse
            val expected = ListenableWorker.Result.success()


            // when
            val result = worker.doWork()

            // then
            verify { accountManager.clear() }
            coVerify { api.revokeAccess(testUserName) }
            assertEquals(expected, result)
        }

}

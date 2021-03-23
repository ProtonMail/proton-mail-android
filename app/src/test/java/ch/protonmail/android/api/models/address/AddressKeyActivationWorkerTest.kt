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

package ch.protonmail.android.api.models.address

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.user.Address
import ch.protonmail.android.domain.entity.user.AddressKey
import ch.protonmail.android.domain.entity.user.AddressKeys
import ch.protonmail.android.domain.entity.user.UserKey
import ch.protonmail.android.domain.entity.user.UserKeys
import ch.protonmail.android.utils.extensions.app
import com.proton.gopenpgp.helper.Helper
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Ignore
import kotlin.test.Test
import kotlin.test.assertEquals

class AddressKeyActivationWorkerTest {

    private val testUserId = Id("id")

    @RelaxedMockK
    private lateinit var context: Context

    @RelaxedMockK
    private lateinit var parameters: WorkerParameters

    private val userManager: UserManager = mockk {
        every { getMailboxPassword(any()) } returns "haslo".toByteArray()
        every { openPgp } returns mockk(relaxed = true)
        coEvery { getUser(any()) } returns mockk(relaxed = true)
    }

    @MockK
    private lateinit var api: ProtonMailApiManager

    private lateinit var worker: AddressKeyActivationWorker

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        mockkStatic(Helper::class)

        worker = AddressKeyActivationWorker(
            context,
            parameters,
            userManager,
            api,
            TestDispatcherProvider
        )
        every { parameters.inputData } returns workDataOf(KEY_INPUT_DATA_USER_ID to testUserId.s)
        every { context.app.organization } returns null
    }

    @AfterTest
    fun tearDown() {
        unmockkStatic(Helper::class)
    }


    @Test
    fun verifyThatSuccessIsReturnedForEmptyKeysToActivateAndErrorsList() = runBlockingTest {
        // given
        val expectedResult = ListenableWorker.Result.success()

        // when
        val result = worker.doWork()


        // then
        assertEquals(expectedResult, result)
    }

    @Ignore(
        "To unblock this test we need to overcome UnsatisfiedLinkError: no gojni in java.library.path" +
            "error linked to Helper.getJsonSHA256Fingerprints usage"
    )
    @Test
    fun verifyThatMessageFormatIsCorrect() = runBlockingTest {
        // given
        val expectedResult = ListenableWorker.Result.success()
        val primaryKey1 = mockk<AddressKey>(relaxed = true) {
            every { id } returns Id("primaryKey1")
        }
        val addressKey1 = mockk<AddressKey>(relaxed = true) {
            every { id } returns Id("addressKey1")
        }
        val userPrimaryKey1 = mockk<UserKey>(relaxed = true) {
            every { id } returns Id("userPrimaryKey1")
        }
        val userKey1 = mockk<UserKey>(relaxed = true) {
            every { id } returns Id("userKey1")
        }
        val testAddressKeys = AddressKeys(primaryKey1, listOf(primaryKey1, addressKey1))
        val testUserKeys = UserKeys(userPrimaryKey1, listOf(userPrimaryKey1, userKey1))
        val testAddress = mockk<Address> {
            every { keys } returns testAddressKeys
        }
        val addressesMap = mapOf(1 to testAddress)
        val testUser = mockk<ch.protonmail.android.domain.entity.user.User> {
            every { addresses.addresses } returns addressesMap
            every { keys } returns testUserKeys
        }
        val testJsonFingerprint = "{\"json\": \"value\"}"
        coEvery { userManager.getLegacyUser(testUserId).toNewUser() } returns testUser
        every { Helper.getJsonSHA256Fingerprints(any()) } returns testJsonFingerprint.toByteArray()

        // when
        val result = worker.doWork()

        // then
        assertEquals(expectedResult, result)
    }
}

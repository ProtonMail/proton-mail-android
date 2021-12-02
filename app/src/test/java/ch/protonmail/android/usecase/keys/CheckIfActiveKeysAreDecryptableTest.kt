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

package ch.protonmail.android.usecase.keys

import ch.protonmail.android.api.models.User
import ch.protonmail.android.api.models.address.Address
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.crypto.AddressCryptoFactory
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.spyk
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CheckIfActiveKeysAreDecryptableTest {

    private val userSpy = spyk<User> {
        every { save() } just runs
    }
    private val decryptableKeysAddressCryptoMock = mockk<AddressCrypto> {
        every { areActiveKeysDecryptable() } returns true
    }
    private val nonDecryptableKeysAddressCryptoMock = mockk<AddressCrypto> {
        every { areActiveKeysDecryptable() } returns false
    }
    private val userManagerMock = mockk<UserManager> {
        every { username } returns TestData.User.USERNAME
        every { user } returns userSpy
    }
    private val addressCryptoFactoryMock = mockk<AddressCryptoFactory> {
        every {
            newAddressCrypto(eq(userManagerMock), any(), TestData.Address.DECRYPTABLE_ID)
        } returns decryptableKeysAddressCryptoMock
        every {
            newAddressCrypto(eq(userManagerMock), any(), TestData.Address.NON_DECRYPTABLE_ID)
        } returns nonDecryptableKeysAddressCryptoMock
    }
    private val checkIfActiveKeysAreDecryptable = CheckIfActiveKeysAreDecryptable(
        userManagerMock,
        addressCryptoFactoryMock
    )

    @Test
    fun `should return true if the keys for all addresses are decryptable`() {
        // given
        givenUserWithAllAddressKeysDecryptable()

        // when
        val areKeysDecryptable = checkIfActiveKeysAreDecryptable()

        // then
        assertTrue(areKeysDecryptable)
    }

    @Test
    fun `should return false if keys for one of the addresses are not decryptable`() {
        // given
        givenUserWithSomeAddressKeysNonDecryptable()

        // when
        val areKeysDecryptable = checkIfActiveKeysAreDecryptable()

        // then
        assertFalse(areKeysDecryptable)
    }

    private fun givenUserWithAllAddressKeysDecryptable() {
        userSpy.setAddresses(
            (1..10).map { TestData.Address.WITH_DECRYPTABLE_KEYS }
        )
    }

    private fun givenUserWithSomeAddressKeysNonDecryptable() {
        userSpy.setAddresses(
            (1..10).map {
                if (it == 5)
                    TestData.Address.WITH_NON_DECRYPTABLE_KEYS
                else
                    TestData.Address.WITH_DECRYPTABLE_KEYS
            }
        )
    }

    private object TestData {
        object User {
            const val USERNAME = "username"
        }

        object Address {
            const val DECRYPTABLE_ID = "address id"
            const val NON_DECRYPTABLE_ID = "can't decrypt this"
            val WITH_DECRYPTABLE_KEYS = addressWithId(DECRYPTABLE_ID)
            val WITH_NON_DECRYPTABLE_KEYS = addressWithId(NON_DECRYPTABLE_ID)

            private fun addressWithId(id: String) = Address(
                id,
                null,
                null,
                0,
                0,
                0,
                0,
                0,
                null,
                null,
                0,
                null
            )
        }
    }
}

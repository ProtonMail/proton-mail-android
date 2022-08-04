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

package ch.protonmail.android.compose.domain

import ch.protonmail.android.core.UserManager
import ch.protonmail.android.testdata.AddressesTestData
import ch.protonmail.android.testdata.UserTestData
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertEquals

internal class GetAddressIndexByAddressIdTest {

    private val userManagerMock = mockk<UserManager>()
    private val getAddressIndexByAddressId = GetAddressIndexByAddressId(userManagerMock)

    @Test
    fun `should return index of the address if found in the user addresses`() {
        // Given
        val expectedIndex = 2
        val idsToAddresses = AddressesTestData.idsToRawAddresses
        val addresses = idsToAddresses.map { it.second }
        val addressId = idsToAddresses[expectedIndex].first
        val userWithAddresses = UserTestData.withAddresses(AddressesTestData.from(idsToAddresses))
        every { userManagerMock.requireCurrentUser() } returns userWithAddresses

        // When
        val actualIndex = getAddressIndexByAddressId(addresses, addressId)

        // Then
        assertEquals(expectedIndex, actualIndex)
    }

    @Test(expected = NoSuchElementException::class)
    fun `should throw when index of the address if not found in the user addresses`() {
        // Given
        val idsToAddresses = AddressesTestData.idsToRawAddresses
        val addresses = idsToAddresses.map { it.second }
        val addressId = idsToAddresses[0].first + "not in the list"
        val userWithAddresses = UserTestData.withAddresses(AddressesTestData.from(idsToAddresses))
        every { userManagerMock.requireCurrentUser() } returns userWithAddresses

        // When
        getAddressIndexByAddressId(addresses, addressId)
    }
}

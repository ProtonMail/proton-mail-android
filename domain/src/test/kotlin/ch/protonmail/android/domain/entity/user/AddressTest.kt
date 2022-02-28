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
package ch.protonmail.android.domain.entity.user

import assert4k.`is`
import assert4k.`not equals`
import assert4k.`null`
import assert4k.assert
import assert4k.equals
import assert4k.that
import assert4k.times
import ch.protonmail.android.domain.entity.EmailAddress
import me.proton.core.user.domain.entity.AddressId
import kotlin.test.Test

/**
 * Test suite for [Address] and [Addresses]
 */
internal class AddressTest {

    @Test
    fun `Addresses_primary return the right address`() {

        // GIVEN
        val addresses = Addresses(mapOf(4 to Address("1"), 5 to Address("2"), 2 to Address("3")))

        // WHEN - THEN
        assert that addresses.primary equals Address("3")
    }

    @Test
    fun `Addresses_primary is null if there are not addresses`() {
        assert that Addresses(emptyMap()).primary `is` `null`
    }

    @Test
    fun `Addresses_sorted works properly`() {

        // GIVEN
        val forth = Address("1")
        val fifth = Address("2")
        val second = Address("3")
        val addresses = Addresses(mapOf(4 to forth, 5 to fifth, 2 to second))

        // WHEN - THEN
        assert that addresses.sorted() * { sorted ->
            sorted equals listOf(second, forth, fifth)
            sorted `not equals` listOf(forth, fifth, second)
        }
    }

    @Test
    fun `Addresses_sorted returns empty list if there are no addresses`() {
        assert that Addresses(emptyMap()).sorted() equals emptyList()
    }

    private fun Address(id: String) = Address(
        AddressId(id),
        "domain_id",
        EmailAddress("$id@mail.com"),
        null,
        null,
        true,
        Address.Type.ORIGINAL,
        allowedToSend = true,
        allowedToReceive = true,
        keys = AddressKeys(null, emptyList())
    )
}

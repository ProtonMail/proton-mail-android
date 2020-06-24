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
package ch.protonmail.android.domain.entity.user

import assert4k.*
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.ValidationException
import kotlin.test.Test

/**
 * Test suite for [AddressKey] and [AddressKeys]
 * @author Davide Farella
 */
internal class AddressKeyTest {

    @Test
    fun `AddressKeys can be created if valid`() {
        AddressKeys(
            primaryKey = AddressKey(Id("id")),
            keys = listOf(AddressKey(Id("id")), AddressKey(Id("another_id")))
        )
        AddressKeys(
            primaryKey = null,
            keys = emptyList()
        )
    }

    @Test
    fun `AddressKeys fails if primaryKey is null, but keys is NOT empty`() {
        assert that fails<ValidationException> {
            AddressKeys(
                primaryKey = null,
                keys = listOf(AddressKey(Id("id")))
            )
        }
    }

    @Test
    fun `AddressKeys fails if primaryKey is NOT null, but keys is empty`() {
        assert that fails<ValidationException> {
            AddressKeys(
                primaryKey = AddressKey(Id("id")),
                keys = emptyList()
            )
        }
    }

    @Test
    fun `AddressKeys fails if keys does not contain primaryKey`() {
        assert that fails<ValidationException> {
            AddressKeys(
                primaryKey = AddressKey(Id("id")),
                keys = listOf(AddressKey(Id("another_id")))
            )
        }
    }
}

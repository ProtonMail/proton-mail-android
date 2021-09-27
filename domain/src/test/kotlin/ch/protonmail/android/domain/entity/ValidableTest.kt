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
package ch.protonmail.android.domain.entity

import assert4k.*
import kotlin.test.Test

/**
 * Test suite for [Validable]
 * @author Davide Farella
 */
internal class ValidableTest {

    @Test
    fun `isValid return proper Boolean`() {
        assert that EmailTestValidable("somebody@email.com").isValid()
        assert that ! EmailTestValidable("invalid").isValid()
    }

    @Test
    fun `validate returns proper Result`() {
        assert that (EmailTestValidable("somebody@email.com").validate() is Validable.Result.Success)
        assert that (EmailTestValidable("invalid").validate() is Validable.Result.Error)
    }

    @Test
    fun `requireValid throws when Validable is not valid`() {

        EmailTestValidable("somebody@email.com").requireValid()

        assert that fails<ValidationException> {
            EmailTestValidable("invalid").requireValid()
        } with "Regex mismatch: <invalid>, <.+@.+\\..+>"
    }

    @Test
    fun `validOrNull return Validable if success`() {
        assert that validOrNull { ValidatedEmail("hello@mail.com") } equals ValidatedEmail("hello@mail.com")
    }

    @Test
    fun `validOrNull return null if failure`() {
        assert that validOrNull { ValidatedEmail("hello") } `is` `null`
    }
}

@Validated
private data class ValidatedEmail(val s: String) : Validable by EmailTestValidator(s) {
    init { requireValid() }
}

private data class EmailTestValidable(val s: String) : Validable by EmailTestValidator(s)

// Regex is representative only for this test case and not intended to properly validate an email address
private fun EmailTestValidator(email: String) = RegexValidator(email, ".+@.+\\..+")

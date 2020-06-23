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
 * Test suite for built-in [Validator]s
 * @author Davide Farella
 */
internal class ValidatorsTest {

    @Test
    fun `custom Validator works correctly`() {
        class PositiveNumber(val number: Int) : Validable by Validator<PositiveNumber>({ number >= 0 })

        assert that PositiveNumber(10).isValid()
        assert that ! PositiveNumber(-10).isValid()
    }

    @Test
    fun `NotBlackStringValidator works correctly`() {
        class SomeText(string: String) : Validable by NotBlankStringValidator(string) {
            init { requireValid() }
        }

        SomeText("hello")
        assert that fails<ValidationException> { SomeText("") }
        assert that fails<ValidationException> { SomeText(" ") }
    }

    @Test
    fun `RegexValidator works correctly`() {
        // Regex is representative only for this test case and not intended to properly validate an email address
        class EmailRegexValidable(string: String) : Validable by RegexValidator(string, "\\w+@[a-z]+\\.[a-z]+")

        assert that EmailRegexValidable("somebody@protonmail.com").isValid()
        assert that ! EmailRegexValidable("somebody@123.456").isValid()
    }

}

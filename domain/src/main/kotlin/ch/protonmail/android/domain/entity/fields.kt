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

/*
 * A set of typed representation of business models that can be described as 'field'.
 * e.g. Email, Username, Password
 *
 * Thanks to this strongly typed paradigm we can about confusion about 'userId' to 'messageId' and similar cases.
 *
 * Even more, this can allow us to run simple validation on object instantiation, so we can ensure that an 'Email'
 * entity respect the proper format since its born to its death
 */

// SORTED FIRSTLY ALPHABETICALLY AND THEN LOGICALLY WHEN 2 OR MORE ARE SO STRONGLY CONNECTED THAT THEY REQUIRES TO
// STAY CLOSE

// IF WE'RE BE GOIN TO APPROACH A BIG SIZE FOR THIS FILE, WE MUST CONSIDER SPLITTING INTO DIFFERENT FILES TO BE PLACED
// INTO A 'field' PACKAGE

/**
 * Represent a given number of bytes
 */
inline class Bytes(val l: ULong)

/**
 * Entity representing an email address
 * [Validable] by [RegexValidator]
 */
@Validated
data class EmailAddress(val s: String) : Validable by RegexValidator(s, VALIDATION_REGEX) {
    init { requireValid() }

    private companion object {
        @Suppress("MaxLineLength") // Nobody can read it anyway ¯\_(ツ)_/¯
        const val VALIDATION_REGEX = """(?:[a-z0-9!#${'$'}%&'*+/=?^_`{|}~-]+(?:\.[a-z0-9!#${'$'}%&'*+/=?^_`{|}~-]+)*|"(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21\x23-\x5b\x5d-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])*")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\[(?:(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(?:25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?|[a-z0-9-]*[a-z0-9]:(?:[\x01-\x08\x0b\x0c\x0e-\x1f\x21-\x5a\x53-\x7f]|\\[\x01-\x09\x0b\x0c\x0e-\x7f])+)\])"""
    }
}

/**
 * Entity representing an id
 * [Validable] by [NotBlankStringValidator]
 */
@Validated
data class Id(val s: String) : Validable by NotBlankStringValidator(s) {
    init { requireValid() }
}

/**
 * Entity representing a generic name
 * [Validable] by [NotBlankStringValidator]
 */
data class Name(val s: String) : Validable by NotBlankStringValidator(s) {
    init { requireValid() }
}

/**
 * Entity representing a generic String that cannot be blank
 * [Validable] by [NotBlankStringValidator]
 */
data class NotBlankString(val s: String) : Validable by NotBlankStringValidator(s) {
    init { requireValid() }
}

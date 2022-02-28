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
package ch.protonmail.android.domain.entity

/**
 * Entity that validates an [Validable]
 * @throws ValidationException if not valid
 *
 * @author Davide Farella
 */
typealias Validator<V> = (V) -> Any

/**
 * Creates a [Validator] using a lambda that validate the entity
 *
 * The lambda can
 * * throw exception
 *   @see require
 *   ```
class PositiveNumber(val number: Int) : Validable by Validator<PositiveNumber>({
    require(number >= 0) { "Number is less that 0" }
})
 *   ```
 * * return a [Boolean], in that case it will be wrapper into a [require]
 *   ```
class PositiveNumber(val number: Int) : Validable by Validator<PositiveNumber>({ number >= 0 })
 *   ```
 */
fun <V> Validator(validation: V.() -> Any) = { v: V ->
    validation(v)
        .also { if (it is Boolean) require(it) }
}.wrap()

/**
 * Returns an implementation of [Validable] that provide the receiver as its [Validator]
 * Example:
 * ```
val MyValidator = { validable: MyValidable ->
// validation logic that return a Validable.Result
}

data class MyValidable(args..) : Validable by MyValidator()
 * ```
 */
operator fun <V : Validable> Validator<V>.invoke() = object : Validable {
    override val validator: Validator<*>
        get() = this@invoke
}

// Mirror of `invoke`. Used internally for readability purpose
private fun <V : Validable> Validator<V>.wrap() = invoke()

/**
 * [Validator] that accepts only strings that are not blank
 */
fun NotBlankStringValidator(field: String) = { _: Any ->
    require(field.isNotBlank()) { "String is blank" }
}.wrap()

/**
 * [Validator] that validate using a [Regex]
 * @param field [String] field to validate
 */
fun RegexValidator(field: String, regex: Regex) = { _: Any ->
    require(regex.matches(field)) { "Regex mismatch: <$field>, <$regex>" }
}.wrap()

/**
 * [Validator] that validate using a [Regex]
 * @param field [String] field to validate
 */
fun RegexValidator(field: String, regex: String) =
    RegexValidator(field, regex.toRegex())

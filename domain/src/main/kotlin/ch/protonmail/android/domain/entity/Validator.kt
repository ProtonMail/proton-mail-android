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

/**
 * Entity that validates an [Validable]
 * @author Davide Farella
 */
typealias Validator<V> = (V) -> Validable.Result

/**
 * Creates a [Validator] using a simple lambda that return `true` if validation is successful
 * ```
class PositiveNumber(val number: Int) : Validable by Validator<PositiveNumber>({ number >= 0 })
 * ```
 */
fun <V> Validator(successBlock: V.() -> Boolean) = { v: V ->
    Validable.Result(successBlock(v))
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
private fun <V : Validable> Validator<V>.wrap() = invoke<V>()

/**
 * [Validator] that accepts only strings that are not blank
 */
fun NotBlankStringValidator(field: String) = { _: Any ->
    Validable.Result(field.isNotBlank())
}.wrap()

/**
 * [Validator] that validate using a [Regex]
 * @param field [String] field to validate
 */
fun RegexValidator(field: String, regex: Regex) = { _: Any ->
    Validable.Result(regex.matches(field))
}.wrap()

/**
 * [Validator] that validate using a [Regex]
 * @param field [String] field to validate
 */
fun RegexValidator(field: String, regex: String) =
    RegexValidator(field, regex.toRegex())

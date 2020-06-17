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

import ch.protonmail.android.domain.entity.Validable.Result.Error
import ch.protonmail.android.domain.entity.Validable.Result.Success

/**
 * An entity that can be validated
 * It requires a [Validator]
 * @see invoke - without args - extension on [Validator]
 *
 * @author Davide Farella
 */
interface Validable {

    /**
     * A [Validator] for this entity must be provided in order to validate it.
     * Start project has been used in order to avoid AN HELL of generics, use it wisely and ensure to provide the proper
     * validator :)
     */
    val validator: Validator<*>

    /**
     * Result of the validation
     * It can be [Success] or [Error] with no further information provided, as now
     */
    sealed class Result {

        object Success : Result()
        object Error : Result()
    }
}

/**
 * @return `true` is validation is successful
 */
@Suppress("UNCHECKED_CAST")
fun <V : Validable> V.isValid() = (validator as Validator<V>).isValid(this)

/**
 * Validate the entity
 * @return [Validable.Result]
 */
@Suppress("UNCHECKED_CAST")
fun <V : Validable> V.validate() = (validator as Validator<V>).validate(this)

/**
 * Complete if validation is successful
 * @throws ValidationException
 *
 * @return receiver [V] of [Validable]
 */
@Suppress("UNCHECKED_CAST")
fun <V : Validable> V.requireValid() = (validator as Validator<V>).requireValid(this)

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

/**
 * Entity that validates an [Validable]
 */
typealias Validator<V> = (V) -> Validable.Result

private fun <V : Validable> Validator<V>.isValid(validable: V) =
    validate(validable) is Validable.Result.Success

private fun <V : Validable> Validator<V>.validate(validable: V): Validable.Result =
    invoke(validable)

private fun <V : Validable> Validator<V>.requireValid(validable: V) = apply {
    validate(validable).also { result ->
        if (result is Validable.Result.Error) throw ValidationException(validable)
    }
}

/**
 * An exception thrown from [Validable.requireValid] in case the validation fails
 * @param validable [Validable] that failed the validation
 */
class ValidationException(validable: Validable) :
    Exception("Validable did not validate successfully: $validable")

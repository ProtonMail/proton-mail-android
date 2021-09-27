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
 *
 * It requires a [Validator]
 * @see invoke - without args - extension on [Validator]
 * or use standard override
 * ```
data class MyValidable(args...) : Validable {
    override val validator = { validable: MyValidable ->
        // validation logic
    }
}
 * ```
 *
 * @see Validated annotation
 * One entity can call [requireValid] in its `init` block, for ensure that that given entity is always valid
 * NOTE: the `init` block must be declared after the override of [validator]
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

        companion object {
            operator fun invoke(isSuccess: Boolean) =
                if (isSuccess) Result.Success else Result.Error
        }
    }
}

// region CHECKS

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
fun <V : Validable> V.requireValid() = (validator as Validator<V>).requireValid(this as V)

/**
 * @return [Validable] created in this lambda which is marked as [Validated] if validation is successful,
 * otherwise `null`
 * Example: ``  validOrNull { MyValidable("hello" }  ``
 */
inline fun <V : Validable> validOrNull(block: () -> V): V? =
    try {
        block()
    } catch (e: ValidationException) {
        null
    }

// endregion

/**
 * An exception thrown from [Validable.requireValid] in case the validation fails
 */
typealias ValidationException = IllegalArgumentException

/**
 * Represents an entity that is validated on its initialisation. ``  init { requireValid() }  ``
 * @throws ValidationException if validation fails.
 *
 * This can be used on an entity that doesn't directly inherit from [Validable], but all its fields are marked as
 * [Validated]
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR)
annotation class Validated

private fun <V : Validable> Validator<V>.isValid(validable: V) =
    validate(validable) is Success

private fun <V : Validable> Validator<V>.validate(validable: V): Validable.Result =
    try {
        invoke(validable)
        Success
    } catch (e: ValidationException) {
        Error
    }

private fun <V : Validable> Validator<V>.requireValid(validable: V) = apply {
    invoke(validable)
}

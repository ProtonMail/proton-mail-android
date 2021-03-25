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

package ch.protonmail.android.domain.util

import arrow.core.Either
import arrow.core.getOrElse
import me.proton.core.domain.arch.Mapper
import me.proton.core.util.kotlin.invoke

/**
 * Returns the right value if it exists, otherwise throw [NoValueError]
 *
 * Example:
 * ```kotlin:ank:playground
 * import arrow.core.Right
 * import arrow.core.Left
 *
 * //sampleStart
 * fun getRight = Right(12).orThrow() // Result: 12
 * fun getLeft = Left(12).orThrow()   // Result: exception thrown
 * //sampleEnd
 * fun main() {
 *   println("right = getRight()")
 *   println("left = getLeft()")
 * }
 * ```
 */
fun <A, B> Either<A, B>.orThrow(): B =
    getOrElse { throw NoValueError(leftOrThrow()) }

/**
 * Returns the left value if it exists, otherwise null
 *
 * Example:
 * ```kotlin:ank:playground
 * import arrow.core.Right
 * import arrow.core.Left
 *
 * //sampleStart
 * val right = Right(12).leftOrNull() // Result: null
 * val left = Left(12).leftOrNull()   // Result: 12
 * //sampleEnd
 * fun main() {
 *   println("right = $right")
 *   println("left = $left")
 * }
 * ```
 */
fun <A, B> Either<A, B>.leftOrNull(): A? =
    fold({ it }, { null })

/**
 * Returns the left value if it exists, otherwise throw [NoValueError]
 *
 * Example:
 * ```kotlin:ank:playground
 * import arrow.core.Right
 * import arrow.core.Left
 *
 * //sampleStart
 * fun getRight = Right(12).orThrow() // Result: exception thrown
 * fun getLeft = Left(12).orThrow()   // Result: 12
 * //sampleEnd
 * fun main() {
 *   println("right = getRight()")
 *   println("left = getLeft()")
 * }
 * ```
 */
fun <A, B> Either<A, B>.leftOrThrow(): A =
    fold({ it }, { throw NoValueError(orThrow()) })

class NoValueError internal constructor (val other: Any?) : Exception(other!!::class.simpleName)

/**
 * Shadow of [Either.map] using a custom [Mapper]
 * The given function is applied if this is a `Right`
 */
inline fun <B, A : Error, C, M : Mapper<B, C>> Either<A, B>.map(
    mapper: M,
    map: M.(B) -> C
): Either<A, C> = map { mapper { map(it) } }

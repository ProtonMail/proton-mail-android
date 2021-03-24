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

package ch.protonmail.android.domain.either

sealed class Either<out A, out B> {

    internal abstract val isLeft: Boolean
    internal abstract val isRight: Boolean

    fun isLeft(): Boolean = isLeft
    fun isRight(): Boolean = isRight

    fun leftOrNull(): A? =
        if (this is Left) a else null

    fun rightOrNull(): B? =
        if (this is Right) b else null

    fun leftOrThrow(): A =
        leftOrNull() ?: throw NoValueError(rightOrThrow())

    fun rightOrThrow(): B =
        rightOrNull() ?: throw NoValueError(leftOrThrow())

    /**
     * Applies `ifLeft` if this is a [Left] or `ifRight` if this is a [Right].
     *
     * Example:
     * ```
     * val result: Either<Exception, Value> = possiblyFailingOperation()
     * result.fold(
     *      { log("operation failed with $it") },
     *      { log("operation succeeded with $it") }
     * )
     * ```
     *
     * @param ifLeft the function to apply if this is a [Left]
     * @param ifRight the function to apply if this is a [Right]
     * @return the results of applying the function
     */
    inline fun <C> fold(ifLeft: (A) -> C, ifRight: (B) -> C): C = when (this) {
        is Right -> ifRight(b)
        is Left -> ifLeft(a)
    }

    /**
     * The given function is applied if this is a [Right].
     *
     * Example:
     * ```
     * Right(12).map { "flower" } // Result: Right("flower")
     * Left(12).map { "flower" }  // Result: Left(12)
     * ```
     */
    inline fun <C> map(f: (B) -> C): Either<A, C> =
        flatMap { Right(f(it)) }


    data class Left<out A, out B>(val a: A) : Either<A, B>() {
        override val isLeft = true
        override val isRight = false
    }

    data class Right<out A, out B>(val b: B) : Either<A, B>() {
        override val isLeft = false
        override val isRight = true
    }


    interface FixBlock {

        operator fun <A, B> Either<A, B>.component1(): B =
            rightOrThrow()
    }


    class NoValueError(val other: Any?) : Exception()


    companion object {

        fun <A> left(left: A): Either<A, Nothing> = Left(left)

        fun <B> right(right: B): Either<Nothing, B> = Right(right)

        fun <A, B> tryOrLeft(left: A, block: () -> B): Either<A, B> = runCatching(block)
            .fold(::Right) { Left(left) }

        /**
         * Evaluate the result of [block] and shortcut if any error happens inside it.
         * @return [Either] of [E] and [B]
         */
        inline fun <A : E, B, E : Error> fix(block: FixBlock.() -> Either<A, B>): Either<E, B> =
            try {
                block(object : FixBlock {})
            } catch (e: NoValueError) {
                @Suppress("UNCHECKED_CAST")
                (e.other as E).left()
            }
    }
}

fun <A> A.left(): Either<A, Nothing> = Either.Left(this)
fun <B> B.right(): Either<Nothing, B> = Either.Right(this)
fun <A> Left(a: A): Either<A, Nothing> = Either.Left(a)
fun <B> Right(b: B): Either<Nothing, B> = Either.Right(b)


/**
 * Binds the given function across [Either.Right].
 *
 * @param f The function to bind across [Either.Right].
 */
inline fun <A, B, C> Either<A, B>.flatMap(f: (B) -> Either<A, C>): Either<A, C> =
    fold({ Left(it) }, { f(it) })

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

package ch.protonmail.android.domain.util

import arrow.core.Either
import arrow.core.Left
import arrow.core.Right
import assert4k.Null
import assert4k.`is`
import assert4k.assert
import assert4k.equals
import assert4k.fails
import assert4k.invoke
import assert4k.that
import assert4k.times
import assert4k.type
import assert4k.unaryPlus
import assert4k.with
import kotlin.test.Test

class EitherTest {

    // region orThrow
    @Test
    fun orThrowWorksCorrectlyIfRight() {
        // given
        val either: Either<Error, Int> = Right(2)

        // when
        val right = either.orThrow()

        // then
        assert that right equals 2
    }

    @Test
    fun orThrowWorksCorrectlyIfLeft() {
        // given
        class IntegerError : Error()
        val error = IntegerError()
        val either: Either<Error, Int> = Left(error)

        // when - then
        assert that fails<NoValueError> {
            either.orThrow()
        } with "IntegerError"
    }
    // endregion

    // region leftOrNull
    @Test
    fun leftOrNullWorksCorrectlyIfRight() {
        // given
        val either: Either<Error, Int> = Right(2)

        // when
        val left = either.leftOrNull()

        // then
        assert that left `is` Null
    }

    @Test
    fun leftOrNullWorksCorrectlyIfLeft() {
        // given
        val error = object : Error() {}
        val either: Either<Error, Int> = Left(error)

        // when
        val left = either.leftOrNull()

        // then
        assert that left equals error
    }
    // endregion

    // region leftOrThrow
    @Test
    fun leftOrThrowWorksCorrectlyIfRight() {
        // given
        val either: Either<Error, Int> = Right(2)

        // when - then
        assert that fails<NoValueError> {
            either.leftOrThrow()
        } with "Int"
    }

    @Test
    fun leftOrThrowWorksCorrectlyIfLeft() {
        // given
        val error = object : Error() {}
        val either: Either<Error, Int> = Left(error)

        // when
        val left = either.leftOrThrow()

        // then
        assert that left equals error
    }
    // endregion

    // region map
    @Test
    fun mapWorksCorrectlyIfRight() {
        // given
        val either: Either<Error, Int> = Right(2)

        // when
        val resultEither = either.map { it * 10 }

        // then
        assert that resultEither * {
            it `is` type<Either.Right<Int>>()
            +orThrow()() equals 20
        }
    }

    @Test
    fun mapWorksCorrectlyIfLeft() {
        // given
        val error = object : Error() {}
        val either: Either<Error, Int> = Left(error)

        // when
        val resultEither = either.map { it * 10 }

        // then
        assert that resultEither * {
            it `is` type<Either.Left<Error>>()
            +leftOrThrow()() equals error
        }
    }
    // endregion
}

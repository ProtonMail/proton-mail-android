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

import assert4k.`is`
import assert4k.assert
import assert4k.equals
import assert4k.invoke
import assert4k.that
import assert4k.times
import assert4k.type
import assert4k.unaryPlus
import kotlin.test.Test

class EitherTest {

    @Test
    fun mapWorksCorrectlyIfRight() {
        // given
        val either: Either<Error, Int> = Right(2)

        // when
        val resultEither = either.map { it * 10 }

        // then
        assert that resultEither * {
            it `is` type<Either.Right<Error, Int>>()
            +rightOrThrow()() equals 20
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
            it `is` type<Either.Left<Error, Int>>()
            +leftOrThrow()() equals error
        }
    }
}

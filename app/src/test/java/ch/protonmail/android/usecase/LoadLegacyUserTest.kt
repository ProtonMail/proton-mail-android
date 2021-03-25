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

package ch.protonmail.android.usecase

import arrow.core.Either
import assert4k.*
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.util.leftOrThrow
import io.mockk.*
import me.proton.core.test.kotlin.CoroutinesTest
import kotlin.test.*

/**
 * Test suite for [LoadLegacyUser]
 */
class LoadLegacyUserTest : CoroutinesTest {

    @Test
    fun returnsLegacyUserIfNoError() = coroutinesTest {
        // given
        val loadLegacyUser = LoadLegacyUser(
            loadLegacyUserDelegate = mockk {
                every { this@mockk.invoke(any()) } returns mockk()
            },
            dispatchers
        )

        // when
        val result = loadLegacyUser(Id("someId"))

        // then
        assert that result.isRight()
    }

    @Test
    fun returnsErrorIfDelegateThrowException() = coroutinesTest {
        // given
        val loadLegacyUser = LoadLegacyUser(
            loadLegacyUserDelegate = mockk {
                every { this@mockk.invoke(any()) } answers { throw IllegalStateException() }
            },
            dispatchers
        )

        // when
        val result = loadLegacyUser(Id("someId"))

        // then
        assert that result * {
            it `is` type<Either.Left<LoadUser.Error>>()
            +leftOrThrow() equals LoadUser.Error
        }
    }
}

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

package ch.protonmail.android.usecase

import arrow.core.Either
import assert4k.assert
import assert4k.that
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.test.kotlin.CoroutinesTest
import kotlin.test.Test

/**
 * Test suite for [LoadLegacyUser]
 */
class LoadLegacyUserTest : CoroutinesTest by CoroutinesTest() {

    @Test
    fun returnsLegacyUserIfNoError() = runTest {
        // given
        val loadLegacyUser = LoadLegacyUser(
            loadLegacyUserDelegate = mockk {
                every { this@mockk.invoke(any()) } returns Either.Right(mockk())
            },
            dispatchers
        )

        // when
        val result = loadLegacyUser(UserId("someId"))

        // then
        assert that result.isRight()
    }
}

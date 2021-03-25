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

import arrow.core.Left
import arrow.core.Right
import arrow.core.right
import assert4k.*
import ch.protonmail.android.api.AccountManager
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.domain.entity.user.User
import ch.protonmail.android.usecase.FindUserIdForUsername.Error.CantLoadUser
import ch.protonmail.android.usecase.FindUserIdForUsername.Error.UserNotFound
import io.mockk.*
import me.proton.core.test.kotlin.CoroutinesTest
import kotlin.test.*

/**
 * Test suite for [FindUserIdForUsername]
 */
class FindUserIdForUsernameTest : CoroutinesTest {

    // region users data
    private val user1 = TestData(Id("id"), Name("username"))
    private val users = setOf(user1)
    // endregion

    private val accountManager: AccountManager = mockk {
        coEvery { allSaved() } returns users.map { it.userId }.toSet()
    }
    private val loadUser: LoadUser = mockk {
        coEvery { this@mockk(any()) } answers {
            users.find { it.userId == firstArg() }
                ?.let {
                    mockk<User> {
                        every { id } returns it.userId
                        every { name } returns it.username
                    }.right()
                } ?: Left(LoadUser.Error)
        }
    }

    private val findUserIdForUsername = FindUserIdForUsername(accountManager, loadUser)

    @Test
    fun returnsUserNotFoundErrorIfUserIsNotSaved() = coroutinesTest {
        // when
        val result = findUserIdForUsername(Name("notSavedUsername"))

        // then
        assert that result equals Left(UserNotFound)
    }

    @Test
    fun returnsCantLoadUser() = coroutinesTest {
        // given
        coEvery { loadUser(any()) } returns Left(LoadUser.Error)

        // when
        val result = findUserIdForUsername(user1.username)

        // then
        assert that result equals Left(CantLoadUser)
    }

    @Test
    fun returnsCorrectResultIfNoErrors() = coroutinesTest {
        // when
        val result = findUserIdForUsername(user1.username)

        // then
        assert that result equals Right(user1.userId)
    }

    private data class TestData(
        val userId: Id,
        val username: Name
    )
}

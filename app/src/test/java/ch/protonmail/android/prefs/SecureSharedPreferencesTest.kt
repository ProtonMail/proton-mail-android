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

package ch.protonmail.android.prefs

import android.content.SharedPreferences
import assert4k.assert
import assert4k.equals
import assert4k.that
import ch.protonmail.android.core.Constants.Prefs.PREF_USER_ID
import ch.protonmail.android.core.Constants.Prefs.PREF_USER_NAME
import ch.protonmail.android.core.PREF_USERNAME
import io.mockk.every
import io.mockk.mockk
import me.proton.core.domain.entity.UserId
import me.proton.core.test.android.mocks.newMockSharedPreferences
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.util.android.sharedpreferences.isEmpty
import me.proton.core.util.android.sharedpreferences.set
import kotlin.test.Test

/**
 * Test suite for [SecureSharedPreferences]
 */
class SecureSharedPreferencesTest {

    /**
     * Test suite for [SecureSharedPreferences.UsernameToIdMigration]
     */
    class UsernameToIdMigrationTest : CoroutinesTest {

        private val user1 = "username1" to UserId("id1")
        private val user2 = "username2" to UserId("id2")

        private val user1Prefs = newMockSharedPreferences.apply {
            this[PREF_USER_ID] = user1.second.id
            this[PREF_USER_NAME] = user1.first
        }
        private val user2Prefs = newMockSharedPreferences.apply {
            this[PREF_USER_ID] = user2.second.id
            this[PREF_USER_NAME] = user2.first
        }

        private val preferencesFactory: SecureSharedPreferences.Factory = mockk {
            val appPreferences = newMockSharedPreferences
            val usersPreferences = mutableMapOf<UserId, SharedPreferences>()

            every { appPreferences() } returns appPreferences
            every { userPreferences(any()) } answers {
                usersPreferences.getOrPut(firstArg()) { newMockSharedPreferences }
            }
            every { _usernamePreferences(any()) } returns newMockSharedPreferences
            every { _usernamePreferences(user1.first) } returns user1Prefs
            every { _usernamePreferences(user2.first) } returns user2Prefs
        }
        private val migration =
            SecureSharedPreferences.UsernameToIdMigration(preferencesFactory, dispatchers)

        @Test
        fun `does remove old preferences`() = coroutinesTest {

            migration(listOf(user1, user2).map { it.first })

            assert that user1Prefs.isEmpty()
            assert that user2Prefs.isEmpty()
        }

        @Test
        fun `new preferences have all the values`() = coroutinesTest {

            migration(listOf(user1, user2).map { it.first })

            assert that preferencesFactory.userPreferences(user1.second).all equals mapOf(
                PREF_USER_ID to user1.second.id,
                PREF_USERNAME to user1.first,
                PREF_USER_NAME to user1.first
            )
            assert that preferencesFactory.userPreferences(user2.second).all equals mapOf(
                PREF_USER_ID to user2.second.id,
                PREF_USERNAME to user2.first,
                PREF_USER_NAME to user2.first
            )
        }

        @Test
        fun `returns correct map of usernames to ids`() = coroutinesTest {

            val result = migration(listOf(user1, user2).map { it.first })
            assert that result equals listOf(user1, user2).toMap()
        }

        @Test
        fun `correctly skips preferences without an user id`() = coroutinesTest {

            val result = migration(listOf(user1, user2).map { it.first } + "username3")
            assert that result equals listOf(user1, user2).toMap()
        }
    }
}

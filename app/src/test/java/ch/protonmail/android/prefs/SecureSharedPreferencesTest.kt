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

package ch.protonmail.android.prefs

import android.content.SharedPreferences
import android.util.Base64
import ch.protonmail.android.core.Constants.Prefs.PREF_USER_ID
import ch.protonmail.android.core.Constants.Prefs.PREF_USER_NAME
import ch.protonmail.android.core.PREF_USERNAME
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.util.android.sharedpreferences.isEmpty
import me.proton.core.util.android.sharedpreferences.set
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(Enclosed::class)
class SecureSharedPreferencesTest {

    @Suppress("DEPRECATION")
    class UsernameToIdMigrationTest : CoroutinesTest by CoroutinesTest() {

        private val user1 = "username1" to UserId("id1")
        private val user2 = "username2" to UserId("id2")

        private val user1Prefs = mockSecureSharedPreferences {
            this[PREF_USER_ID] = user1.second.id
            this[PREF_USER_NAME] = user1.first
        }
        private val user2Prefs = mockSecureSharedPreferences {
            this[PREF_USER_ID] = user2.second.id
            this[PREF_USER_NAME] = user2.first
        }

        private val preferencesFactory: SecureSharedPreferences.Factory = mockk {
            val appPreferences: SecureSharedPreferences = mockSecureSharedPreferences()
            val usersPreferences = mutableMapOf<UserId, SharedPreferences>()

            every { appPreferences() } returns appPreferences
            every { userPreferences(any()) } answers {
                usersPreferences.getOrPut(firstArg()) { mockSecureSharedPreferences() }
            }
            every { _usernamePreferences(any()) } returns mockSecureSharedPreferences()
            every { _usernamePreferences(user1.first) } returns user1Prefs
            every { _usernamePreferences(user2.first) } returns user2Prefs
        }
        private lateinit var migration: SecureSharedPreferences.UsernameToIdMigration

        @BeforeTest
        fun setup() {
            mockkStatic(Base64::class)
            every { Base64.encode(any(), any()) } answers { firstArg() }
            migration =
                SecureSharedPreferences.UsernameToIdMigration(preferencesFactory, dispatchers)
        }

        @AfterTest
        fun teardown() {
            unmockkStatic(Base64::class)
        }

        @Test
        fun `does remove old preferences`() = runTest {

            migration(listOf(user1, user2).map { it.first })

            assertTrue(user1Prefs.isEmpty())
            assertTrue(user2Prefs.isEmpty())
        }

        @Test
        fun `new preferences have all the values`() = runTest {
            // given
            val expectedUser1Preferences = mapOf(
                PREF_USER_ID to user1.second.id,
                PREF_USERNAME to user1.first,
                PREF_USER_NAME to user1.first
            )
            val expectedUser2Preferences = mapOf(
                PREF_USER_ID to user2.second.id,
                PREF_USERNAME to user2.first,
                PREF_USER_NAME to user2.first
            )

            // when
            migration(listOf(user1, user2).map { it.first })

            // then
            assertEquals(expectedUser1Preferences, preferencesFactory.userPreferences(user1.second).all)
            assertEquals(expectedUser2Preferences, preferencesFactory.userPreferences(user2.second).all)
        }

        @Test
        fun `returns correct map of usernames to ids`() = runTest {
            // given
            val expected = listOf(user1, user2).toMap()

            // when
            val result = migration(listOf(user1, user2).map { it.first })

            // then
            assertEquals(expected, result)
        }

        @Test
        fun `correctly skips preferences without an user id`() = runTest {
            // given
            val expected = listOf(user1, user2).toMap()

            // when
            val result = migration(listOf(user1, user2).map { it.first } + "username3")

            // then
            assertEquals(expected, result)
        }

        @Test
        fun `correctly skips preferences with mismatching user id`() = runTest {
            // given
            every { preferencesFactory._usernamePreferences(user1.first) } returns mockSecureSharedPreferences()
            val expected = listOf(user2).toMap()

            // when
            val result = migration(listOf(user1, user2).map { it.first })

            // then
            assertEquals(expected, result)
        }
    }
}

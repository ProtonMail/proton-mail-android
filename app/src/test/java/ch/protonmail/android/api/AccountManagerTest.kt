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

package ch.protonmail.android.api

import android.content.SharedPreferences
import androidx.core.content.edit
import assert4k.Null
import assert4k.`is`
import assert4k.assert
import assert4k.contains
import assert4k.equals
import assert4k.invoke
import assert4k.that
import assert4k.times
import assert4k.unaryPlus
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.prefs.SecureSharedPreferences
import ch.protonmail.android.utils.getStringList
import ch.protonmail.android.utils.putStringList
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.test.android.mocks.newMockSharedPreferences
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.BeforeTest
import kotlin.test.Test

class AccountManagerTest : CoroutinesTest by CoroutinesTest() {

    class UsernameToIdMigrationTest :
        CoroutinesTest by CoroutinesTest({ TestDispatcherProvider(UnconfinedTestDispatcher()) }) {

        private val user1 = "username1" to UserId("id1")
        private val user2 = "username2" to UserId("id2")
        private val user3 = "username3" to UserId("id3")
        private val user4 = "username4" to UserId("id4")

        private lateinit var defaultPreferences: SharedPreferences
        private lateinit var accountManager: AccountManager
        private lateinit var secureSharedPreferencesMigration: SecureSharedPreferences.UsernameToIdMigration
        private lateinit var userManagerMigration: UserManager.UsernameToIdMigration
        private lateinit var migration: AccountManager.UsernameToIdMigration

        @BeforeTest
        fun setUp() {
            defaultPreferences = newMockSharedPreferences
            accountManager = AccountManager(defaultPreferences, dispatchers)
            secureSharedPreferencesMigration = mockk {
                coEvery { this@mockk(any()) } returns mapOf(user1, user2, user3, user4)
            }
            userManagerMigration = mockk(relaxed = true)
            migration = AccountManager.UsernameToIdMigration(
                dispatchers = dispatchers,
                accountManager = accountManager,
                secureSharedPreferencesMigration = secureSharedPreferencesMigration,
                userManagerMigration = userManagerMigration,
                defaultSharedPreferences = defaultPreferences.apply {
                    edit {
                        putStringList(PREF_USERNAMES_LOGGED_IN, listOf(user1, user2).map { it.first })
                        putStringList(PREF_USERNAMES_LOGGED_OUT, listOf(user3, user4).map { it.first })
                    }
                }
            )
        }

        @Test
        fun doesRemoveOldPreferences() = runTest {

            migration()

            assert that defaultPreferences.getStringList(PREF_USERNAMES_LOGGED_IN, null) `is` Null
            assert that defaultPreferences.getStringList(PREF_USERNAMES_LOGGED_OUT, null) `is` Null
        }

        @Test
        fun newPreferencesHaveAllTheValues() = runTest {

            migration()

            assert that accountManager.allLoggedInForTest() * {
                +size() equals 2
                it contains user1.second
                it contains user2.second
            }
            assert that accountManager.allLoggedOutForTest() * {
                +size() equals 2
                it contains user3.second
                it contains user4.second
            }
        }

        @Test
        fun correctlySkipsPreferencesWithoutAnUserId() = runTest {
            coEvery { secureSharedPreferencesMigration(any()) } returns mapOf(user1, user3)

            migration()

            assert that accountManager.allLoggedInForTest() equals setOf(user1.second)
            assert that accountManager.allLoggedOutForTest() equals setOf(user3.second)
        }
    }
}

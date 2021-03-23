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

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import android.util.Base64
import assert4k.assert
import assert4k.equals
import assert4k.that
import ch.protonmail.android.core.Constants.Prefs.PREF_USER_ID
import ch.protonmail.android.core.Constants.Prefs.PREF_USER_NAME
import ch.protonmail.android.core.PREF_USERNAME
import ch.protonmail.android.domain.entity.Id
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkObject
import io.mockk.unmockkStatic
import me.proton.core.test.android.mocks.newMockSharedPreferences
import me.proton.core.test.kotlin.CoroutinesTest
import me.proton.core.util.android.sharedpreferences.isEmpty
import me.proton.core.util.android.sharedpreferences.set
import org.junit.After
import org.junit.Before
import kotlin.test.Test

class SecureSharedPreferencesTest {

    class UsernameToIdMigrationTest : CoroutinesTest {

        private val user1 = "username1" to Id("id1")
        private val user2 = "username2" to Id("id2")

        private val user1Prefs = newMockSharedPreferences.apply {
            this[PREF_USER_ID] = user1.second.s
            this[PREF_USER_NAME] = user1.first
        }
        private val user2Prefs = newMockSharedPreferences.apply {
            this[PREF_USER_ID] = user2.second.s
            this[PREF_USER_NAME] = user2.first
        }

        private val context = mockk<Context> {
            val allPrefs = mutableMapOf<String, SharedPreferences>()

            every { applicationContext } returns this
            // All preferences
            every { getSharedPreferences(any(), any()) } answers {
                allPrefs.getOrPut(firstArg()) { newMockSharedPreferences }
            }
            // Users' old preferences
            every { getSharedPreferences(user1.first, any()) } returns user1Prefs
            every { getSharedPreferences(user2.first, any()) } returns user2Prefs
        }
        private val migration =
            SecureSharedPreferences.UsernameToIdMigration(dispatchers, context)

        @Before
        fun before() {

            // Setup static methods
            mockkStatic(Base64::class, PreferenceManager::class)
            every { Base64.encodeToString(any(), any()) } answers { firstArg<ByteArray>().toString(Charsets.UTF_8) }
            every { PreferenceManager.getDefaultSharedPreferences(any()) } returns
                context.getSharedPreferences("default", 0)

            // Setup SecureSharedPreferences accessors
            mockkObject(SecureSharedPreferences.Companion)
            every { SecureSharedPreferences.getPrefs(any(), any(), any()) } returns
                PreferenceManager.getDefaultSharedPreferences(context)
            every { SecureSharedPreferences.getPrefsForUser(any(), any<Id>()) } answers {
                context.getSharedPreferences(secondArg<Id>().s, 0)
            }
            every { SecureSharedPreferences["_getPrefsForUser"](any<Context>(), any<String>()) } answers {
                context.getSharedPreferences(secondArg(), 0)
            }
        }

        @After
        fun after() {
            unmockkStatic(Base64::class, PreferenceManager::class)
            unmockkObject(SecureSharedPreferences.Companion)
        }

        @Test
        fun `does remove old preferences`() = coroutinesTest {

            migration(listOf(user1, user2).map { it.first })

            assert that user1Prefs.isEmpty()
            assert that user2Prefs.isEmpty()
        }

        @Test
        fun `new preferences have all the values`() = coroutinesTest {

            migration(listOf(user1, user2).map { it.first })

            assert that SecureSharedPreferences.getPrefsForUser(context, user1.second).all equals mapOf(
                PREF_USER_ID to user1.second.s,
                PREF_USERNAME to user1.first,
                PREF_USER_NAME to user1.first
            )
            assert that SecureSharedPreferences.getPrefsForUser(context, user2.second).all equals mapOf(
                PREF_USER_ID to user2.second.s,
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

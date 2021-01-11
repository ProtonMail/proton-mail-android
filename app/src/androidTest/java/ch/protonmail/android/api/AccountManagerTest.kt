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
package ch.protonmail.android.api

import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import junit.framework.Assert.assertEquals
import kotlin.test.BeforeTest
import kotlin.test.Test

@LargeTest
internal class AccountManagerTest_Instrumented {

    private val accountManager = AccountManager.getInstance(InstrumentationRegistry.getInstrumentation().targetContext)

    @BeforeTest
    fun clear() {
        accountManager.clear()
    }

    @Test
    fun onSuccessfulLogin() {
        accountManager.onSuccessfulLogin("alice")
        accountManager.onSuccessfulLogin("bob")
        accountManager.onSuccessfulLogin("charlie")

        assertEquals(listOf("alice", "bob", "charlie"), accountManager.getLoggedInUsers())
        assertEquals(emptyList<String>(), accountManager.getSavedUsers())
    }

    @Test
    fun onSuccessfulLogout() {
        accountManager.onSuccessfulLogin("alice")
        accountManager.onSuccessfulLogin("bob")
        accountManager.onSuccessfulLogin("charlie")

        accountManager.onSuccessfulLogout("bob")

        assertEquals(listOf("alice", "charlie"), accountManager.getLoggedInUsers())
        assertEquals(listOf("bob"), accountManager.getSavedUsers())
    }

    @Test
    fun removingFromSavedList() {
        accountManager.onSuccessfulLogin("alice")

        assertEquals(listOf("alice"), accountManager.getLoggedInUsers())
        assertEquals(emptyList<String>(), accountManager.getSavedUsers())

        accountManager.onSuccessfulLogout("alice")

        assertEquals(emptyList<String>(), accountManager.getLoggedInUsers())
        assertEquals(listOf("alice"), accountManager.getSavedUsers())

        accountManager.removeFromSaved("alice")

        assertEquals(emptyList<String>(), accountManager.getLoggedInUsers())
        assertEquals(emptyList<String>(), accountManager.getSavedUsers())
    }

    @Test
    fun ignoreDuplicates() {
        accountManager.onSuccessfulLogin("alice")
        accountManager.onSuccessfulLogin("alice")

        assertEquals(listOf("alice"), accountManager.getLoggedInUsers())
    }

    @Test
    fun ignoreInvalidUsernames() {
        accountManager.onSuccessfulLogin("alice")
        accountManager.onSuccessfulLogout("bob")

        assertEquals(listOf("alice"), accountManager.getLoggedInUsers())
    }

    @Test
    fun emptyListsAreHandledCorrectly() {
        assertEquals(emptyList<String>(), accountManager.getLoggedInUsers())
        assertEquals(emptyList<String>(), accountManager.getSavedUsers())
    }

}

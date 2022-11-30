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

package ch.protonmail.android.notifications.data.remote.fcm

import android.content.SharedPreferences
import ch.protonmail.android.notifications.data.remote.fcm.model.FirebaseToken
import ch.protonmail.android.prefs.SecureSharedPreferences
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.account.domain.entity.Account
import me.proton.core.account.domain.entity.AccountState
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.accountmanager.domain.getAccounts
import me.proton.core.domain.entity.UserId
import me.proton.core.test.android.mocks.newMockSharedPreferences
import me.proton.core.test.kotlin.CoroutinesTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class MultiUserFcmTokenManagerTest : CoroutinesTest by CoroutinesTest() {

    private val user1 = DataSet(
        UserId("1"),
        userFcmTokenManager = mockk(relaxUnitFun = true),
        sharedPreferences = newMockSharedPreferences
    )
    private val user2 = DataSet(
        UserId("2"),
        userFcmTokenManager = mockk(relaxUnitFun = true),
        sharedPreferences = newMockSharedPreferences
    )
    private val user3 = DataSet(
        UserId("3"),
        userFcmTokenManager = mockk(relaxUnitFun = true),
        sharedPreferences = newMockSharedPreferences
    )
    private val user4 = DataSet(
        UserId("4"),
        userFcmTokenManager = mockk(relaxUnitFun = true),
        sharedPreferences = newMockSharedPreferences
    )

    private val allLoggedInUsers = setOf(user1, user2, user3)

    private val accounts = allLoggedInUsers.map { user ->
        mockk<Account> {
            every { userId } returns UserId(user.userId.id)
            every { state } returns AccountState.Ready
        }
    }

    private val accountManager: AccountManager = mockk(relaxed = true) {
        coEvery { getAccounts(AccountState.Ready) } returns flowOf(accounts)
        coEvery { getAccounts() } returns flowOf(accounts)
    }

    private val preferencesFactory: SecureSharedPreferences.Factory = mockk {
        every { userPreferences(any()) } answers {
            val userIdParam = firstArg<UserId>()
            allLoggedInUsers.first { it.userId == userIdParam }.sharedPreferences
        }
    }

    private val multiUserTokenManager = MultiUserFcmTokenManager(
        accountManager,
        preferencesFactory,
        userFcmTokenManagerFactory = mockk {
            every { create(any()) } answers {
                val sharedPreferencesParam = firstArg<SharedPreferences>()
                allLoggedInUsers.first { it.sharedPreferences == sharedPreferencesParam }.userFcmTokenManager
            }
        }
    )

    @Test
    fun storesFirebaseTokenForAllTheLoggedInUsers() = runTest {
        // given
        val token = FirebaseToken("a token")
        multiUserTokenManager.saveToken(token)

        // when - then
        coVerify {
            user1.userFcmTokenManager.saveToken(token)
            user2.userFcmTokenManager.saveToken(token)
            user3.userFcmTokenManager.saveToken(token)
        }
        coVerify(exactly = 0) {
            user4.userFcmTokenManager.saveToken(any())
        }
    }

    @Test
    fun isTokenSentForAllLoggedUsersReturnsTrueIfTokenIsSentForEveryLoggedInUser() = runTest {
        // given
        coEvery { user1.userFcmTokenManager.isTokenSent() } returns true
        coEvery { user2.userFcmTokenManager.isTokenSent() } returns true
        coEvery { user3.userFcmTokenManager.isTokenSent() } returns true

        // when
        val result = multiUserTokenManager.isTokenSentForAllLoggedUsers()

        assertTrue(result)
    }

    @Test
    fun isTokenSentForAllLoggedUsersReturnsTrueIfTokenIsSentForEveryLoggedInUserButForSavedUser() = runTest {
        // given
        coEvery { user1.userFcmTokenManager.isTokenSent() } returns true
        coEvery { user2.userFcmTokenManager.isTokenSent() } returns true
        coEvery { user3.userFcmTokenManager.isTokenSent() } returns true
        coEvery { user4.userFcmTokenManager.isTokenSent() } returns false

        // when
        val result = multiUserTokenManager.isTokenSentForAllLoggedUsers()

        assertTrue(result)
    }

    @Test
    fun isTokenSentForAllLoggedUsersReturnsFalseIfTokenIsNotSentForOneLoggedInUser() = runTest {
        // given
        coEvery { user1.userFcmTokenManager.isTokenSent() } returns true
        coEvery { user2.userFcmTokenManager.isTokenSent() } returns false
        coEvery { user3.userFcmTokenManager.isTokenSent() } returns true

        // when
        val result = multiUserTokenManager.isTokenSentForAllLoggedUsers()

        assertFalse(result)
    }

    @Test
    fun isTokenSentForAllLoggedUsersReturnsFalseIfTokenIsNotSentForEveryLoggedInUser() = runTest {
        // given
        coEvery { user1.userFcmTokenManager.isTokenSent() } returns false
        coEvery { user2.userFcmTokenManager.isTokenSent() } returns false
        coEvery { user3.userFcmTokenManager.isTokenSent() } returns false

        // when
        val result = multiUserTokenManager.isTokenSentForAllLoggedUsers()

        assertFalse(result)
    }

    @Test
    fun setTokenUnsentForAllLoggedInUsersWorksCorrectly() = runTest {
        // given - then
        multiUserTokenManager.setTokenUnsentForAllSavedUsers()

        // when
        coVerify {
            user1.userFcmTokenManager.setTokenSent(false)
            user2.userFcmTokenManager.setTokenSent(false)
            user3.userFcmTokenManager.setTokenSent(false)
        }
        coVerify(exactly = 0) {
            user4.userFcmTokenManager.setTokenSent(false)
        }
    }


    data class DataSet(
        val userId: UserId,
        val sharedPreferences: SharedPreferences,
        val userFcmTokenManager: FcmTokenManager
    )
}

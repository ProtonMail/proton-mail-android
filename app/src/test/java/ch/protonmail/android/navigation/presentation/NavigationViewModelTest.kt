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

package ch.protonmail.android.navigation.presentation

import android.content.Context
import android.content.SharedPreferences
import app.cash.turbine.test
import ch.protonmail.android.R
import ch.protonmail.android.core.Constants
import ch.protonmail.android.feature.account.AccountStateManager
import ch.protonmail.android.prefs.SecureSharedPreferences
import ch.protonmail.android.testdata.UserIdTestData
import ch.protonmail.android.usecase.IsAppInDarkMode
import ch.protonmail.android.utils.notifier.UserNotifier
import io.mockk.called
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.report.presentation.entity.BugReportOutput
import me.proton.core.test.android.ArchTest
import me.proton.core.test.kotlin.CoroutinesTest
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigationViewModelTest : ArchTest, CoroutinesTest {

    private val isAppInDarkMode: IsAppInDarkMode = mockk()

    private val sharedPrefsMock = mockk<SharedPreferences>()
    private val sharedPreferencesFactoryMock = mockk<SecureSharedPreferences.Factory> {
        every { userPreferences(UserIdTestData.userId) } returns sharedPrefsMock
    }
    private val accountStateManagerMock = mockk<AccountStateManager> {
        every { signOut(UserIdTestData.userId) } returns mockk()
    }
    private val userNotifierMock = mockk<UserNotifier> {
        every { showError(R.string.logged_out_description) } just runs
    }
    private val navigationViewModel = NavigationViewModel(
        isAppInDarkMode,
        sharedPreferencesFactoryMock,
        accountStateManagerMock,
        userNotifierMock,
        dispatchers
    )

    @Test
    fun `should not log out and return true if user id verified correctly`() = runBlockingTest {
        // given
        every { sharedPrefsMock.getString(Constants.Prefs.PREF_USER_NAME, null) } returns USERNAME

        // when
        val userIdVerified = navigationViewModel.verifyPrimaryUserId(UserIdTestData.userId)

        // then
        assertTrue(userIdVerified)
        verify { accountStateManagerMock wasNot called }
    }

    @Test
    fun `should log out, show error, and return false if user id not verified correctly`() = runBlockingTest {
        // given
        every { sharedPrefsMock.getString(Constants.Prefs.PREF_USER_NAME, null) } returns null

        // when
        val userIdVerified = navigationViewModel.verifyPrimaryUserId(UserIdTestData.userId)

        // then
        assertFalse(userIdVerified)
        verify { accountStateManagerMock.signOut(UserIdTestData.userId) }
        verify { userNotifierMock.showError(R.string.logged_out_description) }
    }

    @Test
    fun `should check if app is in dark mode`() {
        // given
        val contextMock = mockk<Context>()
        every { isAppInDarkMode(contextMock) } returns true

        // when
        val isAppInDarkMode = navigationViewModel.isAppInDarkMode(contextMock)

        // then
        assertTrue(isAppInDarkMode)
    }

    @Test
    fun `should emit success message when sending bug report is successful`() = runBlockingTest {
        navigationViewModel.bugReportResultMessageFlow.test {
            // when
            navigationViewModel.onBugReportSent(BugReportOutput.SuccessfullySent(""))

            // then
            assertEquals(R.string.received_report, awaitItem())
        }
    }

    @Test
    fun `should emit error message when sending bug report is cancelled`() = runBlockingTest {
        navigationViewModel.bugReportResultMessageFlow.test {
            // when
            navigationViewModel.onBugReportSent(BugReportOutput.Cancelled)

            // then
            assertEquals(R.string.not_received_report, awaitItem())
        }
    }

    private companion object TestData {

        const val USERNAME = "username"
    }
}

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
package ch.protonmail.android.uitests.tests.login

import android.preference.PreferenceManager
import androidx.test.filters.LargeTest
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.rule.ActivityTestRule
import ch.protonmail.android.activities.guest.LoginActivity
import ch.protonmail.android.uitests.actions.LoginRobot
import ch.protonmail.android.uitests.results.LoginResult
import ch.protonmail.android.uitests.testsHelper.TestUser.Companion.onePassUser
import ch.protonmail.android.uitests.testsHelper.TestUser.Companion.onePassUserWith2FA
import ch.protonmail.android.uitests.testsHelper.TestUser.Companion.twoPassUser
import ch.protonmail.android.uitests.testsHelper.TestUser.Companion.twoPassUserWith2FA
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4ClassRunner::class)
@LargeTest
class LoginTests {

    private val result = LoginResult()
    private val mainAppContext = InstrumentationRegistry.getInstrumentation().targetContext

    @get:Rule
    val activityRule = ActivityTestRule(LoginActivity::class.java)

    @Before
    fun clearSharedPrefs() {
        PreferenceManager.getDefaultSharedPreferences(mainAppContext).edit().clear().apply()
    }

    @Test
    fun loginWithOnePass() {
        LoginRobot()
            .username(onePassUser().name)
            .password(onePassUser().password)
            .signIn()
        result.isLoginSuccessful
    }

    @Test
    fun loginWithTwoPass() {
        LoginRobot()
            .username(twoPassUser().name)
            .password(twoPassUser().password)
            .signIn()
            .mailboxPassword(twoPassUser().mailboxPassword)
            .decrypt()
        result.isLoginSuccessful
    }

    @Test
    fun loginWithOnePassAnd2FA() {
        LoginRobot()
            .username(onePassUserWith2FA().name)
            .password(onePassUserWith2FA().password)
            .signIn()
            .twoFACode(onePassUserWith2FA().twoFACode)
            .confirm2FA()
        result.isLoginSuccessful
    }

    @Test
    fun loginWithTwoPassAnd2FA() {
        LoginRobot()
            .username(twoPassUserWith2FA().name)
            .password(twoPassUserWith2FA().password)
            .signIn()
            .twoFACode(twoPassUserWith2FA().twoFACode)
            .confirm2FA()
            .mailboxPassword(twoPassUserWith2FA().mailboxPassword)
            .decrypt()
        result.isLoginSuccessful
    }
}

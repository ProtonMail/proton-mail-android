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

import androidx.test.filters.LargeTest
import ch.protonmail.android.uitests.robots.login.LoginRobot
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.TestUser.Companion.onePassUser
import ch.protonmail.android.uitests.testsHelper.TestUser.Companion.onePassUserWith2FA
import ch.protonmail.android.uitests.testsHelper.TestUser.Companion.twoPassUser
import ch.protonmail.android.uitests.testsHelper.TestUser.Companion.twoPassUserWith2FA
import org.junit.Test

@LargeTest
class LoginTests : BaseTest() {

    private val loginRobot = LoginRobot()

    @Test
    fun loginWithOnePass() {
        loginRobot
            .loginUser(onePassUser())
            .verify { mailboxLayoutShown() }
    }

    @Test
    fun loginWithTwoPass() {
        loginRobot
            .loginTwoPasswordUser(twoPassUser())
            .verify { mailboxLayoutShown() }
    }

    @Test
    fun loginWithOnePassAnd2FA() {
        loginRobot
            .loginUserWithTwoFA(onePassUserWith2FA())
            .verify { mailboxLayoutShown() }
    }

    @Test
    fun loginWithTwoPassAnd2FA() {
        loginRobot
            .loginTwoPasswordUserWithTwoFA(twoPassUserWith2FA())
            .verify { mailboxLayoutShown() }
    }
}

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

import ch.protonmail.android.uitests.robots.login.LoginRobot
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.TestData.onePassUser
import ch.protonmail.android.uitests.testsHelper.TestData.onePassUserWith2FA
import ch.protonmail.android.uitests.testsHelper.TestData.twoPassUser
import ch.protonmail.android.uitests.testsHelper.TestData.twoPassUserWith2FA
import ch.protonmail.android.uitests.testsHelper.annotations.SmokeTest
import kotlin.test.Test
import org.junit.experimental.categories.Category

class LoginTests : BaseTest() {

    private val loginRobot = LoginRobot()

    @Category(SmokeTest::class)
    @Test
    fun loginWithOnePass() {
        loginRobot
            .loginUser(onePassUser)
            .verify { mailboxLayoutShown() }
    }

    @Category(SmokeTest::class)
    @Test
    fun loginWithTwoPass() {
        loginRobot
            .loginTwoPasswordUser(twoPassUser)
            .decryptMailbox(twoPassUser.mailboxPassword)
            .verify { mailboxLayoutShown() }
    }

    @Category(SmokeTest::class)
    @Test
    fun loginWithOnePassAnd2Fa() {
        loginRobot
            .loginUserWithTwoFa(onePassUserWith2FA)
            .provideTwoFaCode(onePassUserWith2FA.twoFaCode)
            .verify { mailboxLayoutShown() }
    }

    fun loginWithTwoPassAnd2Fa() {
        loginRobot
            .loginUserWithTwoFa(twoPassUserWith2FA)
            .provideTwoFaCodeMailbox(twoPassUserWith2FA.twoFaCode)
            .decryptMailbox(twoPassUserWith2FA.mailboxPassword)
            .verify { mailboxLayoutShown() }
    }
}

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
package ch.protonmail.android.uitests.tests.manageaccounts

import ch.protonmail.android.uitests.robots.login.LoginRobot
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.TestData.onePassUser
import ch.protonmail.android.uitests.testsHelper.TestData.onePassUserWith2FA
import ch.protonmail.android.uitests.testsHelper.TestData.twoPassUser
import ch.protonmail.android.uitests.testsHelper.TestData.twoPassUserWith2FA
import ch.protonmail.android.uitests.testsHelper.annotations.SmokeTest
import ch.protonmail.android.uitests.testsHelper.annotations.TestId
import kotlin.test.Test
import org.junit.experimental.categories.Category

class MultiuserManagementTests : BaseTest() {

    private val loginRobot = LoginRobot()

    @TestId("30793")
    @Test
    fun connectOnePassAccount() {
        loginRobot
            .loginTwoPasswordUser(twoPassUser)
            .decryptMailbox(twoPassUser.mailboxPassword)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .addAccount()
            .connectOnePassAccount(onePassUser)
            .menuDrawer()
            .accountsList()
            .verify { accountAdded(onePassUser.email) }
    }

    @TestId("30794")
    @Test
    fun connectTwoPassAccount() {
        loginRobot
            .loginUser(onePassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .addAccount()
            .connectTwoPassAccount(twoPassUser)
            .menuDrawer()
            .accountsList()
            .verify { accountAdded(twoPassUser.email) }
    }

    @TestId("30795")
    @Test
    fun connectOnePassAccountWithTwoFa() {
        loginRobot
            .loginUser(onePassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .addAccount()
            .connectOnePassAccountWithTwoFa(onePassUserWith2FA)
            .menuDrawer()
            .accountsList()
            .verify { accountAdded(onePassUserWith2FA.email) }
    }

    @TestId("1574")
    @Test
    fun connectTwoPassAccountWithTwoFa() {
        loginRobot
            .loginUser(onePassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .addAccount()
            .connectTwoPassAccountWithTwoFa(twoPassUserWith2FA)
            .menuDrawer()
            .accountsList()
            .verify { accountAdded(twoPassUserWith2FA.email) }
    }

    @TestId("1572")
    @Test
    fun removeAllAccounts() {
        loginRobot
            .loginUser(onePassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .removeAllAccounts()
            .verify { loginScreenDisplayed() }
    }

    @TestId("30796")
    @Category(SmokeTest::class)
    @Test
    fun logoutPrimaryAccount() {
        loginRobot
            .loginUser(onePassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .addAccount()
            .connectTwoPassAccount(twoPassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .logoutAccount(twoPassUser.email)
            .menuDrawer()
            .accountsList()
            .verify { accountLoggedOut(twoPassUser.name) }
    }

    @TestId("30797")
    @Test
    fun logoutSecondaryAccount() {
        loginRobot
            .loginUser(onePassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .addAccount()
            .connectTwoPassAccount(twoPassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .logoutAccount(onePassUser.email)
            .menuDrawer()
            .accountsList()
            .verify { accountLoggedOut(onePassUser.name) }
    }

    @TestId("30798")
    @Test
    fun logoutOnlyRemainingAccount() {
        loginRobot
            .loginUser(onePassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .logoutLastAccount(onePassUser.email)
            .verify { loginScreenDisplayed() }
    }

    @TestId("30799")
    @Category(SmokeTest::class)
    @Test
    fun removePrimaryAccount() {
        loginRobot.loginUser(onePassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .addAccount()
            .connectOnePassAccountWithTwoFa(onePassUserWith2FA)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .removeAccount(onePassUserWith2FA.email)
            .menuDrawer()
            .accountsList()
            .verify { accountRemoved(onePassUserWith2FA.name) }
    }

    @TestId("30800")
    @Test
    fun removeSecondaryAccount() {
        loginRobot
            .loginUser(onePassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .addAccount()
            .connectTwoPassAccount(twoPassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .removeAccount(onePassUser.email)
            .menuDrawer()
            .accountsList()
            .verify { accountRemoved(onePassUser.name) }
    }

    @TestId("30801")
    @Test
    fun removeOnlyRemainingAccount() {
        loginRobot
            .loginUser(onePassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .removeLastAccount(onePassUser.email)
            .verify { loginScreenDisplayed() }
    }

    @TestId("1621")
    @Test
    fun logoutOneRemoveAnotherAccount() {
        loginRobot
            .loginUser(onePassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .addAccount()
            .connectTwoPassAccount(twoPassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .logoutAccount(twoPassUser.email)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .logoutLastAccount(onePassUser.email)
            .verify { loginScreenDisplayed() }
    }

    @TestId("1736")
    @Test
    fun cancelLoginOnTwoFaPrompt() {
        loginRobot
            .loginUser(onePassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .addAccount()
            .cancelLoginOnTwoFaPrompt(onePassUserWith2FA)
            .menuDrawer()
            .accountsList()
            .verify { accountLoggedOut(onePassUserWith2FA.name) }
    }

    @TestId("30802")
    @Test
    fun switchAccount() {
        loginRobot
            .loginUser(onePassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .addAccount()
            .connectTwoPassAccount(twoPassUser)
            .menuDrawer()
            .accountsList()
            .switchToAccount(1)
            .accountsList()
            .manageAccounts()
            .verify { switchedToAccount(onePassUser.name) }
    }

    @TestId("1566")
    @Test
    fun addTwoFreeAccounts() {
        loginRobot
            .loginUserWithTwoFa(twoPassUserWith2FA)
            .provideTwoFaCodeMailbox(twoPassUserWith2FA.twoFaCode)
            .decryptMailbox(twoPassUserWith2FA.mailboxPassword)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .addAccount()
            .connectSecondFreeOnePassAccountWithTwoFa(onePassUserWith2FA)
            .verify { limitReachedDialogDisplayed() }
    }
}

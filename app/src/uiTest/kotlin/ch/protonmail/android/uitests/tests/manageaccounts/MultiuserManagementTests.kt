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
package ch.protonmail.android.uitests.tests.manageaccounts

import ch.protonmail.android.uitests.robots.login.LoginMailRobot
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.TestUser.onePassUser
import ch.protonmail.android.uitests.testsHelper.TestUser.onePassUserWith2FA
import ch.protonmail.android.uitests.testsHelper.TestUser.twoPassUser
import ch.protonmail.android.uitests.testsHelper.TestUser.twoPassUserWith2FA
import ch.protonmail.android.uitests.testsHelper.annotations.SmokeTest
import ch.protonmail.android.uitests.testsHelper.annotations.TestId
import org.junit.experimental.categories.Category
import kotlin.test.Test

class MultiuserManagementTests : BaseTest() {

    private val loginRobot = LoginMailRobot()

    @TestId("30793")
    @Test
    fun connectOnePassAccount() {
        loginRobot
            .loginTwoPassUser()
            .skipOnboarding()
            .menuDrawer()
            .accountsList()
            .addAccount()
            .addOnePassUser()
            .menuDrawer()
            .accountsList()
            .verify { accountAdded(onePassUser.email) }
    }

    @TestId("30794")
    @Test
    fun connectTwoPassAccount() {
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .menuDrawer()
            .accountsList()
            .addAccount()
            .addTwoPassUser()
            .menuDrawer()
            .accountsList()
            .verify { accountAdded(twoPassUser.email) }
    }

    @TestId("30795")
    @Test
    fun connectOnePassAccountWithTwoFa() {
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .menuDrawer()
            .accountsList()
            .addAccount()
            .addOnePassUserWith2FA()
            .menuDrawer()
            .accountsList()
            .verify { accountAdded(onePassUserWith2FA.email) }
    }

    @TestId("1574")
    @Test
    fun connectTwoPassAccountWithTwoFa() {
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .menuDrawer()
            .accountsList()
            .addAccount()
            .addTwoPassUserWith2FA()
            .menuDrawer()
            .accountsList()
            .verify { accountAdded(twoPassUserWith2FA.email) }
    }

    @TestId("30796")
    @Test
    fun logoutPrimaryAccount() {
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .menuDrawer()
            .accountsList()
            .addAccount()
            .addTwoPassUser()
            .menuDrawer()
            .accountsList()
            .logoutAccount(twoPassUser.email)
            .menuDrawer()
            .accountsList()
            .verify { accountLoggedOut(twoPassUser.name) }
    }

    @TestId("30797")
    @Test
    fun logoutSecondaryAccount() {
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .menuDrawer()
            .accountsList()
            .addAccount()
            .addTwoPassUser()
            .menuDrawer()
            .accountsList()
            .logoutSecondaryAccount(onePassUser.email)
            .verify { accountLoggedOut(onePassUser.name) }
    }

    @TestId("30798")
    @Test
    fun logoutOnlyRemainingAccount() {
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .menuDrawer()
            .accountsList()
            .logoutLastAccount(onePassUser.email)
            .verify { loginScreenDisplayed() }
    }

    @TestId("30799")
    @Test
    fun removePrimaryAccount() {
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .menuDrawer()
            .accountsList()
            .addAccount()
            .addOnePassUserWith2FA()
            .menuDrawer()
            .accountsList()
            .removeAccount(onePassUserWith2FA.email)
            .verify { accountRemoved(onePassUserWith2FA.email) }
    }

    @TestId("30800")
    @Test
    fun removeSecondaryAccount() {
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .menuDrawer()
            .accountsList()
            .addAccount()
            .addTwoPassUser()
            .menuDrawer()
            .accountsList()
            .removeAccount(onePassUser.email)
            .verify { accountRemoved(onePassUser.email) }
    }

    @TestId("30801")
    @Test
    fun removeOnlyRemainingAccount() {
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .menuDrawer()
            .accountsList()
            .removeLastAccount(onePassUser.email)
            .verify { loginScreenDisplayed() }
    }

    @TestId("1621")
    @Test
    fun logoutOneRemoveAnotherAccount() {
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .menuDrawer()
            .accountsList()
            .addAccount()
            .addTwoPassUser()
            .menuDrawer()
            .accountsList()
            .logoutAccount(twoPassUser.email)
            .menuDrawer()
            .accountsList()
            .removeLastAccount(onePassUser.email)
            .verify { loginScreenDisplayed() }
    }

    @TestId("1736")
    @Test
    fun cancelLoginOnTwoFaPrompt() {
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .menuDrawer()
            .accountsList()
            .addAccount()
            .cancelLoginOnTwoFaPrompt()
            .menuDrawer()
            .accountsList()
            .verify { accountLoggedOut(onePassUserWith2FA.name) }
    }

    @TestId("30802")
    @Test
    fun switchAccount() {
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .menuDrawer()
            .accountsList()
            .addAccount()
            .addTwoPassUser()
            .menuDrawer()
            .accountsList()
            .switchToAccount(2)
            .menuDrawer()
            .accountsList()
            .verify { switchedToAccount(onePassUser.name) }
    }

    @TestId("1566")
    @Test
    fun addTwoFreeAccounts() {
        loginRobot
            .loginAutoAttachPublicKeyUser()
            .skipOnboarding()
            .menuDrawer()
            .accountsList()
            .addAccount()
            .addFreeAccount()
            .verify { limitReachedToastDisplayed() }
    }
}

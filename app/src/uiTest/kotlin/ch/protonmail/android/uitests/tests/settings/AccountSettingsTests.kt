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
package ch.protonmail.android.uitests.tests.settings

import ch.protonmail.android.uitests.robots.login.LoginMailRobot
import ch.protonmail.android.uitests.robots.settings.account.AccountSettingsRobot
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.TestUser.onePassUser
import ch.protonmail.android.uitests.testsHelper.annotations.SmokeTest
import ch.protonmail.android.uitests.testsHelper.annotations.TestId
import org.junit.experimental.categories.Category
import kotlin.test.BeforeTest
import kotlin.test.Test

class AccountSettingsTests : BaseTest() {

    private val accountSettingsRobot: AccountSettingsRobot = AccountSettingsRobot()
    private val loginRobot = LoginMailRobot()

    @BeforeTest
    override fun setUp() {
        super.setUp()
        loginRobot
            .loginOnePassUser()
            .menuDrawer()
            .settings()
            .openUserAccountSettings(onePassUser)
    }

    @TestId("1657")
    @Test
    fun changeRecoveryEmail() {
//        accountSettingsRobot
//            .recoveryEmail()
//            .changeRecoveryEmail(TestData.twoPassUser)
//            .verify { recoveryEmailChangedTo(TestData.twoPassUser.email) }
    }

    @TestId("30812")
    @Test
    fun navigateToDefaultEmailAddress() {
        accountSettingsRobot
            .defaultEmailAddress()
            .showAll()
            .verify { defaultEmailAddressViewShown() }
    }

    @TestId("1663")
    @Test
    fun changeDisplayName() {
        val newDisplayName = onePassUser.name + System.currentTimeMillis()
        accountSettingsRobot
            .displayNameAndSignature()
            .setDisplayNameTextTo(newDisplayName)
    }

    @TestId("1665")
    @Test
    fun switchSignatureToggleOn() {
        accountSettingsRobot
            .displayNameAndSignature()
            .setSignatureToggleTo(true)
            .verify { signatureToggleCheckedStateIs(true) }
    }

    @TestId("1666")
    @Test
    fun switchSignatureToggleOff() {
        accountSettingsRobot
            .displayNameAndSignature()
            .setSignatureToggleTo(false)
            .verify { signatureToggleCheckedStateIs(false) }
    }

    @TestId("1667")
    @Test
    fun switchMobileSignatureToggleOn() {
        accountSettingsRobot
            .displayNameAndSignature()
            .setMobileSignatureToggleTo(true)
            .verify { mobileSignatureToggleCheckedStateIs(true) }
    }

    @TestId("1668")
    @Test
    fun switchMobileSignatureToggleOff() {
        accountSettingsRobot
            .displayNameAndSignature()
            .setMobileSignatureToggleTo(false)
            .verify { signatureToggleCheckedStateIs(false) }
    }

    @TestId("1478")
    @Test
    fun changeLoginPassword() {
//        accountSettingsRobot
//            .passwordManagement()
//            .changePassword(onePassUser)
//            .verify {
//                passwordChanged()
//                accountSettingsOpened()
//            }
    }

//    @TestId("1480")
//    //TODO enable when multiple user login per test class will be supported
//    fun changeMailboxPassword() {
//        accountSettingsRobot
//            .passwordManagement()
//            .changeMailboxPassword(TestData.twoPassUser)
//            .verify { mailboxPasswordChanged() }
//    }
}

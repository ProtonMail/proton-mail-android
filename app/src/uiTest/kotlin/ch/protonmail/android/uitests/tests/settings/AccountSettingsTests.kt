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
package ch.protonmail.android.uitests.tests.settings

import ch.protonmail.android.uitests.actions.settings.account.AccountSettingsRobot
import ch.protonmail.android.uitests.robots.login.LoginRobot
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.TestData
import ch.protonmail.android.uitests.testsHelper.annotations.SmokeTest
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

class AccountSettingsTests : BaseTest() {

    private val accountSettingsRobot: AccountSettingsRobot = AccountSettingsRobot()
    private val loginRobot = LoginRobot()

    @Before
    override fun setUp() {
        super.setUp()
        loginRobot
            .loginUser(TestData.onePassUser)
            .menuDrawer()
            .settings()
            .selectItemByValue(TestData.onePassUser.email)
    }

    @Test
    fun navigateToSubscription() {
        accountSettingsRobot
            .subscription()
            .verify { subscriptionViewShown() }
    }

    @Category(SmokeTest::class)
    @Test
    fun changeRecoveryEmail() {
        accountSettingsRobot
            .recoveryEmail()
            .changeRecoveryEmail(TestData.twoPassUser)
            .verify { recoveryEmailChangedTo(TestData.twoPassUser.email) }
    }

    @Test
    fun navigateToDefaultEmailAddress() {
        accountSettingsRobot
            .defaultEmailAddress()
            .showAll()
            .verify { defaultEmailAddressViewShown() }
    }

    @Test
    fun changeDisplayName() {
        val newDisplayName = TestData.onePassUser.name + System.currentTimeMillis()
        accountSettingsRobot
            .displayNameAndSignature()
            .setDisplayNameTextTo(newDisplayName)
    }

    @Test
    fun switchSignatureToggleOn() {
        accountSettingsRobot
            .displayNameAndSignature()
            .setSignatureToggleTo(true)
            .verify { signatureToggleCheckedStateIs(true) }
    }

    @Test
    fun switchSignatureToggleOff() {
        accountSettingsRobot
            .displayNameAndSignature()
            .setSignatureToggleTo(false)
            .verify { signatureToggleCheckedStateIs(false) }
    }

    @Test
    fun switchMobileSignatureToggleOn() {
        accountSettingsRobot
            .displayNameAndSignature()
            .setMobileSignatureToggleTo(true)
            .verify { mobileSignatureToggleCheckedStateIs(true) }
    }

    fun changeLoginPassword() {
        accountSettingsRobot
            .passwordManagement()
            .changePassword(TestData.twoPassUser)
            .verify {
                accountSettingsOpened()
                passwordChanged()
            }
    }

//    //TODO enable when multiple user login per test class will be supported
//    fun changeMailboxPassword() {
//        accountSettingsRobot
//            .passwordManagement()
//            .changeMailboxPassword(TestData.twoPassUser)
//            .verify { mailboxPasswordChanged() }
//    }
}

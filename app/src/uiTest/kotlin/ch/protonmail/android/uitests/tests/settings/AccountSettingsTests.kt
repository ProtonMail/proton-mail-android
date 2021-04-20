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

import ch.protonmail.android.uitests.robots.settings.account.AccountSettingsRobot
import ch.protonmail.android.uitests.robots.login.LoginRobot
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.TestData
import ch.protonmail.android.uitests.testsHelper.TestData.onePassUser
import ch.protonmail.android.uitests.testsHelper.annotations.SmokeTest
import ch.protonmail.android.uitests.testsHelper.annotations.TestId
import org.junit.experimental.categories.Category
import kotlin.test.BeforeTest
import kotlin.test.Test

class AccountSettingsTests : BaseTest() {

    private val accountSettingsRobot: AccountSettingsRobot = AccountSettingsRobot()
    private val loginRobot = LoginRobot()

    @BeforeTest
    override fun setUp() {
        super.setUp()
        loginRobot
            .loginUser(onePassUser)
            .menuDrawer()
            .settings()
            .openUserAccountSettings(onePassUser)
    }

    @TestId("30811")
    @Test
    fun navigateToSubscription() {
        accountSettingsRobot
            .subscription()
            .verify { subscriptionViewShown() }
    }

    @TestId("1657")
    @Category(SmokeTest::class)
    @Test
    fun changeRecoveryEmail() {
        accountSettingsRobot
            .recoveryEmail()
            .changeRecoveryEmail(TestData.twoPassUser)
            .verify { recoveryEmailChangedTo(TestData.twoPassUser.email) }
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
    @Category(SmokeTest::class)
    @Test
    fun changeLoginPassword() {
        accountSettingsRobot
            .passwordManagement()
            .changePassword(onePassUser)
            .verify {
                passwordChanged()
                accountSettingsOpened()
            }
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

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
package ch.protonmail.android.uitests.tests.mailbox

import androidx.test.filters.LargeTest
import ch.protonmail.android.uitests.robots.login.LoginRobot
import ch.protonmail.android.uitests.robots.navbar.NavbarRobot
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.TestData
import org.junit.Before
import org.junit.Test

@LargeTest
class NavbarTests : BaseTest() {

    private val navbarRobot = NavbarRobot()
    private val loginRobot = LoginRobot()

    @Before
    override fun setUp() {
        super.setUp()
        loginRobot
            .loginUser(TestData.onePassUser)
    }

    @Test
    fun openNavigationDrawer() {
        navbarRobot
            .openNavbar()
            .verify { navbarOpened() }
    }

    @Test
    fun closeNavigationDrawerWithSwipe() {
        navbarRobot
            .openNavbar()
            .closeNavbarWithSwipe()
            .verify { navbarClosed() }
    }

    @Test
    fun navigateToAccountList() {
        navbarRobot
            .openNavbar()
            .accountList()
            .verify { accountListOpened() }
    }

    @Test
    fun navigateToManageAccounts() {
        navbarRobot
            .openNavbar()
            .accountList()
            .manageAccounts()
            .verify { manageAccountsOpened() }
    }

    @Test
    fun navigateToContacts() {
        navbarRobot
            .openNavbar()
            .navigateToContacts()
            .verify { navigatedToContacts() }
    }

    @Test
    fun navigateToSettings() {
        navbarRobot
            .openNavbar()
            .navigateToSettings()
            .verify { navigatedToSettings() }
    }

    @Test
    fun navigateToReportBugs() {
        navbarRobot
            .openNavbar()
            .navigateToReportBugs()
            .verify { navigatedToReportBugs() }
    }

    @Test
    fun navigateToUpgradeDonate() {
        navbarRobot
            .openNavbar()
            .navigateToUpgradeDonate()
            .verify { navigatedToUpgradeDonate() }
    }
}
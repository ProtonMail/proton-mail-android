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
package ch.protonmail.android.uitests.tests.menu

import ch.protonmail.android.uitests.robots.login.LoginRobot
import ch.protonmail.android.uitests.robots.menu.MenuRobot
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.TestData
import kotlin.test.BeforeTest
import kotlin.test.Test

class MenuTests : BaseTest() {

    private lateinit var menuRobot: MenuRobot
    private val loginRobot = LoginRobot()

    @BeforeTest
    override fun setUp() {
        super.setUp()
        menuRobot = loginRobot
            .loginUser(TestData.onePassUser)
            .menuDrawer()
    }

    @Test
    fun openNavigationDrawer() {
        menuRobot
            .verify { menuOpened() }
    }

    @Test
    fun closeNavigationDrawerWithSwipe() {
        menuRobot
            .closeMenuWithSwipe()
            .verify { menuClosed() }
    }

    @Test
    fun navigateToAccountList() {
        menuRobot
            .accountsList()
            .verify { accountsListOpened() }
    }

    @Test
    fun navigateToManageAccounts() {
        menuRobot
            .accountsList()
            .manageAccounts()
            .verify { manageAccountsOpened() }
    }

    @Test
    fun navigateToContacts() {
        menuRobot
            .contacts()
            .verify { contactsOpened() }
    }

    @Test
    fun navigateToSettings() {
        menuRobot
            .settings()
            .verify { settingsOpened() }
    }

    @Test
    fun navigateToReportBugs() {
        menuRobot
            .reportBugs()
            .verify { reportBugsOpened() }
    }

    @Test
    fun navigateToUpgradeDonate() {
        menuRobot
            .upgradeDonate()
            .verify { upgradeDonateOpened() }
    }
}

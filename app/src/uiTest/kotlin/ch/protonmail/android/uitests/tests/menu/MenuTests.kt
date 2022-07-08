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
package ch.protonmail.android.uitests.tests.menu

import ch.protonmail.android.uitests.robots.login.LoginMailRobot
import ch.protonmail.android.uitests.robots.menu.MenuRobot
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.annotations.TestId
import kotlin.test.BeforeTest
import kotlin.test.Test

class MenuTests : BaseTest() {

    private lateinit var menuRobot: MenuRobot

    private val loginRobot = LoginMailRobot()

    @BeforeTest
    override fun setUp() {
        super.setUp()
        menuRobot = loginRobot
            .loginOnePassUser()
            .menuDrawer()
    }

    @TestId("30803")
    @Test
    fun openNavigationDrawer() {
        menuRobot
            .verify { menuOpened() }
    }

    @TestId("30804")
    @Test
    fun closeNavigationDrawerWithSwipe() {
        menuRobot
            .closeMenuWithSwipe()
            .verify { menuClosed() }
    }

    @TestId("30805")
    @Test
    fun navigateToAccountList() {
        menuRobot
            .accountsList()
            .verify { accountsListOpened() }
    }

    @TestId("30807")
    @Test
    fun navigateToContacts() {
        menuRobot
            .contacts()
            .verify { contactsOpened() }
    }

    @TestId("30808")
    @Test
    fun navigateToSettings() {
        menuRobot
            .settings()
            .verify { settingsOpened() }
    }

    @TestId("30809")
    @Test
    fun navigateToReportBugs() {
        menuRobot
            .reportBugs()
            .verify { reportBugOpened() }
    }
}

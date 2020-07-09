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
package ch.protonmail.android.uitests.robots.navbar

import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.shared.SharedRobot
import ch.protonmail.android.uitests.testsHelper.UIActions

/**
 * [NavbarRobot] class contains actions and verifications for Navigation bar functionality.
 */
open class NavbarRobot : UIActions() {

    fun openNavbar(): NavbarRobot {
        SharedRobot.clickHamburgerOrUpButton()
        return this
    }

    fun closeNavbarWithSwipe(): NavbarRobot {
        swipeLeftObjectWithId(R.id.activeUserDetails)
        return this
    }

    fun accountList(): NavbarRobot {
        clickOnObjectWithId(R.id.buttonExpand)
        return this
    }

    fun manageAccounts(): NavbarRobot {
        clickOnObjectWithId(R.id.manageAccounts)
        return this
    }

    fun navigateToContacts(): NavbarRobot {
        clickOnObjectWithIdAndText(R.id.label, "Contacts")
        return this
    }

    fun navigateToSettings(): NavbarRobot {
        clickChildInRecyclerView(R.id.left_drawer_navigation, 10)
        return this
    }

    fun navigateToReportBugs(): NavbarRobot {
        clickChildInRecyclerView(R.id.left_drawer_navigation, 11)
        return this
    }

    fun navigateToUpgradeDonate(): NavbarRobot {
        clickChildInRecyclerView(R.id.left_drawer_navigation, 12)
        return this
    }

    /**
     * Contains all the validations that can be performed by [NavbarRobot].
     */
    class Verify : NavbarRobot() {

        fun navbarOpened(): NavbarRobot {
            checkIfObjectWithIdIsDisplayed(R.id.userEmailAddress)
            return this
        }

        fun navbarClosed(): NavbarRobot {
            checkIfObjectWithIdNotDisplayed(R.id.activeUserDetails)
            return this
        }

        fun accountListOpened(): NavbarRobot {
            checkIfObjectWithIdHasDescendantWithId(R.id.left_drawer_users, R.id.userAvatar)
            checkIfObjectWithIdAndTextIsDisplayed(R.id.manageAccounts, R.string.manage_accounts)
            return this
        }

        fun manageAccountsOpened(): NavbarRobot {
            checkIfObjectWithTextIsDisplayed(R.string.title_activity_account_manager)
            checkIfObjectWithIdHasDescendantWithId(R.id.accountsRecyclerView, R.id.accUserName)
            checkIfObjectWithIdAndTextIsDisplayed(R.id.addNewUserAccount, R.string.account_manager_add_account)
            return this
        }

        fun navigatedToContacts(): NavbarRobot {
            checkIfObjectWithTextIsDisplayed("Contacts")
            return this
        }

        fun navigatedToSettings(): NavbarRobot {
            checkIfObjectWithTextIsDisplayed("Settings")
            return this
        }

        fun navigatedToReportBugs(): NavbarRobot {
            checkIfObjectWithIdIsDisplayed(R.id.bug_description_title)
            return this
        }

        fun navigatedToUpgradeDonate(): NavbarRobot {
            checkIfObjectWithTextIsDisplayed("Upgrade/Donate")
            return this
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block) as NavbarRobot
}
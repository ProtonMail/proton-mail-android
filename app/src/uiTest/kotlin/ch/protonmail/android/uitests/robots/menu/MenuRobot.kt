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
package ch.protonmail.android.uitests.robots.menu

import androidx.annotation.IdRes
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.contacts.ContactsRobot
import ch.protonmail.android.uitests.robots.mailbox.drafts.DraftsRobot
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.robots.mailbox.labelfolder.LabelFolderRobot
import ch.protonmail.android.uitests.robots.mailbox.sent.SentRobot
import ch.protonmail.android.uitests.robots.mailbox.trash.TrashRobot
import ch.protonmail.android.uitests.robots.manageaccounts.AccountManagerRobot
import ch.protonmail.android.uitests.robots.manageaccounts.ManageAccountsMatchers.withAccountEmailInDrawer
import ch.protonmail.android.uitests.robots.manageaccounts.ManageAccountsMatchers.withLoggedOutAccountNameInDrawer
import ch.protonmail.android.uitests.robots.menu.MenuMatchers.withLabelOrFolderName
import ch.protonmail.android.uitests.robots.menu.MenuMatchers.withMenuItemTag
import ch.protonmail.android.uitests.robots.reportbugs.ReportBugsRobot
import ch.protonmail.android.uitests.robots.settings.SettingsRobot
import ch.protonmail.android.uitests.robots.upgradedonate.UpgradeDonateRobot
import ch.protonmail.android.uitests.testsHelper.StringUtils.stringFromResource
import ch.protonmail.android.uitests.testsHelper.UIActions
import ch.protonmail.android.uitests.testsHelper.click

/**
 * [MenuRobot] class contains actions and verifications for menu functionality.
 */
class MenuRobot {

    fun settings(): SettingsRobot {
        selectMenuItem(settingsText)
        return SettingsRobot()
    }

    fun drafts(): DraftsRobot {
        selectMenuItem(draftsText)
        return DraftsRobot()
    }

    fun inbox(): InboxRobot {
        UIActions.wait.forViewWithId(R.id.left_drawer_navigation)
        selectMenuItem(inboxText)
        return InboxRobot()
    }

    fun sent(): SentRobot {
        selectMenuItem(sentText)
        return SentRobot()
    }

    fun contacts(): ContactsRobot {
        selectMenuItem(contactsText)
        return ContactsRobot()
    }

    fun reportBugs(): ReportBugsRobot {
        selectMenuItem(reportBugsText)
        return ReportBugsRobot()
    }

    fun upgradeDonate(): UpgradeDonateRobot {
        selectMenuItem(upgradeDonateText)
        return UpgradeDonateRobot()
    }

    fun logout(): MenuRobot {
        selectMenuItem(logoutText)
        return this
    }

    fun trash(): TrashRobot {
        selectMenuItem(trashText)
        return TrashRobot()
    }

    fun closeMenuWithSwipe(): MenuRobot {
        UIActions.id.swipeLeftViewWithId(R.id.activeUserDetails)
        return this
    }

    fun labelOrFolder(withName: String): LabelFolderRobot {
        selectMenuLabelOrFolder(withName)
        return LabelFolderRobot()
    }


    fun accountsList(): MenuAccountListRobot {
        UIActions.wait.forViewWithId(R.id.drawerHeaderView).click()
        return MenuAccountListRobot()
    }

    fun closeAccountsList(): MenuRobot {
        UIActions.id.clickViewWithId(R.id.drawerHeaderView)
        return MenuRobot()
    }

    private fun selectMenuItem(@IdRes menuItemName: String) = UIActions.recyclerView
        .clickOnRecyclerViewMatchedItem(leftDrawerNavigationId, withMenuItemTag(menuItemName))

    private fun selectMenuLabelOrFolder(@IdRes labelOrFolderName: String) = UIActions.recyclerView
        .clickOnRecyclerViewMatchedItem(leftDrawerNavigationId, withLabelOrFolderName(labelOrFolderName))

    /**
     * Contains all the validations that can be performed by [MenuRobot].
     */
    class Verify {
        fun menuOpened() = UIActions.check.viewWithIdIsDisplayed(leftDrawerNavigationId)

        fun menuClosed() = UIActions.check.viewWithIdIsNotDisplayed(leftDrawerNavigationId)
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)

    /**
     * [MenuAccountListRobot] class contains actions and verifications for Account list functionality inside Menu drawer
     */
    open inner class MenuAccountListRobot {

        fun manageAccounts(): AccountManagerRobot {
            UIActions.wait.forViewWithId(R.id.manageAccounts).click()
            return AccountManagerRobot()
        }

        fun switchToAccount(accountPosition: Int): MenuRobot {
            UIActions.recyclerView.clickOnRecyclerViewItemByPosition(menuDrawerUserList, accountPosition)
            return MenuRobot()
        }

        /**
         * Contains all the validations that can be performed by [MenuAccountListRobot].
         */
        inner class Verify {
            fun accountsListOpened() = UIActions.check.viewWithIdIsDisplayed(menuDrawerUserList)

            fun accountAdded(email: String) = UIActions.recyclerView
                .scrollToRecyclerViewMatchedItem(menuDrawerUserList, withAccountEmailInDrawer(email))

            fun accountLoggedOut(email: String) = UIActions.recyclerView
                .scrollToRecyclerViewMatchedItem(menuDrawerUserList, withLoggedOutAccountNameInDrawer(email))

            fun accountRemoved(username: String) = UIActions.check
                .viewWithIdAndTextDoesNotExist(menuDrawerUsernameId, username)
        }

        inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
    }

    companion object {
        const val leftDrawerNavigationId = R.id.left_drawer_navigation
        const val menuDrawerUserList = R.id.left_drawer_users
        const val menuDrawerUsernameId = R.id.userName

        val settingsText = stringFromResource(R.string.settings)
        val contactsText = stringFromResource(R.string.contacts)
        val reportBugsText = stringFromResource(R.string.report_bugs)
        val upgradeDonateText = stringFromResource(R.string.upgrade_donate)
        val draftsText = stringFromResource(R.string.drafts)
        val sentText = stringFromResource(R.string.sent)
        val logoutText = stringFromResource(R.string.logout)
        val trashText = stringFromResource(R.string.trash)
        val inboxText = stringFromResource(R.string.inbox)
    }
}

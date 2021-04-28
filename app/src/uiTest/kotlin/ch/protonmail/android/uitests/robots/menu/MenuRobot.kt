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
import ch.protonmail.android.uitests.robots.mailbox.archive.ArchiveRobot
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
import me.proton.core.test.android.instrumented.CoreRobot

/**
 * [MenuRobot] class contains actions and verifications for menu functionality.
 */
class MenuRobot : CoreRobot {

    fun archive(): ArchiveRobot {
        view.withId(R.id.left_drawer_navigation).wait()
        selectMenuItem(archiveText)
        return ArchiveRobot()
    }

    fun settings(): SettingsRobot {
        selectMenuItem(settingsText)
        return SettingsRobot()
    }

    fun drafts(): DraftsRobot {
        selectMenuItem(draftsText)
        return DraftsRobot()
    }

    fun inbox(): InboxRobot {
        view.withId(R.id.left_drawer_navigation).wait()
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
        view.withId(R.id.activeUserDetails).swipeLeft()
        return this
    }

    fun labelOrFolder(withName: String): LabelFolderRobot {
        selectMenuLabelOrFolder(withName)
        return LabelFolderRobot()
    }


    fun accountsList(): MenuAccountListRobot {
        view.withId(R.id.drawerHeaderView).click()
        return MenuAccountListRobot()
    }

    fun closeAccountsList(): MenuRobot {
        view.withId(R.id.drawerHeaderView).click()
        return MenuRobot()
    }

    private fun selectMenuItem(@IdRes menuItemName: String) {
        recyclerView
            .withId(leftDrawerNavigationId)
            .waitUntilPopulated()
            .onHolderItem(withMenuItemTag(menuItemName))
            .click()
    }

    private fun selectMenuLabelOrFolder(@IdRes labelOrFolderName: String) {
        recyclerView
            .withId(leftDrawerNavigationId)
            .waitUntilPopulated()
            .onHolderItem(withLabelOrFolderName(labelOrFolderName))
            .click()
    }


    /**
     * Contains all the validations that can be performed by [MenuRobot].
     */
    class Verify : CoreRobot {

        fun menuOpened() = view.withId(leftDrawerNavigationId).wait()

        fun menuClosed() = view.withId(leftDrawerNavigationId).checkNotDisplayed()
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)

    /**
     * [MenuAccountListRobot] class contains actions and verifications for Account list functionality inside Menu drawer
     */
    open inner class MenuAccountListRobot {

        fun manageAccounts(): AccountManagerRobot {
            view.withId(R.id.manageAccounts).click()
            return AccountManagerRobot()
        }

        fun switchToAccount(accountPosition: Int): MenuRobot {
            recyclerView
                .withId(menuDrawerUserList)
                .waitUntilPopulated()
                .onItemAtPosition(accountPosition)
                .click()
            return MenuRobot()
        }

        fun switchToAccount(email: String): MenuRobot {
            recyclerView
                .withId(menuDrawerUserList)
                .waitUntilPopulated()
                .onHolderItem(withAccountEmailInDrawer(email))
                .click()
            return MenuRobot()
        }

        /**
         * Contains all the validations that can be performed by [MenuAccountListRobot].
         */
        inner class Verify {

            fun accountsListOpened() = view.withId(menuDrawerUserList).wait()

            fun accountAdded(email: String) {
                recyclerView
                    .withId(menuDrawerUserList)
                    .waitUntilPopulated()
                    .scrollToHolder(withAccountEmailInDrawer(email))
            }

            fun accountLoggedOut(email: String) {
                recyclerView
                    .withId(menuDrawerUsernameId)
                    .waitUntilPopulated()
                    .scrollToHolder(withLoggedOutAccountNameInDrawer(email))
            }

            fun accountRemoved(username: String) {
                view.withId(menuDrawerUsernameId).withText(username).checkDoesNotExist()
            }
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
        val archiveText = stringFromResource(R.string.archive)
    }
}

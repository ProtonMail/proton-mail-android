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
import ch.protonmail.android.uitests.robots.manageaccounts.AccountPanelRobot
import ch.protonmail.android.uitests.robots.menu.MenuMatchers.withLabelOrFolderName
import ch.protonmail.android.uitests.robots.menu.MenuMatchers.withMenuItemTag
import ch.protonmail.android.uitests.robots.reportbugs.ReportBugsRobot
import ch.protonmail.android.uitests.robots.settings.SettingsRobot
import me.proton.fusion.Fusion
import me.proton.fusion.utils.StringUtils.stringFromResource

/**
 * [MenuRobot] class contains actions and verifications for menu functionality.
 */
class MenuRobot : Fusion {

    fun archive(): ArchiveRobot {
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

    fun logout(): MenuRobot {
        selectMenuItem(logoutText)
        return this
    }

    fun trash(): TrashRobot {
        selectMenuItem(trashText)
        return TrashRobot()
    }

    fun closeMenuWithSwipe(): MenuRobot {
        view.withId(menuRecyclerView).swipeLeft()
        return this
    }

    fun labelOrFolder(withName: String): LabelFolderRobot {
        selectMenuLabelOrFolder(withName)
        return LabelFolderRobot()
    }

    fun accountsList(): AccountPanelRobot {
        view.withId(R.id.account_expand_imageview).click()
        return AccountPanelRobot()
    }

    private fun selectMenuItem(@IdRes menuItemName: String) {
        recyclerView
            .withId(menuRecyclerView)
//            .waitUntilPopulated()
            .onHolderItem(withMenuItemTag(menuItemName))
            .click()
    }

    private fun selectMenuLabelOrFolder(@IdRes labelOrFolderName: String) {
        recyclerView
            .withId(menuRecyclerView)
//            .waitUntilPopulated()
            .onHolderItem(withLabelOrFolderName(labelOrFolderName))
            .click()
    }

    /**
     * Contains all the validations that can be performed by [MenuRobot].
     */
    class Verify : Fusion {

        fun menuOpened() = view.withId(menuRecyclerView).checkIsDisplayed()

        fun menuClosed() = view.withId(menuRecyclerView).checkIsNotDisplayed()
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)

    companion object {

        const val menuRecyclerView = R.id.menu_recycler_view
        const val menuDrawerUsernameId = R.id.usernameInput

        val settingsText = stringFromResource(R.string.drawer_settings)
        val contactsText = stringFromResource(R.string.drawer_contacts)
        val reportBugsText = stringFromResource(R.string.drawer_report_bug)
        val draftsText = stringFromResource(R.string.drafts)
        val sentText = stringFromResource(R.string.sent)
        val logoutText = stringFromResource(R.string.logout)
        val trashText = stringFromResource(R.string.trash)
        val inboxText = stringFromResource(R.string.inbox)
        val archiveText = stringFromResource(R.string.archive)
    }
}

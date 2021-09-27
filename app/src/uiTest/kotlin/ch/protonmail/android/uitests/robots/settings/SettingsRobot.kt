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
package ch.protonmail.android.uitests.robots.settings

import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.robots.settings.SettingsMatchers.withSettingsHeader
import ch.protonmail.android.uitests.robots.settings.SettingsMatchers.withSettingsValue
import ch.protonmail.android.uitests.robots.settings.account.AccountSettingsRobot
import ch.protonmail.android.uitests.robots.settings.autolock.AutoLockRobot
import ch.protonmail.android.uitests.testsHelper.StringUtils
import ch.protonmail.android.uitests.testsHelper.User
import ch.protonmail.android.uitests.testsHelper.uiactions.UIActions

/**
 * [SettingsRobot] class contains actions and verifications for Settings view.
 */
class SettingsRobot {

    fun navigateUpToInbox(): InboxRobot {
        UIActions.system.clickHamburgerOrUpButton()
        return InboxRobot()
    }

    fun emptyCache(): SettingsRobot {
        UIActions.allOf.clickVisibleViewWithId(R.id.clearCacheButton)
        return this
    }

    fun openUserAccountSettings(user: User): AccountSettingsRobot {
        selectSettingsItemByValue(user.email)
        return AccountSettingsRobot()
    }

    fun selectAutoLock(): AutoLockRobot {
        selectItemByHeader(autoLockText)
        return AutoLockRobot()
    }

    fun selectSettingsItemByValue(value: String): AccountSettingsRobot {
        UIActions.wait.forViewWithId(R.id.settingsRecyclerView)
        UIActions.recyclerView
            .common.clickOnRecyclerViewMatchedItem(R.id.settingsRecyclerView, withSettingsValue(value))
        return AccountSettingsRobot()
    }

    fun selectItemByHeader(header: String) {
        UIActions.wait.forViewWithId(R.id.settingsRecyclerView)
        UIActions.recyclerView
            .common.clickOnRecyclerViewMatchedItem(R.id.settingsRecyclerView, withSettingsHeader(header))
    }

    /**
     * Contains all the validations that can be performed by [SettingsRobot].
     */
    class Verify {

        fun settingsOpened() {
            UIActions.check.viewWithIdIsDisplayed(R.id.settingsRecyclerView)
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)

    companion object {
        val autoLockText = StringUtils.stringFromResource(R.string.auto_lock)
    }
}

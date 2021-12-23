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

import androidx.appcompat.widget.AppCompatImageButton
import androidx.test.espresso.matcher.ViewMatchers
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.robots.settings.SettingsMatchers.withSettingsHeader
import ch.protonmail.android.uitests.robots.settings.SettingsMatchers.withSettingsValue
import ch.protonmail.android.uitests.robots.settings.account.AccountSettingsRobot
import ch.protonmail.android.uitests.robots.settings.autolock.AutoLockRobot
import ch.protonmail.android.uitests.testsHelper.StringUtils
import ch.protonmail.android.uitests.testsHelper.User
import me.proton.core.test.android.instrumented.Robot

/**
 * [SettingsRobot] class contains actions and verifications for Settings view.
 */
class SettingsRobot : Robot {

    fun navigateUpToInbox(): InboxRobot {
        view.instanceOf(AppCompatImageButton::class.java).withParent(view.withId(R.id.toolbar)).click()
        return InboxRobot()
    }

    fun emptyCache(): SettingsRobot {
        view.withId(R.id.clearCacheButton).withVisibility(ViewMatchers.Visibility.VISIBLE).click()
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
        recyclerView
            .withId(R.id.settingsRecyclerView)
//            .waitUntilPopulated()
            .onHolderItem(withSettingsValue(value))
            .click()
        return AccountSettingsRobot()
    }

    private fun selectItemByHeader(header: String) {
        recyclerView
            .withId(R.id.settingsRecyclerView)
//            .waitUntilPopulated()
            .onHolderItem(withSettingsHeader(header))
            .click()
    }

    /**
     * Contains all the validations that can be performed by [SettingsRobot].
     */
    class Verify : Robot {

        fun settingsOpened() {
            view.withId(R.id.settingsRecyclerView).checkDisplayed()
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)

    companion object {

        val autoLockText = StringUtils.stringFromResource(R.string.auto_lock)
    }
}

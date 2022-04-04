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
package ch.protonmail.android.uitests.robots.settings.account

import androidx.annotation.IdRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnHolderItem
import androidx.test.espresso.matcher.ViewMatchers.withId
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.settings.SettingsMatchers.withSettingsHeader
import ch.protonmail.android.uitests.robots.settings.SettingsRobot
import ch.protonmail.android.uitests.robots.settings.account.labelsandfolders.LabelsAndFoldersRobot
import ch.protonmail.android.uitests.robots.settings.account.privacy.PrivacySettingsRobot
import ch.protonmail.android.uitests.robots.settings.account.swipinggestures.SwipingGesturesSettingsRobot
import ch.protonmail.android.uitests.testsHelper.StringUtils.stringFromResource
import me.proton.core.test.android.instrumented.Robot

/**
 * [AccountSettingsRobot] class contains actions and verifications for
 * Account settings functionality.
 */
class AccountSettingsRobot {

    fun privacy(): PrivacySettingsRobot {
        clickOnSettingsItemWithHeader(R.string.privacy)
        return PrivacySettingsRobot()
    }

    fun defaultEmailAddress(): DefaultEmailAddressRobot {
        clickOnSettingsItemWithHeader(R.string.default_mail_address)
        return DefaultEmailAddressRobot()
    }

    fun displayNameAndSignature(): DisplayNameAndSignatureRobot {
        clickOnSettingsItemWithHeader(R.string.display_name_n_signature)
        return DisplayNameAndSignatureRobot()
    }

    fun foldersAndLabels(): LabelsAndFoldersRobot {
        clickOnSettingsItemWithHeader(R.string.labels_and_folders)
        return LabelsAndFoldersRobot()
    }

    fun navigateUpToSettings(): SettingsRobot {
//        UIActions.system.clickHamburgerOrUpButton()
        return SettingsRobot()
    }

    fun swipingGestures(): SwipingGesturesSettingsRobot {
        clickOnSettingsItemWithHeader(R.string.settings_swipe_settings_info)
        return SwipingGesturesSettingsRobot()
    }

    private fun clickOnSettingsItemWithHeader(@IdRes stringId: Int) {
        val header = stringFromResource(stringId)
        onView(withId(R.id.settingsRecyclerView))
            .perform(actionOnHolderItem(withSettingsHeader(header), click()))
    }

    /**
     * Contains all the validations that can be performed by [AccountSettingsRobot].
     */
    class Verify : Robot {

        fun accountSettingsOpened() {
            view.withText(R.string.account_settings).isDescendantOf(view.withId(R.id.toolbar)).checkDisplayed()
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}

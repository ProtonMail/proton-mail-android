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
package ch.protonmail.android.uitests.actions.settings.account

import androidx.annotation.IdRes
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnHolderItem
import androidx.test.espresso.matcher.ViewMatchers.withId
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.settings.SettingsMatchers.withSettingsHeader
import ch.protonmail.android.uitests.robots.settings.account.DefaultEmailAddressRobot
import ch.protonmail.android.uitests.robots.settings.account.DisplayNameAndSignatureRobot
import ch.protonmail.android.uitests.robots.settings.account.LabelsAndFoldersRobot
import ch.protonmail.android.uitests.robots.settings.account.PasswordManagementRobot
import ch.protonmail.android.uitests.robots.settings.account.RecoveryEmailRobot
import ch.protonmail.android.uitests.robots.settings.account.SubscriptionRobot
import ch.protonmail.android.uitests.testsHelper.StringUtils.stringFromResource
import ch.protonmail.android.uitests.testsHelper.UIActions

/**
 * [AccountSettingsRobot] class contains actions and verifications for
 * Account settings functionality.
 */
open class AccountSettingsRobot : UIActions() {

    fun subscription(): SubscriptionRobot {
        clickOnSettingsItemWithHeader(R.string.subscription)
        return SubscriptionRobot()
    }

    fun passwordManagement(): PasswordManagementRobot {
        clickOnSettingsItemWithHeader(R.string.password_manager)
        return PasswordManagementRobot()
    }

    fun recoveryEmail(): RecoveryEmailRobot {
        clickOnSettingsItemWithHeader(R.string.recovery_email)
        return RecoveryEmailRobot()
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

    private fun clickOnSettingsItemWithHeader(@IdRes stringId: Int) {
        val header = stringFromResource(stringId)
        onView(withId(R.id.settingsRecyclerView))
            .perform(actionOnHolderItem(withSettingsHeader(header), click()))
    }

    /**
     * Contains all the validations that can be performed by [AccountSettingsRobot].
     */
    class Verify : AccountSettingsRobot() {

        fun passwordChanged() {
            checkIfToastMessageIsDisplayed(R.string.new_login_password_saved)
        }

        fun mailboxPasswordChanged() {
            checkIfToastMessageIsDisplayed(R.string.new_mailbox_password_saved)
        }
    }

    inline fun verify(block: Verify.() -> Unit) =
        Verify().apply(block) as AccountSettingsRobot
}
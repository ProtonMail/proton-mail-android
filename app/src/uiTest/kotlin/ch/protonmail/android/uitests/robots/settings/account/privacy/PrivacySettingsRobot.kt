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

package ch.protonmail.android.uitests.robots.settings.account.privacy

import androidx.annotation.IdRes
import androidx.annotation.StringRes
import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.settings.SettingsActions
import ch.protonmail.android.uitests.robots.settings.SettingsMatchers.withSettingsHeader
import ch.protonmail.android.uitests.robots.settings.account.AccountSettingsRobot
import ch.protonmail.android.views.SettingsDefaultItemView
import me.proton.core.test.android.instrumented.Robot
import me.proton.core.test.android.instrumented.utils.ActivityProvider
import me.proton.core.test.android.instrumented.utils.StringUtils.stringFromResource

class PrivacySettingsRobot : Robot {

    fun navigateUpToAccountSettings(): AccountSettingsRobot {
        view.instanceOf(AppCompatImageButton::class.java)
            .withParent(view.withId(R.id.toolbar))
            .click()
        return AccountSettingsRobot()
    }

    fun autoDownloadMessages(): AutoDownloadMessagesRobot {
        selectSettingsItem(R.string.auto_download_messages_title)
        return AutoDownloadMessagesRobot()
    }

    fun backgroundSync(): BackgroundSyncRobot {
        selectSettingsItem(R.string.settings_background_sync)
        return BackgroundSyncRobot()
    }

    fun enableAutoShowRemoteImages(): PrivacySettingsRobot {
        toggleSwitchWithTitle(R.string.settings_auto_show_images, true)
        return this
    }

    fun disableAutoShowRemoteImages(): PrivacySettingsRobot {
        toggleSwitchWithTitle(R.string.settings_auto_show_images, false)
        return this
    }

    fun enableAutoShowEmbeddedImages(): PrivacySettingsRobot {
        toggleSwitchWithTitle(R.string.settings_auto_show_embedded_images, true)
        return this
    }

    fun disableAutoShowEmbeddedImages(): PrivacySettingsRobot {
        toggleSwitchWithTitle(R.string.settings_auto_show_embedded_images, false)
        return this
    }

    fun enablePreventTakingScreenshots(): PrivacySettingsRobot {
        toggleSwitchWithTitle(R.string.settings_prevent_taking_screenshots, true)
        return this
    }

    fun disablePreventTakingScreenshots(): PrivacySettingsRobot {
        toggleSwitchWithTitle(R.string.settings_prevent_taking_screenshots, false)
        return this
    }

    fun enableRequestLinkConfirmation(): PrivacySettingsRobot {
        toggleSwitchWithTitle(R.string.hyperlink_confirmation, true)
        return this
    }

    fun disableRequestLinkConfirmation(): PrivacySettingsRobot {
        toggleSwitchWithTitle(R.string.hyperlink_confirmation, false)
        return this
    }

    private fun selectSettingsItem(@StringRes title: Int) {
        recyclerView
            .withId(settingsRecyclerView)
            .onHolderItem(withSettingsHeader(title))
            .click()
    }

    private fun toggleSwitchWithTitle(@IdRes titleId: Int, value: Boolean) {
        SettingsActions.changeToggleState(value, stringFromResource(titleId), switch(titleId))
    }

    private fun switch(@StringRes tagId: Int) = ActivityProvider.currentActivity!!
        .findViewById<RecyclerView>(R.id.settingsRecyclerView)
        .findViewWithTag<SettingsDefaultItemView>(stringFromResource(tagId))
        .findViewById<SwitchCompat>(switchId)

    /**
     * Contains all the validations that can be performed by [PrivacySettingsRobot].
     */
    class Verify : Robot {

        fun autoDownloadImagesIsEnabled() {
            view.withId(R.id.valueText)
                .withText(R.string.enabled)
                .isDescendantOf(view.withTag(R.string.auto_download_messages_title))
                .checkDisplayed()
        }

        fun backgroundSyncIsEnabled() {
            view.withId(R.id.valueText)
                .withText(R.string.disabled)
                .isDescendantOf(view.withTag(R.string.settings_background_sync))
                .checkDisplayed()
        }

        fun takingScreenshotIsDisabled() {
//            UIActions.check
//                .viewWithIdAndAncestorTagIsChecked(switchId, preventTakingScreenshotsText, false)
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)

    companion object {

        private const val switchId = R.id.actionSwitch
        const val settingsRecyclerView = R.id.settingsRecyclerView
        private val preventTakingScreenshotsText = stringFromResource(R.string.settings_prevent_taking_screenshots)
    }
}

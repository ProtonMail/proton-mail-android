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

import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.SwitchCompat
import ch.protonmail.android.R
import me.proton.core.test.android.instrumented.CoreRobot
import me.proton.core.test.android.instrumented.utils.ActivityProvider

class AutoDownloadMessagesRobot : CoreRobot {

    fun navigateUpToPrivacySettings(): PrivacySettingsRobot {
        view
            .instanceOf(AppCompatImageButton::class.java)
            .withParent(view.withId(R.id.toolbar))
            .click()
        return PrivacySettingsRobot()
    }

    fun enableAutoDownloadMessages(): AutoDownloadMessagesRobot {
        view.withId(switchId).wait()
        val switch = ActivityProvider.currentActivity!!.findViewById<SwitchCompat>(switchId)
        toggleSwitch(true, switch)
        return this
    }

    fun disableAutoDownloadMessages(): AutoDownloadMessagesRobot {
        view.withId(switchId).wait()
        val switch = ActivityProvider.currentActivity!!.findViewById<SwitchCompat>(switchId)
        toggleSwitch(false, switch)
        return this
    }

    private fun toggleSwitch(state: Boolean, switch: SwitchCompat) {
        if (state xor switch.isChecked) {
            view.withId(switch.id).click()
        } else {
            view.withId(switch.id).click().click()
        }
    }

    companion object {
        private const val switchId = R.id.enableFeatureSwitch
    }
}

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

package ch.protonmail.android.uitests.robots.settings.account.privacy

import androidx.appcompat.widget.AppCompatImageButton
import androidx.appcompat.widget.SwitchCompat
import ch.protonmail.android.R
import me.proton.fusion.Fusion
import me.proton.fusion.utils.ActivityProvider

class BackgroundSyncRobot : Fusion {

    fun navigateUpToPrivacySettings(): PrivacySettingsRobot {
        view.instanceOf(AppCompatImageButton::class.java)
            .hasParent(view.withId(R.id.toolbar))
            .click()
        return PrivacySettingsRobot()
    }

    fun enableBackgroundSync(): BackgroundSyncRobot {
        view.withId(switchId).checkIsDisplayed()
        val switch = ActivityProvider.currentActivity!!.findViewById<SwitchCompat>(switchId)
        toggleSwitch(true, switch)
        return this
    }

    fun disableBackgroundSync(): BackgroundSyncRobot {
        view.withId(switchId).checkIsDisplayed()
        val switch = ActivityProvider.currentActivity!!.findViewById<SwitchCompat>(switchId)
        toggleSwitch(false, switch)
        return this
    }

    private fun toggleSwitch(state: Boolean, switch: SwitchCompat) {
        when (state xor switch.isChecked) {
            true -> {
                view.withId(switch.id).click()
            }
            false -> {
                view.withId(switch.id).click()
                view.withId(switch.id).click()
            }
        }
    }

    private companion object {

        private const val switchId = R.id.labels_sheet_archive_switch
    }
}

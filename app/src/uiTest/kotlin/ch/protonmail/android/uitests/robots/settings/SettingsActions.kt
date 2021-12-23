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

import androidx.annotation.IdRes
import androidx.appcompat.widget.SwitchCompat
import ch.protonmail.android.uitests.robots.settings.SettingsMatchers.withSettingsHeader
import ch.protonmail.android.uitests.robots.settings.account.privacy.PrivacySettingsRobot
import me.proton.core.test.android.instrumented.Robot

object SettingsActions : Robot {

    fun changeToggleState(state: Boolean, tag: String, @IdRes switch: SwitchCompat) {
        val currentSwitchState = switch.isChecked

        when (state xor currentSwitchState) {
            true -> {
                clickSwitchToggleView(tag, switch.id)
            }
            false -> {
                clickSwitchToggleView(tag, switch.id)
                clickSwitchToggleView(tag, switch.id)
            }
        }
    }

    private fun clickSwitchToggleView(title: String, @IdRes switchId: Int) {
        recyclerView
            .withId(PrivacySettingsRobot.settingsRecyclerView)
            .onHolderItem(withSettingsHeader(title))
            .onItemChildView(view.withId(switchId))
            .click()
    }
}

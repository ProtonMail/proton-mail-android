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

package ch.protonmail.android.uitests.robots.settings.autolock

import androidx.appcompat.widget.AppCompatImageButton
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.settings.SettingsRobot
import ch.protonmail.android.uitests.tests.BaseTest.Companion.targetContext
import ch.protonmail.android.uitests.testsHelper.uiactions.UIActions
import ch.protonmail.android.uitests.testsHelper.uiactions.click

class AutoLockRobot {

    fun navigateUptToSettings(): SettingsRobot {
        UIActions.wait.forViewOfInstanceWithParentId(R.id.toolbar, AppCompatImageButton::class.java).click()
        return SettingsRobot()
    }

    fun enableAutoLock(): PinRobot {
        UIActions.wait.forViewWithIdAndText(headingTextId, R.string.auto_lock_app)
        UIActions.allOf.clickVisibleViewWithId(R.id.actionSwitch)
        return PinRobot()
    }

    fun changeAutoLockTimer(): AutoLockTimeoutRobot {
        UIActions.wait.forViewWithIdAndText(headingTextId, autoLockTimerText).click()
        return AutoLockTimeoutRobot()
    }

    /**
     * Represents Auto lock timeout pop up with options list.
     */
    class AutoLockTimeoutRobot {
        fun selectImmediateAutoLockTimeout(): AutoLockRobot {
            UIActions.listView.clickListItemByPosition(0)
            return AutoLockRobot()
        }

        fun selectFiveMinutesAutoLockTimeout(): AutoLockRobot {
            UIActions.listView.clickListItemByPosition(1)
            return AutoLockRobot()
        }
    }

    companion object {
        private const val timeoutOptionId = R.id.text1
        private const val headingTextId = R.id.headingText
        private const val autoLockTimerText = R.string.auto_lock_timer
        private val immediatelyTimeoutOptionText =
            targetContext.resources.getStringArray(R.array.auto_logout_options_array)[0]
        private val fiveMinutesTimeoutOptionText =
            targetContext.resources.getStringArray(R.array.auto_logout_options_array)[1]
    }
}

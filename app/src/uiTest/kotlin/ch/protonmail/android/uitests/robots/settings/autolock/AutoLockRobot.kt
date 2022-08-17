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

package ch.protonmail.android.uitests.robots.settings.autolock

import android.widget.ListView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.test.espresso.matcher.ViewMatchers
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.settings.SettingsRobot
import ch.protonmail.android.uitests.tests.BaseTest.Companion.targetContext
import me.proton.fusion.Fusion
import org.hamcrest.CoreMatchers.anything

class AutoLockRobot : Fusion {

    fun navigateUptToSettings(): SettingsRobot {
        view.instanceOf(AppCompatImageButton::class.java).hasParent(view.withId(R.id.toolbar)).click()
        return SettingsRobot()
    }

    fun enableAutoLock(): PinRobot {
        view.withId(R.id.actionSwitch).withVisibility(ViewMatchers.Visibility.VISIBLE).click()
        return PinRobot()
    }

    fun changeAutoLockTimer(): AutoLockTimeoutRobot {
        view.withId(headingTextId).withText(autoLockTimerText).click()
        return AutoLockTimeoutRobot()
    }

    /**
     * Represents Auto lock timeout pop up with options list.
     */
    class AutoLockTimeoutRobot : Fusion {

        fun selectImmediateAutoLockTimeout(): AutoLockRobot {
            listView
                .onListItem(anything())
                .inAdapterView(view.instanceOf(ListView::class.java))
                .inRoot(rootView.isPlatformPopUp())
                .atPosition(0)
                .click()
            return AutoLockRobot()
        }

        fun selectFiveMinutesAutoLockTimeout(): AutoLockRobot {
            listView
                .onListItem(anything())
                .inAdapterView(view.instanceOf(ListView::class.java))
                .inRoot(rootView.isPlatformPopUp())
                .atPosition(1)
                .click()
            return AutoLockRobot()
        }
    }

    companion object {

        private const val headingTextId = R.id.headingText
        private const val autoLockTimerText = R.string.auto_lock_timer
        private val immediatelyTimeoutOptionText =
            targetContext.resources.getStringArray(R.array.auto_logout_options_array)[0]
        private val fiveMinutesTimeoutOptionText =
            targetContext.resources.getStringArray(R.array.auto_logout_options_array)[1]
    }
}

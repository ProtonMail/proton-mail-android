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

package ch.protonmail.android.uitests.robots.settings.account.swipinggestures

import ch.protonmail.android.R
import ch.protonmail.android.uitests.testsHelper.uiactions.UIActions

class ChooseSwipeActionRobot {

    fun chooseMessageArchivedAction(): ChooseSwipeActionRobot {
        UIActions.text.clickViewWithText(R.string.swipe_action_mark_read)
        UIActions.text.clickViewWithText(R.string.swipe_action_archive)
        return this
    }

    fun navigateUpToSwipingGestures(): SwipingGesturesSettingsRobot {
        UIActions.system.clickHamburgerOrUpButton()
        return SwipingGesturesSettingsRobot()
    }

    /**
     * Contains all the validations that can be performed by [AccountSettingsRobot].
     */
    class Verify {

        fun messageStarUpdatedIsSelected() {
            UIActions.check.viewWithTextIsChecked(R.string.swipe_action_star)
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}

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

import androidx.appcompat.widget.AppCompatImageButton
import ch.protonmail.android.R
import me.proton.core.test.android.instrumented.Robot

class ChooseSwipeActionRobot : Robot {

    fun chooseMessageArchivedAction(): ChooseSwipeActionRobot {
        view.withText(R.string.swipe_action_mark_read).click()
        view.withText(R.string.swipe_action_archive).click()
        return this
    }

    fun navigateUpToSwipingGestures(): SwipingGesturesSettingsRobot {
        view.instanceOf(AppCompatImageButton::class.java).withParent(view.withId(R.id.toolbar)).click()
        return SwipingGesturesSettingsRobot()
    }

    /**
     * Contains all the validations that can be performed by [ChooseSwipeActionRobot].
     */
    class Verify : Robot {

        fun messageStarUpdatedIsSelected() {
            view.withText(R.string.swipe_action_star).isChecked().checkDisplayed()
        }
    }

    inline fun verify(block: Verify.() -> Unit) = Verify().apply(block)
}

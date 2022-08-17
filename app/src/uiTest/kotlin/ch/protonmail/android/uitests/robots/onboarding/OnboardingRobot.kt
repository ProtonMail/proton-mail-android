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

package ch.protonmail.android.uitests.robots.onboarding

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import ch.protonmail.android.uitests.testsHelper.UICustomViewActions
import ch.protonmail.android.uitests.testsHelper.waitForCondition
import me.proton.fusion.Fusion

class OnboardingRobot : Fusion {

    fun skipOnboarding(): InboxRobot {
        waitForCondition(
            {
                onView(ViewMatchers.withId(R.id.skip)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            },
            watchTimeout = UICustomViewActions.TIMEOUT_60S
        )
        view.withId(R.id.skip).click()
        return InboxRobot()
    }

    fun nextOnboardingScreen(): OnboardingRobot {
        waitForCondition(
            {
                onView(ViewMatchers.withId(R.id.onboarding_button)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
            },
            watchTimeout = UICustomViewActions.TIMEOUT_60S
        )
        view.withId(R.id.onboarding_button).withText(R.string.onboarding_next).click()
        return OnboardingRobot()
    }

    fun finishOnboarding(): InboxRobot {
        view.withId(R.id.onboarding_button).withText(R.string.onboarding_get_started).click()
        return InboxRobot()
    }
}

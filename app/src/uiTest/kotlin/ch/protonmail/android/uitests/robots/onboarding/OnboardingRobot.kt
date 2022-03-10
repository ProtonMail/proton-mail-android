/*
 * Copyright (c) 2022 Proton Technologies AG
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

package ch.protonmail.android.uitests.robots.onboarding

import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.mailbox.inbox.InboxRobot
import me.proton.core.test.android.instrumented.Robot

class OnboardingRobot : Robot {

    fun skipOnboarding(): InboxRobot {
        view.withId(R.id.skip).click()
        return InboxRobot()
    }

    fun nextOnboardingScreen(): OnboardingRobot {
        view.withId(R.id.onboarding_button).withText(R.string.onboarding_next).click()
        return OnboardingRobot()
    }

    fun finishOnboarding(): InboxRobot {
        view.withId(R.id.onboarding_button).withText(R.string.onboarding_get_started).click()
        return InboxRobot()
    }
}

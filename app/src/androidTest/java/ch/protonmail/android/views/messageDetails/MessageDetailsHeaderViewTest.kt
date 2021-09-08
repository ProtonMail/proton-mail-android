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

package ch.protonmail.android.views.messageDetails

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.internal.runner.junit4.AndroidJUnit4ClassRunner
import ch.protonmail.android.R
import ch.protonmail.android.testAndroidInstrumented.assertion.isGone
import ch.protonmail.android.testAndroidInstrumented.assertion.isVisible
import ch.protonmail.android.util.ViewTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4ClassRunner::class)
class MessageDetailsHeaderViewTest : ViewTest<MessageDetailsHeaderView>(::MessageDetailsHeaderView) {

    @Test
    fun shouldHideCollapsedMessageViews() {
        runOnActivityThread {
            testView.showCollapsedMessageViews()
            testView.hideCollapsedMessageViews()
        }

        onCollapsedMessageView().check(isGone())
    }

    @Test
    fun shouldShowCollapsedMessageViews() {
        runOnActivityThread {
            testView.hideCollapsedMessageViews()
            testView.showCollapsedMessageViews()
        }

        onCollapsedMessageView().check(isVisible())
    }

    private fun onCollapsedMessageView(): ViewInteraction = onView(withId(R.id.collapsedMessageViews))
}

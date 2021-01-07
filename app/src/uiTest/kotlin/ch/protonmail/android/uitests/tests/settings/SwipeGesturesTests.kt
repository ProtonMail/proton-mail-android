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

package ch.protonmail.android.uitests.tests.settings

import ch.protonmail.android.uitests.robots.login.LoginRobot
import ch.protonmail.android.uitests.robots.mailbox.MailboxRobotInterface.Companion.swipeLeftMessageDate
import ch.protonmail.android.uitests.robots.mailbox.MailboxRobotInterface.Companion.swipeLeftMessageSubject
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.TestData
import ch.protonmail.android.uitests.testsHelper.annotations.SmokeTest
import ch.protonmail.android.uitests.testsHelper.annotations.TestId
import org.junit.Test
import org.junit.experimental.categories.Category

class SwipeGesturesTests : BaseTest() {

    private val loginRobot = LoginRobot()

    @TestId("29723")
    @Category(SmokeTest::class)
    @Test
    fun deleteMessageWithSwipe() {
        loginRobot
            .loginUser(TestData.onePassUser)
            .menuDrawer()
            .sent()
            .deleteMessageWithSwipe(1)
            .verify {
                messageDeleted(swipeLeftMessageSubject, swipeLeftMessageDate)
            }
    }

    @TestId("29725")
    @Test
    fun starMessageWithSwipe() {
        loginRobot
            .loginUser(TestData.onePassUser)
            .menuDrawer()
            .sent()
            .swipeLeftMessageAtPosition(0)
            .verify { messageStarred() }
    }

    @Test
    fun changeSwipingGesturesForMultipleAccounts() {
        loginRobot
            .loginUser(TestData.onePassUser)
            .menuDrawer()
            .accountsList()
            .manageAccounts()
            .addAccount()
            .connectTwoPassAccount(TestData.twoPassUser)
            .menuDrawer()
            .settings()
            .selectSettingsItemByValue(TestData.twoPassUser.email)
            .swipingGestures()
            .selectSwipeLeft()
            .chooseMessageArchivedAction()
            .navigateUpToSwipingGestures()
            .navigateUpToAccountSettings()
            .navigateUpToSettings()
            .navigateUpToInbox()
            .menuDrawer()
            .accountsList()
            .switchToAccount(TestData.onePassUser.email)
            .settings()
            .selectSettingsItemByValue(TestData.onePassUser.email)
            .swipingGestures()
            .selectSwipeLeft()
            .verify { messageStarUpdatedIsSelected() }
    }
}

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

package ch.protonmail.android.uitests.tests.settings

import ch.protonmail.android.uitests.robots.login.LoginMailRobot
import ch.protonmail.android.uitests.robots.mailbox.MailboxRobotInterface.Companion.deletedMessageDate
import ch.protonmail.android.uitests.robots.mailbox.MailboxRobotInterface.Companion.deletedMessageSubject
import ch.protonmail.android.uitests.robots.mailbox.MailboxRobotInterface.Companion.swipeLeftMessageSubject
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.TestUser.onePassUser
import ch.protonmail.android.uitests.testsHelper.TestUser.twoPassUser
import ch.protonmail.android.uitests.testsHelper.annotations.SmokeTest
import ch.protonmail.android.uitests.testsHelper.annotations.TestId
import org.junit.experimental.categories.Category
import kotlin.test.Test

class SwipeGesturesTests : BaseTest() {

    private val loginRobot = LoginMailRobot()

    @TestId("29723")
    @Category(SmokeTest::class)
    @Test
    fun deleteMessageWithSwipe() {
        loginRobot
            .loginOnePassUser()
            .nextOnboardingScreen()
            .finishOnboarding()
            .menuDrawer()
            .sent()
            .deleteMessageWithSwipe(1)
            .verify {
                messageMovedToTrash(deletedMessageSubject, deletedMessageDate)
            }
    }

    @TestId("29725")
    @Test
    fun starMessageWithSwipe() {
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .menuDrawer()
            .sent()
            .swipeLeftMessageAtPosition(0)
            .verify { messageStarred(swipeLeftMessageSubject) }
    }

    @Test
    fun changeSwipingGesturesForMultipleAccounts() {
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .menuDrawer()
            .accountsList()
            .addAccount()
            .addTwoPassUser()
            .menuDrawer()
            .settings()
            .selectSettingsItemByValue(twoPassUser.email)
            .swipingGestures()
            .selectSwipeLeft()
            .chooseMessageArchivedAction()
            .navigateUpToSwipingGestures()
            .navigateUpToAccountSettings()
            .navigateUpToSettings()
            .navigateUpToInbox()
            .menuDrawer()
            .accountsList()
            .switchToAccount(2)
            .menuDrawer()
            .settings()
            .selectSettingsItemByValue(onePassUser.email)
            .swipingGestures()
            .selectSwipeLeft()
            .verify { messageStarUpdatedIsSelected() }
    }
}

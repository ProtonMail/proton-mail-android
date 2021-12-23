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

package ch.protonmail.android.uitests.tests.spam

import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.login.LoginMailRobot
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.StringUtils.stringFromResource
import ch.protonmail.android.uitests.testsHelper.TestData
import ch.protonmail.android.uitests.testsHelper.TestUser.externalGmailPGPEncrypted
import ch.protonmail.android.uitests.testsHelper.TestUser.onePassUser
import ch.protonmail.android.uitests.testsHelper.mailer.Mail
import kotlin.test.Test

class SpamTests : BaseTest() {

    private val loginRobot = LoginMailRobot()

    //    TODO improve moveMessageFromTrashToFolder() method
    @Test
    fun moveMessageBackToInbox() {
        val from = externalGmailPGPEncrypted
        val to = onePassUser
        val subject = TestData.messageSubject
        val body = TestData.messageBody
        Mail.gmail.from(from).to(to).withSubject(subject).withBody(body).send()
        loginRobot
            .loginOnePassUser()
            .clickMessageBySubject(subject)
            .moveToTrash()
            .menuDrawer()
            .trash()
            .clickMessageByPosition(0)
            .openActionSheet()
            .openFoldersModal()
            .moveMessageFromTrashToFolder(stringFromResource(R.string.inbox))
            .navigateUpToTrash()
            .menuDrawer()
            .inbox()
            .verify { messageWithSubjectExists(subject) }
    }
}

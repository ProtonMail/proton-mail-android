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

package ch.protonmail.android.uitests.tests.drafts

import ch.protonmail.android.uitests.robots.login.LoginRobot
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.TestData
import ch.protonmail.android.uitests.testsHelper.TestData.internalEmailTrustedKeys
import ch.protonmail.android.uitests.testsHelper.TestData.onePassUser
import ch.protonmail.android.uitests.testsHelper.annotations.TestId
import ch.protonmail.android.uitests.testsHelper.annotations.SmokeTest
import org.junit.Before
import org.junit.Test
import org.junit.experimental.categories.Category

class DraftsTests : BaseTest() {

    private val loginRobot = LoginRobot()
    private lateinit var subject: String
    private lateinit var body: String
    private lateinit var to: String

    @Before
    override fun setUp() {
        super.setUp()
        subject = TestData.messageSubject
        body = TestData.messageBody
        to = internalEmailTrustedKeys.email
    }

    @TestId("29753")
    @Test
    fun saveDraft() {
        loginRobot
            .loginUser(onePassUser)
            .compose()
            .draftToSubjectBody(to, subject, body)
            .clickUpButton()
            .confirmDraftSaving()
            .menuDrawer()
            .drafts()
            .refreshMessageList()
            .verify { messageWithSubjectExists(subject) }
    }

    @Category(SmokeTest::class)
    @TestId("29754")
    @Test
    fun saveDraftWithAttachment() {
        loginRobot
            .loginUser(onePassUser)
            .compose()
            .draftSubjectBodyAttachment(to, subject, body)
            .clickUpButton()
            .confirmDraftSaving()
            .menuDrawer()
            .drafts()
            .refreshMessageList()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("1383")
    @Test
    fun sendDraftWithAttachment() {
        loginRobot
            .loginUser(onePassUser)
            .compose()
            .draftSubjectBodyAttachment(to, subject, body)
            .clickUpButton()
            .confirmDraftSaving()
            .menuDrawer()
            .drafts()
            .refreshMessageList()
            .clickDraftBySubject(subject)
            .send()
            .menuDrawer()
            .sent()
            .refreshMessageList()
            .verify { messageWithSubjectExists(subject) }
    }

    @TestId("4278")
    @Test
    fun changeDraftSender() {
        val onePassUserSecondEmail = "2${onePassUser.email}"
        val to = internalEmailTrustedKeys.email
        loginRobot
            .loginUser(onePassUser)
            .compose()
            .draftToSubjectBody(to, subject, body)
            .clickUpButton()
            .confirmDraftSaving()
            .menuDrawer()
            .drafts()
            .clickDraftBySubject(subject)
            .recipients(to)
            .changeSenderTo(onePassUserSecondEmail)
            .clickUpButton()
            .confirmDraftSavingFromDrafts()
            .clickDraftBySubject(subject)
            .verify { fromEmailIs(onePassUserSecondEmail) }
    }

    @TestId("1384")
    @Test
    fun addRecipientsToDraft() {
        val to = internalEmailTrustedKeys.email
        loginRobot
            .loginUser(onePassUser)
            .compose()
            .draftSubjectBody(subject, body)
            .clickUpButton()
            .confirmDraftSaving()
            .menuDrawer()
            .drafts()
            .refreshMessageList()
            .clickDraftBySubject(subject)
            .recipients(to)
            .clickUpButton()
            .confirmDraftSavingFromDrafts()
            .verify { messageWithSubjectAndRecipientExists(subject, to) }
    }

    // Ignore test until MAILAND-982 is fixed.
    // @RailId("1382")
    fun openDraftFromSearch() {
        loginRobot
            .loginUser(onePassUser)
            .compose()
            .draftToSubjectBody(to, subject, body)
            .clickUpButton()
            .confirmDraftSaving()
            .searchBar()
            .searchMessageText(subject)
            .clickSearchedDraftBySubject(subject)
            .verify { messageWithSubjectOpened(subject) }
    }
}

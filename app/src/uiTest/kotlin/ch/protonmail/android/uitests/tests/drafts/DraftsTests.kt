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

import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.device.DeviceRobot
import ch.protonmail.android.uitests.robots.login.LoginRobot
import ch.protonmail.android.uitests.robots.mailbox.composer.ComposerRobot
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.StringUtils.stringFromResource
import ch.protonmail.android.uitests.testsHelper.TestData
import ch.protonmail.android.uitests.testsHelper.TestData.internalEmailTrustedKeys
import ch.protonmail.android.uitests.testsHelper.TestData.onePassUser
import ch.protonmail.android.uitests.testsHelper.annotations.SmokeTest
import ch.protonmail.android.uitests.testsHelper.annotations.TestId
import org.junit.experimental.categories.Category
import kotlin.test.BeforeTest
import kotlin.test.Test

class DraftsTests : BaseTest() {

    private val loginRobot = LoginRobot()
    private val deviceRobot = DeviceRobot()
    private val composerRobot = ComposerRobot()
    private lateinit var subject: String
    private lateinit var body: String
    private lateinit var to: String

    @BeforeTest
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
            .refreshMessageList()
            .clickDraftBySubject(subject)
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

    @TestId("1382")
    // Disabled
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

    @TestId("29754")
    @Test
    fun addAttachmentToDraft() {
        loginRobot
            .loginUser(onePassUser)
            .compose()
            .draftToSubjectBody(to, subject, body)
            .clickUpButton()
            .confirmDraftSaving()
            .menuDrawer()
            .drafts()
            .refreshMessageList()
            .clickDraftBySubject(subject)
            .attachments()
            .addFileAttachment(welcomeDrawable)
            .clickUpButton()
            .confirmDraftSavingFromDrafts()
            .refreshMessageList()
            .clickMessageBySubject(subject)
            .verify { attachmentsAdded() }
    }

    @TestId("1379")
    @Test
    fun minimiseTheAppWhileReplyingToMessage() {
        loginRobot
            .loginUser(onePassUser)
            .refreshMessageList()
            .clickMessageByPosition(0)
            .reply()
            .draftSubjectBody(subject, body)

        deviceRobot
            .clickHomeButton()
            .clickRecentAppsButton()
            .clickRecentAppView()

        composerRobot
            .verify {
                messageWithSubjectOpened(subject)
                bodyWithText(body)
            }
    }

    @TestId("1381")
    @Test
    fun saveDraftWithoutSubject() {
        val noSubject = stringFromResource(R.string.empty_subject)
        loginRobot
            .loginUser(onePassUser)
            .compose()
            .draftToBody(to, body)
            .clickUpButton()
            .confirmDraftSaving()
            .menuDrawer()
            .drafts()
            .refreshMessageList()
            .clickDraftByPosition(0)
            .verify {
                messageWithSubjectOpened(noSubject)
                bodyWithText(body)
            }
    }

    @TestId("1385")
    @Test
    fun savingDraftWithHyphens() {
        val bodyWithHyphens = "This-is-body-with-hyphens!"
        val subjectWithHyphens = "This-is-subject-with-hyphens-$subject"
        loginRobot
            .loginUser(onePassUser)
            .compose()
            .draftSubjectBody(subjectWithHyphens, bodyWithHyphens)
            .clickUpButton()
            .confirmDraftSaving()
            .menuDrawer()
            .drafts()
            .refreshMessageList()
            .clickDraftBySubject(subjectWithHyphens)
            .verify {
                messageWithSubjectOpened(subjectWithHyphens)
                bodyWithText(bodyWithHyphens)
            }
    }

    @TestId("35842")
    @Test
    fun editDraftMultipleTimesAndSend() {
        val subjectEditOne = "Edit one $subject"
        val subjectEditTwo = "Edit two $subject"
        val bodyEditOne = "Edit one $body"
        val bodyEditTwo = "Edit two $body"
        loginRobot
            .loginUser(onePassUser)
            .compose()
            .draftToSubjectBody(to, subject, body)
            .clickUpButton()
            .confirmDraftSaving()
            .menuDrawer()
            .drafts()
            .refreshMessageList()
            .clickDraftBySubject(subject)
            .attachments()
            .addFileAttachment(welcomeDrawable)
            .draftSubjectBody(subjectEditOne, bodyEditOne)
            .clickUpButton()
            .confirmDraftSavingFromDrafts()
            .refreshMessageList()
            .clickDraftBySubject(subjectEditOne)
            .draftSubjectBody(subjectEditTwo, bodyEditTwo)
            .clickUpButton()
            .confirmDraftSavingFromDrafts()
            .refreshMessageList()
            .clickDraftBySubject(subjectEditTwo)
            .verify {
                messageWithSubjectOpened(subjectEditTwo)
                bodyWithText(bodyEditTwo)
            }
    }

    private companion object {
        const val welcomeDrawable = R.drawable.welcome
    }
}

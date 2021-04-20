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

package ch.protonmail.android.uitests.tests.labelsfolders

import ch.protonmail.android.uitests.robots.login.LoginRobot
import ch.protonmail.android.uitests.robots.mailbox.MailboxRobotInterface.Companion.longClickedMessageSubject
import ch.protonmail.android.uitests.robots.mailbox.MailboxRobotInterface.Companion.selectedMessageSubject
import ch.protonmail.android.uitests.robots.mailbox.labelfolder.MessageLocation
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.StringUtils
import ch.protonmail.android.uitests.testsHelper.TestData.onePassUser
import ch.protonmail.android.uitests.testsHelper.annotations.SmokeTest
import ch.protonmail.android.uitests.testsHelper.annotations.TestId
import kotlin.test.Test
import org.junit.experimental.categories.Category

class LabelsFoldersTests : BaseTest() {

    private val loginRobot = LoginRobot()

    @TestId("1458")
    @Test
    fun createRenameAndDeleteFolderFromInbox() {
        val folderName = StringUtils.getEmailString()
        val newFolderName = StringUtils.getEmailString()
        loginRobot
            .loginUser(onePassUser)
            .clickMessageByPosition(1)
            .openFoldersModal()
            .clickCreateFolder()
            .addFolderWithName(folderName)
            .moveMessageFromInboxToFolder(folderName)
            .menuDrawer()
            .settings()
            .openUserAccountSettings(onePassUser)
            .foldersAndLabels()
            .foldersManager()
            .editFolder(folderName, newFolderName, 2)
            .navigateUpToLabelsAndFolders()
            .foldersManager()
            .deleteFolder(newFolderName)
            .verify { folderWithNameDoesNotExist(newFolderName) }
    }

    @TestId("1459")
    @Category(SmokeTest::class)
    @Test
    fun addMessageToCustomFolderFromSent() {
        val folderName = "Folder 1"
        loginRobot
            .loginUser(onePassUser)
            .menuDrawer()
            .sent()
            .refreshMessageList()
            .clickMessageByPosition(1)
            .openFoldersModal()
            .moveMessageFromSentToFolder(folderName)
            .menuDrawer()
            .labelOrFolder(folderName)
            .verify { messageWithSubjectExists(selectedMessageSubject) }
    }

    @TestId("1441")
    @Test
    fun createRenameAndDeleteLabelFromInbox() {
        val labelName = StringUtils.getAlphaNumericStringWithSpecialCharacters()
        val newLabelName = StringUtils.getAlphaNumericStringWithSpecialCharacters()
        loginRobot
            .loginUser(onePassUser)
            .clickMessageByPosition(1)
            .openLabelsModal()
            .addLabel(labelName)
            .selectLabelByName(labelName)
            .apply()
            .navigateUpToInbox()
            .menuDrawer()
            .settings()
            .openUserAccountSettings(onePassUser)
            .foldersAndLabels()
            .labelsManager()
            .editLabel(labelName, newLabelName, 2)
            .deleteLabel(newLabelName)
            .verify { labelWithNameDoesNotExist(newLabelName) }
    }

    @TestId("1438")
    @Category(SmokeTest::class)
    @Test
    fun applyLabelToMessageFromSent() {
        val labelName = "Label 1"
        loginRobot
            .loginUser(onePassUser)
            .menuDrawer()
            .sent()
            .refreshMessageList()
            .clickMessageByPosition(1)
            .openLabelsModal()
            .selectLabelByName(labelName)
            .apply()
            .navigateUpToSent()
            .menuDrawer()
            .labelOrFolder(labelName)
            .refreshMessageList()
            .verify { messageWithSubjectExists(selectedMessageSubject) }
    }

    @TestId("1440")
    @Test
    fun applyLabelToMultipleMessagesFromSent() {
        val labelName = "Label 1"
        loginRobot
            .loginUser(onePassUser)
            .menuDrawer()
            .sent()
            .longClickMessageOnPosition(1)
            .selectMessage(2)
            .addLabel()
            .selectLabelByName(labelName)
            .apply()
            .menuDrawer()
            .labelOrFolder(labelName)
            .refreshMessageList()
            .verify {
                messageWithSubjectExists(longClickedMessageSubject)
                messageWithSubjectExists(selectedMessageSubject)
            }
    }

    @TestId("38407")
    @Test
    fun applyLabelToMessageAndArchive() {
        val labelName = "Label 1"
        loginRobot
            .loginUser(onePassUser)
            .refreshMessageList()
            .clickMessageByPosition(1)
            .openLabelsModal()
            .selectLabelByName(labelName)
            .checkAlsoArchiveCheckBox()
            .applyAndArchive()
            .menuDrawer()
            .labelOrFolder(labelName)
            .refreshMessageList()
            .verify { withMessageSubjectAndLocationExists(selectedMessageSubject, MessageLocation.archive) }
            .menuDrawer()
            .archive()
            .verify { messageWithSubjectExists(selectedMessageSubject) }
    }
}

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

package ch.protonmail.android.uitests.tests.labelsfolders

import ch.protonmail.android.uitests.robots.login.LoginMailRobot
import ch.protonmail.android.uitests.robots.mailbox.MailboxRobotInterface.Companion.longClickedMessageSubject
import ch.protonmail.android.uitests.robots.mailbox.MailboxRobotInterface.Companion.selectedMessageSubject
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.StringUtils
import ch.protonmail.android.uitests.testsHelper.TestUser.onePassUser
import ch.protonmail.android.uitests.testsHelper.annotations.SmokeTest
import ch.protonmail.android.uitests.testsHelper.annotations.TestId
import org.junit.experimental.categories.Category
import kotlin.test.Ignore
import kotlin.test.Test

class LabelsFoldersTests : BaseTest() {

    private val loginRobot = LoginMailRobot()

    @Ignore("Enable after add folder action is added to message details v4 UI")
    @TestId("1458")
    @Test
    fun createRenameAndDeleteFolderFromInbox() {
        val folderName = StringUtils.getEmailString()
        val newFolderName = StringUtils.getEmailString()
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .clickMessageByPosition(1)
            .openActionSheet()
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
    @Test
    fun addMessageToCustomFolderFromSent() {
        val folderName = "Folder 1"
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .menuDrawer()
            .sent()
            .refreshMessageList()
            .clickMessageByPosition(1)
            .openActionSheet()
            .openFoldersModal()
            .moveMessageFromSentToFolder(folderName)
            .menuDrawer()
            .labelOrFolder(folderName)
            .verify { messageWithSubjectExists(selectedMessageSubject) }
    }

    @Ignore("Enable after add label action is added to message details v4 UI")
    @TestId("1441")
    @Test
    fun createRenameAndDeleteLabelFromInbox() {
        val labelName = StringUtils.getAlphaNumericStringWithSpecialCharacters()
        val newLabelName = StringUtils.getAlphaNumericStringWithSpecialCharacters()
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .clickMessageByPosition(1)
            .openActionSheet()
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
    @Test
    fun applyLabelToMessageFromSent() {
        val labelName = "Label 1"
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .menuDrawer()
            .sent()
            .clickMessageByPosition(1)
            .openActionSheet()
            .openLabelsModal()
            .selectLabelByName(labelName)
            .apply()
            .navigateUpToSent()
            .menuDrawer()
            .labelOrFolder(labelName)
            .refreshMessageList()
            .clickMessageBySubject(selectedMessageSubject)
            .verify { labelAdded(labelName) }
    }

    @TestId("1440")
    @Test
    fun applyLabelToMultipleMessagesFromSent() {
        val labelName = "Label 2"
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .menuDrawer()
            .sent()
            .longClickMessageOnPosition(1)
            .selectMessageAtPosition(2)
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
        val labelName = "Label 3"
        loginRobot
            .loginOnePassUser()
            .skipOnboarding()
            .menuDrawer()
            .sent()
            .refreshMessageList()
            .clickMessageByPosition(1)
            .openActionSheet()
            .openLabelsModal()
            .selectLabelByName(labelName)
            .checkAlsoArchiveCheckBox()
            .applyAndArchive()
            .navigateUpToSent()
            .menuDrawer()
            .labelOrFolder(labelName)
            .refreshMessageList()
            .verify { messageWithSubjectExists(selectedMessageSubject) }
            .menuDrawer()
            .archive()
            .verify { messageWithSubjectExists(selectedMessageSubject) }
    }
}

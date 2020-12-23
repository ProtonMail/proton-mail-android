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
import ch.protonmail.android.uitests.tests.BaseTest
import ch.protonmail.android.uitests.testsHelper.StringUtils
import ch.protonmail.android.uitests.testsHelper.TestData.onePassUser
import ch.protonmail.android.uitests.testsHelper.annotations.SmokeTest
import org.junit.Test
import org.junit.experimental.categories.Category

class LabelsFoldersTests : BaseTest() {

    private val loginRobot = LoginRobot()

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

    @Category(SmokeTest::class)
    @Test
    fun addMessageToCustomFolderFromSent() {
        val folderName = "Folder 1"
        loginRobot
            .loginUser(onePassUser)
            .menuDrawer()
            .sent()
            .clickMessageByPosition(1)
            .openFoldersModal()
            .moveMessageFromSentToFolder(folderName)
            .menuDrawer()
            .labelOrFolder(folderName)
            .verify { messageExists(selectedMessageSubject) }
    }

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
            .deleteLabel(labelName)
            .verify { labelWithNameDoesNotExist(labelName) }
    }

    @Category(SmokeTest::class)
    @Test
    fun applyLabelToMessageFromSent() {
        val labelName = "Label 1"
        loginRobot
            .loginUser(onePassUser)
            .menuDrawer()
            .sent()
            .clickMessageByPosition(1)
            .openLabelsModal()
            .selectLabelByName(labelName)
            .apply()
            .navigateUpToSent()
            .menuDrawer()
            .labelOrFolder(labelName)
            .refreshMessageList()
            .verify { messageExists(selectedMessageSubject) }
    }

    // Enable after MAILAND-1280 is fixed
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
                messageExists(longClickedMessageSubject)
                messageExists(selectedMessageSubject)
            }
    }
}

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
package ch.protonmail.android.uitests.robots.mailbox.message

import ch.protonmail.android.R
import ch.protonmail.android.uitests.robots.mailbox.composer.ComposerRobot
import ch.protonmail.android.uitests.testsHelper.UIActions

/**
 * [MessageRobot] class contains actions and verifications for Message detail view functionality.
 */
class MessageRobot {

    fun selectFolder(folderName: String): MessageRobot {
        UIActions.wait.untilViewWithIdAppears(R.id.folders_list_view)
        UIActions.allOf.clickViewWithIdAndText(R.id.folder_name, folderName)
        return this
    }

    fun openFoldersModal(): MessageRobot {
        UIActions.wait.untilViewWithIdAppears(R.id.add_folder)
        UIActions.id.clickViewWithId(R.id.add_folder)
        return this
    }

    fun reply(): ComposerRobot {
        UIActions.id.clickViewWithId(R.id.reply)
        return ComposerRobot()
    }

    fun replyAll(): ComposerRobot {
        UIActions.wait.untilViewWithIdAppears(R.id.reply_all)
        UIActions.id.clickViewWithId(R.id.reply_all)
        return ComposerRobot()
    }

    fun forward(): ComposerRobot {
        UIActions.id.clickViewWithId(R.id.forward)
        return ComposerRobot()
    }

    fun moreOptions(): MessageMoreOptions {
        UIActions.system.clickMoreOptionsButton()
        return MessageMoreOptions()
    }

    fun verifyMessageContainsAttachment(): MessageRobot {
        UIActions.wait.untilViewWithIdAppears(R.id.attachment_title)
        return this
    }

    fun verifyQuotedHeaderShown(): MessageRobot {
        UIActions.wait.untilViewWithIdAppears(R.id.quoted_header)
        return this
    }

    fun verifyAttachmentsAdded(): MessageRobot {
        UIActions.check.viewWithIdIsDisplayed(R.id.attachment_count)
        return this
    }

    fun verifyAttachmentsNotAdded(): MessageRobot {
        UIActions.check.viewWithIdIsNotDisplayed(R.id.attachment_count)
        return this
    }

    inner class MessageMoreOptions {

        fun viewHeaders(): ViewHeadersRobot {
            UIActions.allOf.clickViewWithIdAndText(R.id.title, R.string.view_headers)
            return ViewHeadersRobot()
        }
    }
}

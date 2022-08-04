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

package ch.protonmail.android.mailbox.domain.usecase

import app.cash.turbine.test
import ch.protonmail.android.testdata.UserTestData.userId
import io.mockk.coEvery
import io.mockk.mockk
import junit.framework.Assert.assertEquals
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runBlockingTest
import me.proton.core.domain.arch.DataResult
import me.proton.core.domain.arch.ResponseSource
import me.proton.core.domain.entity.UserId
import me.proton.core.domain.type.IntEnum
import me.proton.core.domain.type.StringEnum
import me.proton.core.mailsettings.domain.entity.ComposerMode
import me.proton.core.mailsettings.domain.entity.MailSettings
import me.proton.core.mailsettings.domain.entity.MessageButtons
import me.proton.core.mailsettings.domain.entity.MimeType
import me.proton.core.mailsettings.domain.entity.PMSignature
import me.proton.core.mailsettings.domain.entity.PackageType
import me.proton.core.mailsettings.domain.entity.ShowImage
import me.proton.core.mailsettings.domain.entity.ShowMoved
import me.proton.core.mailsettings.domain.entity.SwipeAction
import me.proton.core.mailsettings.domain.entity.ViewLayout
import me.proton.core.mailsettings.domain.entity.ViewMode
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import org.junit.Test

class ObserveShowMovedEnabledTest {

    private val mailSettingsRepository: MailSettingsRepository = mockk()

    private val observeShowMovedEnabled = ObserveShowMovedEnabled(
        mailSettingsRepository = mailSettingsRepository
    )

    @Test
    fun verifyShowMovedMessagesInSendAndDraft() = runBlockingTest {
        // given
        coEvery { mailSettingsRepository.getMailSettingsFlow(userId) } returns
            mailSettingsWithShowMoved().toFlowOfDataResult()

        // when
        observeShowMovedEnabled(userId).test {

            // then
            assertEquals(true, awaitItem())
            awaitComplete()
        }
    }

    @Test
    fun verifyDontShowMovedMessagesInSendAndDraft() = runBlockingTest {
        // given
        coEvery { mailSettingsRepository.getMailSettingsFlow(userId) } returns
            mailSettingsWithoutShowMoved().toFlowOfDataResult()

        // when
        observeShowMovedEnabled(userId).test {

            // then
            assertEquals(false, awaitItem())
            awaitComplete()
        }
    }

    private fun mailSettingsWithShowMoved() = MailSettings(
        userId = UserId("userId"),
        displayName = null,
        signature = null,
        autoSaveContacts = true,
        composerMode = IntEnum(1, ComposerMode.Maximized),
        messageButtons = IntEnum(1, MessageButtons.UnreadFirst),
        showImages = IntEnum(1, ShowImage.Remote),
        showMoved = IntEnum(3, ShowMoved.Both),
        viewMode = IntEnum(1, ViewMode.NoConversationGrouping),
        viewLayout = IntEnum(1, ViewLayout.Row),
        swipeLeft = IntEnum(1, SwipeAction.Spam),
        swipeRight = IntEnum(1, SwipeAction.Spam),
        shortcuts = true,
        pmSignature = IntEnum(1, PMSignature.Disabled),
        numMessagePerPage = 1,
        draftMimeType = StringEnum("text/plain", MimeType.PlainText),
        receiveMimeType = StringEnum("text/plain", MimeType.PlainText),
        showMimeType = StringEnum("text/plain", MimeType.PlainText),
        enableFolderColor = true,
        inheritParentFolderColor = true,
        rightToLeft = true,
        attachPublicKey = true,
        sign = true,
        pgpScheme = IntEnum(1, PackageType.ProtonMail),
        promptPin = true,
        stickyLabels = true,
        confirmLink = true
    )

    private fun mailSettingsWithoutShowMoved() = MailSettings(
        userId = UserId("userId"),
        displayName = null,
        signature = null,
        autoSaveContacts = true,
        composerMode = IntEnum(1, ComposerMode.Maximized),
        messageButtons = IntEnum(1, MessageButtons.UnreadFirst),
        showImages = IntEnum(1, ShowImage.Remote),
        showMoved = IntEnum(0, ShowMoved.None),
        viewMode = IntEnum(1, ViewMode.NoConversationGrouping),
        viewLayout = IntEnum(1, ViewLayout.Row),
        swipeLeft = IntEnum(1, SwipeAction.Spam),
        swipeRight = IntEnum(1, SwipeAction.Spam),
        shortcuts = true,
        pmSignature = IntEnum(1, PMSignature.Disabled),
        numMessagePerPage = 1,
        draftMimeType = StringEnum("text/plain", MimeType.PlainText),
        receiveMimeType = StringEnum("text/plain", MimeType.PlainText),
        showMimeType = StringEnum("text/plain", MimeType.PlainText),
        enableFolderColor = true,
        inheritParentFolderColor = true,
        rightToLeft = true,
        attachPublicKey = true,
        sign = true,
        pgpScheme = IntEnum(1, PackageType.ProtonMail),
        promptPin = true,
        stickyLabels = true,
        confirmLink = true
    )

    private fun MailSettings.toFlowOfDataResult() =
        flowOf(DataResult.Success(ResponseSource.Local, this))
}

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

package ch.protonmail.android.mailbox.presentation

import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.featureflags.FeatureFlagsManager
import ch.protonmail.android.mailbox.domain.usecase.ObserveConversationModeEnabled
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
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
import kotlin.test.Test
import kotlin.test.assertEquals

class ConversationModeEnabledTest {

    private val userManager: UserManager = mockk()

    private val featureFlagsManager: FeatureFlagsManager = mockk()

    private val mailSettingsRepository: MailSettingsRepository = mockk()

    private val observerConversationModeEnabled = ObserveConversationModeEnabled(
        featureFlagsManager = featureFlagsManager,
        mailSettingsRepository = mailSettingsRepository
    )

    private val conversationModeEnabled = ConversationModeEnabled(
        userManager = userManager,
        observeConversationModeEnabled = observerConversationModeEnabled
    )

    private val userId = UserId("userId")
    private val secondaryUserId = UserId("secondaryUserId")

    @Test
    fun conversationModeIsEnabledWhenFeatureFlagIsEnabledAndUserViewModeIsConversationMode() = runBlockingTest {
        // given
        every { featureFlagsManager.isChangeViewModeFeatureEnabled() } returns true
        coEvery { userManager.currentUserId } returns userId
        coEvery { mailSettingsRepository.getMailSettingsFlow(userId) } returns
            mailSettingsWithConversationViewMode().toFlowOfDataResult()

        // when
        val actual = conversationModeEnabled(Constants.MessageLocationType.INBOX)

        // then
        assertEquals(true, actual)
    }

    @Test
    fun conversationModeIsDisabledWhenIsChangeViewModeFeatureFlagIsDisabled() {
        // given
        every { featureFlagsManager.isChangeViewModeFeatureEnabled() } returns false
        coEvery { userManager.currentUserId } returns userId
        coEvery { mailSettingsRepository.getMailSettingsFlow(userId) } returns
            mailSettingsWithMessagesViewMode().toFlowOfDataResult()

        // when
        val actual = conversationModeEnabled(Constants.MessageLocationType.INBOX)

        // then
        assertEquals(false, actual)
    }

    @Test
    fun conversationModeIsDisabledWhenFeatureFlagIsEnabledButUserViewModeIsMessagesMode() {
        // given
        every { featureFlagsManager.isChangeViewModeFeatureEnabled() } returns true
        coEvery { userManager.currentUserId } returns userId
        coEvery { mailSettingsRepository.getMailSettingsFlow(userId) } returns
            mailSettingsWithMessagesViewMode().toFlowOfDataResult()

        // when
        val actual = conversationModeEnabled(Constants.MessageLocationType.INBOX)

        // then
        assertEquals(false, actual)
    }

    @Test
    fun conversationModeIsDisabledWhenInputLocationIsDraft() {
        // given
        every { featureFlagsManager.isChangeViewModeFeatureEnabled() } returns true
        coEvery { userManager.currentUserId } returns userId
        coEvery { mailSettingsRepository.getMailSettingsFlow(userId) } returns
            mailSettingsWithConversationViewMode().toFlowOfDataResult()

        // when
        val actual = conversationModeEnabled(Constants.MessageLocationType.DRAFT)

        // then
        assertEquals(false, actual)
    }

    @Test
    fun conversationModeIsDisabledWhenInputLocationIsSent() {
        // given
        every { featureFlagsManager.isChangeViewModeFeatureEnabled() } returns true
        coEvery { userManager.currentUserId } returns userId
        coEvery { mailSettingsRepository.getMailSettingsFlow(userId) } returns
            mailSettingsWithConversationViewMode().toFlowOfDataResult()

        // when
        val actual = conversationModeEnabled(Constants.MessageLocationType.ALL_SENT)

        // then
        assertEquals(false, actual)
    }

    @Test
    fun conversationModeIsDisabledWhenInputLocationIsSearch() {
        // given
        every { featureFlagsManager.isChangeViewModeFeatureEnabled() } returns true
        coEvery { userManager.currentUserId } returns userId
        coEvery { mailSettingsRepository.getMailSettingsFlow(userId) } returns
            mailSettingsWithConversationViewMode().toFlowOfDataResult()

        // when
        val actual = conversationModeEnabled(Constants.MessageLocationType.SEARCH)

        // then
        assertEquals(false, actual)
    }

    @Test
    fun conversationModeIsEnabledWhenLocationIsArchive() {
        // given
        every { featureFlagsManager.isChangeViewModeFeatureEnabled() } returns true
        coEvery { userManager.currentUserId } returns userId
        coEvery { mailSettingsRepository.getMailSettingsFlow(userId) } returns
            mailSettingsWithConversationViewMode().toFlowOfDataResult()

        // when
        val actual = conversationModeEnabled(Constants.MessageLocationType.ARCHIVE)

        // then
        assertEquals(true, actual)
    }

    @Test
    fun verifyThatConversationModeIsEnabledWhenLocationIsNull() {
        // given
        every { featureFlagsManager.isChangeViewModeFeatureEnabled() } returns true
        coEvery { userManager.currentUserId } returns userId
        coEvery { mailSettingsRepository.getMailSettingsFlow(userId) } returns
            mailSettingsWithConversationViewMode().toFlowOfDataResult()

        // when
        val actual = conversationModeEnabled(null)

        // then
        assertEquals(true, actual)
    }

    @Test
    fun conversationModeIsEnabledForSecondaryUserWhenSecondaryUserViewModeIsConversationMode() {
        // given
        every { featureFlagsManager.isChangeViewModeFeatureEnabled() } returns true
        coEvery { userManager.currentUserId } returns userId
        coEvery { mailSettingsRepository.getMailSettingsFlow(secondaryUserId) } returns
            mailSettingsWithConversationViewMode().toFlowOfDataResult()

        // when
        val actual = conversationModeEnabled(null, secondaryUserId)

        // then
        assertEquals(true, actual)
    }

    private fun mailSettingsWithMessagesViewMode() = MailSettings(
        userId = UserId("userId"),
        displayName = null,
        signature = null,
        autoSaveContacts = true,
        composerMode = IntEnum(1, ComposerMode.Maximized),
        messageButtons = IntEnum(1, MessageButtons.UnreadFirst),
        showImages = IntEnum(1, ShowImage.Remote),
        showMoved = IntEnum(1, ShowMoved.Drafts),
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

    private fun mailSettingsWithConversationViewMode() = MailSettings(
        userId = UserId("userId"),
        displayName = null,
        signature = null,
        autoSaveContacts = true,
        composerMode = IntEnum(1, ComposerMode.Maximized),
        messageButtons = IntEnum(1, MessageButtons.UnreadFirst),
        showImages = IntEnum(1, ShowImage.Remote),
        showMoved = IntEnum(1, ShowMoved.Drafts),
        viewMode = IntEnum(0, ViewMode.ConversationGrouping),
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

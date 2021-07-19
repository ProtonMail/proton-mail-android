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

import ch.protonmail.android.api.models.MailSettings
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.featureflags.FeatureFlagsManager
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class ConversationModeEnabledTest {

    @RelaxedMockK
    private lateinit var userManager: UserManager

    @RelaxedMockK
    private lateinit var featureFlagsManager: FeatureFlagsManager

    private lateinit var conversationModeEnabled: ConversationModeEnabled

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        conversationModeEnabled = ConversationModeEnabled(
            featureFlagsManager,
            userManager
        )
    }

    @Test
    fun conversationModeIsEnabledWhenFeatureFlagIsEnabledAndUserViewModeIsConversationMode() {
        every { featureFlagsManager.isChangeViewModeFeatureEnabled() } returns true
        every { userManager.getCurrentUserMailSettingsBlocking() } returns mailSettingsWithConversationViewMode()

        val actual = conversationModeEnabled(Constants.MessageLocationType.INBOX)

        assertEquals(true, actual)
    }

    @Test
    fun conversationModeIsDisabledWhenIsChangeViewModeFeatureFlagIsDisabled() {
        every { featureFlagsManager.isChangeViewModeFeatureEnabled() } returns false

        val actual = conversationModeEnabled(Constants.MessageLocationType.INBOX)

        assertEquals(false, actual)
    }

    @Test
    fun conversationModeIsDisabledWhenFeatureFlagIsEnabledButUserViewModeIsMessagesMode() {
        every { featureFlagsManager.isChangeViewModeFeatureEnabled() } returns true
        every { userManager.getCurrentUserMailSettingsBlocking() } returns mailSettingsWithMessagesViewMode()

        val actual = conversationModeEnabled(Constants.MessageLocationType.INBOX)

        assertEquals(false, actual)
    }

    @Test
    fun conversationModeIsDisabledWhenInputLocationIsDraft() {
        every { featureFlagsManager.isChangeViewModeFeatureEnabled() } returns true
        every { userManager.getCurrentUserMailSettingsBlocking() } returns mailSettingsWithConversationViewMode()

        val actual = conversationModeEnabled(Constants.MessageLocationType.DRAFT)

        assertEquals(false, actual)
    }

    @Test
    fun conversationModeIsDisabledWhenInputLocationIsSent() {
        every { featureFlagsManager.isChangeViewModeFeatureEnabled() } returns true
        every { userManager.getCurrentUserMailSettingsBlocking() } returns mailSettingsWithConversationViewMode()

        val actual = conversationModeEnabled(Constants.MessageLocationType.ALL_SENT)

        assertEquals(false, actual)
    }

    @Test
    fun conversationModeIsDisabledWhenInputLocationIsSearch() {
        every { featureFlagsManager.isChangeViewModeFeatureEnabled() } returns true
        every { userManager.getCurrentUserMailSettingsBlocking() } returns mailSettingsWithConversationViewMode()

        val actual = conversationModeEnabled(Constants.MessageLocationType.SEARCH)

        assertEquals(false, actual)
    }

    @Test
    fun conversationModeIsEnabledWhenLocationIsArchive() {
        every { featureFlagsManager.isChangeViewModeFeatureEnabled() } returns true
        every { userManager.getCurrentUserMailSettingsBlocking() } returns mailSettingsWithConversationViewMode()

        val actual = conversationModeEnabled(Constants.MessageLocationType.ARCHIVE)

        assertEquals(true, actual)
    }

    @Test
    fun verifyThatConversationModeIsEnabledWhenLocationIsNull() {
        // given
        every { featureFlagsManager.isChangeViewModeFeatureEnabled() } returns true
        every { userManager.getCurrentUserMailSettingsBlocking() } returns mailSettingsWithConversationViewMode()

        // when
        val actual = conversationModeEnabled(null)

        // then
        assertEquals(true, actual)
    }

    private fun mailSettingsWithMessagesViewMode(): MailSettings {
        val mailSettings = MailSettings()
        mailSettings.viewMode = 1
        return mailSettings
    }

    private fun mailSettingsWithConversationViewMode(): MailSettings {
        val mailSettings = MailSettings()
        mailSettings.viewMode = 0
        return mailSettings
    }

}

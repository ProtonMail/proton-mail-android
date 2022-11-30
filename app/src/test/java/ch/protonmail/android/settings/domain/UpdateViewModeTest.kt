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

package ch.protonmail.android.settings.domain

import ch.protonmail.android.featureflags.FeatureFlagsManager
import ch.protonmail.android.settings.domain.usecase.UpdateViewMode
import io.mockk.MockKAnnotations
import io.mockk.coJustRun
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.mailsettings.domain.entity.ViewMode
import me.proton.core.mailsettings.domain.repository.MailSettingsRepository
import me.proton.core.test.kotlin.TestDispatcherProvider
import kotlin.test.BeforeTest
import kotlin.test.Test

class UpdateViewModeTest {

    private var featureFlagsManager: FeatureFlagsManager = mockk(relaxed = true)

    private var repository: MailSettingsRepository = mockk()

    private lateinit var updateViewMode: UpdateViewMode

    private val userId = UserId("userId")

    private val dispatchers = TestDispatcherProvider()

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
        updateViewMode = UpdateViewMode(featureFlagsManager, repository, dispatchers)
    }

    @Test
    fun verifyThatUpdateViewModeIsExecutedIfChangeViewModeFeatureIsEnabled() = runTest(dispatchers.Main) {

        // given
        every { featureFlagsManager.isChangeViewModeFeatureEnabled() } returns true
        val viewMode = ViewMode.ConversationGrouping
        coJustRun { repository.updateViewMode(userId, any()) }

        // when
        updateViewMode(userId, viewMode)

        // then
        coVerify { repository.updateViewMode(userId, viewMode) }
    }

    @Test
    fun verifyThatUpdateViewModeIsNotExecutedIfChangeViewModeFeatureIsDisabled() = runTest(dispatchers.Main) {

        // given
        every { featureFlagsManager.isChangeViewModeFeatureEnabled() } returns false
        val viewMode = ViewMode.ConversationGrouping
        coJustRun { repository.updateViewMode(userId, any()) }

        // when
        updateViewMode(userId, viewMode)

        // then
        coVerify(exactly = 0) { repository.updateViewMode(userId, viewMode) }
    }
}

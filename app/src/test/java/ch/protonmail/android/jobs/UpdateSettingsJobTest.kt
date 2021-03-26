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

package ch.protonmail.android.jobs

import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.MailSettings
import ch.protonmail.android.core.ProtonMailApplication
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.di.JobEntryPoint
import ch.protonmail.android.featureflags.FeatureFlagsManager
import ch.protonmail.android.utils.AppUtil
import dagger.hilt.EntryPoints
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test

private const val VIEW_MODE_CONVERSATION = 0

class UpdateSettingsJobTest {

    @RelaxedMockK
    private lateinit var featureFlagsManager: FeatureFlagsManager

    @RelaxedMockK
    private lateinit var mockUserManager: UserManager

    @RelaxedMockK
    private lateinit var mockApiManager: ProtonMailApiManager

    @RelaxedMockK
    private lateinit var jobEntryPoint: JobEntryPoint

    private lateinit var updateSettings: UpdateSettingsJob

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)

        mockkStatic(ProtonMailApplication::class)
        every { ProtonMailApplication.getApplication() } returns mockk()

        mockkStatic(EntryPoints::class)
        every { EntryPoints.get(any(), JobEntryPoint::class.java) } returns jobEntryPoint

        mockkStatic(AppUtil::class)
        justRun { AppUtil.postEventOnUi(any()) }

        every { jobEntryPoint.apiManager() } returns mockApiManager
        every { jobEntryPoint.userManager() } returns mockUserManager
    }

    @Test
    fun jobCallsApiToUpdateAutoShowImagesSettingWhenMailSettingsAreValidAndNotificationEmailDidNotChange() {
        val mailSettings = MailSettings()
        updateSettings = UpdateSettingsJob()

        updateSettings.onRun()

        verify { mockApiManager.updateAutoShowImages(0) }
    }

    @Test
    fun jobCallsApiToUpdateViewModeToggleWhenMailSettingsAreValidAndNotificationEmailDidNotChange() {
        val mailSettings = MailSettings()
        mailSettings.viewMode = VIEW_MODE_CONVERSATION
        updateSettings = UpdateSettingsJob(
            featureFlags = featureFlagsManager
        )
        every { featureFlagsManager.isChangeViewModeFeatureEnabled() } returns true

        updateSettings.onRun()

        verify { mockApiManager.updateViewMode(0) }
    }

    @Test
    fun jobDoesNotCallApiToUpdateViewModeToggleWhenViewModeFeatureFlagIsFalse() {
        val mailSettings = MailSettings()
        updateSettings = UpdateSettingsJob(
            featureFlags = featureFlagsManager
        )
        every { featureFlagsManager.isChangeViewModeFeatureEnabled() } returns false

        updateSettings.onRun()

        verify(exactly = 0) { mockApiManager.updateViewMode(any()) }
    }
}

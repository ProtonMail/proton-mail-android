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

package ch.protonmail.android.feature.rating.usecase

import ch.protonmail.android.feature.rating.MailboxScreenViewInMemoryRepository
import ch.protonmail.android.featureflags.MailFeatureFlags
import ch.protonmail.android.testdata.UserTestData
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import me.proton.core.domain.entity.UserId
import me.proton.core.featureflag.domain.FeatureFlagManager
import me.proton.core.featureflag.domain.entity.FeatureFlag
import me.proton.core.featureflag.domain.entity.Scope
import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ShouldStartRateAppFlowTest {

    private val userId = UserTestData.userId

    private val mailboxScreenViewsRepository: MailboxScreenViewInMemoryRepository = mockk()
    private val featureFlagManager: FeatureFlagManager = mockk(relaxUnitFun = true)

    private val shouldStartRateAppFlow = ShouldStartRateAppFlow(mailboxScreenViewsRepository, featureFlagManager)

    @Test
    fun `start rate app flow is true when feature flag is true and mailbox screen views reached threshold`() = runTest {
        // given
        mockFeatureFlagValue(userId, true)
        mockScreenViews(2)

        // when
        val result = shouldStartRateAppFlow(userId)

        // then
        assertTrue(result)
    }

    @Test
    fun `rate app flow is not started when feature flag is false`() = runTest {
        // given
        mockFeatureFlagValue(userId, false)
        mockScreenViews(2)

        // when
        val result = shouldStartRateAppFlow(userId)

        // then
        assertFalse(result)
    }

    @Test
    fun `rate app flow is not started when mailbox screen views are less than threshold`() = runTest {
        // given
        mockFeatureFlagValue(userId, true)
        mockScreenViews(1)

        // when
        val result = shouldStartRateAppFlow(userId)

        // then
        assertFalse(result)
    }

    @Test
    fun `notify backend that the rate flow was started by disabling feature flag`() = runTest {
        // given
        mockFeatureFlagValue(userId, true)
        mockScreenViews(2)

        // when
        shouldStartRateAppFlow(userId)

        // then
        val featureId = MailFeatureFlags.ShowReviewAppDialog.featureId
        val featureFlag = FeatureFlag(userId, featureId, Scope.User, defaultValue = false, value = false)
        coVerify { featureFlagManager.update(featureFlag) }
    }

    @Test
    fun `rate app feature flag is not refreshed from network each time`() = runTest {
        // This is explicitly checked as it'd be expensive due to high frequency of calls to this use case.
        // Refresh from network happens at app launch through RefreshFeatureFlags use case
        // given
        mockFeatureFlagValue(userId, true)
        mockScreenViews(2)

        // when
        shouldStartRateAppFlow(userId)

        // then
        val featureId = MailFeatureFlags.ShowReviewAppDialog.featureId
        val default = FeatureFlag.default(featureId.id, false)
        coVerify { featureFlagManager.getOrDefault(userId, featureId, default, false) }
    }


    private suspend fun mockFeatureFlagValue(userId: UserId, isEnabled: Boolean) {
        val featureId = MailFeatureFlags.ShowReviewAppDialog.featureId
        coEvery {
            featureFlagManager.getOrDefault(
                userId = userId,
                featureId = featureId,
                default = FeatureFlag.default(featureId.id, false),
                refresh = false
            )
        } returns FeatureFlag(userId, featureId, Scope.User, false, isEnabled)
    }

    private fun mockScreenViews(count: Int) {
        every { mailboxScreenViewsRepository.screenViewCount } returns count
    }
}
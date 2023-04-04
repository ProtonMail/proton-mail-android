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

package ch.protonmail.android.featureflags

import ch.protonmail.android.testdata.AccountTestData
import ch.protonmail.android.testdata.UserTestData
import io.mockk.Called
import io.mockk.coEvery
import io.mockk.coVerifyAll
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.featureflag.domain.FeatureFlagManager
import me.proton.core.featureflag.domain.entity.FeatureFlag
import me.proton.core.featureflag.domain.entity.Scope
import me.proton.core.util.kotlin.DefaultCoroutineScopeProvider
import me.proton.core.util.kotlin.DefaultDispatcherProvider
import org.junit.Test

class RefreshFeatureFlagsTest {

    private val featureFlagManager: FeatureFlagManager = mockk()

    private val accountManager: AccountManager = mockk()

    private val refreshFeatureFlags = RefreshFeatureFlags(
        DefaultCoroutineScopeProvider(DefaultDispatcherProvider()),
        featureFlagManager,
        accountManager
    )

    @Test
    fun `does nothing when there are no accounts`() = runTest {
        // given
        coEvery { accountManager.getAccounts() } returns flowOf()

        // when
        refreshFeatureFlags.refresh()

        // then
        verify { featureFlagManager wasNot Called }
    }

    @Test
    fun `refresh show rating feature flag for each existing user`() = runTest {
        // given
        showRatingsFlagForUserMocked(UserTestData.userId, false)
        showRatingsFlagForUserMocked(UserTestData.secondaryUserId, true)
        coEvery { accountManager.getAccounts() } returns flowOf(AccountTestData.accounts)

        // when
        refreshFeatureFlags.refresh()

        // then
        coVerifyAll {
            showRatingsFlagFetchedForUser(UserTestData.userId)
            showRatingsFlagFetchedForUser(UserTestData.secondaryUserId)
        }
    }

    private fun showRatingsFlagForUserMocked(userId: UserId, isEnabled: Boolean) {
        coEvery {
            featureFlagManager.getOrDefault(
                userId = userId,
                featureId = showReviewAppDialogFeatureFlag.featureId,
                default = FeatureFlag.default(showReviewAppDialogFeatureFlag.featureId.id, false),
                refresh = true
            )
        } returns FeatureFlag(userId, showReviewAppDialogFeatureFlag.featureId, Scope.User, false, isEnabled)
    }

    private suspend fun showRatingsFlagFetchedForUser(userId: UserId) {
        featureFlagManager.getOrDefault(
            userId = userId,
            featureId = showReviewAppDialogFeatureFlag.featureId,
            default = FeatureFlag.default(showReviewAppDialogFeatureFlag.featureId.id, false),
            refresh = true
        )
    }

    companion object {

        private val showReviewAppDialogFeatureFlag = MailFeatureFlags.ShowReviewAppDialog
    }
}
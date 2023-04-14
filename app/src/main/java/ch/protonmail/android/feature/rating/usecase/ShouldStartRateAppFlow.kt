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
import me.proton.core.domain.entity.UserId
import me.proton.core.featureflag.domain.FeatureFlagManager
import me.proton.core.featureflag.domain.entity.FeatureFlag
import javax.inject.Inject

class ShouldStartRateAppFlow @Inject constructor(
    private val mailboxScreenViewsRepository: MailboxScreenViewInMemoryRepository,
    private val featureFlagManager: FeatureFlagManager
) {

    suspend operator fun invoke(userId: UserId) : Boolean {
        if (!isShowReviewFeatureFlagEnabled(userId)) {
            return false
        }
        if (mailboxScreenViewsRepository.screenViewCount < MailboxScreenViewsThreshold) {
            return false
        }
        recordShowReviewFlowConditionsMet(userId)
        return true
    }

    private suspend fun recordShowReviewFlowConditionsMet(userId: UserId) {
        val featureFlag = getShowReviewFeatureFlag(userId)
        val offFeatureFlag = featureFlag.copy(value = false)
        featureFlagManager.update(offFeatureFlag)
    }

    private suspend fun isShowReviewFeatureFlagEnabled(userId: UserId) = getShowReviewFeatureFlag(userId).value

    private suspend fun getShowReviewFeatureFlag(
        userId: UserId
    ) = featureFlagManager.getOrDefault(
        userId = userId,
        featureId = MailFeatureFlags.ShowReviewAppDialog.featureId,
        default = FeatureFlag.default(MailFeatureFlags.ShowReviewAppDialog.featureId.id, false)
    )

    companion object {
        private const val MailboxScreenViewsThreshold = 2
    }
}

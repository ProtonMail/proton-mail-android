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

package ch.protonmail.android.feature.rating

import androidx.annotation.VisibleForTesting
import me.proton.core.domain.entity.UserId
import me.proton.core.featureflag.domain.entity.FeatureFlag
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShowReviewAppInMemoryRepository @Inject constructor() {

    @VisibleForTesting
    var mailboxScreenViews: Int = 0

    @VisibleForTesting
    val featureFlags: MutableMap<UserId, Boolean> = mutableMapOf()

    fun save(featureFlag: FeatureFlag) {
        val userId = featureFlag.userId ?: return
        featureFlags[userId] = featureFlag.value
    }

    fun recordMailboxScreenView() {
        mailboxScreenViews++
        Timber.d("Recording mailbox screen view: count $mailboxScreenViews")
    }

    fun shouldShowRateAppDialog(userId: UserId): Boolean {
        val isFlagEnabled = featureFlags[userId] ?: false
        if (!isFlagEnabled) {
            return false
        }
        return mailboxScreenViews >= MailboxScreenViewsThreshold
    }

    companion object {

        private const val MailboxScreenViewsThreshold = 2
    }
}

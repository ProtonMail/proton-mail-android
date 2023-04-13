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

import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import me.proton.core.accountmanager.domain.AccountManager
import me.proton.core.domain.entity.UserId
import me.proton.core.featureflag.domain.FeatureFlagManager
import me.proton.core.featureflag.domain.entity.FeatureFlag
import me.proton.core.util.kotlin.CoroutineScopeProvider
import timber.log.Timber
import javax.inject.Inject

class RefreshFeatureFlags @Inject constructor(
    private val scopeProvider: CoroutineScopeProvider,
    private val featureFlagManager: FeatureFlagManager,
    private val accountManager: AccountManager
) {

    private val scope get() = scopeProvider.GlobalIOSupervisedScope

    fun refresh() {
        scope.launch {
            val accounts = accountManager.getAccounts().firstOrNull() ?: return@launch
            Timber.d("Refreshing feature flags for ${accounts.count()} accounts")
            accounts.map { it.userId }.forEach { userId ->
                refreshCachedShowRatingsFeatureFlag(userId)
                Timber.d("Rating feature flag refreshed for user $userId")
            }
        }
    }

    private suspend fun refreshCachedShowRatingsFeatureFlag(userId: UserId) {
        featureFlagManager.getOrDefault(
            userId = userId,
            featureId = MailFeatureFlags.ShowReviewAppDialog.featureId,
            default = FeatureFlag.default(MailFeatureFlags.ShowReviewAppDialog.featureId.id, false),
            refresh = true
        )
    }

}

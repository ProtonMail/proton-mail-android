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

package ch.protonmail.android.usecase.fetch

import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.CheckSubscriptionBody
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.VpnPlanType
import ch.protonmail.android.core.Constants.VpnPlanType.Companion.fromString
import ch.protonmail.android.usecase.model.CheckSubscriptionResult
import ch.protonmail.android.utils.extensions.toPMResponseBody
import retrofit2.HttpException
import javax.inject.Inject

class CheckSubscription @Inject constructor(
    private val api: ProtonMailApiManager
) {

    suspend operator fun invoke(
        coupon: String?,
        planIds: MutableList<String>,
        currency: Constants.CurrencyType,
        cycle: Int
    ): CheckSubscriptionResult = runCatching {
        val currentSubscriptions = api.fetchSubscription()

        currentSubscriptions.subscription?.plans?.let { subscriptionPlans ->
            if (subscriptionPlans.size > 0) {
                for (plan in subscriptionPlans) {
                    val vpnPlanType = fromString(plan.name)
                    if (vpnPlanType === VpnPlanType.BASIC || vpnPlanType === VpnPlanType.PLUS) {
                        planIds.add(plan.id)
                    }
                }
            }
        }

        val body = CheckSubscriptionBody(coupon, planIds, currency, cycle)
        val response = api.checkSubscription(body)

        if (response.code == Constants.RESPONSE_CODE_OK) {
            CheckSubscriptionResult.Success(response)
        } else {
            CheckSubscriptionResult.Error(response = response)
        }
    }
        .fold(
            onSuccess = { it },
            onFailure = { throwable ->
                val httpErrorBody = (throwable as? HttpException)?.toPMResponseBody()
                CheckSubscriptionResult.Error(
                    response = httpErrorBody,
                    throwable = throwable
                )
            }
        )
}

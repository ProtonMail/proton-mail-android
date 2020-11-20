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

package ch.protonmail.android.usecase.create

import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.CreateSubscriptionBody
import ch.protonmail.android.api.models.TokenPaymentBody
import ch.protonmail.android.api.utils.ParseUtils
import ch.protonmail.android.core.Constants
import ch.protonmail.android.jobs.payments.GetPaymentMethodsJob
import ch.protonmail.android.jobs.user.FetchUserSettingsJob
import ch.protonmail.android.usecase.model.CreateSubscriptionResult
import ch.protonmail.android.utils.extensions.filterValues
import ch.protonmail.android.utils.extensions.toPMResponseBody
import com.birbit.android.jobqueue.JobManager
import retrofit2.HttpException
import timber.log.Timber
import javax.inject.Inject

/**
 * Use case responsible sending user subscription creation requests.
 *
 *  InputData has to contain non-null values for:
 *  - amount,
 *  - currency,
 *  - cycle,
 *  and optional values for:
 *  - planIds,
 *  - couponCode
 *  - paymentToken
 */
class CreateSubscription @Inject constructor(
    private val api: ProtonMailApiManager,
    private val jobManager: JobManager
) {
    suspend operator fun invoke(
        amount: Int,
        currency: String,
        cycle: Int,
        planIds: MutableList<String> = mutableListOf(),
        couponCode: String? = null,
        paymentToken: String? = null
    ): CreateSubscriptionResult {

        Timber.v("Create Subscription amount:$amount")

        if (currency.isEmpty()) {
            return CreateSubscriptionResult.Error(
                "Incorrect input currency:$currency is incorrect"
            )
        }

        runCatching {
            api.fetchSubscription().subscription?.plans
                ?.onEach {
                    planIds.add(it.id)
                }
        }.onFailure {
            Timber.i(it, "Ignoring fetchSubscription error ${(it as? HttpException)?.toPMResponseBody()}")
        }

        return runCatching {
            val subscriptionBody = if (amount == 0) {
                // don't provide payment method and put "amount" as 0, because we are using stored credits
                CreateSubscriptionBody(0, currency, null, couponCode, planIds, cycle)
            } else {
                // provide new payment method in body
                CreateSubscriptionBody(amount, currency, TokenPaymentBody(paymentToken), couponCode, planIds, cycle)
            }

            val subscriptionResponse = api.createUpdateSubscription(subscriptionBody)
            if (subscriptionResponse.code != Constants.RESPONSE_CODE_OK) {
                return CreateSubscriptionResult.Error(
                    error = subscriptionResponse.error,
                    errorDescription = ParseUtils.compileSingleErrorMessage(
                        subscriptionResponse.details.filterValues(String::class.java)
                    )
                )
            }

            // store payment method if this was first payment using credits from "verification payment"
            if (amount == 0 && paymentToken != null) {
                api.createUpdatePaymentMethod(TokenPaymentBody(paymentToken))
            }

            jobManager.addJobInBackground(FetchUserSettingsJob())
            jobManager.addJobInBackground(GetPaymentMethodsJob())
        }
            .fold(
                onSuccess = { CreateSubscriptionResult.Success },
                onFailure = { throwable ->
                    val httpErrorBody = (throwable as? HttpException)?.toPMResponseBody()
                    CreateSubscriptionResult.Error("Error", httpErrorBody?.error, throwable = throwable)
                }
            )
    }
}

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

package ch.protonmail.android.worker

import android.content.Context
import androidx.hilt.Assisted
import androidx.hilt.work.WorkerInject
import androidx.lifecycle.LiveData
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.CreateSubscriptionBody
import ch.protonmail.android.api.models.TokenPaymentBody
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.Constants.VpnPlanType
import ch.protonmail.android.core.Constants.VpnPlanType.Companion.fromString
import ch.protonmail.android.jobs.payments.GetPaymentMethodsJob
import ch.protonmail.android.jobs.user.FetchUserSettingsJob
import com.birbit.android.jobqueue.JobManager
import javax.inject.Inject


internal const val KEY_INPUT_CREATE_SUBSCRIPT_AMOUNT = "KeyInputCreateSubscriptionAmount"
internal const val KEY_INPUT_CREATE_SUBSCRIPT_CURRENCY = "KeyInputCreateSubscriptionCurrency"
internal const val KEY_INPUT_CREATE_SUBSCRIPT_COUPON_CODE = "KeyInputCreateSubscriptionCouponCode"
internal const val KEY_INPUT_CREATE_SUBSCRIPT_PLAN_IDS = "KeyInputCreateSubscriptionPlanIds"
internal const val KEY_INPUT_CREATE_SUBSCRIPT_CYCLE = "KeyInputCreateSubscriptionCycle"
internal const val KEY_INPUT_CREATE_SUBSCRIPT_PAYMENT_TOKEN = "KeyInputCreateSubscriptionPaymentToken"

/**
 * Work Manager Worker responsible sending user subscription creation requests.
 *
 *  InputData has to contain non-null values for:
 *  - amount,
 *  - currency,
 *  - cycle,
 *  and optional values for:
 *  - planIds,
 *  - couponCode
 *  - paymentToken
 *
 * @see androidx.work.WorkManager
 */
class CreateSubscriptionWorker @WorkerInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val api: ProtonMailApiManager,
    private val jobManager: JobManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val amount = inputData.getInt(KEY_INPUT_CREATE_SUBSCRIPT_AMOUNT, 0)
        val currency = inputData.getString(KEY_INPUT_CREATE_SUBSCRIPT_CURRENCY)
        val cycle = inputData.getInt(KEY_INPUT_CREATE_SUBSCRIPT_CYCLE, 0)
        val planIds = inputData.getStringArray(KEY_INPUT_CREATE_SUBSCRIPT_PLAN_IDS)?.toMutableList()
        val couponCode = inputData.getString(KEY_INPUT_CREATE_SUBSCRIPT_COUPON_CODE)
        val paymentToken = inputData.getString(KEY_INPUT_CREATE_SUBSCRIPT_PAYMENT_TOKEN)

        if (currency.isNullOrEmpty() || planIds == null) {
            return Result.failure(
                workDataOf(
                    KEY_WORKER_ERROR_DESCRIPTION to
                        "Incorrect input parameters currency:$currency, ids:$planIds"
                )
            )
        }

        return runCatching {
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

            val subscriptionBody = if (amount == 0) {
                // don't provide payment method and put "amount" as 0, because we are using stored credits
                CreateSubscriptionBody(0, currency, null, couponCode, planIds, cycle)
            } else {
                // provide new payment method in body
                CreateSubscriptionBody(amount, currency, TokenPaymentBody(paymentToken), couponCode, planIds, cycle)
            }

            val subscriptionResponse = api.createUpdateSubscription(subscriptionBody)
            if (subscriptionResponse.code != Constants.RESPONSE_CODE_OK) {
                return@runCatching Result.failure(
                    workDataOf(
                        KEY_WORKER_ERROR_DESCRIPTION to "createUpdateSubscription response:${subscriptionResponse.code}"
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
                onSuccess = { Result.success() },
                onFailure = { failure(it) }
            )

    }

    class Enqueuer @Inject constructor(private val workManager: WorkManager) {
        fun enqueue(
            amount: Int,
            currency: String,
            cycle: Int,
            planIds: Array<String> = arrayOf(),
            couponCode: String? = null,
            paymentToken: String? = null
        ): LiveData<WorkInfo> {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val workRequest = OneTimeWorkRequestBuilder<CreateSubscriptionWorker>()
                .setConstraints(constraints)
                .setInputData(
                    workDataOf(
                        KEY_INPUT_CREATE_SUBSCRIPT_AMOUNT to amount,
                        KEY_INPUT_CREATE_SUBSCRIPT_CURRENCY to currency,
                        KEY_INPUT_CREATE_SUBSCRIPT_COUPON_CODE to couponCode,
                        KEY_INPUT_CREATE_SUBSCRIPT_PLAN_IDS to planIds,
                        KEY_INPUT_CREATE_SUBSCRIPT_CYCLE to cycle,
                        KEY_INPUT_CREATE_SUBSCRIPT_PAYMENT_TOKEN to paymentToken
                    )
                ).build()

            workManager.enqueue(workRequest)
            return workManager.getWorkInfoByIdLiveData(workRequest.id)
        }

    }
}

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
package ch.protonmail.android.api.segments.payment

import ch.protonmail.android.api.models.AvailablePlansResponse
import ch.protonmail.android.api.models.CheckSubscriptionBody
import ch.protonmail.android.api.models.CheckSubscriptionResponse
import ch.protonmail.android.api.models.GetSubscriptionResponse
import ch.protonmail.android.api.models.PaymentMethodsResponse
import ch.protonmail.android.api.models.PaymentsStatusResponse
import ch.protonmail.android.api.segments.BaseApi
import ch.protonmail.android.api.utils.ParseUtils
import java.io.IOException

class PaymentApi(
    private val service: PaymentService,
    private val pubService: PaymentPubService
) : BaseApi(), PaymentApiSpec {

    override suspend fun fetchSubscription(): GetSubscriptionResponse = service.fetchSubscription()

    override suspend fun fetchPaymentMethods(): PaymentMethodsResponse = service.fetchPaymentMethods()

    override suspend fun fetchPaymentsStatus(): PaymentsStatusResponse = service.fetchPaymentsStatus()

    override suspend fun checkSubscription(body: CheckSubscriptionBody): CheckSubscriptionResponse =
        service.checkSubscription(body)

    @Throws(IOException::class)
    override fun fetchAvailablePlans(currency: String, cycle: Int): AvailablePlansResponse =
        ParseUtils.parse(pubService.fetchAvailablePlans(currency, cycle).execute())
}

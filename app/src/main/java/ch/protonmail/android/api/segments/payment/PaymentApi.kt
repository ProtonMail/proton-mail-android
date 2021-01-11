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
import ch.protonmail.android.api.models.CreatePaymentTokenBody
import ch.protonmail.android.api.models.CreatePaymentTokenSuccessResponse
import ch.protonmail.android.api.models.CreateSubscriptionBody
import ch.protonmail.android.api.models.CreateUpdateSubscriptionResponse
import ch.protonmail.android.api.models.DonateBody
import ch.protonmail.android.api.models.GetPaymentTokenResponse
import ch.protonmail.android.api.models.GetSubscriptionResponse
import ch.protonmail.android.api.models.PaymentMethodResponse
import ch.protonmail.android.api.models.PaymentMethodsResponse
import ch.protonmail.android.api.models.PaymentsStatusResponse
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.models.TokenPaymentBody
import ch.protonmail.android.api.models.VerifyBody
import ch.protonmail.android.api.models.VerifyResponse
import ch.protonmail.android.api.segments.BaseApi
import ch.protonmail.android.api.utils.ParseUtils
import retrofit2.Call
import java.io.IOException

class PaymentApi(
    private val service: PaymentService,
    private val pubService: PaymentPubService
) : BaseApi(), PaymentApiSpec {

    override suspend fun fetchSubscription(): GetSubscriptionResponse = service.fetchSubscription()

    override suspend fun fetchPaymentMethods(): PaymentMethodsResponse = service.fetchPaymentMethods()

    override suspend fun fetchPaymentsStatus(): PaymentsStatusResponse = service.fetchPaymentsStatus()

    override suspend fun createUpdatePaymentMethod(body: TokenPaymentBody): PaymentMethodResponse =
        service.createUpdatePaymentMethod(body)

    override suspend fun createUpdateSubscription(body: CreateSubscriptionBody): CreateUpdateSubscriptionResponse =
        service.createUpdateSubscription(body)

    @Throws(IOException::class)
    override fun donate(body: DonateBody): ResponseBody = ParseUtils.parse(service.donate(body).execute())

    override suspend fun checkSubscription(body: CheckSubscriptionBody): CheckSubscriptionResponse =
        service.checkSubscription(body)

    @Throws(IOException::class)
    override fun fetchAvailablePlans(currency: String, cycle: Int): AvailablePlansResponse =
        ParseUtils.parse(pubService.fetchAvailablePlans(currency, cycle).execute())

    @Throws(Exception::class)
    override fun verifyPayment(body: VerifyBody): VerifyResponse = ParseUtils.parse(pubService.verifyPayment(body).execute())

    @Throws(IOException::class)
    override fun createPaymentToken(
        body: CreatePaymentTokenBody,
        token: String?,
        tokenType: String?
    ): Call<CreatePaymentTokenSuccessResponse> = pubService.createPaymentToken(body, token, tokenType)

    @Throws(IOException::class)
    override fun getPaymentToken(token: String): Call<GetPaymentTokenResponse> = pubService.getPaymentToken(token)

}

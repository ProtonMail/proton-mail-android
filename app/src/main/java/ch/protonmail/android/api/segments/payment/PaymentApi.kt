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

import androidx.annotation.WorkerThread
import ch.protonmail.android.api.models.*
import ch.protonmail.android.api.segments.BaseApi
import ch.protonmail.android.api.utils.ParseUtils
import retrofit2.Call
import java.io.IOException

class PaymentApi(private val service: PaymentService,
                 private val pubService: PaymentPubService) : BaseApi(), PaymentApiSpec {

    @Throws(IOException::class)
    override fun fetchSubscription(): GetSubscriptionResponse = ParseUtils.parse(service.fetchSubscription().execute())

    @Throws(IOException::class)
    override fun fetchPaymentMethods(): PaymentMethodsResponse = ParseUtils.parse(service.fetchPaymentMethods().execute())

    @Throws(IOException::class)
    override fun fetchPaymentsStatus(): PaymentsStatusResponse =
            ParseUtils.parse(service.fetchPaymentsStatus().execute())

    @Throws(IOException::class)
    override fun createUpdatePaymentMethod(body: TokenPaymentBody): Call<PaymentMethodResponse> = service.createUpdatePaymentMethod(body)

    @WorkerThread
    @Throws(IOException::class)
    override fun createUpdateSubscription(body: CreateSubscriptionBody): CreateUpdateSubscriptionResponse
            = ParseUtils.parse(service.createUpdateSubscription(body).execute())

    @Throws(IOException::class)
    override fun donate(body: DonateBody): ResponseBody = ParseUtils.parse(service.donate(body).execute())

    @Throws(IOException::class)
    override fun checkSubscription(body: CheckSubscriptionBody): CheckSubscriptionResponse =
            ParseUtils.parse(service.checkSubscription(body).execute())

    @Throws(IOException::class)
    override fun fetchAvailablePlans(currency: String, cycle: Int): AvailablePlansResponse =
            ParseUtils.parse(pubService.fetchAvailablePlans(currency, cycle).execute())

    @Throws(Exception::class)
    override fun verifyPayment(body: VerifyBody): VerifyResponse = ParseUtils.parse(pubService.verifyPayment(body).execute())

    @Throws(IOException::class)
    override fun createPaymentToken(body: CreatePaymentTokenBody): Call<CreatePaymentTokenSuccessResponse> = pubService.createPaymentToken(body)

    @Throws(IOException::class)
    override fun getPaymentToken(token: String): Call<GetPaymentTokenResponse> = pubService.getPaymentToken(token)

}

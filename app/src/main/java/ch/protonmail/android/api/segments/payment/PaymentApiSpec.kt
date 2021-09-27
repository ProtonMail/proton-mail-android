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
import ch.protonmail.android.api.models.GetPaymentTokenResponse
import ch.protonmail.android.api.models.GetSubscriptionResponse
import ch.protonmail.android.api.models.PaymentMethodResponse
import ch.protonmail.android.api.models.PaymentMethodsResponse
import ch.protonmail.android.api.models.PaymentsStatusResponse
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.models.TokenPaymentBody
import ch.protonmail.android.api.models.VerifyBody
import ch.protonmail.android.api.models.VerifyResponse
import retrofit2.Call
import retrofit2.http.Path
import java.io.IOException

interface PaymentApiSpec {

    suspend fun fetchSubscription(): GetSubscriptionResponse

    suspend fun fetchPaymentMethods(): PaymentMethodsResponse

    suspend fun fetchPaymentsStatus(): PaymentsStatusResponse

    suspend fun checkSubscription(body: CheckSubscriptionBody): CheckSubscriptionResponse

    suspend fun createUpdateSubscription(body: CreateSubscriptionBody): CreateUpdateSubscriptionResponse

    suspend fun createUpdatePaymentMethod(body: TokenPaymentBody): PaymentMethodResponse

    @Throws(IOException::class)
    fun fetchAvailablePlans(currency: String, cycle: Int): AvailablePlansResponse

    @Throws(Exception::class)
    fun verifyPayment(body: VerifyBody): VerifyResponse

    @Throws(IOException::class)
    fun createPaymentToken(
        body: CreatePaymentTokenBody,
        token: String?,
        tokenType: String?
    ): Call<CreatePaymentTokenSuccessResponse>

    @Throws(IOException::class)
    fun getPaymentToken(@Path("token") token: String): Call<GetPaymentTokenResponse>

}

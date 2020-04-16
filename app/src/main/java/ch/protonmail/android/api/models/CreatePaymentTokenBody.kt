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
package ch.protonmail.android.api.models

import com.google.gson.annotations.SerializedName

// region constants
private const val FIELD_AMOUNT = "Amount"
private const val FIELD_CURRENCY = "Currency"
private const val FIELD_PAYMENT = "Payment"
private const val FIELD_PAYMENT_METHOD_ID = "PaymentMethodID"
private const val FIELD_TYPE = "Type"
private const val FIELD_DETAILS = "Details"

const val PAYMENT_TYPE_PAYPAL = "paypal"
const val PAYMENT_TYPE_CARD = "card"
// endregion

data class CreatePaymentTokenBody(
        @SerializedName(FIELD_AMOUNT)
        var amount: Int,
        @SerializedName(FIELD_CURRENCY)
        var currency: String,
        @SerializedName(FIELD_PAYMENT)
        var payment: PaymentType?,
        @SerializedName(FIELD_PAYMENT_METHOD_ID)
        var paymentMethodId: String?
)

sealed class PaymentType(@SerializedName(FIELD_TYPE) var type: String) {
    object PayPal : PaymentType(PAYMENT_TYPE_PAYPAL)
    data class Card(@SerializedName(FIELD_DETAILS) var details: Map<String, String>) : PaymentType(PAYMENT_TYPE_CARD)
}

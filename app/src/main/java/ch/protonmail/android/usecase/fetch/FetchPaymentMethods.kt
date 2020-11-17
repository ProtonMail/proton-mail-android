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
import ch.protonmail.android.core.Constants
import ch.protonmail.android.usecase.model.FetchPaymentMethodsResult
import javax.inject.Inject

class FetchPaymentMethods @Inject constructor(
    private val api: ProtonMailApiManager
) {

    suspend operator fun invoke(): FetchPaymentMethodsResult = runCatching {
        val paymentMethodsResponse = api.fetchPaymentMethods()
        if (paymentMethodsResponse.code == Constants.RESPONSE_CODE_OK) {
            FetchPaymentMethodsResult.Success(paymentMethodsResponse.paymentMethods)
        } else {
            FetchPaymentMethodsResult.Error()
        }
    }
        .fold(
            onSuccess = { it },
            onFailure = { FetchPaymentMethodsResult.Error(it) }
        )
}

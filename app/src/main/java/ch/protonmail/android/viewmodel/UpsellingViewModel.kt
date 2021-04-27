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

package ch.protonmail.android.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.core.Constants
import ch.protonmail.android.usecase.create.CreateSubscription
import ch.protonmail.android.usecase.fetch.CheckSubscription
import ch.protonmail.android.usecase.model.CheckSubscriptionResult
import ch.protonmail.android.usecase.model.CreateSubscriptionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class UpsellingViewModel @Inject constructor(
    private val createSubscriptionUseCase: CreateSubscription,
    private val checkSubscriptionUseCase: CheckSubscription
) : ViewModel() {

    private val createSubscriptionData = MutableLiveData<CreateSubscriptionResult>()
    private val checkSubscriptionData = MutableLiveData<CheckSubscriptionResult>()

    val createSubscriptionResult: LiveData<CreateSubscriptionResult>
        get() = createSubscriptionData
    val checkSubscriptionResult: LiveData<CheckSubscriptionResult>
        get() = checkSubscriptionData

    fun createSubscription(
        amount: Int,
        currency: String,
        cycle: Int,
        planIds: MutableList<String>,
        couponCode: String?,
        token: String?
    ) {
        viewModelScope.launch {
            val result = createSubscriptionUseCase(
                amount,
                currency,
                cycle,
                planIds,
                couponCode,
                token
            )
            Timber.v("createSubscription result $result")
            createSubscriptionData.value = result
        }
    }

    fun checkSubscription(
        coupon: String?,
        planIds: MutableList<String>,
        currency: Constants.CurrencyType,
        cycle: Int
    ) {
        viewModelScope.launch {
            val result = checkSubscriptionUseCase(
                coupon,
                planIds,
                currency,
                cycle
            )
            checkSubscriptionData.value = result
        }
    }
}

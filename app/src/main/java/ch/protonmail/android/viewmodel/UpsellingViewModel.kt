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

import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ch.protonmail.android.usecase.create.CreateSubscription
import ch.protonmail.android.usecase.model.CreateSubscriptionResult
import kotlinx.coroutines.launch
import timber.log.Timber

class UpsellingViewModel @ViewModelInject constructor(
    private val createSubscriptionUseCase: CreateSubscription
) : ViewModel() {

    private val createSubscriptionData = MutableLiveData<CreateSubscriptionResult>()

    val createSubscriptionResult: LiveData<CreateSubscriptionResult>
        get() = createSubscriptionData

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
}

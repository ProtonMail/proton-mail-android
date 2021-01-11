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
import ch.protonmail.android.usecase.fetch.FetchPaymentMethods
import ch.protonmail.android.usecase.model.FetchPaymentMethodsResult
import kotlinx.coroutines.launch

class AccountTypeViewModel @ViewModelInject constructor(
    val fetchPaymentMethodsUseCase: FetchPaymentMethods
) : ViewModel() {

    private val fetchPaymentMethodsData = MutableLiveData<FetchPaymentMethodsResult>()

    val fetchPaymentMethodsResult: LiveData<FetchPaymentMethodsResult>
        get() = fetchPaymentMethodsData

    fun fetchPaymentMethods() {
        viewModelScope.launch {
            val result = fetchPaymentMethodsUseCase()
            fetchPaymentMethodsData.value = result
        }
    }
}

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
package ch.protonmail.android.viewmodels

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ch.protonmail.android.api.models.CreatePaymentTokenBody
import ch.protonmail.android.api.models.CreatePaymentTokenErrorResponse
import ch.protonmail.android.api.models.CreatePaymentTokenNetworkErrorResponse
import ch.protonmail.android.api.models.CreatePaymentTokenResponse
import ch.protonmail.android.api.models.CreatePaymentTokenSuccessResponse
import ch.protonmail.android.api.models.PaymentType
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.ProtonMailApplication
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class BillingViewModel(application: Application) : AndroidViewModel(application) {

    private val _createPaymentToken = MutableLiveData<CreatePaymentTokenResponse>()
    private val _createPaymentTokenFromPaymentMethodId = MutableLiveData<CreatePaymentTokenResponse>()

    private val gson = Gson()
    private val responseBodyType = object : TypeToken<ResponseBody>() {}.type

    fun createPaymentToken(amount: Int, currency: Constants.CurrencyType, payment: PaymentType): LiveData<CreatePaymentTokenResponse> {
        (getApplication() as ProtonMailApplication).api.createPaymentToken(CreatePaymentTokenBody(amount, currency.name, payment, null)).enqueue(object : Callback<CreatePaymentTokenSuccessResponse> {
            override fun onFailure(call: Call<CreatePaymentTokenSuccessResponse>, t: Throwable) {
                _createPaymentToken.value = CreatePaymentTokenNetworkErrorResponse()
            }

            override fun onResponse(call: Call<CreatePaymentTokenSuccessResponse>, response: Response<CreatePaymentTokenSuccessResponse>) {
                _createPaymentToken.value = if (response.isSuccessful) {
                    response.body() as CreatePaymentTokenSuccessResponse
                } else {
                    val errorResponse: ResponseBody = gson.fromJson(response.errorBody()!!.charStream(), responseBodyType)
                    CreatePaymentTokenErrorResponse(errorResponse.code, errorResponse.error)
                }
            }
        })
        return _createPaymentToken // TODO eliminate multiple calls by marking request pending?
    }

    fun createPaymentTokenFromPaymentMethodId(amount: Int, currency: Constants.CurrencyType, paymentMethodId: String): LiveData<CreatePaymentTokenResponse> {
        (getApplication() as ProtonMailApplication).api.createPaymentToken(CreatePaymentTokenBody(amount, currency.name, null, paymentMethodId)).enqueue(object : Callback<CreatePaymentTokenSuccessResponse> {
            override fun onFailure(call: Call<CreatePaymentTokenSuccessResponse>, t: Throwable) {
                _createPaymentTokenFromPaymentMethodId.value = CreatePaymentTokenNetworkErrorResponse()
            }

            override fun onResponse(call: Call<CreatePaymentTokenSuccessResponse>, response: Response<CreatePaymentTokenSuccessResponse>) {
                _createPaymentTokenFromPaymentMethodId.value = if (response.isSuccessful) {
                    response.body() as CreatePaymentTokenSuccessResponse
                } else {
                    val errorResponse: ResponseBody = gson.fromJson(response.errorBody()!!.charStream(), responseBodyType)
                    CreatePaymentTokenErrorResponse(errorResponse.code, errorResponse.error)
                }
            }
        })
        return _createPaymentTokenFromPaymentMethodId // TODO eliminate multiple calls by marking request pending?
    }

}

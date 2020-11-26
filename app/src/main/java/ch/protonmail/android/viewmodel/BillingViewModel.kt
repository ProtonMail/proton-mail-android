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
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.CreatePaymentTokenBody
import ch.protonmail.android.api.models.CreatePaymentTokenErrorResponse
import ch.protonmail.android.api.models.CreatePaymentTokenNetworkErrorResponse
import ch.protonmail.android.api.models.CreatePaymentTokenResponse
import ch.protonmail.android.api.models.CreatePaymentTokenSuccessResponse
import ch.protonmail.android.api.models.PaymentType
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.core.Constants
import ch.protonmail.android.usecase.create.CreateSubscription
import ch.protonmail.android.usecase.fetch.FetchPaymentMethods
import ch.protonmail.android.usecase.model.CreateSubscriptionResult
import ch.protonmail.android.usecase.model.FetchPaymentMethodsResult
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import timber.log.Timber

class BillingViewModel @ViewModelInject constructor(
    private val protonMailApiManager: ProtonMailApiManager,
    private val createSubscription: CreateSubscription,
    private val fetchPaymentMethods: FetchPaymentMethods
) : ViewModel() {

    private val _createPaymentToken = MutableLiveData<CreatePaymentTokenResponse>()
    private val _createPaymentTokenFromPaymentMethodId = MutableLiveData<CreatePaymentTokenResponse>()
    private val _createSubscriptionData = MutableLiveData<CreateSubscriptionResult>()
    private val fetchPaymentMethodsData = MutableLiveData<FetchPaymentMethodsResult>()

    private val gson = Gson()
    private val responseBodyType = object : TypeToken<ResponseBody>() {}.type

    private lateinit var lastCreatePaymentTokenBody: CreatePaymentTokenBody
    val createSubscriptionResult: LiveData<CreateSubscriptionResult>
        get() = _createSubscriptionData

    val fetchPaymentMethodsResult: LiveData<FetchPaymentMethodsResult>
        get() = fetchPaymentMethodsData

    @JvmOverloads
    fun createPaymentToken(
        amount: Int,
        currency: Constants.CurrencyType,
        payment: PaymentType,
        token: String? = null,
        tokenType: String? = null
    ): LiveData<CreatePaymentTokenResponse> {
        lastCreatePaymentTokenBody = CreatePaymentTokenBody(amount, currency.name, payment, null)
        protonMailApiManager.createPaymentToken(lastCreatePaymentTokenBody, token, tokenType)
            .enqueue(
                object : Callback<CreatePaymentTokenSuccessResponse> {

                    override fun onFailure(call: Call<CreatePaymentTokenSuccessResponse>, t: Throwable) {
                        _createPaymentToken.value = CreatePaymentTokenNetworkErrorResponse()
                    }

                    override fun onResponse(call: Call<CreatePaymentTokenSuccessResponse>, response: Response<CreatePaymentTokenSuccessResponse>) {
                        _createPaymentToken.value = if (response.isSuccessful) {
                            response.body() as CreatePaymentTokenSuccessResponse
                        } else {
                            val errorResponse: ResponseBody = gson.fromJson(response.errorBody()!!.charStream(), responseBodyType)
                            CreatePaymentTokenErrorResponse(errorResponse.code, errorResponse.error, errorResponse.details)
                        }
                    }
                }
            )
        return _createPaymentToken // TODO eliminate multiple calls by marking request pending?
    }

    @JvmOverloads
    fun createPaymentTokenFromPaymentMethodId(
        amount: Int,
        currency: Constants.CurrencyType,
        paymentMethodId: String,
        token: String? = null,
        tokenType: String? = null
    ): LiveData<CreatePaymentTokenResponse> {
        lastCreatePaymentTokenBody = CreatePaymentTokenBody(amount, currency.name, null, paymentMethodId)
        protonMailApiManager.createPaymentToken(lastCreatePaymentTokenBody, token, tokenType)
            .enqueue(
                object : Callback<CreatePaymentTokenSuccessResponse> {

                    override fun onFailure(call: Call<CreatePaymentTokenSuccessResponse>, t: Throwable) {
                        _createPaymentTokenFromPaymentMethodId.value = CreatePaymentTokenNetworkErrorResponse()
                    }

                    override fun onResponse(call: Call<CreatePaymentTokenSuccessResponse>, response: Response<CreatePaymentTokenSuccessResponse>) {
                        _createPaymentTokenFromPaymentMethodId.value = if (response.isSuccessful) {
                            response.body() as CreatePaymentTokenSuccessResponse
                        } else {
                            val errorResponse: ResponseBody = gson.fromJson(response.errorBody()!!.charStream(), responseBodyType)
                            CreatePaymentTokenErrorResponse(errorResponse.code, errorResponse.error, errorResponse.details)
                        }
                    }
                }
            )
        return _createPaymentTokenFromPaymentMethodId // TODO eliminate multiple calls by marking request pending?
    }

    fun retryCreatePaymentToken(token: String, tokenType: String) {
        if (this::lastCreatePaymentTokenBody.isInitialized) {
            with(lastCreatePaymentTokenBody) {
                if (paymentMethodId == null) {
                    createPaymentToken(amount, Constants.CurrencyType.valueOf(currency), payment!!, token, tokenType)
                } else {
                    createPaymentTokenFromPaymentMethodId(
                        amount, Constants.CurrencyType.valueOf(currency),
                        paymentMethodId!!, token, tokenType
                    )
                }
            }
        } else {
            Timber.d("lastCreatePaymentTokenBody variable is not initialized")
        }
    }

    fun createSubscriptionForPaymentToken(
        token: String,
        amount: Int,
        currency: Constants.CurrencyType,
        planIds: MutableList<String>,
        cycle: Int,
        couponCode: String? = null
    ) {
        viewModelScope.launch {

            val result = createSubscription(
                amount,
                currency.name,
                cycle,
                planIds,
                couponCode,
                token
            )
            Timber.v("Create subscription result $result")
            _createSubscriptionData.value = result
        }
    }

    fun fetchPaymentMethodTypes() {
        viewModelScope.launch {
            val result = fetchPaymentMethods()
            fetchPaymentMethodsData.value = result
        }
    }
}

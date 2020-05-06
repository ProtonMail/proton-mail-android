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
package ch.protonmail.android.api.interceptors

import android.annotation.SuppressLint
import android.util.Log
import okhttp3.Interceptor
import okhttp3.Response

// Temporary no-op bypass interceptor (should be reworked)
class DnsOverHttpsInterceptor : Interceptor {

    @SuppressLint("LogNotTimber")
    override fun intercept(chain: Interceptor.Chain): Response {
        Log.d(TAG, "DohInterceptor: intercept")
        Log.d(TAG, "request is: " + chain.request().url())
        return chain.proceed(chain.request())
    }

    companion object {
        val TAG = "DnsOverHttpsInterceptor"
    }
}
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
package ch.protonmail.android.api.utils

import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.models.base.MultipleResponseBody
import ch.protonmail.android.utils.AppUtil
import ch.protonmail.android.utils.extensions.toPMResponseBody
import com.google.gson.Gson
import retrofit2.HttpException
import retrofit2.Response

object ParseUtils {

    inline fun <reified T : ResponseBody> parse(response: Response<T>): T {
        val obj = if (!response.isSuccessful) {
            Gson().fromJson(response.errorBody()?.string(), T::class.java)
        } else {
            response.body() as T
        }
        AppUtil.checkForErrorCodes(obj.code, obj.error)
        return obj
    }

    inline fun <reified T : MultipleResponseBody> parse(response: Response<T>): T {
        val obj = if (!response.isSuccessful) {
            Gson().fromJson(response.errorBody()?.string(), T::class.java)
        } else {
            response.body() as T
        }
        AppUtil.checkForErrorCodes(obj.code, obj.error)
        return obj
    }

    fun doOnError(it: Throwable) {
        if (it is HttpException) {
            val obj = it.toPMResponseBody()
            obj?.let {
                AppUtil.checkForErrorCodes(obj.code, obj.error)
            }
        }
    }

    fun compileSingleErrorMessage(errorMap: Map<String, String>): String {
        val errorBuilder = StringBuilder()
        val newLine = "\n"
        for (entry in errorMap.entries) {
            errorBuilder.append(entry.key + " : " + entry.value)
            errorBuilder.append(newLine)
        }
        return errorBuilder.toString()
    }
}

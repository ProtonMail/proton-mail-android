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
package ch.protonmail.android.api.segments

import android.util.Pair
import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.utils.ParseUtils
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.util.Collections
import java.util.concurrent.ArrayBlockingQueue

// region constants
const val REFRESH_PATH = "/auth/refresh"
const val AUTH_PATH = "auth"
const val AUTH_INFO_PATH = "auth/info"
const val ATTACH_PATH = "attach"

const val ONE_MINUTE = 60L
const val THREE_SECONDS = 3L
const val TEN_SECONDS = 10L
const val THIRTY_SECONDS = 30L

const val HEADER_LOCALE = "x-pm-locale"
const val HEADER_UID = "x-pm-uid"
const val HEADER_APP_VERSION = "x-pm-appversion"
const val HEADER_AUTH = "Authorization"
const val HEADER_USER_AGENT = "User-Agent"

const val RESPONSE_CODE_UNAUTHORIZED = 401
const val RESPONSE_CODE_GATEWAY_TIMEOUT = 504
const val RESPONSE_CODE_TOO_MANY_REQUESTS = 429
const val RESPONSE_CODE_UNPROCESSABLE_ENTITY = 422
const val RESPONSE_CODE_SERVICE_UNAVAILABLE = 503
const val RESPONSE_CODE_INVALID_ID = 2061
const val RESPONSE_CODE_MESSAGE_READING_RESTRICTED = 2028
const val RESPONSE_CODE_ERROR_GROUP_ALREADY_EXIST = 2500
const val RESPONSE_CODE_INVALID_APP_CODE = 5002
const val RESPONSE_CODE_FORCE_UPGRADE = 5003
const val RESPONSE_CODE_OLD_PASSWORD_INCORRECT = 8002
const val RESPONSE_CODE_ERROR_VERIFICATION_NEEDED = 9001
const val RESPONSE_CODE_NOT_ALLOWED = 2011
const val RESPONSE_CODE_AUTH_AUTH_ACCOUNT_FAILED_GENERIC = 10_001
const val RESPONSE_CODE_AUTH_AUTH_ACCOUNT_DELETED = 10_002
const val RESPONSE_CODE_AUTH_AUTH_ACCOUNT_DISABLED = 10_003
const val RESPONSE_CODE_ATTACHMENT_DELETE_ID_INVALID = 11_123
const val RESPONSE_CODE_EMAIL_FAILED_VALIDATION = 12_006
const val RESPONSE_CODE_NEW_PASSWORD_INCORRECT = 12_022
const val RESPONSE_CODE_NEW_PASSWORD_MESSED_UP = 12_020
const val RESPONSE_CODE_INVALID_EMAIL = 12_065
const val RESPONSE_CODE_INCORRECT_PASSWORD = 12_066
const val RESPONSE_CODE_ERROR_CONTACT_EXIST_THIS_EMAIL = 13_002
const val RESPONSE_CODE_ERROR_INVALID_EMAIL = 13_006
const val RESPONSE_CODE_ERROR_EMAIL_EXIST = 13_007
const val RESPONSE_CODE_ERROR_EMAIL_VALIDATION_FAILED = 13_014
const val RESPONSE_CODE_ERROR_EMAIL_DUPLICATE_FAILED = 13_061
const val RESPONSE_CODE_MESSAGE_DOES_NOT_EXIST = 15_052
const val RESPONSE_CODE_RECIPIENT_NOT_FOUND = 33_102
// endregion

open class BaseApi {
    protected inline fun <reified T : ResponseBody> executeAll(list: List<Call<T>>): List<T?> {
        if (list.isEmpty()) {
            return emptyList()
        }
        val queue = ArrayBlockingQueue<Pair<Int, Any?>>(list.size)
        for (i in 0 until list.size) {
            val call = list[i]
            call.enqueue(object : Callback<T> {
                override fun onFailure(call: Call<T>, t: Throwable) {
                    queue.add(Pair(i, t))
                }

                override fun onResponse(call: Call<T>, response: Response<T>) {
                    queue.add(Pair(i, ParseUtils.parse(response)))
                }
            })
        }

        val result = ArrayList<T?>(Collections.nCopies(list.size, null))
        try {
            for (call in list) {
                val pair = queue.take()
                if (pair.second is Throwable) {
                    throw pair.second as Throwable
                }
                result[pair.first] = pair.second as T
            }
        } catch (e: InterruptedException) {
            throw RuntimeException("Interrupted!")
        }

        return List(list.size) { i: Int -> result[i] }
    }
}

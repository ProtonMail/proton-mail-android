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
package ch.protonmail.android.contacts.groups

import androidx.lifecycle.ViewModel
import ch.protonmail.android.api.models.ResponseBody
import com.google.gson.Gson
import retrofit2.HttpException

/**
 * Created by kadrikj on 9/21/18. */
open class ContactGroupsBaseViewModel: ViewModel() {

    fun parseErrorApiResponse(exception: HttpException): String? {
        return try {
            val responseBody = Gson().fromJson(exception.response()?.errorBody()?.string(), ResponseBody::class.java)
            responseBody.error
        } catch (e: Exception) {
            null
        }
    }
}

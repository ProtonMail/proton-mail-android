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
package ch.protonmail.android.api.models.room.contacts

import androidx.room.TypeConverter
import com.google.gson.Gson

/**
 * Created by kadrikj on 8/30/18. */
class ContactEmailConverter {
    @TypeConverter
    fun contactEmailLabelsToString(contactEmailLabels: List<String>?): String? {
        if (contactEmailLabels.orEmpty().isEmpty()) {
            return ""
        }
        return Gson().toJson(contactEmailLabels)
    }

    @TypeConverter
    fun stringToContactEmailLabels(value: String?): List<String>? {
        if (value.orEmpty().isEmpty()) {
            return ArrayList()
        }
        return Gson().fromJson(value, Array<String>::class.java).asList()
    }
}
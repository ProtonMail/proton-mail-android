/*
 * Copyright (c) 2022 Proton AG
 *
 * This file is part of Proton Mail.
 *
 * Proton Mail is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Proton Mail is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Proton Mail. If not, see https://www.gnu.org/licenses/.
 */
package ch.protonmail.android.utils

import android.content.SharedPreferences
import com.google.gson.Gson

private val gson = Gson()

/**
 * Equivalent of [SharedPreferences.Editor.putStringSet] but for lists, preserving order.
 */
fun SharedPreferences.Editor.putStringList(key: String, values: List<String>?): SharedPreferences.Editor {
    return if (values == null) {
        this.remove(key)
    } else {
        this.putString(key, gson.toJson(values))
    }
}

/**
 * Equivalent of [SharedPreferences.getStringSet] but for lists, preserving order.
 */
fun SharedPreferences.getStringList(key: String, defValues: List<String>?): List<String>? {
    val json = this.getString(key, null)
    return if (json == null) {
        defValues
    } else {
        gson.fromJson<Array<String>>(json, Array<String>::class.java).toList()
    }
}

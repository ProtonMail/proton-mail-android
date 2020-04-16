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
package ch.protonmail.android.utils.extensions

import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.json.JSONObject



fun SharedPreferences.saveMap(key: String, map: HashMap<String, String>) {
    val mapString = Gson().toJson(map)
    edit().putString(key, mapString).apply()
}

fun SharedPreferences.loadMap(key: String) : HashMap<String, String> {
    val mapString = getString(key, JsonObject().toString())
    val hashMap = HashMap<String, String>()
    val jsonObject = JSONObject(mapString)
    val keysItr = jsonObject.keys()
    while (keysItr.hasNext()) {
        val key = keysItr.next()
        val value = jsonObject.get(key) as String
        hashMap[key] = value
    }
    return hashMap
}

fun SharedPreferences.findValueByKeyInMap(prefsKey: String, mapKey: String): String? {
    val map = loadMap(prefsKey)
    return map[mapKey]
}
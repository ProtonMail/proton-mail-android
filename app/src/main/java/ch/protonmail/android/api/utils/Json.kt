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

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule

/**
 * Created by dinokadrikj on 3/5/20.
 */
object Json {
    var MAPPER: ObjectMapper = ObjectMapper()

    fun toString(value: Any?): String? {
        return try {
            MAPPER.writeValueAsString(value)
        } catch (e: JsonProcessingException) {
            null
        }
    }

    init {
        val dateModule = SimpleModule("date", Version(1, 0, 0, null, null, null))
        MAPPER.registerModule(dateModule)
        MAPPER.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true)
        MAPPER.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        MAPPER.configure(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES, false)
        MAPPER.configure(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES, true)
    }
}
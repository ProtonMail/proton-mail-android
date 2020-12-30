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
package ch.protonmail.android.api.models

import androidx.room.ColumnInfo
import androidx.room.Ignore
import ch.protonmail.android.api.models.room.contacts.COLUMN_CONTACT_DATA_NAME
import ch.protonmail.android.api.models.room.contacts.COLUMN_CONTACT_EMAILS_EMAIL
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParseException
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.io.Serializable
import java.lang.reflect.Type

class MessageRecipient : Serializable, Comparable<MessageRecipient> {
    @ColumnInfo(name = COLUMN_CONTACT_DATA_NAME)
    val name: String

    @ColumnInfo(name = COLUMN_CONTACT_EMAILS_EMAIL)
    val address: String

    @Ignore
    var icon = 0 // for pgp

    @Ignore
    var iconColor = 0 // for pgp

    @Ignore
    var description = 0 // for clicking description

    @Ignore
    var isPGP = false

    @Ignore
    var group: String? = null

    @Ignore
    var groupIcon = 0

    @Ignore
    var groupColor = 0

    @Ignore
    var groupRecipients: List<MessageRecipient>? = null

    @Ignore
    var isSelected = false

    constructor(name: String, address: String, group: String?) {
        this.name = name
        this.address = address
        this.group = group
    }

    constructor(name: String, address: String) {
        this.name = name
        this.address = address
    }

    override fun toString() = "$name $address"

    override fun compareTo(other: MessageRecipient) = name.compareTo(other.name)

    // the code below is Android 6 bug fix
    class MessageRecipientSerializer : JsonSerializer<MessageRecipient> {
        override fun serialize(src: MessageRecipient, typeOfSrc: Type, context: JsonSerializationContext): JsonElement {
            val json = JsonObject()
            json.addProperty("Name", src.name)
            json.addProperty("Address", src.address)
            json.addProperty("Group", src.group)
            return json
        }
    }

    class MessageRecipientDeserializer : JsonDeserializer<MessageRecipient> {
        @Throws(JsonParseException::class)
        override fun deserialize(
            json: JsonElement,
            typeOfT: Type,
            context: JsonDeserializationContext
        ): MessageRecipient {
            val jsonObject = json as JsonObject
            val address = jsonObject["Address"].asString
            val name = jsonObject["Name"].asString
            val groupJsonElement = jsonObject["Group"]
            var messageRecipient = MessageRecipient(name, address)
            if (groupJsonElement != null) {
                val group = groupJsonElement.asString
                messageRecipient = MessageRecipient(name, address, group)
            }
            return messageRecipient
        }
    }
}

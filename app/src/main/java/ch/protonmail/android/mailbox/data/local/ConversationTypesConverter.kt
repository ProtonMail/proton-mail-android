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

package ch.protonmail.android.mailbox.data.local

import androidx.room.TypeConverter
import ch.protonmail.android.data.local.model.MessageSender
import ch.protonmail.android.mailbox.data.local.model.LabelContextDatabaseModel
import com.google.gson.Gson

class ConversationTypesConverter {

    @TypeConverter
    fun messageRecipientsListToString(messageSenders: List<MessageSender>?): String? =
        Gson().toJson(messageSenders)

    @TypeConverter
    fun stringToMessageSendersList(messageSendersString: String?): List<MessageSender>? {
        messageSendersString ?: return null
        return Gson().fromJson(messageSendersString, Array<MessageSender>::class.java).asList()
    }

    @TypeConverter
    fun labelsContextListToString(labelsContext: List<LabelContextDatabaseModel>?): String? =
        Gson().toJson(labelsContext)

    @TypeConverter
    fun stringToLabelsContextList(labelContextString: String?): List<LabelContextDatabaseModel>? {
        labelContextString ?: return null
        return Gson().fromJson(labelContextString, Array<LabelContextDatabaseModel>::class.java).asList()
    }
}

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
package ch.protonmail.android.data.local.model

import androidx.room.ColumnInfo
import com.google.gson.annotations.SerializedName
import java.io.Serializable

data class MessageSender constructor(

    @ColumnInfo(name = COLUMN_MESSAGE_SENDER_NAME)
    var name: String?,

    @ColumnInfo(name = COLUMN_MESSAGE_SENDER_EMAIL)
    @SerializedName("Address")
    var emailAddress: String?,

    @ColumnInfo(name = COLUMN_MESSAGE_SENDER_IS_PROTON, defaultValue = "0")
    val isProton: Boolean = false

) : Serializable, Comparable<MessageSender> {

    constructor(name: String?, emailAddress: String?) : this(name, emailAddress, false)

    override fun toString(): String =
        "$name $emailAddress"

    override fun compareTo(other: MessageSender) =
        name!!.compareTo(other.name!!)
}

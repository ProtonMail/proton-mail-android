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

import ch.protonmail.android.api.models.ContactEncryptedData
import ch.protonmail.android.api.models.ResponseBody
import com.google.gson.annotations.SerializedName
import java.io.Serializable

private const val FIELD_CONTACT = "Contact"
private const val FIELD_ID = "ID"
private const val FIELD_NAME = "Name"
private const val FIELD_UID = "UID"
private const val FIELD_CREATE_TIME = "CreateTime"
private const val FIELD_MODIFY_TIME = "ModifyTime"
private const val FIELD_SIZE = "Size"
private const val FIELD_DEFAULTS = "Defaults"
private const val FIELD_EMAILS = "ContactEmails"
private const val FIELD_CARDS = "Cards"

data class ServerFullContactDetails(

    @SerializedName(FIELD_ID)
    var id: String,

    @SerializedName(FIELD_NAME)
    var name: String,

    @SerializedName(FIELD_UID)
    var uid: String,

    @SerializedName(FIELD_CREATE_TIME)
    var createTime: Long,

    @SerializedName(FIELD_MODIFY_TIME)
    var modifyTime: Long,

    @SerializedName(FIELD_SIZE)
    var size: Int,

    @SerializedName(FIELD_DEFAULTS)
    var defaults: Int,

    @SerializedName(FIELD_EMAILS)
    var emails: List<ContactEmail>,

    @SerializedName(FIELD_CARDS)
    var encryptedData: List<ContactEncryptedData>?

) : Serializable


class FullContactDetailsResponse : ResponseBody() {

    @SerializedName(FIELD_CONTACT)
    private lateinit var serverContact: ServerFullContactDetails

    val contact: FullContactDetails by lazy {
        val factory = FullContactDetailsFactory()
        factory.createFullContactDetails(serverContact)
    }
}

class FullContactDetailsFactory {

    fun createFullContactDetails(serverFullContactDetails: ServerFullContactDetails): FullContactDetails =
        with(serverFullContactDetails) {
            FullContactDetails(
                id,
                name,
                uid,
                createTime,
                modifyTime,
                size,
                defaults,
                emails,
                encryptedData?.toMutableList() ?: mutableListOf()
            )
        }
}

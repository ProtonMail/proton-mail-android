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
package ch.protonmail.android.api.models.messages.receive

import ch.protonmail.android.api.models.ResponseBody
import ch.protonmail.android.api.models.contacts.receive.ContactLabelFactory
import ch.protonmail.android.core.Constants
import com.google.gson.annotations.SerializedName

// region constants
private const val FIELD_LABEL = "Label"
// endregion

/**
 * Created by dkadrikj on 17.7.15.
 */

class LabelResponse : ResponseBody() {

    @SerializedName(FIELD_LABEL)
    private lateinit var serverLabel: ServerLabel

    val label by lazy {
        val labelFactory = LabelFactory()
        labelFactory.createDBObjectFromServerObject(serverLabel)
    }

    val contactGroup by lazy {
        val contactLabelFactory = ContactLabelFactory()
        contactLabelFactory.createDBObjectFromServerObject(serverLabel)
    }

    fun hasError(): Boolean {
        return code != Constants.RESPONSE_CODE_OK
    }

}

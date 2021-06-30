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

import ch.protonmail.android.api.models.LabelBody
import ch.protonmail.android.api.utils.Fields
import com.google.gson.annotations.SerializedName

/**
 * Created by Kamil Rajtar on 19.07.18.
 * TODO: Move to more appropriate package, because contact groups and message labels depend on this class.
 * */
data class ServerLabel(
    var ID: String? = null,
    @SerializedName(Fields.Label.NAME)
    var name: String? = null,
    @SerializedName(Fields.Label.COLOR)
    var color: String? = null,
    @SerializedName(Fields.Label.DISPLAY)
    var display: Int? = null,
    @SerializedName(Fields.Label.ORDER)
    var order: Int? = null,
    @SerializedName(Fields.Label.EXCLUSIVE)
    var exclusive: Int? = null,
    @SerializedName(Fields.Label.TYPE)
    var type: Int? = null
) {

    val labelBody by lazy {
        LabelBody(name, color, display!!, exclusive!!, type!!)
    }
}

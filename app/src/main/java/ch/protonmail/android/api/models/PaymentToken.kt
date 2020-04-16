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

import com.google.gson.annotations.SerializedName

data class PaymentToken(val type: String = "token") {

    enum class Status(val id: Int) {
        @SerializedName("0")
        PENDING(0),
        @SerializedName("1")
        CHARGEABLE(1),
        @SerializedName("2")
        FAILED(2),
        @SerializedName("3")
        CONSUMED(3),
        @SerializedName("4")
        NOT_SUPPORTED(4)
    }

}


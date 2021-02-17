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

import ch.protonmail.android.api.utils.Fields
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Organization(
    @SerialName(Fields.Organization.DISPLAY_NAME)
    val displayName: String?,

    @SerialName(Fields.Organization.PLAN_NAME)
    val planName: String? = null,

    @SerialName(Fields.Organization.VPN_PLAN_NAME)
    val vpnPlanName: String? = null,

    @SerialName(Fields.Organization.MAX_DOMAINS)
    val maxDomains: Int,

    @SerialName(Fields.Organization.MAX_ADDRESSES)
    val maxAddresses: Int,

    @SerialName(Fields.Organization.MAX_SPACE)
    val maxSpace: Long,

    @SerialName(Fields.Organization.MAX_MEMBERS)
    val maxMembers: Int,

    @SerialName(Fields.Organization.MAX_VPN)
    val maxVPN: Int,

    @SerialName(Fields.Organization.TWO_FACTOR_GRACE_PERIOD)
    val twoFactor: Int?,

    @SerialName(Fields.Organization.USED_DOMAINS)
    val usedDomains: Int,

    @SerialName(Fields.Organization.USED_MEMBERS)
    val usedMembers: Int,

    @SerialName(Fields.Organization.USED_ADDRESSES)
    val usedAddresses: Int,

    @SerialName(Fields.Organization.USED_SPACE)
    val usedSpace: Long,

    @SerialName(Fields.Organization.ASSIGNED_SPACE)
    val assignedSpace: Long,
)

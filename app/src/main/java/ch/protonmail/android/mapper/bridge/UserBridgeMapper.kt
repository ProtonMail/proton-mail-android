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
@file:Suppress("DEPRECATION") // Suppress deprecated usages from old entity

package ch.protonmail.android.mapper.bridge

import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.domain.entity.NotBlankString
import ch.protonmail.android.domain.entity.bytes
import ch.protonmail.android.domain.entity.user.Delinquent
import ch.protonmail.android.domain.entity.user.Plan
import ch.protonmail.android.domain.entity.user.Role
import ch.protonmail.android.domain.entity.user.User
import ch.protonmail.android.domain.entity.user.UserSpace
import me.proton.core.util.kotlin.invoke
import me.proton.core.util.kotlin.takeIfNotBlank
import me.proton.core.util.kotlin.toBoolean
import javax.inject.Inject
import ch.protonmail.android.api.models.User as OldUser

/**
 * Transforms [ch.protonmail.android.api.models.User] to [ch.protonmail.android.domain.entity.user.User]
 * Inherit from [BridgeMapper]
 */
class UserBridgeMapper @Inject constructor(
    private val addressMapper: AddressesBridgeMapper,
    private val keysMapper: UserKeysBridgeMapper
) : BridgeMapper<OldUser, User> {

    override fun OldUser.toNewModel(): User {

        return User(
            id = Id(id),
            name = Name(name),
            addresses = addressMapper { addresses.toNewModel() },
            keys = keysMapper { keys.toNewModel() },
            plans = getPlans(services, subscribed),
            private = private.toBoolean(),
            role = getRole(role),
            organizationPrivateKey = getOrganizationKey(organizationPrivateKey),
            currency = NotBlankString(currency),
            credits = credit,
            delinquent = getDelinquent(delinquentValue),
            totalUploadLimit = maxUpload.bytes,
            dedicatedSpace = UserSpace(usedSpace.bytes, maxSpace.bytes)
        )
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun getPlans(services: Int, subscribed: Int) = buildSet {
        fun Int.hasMail() = MAIL_PLAN_VALUE and this == MAIL_PLAN_VALUE
        fun Int.hasVpn() = VPN_PLAN_VALUE and this == VPN_PLAN_VALUE

        if (subscribed.hasMail()) add(Plan.Mail.Paid)
        else if (services.hasMail()) add(Plan.Mail.Free)

        if (subscribed.hasVpn()) add(Plan.Vpn.Paid)
        else if (services.hasVpn()) add(Plan.Vpn.Free)
    }

    private fun getRole(value: Int) = Role.values().first { it.i == value }

    private fun getOrganizationKey(key: String?) = key?.takeIfNotBlank()?.let(::NotBlankString)

    private fun getDelinquent(value: Int) = when (value.toUInt()) {
        Delinquent.None.i -> Delinquent.None
        Delinquent.InvoiceAvailable.i -> Delinquent.InvoiceAvailable
        Delinquent.InvoiceOverdue.i -> Delinquent.InvoiceOverdue
        Delinquent.InvoiceDelinquent.i -> Delinquent.InvoiceDelinquent
        Delinquent.IncomingMailDisabled.i -> Delinquent.IncomingMailDisabled
        else -> throw IllegalArgumentException("Cannot get Delinquent for value $value")
    }

    companion object {
        private const val MAIL_PLAN_VALUE = 1 // 001
        private const val VPN_PLAN_VALUE = 4 // 100

        /**
         * @return default configuration for [UserBridgeMapper]
         * This is temporary and intended to be used ONLY where a proper DI is not possible ATM
         */
        @JvmStatic
        fun buildDefault() = UserBridgeMapper(
            AddressesBridgeMapper(AddressBridgeMapper(AddressKeysBridgeMapper(AddressKeyBridgeMapper()))),
            UserKeysBridgeMapper(UserKeyBridgeMapper())
        )
    }
}

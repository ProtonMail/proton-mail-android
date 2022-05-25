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
package ch.protonmail.android.domain.entity.user

import ch.protonmail.android.domain.entity.Bytes
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.domain.entity.NotBlankString
import ch.protonmail.android.domain.entity.Validable
import ch.protonmail.android.domain.entity.Validated
import ch.protonmail.android.domain.entity.Validator
import ch.protonmail.android.domain.entity.requireValid
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.AddressId

/**
 * Representation of a server user.
 *
 * [Validable]
 *
 * * [addresses] can be empty only if no [Plan.Mail] is available in [plans]
 *
 * * [keys] can be empty if no [addresses] is set
 *
 * * [plans] contains at most 1 [Plan.Mail] and at most 1 [Plan.Vpn]
 *
 * @author Davide Farella
 */
@Validated
data class User( // TODO: consider naming UserInfo or similar
    val id: UserId,
    val name: Name,
    val addresses: Addresses,
    val keys: UserKeys,
    val plans: Collection<Plan>,
    /**
     * Whether the user controls their own keys or not, all free users are Private
     */
    val private: Boolean,
    val role: Role,
    val currency: NotBlankString, // might not be worth to have an endless enum
    /**
     * Monetary credits for this user, this value is affected by [currency]
     */
    val credits: Int,
    val delinquent: Delinquent,
    /**
     * Size limit for a Message body + sum of attachments
     */
    val totalUploadLimit: Bytes,
    val dedicatedSpace: UserSpace

) : Validable by Validator<User>({

    // Addresses
    require(addresses.hasAddresses || plans.none { it is Plan.Mail }) { "Mail plan but no addresses" }

    // Keys
    require(keys.hasKeys || !addresses.hasAddresses) { "Has addresses but no keys" }

    // Plans
    require(
        plans.count { it is Plan.Mail } <= 1 &&
            plans.count { it is Plan.Vpn } <= 1
    ) {
        "Has 2 or more plans of the same type"
    }
}) {

    init {
        requireValid()
    }

    fun findAddressById(addressId: AddressId): Address? =
        addresses.addresses.values.find { it.id == addressId }
}

sealed class Delinquent(val i: UInt, val mailRoutesAccessible: Boolean = true) {

    object None : Delinquent(0.toUInt())
    object InvoiceAvailable : Delinquent(1.toUInt())
    object InvoiceOverdue : Delinquent(2.toUInt())
    object InvoiceDelinquent : Delinquent(3.toUInt(), mailRoutesAccessible = false)
    object IncomingMailDisabled : Delinquent(4.toUInt(), mailRoutesAccessible = false)
}

enum class Role(val i: Int) {
    NO_ORGANIZATION(0),
    ORGANIZATION_MEMBER(1),
    ORGANIZATION_ADMIN(2)
}

// TODO can this entity be used on other spaces under a different name?
data class UserSpace(val used: Bytes, val total: Bytes)

fun User.isPaidUser() = plans.filterIsInstance<Plan.Mail.Paid>().isNotEmpty()

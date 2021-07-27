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

import me.proton.core.domain.entity.UserId
import ch.protonmail.android.domain.entity.NotBlankString
import ch.protonmail.android.domain.entity.PgpField
import ch.protonmail.android.domain.entity.user.AddressKey
import ch.protonmail.android.domain.entity.user.AddressKeys
import me.proton.core.domain.arch.map
import me.proton.core.util.kotlin.invoke
import me.proton.core.util.kotlin.takeIfNotBlank
import javax.inject.Inject
import ch.protonmail.android.api.models.Keys as OldKey

/**
 * Transforms a collection of [ch.protonmail.android.api.models.Keys] to
 * [ch.protonmail.android.domain.entity.user.AddressKeys]
 * Inherit from [BridgeMapper]
 */
class AddressKeysBridgeMapper @Inject constructor(
    private val keyMapper: AddressKeyBridgeMapper
) : BridgeMapper<Collection<OldKey>, AddressKeys> {

    override fun Collection<OldKey>.toNewModel(): AddressKeys {
        if (isEmpty()) return AddressKeys.Empty

        val primaryOldKey = find { it.isPrimary } ?: first()
        val primaryKey = keyMapper { primaryOldKey.toNewModel() }

        return AddressKeys(primaryKey, map(keyMapper) { it.toNewModel() })
    }
}

/**
 * Transforms a single of [ch.protonmail.android.api.models.Keys] to
 * [ch.protonmail.android.domain.entity.user.AddressKey]
 * Inherit from [BridgeMapper]
 */
class AddressKeyBridgeMapper @Inject constructor() : BridgeMapper<OldKey, AddressKey> {

    override fun OldKey.toNewModel() = AddressKey(
        id = UserId(id),
        version = 4.toUInt(), // TODO not implemented on old Keys
        canEncrypt = canEncrypt(flags),
        canVerifySignature = canVerifySignature(flags),
        publicKey = PgpField.PublicKey(NotBlankString("none")), // TODO not implemented on old Keys!!!
        privateKey = PgpField.PrivateKey(NotBlankString(privateKey)),
        token = getToken(token),
        signature = getSignature(signature),
        activation = getActivation(activation),
        active = getActive(active)
    )

    private fun canEncrypt(flags: Int) =
        CAN_ENCRYPT_VALUE and flags == CAN_ENCRYPT_VALUE

    private fun canVerifySignature(flags: Int) =
        CAN_VERIFY_SIGNATURE and flags == CAN_VERIFY_SIGNATURE

    private fun getToken(token: String?) =
        token?.takeIfNotBlank()?.let { PgpField.Message(NotBlankString(it)) }

    private fun getSignature(signature: String?) =
        signature?.takeIfNotBlank()?.let { PgpField.Signature(NotBlankString(it)) }

    private fun getActivation(activation: String?) =
        activation?.takeIfNotBlank()?.let { PgpField.Message(NotBlankString(it)) }

    private fun getActive(active: Int) = active == 1

    private companion object {
        const val CAN_VERIFY_SIGNATURE = 1 // 01
        const val CAN_ENCRYPT_VALUE = 2 // 10
    }
}

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

package ch.protonmail.android.feature.user

import ch.protonmail.android.api.models.Keys
import ch.protonmail.android.api.models.address.Address
import kotlinx.coroutines.runBlocking
import me.proton.core.crypto.common.keystore.EncryptedByteArray
import me.proton.core.domain.entity.UserId
import me.proton.core.key.domain.extension.primary
import me.proton.core.network.domain.ApiException
import me.proton.core.user.domain.UserManager
import me.proton.core.user.domain.entity.User
import me.proton.core.user.domain.entity.UserAddress
import me.proton.core.user.domain.extension.primary
import me.proton.core.util.kotlin.toInt

@Deprecated("Replaced by Core UserManager", ReplaceWith("Core UserManager"))
@Throws(ApiException::class)
fun UserManager.getUserBlocking(userId: UserId): User = runBlocking {
    getUser(userId)
}

@Deprecated("Replaced by Core UserManager", ReplaceWith("Core UserManager"))
@Throws(ApiException::class)
fun UserManager.getAddressesBlocking(userId: UserId): List<UserAddress> = runBlocking {
    // Refresh only if we have no address.
    getAddresses(userId, refresh = getAddresses(userId).isEmpty())
}

@Deprecated("Replaced by Core UserManager", ReplaceWith("Core UserManager"))
@Throws(ApiException::class)
fun UserManager.getPrimaryAddressBlocking(userId: UserId): UserAddress? = getAddressesBlocking(userId).primary()

@Deprecated("Replaced by Core UserManager", ReplaceWith("Core UserManager"))
@Throws(ApiException::class)
fun UserManager.getLegacyKeysBlocking(userId: UserId): List<Keys> = getUserBlocking(userId).keys.map { key ->
    Keys(
        key.keyId.id,
        key.privateKey.key,
        0,
        key.privateKey.isPrimary.toInt(),
        null,
        null,
        key.activation,
        key.privateKey.isActive.toInt()
    )
}

@Deprecated("Replaced by Core UserManager", ReplaceWith("Core UserManager"))
@Throws(ApiException::class)
fun UserManager.getLegacyAddressesBlocking(userId: UserId): List<Address> =
    getAddressesBlocking(userId).map { address ->
        Address(
            address.addressId.id,
            address.domainId,
            address.email,
            address.canSend.toInt(),
            address.canReceive.toInt(),
            address.enabled.toInt(),
            requireNotNull(address.type?.value),
            address.order,
            address.displayName,
            address.signature,
            address.keys.isNotEmpty().toInt(),
            address.keys.map { key ->
                Keys(
                    key.keyId.id,
                    key.privateKey.key,
                    key.flags,
                    key.privateKey.isPrimary.toInt(),
                    key.token,
                    key.signature,
                    key.activation,
                    key.active.toInt()
                )
            }
        )
    }

@Deprecated("Replaced by Core UserManager", ReplaceWith("Core User.useKeys"))
@Throws(ApiException::class)
fun User.getUserPassphrase(): EncryptedByteArray? =
    keys.primary()?.privateKey?.passphrase

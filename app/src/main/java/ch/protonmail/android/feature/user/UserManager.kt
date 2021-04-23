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

package ch.protonmail.android.feature.user

import ch.protonmail.android.api.models.Keys
import ch.protonmail.android.api.models.address.Address
import ch.protonmail.android.domain.entity.Id
import kotlinx.coroutines.runBlocking
import me.proton.core.crypto.common.keystore.decryptWith
import me.proton.core.domain.entity.UserId
import me.proton.core.key.domain.extension.primary
import me.proton.core.network.domain.ApiException
import me.proton.core.user.domain.UserManager
import me.proton.core.user.domain.extension.primary
import me.proton.core.util.kotlin.toInt
import kotlin.jvm.Throws

@Deprecated("Replaced by Core UserManager", ReplaceWith("Core UserManager"))
@Throws(ApiException::class)
fun UserManager.getUserBlocking(userId: Id) = runBlocking {
    getUser(UserId(userId.s))
}

@Deprecated("Replaced by Core UserManager", ReplaceWith("Core UserManager"))
@Throws(ApiException::class)
fun UserManager.getAddressesBlocking(userId: Id) = runBlocking {
    // Refresh only if we have no address.
    getAddresses(UserId(userId.s), refresh = getAddresses(UserId(userId.s)).isEmpty())
}

@Deprecated("Replaced by Core UserManager", ReplaceWith("Core UserManager"))
@Throws(ApiException::class)
fun UserManager.getPrimaryAddressBlocking(userId: Id) = getAddressesBlocking(userId).primary()

@Deprecated("Replaced by Core UserManager", ReplaceWith("Core UserManager"))
@Throws(ApiException::class)
fun ch.protonmail.android.core.UserManager.getMailboxPasswordBlocking(userId: Id): ByteArray? = runBlocking {
    getMailboxPassword(userId)
}

@Deprecated("Replaced by Core UserManager", ReplaceWith("Core UserManager"))
@Throws(ApiException::class)
fun UserManager.getLegacyKeysBlocking(userId: Id) = getUserBlocking(userId).keys.map { key ->
    Keys(
        key.keyId.id,
        key.privateKey.key,
        0,
        key.privateKey.isPrimary.toInt(),
        null,
        null,
        key.activation,
        1
    )
}

@Deprecated("Replaced by Core UserManager", ReplaceWith("Core UserManager"))
@Throws(ApiException::class)
fun UserManager.getLegacyAddressesBlocking(userId: Id) = getAddressesBlocking(userId).map { address ->
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
        null,
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

@Deprecated("Replaced by Core UserManager", ReplaceWith("Core UserManager"))
@Throws(ApiException::class)
suspend fun ch.protonmail.android.core.UserManager.getMailboxPassword(userId: Id): ByteArray? {
    val user = coreUserManager.getUser(UserId(userId.s))
    val passphrase = user.keys.primary()?.privateKey?.passphrase
    val decryptedPassphrase = checkNotNull(passphrase).decryptWith(coreKeyStoreCrypto)
    return decryptedPassphrase.array
}

@Deprecated("Replaced by Core UserManager", ReplaceWith("Core UserManager"))
@Throws(ApiException::class)
suspend fun ch.protonmail.android.core.UserManager.getCurrentUserMailboxPassword(): ByteArray? =
    currentUserId?.let { getMailboxPassword(it) }

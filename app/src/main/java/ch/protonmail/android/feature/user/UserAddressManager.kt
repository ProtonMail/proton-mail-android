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

import kotlinx.coroutines.runBlocking
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.UserAddressManager
import me.proton.core.user.domain.entity.AddressId

@Deprecated("Replaced by Core UserAddressManager", ReplaceWith("Core UserAddressManager"))
fun UserAddressManager.updateAddressBlocking(
    userId: UserId,
    addressId: UserId,
    displayName: String? = null,
    signature: String? = null
) = runBlocking {
    updateAddress(UserId(userId.id), AddressId(addressId.id), displayName, signature)
}

@Deprecated("Replaced by Core UserAddressManager", ReplaceWith("Core UserAddressManager"))
fun UserAddressManager.updateOrderBlocking(
    userId: UserId,
    addressIds: List<UserId>
) = runBlocking {
    updateOrder(UserId(userId.id), addressIds.map { AddressId(it.id) })
}

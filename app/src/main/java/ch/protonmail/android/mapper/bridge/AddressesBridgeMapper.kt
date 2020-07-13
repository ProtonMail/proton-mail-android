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
package ch.protonmail.android.mapper.bridge

import ch.protonmail.android.domain.entity.user.Addresses
import ch.protonmail.android.api.models.address.Address as OldAddress

/**
 * Transforms a collection of [ch.protonmail.android.api.models.address.Address] to
 * [ch.protonmail.android.domain.entity.user.Addresses]
 * Inherit from [BridgeMapper]
 */
class AddressesBridgeMapper : BridgeMapper<Collection<OldAddress>, Addresses> {

    override fun Collection<OldAddress>.toNewModel(): Addresses {
        TODO("Not yet implemented")
    }
}

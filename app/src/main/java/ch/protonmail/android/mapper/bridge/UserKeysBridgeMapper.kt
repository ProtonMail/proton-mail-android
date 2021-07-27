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
import ch.protonmail.android.domain.entity.user.UserKey
import ch.protonmail.android.domain.entity.user.UserKeys
import me.proton.core.domain.arch.map
import me.proton.core.util.kotlin.invoke
import me.proton.core.util.kotlin.takeIfNotBlank
import javax.inject.Inject
import ch.protonmail.android.api.models.Keys as OldKey

/**
 * Transforms a collection of [ch.protonmail.android.api.models.Keys] to
 * [ch.protonmail.android.domain.entity.user.UserKeys]
 * Inherit from [BridgeMapper]
 */
class UserKeysBridgeMapper @Inject constructor(
    private val keyMapper: UserKeyBridgeMapper
) : BridgeMapper<Collection<OldKey>, UserKeys> {

    override fun Collection<OldKey>.toNewModel(): UserKeys {
        if (isEmpty()) return UserKeys.Empty

        val primaryOldKey = find { it.isPrimary } ?: first()
        val primaryKey = keyMapper { primaryOldKey.toNewModel() }

        return UserKeys(primaryKey, map(keyMapper) { it.toNewModel() })
    }
}

/**
 * Transforms a single of [ch.protonmail.android.api.models.Keys] to
 * [ch.protonmail.android.domain.entity.user.UserKey]
 * Inherit from [BridgeMapper]
 */
class UserKeyBridgeMapper @Inject constructor() : BridgeMapper<OldKey, UserKey> {

    override fun OldKey.toNewModel() = UserKey(
        id = UserId(id),
        version = 4.toUInt(), // TODO not implemented on old Keys
        privateKey = PgpField.PrivateKey(NotBlankString(privateKey)),
        token = getToken(token)
    )

    private fun getToken(token: String?) = token?.takeIfNotBlank()?.let { PgpField.Message(NotBlankString(it)) }
}

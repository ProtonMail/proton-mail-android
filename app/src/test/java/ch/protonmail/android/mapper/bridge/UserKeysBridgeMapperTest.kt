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

import assert4k.assert
import assert4k.equals
import assert4k.invoke as fix
import assert4k.that
import assert4k.times
import assert4k.unaryPlus
import me.proton.core.util.kotlin.invoke
import kotlin.test.Test
import ch.protonmail.android.api.models.Keys as OldKey

/**
 * Test suite for [UserKeysBridgeMapper] and [UserKeyBridgeMapper]
 */
internal class UserKeysBridgeMapperTest {

    private val singleMapper = UserKeyBridgeMapper()
    private val multiMapper = UserKeysBridgeMapper(singleMapper)

    @Test
    fun `can map correctly single Key`() {
        val oldKey = OldKey(
            id = "id",
            privateKey = "-----BEGIN PGP PRIVATE_KEY_BLOCK-----private_key-----END PGP PRIVATE_KEY_BLOCK-----",
            token = "-----BEGIN PGP MESSAGE-----token-----END PGP MESSAGE-----"
        )

        val newKey = singleMapper { oldKey.toNewModel() }

        assert that newKey * {
            +id.s equals "id"
            +privateKey.content.s equals "private_key"
            +token?.content?.s equals "token"
        }
    }

    @Test
    fun `can map correctly multiple keys`() {
        val oldKeys = (1..10).map { OldKey("$it", primary = it == 4) }

        val newKeys = multiMapper { oldKeys.toNewModel() }

        assert that newKeys * {
            +primaryKey?.id?.s equals "4"
            +keys.size.fix() equals 10
        }
    }

    @Test
    fun `does pick first Key as primary, if none is defined`() {
        val oldKeys = (1..10).map { OldKey("$it") }

        val newKeys = multiMapper { oldKeys.toNewModel() }

        assert that newKeys * {
            +primaryKey?.id?.s equals "1"
            +keys.size.fix() equals 10
        }
    }

    private fun OldKey(
        id: String = "none",
        primary: Boolean = false,
        privateKey: String = "none",
        token: String = "none"
    ) = OldKey(id, privateKey, 0, if (primary) 1 else 0, token, "none", "none")
}

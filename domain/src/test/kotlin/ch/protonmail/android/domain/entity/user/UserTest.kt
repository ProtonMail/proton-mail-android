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
@file:OptIn(ExperimentalUnsignedTypes::class)

package ch.protonmail.android.domain.entity.user

import assert4k.assert
import assert4k.fails
import assert4k.that
import ch.protonmail.android.domain.entity.Bytes
import ch.protonmail.android.domain.entity.EmailAddress
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.domain.entity.NotBlankString
import ch.protonmail.android.domain.entity.PgpField
import ch.protonmail.android.domain.entity.ValidationException
import ch.protonmail.android.domain.entity.user.Plan.Mail
import ch.protonmail.android.domain.entity.user.Plan.Vpn
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.AddressId
import kotlin.test.Test

/**
 * Test suite for [User]
 * @author Davide Farella
 */
internal class UserTest {

    // region addresses
    @Test
    fun `User can be created if addresses are valid`() {
        User(addresses = Addresses(emptyMap()), plans = listOf(Vpn.Paid))
        User(addresses = notEmptyAddresses, plans = listOf(Mail.Paid))
    }

    @Test
    fun `User fails if there are no address by has Mail plan`() {
        assert that fails<ValidationException> {
            User(addresses = Addresses(emptyMap()), plans = listOf(Mail.Paid))
        }
    }

    // endregion

    // region keys
    @Test
    fun `User can be created if keys are valid`() {
        User(addresses = Addresses(emptyMap()), keys = UserKeys.Empty)
        User(addresses = notEmptyAddresses, keys = notEmptyKeys)
    }

    @Test
    fun `User fails if there are addresses but no keys`() {
        assert that fails<ValidationException> {
            User(addresses = notEmptyAddresses, keys = UserKeys.Empty)
        }
    }

    // endregion

    // region plans
    @Test
    fun `User can be created if plans are valid`() {
        User(plans = listOf())
        User(plans = listOf(Mail.Free))
        User(plans = listOf(Mail.Paid))
        User(plans = listOf(Vpn.Free))
        User(plans = listOf(Vpn.Paid))
        User(plans = listOf(Mail.Free, Vpn.Free))
        User(plans = listOf(Mail.Free, Vpn.Paid))
        User(plans = listOf(Mail.Paid, Vpn.Free))
        User(plans = listOf(Mail.Paid, Vpn.Paid))
    }

    @Test
    fun `User fails if there are 2 Mail plans`() {
        assert that fails<ValidationException> { User(plans = listOf(Mail.Free, Mail.Paid)) }
    }

    @Test
    fun `User fails if there are 2 Vpn plans`() {
        assert that fails<ValidationException> { User(plans = listOf(Vpn.Free, Vpn.Paid)) }
    }

    @Test
    fun `User fails if there are 2 Mail and 2 Vpn plans`() {
        assert that fails<ValidationException> { User(plans = listOf(Mail.Free, Mail.Paid, Vpn.Free, Vpn.Paid)) }
    }
    // endregion

    private fun User(
        addresses: Addresses = notEmptyAddresses,
        keys: UserKeys = notEmptyKeys,
        role: Role = Role.NO_ORGANIZATION,
        plans: Collection<Plan> = emptyList()
    ) = User(
        UserId("id"),
        Name("davide"),
        addresses,
        keys,
        plans,
        false,
        role,
        NotBlankString("Eur"),
        0,
        Delinquent.None,
        Bytes(5_000.toULong()),
        UserSpace(Bytes(25_000.toULong()), Bytes(5_000_000.toULong()))
    )

    private val notEmptyAddresses = Addresses(
        mapOf(
            1 to
                Address(
                    AddressId("address"),
                    "domainId",
                    EmailAddress("dav@protonmail.ch"),
                    null,
                    null,
                    enabled = true,
                    type = Address.Type.ORIGINAL,
                    allowedToSend = true,
                    allowedToReceive = true,
                    keys = AddressKeys(null, emptyList())
                )
        )
    )

    private val dummyKey = UserKey(
        UserId("key"),
        4.toUInt(),
        PgpField.PrivateKey(NotBlankString("key")),
        PgpField.Message(NotBlankString("token")),
        active = true
    )

    private val notEmptyKeys = UserKeys(dummyKey, listOf(dummyKey))
}

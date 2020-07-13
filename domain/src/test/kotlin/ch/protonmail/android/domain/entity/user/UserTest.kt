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
@file:OptIn(ExperimentalUnsignedTypes::class)

package ch.protonmail.android.domain.entity.user

import assert4k.*
import ch.protonmail.android.domain.entity.Bytes
import ch.protonmail.android.domain.entity.EmailAddress
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.domain.entity.NotBlankString
import ch.protonmail.android.domain.entity.PgpField
import ch.protonmail.android.domain.entity.ValidationException
import ch.protonmail.android.domain.entity.user.Plan.Mail
import ch.protonmail.android.domain.entity.user.Plan.Vpn
import kotlin.test.Test

/**
 * Test suite for [User]
 * @author Davide Farella
 */
internal class UserTest {

    // region addresses
    @Test
    fun `User can be created if addresses are valid`() {
        User(plans = listOf(Vpn.Paid), addresses = Addresses(emptyMap()))
        User(plans = listOf(Mail.Paid), addresses = notEmptyAddresses)
    }

    @Test
    fun `User fails if there are no address by has Mail plan`() {
        assert that fails<ValidationException> {
            User(plans = listOf(Mail.Paid), addresses = Addresses(emptyMap()))
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

    // region role
    @Test
    fun `User can be created if role is valid`() {
        User(role = Role.NO_ORGANIZATION, organizationPrivateKey = null)
        User(role = Role.ORGANIZATION_MEMBER, organizationPrivateKey = null)
        User(role = Role.ORGANIZATION_ADMIN, organizationPrivateKey = NotBlankString("key"))
    }

    @Test
    fun `User fails if role is NO_ORGANIZATION but has organization key`() {
        assert that fails<ValidationException> {
            User(role = Role.NO_ORGANIZATION, organizationPrivateKey = NotBlankString("key"))
        }
    }

    @Test
    fun `User fails if role is ORGANIZATION_MEMBER but has organization key`() {
        assert that fails<ValidationException> {
            User(role = Role.ORGANIZATION_MEMBER, organizationPrivateKey = NotBlankString("key"))
        }
    }

    @Test
    fun `User fails if role is ORGANIZATION_ADMIN but has NOT organization key`() {
        assert that fails<ValidationException> {
            User(role = Role.ORGANIZATION_ADMIN, organizationPrivateKey = null)
        }
    }
    // endregion

    private fun User(
        addresses: Addresses = notEmptyAddresses,
        keys: UserKeys = notEmptyKeys,
        role: Role = Role.NO_ORGANIZATION,
        organizationPrivateKey: NotBlankString? = null,
        plans: Collection<Plan> = emptyList()
    ) = User(
        Id("id"),
        Name("davide"),
        addresses,
        keys,
        plans,
        false,
        role,
        organizationPrivateKey,
        NotBlankString("Eur"),
        0,
        Delinquent.None,
        Bytes(5_000u),
        UserSpace(Bytes(25_000u), Bytes(5_000_000u))
    )

    private val notEmptyAddresses = Addresses(
        mapOf(
            1 to
                Address(
                    Id("address"),
                    Id("domainId"),
                    EmailAddress("dav@protonmail.ch"),
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
        Id("key"),
        4u,
        PgpField.PrivateKey(NotBlankString("key")),
        PgpField.Message(NotBlankString("token"))
    )

    private val notEmptyKeys = UserKeys(dummyKey, listOf(dummyKey))
}

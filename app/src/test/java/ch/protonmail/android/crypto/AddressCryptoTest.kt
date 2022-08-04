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

package ch.protonmail.android.crypto

import ch.protonmail.android.api.models.Keys
import ch.protonmail.android.api.models.User
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.domain.entity.NotBlankString
import ch.protonmail.android.domain.entity.PgpField
import ch.protonmail.android.domain.entity.user.AddressKey
import ch.protonmail.android.utils.crypto.OpenPGP
import io.mockk.every
import io.mockk.mockk
import me.proton.core.crypto.common.keystore.EncryptedByteArray
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.AddressId
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertContentEquals

private const val USER_ID_VALUE = "userId"
private const val ADDRESS_ID_VALUE = "addressId"
private val USER_PASSPHRASE = "mailboxPassword".toByteArray()
private const val TOKEN = "token"
private const val SIGNATURE = "signature"
private const val PRIVATE_KEY_SUCCESSFUL_DECRYPTING = "Valid Private Key able to decrypt token"
private const val PRIVATE_KEY_FAILING_DECRYPTING = "Invalid private Key Id, will fail decrypting token"

class AddressCryptoTest {


    private val validKeyThatSucceedsDecrypting = buildKeys(PRIVATE_KEY_SUCCESSFUL_DECRYPTING)

    private val invalidKeyThatFailsDecrypting = buildKeys(PRIVATE_KEY_FAILING_DECRYPTING)

    private val mockUser = mockk<User> {
        every { keys } returns listOf(
            invalidKeyThatFailsDecrypting,
            validKeyThatSucceedsDecrypting
        )
    }

    private val userManager = mockk<UserManager> {
        every { getUserPassphraseBlocking(UserId(USER_ID_VALUE)) } returns EncryptedByteArray(USER_PASSPHRASE)
        every { currentLegacyUser } returns mockUser
    }

    private val openPgp = mockk<OpenPGP>()

    private val addressCrypto = AddressCrypto(
        userManager = userManager,
        openPgp = openPgp,
        userId = UserId(USER_ID_VALUE),
        addressId = AddressId(ADDRESS_ID_VALUE)
    )

    @Test
    fun whenPassphraseForKeyIsRequestedForNonMigratedUserThenTheMailboxPasswordIsReturned() {
        val key = buildAddressKey(null, null)

        val actual = addressCrypto.passphraseFor(key)

        assertContentEquals(USER_PASSPHRASE, actual?.array)
    }

    private fun buildKeys(privateKey: String) = Keys(
        UUID.randomUUID().toString(),
        privateKey,
        0,
        0,
        TOKEN,
        SIGNATURE,
        "",
        0
    )

    private fun buildAddressKey(
        token: PgpField.Message?,
        signature: PgpField.Signature?
    ): AddressKey = AddressKey(
        UserId(""),
        UInt.MIN_VALUE,
        false,
        false,
        PgpField.PublicKey(NotBlankString("pubKey")),
        PgpField.PrivateKey(NotBlankString("privateKey")),
        token,
        signature,
        PgpField.Message(NotBlankString("activation")),
        true
    )
}

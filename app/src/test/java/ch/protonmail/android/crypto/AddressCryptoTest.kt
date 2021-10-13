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

package ch.protonmail.android.crypto

import ch.protonmail.android.api.models.Keys
import ch.protonmail.android.api.models.User
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.domain.entity.NotBlankString
import ch.protonmail.android.domain.entity.PgpField
import ch.protonmail.android.domain.entity.user.AddressKey
import ch.protonmail.android.utils.crypto.OpenPGP
import com.proton.gopenpgp.crypto.PlainMessage
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.AddressId
import org.junit.Ignore
import java.util.UUID
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import com.proton.gopenpgp.crypto.Crypto as GoOpenPgpCrypto

private const val USER_ID_VALUE = "userId"
private const val ADDRESS_ID_VALUE = "addressId"
private const val MAILBOX_PASSWORD = "mailboxPassword"
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
        every { getUserPassphraseBlocking(UserId(USER_ID_VALUE)) } returns MAILBOX_PASSWORD.toByteArray()
        every { currentLegacyUser } returns mockUser
    }

    private val openPgp = mockk<OpenPGP>()

    private val addressCrypto = AddressCrypto(
        userManager = userManager,
        openPgp = openPgp,
        userId = UserId(USER_ID_VALUE),
        addressId = AddressId(ADDRESS_ID_VALUE)
    )

    @AfterTest
    fun tearDown() {
        unmockkStatic(GoOpenPgpCrypto::class)
    }

    @Test
    fun whenPassphraseForKeyIsRequestedForNonMigratedUserThenTheMailboxPasswordIsReturned() {
        val key = buildAddressKey(null, null)

        val actual = addressCrypto.passphraseFor(key)

        assertEquals(MAILBOX_PASSWORD, actual?.toString(Charsets.UTF_8))
    }

    @Test
    @Ignore("Kept for discussion purposes on the PR, will drop afterwards")
    fun whenPassphraseForKeyIsRequestedForMigratedUserThenAllKeysAreCheckedForAValidSignedToken() {
        val token = PgpField.Message(NotBlankString(TOKEN))
        val signature = PgpField.Signature(NotBlankString("Signature"))
        val addressKey = buildAddressKey(token, signature)

        mockkStatic(GoOpenPgpCrypto::class) {
            every { GoOpenPgpCrypto.newPGPMessageFromArmored(TOKEN) } answers { mockk() }
            every { GoOpenPgpCrypto.newKeyFromArmored(PRIVATE_KEY_FAILING_DECRYPTING) } returns mockk {
                every { GoOpenPgpCrypto.newKeyRing(any()) } returns mockk {
                    every { decrypt(any(), null, 0) } returns null
                }
            }

            every { GoOpenPgpCrypto.newKeyFromArmored(PRIVATE_KEY_SUCCESSFUL_DECRYPTING) } returns mockk {
                every { GoOpenPgpCrypto.newKeyRing(any()) } returns mockk {
                    every { decrypt(any(), null, 0) } returns PlainMessage("decrypted token")
                }
            }
        }

        val actual = addressCrypto.passphraseFor(addressKey)

        val decryptedToken = "decrypted token"
        assertEquals(decryptedToken, actual?.toString(Charsets.UTF_8))
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

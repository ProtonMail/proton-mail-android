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
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.domain.entity.Name
import ch.protonmail.android.domain.entity.NotBlankString
import ch.protonmail.android.domain.entity.PgpField
import ch.protonmail.android.domain.entity.user.AddressKey
import ch.protonmail.android.utils.crypto.OpenPGP
import io.mockk.every
import io.mockk.mockk
import org.junit.Test
import kotlin.test.assertNull

private const val USER_ID_VALUE = "userId"
private const val ADDRESS_ID_VALUE = "addressId"
private const val MAILBOX_PASSWORD = "mailboxPassword"
private const val TOKEN =
    """
        -----BEGIN PGP MESSAGE-----
        Version: GopenPGP 2.3.1
        Comment: https://gopenpgp.org
    
        wV4DNBPNx6Mfm4QSAQdA+gjeD8AGMVyluYObzKlo9B9kU4SNeiqAWjqwFGXTHFMw
        DZzOa6EIuNeyTQoGrFSKma3Z0oyKNCRvsF70Vt1K8wSEx/dNsLn0AGjMm0D1Cm9z
        0lAB3vhbnxfSE2/cVCBRjs87k9TvFcoZHp5eztyL1/BkXfLyugh6nFp5LV8H0ZO/
        HWS28TNwxGtna0QZtLe2Xe+tEm/wnW4NFZ3SOyMNBvWWmw==
        =LjHO
        -----END PGP MESSAGE-----
    """
private const val SIGNATURE =
    """
        -----BEGIN PGP SIGNATURE-----
        Version: GopenPGP 2.3.1
        Comment: https://gopenpgp.org

        wnUEABYKACcFAmHAhc4JkD8UQEqW+HQOFqEEWT32tVwJth9elkubPxRASpb4dA4A
        AElYAP0Uya2xCtEKuv+7qsIqlb/LvTLNY+/1csBo1sse7WKN2AD+PkU1i6RJHs+u
        BKwLEalXrDxzHVBKw2QK29wOn8tZXgY=
        =fdg7
        -----END PGP SIGNATURE-----
    """
private const val PRIVATE_KEY =
    """
        -----BEGIN PGP PRIVATE KEY BLOCK-----
        Version: GopenPGP 2.3.1
        Comment: https://gopenpgp.org
    
        xYYEYcCFzhYJKwYBBAHaRw8BAQdAU/1bnScil300DPZgNarSabg+D7DgWmnTA+wR
        6Mp/97r+CQMIzPA6oob4f6xgD0rhEUmxO/MHZTIywg1fbXa5pzYdpMaEKaUUp015
        eziVhKXcFPA/6upQT1hlFC8lt49YTMTbxs24k9NN+wOaJbAJOLrPL80LdGVzdCA8
        dGVzdD7CjAQTFggAPgUCYcCFzgmQPxRASpb4dA4WoQRZPfa1XAm2H16WS5s/FEBK
        lvh0DgIbAwIeAQIZAQMLCQcCFQgDFgACAiIBAACvDgEAzMpdqT/wPWFo1S7+daUg
        o2nQpGZ3M5wIjzQ2C9V/CHEA/i7AqNNCJYMbWysMNdohYWUeGgTUxKC0WdKEvuql
        5HICx4sEYcCFzhIKKwYBBAGXVQEFAQEHQFEbuqzVb2s3I/kQu6VPXy4abibrDnIP
        0RHGmi9fZRxIAwEKCf4JAwjzE/GkMIwewmBEmURSZ40FYGLsIGDY8P1N5Jty0bAK
        cb9w3+nHND3IQldzGoiEk/y5kFI1UFR2A9TCpxSUKW1qMSFKzmi/VPxJaksQZIfq
        wngEGBYIACoFAmHAhc4JkD8UQEqW+HQOFqEEWT32tVwJth9elkubPxRASpb4dA4C
        GwwAAGsVAP9wOFHhuRzbp8kmmZJKs0sty8Kqo8w8A+HvGpHh/pYHlgEAlxjYenkr
        u9wJxv2+Wc4w4aIT8gDCPxanKA6L9y0yiws=
        =Jt+6
        -----END PGP PRIVATE KEY BLOCK-----
    """

class AddressCryptoTest {

    private val userManager = mockk<UserManager> {
        every { getMailboxPassword(any()) } returns MAILBOX_PASSWORD.toByteArray()
        every { user.keys } returns listOf(
            Keys(
                "keyId",
                PRIVATE_KEY.trimIndent(),
                3,
                1,
                null,
                null,
                null,
            1
            )
        )
    }

    private val openPgp = mockk<OpenPGP>()

    private val addressCrypto = AddressCrypto(
        userManager = userManager,
        openPgp = openPgp,
        username = Name(USER_ID_VALUE),
        addressId = Id(ADDRESS_ID_VALUE)
    )

    @Test
    fun whenTokenHasWrongFormatPassphraseForKeyReturnsNull() {
        // Given
        val key = AddressKey(
            Id("key-id"),
            UInt.MIN_VALUE,
            canEncrypt = true,
            canVerifySignature = true,
            PgpField.PublicKey(NotBlankString("pubKey")),
            PgpField.PrivateKey(NotBlankString("privateKey")),
            PgpField.Message(NotBlankString(TOKEN.trimIndent())),
            PgpField.Signature(NotBlankString(SIGNATURE.trimIndent())),
            null,
            active = true
        )
        val actual = addressCrypto.passphraseFor(key)

        assertNull(actual)
    }
}

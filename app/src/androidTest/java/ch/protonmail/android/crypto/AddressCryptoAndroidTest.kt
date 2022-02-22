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

import ch.protonmail.android.core.UserManager
import ch.protonmail.android.domain.entity.NotBlankString
import ch.protonmail.android.domain.entity.PgpField
import ch.protonmail.android.domain.entity.user.AddressKey
import ch.protonmail.android.domain.entity.user.UserKey
import ch.protonmail.android.utils.crypto.OpenPGP
import io.mockk.every
import io.mockk.mockk
import me.proton.core.domain.entity.UserId
import me.proton.core.user.domain.entity.AddressId
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val USER_ID_VALUE = "userId"
private const val ADDRESS_ID_VALUE = "addressId"
private const val MAILBOX_PASSWORD = "mailboxPassword"

private const val TOKEN = """
    -----BEGIN PGP MESSAGE-----
    Version: GopenPGP 2.4.2
    Comment: https://gopenpgp.org
    
    wV4DNBPNx6Mfm4QSAQdAkWuehTwowG3j+2wWt0hWaJ/fGZBiTSquYkmQt+4LAyMw
    MrKPid5w5tPq5+Hcx0jR7fGM1k073l0E1VnqpHszupWIX1A4LnzEMlKWb71h5NEi
    0nEBf5PU6nnvxav1jU+rQ6WSu1Pxo/g+Uyuty0vxzOllkKOlsQCiXndG17iNrKC3
    oYejqVSL+6neXodbQBSlRoB/c6rFeHnVvYcBcaDxV+V9U3vnnB/1/bcxDEHRQjwN
    kje5FMnbO5xl34t+IEEsGCSPIg==
    =1gZM
    -----END PGP MESSAGE-----
"""

private const val TOKEN_SIGNATURE = """
    -----BEGIN PGP SIGNATURE-----
    Version: GopenPGP 2.4.2
    Comment: https://gopenpgp.org
    
    wnUEABYKACcFAmHm9ZsJkD8UQEqW+HQOFiEEWT32tVwJth9elkubPxRASpb4dA4A
    AIAoAQDUokbo4tfe7W9LN6gXRcK0QWcIVGRMF+YREj771c/BvgEA/fdNmNaYG+1Y
    E9xYF57tYJVc+oK4mJGPh2g778/EygU=
    =UyYQ
    -----END PGP SIGNATURE-----
"""
private const val MALFORMED_TOKEN =
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
private const val MALFORMED_TOKEN_SIGNATURE =
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
private const val ACTIVER_USER_KEY =
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

private const val INACTIVE_USER_KEY = """
    -----BEGIN PGP PRIVATE KEY BLOCK-----
    Version: GopenPGP 2.4.2
    Comment: https://gopenpgp.org
    
    xYYEYeb2nRYJKwYBBAHaRw8BAQdAHJZyhQDkJ2p26tfaTgcOHslvgYIEQDplgFg7
    z2W9r+X+CQMIwpOOkDi1VcBgi6THLuyTj6Z5cb00cx0GS/Q5UveuG8Y7/p8l4GPH
    w9lzBt/mY3bKCvm1Wg1/UtVfTb5zmk0F5pCP/0zBeXy3Y4OV5sZt1M0pdGVzdEBw
    cm90b25tYWlsLmNvbSA8dGVzdEBwcm90b25tYWlsLmNvbT7CjAQTFggAPgUCYeb2
    nQmQ9BukUtAw7lUWIQS99VizpbwB9rYZGUD0G6RS0DDuVQIbAwIeAQIZAQMLCQcC
    FQgDFgACAiIBAADc5QD+Ij70z8c9REffgEOwGcbgqLV7VKWcVGpEIvzdTBdWUKYB
    AOVRKmUc9t0lj1/YxsvVCS3Zpi4UhCw9IrDhmBh2z2wFx4sEYeb2nRIKKwYBBAGX
    VQEFAQEHQOvSVeTWJgZO0A0huM1VyioH7byX+KZw7HurpATdO64dAwEKCf4JAwhH
    nVgrABxe0WCZBrBpAn+C4VI6xWhGglJT87mhkjpLAIxpnVjUNof7Rh6yF0FwE7WK
    5hKupSji5XjX5eRPtuTC96SKrbvxcdRoOGzVEu8ywngEGBYIACoFAmHm9p0JkPQb
    pFLQMO5VFiEEvfVYs6W8Afa2GRlA9BukUtAw7lUCGwwAAMGgAP9J1v9FF/2HKssB
    LUdnOC3w1aVQ3Ym3RqMlVCWClwqAfAEAzbSxOzLyBLcBywr8BgAyZqJFSmB7mM6Y
    yTn2ZnTTsA4=
    =JH3w
    -----END PGP PRIVATE KEY BLOCK-----
"""

class AddressCryptoAndroidTest {

    private val userManager = mockk<UserManager> {
        every { getUserPassphraseBlocking(UserId(USER_ID_VALUE)) } returns MAILBOX_PASSWORD.toByteArray()
        every { currentUser?.keys?.keys } returns listOf(
            UserKey(
                UserId(USER_ID_VALUE),
                0u,
                PgpField.PrivateKey(NotBlankString(ACTIVER_USER_KEY.trimIndent())),
                null,
                active = true,
            )
        )
    }

    private val openPgp = mockk<OpenPGP>()

    private val addressCrypto = AddressCrypto(
        userManager = userManager,
        openPgp = openPgp,
        userId = UserId(USER_ID_VALUE),
        addressId = AddressId(ADDRESS_ID_VALUE)
    )

    @Test
    fun whenTokenHasWrongFormatPassphraseForKeyReturnsNull() {
        // Given
        val key = AddressKey(
            UserId(""),
            UInt.MIN_VALUE,
            canEncrypt = true,
            canVerifySignature = true,
            PgpField.PublicKey(NotBlankString("pubKey")),
            PgpField.PrivateKey(NotBlankString("privateKey")),
            PgpField.Message(NotBlankString(MALFORMED_TOKEN.trimIndent())),
            PgpField.Signature(NotBlankString(MALFORMED_TOKEN_SIGNATURE.trimIndent())),
            null,
            active = true
        )
        // when
        val actual = addressCrypto.passphraseFor(key)
        // then
        assertNull(actual)
    }

    @Test
    fun whenUserHasInactiveKeyTokenDecryptionSucceeds() {
        // given
        every { userManager.currentUser?.keys?.keys } returns listOf(
            UserKey(
                UserId(USER_ID_VALUE),
                0u,
                PgpField.PrivateKey(NotBlankString(INACTIVE_USER_KEY.trimIndent())),
                null,
                active = true,
            ),
            UserKey(
                UserId(USER_ID_VALUE),
                0u,
                PgpField.PrivateKey(NotBlankString(ACTIVER_USER_KEY.trimIndent())),
                null,
                active = true,
            ),
        )
        val key = AddressKey(
            UserId(""),
            UInt.MIN_VALUE,
            canEncrypt = true,
            canVerifySignature = true,
            PgpField.PublicKey(NotBlankString("pubKey")),
            PgpField.PrivateKey(NotBlankString("privateKey")),
            PgpField.Message(NotBlankString(TOKEN.trimIndent())),
            PgpField.Signature(NotBlankString(TOKEN_SIGNATURE.trimIndent())),
            null,
            active = true
        )
        // when
        val actual = addressCrypto.passphraseFor(key)
        // then
        assertEquals("abababababababababababababababababababababababababababababababab", actual?.let { String(it) })
    }
}

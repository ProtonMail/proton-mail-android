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

package ch.protonmail.android.api.models.factories;


import android.content.Context
import ch.protonmail.android.api.ProtonMailApiManager
import ch.protonmail.android.api.models.ContactEncryptedData
import ch.protonmail.android.api.models.MailSettings
import ch.protonmail.android.api.models.PublicKeyBody
import ch.protonmail.android.api.models.PublicKeyResponse
import ch.protonmail.android.api.models.SendPreference
import ch.protonmail.android.api.models.enumerations.MIMEType
import ch.protonmail.android.api.models.enumerations.PackageType
import ch.protonmail.android.core.Constants
import ch.protonmail.android.core.UserManager
import ch.protonmail.android.data.local.ContactDatabase
import ch.protonmail.android.data.local.model.FullContactDetails
import ch.protonmail.android.domain.entity.NotBlankString
import ch.protonmail.android.domain.entity.PgpField
import ch.protonmail.android.domain.entity.user.Addresses
import ch.protonmail.android.domain.entity.user.UserKey
import ch.protonmail.android.domain.entity.user.UserKeys
import ch.protonmail.android.utils.crypto.OpenPGP
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import me.proton.core.crypto.common.keystore.EncryptedByteArray
import me.proton.core.crypto.common.keystore.KeyStoreCrypto
import me.proton.core.crypto.common.keystore.PlainByteArray
import me.proton.core.domain.entity.UserId
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

private const val USER_ID_VALUE = "userId"
private const val MAILBOX_PASSWORD = "12345678"
private const val MAILBOX_PRIVATE_KEY =
    """
        -----BEGIN PGP PRIVATE KEY BLOCK-----
        Comment: contact signing key
        
        xYYEYeaS+hYJKwYBBAHaRw8BAQdAHOi00L8NL+FWB7w0c1UhR+QKXJJF+Eb9
        aKTbauiKjob+CQMIHpzB+/3INlZgEM6nu1zL+/SKnOG70Ev04tkLCF3kMr9p
        XEgYpPmP227AYopiUAXJ4+7BOuqTC6c/uS0NX7kdAd+IT3aOlzs387UROtQp
        aM07bm90X2Zvcl9lbWFpbF91c2VAZG9tYWluLnRsZCA8bm90X2Zvcl9lbWFp
        bF91c2VAZG9tYWluLnRsZD7CjwQQFgoAIAUCYeaS+gYLCQcIAwIEFQgKAgQW
        AgEAAhkBAhsDAh4BACEJEMWIjIjnBBwHFiEENxbs5dz3SjF9Nc15xYiMiOcE
        HAegIAEAkBAx2kzOAQonePZrQMeekQIu8bRMaSxRfsswK6opXxQA/A1tnpRJ
        smkSouinDwA5eluebvLqZ0X/FgyUwbrF0zIJx4sEYeaS+hIKKwYBBAGXVQEF
        AQEHQHnABuOUa8OryKkSurWsW32KwjYq++deShnVhtBpTukLAwEIB/4JAwh+
        wgGrUBh59WDIgBTJjanY3HTfCSu1jvtBggGS5lJ4EttsO6AMBwahBf128xe5
        5ku05wPPYhas/bebEswPUR1jXMT4pTG8oVJEu976s1ZRwngEGBYIAAkFAmHm
        kvoCGwwAIQkQxYiMiOcEHAcWIQQ3Fuzl3PdKMX01zXnFiIyI5wQcB56iAP95
        5EhHmzs2xDdpLokyJp4JiWWIezDi3ROLdsyQ8CGlZwEApM9K4K06rnb1woIL
        aypRDOHYuyaQkaq+d+7RmCVAXQo=
        =pW6d
        -----END PGP PRIVATE KEY BLOCK-----
    """

private const val INTERNAL_CONTACT_DATA_WITH_PINNED_KEY =
    """
        BEGIN:VCARD
        VERSION:4.0
        FN;PREF=1:free
        ITEM1.EMAIL;PREF=1:free@proton.black
        ITEM1.KEY;PREF=1:data:application/pgp-keys;base64,xjMEYYvJXhYJKwYBBAHaRw8BA
         QdAsAcJAP6RrHUbbQDflTbt0KdheIiWwcbNf6GaKToFyQzNJWZyZWVAcHJvdG9uLmJsYWNrIDxm
         cmVlQHByb3Rvbi5ibGFjaz7CjwQQFgoAIAUCYYvJXgYLCQcIAwIEFQgKAgQWAgEAAhkBAhsDAh4
         BACEJEElvc9JAKP/JFiEERn9qy9Z5b1tdfy/JSW9z0kAo/8maiAEA/1cnxvyJIBu3/Cpd77tsau
         kZ7KeJz+J7TxlO2662OwcBAMpbtvl7Jv+HdpFFXh3I2yUzfb/bLL4LIXCoAVi4l5wMzjgEYYvJX
         hIKKwYBBAGXVQEFAQEHQAdE15eqlEc9aVzg4CpxOMmmzg1MtprlPIIeRFujcPklAwEIB8J4BBgW
         CAAJBQJhi8leAhsMACEJEElvc9JAKP/JFiEERn9qy9Z5b1tdfy/JSW9z0kAo/8mgPAEA5CG75ku
         Tc+FNq3kzpeHZvrP3i821cDEw1OqbP9tPsvUBAPnsEBo4WUYh8RT6iVjOR6p1YYJSOiXvBmCTDO
         tfko4J
        PRODID;VALUE=TEXT:-//ProtonMail//ProtonMail vCard 1.0.0//EN
        UID:proton-autosave-cbbbe53e-eee5-4a95-811d-c4b526b85159
        END:VCARD
    """

private const val INTERNAL_CONTACT_SIGNATURE =
    """
        -----BEGIN PGP SIGNATURE-----
        Version: OpenPGP.js v4.10.10
        Comment: https://openpgpjs.org
        
        wnUEARYKAAYFAmHmlNYAIQkQxYiMiOcEHAcWIQQ3Fuzl3PdKMX01zXnFiIyI
        5wQcB467AQD8Z7/rnLt+rVN2aYiMkHKQHkhC3dsAP50GALu165lffAEAs8aj
        jwChji+eaesbKW9IWc/Ko3kPCFYxa8tNdWjZBAM=
        =aA/q
        -----END PGP SIGNATURE-----
    """

private const val INTERNAL_CONTACT_PINNED_KEY =
    """
        -----BEGIN PGP PUBLIC KEY BLOCK-----
        Version: ProtonMail
        
        xjMEYYvJXhYJKwYBBAHaRw8BAQdAsAcJAP6RrHUbbQDflTbt0KdheIiWwcbN
        f6GaKToFyQzNJWZyZWVAcHJvdG9uLmJsYWNrIDxmcmVlQHByb3Rvbi5ibGFj
        az7CjwQQFgoAIAUCYYvJXgYLCQcIAwIEFQgKAgQWAgEAAhkBAhsDAh4BACEJ
        EElvc9JAKP/JFiEERn9qy9Z5b1tdfy/JSW9z0kAo/8maiAEA/1cnxvyJIBu3
        /Cpd77tsaukZ7KeJz+J7TxlO2662OwcBAMpbtvl7Jv+HdpFFXh3I2yUzfb/b
        LL4LIXCoAVi4l5wMzjgEYYvJXhIKKwYBBAGXVQEFAQEHQAdE15eqlEc9aVzg
        4CpxOMmmzg1MtprlPIIeRFujcPklAwEIB8J4BBgWCAAJBQJhi8leAhsMACEJ
        EElvc9JAKP/JFiEERn9qy9Z5b1tdfy/JSW9z0kAo/8mgPAEA5CG75kuTc+FN
        q3kzpeHZvrP3i821cDEw1OqbP9tPsvUBAPnsEBo4WUYh8RT6iVjOR6p1YYJS
        OiXvBmCTDOtfko4J
        =NGFq
        -----END PGP PUBLIC KEY BLOCK-----
    """

private const val EXTERNAL_CONTACT_DATA_WITH_PINNED_KEY_EXPIRED_SUBKEY =
    """
        BEGIN:VCARD
        VERSION:4.0
        FN;PREF=1:expired-encryption-key@domain.tld
        ITEM1.EMAIL;PREF=1:expired-encryption-key@domain.tld
        UID:proton-web-efc9290f-568b-ff71-adfe-434c07b51b72
        ITEM1.KEY;PREF=1:data:application/pgp-keys;base64,xjMEYsWyPxYJKwYBBAHaRw8BA
         QdApy7e/4nQpvqU7Nb+VKcxBOn6k5Ns9A96ZgllUvoKXFrNPURhaWx5IHJvdGF0aW5nIGVuY3J5
         cHRpb24gc3Via2V5IDxkYWlseS1zdWJrZXlAcHJvdG9ua2V5LmNvbT7CqwQTFgoAPBYhBOsxSNg
         7naEMH6zUw0s0O0Tu5J/1BQJixbI/AhsDBQsJCAcCAyICAQYVCgkICwIEFgIDAQIeBwIXgAAhCR
         BLNDtE7uSf9RYhBOsxSNg7naEMH6zUw0s0O0Tu5J/1MYkA/0MHro5TnjHjKJfZDgjqb8Gx1TSi4
         vn9It6Hvb68va9iAQCkdKi+dnPZR9BWVM7cbGFxRRADPojCjFMwUwi8NPq+A87AzQRixoZQAQwA
         uKcvcJVpxqWGeFYzCY3wCwunuYoWReZtpIQOSOIFX0Lj0Wxd6oFLYR9FZN/hOGI9JG1ijpjgWVM
         F9FN/rLUtNtUt6nyQ0AraFPQMtJKXIe1QN9g2p1M2O1Tq+d0z0/Hyrf5wVNE0NpwH9mloLPWtvg
         VGZ1zXs9D6A8XOhXhN7+qYvT2IGuQCqOL5dx3CaFepDESyUN6VYTRxZS0vWJDelAfD/D/LoAj15
         +a5lU+9gfKwI1a5CTGbiaOlLvRGWphsIGVW5unxF3kRHj3RKaU03mytRd0qnQNkfHTNN8VRDQJE
         /sVLcBcgYbsRLmPovHyAsAl1ZgOqi+GnWX+9owDS315BbKAtKMcbeB8JekivpuYu/Zqt8ZlqPsv
         XzkJ6EXAEiMA6sAylsotoaVq5HcyOtydQAhIR2vD8m7fzOP3f3MDUIJ4qlDMiGwydaN/JO5vCO/
         B8EftP2giuY0804TvxFms2a84nINKGMxsKbXcFKPVRGyAZSCeYyVT+pWNbim9zABEBAAHClQQYF
         ggAJhYhBOsxSNg7naEMH6zUw0s0O0Tu5J/1BQJixoZQAhsMBQkAAVGAACEJEEs0O0Tu5J/1FiEE
         6zFI2DudoQwfrNTDSzQ7RO7kn/XvcwEAnK8HSZOTYh9MUPpchBmAoD+Ezv2zcs79TUazFn6im3s
         BAIWZEWLVN//jr6I+88z2qkF/B8xYBjKvml0nKr6mMpcJ
        END:VCARD
    """

private const val EXTERNAL_CONTACT_DATA_WITH_PINNED_KEY_EXPIRED_SUBKEY_SIGNATURE =
    """
        -----BEGIN PGP SIGNATURE-----

        wr0EARYKAG8FgmO/0T8JEMWIjIjnBBwHRxQAAAAAAB4AIHNhbHRAbm90YXRpb25z
        LnNlcXVvaWEtcGdwLm9yZ6La49zW9I1JjszhnCGSEbn3au0h6TqkyTy4RP9W6JEe
        FiEENxbs5dz3SjF9Nc15xYiMiOcEHAcAACKmAQDpxS9FtQRyHB8LbxmVt25Q+Oy2
        3f+QTIu9qovB7fpOiwEA48mQghjemyspeelz7uofvk3Vkt8tqIT/zTq1Y/ScbQ4=
        =zL80
        -----END PGP SIGNATURE-----
    """

public class SendPreferencesFactoryTest {
    private val context = mockk<Context>(relaxed = true)
    private val apiManager = mockk<ProtonMailApiManager>()
    private val mailSettings = MailSettings()
    private val userId = mockk<UserId>(USER_ID_VALUE)
    private val primaryUserKey = UserKey(
        userId,
        0u,
        PgpField.PrivateKey(NotBlankString(MAILBOX_PRIVATE_KEY.trimIndent())),
        null,
        active = true
    )

    private val keyStoreCrypto = mockk<KeyStoreCrypto>{
        every {
            decrypt(
                match<EncryptedByteArray> {
                    it.array.contentEquals(MAILBOX_PASSWORD.toByteArray())
                }
            )
        } answers { PlainByteArray(MAILBOX_PASSWORD.toByteArray()) }
    }

    private val userManager = mockk<UserManager> {
        every { this@mockk.keyStoreCrypto } returns this@SendPreferencesFactoryTest.keyStoreCrypto
        every { getUserPassphraseBlocking(userId) } returns EncryptedByteArray(MAILBOX_PASSWORD.toByteArray())
        every { getMailSettingsBlocking(userId) } returns mailSettings
        every { openPgp } returns OpenPGP()
        every { getUserBlocking(any()) } returns mockk {
            every { addresses } returns Addresses(emptyMap())
            every { keys }  returns UserKeys(
                primaryUserKey,
                listOf(primaryUserKey)
            );
        }
    }

    lateinit var sendPreferencesFactory: SendPreferencesFactory

    @BeforeTest
    fun setup() {
        mockkObject(ContactDatabase.Companion)
        every {
            ContactDatabase.getInstance(context, userId)
        } returns mockk<ContactDatabase>(relaxed = true)
        sendPreferencesFactory = SendPreferencesFactory(context, apiManager, userManager, userId)    }

    @Test
    fun testBuildFromContactWithPinnedKeyInternal() {
        val contactDataWithPinnedKey = INTERNAL_CONTACT_DATA_WITH_PINNED_KEY.trimIndent()
        val pinnedKey = INTERNAL_CONTACT_PINNED_KEY.trimIndent()
        val contactSignature = INTERNAL_CONTACT_SIGNATURE.trimIndent()
        val signedContactDetails = mockk<FullContactDetails> {
            every { encryptedData } returns listOf(ContactEncryptedData(
                contactDataWithPinnedKey,
                contactSignature,
                Constants.VCardType.SIGNED
            ))
        }
        val publicKeyResponse = PublicKeyResponse(PublicKeyResponse.RecipientType.INTERNAL.value, "", arrayOf( mockk<PublicKeyBody> {
            every { isAllowedForSending } returns true
            every { isAllowedForVerification } returns true
            every { publicKey } returns pinnedKey
        } ))
        val expected = SendPreference(
            "free@proton.black", true, true, MIMEType.HTML, pinnedKey, PackageType.PM,
            true, true, true, false
        )

        val actual = sendPreferencesFactory.buildFromContact("free@proton.black", publicKeyResponse, signedContactDetails) as SendPreference
        assertEqualSecurityPreferences(expected, actual)
    }

    @Test
    fun testBuildFromContactWithPinnedKeyExternal() {
        val externalPinnedKey = """
            -----BEGIN PGP PUBLIC KEY BLOCK-----

            xjMEYeaS+hYJKwYBBAHaRw8BAQdAHOi00L8NL+FWB7w0c1UhR+QKXJJF+Eb9
            aKTbauiKjobNO25vdF9mb3JfZW1haWxfdXNlQGRvbWFpbi50bGQgPG5vdF9m
            b3JfZW1haWxfdXNlQGRvbWFpbi50bGQ+wo8EEBYKACAFAmHmkvoGCwkHCAMC
            BBUICgIEFgIBAAIZAQIbAwIeAQAhCRDFiIyI5wQcBxYhBDcW7OXc90oxfTXN
            ecWIjIjnBBwHoCABAJAQMdpMzgEKJ3j2a0DHnpECLvG0TGksUX7LMCuqKV8U
            APwNbZ6USbJpEqLopw8AOXpbnm7y6mdF/xYMlMG6xdMyCc44BGHmkvoSCisG
            AQQBl1UBBQEBB0B5wAbjlGvDq8ipErq1rFt9isI2KvvnXkoZ1YbQaU7pCwMB
            CAfCeAQYFggACQUCYeaS+gIbDAAhCRDFiIyI5wQcBxYhBDcW7OXc90oxfTXN
            ecWIjIjnBBwHnqIA/3nkSEebOzbEN2kuiTImngmJZYh7MOLdE4t2zJDwIaVn
            AQCkz0rgrTqudvXCggtrKlEM4di7JpCRqr537tGYJUBdCg==
            =v4yu
            -----END PGP PUBLIC KEY BLOCK-----
        """.trimIndent()
        val externalContactDataWithKey = """
            BEGIN:VCARD
            VERSION:4.0
            FN;PREF=1:external@proton.test
            ITEM1.EMAIL;PREF=1:external@proton.test
            ITEM1.KEY;PREF=1:data:application/pgp-keys;base64,xjMEYeaS+hYJKwYBBAHaRw8BA
             QdAHOi00L8NL+FWB7w0c1UhR+QKXJJF+Eb9aKTbauiKjobNO25vdF9mb3JfZW1haWxfdXNlQGRv
             bWFpbi50bGQgPG5vdF9mb3JfZW1haWxfdXNlQGRvbWFpbi50bGQ+wo8EEBYKACAFAmHmkvoGCwk
             HCAMCBBUICgIEFgIBAAIZAQIbAwIeAQAhCRDFiIyI5wQcBxYhBDcW7OXc90oxfTXNecWIjIjnBB
             wHoCABAJAQMdpMzgEKJ3j2a0DHnpECLvG0TGksUX7LMCuqKV8UAPwNbZ6USbJpEqLopw8AOXpbn
             m7y6mdF/xYMlMG6xdMyCc44BGHmkvoSCisGAQQBl1UBBQEBB0B5wAbjlGvDq8ipErq1rFt9isI2
             KvvnXkoZ1YbQaU7pCwMBCAfCeAQYFggACQUCYeaS+gIbDAAhCRDFiIyI5wQcBxYhBDcW7OXc90o
             xfTXNecWIjIjnBBwHnqIA/3nkSEebOzbEN2kuiTImngmJZYh7MOLdE4t2zJDwIaVnAQCkz0rgrT
             qudvXCggtrKlEM4di7JpCRqr537tGYJUBdCg==
            UID:proton-web-1afd2106-0c8e-0679-d8c0-beb3afe56dd9
            ITEM1.X-PM-ENCRYPT:true
            ITEM1.X-PM-SIGN:true
            END:VCARD
        """.trimIndent()
        val externalContactSignature = """
            -----BEGIN PGP SIGNATURE-----

            wnUEARYKAAYFAmHm2y8AIQkQxYiMiOcEHAcWIQQ3Fuzl3PdKMX01zXnFiIyI
            5wQcB/hpAQC/sFVHObAv0Povpe3sFvMIFsTtJMQUuPRW+1kFKMJclAEArEm5
            0sn/DoagcfyQric+5pxrXQ5W3saaXcZalFk7yww=
            =R7ON
            -----END PGP SIGNATURE-----
        """.trimIndent()
        val signedContactDetails = mockk<FullContactDetails> {
            every { encryptedData } returns listOf(ContactEncryptedData(
                externalContactDataWithKey,
                externalContactSignature,
                Constants.VCardType.SIGNED
            ))
        }
        val publicKeyResponse = PublicKeyResponse(PublicKeyResponse.RecipientType.EXTERNAL.value, "", emptyArray())
        val expected = SendPreference(
            "external@proton.test", true, true, MIMEType.HTML, externalPinnedKey, PackageType.PGP_MIME,
            true, true, true, false
        )

        val actual = sendPreferencesFactory.buildFromContact("external@proton.test", publicKeyResponse, signedContactDetails) as SendPreference
        assertEqualSecurityPreferences(expected, actual)
    }

    @Test
    fun testBuildFromContactWithPinnedKeyButNoApiResponseInternal() {
        val contactDataWithPinnedKey = INTERNAL_CONTACT_DATA_WITH_PINNED_KEY.trimIndent()
        val contactSignature = INTERNAL_CONTACT_SIGNATURE.trimIndent()
        val signedContactDetails = mockk<FullContactDetails> {
            every { encryptedData } returns listOf(ContactEncryptedData(
                contactDataWithPinnedKey,
                contactSignature,
                Constants.VCardType.SIGNED
            ))
        }
        val untrustedPublicKey = """-----BEGIN PGP PUBLIC KEY BLOCK-----

            xjMEYeaS+hYJKwYBBAHaRw8BAQdAHOi00L8NL+FWB7w0c1UhR+QKXJJF+Eb9
            aKTbauiKjobNO25vdF9mb3JfZW1haWxfdXNlQGRvbWFpbi50bGQgPG5vdF9m
            b3JfZW1haWxfdXNlQGRvbWFpbi50bGQ+wo8EEBYKACAFAmHmkvoGCwkHCAMC
            BBUICgIEFgIBAAIZAQIbAwIeAQAhCRDFiIyI5wQcBxYhBDcW7OXc90oxfTXN
            ecWIjIjnBBwHoCABAJAQMdpMzgEKJ3j2a0DHnpECLvG0TGksUX7LMCuqKV8U
            APwNbZ6USbJpEqLopw8AOXpbnm7y6mdF/xYMlMG6xdMyCc44BGHmkvoSCisG
            AQQBl1UBBQEBB0B5wAbjlGvDq8ipErq1rFt9isI2KvvnXkoZ1YbQaU7pCwMB
            CAfCeAQYFggACQUCYeaS+gIbDAAhCRDFiIyI5wQcBxYhBDcW7OXc90oxfTXN
            ecWIjIjnBBwHnqIA/3nkSEebOzbEN2kuiTImngmJZYh7MOLdE4t2zJDwIaVn
            AQCkz0rgrTqudvXCggtrKlEM4di7JpCRqr537tGYJUBdCg==
            =v4yu
            -----END PGP PUBLIC KEY BLOCK-----""".trimIndent()
        val publicKeyResponse = PublicKeyResponse(PublicKeyResponse.RecipientType.INTERNAL.value, "", arrayOf( mockk<PublicKeyBody> {
            every { isAllowedForSending } returns true
            every { isAllowedForVerification } returns true
            every { publicKey } returns untrustedPublicKey
        } ))
        val expected = SendPreference(
            "free@proton.black", true, true, MIMEType.HTML, untrustedPublicKey, PackageType.PM,
            false, true, true, false
        )

        val actual = sendPreferencesFactory.buildFromContact( "free@proton.black", publicKeyResponse, signedContactDetails) as SendPreference
        assertEqualSecurityPreferences(expected, actual)
    }

    @Test
    fun `testBuildFromContactWithPinnedKeyExpiredSubKeyNoApiKeyExternal`() {
        val contactDataWithPinnedKey = EXTERNAL_CONTACT_DATA_WITH_PINNED_KEY_EXPIRED_SUBKEY.trimIndent()
        val contactSignature = EXTERNAL_CONTACT_DATA_WITH_PINNED_KEY_EXPIRED_SUBKEY_SIGNATURE.trimIndent()
        val signedContactDetails = mockk<FullContactDetails> {
            every { encryptedData } returns listOf(ContactEncryptedData(
                contactDataWithPinnedKey,
                contactSignature,
                Constants.VCardType.SIGNED
            ))
        }
        val publicKeyResponse = PublicKeyResponse(PublicKeyResponse.RecipientType.EXTERNAL.value, "", emptyArray())
        val expected = SendPreference(
            "expired-encryption-key@domain.tld", false, false, MIMEType.HTML, null, PackageType.CLEAR,
            false, true, true, false
        )

        val actual = sendPreferencesFactory.buildFromContact( "expired-encryption-key@domain.tld", publicKeyResponse, signedContactDetails) as SendPreference
        assertEqualSecurityPreferences(expected, actual)
    }

    @Test
    fun testBuildFromContactWithInvalidSignature() {
        val contactDataWithPinnedKey = INTERNAL_CONTACT_DATA_WITH_PINNED_KEY.trimIndent()
        val pinnedKey = INTERNAL_CONTACT_PINNED_KEY.trimIndent()
        val invalidContactSignature = """-----BEGIN PGP SIGNATURE-----

            wnUEARYKAAYFAmHm32sAIQkQxYiMiOcEHAcWIQQ3Fuzl3PdKMX01zXnFiIyI
            5wQcB96rAPwNg/gXK7s9xsccoyx84dh4VxrYWL+Mf1LDfOJynx4ppgEAkYXw
            3QGk4jU6nd0H8OIn/W8zGupsrvuvbVTW7xOtXw8=
            =2300
            -----END PGP SIGNATURE-----
        """.trimIndent()
        val signedContactDetails = mockk<FullContactDetails> {
            every { encryptedData } returns listOf(ContactEncryptedData(
                contactDataWithPinnedKey,
                invalidContactSignature,
                Constants.VCardType.SIGNED
            ))
        }
        val publicKeyResponse = PublicKeyResponse(PublicKeyResponse.RecipientType.INTERNAL.value, "", arrayOf( mockk<PublicKeyBody> {
            every { isAllowedForSending } returns true
            every { isAllowedForVerification } returns true
            every { publicKey } returns pinnedKey
        } ))
        val expected = SendPreference(
            "free@proton.black", true, true, MIMEType.HTML, pinnedKey, PackageType.PM,
            true, true, false, false
        )
        val actual = sendPreferencesFactory.buildFromContact("free@proton.black", publicKeyResponse, signedContactDetails)
        assertEqualSecurityPreferences(expected, actual)
    }

    @Test
    fun testBuildFromContactWithoutPinnedKeyInternal() {
        val unsignedContactData = """BEGIN:VCARD
        VERSION:4.0
        PRODID:-//ProtonMail//ProtonMail vCard 1.0.0//EN
        FN:free@proton.black
        UID:proton-autosave-8d3f4eaa-31a4-49fd-b365-81f5b69d37e7
        item1.EMAIL;PREF=1:free@proton.black
        END:VCARD
        """
        val signedContactDetails = mockk<FullContactDetails> {
            every { encryptedData } returns listOf(ContactEncryptedData(
                unsignedContactData,
                "",
                Constants.VCardType.UNSIGNED
            ))
        }
        val untrustedPublicKey = "-----BEGIN PGP PUBLIC KEY BLOCK-----\n\nuntrusted-public-key-data\n-----END PGP PUBLIC KEY BLOCK-----";
        val publicKeyResponse = PublicKeyResponse(PublicKeyResponse.RecipientType.INTERNAL.value, "", arrayOf( mockk<PublicKeyBody> {
            every { isAllowedForSending } returns true
            every { isAllowedForVerification } returns true
            every { publicKey } returns untrustedPublicKey
        } ))
        val expected = SendPreference(
            "free@proton.black", true, true, MIMEType.HTML, untrustedPublicKey, PackageType.PM,
            false, false, false, false
        )

        val actual = sendPreferencesFactory.buildFromContact("free@proton.black", publicKeyResponse, signedContactDetails) as SendPreference
        assertEqualSecurityPreferences(expected, actual)
    }

    @Test
    fun testBuildFromContactWithoutPinnedKeyExternal() {
        val unsignedContactData = """BEGIN:VCARD
        VERSION:4.0
        PRODID:-//ProtonMail//ProtonMail vCard 1.0.0//EN
        FN:free@proton.black
        UID:proton-autosave-8d3f4eaa-31a4-49fd-b365-81f5b69d37e7
        item1.EMAIL;PREF=1:free@proton.black
        END:VCARD
        """
        val signedContactDetails = mockk<FullContactDetails> {
            every { encryptedData } returns listOf(ContactEncryptedData(
                unsignedContactData,
                "",
                Constants.VCardType.UNSIGNED
            ))
        }
        val publicKeyResponse = PublicKeyResponse(PublicKeyResponse.RecipientType.EXTERNAL.value, "", emptyArray())
        val expected = SendPreference(
            "free@proton.black", false, false, MIMEType.HTML, null, PackageType.CLEAR,
            false, false, false, false
        )

        val actual = sendPreferencesFactory.buildFromContact("free@proton.black", publicKeyResponse, signedContactDetails) as SendPreference
        assertEqualSecurityPreferences(expected, actual)
    }

    private fun assertEqualSecurityPreferences(expected: SendPreference, actual: SendPreference) {
        assertEquals(expected.emailAddress, actual.emailAddress)
        assertEquals(expected.isVerified, actual.isVerified)
        if (expected.publicKey == null) {
            assertNull(actual.publicKey)
        } else {
            assertEquals(cleanArmoredDataForEquality(expected.publicKey), cleanArmoredDataForEquality(actual.publicKey))
        }
        assertEquals(expected.isPublicKeyPinned, actual.isPublicKeyPinned)
        assertEquals(expected.hasPinnedKeys(), actual.hasPinnedKeys())
        assertEquals(expected.isEncryptionEnabled, actual.isEncryptionEnabled)
        assertEquals(expected.isSignatureEnabled, actual.isSignatureEnabled)
    }

    // Extract base64 part of armored data to compare values generated across different libraries
    private fun cleanArmoredDataForEquality(armored: String): String {
        val noHeaders = armored.split("\n\n")[1]
        // OpenPGP.js and gopenpgp armor to different line lengths, so we need to remove new line chars
        return noHeaders.filter { !it.isWhitespace() }
    }
}
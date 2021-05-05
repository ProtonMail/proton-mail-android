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

package ch.protonmail.android.crypto.factories

import androidx.test.filters.SmallTest
import ch.protonmail.android.api.models.MessageRecipient
import ch.protonmail.android.api.models.SendPreference
import ch.protonmail.android.api.models.enumerations.MIMEType
import ch.protonmail.android.api.models.enumerations.PackageType
import ch.protonmail.android.api.models.factories.MessageSecurityOptions
import ch.protonmail.android.api.models.factories.PackageFactory
import ch.protonmail.android.crypto.AddressCrypto
import ch.protonmail.android.crypto.CipherText
import ch.protonmail.android.data.local.model.Message
import ch.protonmail.android.domain.entity.Id
import ch.protonmail.android.utils.HTMLToMDConverter
import ch.protonmail.android.utils.base64.Base64Encoder
import com.proton.gopenpgp.crypto.SessionKey
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.InjectMockKs
import io.mockk.impl.annotations.MockK
import io.mockk.impl.annotations.RelaxedMockK
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.BeforeTest
import kotlin.test.Test

@SmallTest
class PackageFactoryTest {

    @MockK
    private lateinit var addressCryptoFactory: AddressCrypto.Factory

    @RelaxedMockK
    private lateinit var base64Encoder: Base64Encoder

    @RelaxedMockK
    private lateinit var htmlToMDConverter: HTMLToMDConverter

    @InjectMockKs
    private lateinit var packageFactory: PackageFactory

    @BeforeTest
    fun setUp() {
        MockKAnnotations.init(this)
    }

    @Test
    fun generatePackagesCreatesTopLevelPackageWithDataFromCryptoWhenSendPreferencesForRecipientsAreNotEmpty() {
        val addressId = "addressId"
        val message = Message(
            addressID = addressId,
            toList = listOf(MessageRecipient("recipient234", "email823@pm.me")),
            messageBody = "Some message body"
        )
        val preferencesPublicKey = "publicKey"
        val preferences = listOf(
            SendPreference(
                "email823@pm.me",
                true,
                true,
                MIMEType.PLAINTEXT,
                preferencesPublicKey,
                PackageType.PGP_MIME,
                false,
                true,
                false,
                false
            )
        )
        val securityOptions = MessageSecurityOptions("pwd", "hint", 180000L)
        val crypto = mockk<AddressCrypto>()
        val bodyPlainText = "html converted to plainText"
        val keyPackage = "cipherTextKeyPacket".toByteArray()
        val cipherText = mockk<CipherText>() {
            every { keyPacket } returns keyPackage
            every { dataPacket } returns "dataPacket".toByteArray()
        }
        val currentUsername = "Marino"
        val currentUserId = Id("marino")
        every { base64Encoder.encode(any()) } returns "encodedBase64"
        every { addressCryptoFactory.create(currentUserId, Id(addressId)) } returns crypto
        every { htmlToMDConverter.convert(any()) } returns bodyPlainText
        every { crypto.encrypt(bodyPlainText, true) } returns cipherText
        every { crypto.decryptKeyPacket(keyPackage) } returns "decryptedKeyPackets".toByteArray()
        every { crypto.encryptKeyPacket(any(), any()) } returns "encryptedKeyPackets".toByteArray()
        every { crypto.getSessionKey(keyPackage) } returns SessionKey("token".toByteArray(), "algorithm")

        packageFactory.generatePackages(message, preferences, securityOptions, currentUserId)

        verify { crypto.decryptKeyPacket(any()) }
        verify { crypto.getSessionKey(any()) }
        verify { crypto.encryptKeyPacket(any(), any()) }
    }

}
